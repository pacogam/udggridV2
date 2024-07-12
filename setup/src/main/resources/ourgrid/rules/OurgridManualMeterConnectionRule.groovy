package ourgrid.rules

import org.openremote.manager.rules.RulesBuilder
import org.openremote.model.attribute.AttributeInfo
import org.openremote.model.query.AssetQuery
import org.openremote.model.rules.Assets

import java.util.logging.Logger

Logger LOG = binding.LOG
RulesBuilder rules = binding.rules
Assets assets = binding.assets

// Put the "input power meter name", "input power meter Asset ID" and "output OurGrid meter Asset ID" here:
def powerMeters = [
        ["Meter 1", "uniqueInputId1", "uniqueOutputId1"],
        ["Meter 2", "uniqueInputId2", "uniqueOutputId2"],
        ["Meter 3", "uniqueInputId3", "uniqueOutputId3"]
]

def inputAssetIds = powerMeters.collect { it[1] } as String[]
def inputOutputAssetIdsMap = powerMeters.collectEntries { [(it[1]), it[2]] } as HashMap<String, String>

rules.add()
        .name("Power meter to OurGrid meter connection")
        .when({ facts ->

            // Find attribute changes
            List<AttributeInfo> changes = facts
                    .matchAssetState(
                            new AssetQuery()
                                    .ids(inputAssetIds)
                                    .attributeName("power")
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

            // Bind assetStates for the then trigger
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

            // Collect input Asset ID's and power values from changes
            def inputAssetIdsValuesMap = changes.collectEntries { [(it.id), it.value.orElse(null)] } as HashMap<String, Double>

            // Create a new HashMap linking output ID's to the corresponding power values
            def outputAssetIdsValuesMap = inputAssetIdsValuesMap.collectEntries { inputId, value ->
                String outputId = inputOutputAssetIdsMap[inputId]
                Double valueWatt = null

                // Convert power value from kiloWatt to Watt
                if (value != null) {
                    valueWatt = (1000 * (value as Double)).round()
                }
                [(outputId), valueWatt]
            } as HashMap<String, Double>

            // Update OurGrid Meter: power attribute
            outputAssetIdsValuesMap.forEach { outputId, value ->
                assets.dispatch(outputId, "power", value)
            }
        })
