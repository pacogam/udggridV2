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

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.openremote.agent.custom.ourgrid.OurgridChallengesAsset
import org.openremote.agent.custom.ourgrid.OurgridMeterAsset
import org.openremote.agent.custom.ourgrid.OurgridPeaksAsset
import org.openremote.container.persistence.PersistenceService
import org.openremote.manager.rules.RulesBuilder
import org.openremote.manager.rules.RulesFacts
import org.openremote.model.attribute.AttributeInfo
import org.openremote.model.geo.GeoJSONPoint
import org.openremote.model.query.AssetQuery
import org.openremote.model.rules.Assets
import org.openremote.model.util.ValueUtil
import org.postgresql.util.PGobject

import java.sql.*
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.Date
import java.util.logging.Logger

Assets assets = binding.assets
RulesBuilder rules = binding.rules
Logger LOG = binding.LOG

// -------------------------input------------------------- //

// Set the asset ID of relevant assets here:
String parentDistrictAssetId = "setId1"
String parentMeterAssetId = "setId2"
String solarAssetId = "setId3"
String researchAsset1Id = "setId4"
String challengesAssetId = "setId5"
String peaksAssetId = "setId6"

// Set the attribute names for summation here (or leave empty to include all child asset attribute names with 'Rule state'):
String[] attributeNames = ["power", "energyImportTotal", "energyExportTotal", "energyNetTotal", "gasImportTotal", "gasFlowRate"]


// Set 'true' if you want district power to be calculated by OurGrid, set 'false' if you want to connect district power attributes manually:
boolean calculatePowerNetDistrict = true
boolean calculatePowerNetDistrictForecast = true
boolean calculatePowerConsumptionDistrict = true

// ------------------------------------------------------- //

// Time triggers for rules
long rulesStartTimeMillis = System.currentTimeMillis()
long previousTimeMillisRule1 = rulesStartTimeMillis - rulesStartTimeMillis % (1 * 60 * 1000) + (1 * 60 * 1000)
long previousTimeMillisRule2 = rulesStartTimeMillis - rulesStartTimeMillis % (5 * 60 * 1000) + (5 * 60 * 1000)
long previousTimeMillisRule3 = rulesStartTimeMillis - rulesStartTimeMillis % (1 * 60 * 1000) + (1 * 60 * 1000)

// Date triggers for rules
SimpleDateFormat dateOnlyFormat = new SimpleDateFormat("yyyy-MM-dd")
String previousDateRule5 = dateOnlyFormat.format(new Date())
String previousDateRule6 = dateOnlyFormat.format(new Date())
String previousDateRule8 = dateOnlyFormat.format(new Date())

// Forecast variables
TreeMap<String, Double> previousDatapoints = new TreeMap<>()
TreeMap<String, Double> consumptionForecastHistorical = new TreeMap<>()

// Challenges variables
boolean challengeActive = false
boolean challengeActivePrevious = false
long challengeStartMillis = 0
long challengeEndMillis = 0
Map<String, Integer> challengePointsMap = new HashMap<>()

// Peak points variables
boolean turnOnPeakPointsPrevious = false

