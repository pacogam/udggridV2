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

import java.util.logging.Logger

Assets assets = binding.assets
RulesBuilder rules = binding.rules
Logger LOG = binding.LOG


// -------------------------input------------------------- //

// Set the asset ID's:
String meterSumAssetId = "setId1"
String challengesAssetId = "setId2"

// ------------------------------------------------------- //


// Action triggers for rules
String challengeGeneralStatusPreviousRule1 = ""

rules.add()
        .priority(1)
        .name("OurGrid join challenge automatically rule")
        .when({ facts ->
            boolean triggerRule = false

            // Check if challenges asset ID is valid
            def challengesAsset = facts.matchFirstAssetState(new AssetQuery().ids(challengesAssetId).attributeName(OurgridChallengesAsset.CHALLENGE_GENERAL_STATUS.name)) as Optional<AttributeInfo>

            if (challengesAsset.isEmpty()) {
                LOG.warning("No Challenges asset found with ID: '" + challengesAssetId + "'; Check asset ID and if the rule state configuration is added to the attribute")
                return false
            }

            def challengeGeneralStatus = challengesAsset.get().value.orElse(null).toString() as String
            def joinedChallenge = OurgridChallengesAsset.ChallengeStatusGeneralValueType.joinedChallenge.toString() as String

            if (challengeGeneralStatus == joinedChallenge && challengeGeneralStatusPreviousRule1 != joinedChallenge) {
                def joinChallenge = OurgridMeterAsset.ChallengeStatusValueType.joinChallenge.toString() as String
                def metersReadyToJoin = assets.getResults(new AssetQuery().parents(meterSumAssetId).types(OurgridMeterAsset).attributeValue(OurgridMeterAsset.CHALLENGE_STATUS.name, joinChallenge)).any()

                // Trigger rule after district rules have updated meter challenge status to joinChallenge
                if (metersReadyToJoin) {
                    challengeGeneralStatusPreviousRule1 = joinedChallenge
                    triggerRule = true
                }
            } else {
                challengeGeneralStatusPreviousRule1 = challengeGeneralStatus
            }

            return triggerRule
        })
        .then({ facts ->
            def joinedChallenge = OurgridMeterAsset.ChallengeStatusValueType.joinedChallenge.toString() as String

            def attributeNamesMeterAsset = [
                    OurgridMeterAsset.CHALLENGE_STATUS.name
            ] as String[]

            def metersAttributes = getChildrenAttributes(meterSumAssetId, attributeNamesMeterAsset, OurgridMeterAsset, facts) as Map<String, Map<String, Object>>

            // Get household meter asset ID's
            def meterAssetIds = assets.getResults(new AssetQuery().parents(meterSumAssetId).types(OurgridMeterAsset)).map { it.id }.toList() as String[]

            // Join challenge if meter has a child asset with allow automatic control turned on
            def allowAutomaticControlList = facts
                    .matchAssetState(new AssetQuery().parents(meterAssetIds).attributeNames(OurgridBatteryAsset.ALLOW_AUTOMATIC_CONTROL_BUTTON.name))
                    .findAll { it.value.orElse(false) == true }
                    .toList() as List<AttributeInfo> as List<AttributeInfo>

            def allowAutomaticControlMeterAssetIds = allowAutomaticControlList.parentId.unique()

            for (String assetId : allowAutomaticControlMeterAssetIds) {
                def meterChallengeStatus = metersAttributes[assetId]?.get(OurgridMeterAsset.CHALLENGE_STATUS.name)?.toString() as String

                if (meterChallengeStatus != joinedChallenge) {
                    assets.dispatch(assetId, OurgridMeterAsset.CHALLENGE_STATUS.name, joinedChallenge)
                }
            }
        })

private Map<String, Map<String, Object>> getChildrenAttributes(String parentAssetId, String[] attributeNames, Class<Asset> assetType, RulesFacts facts) {
    // Check if parent asset ID is valid
    def parentAsset = assets.getResults(new AssetQuery().ids(parentAssetId)).findFirst() as Optional<Asset>

    if (parentAsset.isEmpty()) {
        LOG.warning("No parent asset found with ID: '" + parentAsset + "'; Check asset ID")
        return
    }

    def attributesList = facts
            .matchAssetState(new AssetQuery().parents(parentAssetId).types(assetType).attributeNames(attributeNames))
            .toList() as List<AttributeInfo>

    // Group attributes per asset ID
    def attributes = [:].withDefault { [:].withDefault { null } } as Map<String, Map<String, Object>>

    attributesList.each { attributeInfo ->
        def id = attributeInfo.id as String
        def attributeName = attributeInfo.name as String
        def value = attributeInfo.value.orElse(null)

        attributes[id][attributeName] = value
    }

    if (attributes.isEmpty()) {
        LOG.warning("No attributes found for children of parent asset with ID: '" + parentAsset + "'; Check children assets and if the rule state configuration is added to the attributes")
    }

    return attributes
}
