package ourgrid.rules

import org.openremote.agent.custom.ourgrid.OurgridBatteryAsset
import org.openremote.agent.custom.ourgrid.OurgridChallengesAsset
import org.openremote.agent.custom.ourgrid.OurgridMeterAsset
import org.openremote.manager.rules.RulesBuilder
import org.openremote.manager.rules.RulesFacts
import org.openremote.model.asset.Asset
import org.openremote.model.attribute.AttributeInfo
import org.openremote.model.query.AssetQuery
import org.openremote.model.rules.Assets

import java.text.SimpleDateFormat
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.logging.Logger

Logger LOG = binding.LOG
RulesBuilder rules = binding.rules
Assets assets = binding.assets

// Set the asset ID's:
String meterSumAssetId = "setId1"
String challengesAssetId = "setId2"


// Time triggers for rules
ZoneId zone = ZoneId.of("Europe/Amsterdam")
int previousHourRule1 = -1
long previousMillisRule2 = System.currentTimeMillis() - System.currentTimeMillis() % (1 * 60 * 1000) + (1 * 60 * 1000)

// Action triggers for rules
String challengeGeneralStatusPreviousRule2 = ""
String challengeGeneralStatusPreviousRule3 = ""

rules.add()
        .priority(1)
        .name("OurGrid battery start charging rule")
        .when({ facts ->
            boolean triggerRule = false
            int currentHour = ZonedDateTime.ofInstant(facts.getClock().getNow(), zone).toLocalDateTime().getHour()

            // Trigger rule at 2:00am and 1:00pm
            if ((previousHourRule1 == 1 && currentHour == 2) || (previousHourRule1 == 12 && currentHour == 13)) {
                triggerRule = true
            }

            previousHourRule1 = currentHour

            return triggerRule
        })
        .then({ facts ->
            def attributeNames = [
                    OurgridBatteryAsset.ENERGY_LEVEL_PERCENTAGE.name,
                    OurgridBatteryAsset.ENERGY_LEVEL_PERCENTAGE_MAX.name,
                    OurgridBatteryAsset.POWER_IMPORT_MAX.name,
                    OurgridBatteryAsset.POWER_SETPOINT.name
            ] as String[]

            def batteriesAttributes = getBatteriesAttributes(meterSumAssetId, attributeNames, facts) as Map<String, Map<String, Object>>

            batteriesAttributes.each {
                def assetId = it.key as String
                def energyLevelPercentage = it.value.get(OurgridBatteryAsset.ENERGY_LEVEL_PERCENTAGE.name) as Double
                def energyLevelPercentageMax = it.value.get(OurgridBatteryAsset.ENERGY_LEVEL_PERCENTAGE_MAX.name) as Double
                def powerImportMax = it.value.get(OurgridBatteryAsset.POWER_IMPORT_MAX.name) as Double
                def powerSetpoint = it.value.get(OurgridBatteryAsset.POWER_SETPOINT.name) as Double

                if (powerImportMax == null || energyLevelPercentage == null || energyLevelPercentageMax == null || energyLevelPercentage >= energyLevelPercentageMax) {
                    powerImportMax = 0.0
                }

                if (powerSetpoint == null) {
                    powerSetpoint = 0.0
                }

                if (powerSetpoint != powerImportMax) {
                    assets.dispatch(assetId, OurgridBatteryAsset.POWER_SETPOINT.name, powerImportMax)
                }
            }
        })