rules.add()
        .priority(1)
        .name("Group summation rule")
        .when({ facts ->
            long currentTimeMillis = facts.clock.currentTimeMillis

            // Trigger rule every 1 minute
            if (currentTimeMillis > previousTimeMillisRule1) {
                previousTimeMillisRule1 += (1 * 60 * 1000)
            } else {
                return false
            }

            // Check if parent asset ID is valid
            Optional<AttributeInfo> parent = facts.matchFirstAssetState(new AssetQuery().ids(parentMeterAssetId))

            if (parent.isEmpty()) {
                LOG.warning("No Parent Asset found with ID: '" + parentMeterAssetId + "'; Check Parent Asset ID and if the 'Rule state' configuration item is added to the attribute")
                return false
            }

            // Find attribute changes in group
            List<AttributeInfo> changes = facts
                    .matchAssetState(
                            new AssetQuery()
                                    .parents(parentMeterAssetId)
                                    .types(OurgridMeterAsset)
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

            // Get attribute names of changes
            String[] changesAttributeNames = changes
                    .collect { it.name }
                    .unique()

            // Find all relevant children attributes
            List<AttributeInfo> children = Collections.synchronizedList(new ArrayList<>())

            if (attributeNames.length > 0) {
                children = facts.matchAssetState(
                        new AssetQuery()
                                .parents(parentMeterAssetId)
                                .attributeNames(attributeNames)
                ).toList()
            }

            // Bind attribute info for the then trigger
            facts.bind("changesAttributeNames", changesAttributeNames)

            if (!changes.isEmpty()) {
                facts.bind("changes", changes)
            }

            if (!children.isEmpty()) {
                facts.bind("children", children)
            }

            // Trigger rule
            return true
        })
        .then({ facts ->
            String[] changesAttributeNames = facts.bound("changesAttributeNames")
            def changes = facts.bound("changes") as List<AttributeInfo>
            def children = facts.bound("children") as List<AttributeInfo>

            // Create fact for each attribute change
            if (changes != null) {
                changes.forEach { attributeInfo -> facts.put(attributeInfo.id + attributeInfo.name, attributeInfo as Object) }
            }

            // Calculate number of devices
            def numberOfDevices = children
                    .findAll { it.name == "power" }
                    .size()

            // Get previous number of devices from district parent
            def numberOfDevicesPrevious = facts.matchFirstAssetState(
                    new AssetQuery()
                            .ids(parentDistrictAssetId)
                            .attributeName("numberOfDevices")
            ).flatMap { it.value }.orElse(null)

            // Update district parent - number of devices attribute
            if (numberOfDevices != numberOfDevicesPrevious) {
                assets.dispatch(parentDistrictAssetId, "numberOfDevices", numberOfDevices)
            }

            // Get active period
            def activePeriodMinutes = facts.matchFirstAssetState(
                    new AssetQuery()
                            .ids(parentDistrictAssetId)
                            .attributeName("activePeriod")
            ).flatMap { it.value }.orElse(null)

            if (activePeriodMinutes == null) {
                activePeriodMinutes = 5
                assets.dispatch(parentDistrictAssetId, "activePeriod", activePeriodMinutes)
            }

            long activePeriodMillis = activePeriodMinutes * 60000
            long currentTimestamp = facts.clock.currentTimeMillis

            // Find all active children
            def childrenActive = children
                    .findAll { (currentTimestamp - it.timestamp) < activePeriodMillis }

            // Find all active power readings
            def activePowerReadingsMap = childrenActive
                    .findAll { it.name == "power" }
                    .findAll { it.value.isPresent() }
                    .collectEntries { [it.id, it.value.get()] }

            // Calculate number of active power readings
            def numberOfActivePowerReadings = activePowerReadingsMap.size()

            // Get previous number of active devices from district parent
            def numberOfActivePowerReadingsPrevious = facts.matchFirstAssetState(
                    new AssetQuery()
                            .ids(parentDistrictAssetId)
                            .attributeName("numberOfActivePowerReadings")
            ).flatMap { it.value }.orElse(null)

            // Update district parent - number of active devices attribute
            if (numberOfActivePowerReadings != numberOfActivePowerReadingsPrevious) {
                assets.dispatch(parentDistrictAssetId, "numberOfActivePowerReadings", numberOfActivePowerReadings)
            }

            // Stop here when group is empty
            if (children == null) {
                return
            }

            Double netPowerMetersWatt = null

            // Sum values per attribute
            for (attributeName in changesAttributeNames) {
                List<AttributeInfo> childrenFiltered = children

                // Attributes that need custom processing
                if (attributeName == "gasFlowRate" || attributeName == "power") {
                    childrenFiltered = childrenActive
                }

                def sum = childrenFiltered
                        .findAll { it.name == attributeName }
                        .findAll { it.value.isPresent() }
                        .collect { it.value.get() }
                        .sum()

                // Update parent
                if (sum != null) {
                    if (attributeName == "power") {
                        netPowerMetersWatt = (sum as Double).round()
                        assets.dispatch(parentMeterAssetId, attributeName, netPowerMetersWatt)
                    } else {
                        sum = ((sum as Double) * 1000).round() / 1000
                        assets.dispatch(parentMeterAssetId, attributeName, sum)
                    }
                }
            }

            // Asset ID's of active power readings
            def activePowerReadingsIds = activePowerReadingsMap.keySet().toList() as String[]

            // Asset ID's of active power reading with automatic solar capacity estimation
            def automaticSolarCapacityEstimationIds = new HashSet(facts
                    .matchAssetState(new AssetQuery()
                            .ids(activePowerReadingsIds)
                            .attributeName("estimateSolarCapacityManually"))
                    .toList()
                    .findAll { it.value.orElse(false) == false }
                    .collect { it.id })

            // Get power minimum of all active children
            def powerMinimumMap = facts
                    .matchAssetState(new AssetQuery()
                            .ids(activePowerReadingsIds)
                            .attributeName("powerMinimum"))
                    .toList()
                    .collectEntries { [it.id, it.value.orElse(null)] }

            // Get estimated solar capacity of all active children
            def estimatedSolarCapacityMap = facts
                    .matchAssetState(new AssetQuery()
                            .ids(activePowerReadingsIds)
                            .attributeName("estimatedSolarCapacity"))
                    .toList()
                    .collectEntries { [it.id, it.value.orElse(null)] }

            // Get solar asset location
            def solarAssetLocation = facts.matchFirstAssetState(
                    new AssetQuery()
                            .ids(solarAssetId)
                            .attributeName("location")
            ).flatMap { it.value }.orElse(null) as GeoJSONPoint

            def solarIrradiance = null as Double

            if (solarAssetLocation != null) {
                // Calculate maximum solar irradiance for given day and location
                def latitude = solarAssetLocation.getY() as double
                def longitude = solarAssetLocation.getX() as double

                solarIrradiance = calculateSolarIrradiance(currentTimestamp, latitude, longitude)

                activePowerReadingsMap.each { entry ->
                    def assetId = entry.key as String
                    def power = (entry.value as Double).round() as Double
                    def powerMinimum = powerMinimumMap.get(assetId) as Double

                    if (powerMinimum == null || power <= powerMinimum) {
                        assets.dispatch(assetId, "powerMinimum", power)

                        // Calculate estimated solar capacity
                        if (automaticSolarCapacityEstimationIds.contains(assetId)) {
                            def estimatedSolarCapacityPrevious = estimatedSolarCapacityMap.get(assetId) as Double

                            if (power < 0) {
                                def solarIrradianceFactor = 1.0 as Double

                                // Standard testing conditions for solar panels = 1000 Watt/m2
                                if (solarIrradiance > 0 && solarIrradiance < 1000) {
                                    solarIrradianceFactor = 1000 / solarIrradiance as Double
                                }

                                def estimatedSolarCapacity = (solarIrradianceFactor * power / 100).round() / -10

                                estimatedSolarCapacityMap.put(assetId, estimatedSolarCapacity)
                                assets.dispatch(assetId, "estimatedSolarCapacity", estimatedSolarCapacity)
                            } else if (power >= 0 && estimatedSolarCapacityPrevious == null) {
                                assets.dispatch(assetId, "estimatedSolarCapacity", 0.0)
                            }
                        }
                    }
                }
            }

            // Calculate the total estimated solar capacity of active meters
            def estimatedSolarCapacitySum = estimatedSolarCapacityMap.values().findAll { it != null }.sum()
            def estimatedSolarCapacityMeters = null

            if (estimatedSolarCapacitySum != null) {
                estimatedSolarCapacityMeters = (estimatedSolarCapacitySum * 1000).round() / 1000 as Double
            }
            // Update meter parent - estimated solar capacity attribute
            if (estimatedSolarCapacityMeters != null) {
                assets.dispatch(parentMeterAssetId, "estimatedSolarCapacity", estimatedSolarCapacityMeters)
            }

            // Get number of households from district parent
            Integer numberOfHouseholds = facts.matchFirstAssetState(
                    new AssetQuery()
                            .ids(parentDistrictAssetId)
                            .attributeName("numberOfHouseholds")
            ).flatMap { it.value }.orElse(null) as Integer

            // Get previous power correction factor from district parent
            Double powerCorrectionFactorPrevious = facts.matchFirstAssetState(
                    new AssetQuery()
                            .ids(parentDistrictAssetId)
                            .attributeName("powerCorrectionFactor")
            ).flatMap { it.value }.orElse(null) as Double

            // Get solar power from district parent
            Double solarPowerDistrict = facts.matchFirstAssetState(
                    new AssetQuery()
                            .ids(parentDistrictAssetId)
                            .attributeName("powerSolarDistrict")
            ).flatMap { it.value }.orElse(null) as Double

            // Get power import max from district parent
            Double powerImportMax = facts.matchFirstAssetState(
                    new AssetQuery()
                            .ids(parentDistrictAssetId)
                            .attributeName("powerImportMax")
            ).flatMap { it.value }.orElse(null) as Double

            // Get dynamic estimated solar capacity from district parent
            Double estimatedSolarCapacityDistrictPrevious = facts.matchFirstAssetState(
                    new AssetQuery()
                            .ids(parentDistrictAssetId)
                            .attributeName("estimatedSolarCapacityDistrict")
            ).flatMap { it.value }.orElse(null) as Double

            // Get static solar capacity from solar asset
            Double solarCapacitySolarAsset = facts.matchFirstAssetState(
                    new AssetQuery()
                            .ids(solarAssetId)
                            .attributeName("powerExportMax")
            ).flatMap { it.value }.orElse(null) as Double

            // Get turn on dynamic solar capacity from district parent
            Boolean turnOnDynamicSolarCapacity = facts.matchFirstAssetState(
                    new AssetQuery()
                            .ids(parentDistrictAssetId)
                            .attributeName("turnOnDynamicSolarCapacity")
            ).flatMap { it.value }.orElse(false) as Boolean

            // Update district parent
            if (netPowerMetersWatt != null && numberOfHouseholds != null && numberOfActivePowerReadings > 0) {
                def powerCorrectionFactor = 1.0 as Double
                def consumptionPowerDistrict
                def netPowerDistrict

                if (calculatePowerNetDistrict) {
                    powerCorrectionFactor = numberOfHouseholds / numberOfActivePowerReadings as Double
                    netPowerDistrict = (powerCorrectionFactor * netPowerMetersWatt).round() / 1000 as Double
                    consumptionPowerDistrict = netPowerDistrict

                    // Update district parent - net power attribute
                    assets.dispatch(parentDistrictAssetId, "powerDistrict", netPowerDistrict)
                } else {
                    netPowerDistrict = facts
                            .matchFirstAssetState(new AssetQuery().ids(parentDistrictAssetId).attributeName("powerDistrict"))
                            .flatMap { it.value }
                            .orElse(null) as Double

                    consumptionPowerDistrict = netPowerDistrict
                }

                // Update district parent - power import percentage attribute
                if (powerImportMax != null && netPowerDistrict != null) {
                    def netPowerDistrictPercentage = (netPowerDistrict / powerImportMax * 100).round() as Integer
                    assets.dispatch(parentDistrictAssetId, "powerImportPercentage", netPowerDistrictPercentage)
                }

                // Update district parent - power correction factor attribute
                if (powerCorrectionFactor != powerCorrectionFactorPrevious) {
                    assets.dispatch(parentDistrictAssetId, "powerCorrectionFactor", powerCorrectionFactor)
                }

                if (solarPowerDistrict != null && solarCapacitySolarAsset != null && solarCapacitySolarAsset > 0) {
                    // Calculate static solar power production
                    def solarPowerMetersWatt = (solarPowerDistrict * 1000) / powerCorrectionFactor as Double

                    // Calculate dynamic solar power production
                    if (turnOnDynamicSolarCapacity && solarAssetLocation != null && estimatedSolarCapacityMeters != null) {
                        solarPowerMetersWatt = (solarPowerDistrict * 1000) * (estimatedSolarCapacityMeters / solarCapacitySolarAsset)

                        // Update district parent - estimated solar capacity attribute
                        def estimatedSolarCapacityDistrict = (powerCorrectionFactor * estimatedSolarCapacityMeters * 1000).round() / 1000 as Double

                        if (estimatedSolarCapacityDistrict != estimatedSolarCapacityDistrictPrevious) {
                            assets.dispatch(parentDistrictAssetId, "estimatedSolarCapacityDistrict", estimatedSolarCapacityDistrict)
                        }
                    }

                    consumptionPowerDistrict = (powerCorrectionFactor * (netPowerMetersWatt - solarPowerMetersWatt)).round() / 1000
                }

                // Update district parent - power consumption attribute
                if (calculatePowerConsumptionDistrict && consumptionPowerDistrict != null) {
                    assets.dispatch(parentDistrictAssetId, "powerConsumptionDistrict", consumptionPowerDistrict)
                }
            }
        })

rules.add()
        .priority(2)
        .name("Forecast rule")
        .when({ facts ->
            long currentTimeMillis = facts.clock.currentTimeMillis

            // Trigger rule every 5 minutes
            if (currentTimeMillis > previousTimeMillisRule2) {
                previousTimeMillisRule2 += (5 * 60 * 1000)
                return true
            }

            return false
        })
        .then({ facts ->
            long currentTimeMillis = facts.clock.currentTimeMillis

            // Calculate 'net power forecast' from 'solar power forecast' & 'consumption power forecast'
            if (calculatePowerNetDistrictForecast) {
                Object[] result = calculateNetPowerForecast(currentTimeMillis, solarAssetId, parentDistrictAssetId, previousDatapoints, consumptionForecastHistorical)
                previousDatapoints = (TreeMap<String, Double>) result[0]
                consumptionForecastHistorical = (TreeMap<String, Double>) result[1]
            }

            // Interpolate solar power value from solar power forecast
            Double interpolatedSolarValue = calculateForecastValue(currentTimeMillis, solarAssetId, "powerForecast")

            // Interpolate net power value from net power forecast
            Double interpolatedNetPowerValue = calculateForecastValue(currentTimeMillis, parentDistrictAssetId, "powerDistrict")

            // Update district parent - solar power attribute
            if (interpolatedSolarValue != null) {
                interpolatedSolarValue = (interpolatedSolarValue * 1000).round() / 1000
                assets.dispatch(parentDistrictAssetId, "powerSolarDistrict", interpolatedSolarValue)
            }

            // Update research asset 1 - net power forecast attribute
            if (interpolatedNetPowerValue != null && researchAsset1Id != "") {
                interpolatedNetPowerValue = (interpolatedNetPowerValue * 1000).round() / 1000
                assets.dispatch(researchAsset1Id, "netPowerForecast", interpolatedNetPowerValue)
            }
        })

rules.add()
        .priority(3)
        .name("Challenges main rule")
        .when({ facts ->

            // Check if parent asset ID is valid
            def parentAsset = facts.matchFirstAssetState(
                    new AssetQuery().ids(parentMeterAssetId))

            if (parentAsset.isEmpty()) {
                LOG.warning("No Parent Asset found with ID: '" + parentMeterAssetId + "'; Check Parent Asset ID and if the rule state configuration is added to the attribute")
                return false
            }

            // Check if challenges asset ID is valid
            def challengesAsset = facts.matchFirstAssetState(
                    new AssetQuery().ids(challengesAssetId))

            if (challengesAsset.isEmpty()) {
                LOG.warning("No Challenges Asset found with ID: '" + challengesAssetId + "'; Check Challenges Asset ID and if the rule state configuration is added to the attribute")
                return false
            }

            // Find attribute changes
            def changesChallenges = facts.matchAssetState(
                    new AssetQuery()
                            .ids(challengesAssetId)
                            .attributeName(OurgridChallengesAsset.CHALLENGE_START.name)
            ).filter { state ->
                def changed = false

                // Get previous state from facts
                def previous = facts.matchFirst(state.id + state.name) as Optional<AttributeInfo>

                if (state.timestamp > previous.map { it.timestamp }.orElse(0)) {
                    // State has been updated (value may be the same but still push this to the children)
                    changed = true
                }

                return changed
            }.toList()

            // Find attribute changes in group
            def changesChildren = facts.matchAssetState(
                    new AssetQuery()
                            .parents(parentMeterAssetId)
                            .types(OurgridMeterAsset)
                            .attributeNames(OurgridMeterAsset.CHALLENGE_JOIN_BUTTON.name)
            ).filter { state ->
                def changed = false

                // Get previous state from facts
                def previous = facts.matchFirst(state.id + state.name) as Optional<AttributeInfo>

                if (state.timestamp > previous.map { it.timestamp }.orElse(0)) {
                    // State has been updated (value may be the same but still push this to the children)
                    changed = true
                }

                return changed
            }.filter { state ->
                // Keep state only if button press changed to true
                boolean buttonState = state.value.orElse(false)

                return buttonState
            }.toList()

            // Find all relevant children's attributes
            def children = facts.matchAssetState(
                    new AssetQuery()
                            .parents(parentMeterAssetId)
                            .types(OurgridMeterAsset)
                            .attributeNames(
                                    OurgridMeterAsset.POWER.name,
                                    OurgridMeterAsset.CHALLENGE_POWER_LIMIT.name,
                                    OurgridMeterAsset.CHALLENGE_POWER_LIMIT_METHOD.name,
                                    OurgridMeterAsset.CONNECTION_STATUS.name,
                                    OurgridMeterAsset.CHALLENGE_STATUS.name,
                                    OurgridMeterAsset.CHALLENGE_POINT_TIMER_START.name,
                                    OurgridMeterAsset.CHALLENGE_POINTS.name,
                                    OurgridMeterAsset.CHALLENGE_POINTS_CURRENT.name,
                                    OurgridMeterAsset.CHALLENGE_POINTS_PER_CHALLENGE.name,
                                    OurgridMeterAsset.CHALLENGES_JOINED.name
                            )
            ).toList()

            // Rule triggers
            boolean triggerRule = false

            long currentMillis = facts.clock.currentTimeMillis

            // Trigger rule every 1 minute = 60000 ms
            if (currentMillis > previousTimeMillisRule3) {
                previousTimeMillisRule3 += 60000
                triggerRule = true
            }

            // Trigger rule if there are changes to process
            if (!changesChallenges.isEmpty()) {
                triggerRule = true
                facts.bind("changesChallenges", changesChallenges)
            }

            if (!changesChildren.isEmpty()) {
                triggerRule = true
                facts.bind("changesChildren", changesChildren)
            }

            if (!children.isEmpty()) {
                facts.bind("children", children)
            }

            return triggerRule
        })
        .then({ facts ->
            def changesChildren = facts.bound("changesChildren") as List<AttributeInfo>
            def changesChallenges = facts.bound("changesChallenges") as List<AttributeInfo>
            def children = facts.bound("children") as List<AttributeInfo>

            // Create fact for state of attribute
            if (changesChildren != null) {
                changesChildren.forEach { state ->
                    facts.put(state.id + state.name, state as Object)
                }
            }

            if (changesChallenges != null) {
                changesChallenges.forEach { state ->
                    facts.put(state.id + state.name, state as Object)
                }
            }

            // Stop if group is empty
            if (children == null) {
                return
            }

            long currentTimestamp = facts.clock.currentTimeMillis
            long minute = (long) Math.floor((currentTimestamp - challengeStartMillis) / 60000)
            long updateTimestamp = challengeStartMillis + minute * 60000

            // Create a map using id as key and attribute structure as value
            def childrenAttributes = [:].withDefault { [:].withDefault { null } }

            children.each { state ->
                String id = state.id
                String attributeName = state.name
                def value = state.value.orElse(null)

                childrenAttributes[id][attributeName] = value
            }

            // Get active period
            def activePeriodValue = facts.matchFirstAssetState(
                    new AssetQuery()
                            .ids(parentDistrictAssetId)
                            .attributeName("activePeriod")
            ).flatMap { it.value }.orElse(null)

            // Default active period of 5 minutes
            long activePeriodMillis = 300000

            if (activePeriodValue == null) {
                assets.dispatch(parentDistrictAssetId, "activePeriod", 5)
            } else {
                activePeriodMillis = activePeriodValue * 60000
            }

            // Check current connection of meters
            Map<String, Boolean> childrenConnection = children
                    .findAll { it.name == OurgridMeterAsset.POWER.name }
                    .collectEntries { state ->
                        boolean connection = false

                        if (state.value.isPresent()) {
                            connection = (currentTimestamp - state.timestamp) < activePeriodMillis
                        }

                        [state.id, connection]
                    }

            // Update connection status & set power reading to null when disconnected
            childrenConnection.forEach { AssetId, connection ->
                String connectionStatus = childrenAttributes.get(AssetId).get(OurgridMeterAsset.CONNECTION_STATUS.name).toString()

                if (connectionStatus == "connected" && !connection) {
                    assets.dispatch(AssetId, OurgridMeterAsset.CONNECTION_STATUS.name, "disconnected")
                    assets.dispatch(AssetId, OurgridMeterAsset.POWER.name, null)
                } else if (connectionStatus == "disconnected" && connection) {
                    assets.dispatch(AssetId, OurgridMeterAsset.CONNECTION_STATUS.name, "connected")
                } else if (connectionStatus == "null" && !connection) {
                    assets.dispatch(AssetId, OurgridMeterAsset.CONNECTION_STATUS.name, "disconnected")
                    assets.dispatch(AssetId, OurgridMeterAsset.POWER.name, null)
                } else if (connectionStatus == "null" && connection) {
                    assets.dispatch(AssetId, OurgridMeterAsset.CONNECTION_STATUS.name, "connected")
                }
            }

            //// Challenges ////
            boolean turnOnChallenges = facts.matchFirstAssetState(
                    new AssetQuery()
                            .ids(challengesAssetId)
                            .attributeName(OurgridChallengesAsset.TURN_ON_CHALLENGES.name)
            ).flatMap { it.value }.orElse(false) as boolean

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")

            // Automatic challenge start from forecast
            if (turnOnChallenges && !challengeActive) {
                def powerImportMaxValue = facts.matchFirstAssetState(
                        new AssetQuery()
                                .ids(parentDistrictAssetId)
                                .attributeName("powerImportMax")
                ).flatMap { it.value }.orElse(null)

                def powerImportCriticalPercentage = facts.matchFirstAssetState(
                        new AssetQuery()
                                .ids(parentDistrictAssetId)
                                .attributeName("powerImportCriticalPercentage")
                ).flatMap { it.value }.orElse(null)

                def challengeWaitValue = facts.matchFirstAssetState(
                        new AssetQuery()
                                .ids(challengesAssetId)
                                .attributeName(OurgridChallengesAsset.CHALLENGE_WAIT.name)
                ).flatMap { it.value }.orElse(null)

                def challengeDurationValue = facts.matchFirstAssetState(
                        new AssetQuery()
                                .ids(challengesAssetId)
                                .attributeName(OurgridChallengesAsset.CHALLENGE_DURATION.name)
                ).flatMap { it.value }.orElse(null)

                if (powerImportMaxValue != null && powerImportCriticalPercentage != null && challengeWaitValue != null && challengeDurationValue != null) {
                    long challengeWaitMillis = challengeWaitValue * 60000
                    long currentTimestampCeilMinute = currentTimestamp - (currentTimestamp % 60000) + 60000

                    long challengeStartTimestampMillis = currentTimestampCeilMinute + challengeWaitMillis

                    // Interpolate net power value from net power forecast
                    Double interpolatedNetPowerValue = calculateForecastValue(challengeStartTimestampMillis, parentDistrictAssetId, "powerDistrict")

                    if (interpolatedNetPowerValue != null) {
                        Double netPowerCritical = powerImportMaxValue * powerImportCriticalPercentage / 100

                        if (interpolatedNetPowerValue > netPowerCritical) {
                            long challengeDurationMillis = challengeDurationValue * 60000

                            challengeStartMillis = challengeStartTimestampMillis
                            challengeEndMillis = challengeStartTimestampMillis + challengeDurationMillis

                            // Convert timestamp to a Date object
                            Date dateTimeFrom = new Date(challengeStartMillis)
                            Date dateTimeTo = new Date(challengeEndMillis)

                            // Format Date object as a string
                            String dateTimeFromStr = sdf.format(dateTimeFrom)
                            String dateTimeToStr = sdf.format(dateTimeTo)

                            // Update attributes
                            assets.dispatch(challengesAssetId, OurgridChallengesAsset.CHALLENGE_START.name, dateTimeFromStr)
                            assets.dispatch(challengesAssetId, OurgridChallengesAsset.CHALLENGE_END.name, dateTimeToStr)

                            challengeActive = true
                        }
                    }
                }
            }

            // Calculate power limit per household for new challenge
            Map<String, Integer> powerLimitMap = children
                    .findAll { it.name == OurgridMeterAsset.CHALLENGE_POWER_LIMIT.name }
                    .collectEntries { state -> [state.id, state.value.orElse(null)] }

            if (challengeActive && !challengeActivePrevious) {
                powerLimitMap = calculatePowerLimits(facts, childrenAttributes, challengesAssetId)
                challengePointsMap.clear()

                challengeActivePrevious = true
            }

            // Get challenge status per household
            Map<String, String> childrenChallengeStatus = children
                    .findAll { it.name == OurgridMeterAsset.CHALLENGE_STATUS.name }
                    .collectEntries { state -> [state.id, state.value.orElse('unknown')] }

            // Join challenge at button press
            changesChildren.findAll { it.name == OurgridMeterAsset.CHALLENGE_JOIN_BUTTON.name }
                    .forEach { state ->
                        boolean button = state.value.orElse(false)
                        boolean connection = childrenConnection.get(state.id)

                        if (button) {
                            // Reset button
                            assets.dispatch(state.id, OurgridMeterAsset.CHALLENGE_JOIN_BUTTON.name, false)
                            String challengeStatus = childrenChallengeStatus.get(state.id)

                            if (challengeStatus == "joinChallenge" && connection) {
                                childrenChallengeStatus.put(state.id, "joinedChallenge")

                                if (currentTimestamp < challengeStartMillis) {
                                    assets.dispatch(state.id, OurgridMeterAsset.CHALLENGE_STATUS.name, "joinedChallenge")
                                }
                            }
                        }
                    }

            Map<String, Long> childrenPointTimerStart = children
                    .findAll { it.name == OurgridMeterAsset.CHALLENGE_POINT_TIMER_START.name }
                    .collectEntries { state -> [state.id, state.value.orElse(0L)] }

            // Update Challenge status
            childrenChallengeStatus.each { assetId, status ->
                String challengeStatus = status.toString()
                boolean connection = childrenConnection.get(assetId)
                Integer points = childrenAttributes.get(assetId).get(OurgridMeterAsset.CHALLENGE_POINTS.name) as Integer

                if (currentTimestamp < challengeStartMillis) {
                    if (challengeStatus != "joinChallenge" && challengeStatus != "joinedChallenge") {
                        assets.dispatch(assetId, OurgridMeterAsset.CHALLENGE_STATUS.name, "joinChallenge")
                        childrenChallengeStatus.put(assetId, "joinChallenge")
                    }
                } else if (currentTimestamp >= challengeStartMillis && currentTimestamp <= challengeEndMillis) {
                    if (challengeStatus == "joinedChallenge") {
                        if (connection) {
                            assets.dispatch(assetId, OurgridMeterAsset.CHALLENGE_STATUS.name, "activeChallenge")
                            childrenChallengeStatus.put(assetId, "activeChallenge")
                        } else {
                            assets.dispatch(assetId, OurgridMeterAsset.CHALLENGE_STATUS.name, "inactiveChallenge")
                            childrenChallengeStatus.put(assetId, "inactiveChallenge")
                        }

                        // Update number of joined challenges
                        def challengesJoined = childrenAttributes.get(assetId).get(OurgridMeterAsset.CHALLENGES_JOINED.name)

                        if (challengesJoined == null) {
                            challengesJoined = 0
                        }
                        challengesJoined++
                        assets.dispatch(assetId, OurgridMeterAsset.CHALLENGES_JOINED.name, challengesJoined)

                        // Reset challenge points for new challenge
                        assets.dispatch(assetId, OurgridMeterAsset.CHALLENGE_POINTS_CURRENT.name, 0)

                        // Number of total points at start of new challenge
                        assets.dispatch(assetId, OurgridMeterAsset.CHALLENGE_POINTS.name, points)
                        challengePointsMap.put(assetId, points)

                        // Update earn point timer
                        assets.dispatch(assetId, OurgridMeterAsset.CHALLENGE_POINT_TIMER_START.name, updateTimestamp)
                        childrenPointTimerStart.put(assetId, updateTimestamp)
                    } else if (challengeStatus == "inactiveChallenge" && connection) {
                        assets.dispatch(assetId, OurgridMeterAsset.CHALLENGE_STATUS.name, "activeChallenge")
                        childrenChallengeStatus.put(assetId, "activeChallenge")

                        assets.dispatch(assetId, OurgridMeterAsset.CHALLENGE_POINT_TIMER_START.name, updateTimestamp)
                        childrenPointTimerStart.put(assetId, updateTimestamp)
                    } else if (challengeStatus == "activeChallenge" && !connection) {
                        assets.dispatch(assetId, OurgridMeterAsset.CHALLENGE_STATUS.name, "inactiveChallenge")
                        childrenChallengeStatus.put(assetId, "inactiveChallenge")
                    }
                } else if (currentTimestamp > challengeEndMillis) {
                    if (challengeStatus != "noChallenge") {
                        assets.dispatch(assetId, OurgridMeterAsset.CHALLENGE_STATUS.name, "noChallenge")
                    }
                }
            }

            def challengePointIntervalValue = facts.matchFirstAssetState(
                    new AssetQuery()
                            .ids(challengesAssetId)
                            .attributeName(OurgridChallengesAsset.CHALLENGE_EARN_POINT_INTERVAL.name)
            ).flatMap { it.value }.orElse(null)

            // Default earn point interval of 6 minutes
            long challengePointIntervalMillis = 360000

            if (challengePointIntervalValue == null) {
                assets.dispatch(challengesAssetId, OurgridChallengesAsset.CHALLENGE_EARN_POINT_INTERVAL.name, 6)
            } else {
                challengePointIntervalMillis = challengePointIntervalValue * 60000
            }

            // Award challenge points
            if (challengeActive) {
                childrenChallengeStatus.each { assetId, status ->
                    String challengeStatus = status.toString()

                    def childAttributes = childrenAttributes.get(assetId)
                    def netPower = childAttributes.get(OurgridMeterAsset.POWER.name)
                    def points = childAttributes.get(OurgridMeterAsset.CHALLENGE_POINTS.name)
                    def pointsCurrent = childAttributes.get(OurgridMeterAsset.CHALLENGE_POINTS_CURRENT.name)

                    Integer powerLimit = powerLimitMap.get(assetId)

                    if (powerLimit == null) {
                        powerLimit = 2000
                        assets.dispatch(assetId, OurgridMeterAsset.CHALLENGE_POWER_LIMIT.name, powerLimit)
                    }

                    if (points == null) {
                        points = 0
                        assets.dispatch(assetId, OurgridMeterAsset.CHALLENGE_POINTS.name, points)
                    }

                    if (pointsCurrent == null) {
                        pointsCurrent = 0
                        assets.dispatch(assetId, OurgridMeterAsset.CHALLENGE_POINTS_CURRENT.name, pointsCurrent)
                    }

                    // Update total number of points during challenge
                    if (netPower != null && challengeStatus == "activeChallenge" && currentTimestamp >= challengeStartMillis && currentTimestamp <= challengeEndMillis) {
                        long pointTimerStart = childrenPointTimerStart.get(assetId)

                        if (netPower > powerLimit) {
                            // Reset timer
                            assets.dispatch(assetId, OurgridMeterAsset.CHALLENGE_POINT_TIMER_START.name, updateTimestamp)
                        } else if (currentTimestamp - pointTimerStart >= challengePointIntervalMillis) {
                            // Award point
                            points++
                            pointsCurrent++
                            assets.dispatch(assetId, OurgridMeterAsset.CHALLENGE_POINTS.name, points)
                            assets.dispatch(assetId, OurgridMeterAsset.CHALLENGE_POINTS_CURRENT.name, pointsCurrent)
                            assets.dispatch(assetId, OurgridMeterAsset.CHALLENGE_POINT_TIMER_START.name, updateTimestamp)
                        }
                    } else if (netPower != null && challengeStatus == "activeChallenge" && currentTimestamp > challengeEndMillis) {
                        // Award remaining point
                        if (netPower <= powerLimit) {
                            points++
                            pointsCurrent++
                            assets.dispatch(assetId, OurgridMeterAsset.CHALLENGE_POINTS.name, points)
                            assets.dispatch(assetId, OurgridMeterAsset.CHALLENGE_POINTS_CURRENT.name, pointsCurrent)
                            assets.dispatch(assetId, OurgridMeterAsset.CHALLENGE_POINT_TIMER_START.name, updateTimestamp)
                        }
                    }

                    // Update at end of challenge
                    if (currentTimestamp > challengeEndMillis) {
                        // Number of earned points per challenge, -1 means household did not join challenge
                        Integer pointsPrevious = challengePointsMap.get(assetId)
                        int pointsCurrentChallenge = -1

                        if (pointsPrevious != null) {
                            pointsCurrentChallenge = points - pointsPrevious
                        }
                        challengePointsMap.put(assetId, pointsCurrentChallenge)
                        assets.dispatch(assetId, OurgridMeterAsset.CHALLENGE_POINTS_PER_CHALLENGE.name, pointsCurrentChallenge)
                    }
                }
            }

            // Challenges asset general status (and perfect household participant)
            if (challengeActive) {
                def challengeStatusGeneral = facts.matchFirstAssetState(
                        new AssetQuery()
                                .ids(challengesAssetId)
                                .attributeName(OurgridChallengesAsset.CHALLENGE_GENERAL_STATUS.name)
                ).flatMap { it.value }.orElse("unknown") as String

                def pointsMax = facts.matchFirstAssetState(
                        new AssetQuery()
                                .ids(challengesAssetId)
                                .attributeName(OurgridChallengesAsset.CHALLENGE_POINTS_MAX.name)
                ).flatMap { it.value }.orElse(0) as Integer

                if (currentTimestamp < challengeStartMillis) {
                    if (challengeStatusGeneral != "joinedChallenge") {
                        assets.dispatch(challengesAssetId, OurgridChallengesAsset.CHALLENGE_GENERAL_STATUS.name, "joinedChallenge")
                    }
                } else if (currentTimestamp >= challengeStartMillis && currentTimestamp <= challengeEndMillis) {
                    if (challengeStatusGeneral != "activeChallenge") {
                        assets.dispatch(challengesAssetId, OurgridChallengesAsset.CHALLENGE_GENERAL_STATUS.name, "activeChallenge")
                        assets.dispatch(challengesAssetId, OurgridChallengesAsset.CHALLENGE_POINT_TIMER_START.name, updateTimestamp)
                        assets.dispatch(challengesAssetId, OurgridChallengesAsset.CHALLENGE_POINTS_MAX.name, pointsMax)
                    } else if (challengeStatusGeneral == "activeChallenge") {
                        // Award point to perfect household
                        def pointTimerStartValue = facts.matchFirstAssetState(
                                new AssetQuery()
                                        .ids(challengesAssetId)
                                        .attributeName(OurgridChallengesAsset.CHALLENGE_POINT_TIMER_START.name)
                        ).flatMap { it.value }.orElse(null)

                        if (pointTimerStartValue != null) {
                            long pointTimerStartMillis = pointTimerStartValue as long

                            if ((currentTimestamp - pointTimerStartMillis) >= challengePointIntervalMillis) {
                                pointsMax++
                                assets.dispatch(challengesAssetId, OurgridChallengesAsset.CHALLENGE_POINTS_MAX.name, pointsMax)
                                assets.dispatch(challengesAssetId, OurgridChallengesAsset.CHALLENGE_POINT_TIMER_START.name, updateTimestamp)
                            }
                        }
                    }
                } else if (currentTimestamp > challengeEndMillis) {
                    // Award remaining point to perfect household
                    if (challengeStatusGeneral == "activeChallenge") {
                        pointsMax++
                        assets.dispatch(challengesAssetId, OurgridChallengesAsset.CHALLENGE_POINTS_MAX.name, pointsMax)
                        assets.dispatch(challengesAssetId, OurgridChallengesAsset.CHALLENGE_POINT_TIMER_START.name, updateTimestamp)
                    }

                    // Calculate total points earned by all users
                    def pointsTotal = facts.matchFirstAssetState(
                            new AssetQuery()
                                    .ids(challengesAssetId)
                                    .attributeName(OurgridChallengesAsset.CHALLENGE_POINTS_TOTAL.name)
                    ).flatMap { it.value }.orElse(0)

                    def pointsTotalCurrentChallenge = challengePointsMap.findAll { it.value >= 0 }.values().sum()

                    if (pointsTotalCurrentChallenge == null) {
                        pointsTotalCurrentChallenge = 0
                    }
                    pointsTotal = pointsTotal + pointsTotalCurrentChallenge

                    // Number of participating households in current challenge
                    def participationCount = challengePointsMap.count { key, value -> value >= 0 }

                    // Household participation rate (%) in current challenge
                    double participationRate = 0.0

                    if (challengePointsMap.size() > 0) {
                        participationRate = (participationCount / challengePointsMap.size() * 100).round(1)
                    }

                    assets.dispatch(challengesAssetId, OurgridChallengesAsset.CHALLENGE_POINTS_TOTAL.name, pointsTotal)
                    assets.dispatch(challengesAssetId, OurgridChallengesAsset.CHALLENGE_PARTICIPATION.name, participationCount)
                    assets.dispatch(challengesAssetId, OurgridChallengesAsset.CHALLENGE_PARTICIPATION_RATE.name, participationRate)

                    assets.dispatch(challengesAssetId, OurgridChallengesAsset.CHALLENGE_GENERAL_STATUS.name, "noChallenge")

                    challengeActive = false
                    challengeActivePrevious = false
                }
            }
        })

rules.add()
        .priority(4)
        .name("Activate challenge manually rule")
        .when({
            facts ->
                // Put your attribute name here:
                String attributeName = OurgridChallengesAsset.ACTIVATE_CHALLENGE_MANUALLY.name

                // Find first matching attribute using an asset query
                facts.matchFirstAssetState(
                        new AssetQuery()
                                .ids(challengesAssetId)
                                .attributeName(attributeName)
                ).map { assetState ->
                    // Check if this rule should fire
                    Optional<Long> lastFireTimestamp = facts.getOptional(attributeName)
                    if (lastFireTimestamp.isPresent() && assetState.getTimestamp() <= lastFireTimestamp.get()) {
                        return false
                    }
                    // Compute and bind any facts required for the then closure
                    facts.bind("assetState", assetState)
                    true
                }.orElseGet {
                    // Asset state didn't match so clear out any custom facts to allow the rule to fire next time the when closure matches
                    facts.remove(attributeName)
                    false
                }
        })
        .then({
            facts ->
                // Extract any bound facts
                AttributeInfo attributeInfo = facts.bound("assetState")

                // Insert the custom fact to prevent rule loop
                facts.put(attributeInfo.name, attributeInfo.getTimestamp())

                long currentTimestamp = facts.clock.currentTimeMillis
                long updateTimestamp = currentTimestamp - (currentTimestamp % 60000)

                // Get attribute values
                boolean button = attributeInfo.value.orElse(false)

                boolean turnOnChallenges = facts.matchFirstAssetState(
                        new AssetQuery()
                                .ids(challengesAssetId)
                                .attributeName(OurgridChallengesAsset.TURN_ON_CHALLENGES.name)
                ).flatMap { it.value }.orElse(false) as boolean

                def challengeWaitValue = facts.matchFirstAssetState(
                        new AssetQuery()
                                .ids(challengesAssetId)
                                .attributeName(OurgridChallengesAsset.CHALLENGE_WAIT.name)
                ).flatMap { it.value }.orElse(null)

                if (challengeWaitValue == null) {
                    challengeWaitValue = 15
                    assets.dispatch(challengesAssetId, OurgridChallengesAsset.CHALLENGE_WAIT.name, challengeWaitValue)
                }

                def challengeDurationValue = facts.matchFirstAssetState(
                        new AssetQuery()
                                .ids(challengesAssetId)
                                .attributeName(OurgridChallengesAsset.CHALLENGE_DURATION.name)
                ).flatMap { it.value }.orElse(null)

                if (challengeDurationValue == null) {
                    challengeDurationValue = 60
                    assets.dispatch(challengesAssetId, OurgridChallengesAsset.CHALLENGE_DURATION.name, challengeDurationValue)
                }

                if (button) {
                    assets.dispatch(challengesAssetId, attributeInfo.name, false)

                    if (!turnOnChallenges) {
                        LOG.info("Challenges are turned off for district with asset ID = '" + parentDistrictAssetId + "'")
                        return
                    }

                    if (!challengeActive) {
                        long challengeWaitMillis = challengeWaitValue * 60000 + 60000
                        long challengeDurationMillis = challengeDurationValue * 60000

                        challengeStartMillis = updateTimestamp + challengeWaitMillis
                        challengeEndMillis = updateTimestamp + challengeWaitMillis + challengeDurationMillis

                        // Create desired date-time format
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")

                        // Convert timestamp to a Date object
                        Date dateTimeFrom = new Date(challengeStartMillis)
                        Date dateTimeTo = new Date(challengeEndMillis)

                        // Format Date object as a string
                        String dateTimeFromStr = sdf.format(dateTimeFrom)
                        String dateTimeToStr = sdf.format(dateTimeTo)

                        // Update attributes
                        assets.dispatch(challengesAssetId, OurgridChallengesAsset.CHALLENGE_GENERAL_STATUS.name, "joinedChallenge")
                        assets.dispatch(challengesAssetId, OurgridChallengesAsset.CHALLENGE_START.name, dateTimeFromStr)
                        assets.dispatch(challengesAssetId, OurgridChallengesAsset.CHALLENGE_END.name, dateTimeToStr)
                        challengeActive = true
                    } else {
                        LOG.info("A challenge is already activate for district with asset ID = '" + parentDistrictAssetId + "'")
                    }
                }
        })

rules.add()
        .priority(5)
        .name("Challenges exchange points rule")
        .when({
            facts ->
                // Check if asset ID is valid
                String assetId = challengesAssetId

                def targetAsset = facts.matchFirstAssetState(
                        new AssetQuery().ids(assetId))

                if (targetAsset.isEmpty()) {
                    LOG.warning("No asset found with ID: '" + assetId + "'; Check asset ID and if the rule state configuration is added to the attribute")
                    return false
                }

                // Find attribute changes
                def changes = facts.matchAssetState(
                        new AssetQuery()
                                .ids(assetId)
                                .attributeNames(
                                        OurgridChallengesAsset.CHALLENGE_BUDGET.name,
                                        OurgridChallengesAsset.CHALLENGE_POINTS_YEAR_PREDICTION_MANUAL.name
                                )
                ).filter { state ->
                    def changed = false

                    // Get previous state from facts
                    def previous = facts.matchFirst(state.id + state.name) as Optional<AttributeInfo>

                    // Check if state has been updated (attribute value can be unchanged)
                    if (state.timestamp > previous.map { it.timestamp }.orElse(0)) {
                        changed = true
                    }

                    return changed
                }.toList()

                // Rule triggers
                boolean triggerRule = false

                long currentMillis = facts.clock.currentTimeMillis
                String dateCurrent = dateOnlyFormat.format(new Date(currentMillis))

                // Trigger rule at 0:00am
                if (dateCurrent != previousDateRule5) {
                    previousDateRule5 = dateCurrent
                    triggerRule = true
                }

                // Trigger rule if there are changes to process
                if (!changes.isEmpty()) {
                    facts.bind("changes", changes)
                    triggerRule = true
                }

                return triggerRule
        })
        .then({
            facts ->
                def changes = facts.bound("changes") as List<AttributeInfo>

                // Create fact for state of attribute
                if (changes != null) {
                    changes.forEach { state ->
                        facts.put(state.id + state.name, state as Object)
                    }
                }

                // Get attribute values
                def budgetValue = facts.matchFirstAssetState(
                        new AssetQuery()
                                .ids(challengesAssetId)
                                .attributeName(OurgridChallengesAsset.CHALLENGE_BUDGET.name)
                ).flatMap { it.value }.orElse(null)

                def pointsYearPredictionManualValue = facts.matchFirstAssetState(
                        new AssetQuery()
                                .ids(challengesAssetId)
                                .attributeName(OurgridChallengesAsset.CHALLENGE_POINTS_YEAR_PREDICTION_MANUAL.name)
                ).flatMap { it.value }.orElse(0)

                def pointsTotalValue = facts.matchFirstAssetState(
                        new AssetQuery()
                                .ids(challengesAssetId)
                                .attributeName(OurgridChallengesAsset.CHALLENGE_POINTS_TOTAL.name)
                ).flatMap { it.value }.orElse(null)

                if (pointsTotalValue == null) {
                    assets.dispatch(challengesAssetId, OurgridChallengesAsset.CHALLENGE_POINTS_TOTAL.name, 0)
                }

                // Get day of year
                long currentTimestamp = facts.clock.currentTimeMillis
                Date currentDate = new Date(currentTimestamp)
                Calendar calendar = Calendar.getInstance()
                calendar.setTime(currentDate)
                int dayOfYear = calendar.get(Calendar.DAY_OF_YEAR)

                // Calculate predicted number of all earned points during a year based on current number of earned points
                if (pointsTotalValue != null) {
                    int pointsYearPredictionAutomatic = (int) Math.round((Double) (pointsTotalValue / dayOfYear * 365))
                    assets.dispatch(challengesAssetId, OurgridChallengesAsset.CHALLENGE_POINTS_YEAR_PREDICTION_AUTOMATIC.name, pointsYearPredictionAutomatic)
                }

                // Calculate the exchange rate
                if (budgetValue != null && pointsYearPredictionManualValue > 0) {
                    def exchangeRate = budgetValue / pointsYearPredictionManualValue
                    assets.dispatch(challengesAssetId, OurgridChallengesAsset.CHALLENGE_POINTS_EXCHANGE_RATE.name, exchangeRate)
                }
        })

rules.add()
        .priority(6)
        .name("Peak points rule")
        .when({
            facts ->
                // Check if parent asset ID is valid
                def parentAsset = facts.matchFirstAssetState(new AssetQuery().ids(parentMeterAssetId))

                if (parentAsset.isEmpty()) {
                    LOG.warning("No Parent Asset found with ID: '" + parentMeterAssetId + "'; Check Parent Asset ID and if the rule state configuration is added to the attribute")
                    return false
                }

                // Check if peaks asset ID is valid
                def peaksAsset = facts.matchFirstAssetState(new AssetQuery().ids(peaksAssetId))

                if (peaksAsset.isEmpty()) {
                    LOG.warning("No Peaks Asset found with ID: '" + challengesAssetId + "'; Check Peaks Asset ID and if the rule state configuration is added to the attribute")
                    return false
                }

                // Check if peak points are turned on
                Boolean turnOnPeakPoints = facts.matchFirstAssetState(
                        new AssetQuery()
                                .ids(peaksAssetId)
                                .attributeName(OurgridPeaksAsset.TURN_ON_PEAK_POINTS.name)
                ).flatMap { it.value }.orElse(false)

                // Prevent instant rule trigger when peak points are turned on
                if (turnOnPeakPoints && !turnOnPeakPointsPrevious) {
                    turnOnPeakPointsPrevious = true
                    previousDateRule6 = dateOnlyFormat.format(new Date())
                } else if (!turnOnPeakPoints && turnOnPeakPointsPrevious) {
                    turnOnPeakPointsPrevious = false
                }

                if (!turnOnPeakPoints) {
                    return false
                }

                // Find all relevant children's attributes
                def children = facts.matchAssetState(
                        new AssetQuery()
                                .parents(parentMeterAssetId)
                                .types(OurgridMeterAsset)
                                .attributeName(OurgridMeterAsset.PEAK_POINTS.name)
                ).toList()

                if (!children.isEmpty()) {
                    facts.bind("children", children)
                } else {
                    return false
                }

                // Rule trigger
                boolean triggerRule = false

                long currentMillis = facts.clock.currentTimeMillis
                String dateCurrent = dateOnlyFormat.format(new Date(currentMillis))

                // Trigger rule at 0:00am
                if (dateCurrent != previousDateRule6) {
                    facts.bind("dateFromStr", previousDateRule6)
                    facts.bind("dateToStr", dateCurrent)
                    previousDateRule6 = dateCurrent
                    triggerRule = true
                }

                return triggerRule
        })
        .then({
            facts ->
                def children = facts.bound("children") as List<AttributeInfo>
                def dateFromStr = facts.bound("dateFromStr") as String
                def dateToStr = facts.bound("dateToStr") as String

                // Create a map using child asset ID as key and corresponding attributes as value
                Map<String, Map<Object, Object>> childrenAttributes = [:].withDefault { [:].withDefault { null } }

                children.each { attributeInfo ->
                    String assetId = attributeInfo.id
                    String attributeName = attributeInfo.name
                    def value = attributeInfo.value.orElse(null)

                    childrenAttributes[assetId][attributeName] = value
                }

                // Peak points per household map
                Map<String, Double> peakPointsMap = children
                        .findAll { it.name == OurgridMeterAsset.PEAK_POINTS.name }
                        .collectEntries { attributeInfo -> [attributeInfo.id, attributeInfo.value.orElse(0.0)] }

                // Get attribute values
                Integer activePeriodMinutes = facts.matchFirstAssetState(
                        new AssetQuery()
                                .ids(parentDistrictAssetId)
                                .attributeName("activePeriod")
                ).flatMap { it.value }.orElse(5) as Integer

                long activePeriodMillis = activePeriodMinutes * 60000L

                Double connectionQualityThreshold = facts.matchFirstAssetState(
                        new AssetQuery()
                                .ids(peaksAssetId)
                                .attributeName(OurgridPeaksAsset.CONNECTION_QUALITY_THRESHOLD.name)
                ).flatMap { it.value }.orElse(null) as Double

                if (connectionQualityThreshold == null) {
                    connectionQualityThreshold = 95.0
                    assets.dispatch(peaksAssetId, OurgridPeaksAsset.CONNECTION_QUALITY_THRESHOLD.name, connectionQualityThreshold)
                }

                Double peakConsumptionPercentageThreshold = facts.matchFirstAssetState(
                        new AssetQuery()
                                .ids(peaksAssetId)
                                .attributeNames(OurgridPeaksAsset.PEAK_CONSUMPTION_THRESHOLD.name)
                ).flatMap { it.value }.orElse(null) as Double

                if (peakConsumptionPercentageThreshold == null) {
                    peakConsumptionPercentageThreshold = 16.7
                    assets.dispatch(peaksAssetId, OurgridPeaksAsset.PEAK_CONSUMPTION_THRESHOLD.name, peakConsumptionPercentageThreshold)
                }

                Double peakPointsDay = facts.matchFirstAssetState(
                        new AssetQuery()
                                .ids(peaksAssetId)
                                .attributeName(OurgridPeaksAsset.PEAK_POINTS_DAY.name)
                ).flatMap { it.value }.orElse(null) as Double

                if (peakPointsDay == null) {
                    peakPointsDay = 1.0
                    assets.dispatch(peaksAssetId, OurgridPeaksAsset.PEAK_POINTS_DAY.name, peakPointsDay)
                }

                String peakPeriodsJSON = facts.matchFirstAssetState(
                        new AssetQuery()
                                .ids(peaksAssetId)
                                .attributeName(OurgridPeaksAsset.PEAK_PERIODS.name)
                ).flatMap { it.value }.orElse("")

                if (peakPeriodsJSON == "") {
                    peakPeriodsJSON = '[{"peak_period_start": "7:00", "peak_period_end": "9:00"}, {"peak_period_start": "17:00", "peak_period_end": "19:00"}]'
                    assets.dispatch(peaksAssetId, OurgridPeaksAsset.PEAK_PERIODS.name, peakPeriodsJSON)
                }

                // Parse peak periods JSON
                List<String[]> peakPeriodsList = new ArrayList<>()
                try {
                    ObjectMapper mapper = new ObjectMapper()
                    JsonNode jsonNode = mapper.readTree(peakPeriodsJSON)

                    for (JsonNode periodNode : jsonNode) {
                        String[] peakPeriod = new String[2]
                        peakPeriod[0] = dateFromStr + " " + periodNode.get("peak_period_start").asText()
                        peakPeriod[1] = dateFromStr + " " + periodNode.get("peak_period_end").asText()
                        peakPeriodsList.add(peakPeriod)
                    }
                } catch (Exception e) {
                    LOG.info("Incorrect peak periods JSON:\n '" + peakPeriodsJSON + "'\n Example of a correct peak periods JSON: [{\"peak_period_start\": \"7:00\",\"peak_period_end\": \"9:00\"}]")
                }

                // Create time intervals for given day
                long dateTimeFromMillis = dateOnlyFormat.parse(dateFromStr).getTime()
                long dateTimeToMillis = dateOnlyFormat.parse(dateToStr).getTime()
                TreeMap<Long, Boolean> timeIntervals = new TreeMap<>()
                SimpleDateFormat dateWithoutSecondsFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm")

                for (long i = dateTimeFromMillis; i < dateTimeToMillis; i += activePeriodMillis) {
                    timeIntervals.put(i, false)
                }

                // Set peak period intervals to true
                peakPeriodsList.forEach { peakPeriod ->
                    long periodStartMillis = dateWithoutSecondsFormat.parse(peakPeriod[0]).getTime()
                    long periodEndMillis = dateWithoutSecondsFormat.parse(peakPeriod[1]).getTime()

                    long intervalStartMillis = periodStartMillis - periodStartMillis % activePeriodMillis
                    long intervalEndMillis = periodEndMillis - periodEndMillis % activePeriodMillis

                    for (long i = intervalStartMillis; i < intervalEndMillis; i += activePeriodMillis) {
                        timeIntervals.put(i, true)
                    }
                }

                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")

                // Calculate peak period power consumption per Household
                childrenAttributes.forEach { assetId, values ->
                    Double peakPointsHousehold = values.get(OurgridMeterAsset.PEAK_POINTS.name) as Double
                    TreeMap<String, Double> powerDatapoints = getDatabaseDatapoints("asset_datapoint", assetId, "power", dateFromStr, dateToStr)

                    // Data binning
                    Map<Long, List<Double>> intervalGroups = new TreeMap<>()
                    powerDatapoints.forEach { timeStamp, powerValue ->
                        long timeMillis = sdf.parse(timeStamp).getTime()
                        long intervalStart = timeMillis - timeMillis % activePeriodMillis
                        intervalGroups.putIfAbsent(intervalStart, new ArrayList<>())
                        intervalGroups.get(intervalStart).add(powerValue)
                    }

                    Map<Long, Double> intervalAverages = new TreeMap<>()
                    intervalGroups.each { intervalStart, powerValues ->
                        double average = powerValues.sum() / powerValues.size()

                        // Set negative power consumption to 0
                        if (average < 0) {
                            average = 0
                        }
                        intervalAverages.put(intervalStart, average)
                    }

                    // Get peak power consumption
                    int countInsidePeakIntervals = 0
                    int countOutsidePeakIntervals = 0
                    double insidePeakConsumption = 0.0
                    double outsidePeakConsumption = 0.0

                    timeIntervals.forEach { intervalStart, insidePeak ->
                        Double intervalPower = intervalAverages.get(intervalStart)

                        if (intervalAverages.get(intervalStart) != null && insidePeak) {
                            insidePeakConsumption += intervalPower
                            countInsidePeakIntervals++
                        } else if (intervalAverages.get(intervalStart) != null && !insidePeak) {
                            outsidePeakConsumption += intervalPower
                            countOutsidePeakIntervals++
                        }
                    }

                    // Calculate connection quality percentage
                    double connectionQuality = ((countInsidePeakIntervals + countOutsidePeakIntervals) / timeIntervals.size() * 100).round(1)
                    assets.dispatch(assetId, OurgridMeterAsset.CONNECTION_QUALITY.name, connectionQuality)

                    if (connectionQuality >= connectionQualityThreshold) {
                        // Calculate peak power consumption percentage
                        double totalConsumption = insidePeakConsumption + outsidePeakConsumption
                        double peakConsumptionPercentage = 0.0

                        if (totalConsumption != 0) {
                            peakConsumptionPercentage = (insidePeakConsumption / totalConsumption * 100).round(1)
                        }

                        // Award peak points
                        if (peakConsumptionPercentage <= peakConsumptionPercentageThreshold) {
                            if (peakPointsHousehold == null) {
                                peakPointsHousehold = 0.0
                            }
                            peakPointsHousehold += peakPointsDay
                            peakPointsMap.put(assetId, peakPointsHousehold)
                            assets.dispatch(assetId, OurgridMeterAsset.PEAK_POINTS.name, peakPointsHousehold)
                        }
                    }
                }

                // Update total peak points
                def totalPoints = peakPointsMap.values().sum()
                assets.dispatch(peaksAssetId, OurgridPeaksAsset.PEAK_POINTS_TOTAL.name, totalPoints)
        })

rules.add()
        .priority(7)
        .name("Update household earnings rule")
        .when({
            facts ->
                // Find attribute changes in group
                def changesChildren = facts.matchAssetState(
                        new AssetQuery()
                                .parents(parentMeterAssetId)
                                .types(OurgridMeterAsset)
                                .attributeNames(
                                        OurgridMeterAsset.CHALLENGE_POINTS.name,
                                        OurgridMeterAsset.PEAK_POINTS.name,
                                )
                ).filter { state ->
                    def changed = false

                    // Get previous state from facts
                    def previous = facts.matchFirst(state.id + state.name) as Optional<AttributeInfo>

                    if (state.timestamp > previous.map { it.timestamp }.orElse(0)) {
                        // State has been updated (value may be the same but still push this to the children)
                        changed = true
                    }

                    return changed
                }.toList()

                // Rule triggers
                boolean triggerRule = false

                // Trigger rule if there are changes to process
                if (!changesChildren.isEmpty()) {
                    triggerRule = true
                    facts.bind("changesChildren", changesChildren)
                }

                return triggerRule
        })
        .then({
            facts ->
                def changesChildren = facts.bound("changesChildren") as List<AttributeInfo>

                // Create fact for state of attribute
                if (changesChildren != null) {
                    changesChildren.forEach { state ->
                        facts.put(state.id + state.name, state as Object)
                    }
                }

                // Get both point types for children with changed points
                String[] assetIds = changesChildren.id.unique()

                def children = facts.matchAssetState(
                        new AssetQuery()
                                .ids(assetIds)
                                .attributeNames(
                                        OurgridMeterAsset.CHALLENGE_POINTS.name,
                                        OurgridMeterAsset.PEAK_POINTS.name
                                )
                ).toList()

                // Create a map using child asset ID as key and corresponding attributes as value
                Map<String, Map<Object, Object>> childrenAttributes = [:].withDefault { [:].withDefault { null } }

                children.each { attributeInfo ->
                    String assetId = attributeInfo.id
                    String attributeName = attributeInfo.name
                    def value = attributeInfo.value.orElse(null)

                    childrenAttributes[assetId][attributeName] = value
                }

                // Get exchange rate
                def challengeExchangeRateValue = facts.matchFirstAssetState(
                        new AssetQuery()
                                .ids(challengesAssetId)
                                .attributeName(OurgridChallengesAsset.CHALLENGE_POINTS_EXCHANGE_RATE.name)
                ).flatMap { it.value }.orElse(null)

                // Calculate total points and corresponding earnings
                childrenAttributes.forEach { assetId, values ->
                    def challengePoints = values.get(OurgridMeterAsset.CHALLENGE_POINTS.name)
                    def peakPoints = values.get(OurgridMeterAsset.PEAK_POINTS.name)

                    if (challengePoints == null) {
                        challengePoints = 0
                    }

                    if (peakPoints == null) {
                        peakPoints = 0
                    }

                    def totalPoints = challengePoints + peakPoints
                    assets.dispatch(assetId, OurgridMeterAsset.TOTAL_POINTS.name, totalPoints)

                    if (challengeExchangeRateValue != null) {
                        def earnings = (challengeExchangeRateValue * totalPoints as Double).round(2)
                        assets.dispatch(assetId, OurgridMeterAsset.CHALLENGE_EARNINGS.name, earnings)
                    }
                }
        })

rules.add()
        .priority(8)
        .name("Calculate baseline power rule")
        .when({ facts ->
            // Rule triggers
            boolean triggerRule = false
            long currentMillis = facts.clock.currentTimeMillis

            // Trigger rule at 1:00am
            String dateCurrent = dateOnlyFormat.format(new Date(currentMillis - 3600000L))

            if (dateCurrent != previousDateRule8) {
                facts.bind("dateFromStr", previousDateRule8)
                facts.bind("dateToStr", dateCurrent)
                previousDateRule8 = dateCurrent
                triggerRule = true
            }

            return triggerRule
        })
        .then({ facts ->
            def dateFromStr = facts.bound("dateFromStr") as String
            def dateToStr = facts.bound("dateToStr") as String

            // Find all relevant children's attributes
            def children = facts.matchAssetState(
                    new AssetQuery()
                            .parents(parentMeterAssetId)
                            .types(OurgridMeterAsset)
                            .attributeNames(OurgridMeterAsset.POWER.name)
            ).toList()

            // Stop if group is empty
            if (children.isEmpty()) {
                return
            }

            // Create a map using child asset ID as key and corresponding attributes as value
            Map<String, Map<Object, Object>> childrenAttributes = [:].withDefault { [:].withDefault { null } }

            children.each { attributeInfo ->
                String assetId = attributeInfo.id
                String attributeName = attributeInfo.name
                def value = attributeInfo.value.orElse(null)

                childrenAttributes[assetId][attributeName] = value
            }

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")

            childrenAttributes.forEach { assetId, values ->
                // Get power data-points of previous day
                TreeMap<String, Double> powerDatapoints = getDatabaseDatapoints("asset_datapoint", assetId, OurgridMeterAsset.POWER.name, dateFromStr, dateToStr)

                // Find power data-points between 0:00am and 4:00am, and between 11:00pm and 12:00pm
                def powerDataPointsNightTime = powerDatapoints.findAll { timestamp, power ->
                    // Handle different timestamp formats from the database
                    long timestampMillis = sdf.parse(timestamp).getTime()
                    LocalTime localTime = Instant.ofEpochMilli(timestampMillis).atZone(ZoneId.systemDefault()).toLocalTime()
                    localTime.isBefore(LocalTime.of(4, 0)) || localTime.isAfter(LocalTime.of(23, 0))
                }

                // Calculate baseline power by finding the minimum power value between 0:00am and 4:00am, or between 11:00pm and 12:00pm
                def powerBaselineEntry = null as Map.Entry<String, Double>

                if (!powerDataPointsNightTime.isEmpty()) {
                    powerBaselineEntry = powerDataPointsNightTime.entrySet().min { it.value }
                }

                if (powerBaselineEntry != null) {
                    def powerBaseline = powerBaselineEntry.value.round() as Double

                    // Update power baseline attribute
                    assets.dispatch(assetId, OurgridMeterAsset.POWER_BASELINE.name, powerBaseline)
                }
            }
        })


private double calculateSolarIrradiance(long timestampMillis, double latitude, double longitude) {
    // Date-time
    LocalDateTime dateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(timestampMillis), ZoneId.systemDefault())
    int dayOfYear = dateTime.getDayOfYear()

    // Declination angle
    double declination = 23.44 * Math.sin(Math.toRadians((360.0 / 365.0) * (dayOfYear - 81)))
    double declinationRadians = Math.toRadians(declination)

    // Hour angle - Assumption: hour angle is 0 at solar noon (maximum daily irradiance)
    double hourAngle = 0
    double hourAngleRadians = Math.toRadians(hourAngle)

    // Zenith angle
    double latitudeRadians = Math.toRadians(latitude)
    double cosZenithAngle = Math.sin(latitudeRadians) * Math.sin(declinationRadians) + Math.cos(latitudeRadians) * Math.cos(declinationRadians) * Math.cos(hourAngleRadians)
    double zenithAngleRadians = Math.acos(cosZenithAngle)
    double zenithAngle = Math.toDegrees(zenithAngleRadians)

    // Top of atmosphere solar irradiance
    double solarConstant = 1361 // Watt/m2
    double topOfAtmosphereSolarIrradiance = solarConstant * (1 + 0.034 * Math.cos(2 * Math.PI * (dayOfYear - 3) / 365.0))

    // Air mass
    double airMass = 1 / (Math.cos(zenithAngleRadians) + 0.50572 * Math.pow((96.07995 - zenithAngle), -1.6364))

    // Clear sky optical depth ~ 0.2-0.3
    double opticalDepth = 0.25

    // Solar irradiance - Assumption: panel is perpendicular to sun at powerMinimum, I = I_s * cos(0) * T_air
    double solarIrradiance = (topOfAtmosphereSolarIrradiance * Math.exp(-airMass * opticalDepth)).round()

//    LOG.info("dateTime: " + dateTime.toString())
//    LOG.info("dayOfYear: " + dayOfYear.toString())
//    LOG.info("latitude: " + (Math.round(latitude * 100) / 100).toString())
//    LOG.info("longitude: " + (Math.round(longitude * 100) / 100).toString())
//    LOG.info("declination: " + (Math.round(declination * 100) / 100).toString())
//    LOG.info("zenithAngle: " + (Math.round(zenithAngle * 100) / 100).toString())
//    LOG.info("altitudeAngle: " + (90 - Math.round(zenithAngle * 100) / 100).toString())
//    LOG.info("topOfAtmosphereSolarIrradiance: " + (Math.round(topOfAtmosphereSolarIrradiance * 100) / 100).toString())
//    LOG.info("airMass: " + (Math.round(airMass * 100) / 100).toString())
//    LOG.info("solarIrradiance: " + solarIrradiance.toString())

    return solarIrradiance
}

