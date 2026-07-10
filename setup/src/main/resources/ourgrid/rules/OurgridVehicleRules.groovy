package ourgrid.rules

import org.openremote.agent.custom.ourgrid.OurgridChallengesAsset
import org.openremote.agent.custom.ourgrid.OurgridMeterAsset
import org.openremote.agent.custom.ourgrid.OurgridVehicleAsset
import org.openremote.manager.rules.RulesBuilder
import org.openremote.manager.rules.RulesFacts
import org.openremote.model.asset.Asset
import org.openremote.model.attribute.AttributeInfo
import org.openremote.model.query.AssetQuery
import org.openremote.model.query.filter.NumberPredicate
import org.openremote.model.rules.Assets

import java.util.function.Function
import java.util.logging.Logger

Assets assets = binding.assets
RulesBuilder rules = binding.rules
Logger LOG = binding.LOG


// -------------------------input------------------------- //

// Set the asset ID's:
String meterSumAssetId = "setId1"
String challengesAssetId = "setId2"

// Vehicle control settings
double vehicleStopChargingMinMeterPower = 5000.0 // Minimum meter power consumption (in W) required to stop charging vehicles during challenges

// ------------------------------------------------------- //


// Triggers for rules
String challengeGeneralStatusPreviousRule2 = ""
String challengeGeneralStatusPreviousRule3 = ""

// Time triggers for rules
long rulesStartTimeMillis = System.currentTimeMillis()

// Set the [triggerPeriodMillis, triggerDelayMillis]:
long[] triggerInputsRule2 = [1 * 60 * 1000, 1 * 60 * 1000]
long triggerTimeMillisRule2 = calculateNextTriggerTime(rulesStartTimeMillis, triggerInputsRule2)

private static long calculateNextTriggerTime(long timeMillis, long[] triggerInputs) {
    long triggerPeriodMillis = triggerInputs[0]
    long triggerDelayMillis = triggerInputs[1]

    return timeMillis - timeMillis % triggerPeriodMillis + triggerPeriodMillis + triggerDelayMillis
}

rules.add()
        .priority(2)
        .name("OurGrid vehicle control stop charging rule")
        .when({ facts ->
            boolean triggerRule = false
            long currentTimeMillis = facts.clock.currentTimeMillis

            // Trigger rule on a time-based interval
            if (currentTimeMillis > triggerTimeMillisRule2) {
                triggerTimeMillisRule1 = calculateNextTriggerTime(currentTimeMillis, triggerInputsRule2)
                return true
            }

            def challengeGeneralStatus = facts
                    .matchFirstAssetState(new AssetQuery().ids(challengesAssetId).attributeName(OurgridChallengesAsset.CHALLENGE_GENERAL_STATUS.name))
                    .flatMap { it.value }
                    .orElse("") as String

            def activeChallenge = OurgridChallengesAsset.ChallengeStatusGeneralValueType.activeChallenge.toString() as String
            def joinedChallenge = OurgridChallengesAsset.ChallengeStatusGeneralValueType.joinedChallenge.toString() as String

            if (challengeGeneralStatus == activeChallenge && challengeGeneralStatusPreviousRule2 == joinedChallenge) {
                triggerRule = true
            }

            challengeGeneralStatusPreviousRule2 = challengeGeneralStatus

            return triggerRule
        })
        .then({ facts ->
            def challengeGeneralStatus = facts
                    .matchFirstAssetState(new AssetQuery().ids(challengesAssetId).attributeName(OurgridChallengesAsset.CHALLENGE_GENERAL_STATUS.name))
                    .flatMap { it.value }
                    .orElse(null) as String

            def activeChallenge = OurgridChallengesAsset.ChallengeStatusGeneralValueType.activeChallenge.toString() as String

            if (challengeGeneralStatus == activeChallenge) {
                // Vehicle charging logic during challenge
                def attributeNames = [
                        OurgridVehicleAsset.ALLOW_AUTOMATIC_CONTROL_BUTTON.name,
                        OurgridVehicleAsset.ALLOW_STOP_CHARGING_BUTTON.name,
                ] as String[]

                def meterAssetQueryCustomizer = (AssetQuery assetQuery) -> assetQuery.attributeValue(OurgridMeterAsset.POWER.name, new NumberPredicate(vehicleStopChargingMinMeterPower, AssetQuery.Operator.GREATER_EQUALS))
                def vehicleAttributes = getVehicleAttributes(meterSumAssetId, meterAssetQueryCustomizer, attributeNames, facts) as Map<String, Map<String, Object>>

                vehicleAttributes.each {
                    def assetId = it.key as String
                    def allowAutomaticControlButton = it.value.get(OurgridVehicleAsset.ALLOW_AUTOMATIC_CONTROL_BUTTON.name) as Boolean
                    def allowStopChargingButton = it.value.get(OurgridVehicleAsset.ALLOW_STOP_CHARGING_BUTTON.name) as Boolean

                    if (allowAutomaticControlButton == null) {
                        allowAutomaticControlButton = true
                    }

                    if (allowStopChargingButton == null) {
                        allowStopChargingButton = false
                    }

                    if (allowAutomaticControlButton && !allowStopChargingButton) {
                        assets.dispatch(assetId, OurgridVehicleAsset.ALLOW_STOP_CHARGING_BUTTON.name, true)
                    }
                }
            }
        })

