package ourgrid.rules

import org.openremote.manager.rules.RulesBuilder
import org.openremote.model.attribute.AttributeInfo
import org.openremote.model.query.AssetQuery
import org.openremote.model.rules.Assets

import java.util.logging.Logger

Assets assets = binding.assets
RulesBuilder rules = binding.rules
Logger LOG = binding.LOG

// -------------------------input------------------------- //

// Set the ["inputAssetName", "inputAttributeName", "inputAssetId", "outputAssetName", "outputAttributeName", "outputAssetId"] here:
def attributes = [
        ["inputAssetName1", "inputAttributeName1", "inputAssetId1", "outputAssetName1", "outputAttributeName1", "outputAssetId1"],
        ["inputAssetName2", "inputAttributeName2", "inputAssetId2", "outputAssetName2", "outputAttributeName2", "outputAssetId2"],
        ["inputAssetName3", "inputAttributeName3", "inputAssetId3", "outputAssetName3", "outputAttributeName3", "outputAssetId3"]
]

// ------------------------------------------------------- //

// Collect all unique asset ID's and attribute names
def inputAttributeNames = attributes.collect { it[1] }.unique() as String[]
def inputAssetIds = attributes.collect { it[2] }.unique() as String[]

// Find the minimum number of required asset queries
def assetQueryOptimisedBy = "assetIds"

if (inputAssetIds.size() > inputAttributeNames.size()) {
    assetQueryOptimisedBy = "attributeNames"
}

// Map all asset queries
def assetQueryMap = attributes.inject([:]) { map, row ->
    def inputAttributeName = row[1]
    def inputAssetId = row[2]

    if (assetQueryOptimisedBy == "assetIds") {
        map[inputAssetId] = (map[inputAssetId] ?: []) + inputAttributeName
    } else if (assetQueryOptimisedBy == "attributeNames") {
        map[inputAttributeName] = (map[inputAttributeName] ?: []) + inputAssetId
    }
    return map
} as Map<String, List<Map<String, String>>>

// Map input to output keys
def inputOutputMap = attributes.inject([:]) { map, row ->
    def inputAttributeName = row[1]
    def inputAssetId = row[2]
    def inputKey = inputAssetId + inputAttributeName
    def outputAttribute = [assetId: row[5], attributeName: row[4]]

    map[inputKey] = (map[inputKey] ?: []) + [outputAttribute]
    return map
}

rules.add()
        .name("Link attributes rule")
        .when({ facts ->
            def attributeChanges = []

            // Find attribute changes for each asset query
            assetQueryMap.each {
                def inputKey = it.key as String
                def inputValues = it.value as String[]

                // Create asset query
                def assetQuery = new AssetQuery()

                if (assetQueryOptimisedBy == "assetIds") {
                    assetQuery = assetQuery.ids(inputKey).attributeNames(inputValues)
                } else if (assetQueryOptimisedBy == "attributeNames") {
                    assetQuery = assetQuery.ids(inputValues).attributeNames(inputKey)
                }

                // Find attribute changes
                List<AttributeInfo> changes = facts
                        .matchAssetState(assetQuery)
                        .filter { attributeInfo ->
                            boolean timestampChanged = false

                            // Get previous attribute state from facts
                            Optional<AttributeInfo> attributeInfoPrevious = facts.matchFirst(attributeInfo.id + attributeInfo.name)

                            // Check if attribute timestamp has been updated (attribute value can be the same)
                            if (attributeInfo.timestamp > attributeInfoPrevious.map { it.timestamp }.orElse(0)) {
                                timestampChanged = true
                            }
                            return timestampChanged
                        }
                        .toList()

                attributeChanges.addAll(changes)
            }

            // Bind attribute info for the then trigger
            if (!attributeChanges.isEmpty()) {
                facts.bind("attributeChanges", attributeChanges)
            }

            // Trigger rule if there are attribute changes to process
            return !attributeChanges.isEmpty()
        })
        .then({ facts ->
            def attributeChanges = facts.bound("attributeChanges") as List<AttributeInfo>

            // Create fact for each attribute change
            if (attributeChanges != null) {
                attributeChanges.forEach { attributeInfo -> facts.put(attributeInfo.id + attributeInfo.name, attributeInfo as Object) }
            }

            // Update linked output attributes
            attributeChanges.forEach { attributeInfo ->
                def inputKey = attributeInfo.id + attributeInfo.name
                def outputKeys = inputOutputMap[inputKey] as List<Map<String, String>>

                if (outputKeys == null) {
                    return
                }

                String value = attributeInfo.value.orElse(null)

                outputKeys.each { outputKey ->
                    def outputAssetId = outputKey.get("assetId")
                    def outputAttributeName = outputKey.get("attributeName")

                    // Update attribute
                    assets.dispatch(outputAssetId, outputAttributeName, value)
                }
            }
        })