private Map<String, Integer> calculatePowerLimits(RulesFacts facts, def childrenAttributes, String challengesAssetId) {
    Assets assets = binding.assets
    Logger LOG = binding.LOG

    Map<String, Integer> powerLimitMap = new HashMap<>()

    // Get attribute values
    def defaultPowerLimitMethod = facts.matchFirstAssetState(
            new AssetQuery()
                    .ids(challengesAssetId)
                    .attributeName(OurgridChallengesAsset.CHALLENGE_DEFAULT_POWER_LIMIT_METHOD.name)
    ).flatMap { it.value }.orElse(null) as String

    if (defaultPowerLimitMethod == null) {
        defaultPowerLimitMethod = "ladder"
        assets.dispatch(challengesAssetId, OurgridChallengesAsset.CHALLENGE_DEFAULT_POWER_LIMIT_METHOD.name, defaultPowerLimitMethod)
    }

    def powerLimitTarget = facts.matchFirstAssetState(
            new AssetQuery()
                    .ids(challengesAssetId)
                    .attributeName(OurgridChallengesAsset.CHALLENGE_POWER_LIMIT_TARGET.name)
    ).flatMap { it.value }.orElse(null) as Integer

    if (powerLimitTarget == null) {
        powerLimitTarget = 2000
        assets.dispatch(challengesAssetId, OurgridChallengesAsset.CHALLENGE_POWER_LIMIT_TARGET.name, powerLimitTarget)
    }

    def powerLimitMinimum = facts.matchFirstAssetState(
            new AssetQuery()
                    .ids(challengesAssetId)
                    .attributeName(OurgridChallengesAsset.CHALLENGE_POWER_LIMIT_MINIMUM.name)
    ).flatMap { it.value }.orElse(null) as Integer

    if (powerLimitMinimum == null) {
        powerLimitMinimum = 2000
        assets.dispatch(challengesAssetId, OurgridChallengesAsset.CHALLENGE_POWER_LIMIT_MINIMUM.name, powerLimitMinimum)
    }

    def powerLimitMaximum = facts.matchFirstAssetState(
            new AssetQuery()
                    .ids(challengesAssetId)
                    .attributeName(OurgridChallengesAsset.CHALLENGE_POWER_LIMIT_MAXIMUM.name)
    ).flatMap { it.value }.orElse(null) as Integer

    if (powerLimitMaximum == null) {
        powerLimitMaximum = 3000
        assets.dispatch(challengesAssetId, OurgridChallengesAsset.CHALLENGE_POWER_LIMIT_MAXIMUM.name, powerLimitMaximum)
    }

    def powerLimitInterval = facts.matchFirstAssetState(
            new AssetQuery()
                    .ids(challengesAssetId)
                    .attributeName(OurgridChallengesAsset.CHALLENGE_POWER_LIMIT_INTERVAL.name)
    ).flatMap { it.value }.orElse(null) as Integer

    if (powerLimitInterval == null) {
        powerLimitInterval = 100
        assets.dispatch(challengesAssetId, OurgridChallengesAsset.CHALLENGE_POWER_LIMIT_INTERVAL.name, powerLimitInterval)
    }

    def powerLimitProDegPercentage = facts.matchFirstAssetState(
            new AssetQuery()
                    .ids(challengesAssetId)
                    .attributeName(OurgridChallengesAsset.CHALLENGE_POWER_LIMIT_PROMOTION_PERCENTAGE.name)
    ).flatMap { it.value }.orElse(null) as Integer

    if (powerLimitProDegPercentage == null) {
        powerLimitProDegPercentage = 20
        assets.dispatch(challengesAssetId, OurgridChallengesAsset.CHALLENGE_POWER_LIMIT_PROMOTION_PERCENTAGE.name, powerLimitProDegPercentage)
    }

    def challengeDuration = facts.matchFirstAssetState(
            new AssetQuery()
                    .ids(challengesAssetId)
                    .attributeName(OurgridChallengesAsset.CHALLENGE_DURATION.name)
    ).flatMap { it.value }.orElse(null) as Long

    if (challengeDuration == null) {
        challengeDuration = 60
        assets.dispatch(challengesAssetId, OurgridChallengesAsset.CHALLENGE_DURATION.name, challengeDuration)
    }

    def challengePointInterval = facts.matchFirstAssetState(
            new AssetQuery()
                    .ids(challengesAssetId)
                    .attributeName(OurgridChallengesAsset.CHALLENGE_EARN_POINT_INTERVAL.name)
    ).flatMap { it.value }.orElse(null) as Long

    if (challengePointInterval == null) {
        challengePointInterval = 6
        assets.dispatch(challengesAssetId, OurgridChallengesAsset.CHALLENGE_EARN_POINT_INTERVAL.name, challengePointInterval)
    }

    // Maximum number of points that can be earned during challenge
    int maxPoints = (int) Math.ceil(challengeDuration / challengePointInterval)

    int degradationZone = (int) Math.round(maxPoints * powerLimitProDegPercentage / 100)
    int promotionZone = (int) Math.round(maxPoints * (1 - powerLimitProDegPercentage / 100))

    childrenAttributes.forEach { assetId, values ->
        String powerLimitMethod = values.get(OurgridMeterAsset.CHALLENGE_POWER_LIMIT_METHOD.name).toString()
        def pointsPreviousChallenge = values.get(OurgridMeterAsset.CHALLENGE_POINTS_PER_CHALLENGE.name)
        def powerLimit = values.get(OurgridMeterAsset.CHALLENGE_POWER_LIMIT.name)

        // Set power limit method
        if (powerLimitMethod == "null") {
            powerLimitMethod = defaultPowerLimitMethod.toString()

            assets.dispatch(assetId.toString(), OurgridMeterAsset.CHALLENGE_POWER_LIMIT_METHOD.name, powerLimitMethod)
        }

        // Calculate power limit based on method
        switch (powerLimitMethod) {
            case "constant":
                powerLimit = powerLimitTarget
                break
            case "ladder":
                // Reset power limit and points when out of current range
                if (powerLimit == null || powerLimit < powerLimitMinimum || powerLimit > powerLimitMaximum) {
                    // Check if power limit target is in current range
                    if (powerLimitTarget < powerLimitMinimum) {
                        powerLimit = powerLimitMinimum
                        LOG.info("Current power limit below power limit minimum for asset ID: '" + assetId.toString() + "'. Power limit is set to: " + powerLimit.toString() + "W")
                    } else if (powerLimitTarget > powerLimitMaximum) {
                        powerLimit = powerLimitMaximum
                        LOG.info("Current power limit above power limit maximum for asset ID: '" + assetId.toString() + "'. Power limit is set to: " + powerLimit.toString() + "W")
                    } else {
                        powerLimit = powerLimitTarget
                    }
                    pointsPreviousChallenge = null
                }

                // Adjust power limit based on earned points in previous challenge
                if (pointsPreviousChallenge != null && pointsPreviousChallenge != -1) {
                    if (pointsPreviousChallenge < degradationZone && powerLimit < powerLimitMaximum) {
                        powerLimit += powerLimitInterval
                    } else if (pointsPreviousChallenge > promotionZone && powerLimit > powerLimitMinimum) {
                        powerLimit -= powerLimitInterval
                    }
                }
                break
        }
        powerLimitMap.put(assetId.toString(), powerLimit)
        assets.dispatch(assetId.toString(), OurgridMeterAsset.CHALLENGE_POWER_LIMIT.name, powerLimit)
    }
    return powerLimitMap
}


