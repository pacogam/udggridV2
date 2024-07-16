package ourgrid.rules

import org.openremote.manager.rules.RulesBuilder
import org.openremote.model.attribute.AttributeInfo
import org.openremote.model.query.AssetQuery
import org.openremote.model.rules.Assets

import java.util.logging.Logger

Logger LOG = binding.LOG
RulesBuilder rules = binding.rules
Assets assets = binding.assets

// Put the ["asset name", "attribute name" ,"input asset ID" and "output asset ID"] here:
def attributes = [
        ["AssetName1", "attributeName1", "inputAssetId1", "outputAssetId1"],
        ["AssetName2", "attributeName2", "inputAssetId2", "outputAssetId2"],
        ["AssetName3", "attributeName3", "inputAssetId3", "outputAssetId3"]
]

def attributeNames = attributes.collect { it[1] }.unique() as String[]
def inputAssetIds = attributes.collect { it[2] } as String[]
def inputOutputAssetIdsMap = attributes.collectEntries { [(it[2]), it[3]] } as HashMap<String, String>

rules.add()
        .name("Link attributes rule")
        .when({ facts ->

            // Find attribute changes
            List<AttributeInfo> changes = facts
                    .matchAssetState(
                            new AssetQuery()
                                    .ids(inputAssetIds)
                                    .attributeNames(attributeNames)
                    )
                    .filter { attributeInfo ->
                        boolean timestampChanged = false

                        // Get previous attribute state from facts
                        Optional<AttributeInfo> previous = facts.matchFirst(attributeInfo.id + attributeInfo.name)

                        // Check if attribute timestamp has been updated (attribute value can be the same)
                        if (attributeInfo.timestamp > previous.map { it.timestamp }.orElse(0)) {
                            timestampChanged = true
                        }
                        return timestampChanged
                    }
                    .toList()

            // Bind attribute info for the then trigger
            if (!changes.isEmpty()) {
                facts.bind("changes", changes)
            }

            // Trigger rule if there are changes to process
            return !changes.isEmpty()
        })
        .then({ facts ->
            def changes = facts.bound("changes") as List<AttributeInfo>

            // Create fact for each attribute change
            if (changes != null) {
                changes.forEach { attributeInfo -> facts.put(attributeInfo.id + attributeInfo.name, attributeInfo as Object) }
            }

            // Update linked output attributes
            changes.forEach {
                String outputAssetId = inputOutputAssetIdsMap[it.id]
                String attributeName = it.name
                String value = it.value.orElse(null)

                // Update attribute
                assets.dispatch(outputAssetId, attributeName, value)
            }
        })
