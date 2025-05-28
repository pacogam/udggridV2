package ourgrid.rules

import org.openremote.agent.custom.ourgrid.OurgridBatteryAsset
import org.openremote.agent.custom.ourgrid.OurgridMeterAsset
import org.openremote.manager.rules.RulesBuilder
import org.openremote.manager.rules.RulesFacts
import org.openremote.model.asset.Asset
import org.openremote.model.attribute.AttributeInfo
import org.openremote.model.query.AssetQuery
import org.openremote.model.rules.Assets

import java.time.Clock
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.logging.Logger

Logger LOG = binding.LOG
RulesBuilder rules = binding.rules
Assets assets = binding.assets

// Put the asset ID of relevant assets here:
String meterSumAssetId = "setId1"


// Time triggers for rules
ZoneId zone = ZoneId.of("Europe/Amsterdam")
LocalDateTime previousTimestampRule1 = LocalDateTime.ofInstant(Clock.system(zone).instant(), zone)
long previousMillisRule2 = System.currentTimeMillis() - System.currentTimeMillis() % (1 * 60 * 1000) + (1 * 60 * 1000)

rules.add()
        .priority(1)
        .name("OurGrid battery charging rule")
        .when({ facts ->
            def currentTimestamp = ZonedDateTime.ofInstant(facts.getClock().getNow(), zone).toLocalDateTime() as LocalDateTime

            // Trigger rule at 2:00am and 1:00pm
            if ((previousTimestampRule1.getHour() <= 1 && currentTimestamp.getHour() == 2) || (previousTimestampRule1.getHour() <= 12 && currentTimestamp.getHour() == 13)) {
                previousTimestampRule1 = currentTimestamp
                return true
            }

            return false
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
        .name("OurGrid battery stop charging rule")
        .when({ facts ->
            boolean triggerRule = false
            long currentMillis = facts.clock.currentTimeMillis

            // Trigger rule every 1 minute = 60000 ms
            if (currentMillis > previousMillisRule2) {
                previousMillisRule2 += 60000
                triggerRule = true
            }

            return triggerRule
        })
        .then({ facts ->
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

                if (powerSetpoint == null || energyLevelPercentage == null || energyLevelPercentageMax == null || (powerSetpoint > 0.0 && energyLevelPercentage >= energyLevelPercentageMax)) {
                    assets.dispatch(assetId, OurgridBatteryAsset.POWER_SETPOINT.name, 0.0)
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