private Double calculateForecastValue(long timestamp, String assetId, String attributeName) {
    // Time range for fetching data-points from database. 60 minutes: 60x60*1000 = 3600000ms
    long timestampFrom = timestamp - 3600000
    long timestampTo = timestamp + 3600000

    // Create desired date-time format
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")

    // Convert timestamp to a Date object
    Date dateTime = new Date(timestamp)
    Date dateTimeFrom = new Date(timestampFrom)
    Date dateTimeTo = new Date(timestampTo)

    // Format Date object as a string
    String dateTimeStr = sdf.format(dateTime)
    String dateTimeFromStr = sdf.format(dateTimeFrom)
    String dateTimeToStr = sdf.format(dateTimeTo)

    // Get forecast data-points from database
    TreeMap<String, Double> forecast = getDatabaseDatapoints("asset_predicted_datapoint", assetId, attributeName, dateTimeFromStr, dateTimeToStr)

    // List with desired date-times for forecast interpolation
    List<String> dateTimeList = new ArrayList<>()
    dateTimeList.add(dateTimeStr)

    // Interpolate forecast
    TreeMap<String, Double> interpolatedForecast = interpolateForecast(dateTimeList, sdf, forecast)

    Double interpolatedValue = interpolatedForecast.get(dateTimeStr)

    return interpolatedValue
}

