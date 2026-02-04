package ourgrid.rules

import org.openremote.agent.custom.ourgrid.OurgridChallengesAsset
import org.openremote.agent.custom.ourgrid.OurgridMeterAsset
import org.openremote.agent.custom.ourgrid.OurgridPeaksAsset
import org.openremote.manager.rules.RulesBuilder
import org.openremote.model.query.AssetQuery
import org.openremote.model.rules.Assets

import java.util.logging.Logger

Assets assets = binding.assets
RulesBuilder rules = binding.rules
Logger LOG = binding.LOG

// -------------------------input------------------------- //

/* Add the following attributes to the challenges asset:
    - attributeName="collectAndResetEarnedPoints", valueType="Boolean"; Configuration items: "Read only", "Rule state"
    - attributeName="totalPointsSummary", valueType="Text"; Configuration items: "Multiline", "Read only", "Rule state"
 */

// Set asset ID's:
String meterSumAssetId = "setId1"
String challengesAssetId = "setId2"
String peaksAssetId = "setId3"

// Set this variable to true when you want to reset all data-points when creating a points summary:
boolean resetEarnedPoints = false

// ------------------------------------------------------- //

def assetType = OurgridMeterAsset
String[] attributeNames = [OurgridMeterAsset.CHALLENGE_POINTS.name, OurgridMeterAsset.PEAK_POINTS.name, OurgridMeterAsset.CHALLENGE_EARNINGS.name]
int newAttributeValue = 0

String attributeNameButton = "collectAndResetEarnedPoints"
String attributeNameSummary = "totalPointsSummary"

rules.add()
        .name("Collect and reset earned points rule")
        .when({ facts ->
            def challengesAsset = facts.matchFirstAssetState(new AssetQuery().ids(challengesAssetId).attributeNames(attributeNameButton))

            if (challengesAsset.isEmpty()) {
                LOG.warning(String.format("No OurGrid Challenges Asset found with: assetId='%s' and attributeName='%s'; Check the asset ID and if the 'Rule state' configuration item is added to the attribute", challengesAssetId, attributeNameButton))
                return false
            }

            boolean collectAndResetEarnedPoints = challengesAsset.flatMap { it.value }.orElse(false) as Boolean

            if (collectAndResetEarnedPoints) {
                assets.dispatch(challengesAssetId, attributeNameButton, false)
                LOG.info(String.format("Collect and reset earned points for OurGrid Challenges Asset: assetId='%s', assetName='%s';", challengesAssetId, challengesAsset.get().assetName))
                return true
            }

            return false
        })
        .then({ facts ->
            // Find all relevant attributes
            def attributeList = facts
                    .matchAssetState(
                            new AssetQuery()
                                    .parents(meterSumAssetId)
                                    .types(assetType)
                                    .attributeNames(attributeNames)
                    )
                    .toList()

            // 1. Update attributes with new value
            Map<String, Integer> challengePointsMap = new HashMap<>()
            Map<String, Double> peakPointsMap = new HashMap<>()
            Map<String, Double> earningsMap = new HashMap<>()

            attributeList.forEach { attributeInfo ->
                String assetId = attributeInfo.id
                String assetName = attributeInfo.assetName
                String attributeName = attributeInfo.name

                if (attributeName == OurgridMeterAsset.CHALLENGE_POINTS.name) {
                    def challengePoints = attributeInfo.value.orElse(0) as Integer
                    challengePointsMap.put(assetName, challengePoints)
                } else if (attributeName == OurgridMeterAsset.PEAK_POINTS.name) {
                    def peakPoints = attributeInfo.value.orElse(0) as Double
                    peakPointsMap.put(assetName, peakPoints)
                } else if (attributeName == OurgridMeterAsset.CHALLENGE_EARNINGS.name) {
                    def earnings = attributeInfo.value.orElse(0) as Double
                    earningsMap.put(assetName, earnings)
                }

                if (resetEarnedPoints) {
                    assets.dispatch(assetId, attributeName, newAttributeValue)
                }
            }

            if (resetEarnedPoints) {
                assets.dispatch(challengesAssetId, OurgridChallengesAsset.CHALLENGE_POINTS_TOTAL.name, newAttributeValue)
                assets.dispatch(peaksAssetId, OurgridPeaksAsset.PEAK_POINTS_TOTAL.name, newAttributeValue)
            }


            // 2. Create earned points summary in CSV format
            String outputCSV = "assetName,challengePoints,peakPoints,totalPoints,earnings\n"

            // Sort asset names in alphabetical order
            List<String> assetNames = challengePointsMap.keySet().sort()

            assetNames.forEach { assetName ->
                def challengePoints = challengePointsMap.get(assetName)
                def peakPoints = peakPointsMap.get(assetName)
                def totalPoints = challengePoints + peakPoints
                def earnings = earningsMap.get(assetName)

                String rowCSV = assetName + "," + challengePoints + "," + peakPoints + "," + totalPoints + "," + earnings + "\n"
                outputCSV = outputCSV + rowCSV
            }

            LOG.info(String.format("Earned points summary can be found in the Challenges Asset: assetId='%s', attributeName'%s'", challengesAssetId, attributeNameSummary))

            assets.dispatch(challengesAssetId, attributeNameSummary, outputCSV)
        })