rules.add()
        .priority(2)
        .name("OurGrid battery control power set-point rule")
        .when({ facts ->
            boolean triggerRule = false
            long currentMillis = facts.clock.currentTimeMillis

            // Trigger rule every 1 minute = 60000 ms
            if (currentMillis >= previousMillisRule2) {
                previousMillisRule2 += 60000
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
                // Power set-point logic during challenge
                def attributeNames = [
                        OurgridBatteryAsset.ALLOW_AUTOMATIC_CONTROL_BUTTON.name,
                        OurgridBatteryAsset.ALLOW_DISCHARGING_BUTTON.name,
                        OurgridBatteryAsset.ENERGY_CAPACITY.name,
                        OurgridBatteryAsset.ENERGY_LEVEL_PERCENTAGE.name,
                        OurgridBatteryAsset.ENERGY_LEVEL_PERCENTAGE_MIN.name,
                        OurgridBatteryAsset.POWER_EXPORT_MAX.name,
                        OurgridBatteryAsset.POWER_SETPOINT.name
                ] as String[]

                def batteriesAttributes = getBatteriesAttributes(meterSumAssetId, attributeNames, facts) as Map<String, Map<String, Object>>

                batteriesAttributes.each {
                    def assetId = it.key as String
                    def allowAutomaticControlButton = it.value.get(OurgridBatteryAsset.ALLOW_AUTOMATIC_CONTROL_BUTTON.name) as Boolean
                    def allowDischargingButton = it.value.get(OurgridBatteryAsset.ALLOW_DISCHARGING_BUTTON.name) as Boolean
                    def energyCapacity = it.value.get(OurgridBatteryAsset.ENERGY_CAPACITY.name) as Double
                    def energyLevelPercentage = it.value.get(OurgridBatteryAsset.ENERGY_LEVEL_PERCENTAGE.name) as Double
                    def energyLevelPercentageMin = it.value.get(OurgridBatteryAsset.ENERGY_LEVEL_PERCENTAGE_MIN.name) as Double
                    def powerExportMax = it.value.get(OurgridBatteryAsset.POWER_EXPORT_MAX.name) as Double
                    def powerSetpoint = (it.value.get(OurgridBatteryAsset.POWER_SETPOINT.name) as Double)

                    if (powerSetpoint == null || energyLevelPercentage == null || energyLevelPercentageMin == null ||
                            (powerSetpoint < 0.0 && energyLevelPercentage <= energyLevelPercentageMin)) {
                        powerSetpoint = 0.0
                    }

                    if (allowDischargingButton == null) {
                        allowDischargingButton = false
                    }

                    if (allowAutomaticControlButton == true && allowDischargingButton == false) {
                        allowDischargingButton = true
                        assets.dispatch(assetId, OurgridBatteryAsset.ALLOW_DISCHARGING_BUTTON.name, true)
                    }

                    def challengeEnd = facts
                            .matchFirstAssetState(new AssetQuery().ids(challengesAssetId).attributeName(OurgridChallengesAsset.CHALLENGE_END.name))
                            .flatMap { it.value }
                            .orElse(null) as String

                    def powerSetpointDischarge = 0.0 as Double

                    if (challengeEnd != null && energyCapacity != null && powerExportMax != null && allowDischargingButton == true) {
                        def sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss") as SimpleDateFormat
                        long challengeEndMillis = sdf.parse(challengeEnd).getTime()
                        long currentMillis = facts.clock.currentTimeMillis

                        if (currentMillis < challengeEndMillis) {
                            def challengeDurationHours = (challengeEndMillis - currentMillis) / 3600000 as Double
                            def energyCapacityUsable = energyCapacity * (energyLevelPercentage - energyLevelPercentageMin) / 100 as Double
                            powerSetpointDischarge = Math.round(-1000 * energyCapacityUsable / challengeDurationHours) / 1000

                            if (powerSetpointDischarge < -powerExportMax) {
                                powerSetpointDischarge = -powerExportMax
                            }
                        }
                    }

                    if (powerSetpoint != powerSetpointDischarge) {
                        assets.dispatch(assetId, OurgridBatteryAsset.POWER_SETPOINT.name, powerSetpointDischarge)
                    }
                }
            } else {
                // Power set-point logic outside challenge
                def attributeNames = [
                        OurgridBatteryAsset.ENERGY_LEVEL_PERCENTAGE.name,
                        OurgridBatteryAsset.ENERGY_LEVEL_PERCENTAGE_MAX.name,
                        OurgridBatteryAsset.POWER_SETPOINT.name
                ] as String[]

                def batteriesAttributes = getBatteriesAttributes(meterSumAssetId, attributeNames, facts) as Map<String, Map<String, Object>>

                batteriesAttributes.each {
                    def assetId = it.key as String
                    def energyLevelPercentage = it.value.get(OurgridBatteryAsset.ENERGY_LEVEL_PERCENTAGE.name) as Double
                    def energyLevelPercentageMax = it.value.get(OurgridBatteryAsset.ENERGY_LEVEL_PERCENTAGE_MAX.name) as Double
                    def powerSetpoint = it.value.get(OurgridBatteryAsset.POWER_SETPOINT.name) as Double

                    def powerSetpointNew = powerSetpoint as Double

                    if (powerSetpointNew == null || energyLevelPercentage == null || energyLevelPercentageMax == null ||
                            (powerSetpointNew > 0.0 && energyLevelPercentage >= energyLevelPercentageMax) || (powerSetpointNew < 0.0)
                    ) {
                        powerSetpointNew = 0.0
                    }

                    if (powerSetpoint == null || powerSetpoint != powerSetpointNew) {
                        assets.dispatch(assetId, OurgridBatteryAsset.POWER_SETPOINT.name, 0.0)
                    }
                }
            }
        })

rules.add()
        .priority(3)
        .name("OurGrid battery end of challenge rule")
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
                    OurgridBatteryAsset.ALLOW_DISCHARGING_BUTTON.name
            ] as String[]

            def batteriesAttributes = getBatteriesAttributes(meterSumAssetId, attributeNames, facts) as Map<String, Map<String, Object>>

            batteriesAttributes.each {
                def assetId = it.key as String
                def allowDischargingButton = it.value.get(OurgridBatteryAsset.ALLOW_DISCHARGING_BUTTON.name) as Boolean

                if (allowDischargingButton == true) {
                    assets.dispatch(assetId, OurgridBatteryAsset.ALLOW_DISCHARGING_BUTTON.name, false)
                }
            }
        })


private Map<String, Map<String, Object>> getBatteriesAttributes(String meterSumAssetId, String[] attributeNames, RulesFacts facts) {
    // Check if parent asset ID is valid
    def meterSumAsset = assets.getResults(new AssetQuery().ids(meterSumAssetId)).findFirst() as Optional<Asset>

    if (meterSumAsset.isEmpty()) {
        LOG.warning("No Meter Sum asset found with ID: '" + meterSumAssetId + "'; Check asset ID")
        return
    }

    // Get household meter asset ID's
    def meterAssetIds = assets.getResults(new AssetQuery().parents(meterSumAssetId).types(OurgridMeterAsset)).map { it.id }.toList() as String[]

    def batteriesAttributesList = facts
            .matchAssetState(new AssetQuery().parents(meterAssetIds).types(OurgridBatteryAsset).attributeNames(attributeNames))
            .toList() as List<AttributeInfo>

    // Group attributes per asset ID
    def batteriesAttributes = [:].withDefault { [:].withDefault { null } } as Map<String, Map<String, Object>>

    batteriesAttributesList.each { attributeInfo ->
        def id = attributeInfo.id as String
        def attributeName = attributeInfo.name as String
        def value = attributeInfo.value.orElse(null)

        batteriesAttributes[id][attributeName] = value
    }

    return batteriesAttributes
}