private Object[] calculateNetPowerForecast(long currentTimestamp, String solarAssetId, String parentDistrictAssetId, TreeMap<String, Double> datapointsPrevious, TreeMap<String, Double> consumptionHistorical) {
    // Desired forecast period in milliseconds
    long periodMillis = 24 * 60 * 60 * 1000
    // Desired interpolation interval in milliseconds
    long intervalMillis = 15 * 60 * 1000

    // Time range for fetching data-points from database. 15 minutes: 15x60*1000 = 900000ms (Solar forecast interval)
    long timestampDelete = currentTimestamp - periodMillis
    long timestampFrom = currentTimestamp - 900000
    long timestampTo = currentTimestamp + 900000 + periodMillis

    // Create desired date-time format
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")

    // Convert timestamp to a Date object
    Date dateTimeDelete = new Date(timestampDelete)
    Date dateTimeFrom = new Date(timestampFrom)
    Date dataTimeTo = new Date(timestampTo)

    // Format Date object as a string
    String dateTimeDeleteStr = sdf.format(dateTimeDelete)
    String dateTimeFromStr = sdf.format(dateTimeFrom)
    String dateTimeToStr = sdf.format(dataTimeTo)

    // Delete old forecast data-points
    deleteDatabaseDatapoints(parentDistrictAssetId, "powerDistrict", dateTimeDeleteStr)

    // Get forecast data-points from database
    TreeMap<String, Double> solarForecast = getDatabaseDatapoints("asset_predicted_datapoint", solarAssetId, "powerForecast", dateTimeFromStr, dateTimeToStr)
    TreeMap<String, Double> consumptionForecast = getDatabaseDatapoints("asset_predicted_datapoint", parentDistrictAssetId, "powerConsumptionDistrict", dateTimeFromStr, dateTimeToStr)

    // Find first non-null entry in consumption forecast
    for (Map.Entry<String, Double> entry : consumptionForecast.entrySet()) {
        String key = entry.getKey()
        Double value = entry.getValue()

        if (value != null) {
            consumptionHistorical.put(key, value)
            break
        }
    }

    // Delete historical consumption forecast data-points locally
    consumptionHistorical.entrySet().removeIf(entry -> entry.getKey() < dateTimeDeleteStr)

    // Add current day historical data-points to consumption forecast locally
    consumptionForecast.putAll(consumptionHistorical)

    // List with desired date-times for forecast interpolation
    List<String> dateTimeList = new ArrayList<>()

    // Calculate start timestamp, rounded down to the nearest time interval
    long roundedTimestamp = currentTimestamp - (currentTimestamp % (intervalMillis))

    for (long i = roundedTimestamp; i <= roundedTimestamp + periodMillis; i += intervalMillis) {
        Date dateTime = new Date(i)
        String dateTimeStr = sdf.format(dateTime)
        dateTimeList.add(dateTimeStr)
    }

    // Interpolate forecasts
    TreeMap<String, Double> interpolatedSolar = interpolateForecast(dateTimeList, sdf, solarForecast)
    TreeMap<String, Double> interpolatedConsumption = interpolateForecast(dateTimeList, sdf, consumptionForecast)

    // Calculate net power forecast
    TreeMap<String, Double> netPowerForecast = new TreeMap<>()

    for (String key : dateTimeList) {
        long timestamp = sdf.parse(key).getTime()

        // Update only future data points
        if (timestamp > currentTimestamp) {
            Double solarValue = interpolatedSolar.get(key)
            Double consumptionValue = interpolatedConsumption.get(key)

            if (solarValue != null && consumptionValue != null) {
                Double sum = solarValue + consumptionValue
                netPowerForecast.put(key, sum)
            } else if (solarValue != null) {
                netPowerForecast.put(key, solarValue)
            } else if (consumptionValue != null) {
                netPowerForecast.put(key, consumptionValue)
            }
        }
    }

    // List with new or updated forecast values
    TreeMap<String, Double> newEntries = new TreeMap<>()

    // Compare current to previous data-points
    for (Map.Entry<String, Double> entry : netPowerForecast.entrySet()) {
        String key = entry.getKey()
        Double currentValue = entry.getValue()
        Double previousValue = datapointsPrevious.get(key)

        // Add new or updated data-points
        if (previousValue == null || currentValue != previousValue) {
            newEntries.put(key, currentValue)
        }
    }

    // Upsert forecast data-points into database
    if (newEntries != null) {
        upsertDatabaseDatapoints(parentDistrictAssetId, "powerDistrict", newEntries)
    }

    datapointsPrevious = new TreeMap<>(netPowerForecast)

    return new Object[]{datapointsPrevious, consumptionHistorical}
}

