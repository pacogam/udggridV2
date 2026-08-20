/*
 * Copyright 2026, OpenRemote Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
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

Assets assets = binding.assets
RulesBuilder rules = binding.rules
Logger LOG = binding.LOG


// -------------------------input------------------------- //

// Set the asset ID's:
String meterSumAssetId = "setId1"
String challengesAssetId = "setId2"

// ------------------------------------------------------- //


// Triggers for rules
boolean chargingTriggered = false
int previousHourRule1 = -1
String challengeGeneralStatusPreviousRule2 = ""
String challengeGeneralStatusPreviousRule3 = ""

ZoneId zone = ZoneId.of("Europe/Amsterdam")
def sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss") as SimpleDateFormat

// Time triggers for rules
long rulesStartTimeMillis = System.currentTimeMillis()

// Set the [triggerPeriodMillis, triggerDelayMillis]:
long[] triggerInputsRule4 = [1 * 60 * 1000, 1 * 60 * 1000]
long triggerTimeMillisRule4 = calculateNextTriggerTime(rulesStartTimeMillis, triggerInputsRule4)

private static long calculateNextTriggerTime(long timeMillis, long[] triggerInputs) {
    long triggerPeriodMillis = triggerInputs[0]
    long triggerDelayMillis = triggerInputs[1]

    return timeMillis - timeMillis % triggerPeriodMillis + triggerPeriodMillis + triggerDelayMillis
}

rules.add()
        .priority(1)
        .name("OurGrid battery start charging rule")
        .when({ facts ->
            boolean triggerRule = false
            int currentHour = ZonedDateTime.ofInstant(facts.getClock().getNow(), zone).toLocalDateTime().getHour()

            // Trigger rule at 2:00 and 13:00
            if ((previousHourRule1 == 1 && currentHour == 2) || (previousHourRule1 == 12 && currentHour == 13)) {
                triggerRule = true
            }

            previousHourRule1 = currentHour

            return triggerRule
        })
        .then({ facts ->
            def attributeNames = [
                    OurgridBatteryAsset.ALLOW_AUTOMATIC_CONTROL_BUTTON.name,
                    OurgridBatteryAsset.ENERGY_LEVEL_PERCENTAGE.name,
                    OurgridBatteryAsset.ENERGY_LEVEL_PERCENTAGE_MAX.name,
                    OurgridBatteryAsset.POWER_IMPORT_MAX.name,
                    OurgridBatteryAsset.POWER_SETPOINT.name
            ] as String[]

            def batteriesAttributes = getBatteriesAttributes(meterSumAssetId, attributeNames, facts) as Map<String, Map<String, Object>>

            batteriesAttributes.each {
                def assetId = it.key as String
                def allowAutomaticControlButton = it.value.getOrDefault(OurgridBatteryAsset.ALLOW_AUTOMATIC_CONTROL_BUTTON.name, null) as Boolean
                def energyLevelPercentage = it.value.getOrDefault(OurgridBatteryAsset.ENERGY_LEVEL_PERCENTAGE.name, null) as Double
                def energyLevelPercentageMax = it.value.getOrDefault(OurgridBatteryAsset.ENERGY_LEVEL_PERCENTAGE_MAX.name, null) as Double
                def powerImportMax = it.value.getOrDefault(OurgridBatteryAsset.POWER_IMPORT_MAX.name, null) as Double
                def powerSetpoint = it.value.getOrDefault(OurgridBatteryAsset.POWER_SETPOINT.name, 0.0) as double

                if (allowAutomaticControlButton == true) {
                    if (energyLevelPercentage == null || energyLevelPercentageMax == null || powerImportMax == null || energyLevelPercentage >= energyLevelPercentageMax) {
                        powerImportMax = 0.0
                    }

                    if (powerSetpoint != powerImportMax) {
                        assets.dispatch(assetId, OurgridBatteryAsset.POWER_SETPOINT.name, powerImportMax)
                        chargingTriggered = true
                    }
                }
            }
        })

rules.add()
        .priority(2)
        .name("OurGrid battery start of challenge rule")
        .when(facts -> {
            boolean triggerRule = false

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
        .then(facts -> {
            def challengeStart = facts
                    .matchFirstAssetState(new AssetQuery().ids(challengesAssetId).attributeName(OurgridChallengesAsset.CHALLENGE_START.name))
                    .flatMap { it.value }
                    .orElse(null) as String

            def challengeEnd = facts
                    .matchFirstAssetState(new AssetQuery().ids(challengesAssetId).attributeName(OurgridChallengesAsset.CHALLENGE_END.name))
                    .flatMap { it.value }
                    .orElse(null) as String

            long currentTimeMillis = facts.clock.currentTimeMillis
            long challengeStartMillis = sdf.parse(challengeStart).getTime()
            long challengeEndMillis = sdf.parse(challengeEnd).getTime()

            def attributeNames = [
                    OurgridBatteryAsset.ALLOW_AUTOMATIC_CONTROL_BUTTON.name,
                    OurgridBatteryAsset.ENERGY_CAPACITY.name,
                    OurgridBatteryAsset.ENERGY_LEVEL_PERCENTAGE.name,
                    OurgridBatteryAsset.ENERGY_LEVEL_PERCENTAGE_MAX.name,
                    OurgridBatteryAsset.ENERGY_LEVEL_PERCENTAGE_MIN.name,
                    OurgridBatteryAsset.POWER_EXPORT_MAX.name,
                    OurgridBatteryAsset.POWER_SETPOINT.name
            ] as String[]

            def batteriesAttributes = getBatteriesAttributes(meterSumAssetId, attributeNames, facts) as Map<String, Map<String, Object>>

            batteriesAttributes.each {
                def assetId = it.key as String
                def allowAutomaticControlButton = it.value.get(OurgridBatteryAsset.ALLOW_AUTOMATIC_CONTROL_BUTTON.name) as Boolean
                def energyCapacity = it.value.get(OurgridBatteryAsset.ENERGY_CAPACITY.name) as Double
                def energyLevelPercentage = it.value.get(OurgridBatteryAsset.ENERGY_LEVEL_PERCENTAGE.name) as Double
                def energyLevelPercentageMax = it.value.get(OurgridBatteryAsset.ENERGY_LEVEL_PERCENTAGE_MAX.name) as Double
                def energyLevelPercentageMin = it.value.get(OurgridBatteryAsset.ENERGY_LEVEL_PERCENTAGE_MIN.name) as Double
                def powerExportMax = it.value.get(OurgridBatteryAsset.POWER_EXPORT_MAX.name) as Double
                def powerSetpoint = it.value.get(OurgridBatteryAsset.POWER_SETPOINT.name) as Double

                if (allowAutomaticControlButton == true) {
                    boolean allowDischargingButtonNew = true
                    double powerSetpointNew = calculatePowerSetpoint(energyCapacity, energyLevelPercentage, energyLevelPercentageMax, energyLevelPercentageMin, powerExportMax, powerSetpoint, allowDischargingButtonNew, currentTimeMillis, challengeStartMillis, challengeEndMillis)
                    updatePowerSetpoint(assets, assetId, allowAutomaticControlButton, allowDischargingButtonNew, powerSetpoint, powerSetpointNew)
                    assets.dispatch(assetId, OurgridBatteryAsset.ALLOW_DISCHARGING_BUTTON.name, allowDischargingButtonNew)
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
                    OurgridBatteryAsset.ALLOW_AUTOMATIC_CONTROL_BUTTON.name,
                    OurgridBatteryAsset.ALLOW_DISCHARGING_BUTTON.name,
                    OurgridBatteryAsset.POWER_SETPOINT.name
            ] as String[]

            def batteriesAttributes = getBatteriesAttributes(meterSumAssetId, attributeNames, facts) as Map<String, Map<String, Object>>

            batteriesAttributes.each {
                def assetId = it.key as String
                def allowAutomaticControlButton = it.value.get(OurgridBatteryAsset.ALLOW_AUTOMATIC_CONTROL_BUTTON.name) as Boolean
                def allowDischargingButton = it.value.get(OurgridBatteryAsset.ALLOW_DISCHARGING_BUTTON.name) as Boolean
                def powerSetpoint = it.value.get(OurgridBatteryAsset.POWER_SETPOINT.name) as Double

                if (allowDischargingButton == true) {
                    updatePowerSetpoint(assets, assetId, allowAutomaticControlButton, allowDischargingButton, powerSetpoint, 0.0)
                    assets.dispatch(assetId, OurgridBatteryAsset.ALLOW_DISCHARGING_BUTTON.name, false)
                }
            }
        })

rules.add()
        .priority(4)
        .name("OurGrid battery control set-point rule")
        .when({ facts ->
            long currentTimeMillis = facts.clock.currentTimeMillis

            // Trigger rule on a time-based interval
            if (currentTimeMillis > triggerTimeMillisRule4) {
                triggerTimeMillisRule1 = calculateNextTriggerTime(currentTimeMillis, triggerInputsRule4)
                return true
            }

            return false
        })
        .then({ facts ->
            // Charging triggered, skip set-point calculation for this cycle
            if (chargingTriggered) {
                chargingTriggered = false
                return
            }

            def challengeStart = facts
                    .matchFirstAssetState(new AssetQuery().ids(challengesAssetId).attributeName(OurgridChallengesAsset.CHALLENGE_START.name))
                    .flatMap { it.value }
                    .orElse(null) as String

            def challengeEnd = facts
                    .matchFirstAssetState(new AssetQuery().ids(challengesAssetId).attributeName(OurgridChallengesAsset.CHALLENGE_END.name))
                    .flatMap { it.value }
                    .orElse(null) as String

            long currentTimeMillis = facts.clock.currentTimeMillis
            long challengeStartMillis = sdf.parse(challengeStart).getTime()
            long challengeEndMillis = sdf.parse(challengeEnd).getTime()

            def attributeNames = [
                    OurgridBatteryAsset.ALLOW_AUTOMATIC_CONTROL_BUTTON.name,
                    OurgridBatteryAsset.ALLOW_DISCHARGING_BUTTON.name,
                    OurgridBatteryAsset.ENERGY_CAPACITY.name,
                    OurgridBatteryAsset.ENERGY_LEVEL_PERCENTAGE.name,
                    OurgridBatteryAsset.ENERGY_LEVEL_PERCENTAGE_MAX.name,
                    OurgridBatteryAsset.ENERGY_LEVEL_PERCENTAGE_MIN.name,
                    OurgridBatteryAsset.POWER_EXPORT_MAX.name,
                    OurgridBatteryAsset.POWER_SETPOINT.name
            ] as String[]

            def batteriesAttributes = getBatteriesAttributes(meterSumAssetId, attributeNames, facts) as Map<String, Map<String, Object>>

            batteriesAttributes.each {
                def assetId = it.key as String
                def allowAutomaticControlButton = it.value.get(OurgridBatteryAsset.ALLOW_AUTOMATIC_CONTROL_BUTTON.name) as Boolean
                def allowDischargingButton = it.value.getOrDefault(OurgridBatteryAsset.ALLOW_DISCHARGING_BUTTON.name, false) as Boolean
                def energyCapacity = it.value.getOrDefault(OurgridBatteryAsset.ENERGY_CAPACITY.name, null) as Double
                def energyLevelPercentage = it.value.getOrDefault(OurgridBatteryAsset.ENERGY_LEVEL_PERCENTAGE.name, null) as Double
                def energyLevelPercentageMax = it.value.getOrDefault(OurgridBatteryAsset.ENERGY_LEVEL_PERCENTAGE_MAX.name, null) as Double
                def energyLevelPercentageMin = it.value.getOrDefault(OurgridBatteryAsset.ENERGY_LEVEL_PERCENTAGE_MIN.name, null) as Double
                def powerExportMax = it.value.getOrDefault(OurgridBatteryAsset.POWER_EXPORT_MAX.name, null) as Double
                def powerSetpoint = it.value.getOrDefault(OurgridBatteryAsset.POWER_SETPOINT.name, null) as Double

                double powerSetpointNew = calculatePowerSetpoint(energyCapacity, energyLevelPercentage, energyLevelPercentageMax, energyLevelPercentageMin, powerExportMax, powerSetpoint, allowDischargingButton, currentTimeMillis, challengeStartMillis, challengeEndMillis)
                updatePowerSetpoint(assets, assetId, allowAutomaticControlButton, allowDischargingButton, powerSetpoint, powerSetpointNew)
            }
        })

rules.add()
        .priority(5)
        .name("OurGrid battery disable automatic control rule")
        .when({ facts ->

            // Get household meter asset ID's
            def meterAssetIds = assets.getResults(new AssetQuery().parents(meterSumAssetId).types(OurgridMeterAsset)).map { it.id }.toList() as String[]

            AssetQuery assetQuery = new AssetQuery().parents(meterAssetIds).types(OurgridBatteryAsset).attributeNames(OurgridBatteryAsset.ALLOW_AUTOMATIC_CONTROL_BUTTON.name)

            // Find attribute changes
            List<AttributeInfo> attributeChanges = facts
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

            attributeChanges.forEach { attributeInfo ->
                String assetId = attributeInfo.id
                Boolean value = attributeInfo.value.orElse(null)

                if (value == false) {
                    // Update attribute
                    assets.dispatch(assetId, OurgridBatteryAsset.POWER_SETPOINT.name, 0.0)
                }
            }
        })


private static double calculatePowerSetpoint(Double energyCapacity, Double energyLevelPercentage, double energyLevelPercentageMax, double energyLevelPercentageMin, Double powerExportMax, Double powerSetpoint, Boolean allowDischargingButton, long currentTimeMillis, long challengeStartMillis, long challengeEndMillis) {
    double powerSetpointDefault = 0.0

    if (energyCapacity == null || energyLevelPercentage == null || powerExportMax == null) {
        return powerSetpointDefault
    }

    if (powerSetpoint < 0.0 && allowDischargingButton == false) {
        return powerSetpointDefault
    }

    if (powerSetpoint < 0.0 && energyLevelPercentage <= energyLevelPercentageMin) {
        return powerSetpointDefault
    }

    if (powerSetpoint > 0.0 && energyLevelPercentage >= energyLevelPercentageMax) {
        return powerSetpointDefault
    }

    double powerSetpointNew = powerSetpoint

    if (allowDischargingButton == true && currentTimeMillis >= challengeStartMillis && currentTimeMillis < challengeEndMillis) {
        def challengeDurationHours = (challengeEndMillis - currentTimeMillis) / 3600000 as Double
        def energyCapacityUsable = energyCapacity * (energyLevelPercentage - energyLevelPercentageMin) / 100 as Double

        if (energyCapacityUsable > 0.0) {
            powerSetpointNew = Math.round(-1000 * energyCapacityUsable / challengeDurationHours) / 1000

            if (powerSetpointNew < -powerExportMax) {
                powerSetpointNew = -powerExportMax
            }
        }
    }

    return powerSetpointNew
}

private static void updatePowerSetpoint(Assets assets, String assetId, Boolean allowAutomaticControlButton, Boolean allowDischargingButton, Double powerSetpoint, double powerSetpointNew) {
    if (allowAutomaticControlButton == false && allowDischargingButton == false) {
        return
    }

    if (powerSetpoint == null || powerSetpoint != powerSetpointNew) {
        assets.dispatch(assetId, OurgridBatteryAsset.POWER_SETPOINT.name, powerSetpointNew)
    }
}

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