rules.add()
        .priority(3)
        .name("OurGrid vehicle end of challenge rule")
        .when({ facts ->
            boolean triggerRule = false
            // Check if challenges asset ID is valid
            def challengesAsset = facts.matchFirstAssetState(new AssetQuery().ids(challengesAssetId).attributeName(OurgridChallengesAsset.CHALLENGE_GENERAL_STATUS.name)) as Optional<AttributeInfo>

            if (challengesAsset.isEmpty()) {
                LOG.warning("No Challenges asset found with ID: '" + challengesAssetId + "'; Check asset ID and if the rule state configuration is added to the attribute")
                return false
            }

            def challengeGeneralStatus = challengesAsset.get().value.orElse(null).toString() as String
            def activeChallenge = OurgridChallengesAsset.ChallengeStatusGeneralValueType.activeChallenge.toString() as String
            def noChallenge = OurgridChallengesAsset.ChallengeStatusGeneralValueType.noChallenge.toString() as String

            if (challengeGeneralStatus == noChallenge && challengeGeneralStatusPreviousRule3 == activeChallenge) {
                triggerRule = true
            }

            challengeGeneralStatusPreviousRule3 = challengeGeneralStatus

            return triggerRule
        })
        .then({ facts ->
            def attributeNames = [
                    OurgridVehicleAsset.ALLOW_STOP_CHARGING_BUTTON.name
            ] as String[]

            def vehicleAttributes = getVehicleAttributes(meterSumAssetId, null, attributeNames, facts) as Map<String, Map<String, Object>>

            vehicleAttributes.each {
                def assetId = it.key as String
                def allowStopChargingButton = it.value.get(OurgridVehicleAsset.ALLOW_STOP_CHARGING_BUTTON.name) as Boolean

                if (allowStopChargingButton == true) {
                    assets.dispatch(assetId, OurgridVehicleAsset.ALLOW_STOP_CHARGING_BUTTON.name, false)
                }
            }
        })


private Map<String, Map<String, Object>> getVehicleAttributes(String meterSumAssetId, Function<AssetQuery, AssetQuery> meterAssetQueryCustomizer, String[] attributeNames, RulesFacts facts) {
    // Check if parent asset ID is valid
    def meterSumAsset = assets.getResults(new AssetQuery().ids(meterSumAssetId)).findFirst() as Optional<Asset>

    if (meterSumAsset.isEmpty()) {
        LOG.warning("No Meter Sum asset found with ID: '" + meterSumAssetId + "'; Check asset ID")
        return
    }

    // Get household meter asset ID's
    def meterAssetQuery = new AssetQuery().parents(meterSumAssetId).types(OurgridMeterAsset)
    if (meterAssetQueryCustomizer != null) {
        meterAssetQuery = meterAssetQueryCustomizer.apply(meterAssetQuery)
    }
    def meterAssetIds = assets.getResults(meterAssetQuery).map { it.id }.toList() as String[]

    def vehicleAttributesList = facts
            .matchAssetState(new AssetQuery().parents(meterAssetIds).types(OurgridVehicleAsset).attributeNames(attributeNames))
            .toList() as List<AttributeInfo>

    // Group attributes per asset ID
    def vehicleAttributes = [:].withDefault { [:].withDefault { null } } as Map<String, Map<String, Object>>

    vehicleAttributesList.each { attributeInfo ->
        def id = attributeInfo.id as String
        def attributeName = attributeInfo.name as String
        def value = attributeInfo.value.orElse(null)

        vehicleAttributes[id][attributeName] = value
    }

    return vehicleAttributes
}