private TreeMap<String, Double> interpolateForecast(List<String> dateTimeList, SimpleDateFormat sdf, TreeMap<String, Double> forecast) {
    Logger LOG = binding.LOG

    TreeMap<String, Double> interpolatedValues = new TreeMap<>()

    for (String dateTimeStr : dateTimeList) {
        // Find the two closest date-times in forecast
        String lowerDateTimeStr = forecast.floorKey(dateTimeStr)
        String upperDateTimeStr = forecast.ceilingKey(dateTimeStr)

        if (lowerDateTimeStr != null && upperDateTimeStr != null) {
            if (lowerDateTimeStr == upperDateTimeStr) {
                // Exact value found in forecast
                Double exactValue = forecast.get(dateTimeStr)

                interpolatedValues.put(dateTimeStr, exactValue)
            } else {
                try {
                    Date dateTime = sdf.parse(dateTimeStr)
                    Date lowerDateTime = sdf.parse(lowerDateTimeStr)
                    Date upperDateTime = sdf.parse(upperDateTimeStr)

                    // Calculate interpolation factor
                    long diff1 = dateTime.getTime() - lowerDateTime.getTime()
                    long diff2 = upperDateTime.getTime() - lowerDateTime.getTime()
                    double factor = (double) diff1 / diff2

                    // Interpolate value
                    Double lowerValue = forecast.get(lowerDateTimeStr)
                    Double upperValue = forecast.get(upperDateTimeStr)
                    Double interpolatedValue = lowerValue + factor * (upperValue - lowerValue)

                    interpolatedValues.put(dateTimeStr, interpolatedValue)
                } catch (Exception e) {
                    LOG.warning("Failed interpolation; Exception: " + e)
                }
            }
        } else {
            interpolatedValues.put(dateTimeStr, null)
        }
    }
    return interpolatedValues
}


private TreeMap<String, Double> getDatabaseDatapoints(String tableName, String assetId, String attributeName, String dateTimeFromStr, String dateTimeToStr) {
    Logger LOG = binding.LOG

    TreeMap<String, Double> datapoints = new TreeMap<>()

    try (Connection connection = connectToDatabase()
         Statement statement = connection.createStatement()) {

        String query =
                "SELECT timestamp, value " +
                        "FROM " + tableName + " " +
                        "WHERE entity_id = '" + assetId + "' " +
                        "AND attribute_name = '" + attributeName + "' " +
                        "AND timestamp BETWEEN '" + dateTimeFromStr + "' AND '" + dateTimeToStr + "' " +
                        "ORDER BY timestamp ASC;"

        try (ResultSet resultSet = statement.executeQuery(query)) {
            while (resultSet.next()) {
                String value1 = resultSet.getString("timestamp")
                Double value2 = resultSet.getDouble("value")

                datapoints.put(value1, value2)
            }
        }
    } catch (SQLException e) {
        LOG.warning("Failed to obtain data-points from database; Exception: " + e)
    }
    return datapoints
}

private void upsertDatabaseDatapoints(String assetId, String attributeName, TreeMap<String, Double> datapoints) {
    Logger LOG = binding.LOG

    try (Connection connection = connectToDatabase()) {
        String query =
                "INSERT INTO asset_predicted_datapoint (entity_id, attribute_name, value, timestamp) VALUES (?, ?, ?, ?)" +
                        "ON CONFLICT (entity_id, attribute_name, timestamp) DO UPDATE " +
                        "SET value = EXCLUDED.value"

        PreparedStatement preparedStatement = connection.prepareStatement(query)

        for (Map.Entry<String, Double> entry : datapoints.entrySet()) {
            String key = entry.getKey()
            Double value = entry.getValue()

            PGobject pgJsonValue = new PGobject()
            pgJsonValue.setType("jsonb")
            pgJsonValue.setValue(ValueUtil.asJSON(value).orElse("null"))

            preparedStatement.setString(1, assetId)
            preparedStatement.setString(2, attributeName)
            preparedStatement.setObject(3, pgJsonValue)
            preparedStatement.setTimestamp(4, Timestamp.valueOf(key))

            preparedStatement.addBatch()
        }

        int[] affectedRows = preparedStatement.executeBatch()

        for (int affectedRow : affectedRows) {
            if (affectedRow != 1) {
                LOG.warning("Failed to insert row into database")
            }
        }
    } catch (SQLException e) {
        LOG.warning("Failed to insert data-points into database; Exception: " + e)
    }
}

private void deleteDatabaseDatapoints(String assetId, String attributeName, String timestamp) {
    Logger LOG = binding.LOG

    try (Connection connection = connectToDatabase()) {
        String query =
                "DELETE FROM asset_predicted_datapoint " +
                        "WHERE entity_id = '" + assetId + "' " +
                        "AND attribute_name = '" + attributeName + "' " +
                        "AND timestamp < '" + timestamp + "';"

        PreparedStatement preparedStatement = connection.prepareStatement(query)

        int rowsDeleted = preparedStatement.executeUpdate()
    } catch (SQLException e) {
        LOG.warning("Failed to delete data-points from database; Exception: " + e)
    }
}

private Connection connectToDatabase() throws SQLException {
    String dbPort = System.getenv(PersistenceService.OR_DB_PORT)
    String dbHost = System.getenv(PersistenceService.OR_DB_HOST)
    String dbName = System.getenv(PersistenceService.OR_DB_NAME)
    String dbUsername = System.getenv(PersistenceService.OR_DB_USER)
    String dbPassword = System.getenv(PersistenceService.OR_DB_PASSWORD)

    if (dbPort == null) {
        dbPort = PersistenceService.OR_DB_PORT_DEFAULT.toString()
    }
    if (dbHost == null) {
        dbHost = PersistenceService.OR_DB_HOST_DEFAULT
    }
    if (dbName == null) {
        dbName = PersistenceService.OR_DB_NAME_DEFAULT
    }
    if (dbUsername == null) {
        dbUsername = PersistenceService.OR_DB_USER_DEFAULT
    }
    if (dbPassword == null) {
        dbPassword = PersistenceService.OR_DB_PASSWORD_DEFAULT
    }

    String databaseUrl = "jdbc:postgresql://" + dbHost + ":" + dbPort + "/" + dbName

    return DriverManager.getConnection(databaseUrl, dbUsername, dbPassword)
}
