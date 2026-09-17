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
package org.openremote.agent.custom.ourgrid;

import static org.openremote.model.Constants.*;

import jakarta.persistence.Entity;
import java.util.Optional;
import org.openremote.model.asset.Asset;
import org.openremote.model.asset.AssetDescriptor;
import org.openremote.model.attribute.Attribute;
import org.openremote.model.attribute.MetaItem;
import org.openremote.model.reschool.DeviceCharacteristic;
import org.openremote.model.util.ValueUtil;
import org.openremote.model.value.AttributeDescriptor;
import org.openremote.model.value.MetaItemType;
import org.openremote.model.value.ValueDescriptor;
import org.openremote.model.value.ValueType;

@Entity
public class OurgridMeterAsset extends Asset<OurgridMeterAsset> implements OurgridDeviceAsset {

  public static final AttributeDescriptor<Double> CONNECTION_QUALITY =
      new AttributeDescriptor<>(
          "connectionQuality",
          ValueType.POSITIVE_NUMBER,
          new MetaItem<>(MetaItemType.LABEL, "   Connection quality (previous day)"),
          new MetaItem<>(MetaItemType.READ_ONLY),
          new MetaItem<>(MetaItemType.ACCESS_RESTRICTED_READ),
          new MetaItem<>(MetaItemType.RULE_STATE),
          new MetaItem<>(MetaItemType.STORE_DATA_POINTS));

  public enum ConnectionStatusValueType {
    connected,
    disconnected
  }

  public static final ValueDescriptor<OurgridMeterAsset.ConnectionStatusValueType>
      CONNECTION_STATUS_VALUE_TYPE =
          new ValueDescriptor<>(
              "ConnectionStatusValueType", OurgridMeterAsset.ConnectionStatusValueType.class);

  public static final AttributeDescriptor<OurgridMeterAsset.ConnectionStatusValueType>
      CONNECTION_STATUS =
          new AttributeDescriptor<>(
              "connectionStatus",
              CONNECTION_STATUS_VALUE_TYPE,
              new MetaItem<>(MetaItemType.LABEL, "   Connection status"),
              new MetaItem<>(MetaItemType.READ_ONLY),
              new MetaItem<>(MetaItemType.ACCESS_RESTRICTED_READ),
              new MetaItem<>(MetaItemType.RULE_STATE),
              new MetaItem<>(MetaItemType.STORE_DATA_POINTS));

  public enum ChallengeStatusValueType {
    joinChallenge,
    joinedChallenge,
    activeChallenge,
    inactiveChallenge,
    noChallenge
  }

  public static final ValueDescriptor<OurgridMeterAsset.ChallengeStatusValueType>
      CHALLENGE_STATUS_VALUE_TYPE =
          new ValueDescriptor<>(
              "challengeStatusValueType", OurgridMeterAsset.ChallengeStatusValueType.class);

  public static final AttributeDescriptor<OurgridMeterAsset.ChallengeStatusValueType>
      CHALLENGE_STATUS =
          new AttributeDescriptor<>(
              "challengeStatus",
              CHALLENGE_STATUS_VALUE_TYPE,
              new MetaItem<>(MetaItemType.LABEL, "   Challenge status"),
              new MetaItem<>(MetaItemType.READ_ONLY),
              new MetaItem<>(MetaItemType.ACCESS_RESTRICTED_READ),
              new MetaItem<>(MetaItemType.RULE_STATE));

  public static final AttributeDescriptor<Boolean> CHALLENGE_JOIN_BUTTON =
      new AttributeDescriptor<>(
          "challengeJoinButton",
          ValueType.BOOLEAN,
          new MetaItem<>(MetaItemType.LABEL, "    Challenge join button"),
          new MetaItem<>(MetaItemType.ACCESS_RESTRICTED_READ),
          new MetaItem<>(MetaItemType.ACCESS_RESTRICTED_WRITE),
          new MetaItem<>(MetaItemType.RULE_STATE));

  public static final AttributeDescriptor<Integer> CHALLENGE_POINTS =
      new AttributeDescriptor<>(
          "challengePoints",
          ValueType.POSITIVE_INTEGER,
          new MetaItem<>(MetaItemType.LABEL, "  Challenge points (total)"),
          new MetaItem<>(MetaItemType.READ_ONLY),
          new MetaItem<>(MetaItemType.ACCESS_RESTRICTED_READ),
          new MetaItem<>(MetaItemType.RULE_STATE),
          new MetaItem<>(MetaItemType.STORE_DATA_POINTS),
          new MetaItem<>(MetaItemType.DATA_POINTS_MAX_AGE_DAYS, 366));

  public static final AttributeDescriptor<Integer> CHALLENGE_POINTS_CURRENT =
      new AttributeDescriptor<>(
          "challengePointsCurrent",
          ValueType.POSITIVE_INTEGER,
          new MetaItem<>(MetaItemType.LABEL, "  Challenge points (current challenge)"),
          new MetaItem<>(MetaItemType.READ_ONLY),
          new MetaItem<>(MetaItemType.ACCESS_RESTRICTED_READ),
          new MetaItem<>(MetaItemType.RULE_STATE));

  public static final AttributeDescriptor<Integer> CHALLENGE_POINTS_PER_CHALLENGE =
      new AttributeDescriptor<>(
          "challengePointsPerChallenge",
          ValueType.INTEGER,
          new MetaItem<>(MetaItemType.LABEL, "  Challenge points (last challenge)"),
          new MetaItem<>(MetaItemType.READ_ONLY),
          new MetaItem<>(MetaItemType.ACCESS_RESTRICTED_READ),
          new MetaItem<>(MetaItemType.RULE_STATE),
          new MetaItem<>(MetaItemType.STORE_DATA_POINTS),
          new MetaItem<>(MetaItemType.DATA_POINTS_MAX_AGE_DAYS, 366));

  public static final AttributeDescriptor<Integer> CHALLENGES_JOINED =
      new AttributeDescriptor<>(
          "challengesJoined",
          ValueType.POSITIVE_INTEGER,
          new MetaItem<>(MetaItemType.LABEL, "  Challenges joined"),
          new MetaItem<>(MetaItemType.READ_ONLY),
          new MetaItem<>(MetaItemType.ACCESS_RESTRICTED_READ),
          new MetaItem<>(MetaItemType.RULE_STATE),
          new MetaItem<>(MetaItemType.STORE_DATA_POINTS));

  public static final AttributeDescriptor<Double> CHALLENGE_EARNINGS =
      new AttributeDescriptor<>(
          "challengeEarnings",
          ValueType.POSITIVE_NUMBER,
          new MetaItem<>(MetaItemType.LABEL, "  Earnings (€)"),
          new MetaItem<>(MetaItemType.READ_ONLY),
          new MetaItem<>(MetaItemType.ACCESS_RESTRICTED_READ),
          new MetaItem<>(MetaItemType.RULE_STATE),
          new MetaItem<>(MetaItemType.STORE_DATA_POINTS),
          new MetaItem<>(MetaItemType.DATA_POINTS_MAX_AGE_DAYS, 366));

  public static final AttributeDescriptor<Long> CHALLENGE_POINT_TIMER_START =
      new AttributeDescriptor<>(
              "challengePointTimerStart",
              ValueType.LONG,
              new MetaItem<>(MetaItemType.LABEL, "  Challenge point timer start"),
              new MetaItem<>(MetaItemType.READ_ONLY),
              new MetaItem<>(MetaItemType.ACCESS_RESTRICTED_READ),
              new MetaItem<>(MetaItemType.RULE_STATE),
              new MetaItem<>(MetaItemType.STORE_DATA_POINTS))
          .withUnits(UNITS_MILLI, UNITS_SECOND);

  public static final AttributeDescriptor<Double> CHALLENGE_POWER_LIMIT =
      new AttributeDescriptor<>(
              "challengePowerLimit",
              ValueType.NUMBER,
              new MetaItem<>(MetaItemType.LABEL, "  Challenge power limit"),
              new MetaItem<>(MetaItemType.READ_ONLY),
              new MetaItem<>(MetaItemType.ACCESS_RESTRICTED_READ),
              new MetaItem<>(MetaItemType.RULE_STATE),
              new MetaItem<>(MetaItemType.STORE_DATA_POINTS),
              new MetaItem<>(MetaItemType.DATA_POINTS_MAX_AGE_DAYS, 366))
          .withUnits(UNITS_WATT);

  public enum ChallengePowerLimitMeterValueType {
    manual,
    constant,
    ladder
  }

  public static final ValueDescriptor<OurgridMeterAsset.ChallengePowerLimitMeterValueType>
      CHALLENGE_POWER_LIMIT_METER_VALUE_TYPE =
          new ValueDescriptor<>(
              "ChallengePowerLimitMeterValueType",
              OurgridMeterAsset.ChallengePowerLimitMeterValueType.class);

  public static final AttributeDescriptor<OurgridMeterAsset.ChallengePowerLimitMeterValueType>
      CHALLENGE_POWER_LIMIT_METHOD =
          new AttributeDescriptor<>(
              "challengePowerLimitMethod",
              CHALLENGE_POWER_LIMIT_METER_VALUE_TYPE,
              new MetaItem<>(MetaItemType.LABEL, "  Challenge power limit method"),
              new MetaItem<>(MetaItemType.RULE_STATE));

  public static final AttributeDescriptor<Double> PEAK_POINTS =
      new AttributeDescriptor<>(
          "peakPoints",
          ValueType.POSITIVE_NUMBER,
          new MetaItem<>(MetaItemType.LABEL, "  Peak points"),
          new MetaItem<>(MetaItemType.READ_ONLY),
          new MetaItem<>(MetaItemType.ACCESS_RESTRICTED_READ),
          new MetaItem<>(MetaItemType.RULE_STATE),
          new MetaItem<>(MetaItemType.STORE_DATA_POINTS),
          new MetaItem<>(MetaItemType.DATA_POINTS_MAX_AGE_DAYS, 366));

  public static final AttributeDescriptor<Double> TOTAL_POINTS =
      new AttributeDescriptor<>(
          "totalPoints",
          ValueType.POSITIVE_NUMBER,
          new MetaItem<>(MetaItemType.LABEL, "  Total points (peak + challenge)"),
          new MetaItem<>(MetaItemType.READ_ONLY),
          new MetaItem<>(MetaItemType.ACCESS_RESTRICTED_READ),
          new MetaItem<>(MetaItemType.RULE_STATE),
          new MetaItem<>(MetaItemType.STORE_DATA_POINTS),
          new MetaItem<>(MetaItemType.DATA_POINTS_MAX_AGE_DAYS, 366));

  public static final AttributeDescriptor<String> HOUSEHOLD_ENERGY_CHARACTERISTICS =
      new AttributeDescriptor<>(
          "householdEnergyCharacteristics",
          ValueType.TEXT,
          new MetaItem<>(MetaItemType.LABEL, " Household energy characteristics"),
          new MetaItem<>(MetaItemType.ACCESS_RESTRICTED_READ),
          new MetaItem<>(MetaItemType.ACCESS_RESTRICTED_WRITE));

  public static final AttributeDescriptor<String> DEVICE_ID =
      new AttributeDescriptor<>(
          "deviceId",
          ValueType.TEXT,
          new MetaItem<>(MetaItemType.LABEL, " Earn-E device ID"),
          new MetaItem<>(MetaItemType.READ_ONLY),
          new MetaItem<>(MetaItemType.ACCESS_RESTRICTED_READ));

  public static final AttributeDescriptor<String> SMARTMETER_MODEL =
      new AttributeDescriptor<>(
          "smartmeterModel",
          ValueType.TEXT,
          new MetaItem<>(MetaItemType.LABEL, " Smart-meter model"),
          new MetaItem<>(MetaItemType.READ_ONLY),
          new MetaItem<>(MetaItemType.ACCESS_RESTRICTED_READ));

  public static final AttributeDescriptor<String> TIMESTAMP =
      new AttributeDescriptor<>(
          "timestamp",
          ValueType.TEXT,
          new MetaItem<>(MetaItemType.LABEL, " Timestamp"),
          new MetaItem<>(MetaItemType.READ_ONLY),
          new MetaItem<>(MetaItemType.ACCESS_RESTRICTED_READ));

  public static final AttributeDescriptor<String> SOFTWARE_VERSION =
      new AttributeDescriptor<>(
          "softwareVersion",
          ValueType.TEXT,
          new MetaItem<>(MetaItemType.LABEL, " Earn-E device software version"),
          new MetaItem<>(MetaItemType.READ_ONLY),
          new MetaItem<>(MetaItemType.ACCESS_RESTRICTED_READ));

  public static final AttributeDescriptor<Double> WIFI_SIGNAL =
      new AttributeDescriptor<>(
              "wifiSignal",
              ValueType.NUMBER,
              new MetaItem<>(MetaItemType.LABEL, "WiFi signal"),
              new MetaItem<>(MetaItemType.READ_ONLY),
              new MetaItem<>(MetaItemType.ACCESS_RESTRICTED_READ),
              new MetaItem<>(MetaItemType.STORE_DATA_POINTS))
          .withUnits(UNITS_DECIBEL, UNITS_MILLI);

  public static final AttributeDescriptor<Double> POWER =
      new AttributeDescriptor<>(
              "power",
              ValueType.NUMBER,
              new MetaItem<>(MetaItemType.LABEL, "Power"),
              new MetaItem<>(MetaItemType.READ_ONLY),
              new MetaItem<>(MetaItemType.ACCESS_RESTRICTED_READ),
              new MetaItem<>(MetaItemType.RULE_STATE),
              new MetaItem<>(MetaItemType.STORE_DATA_POINTS))
          .withUnits(UNITS_WATT);

  public static final AttributeDescriptor<Double> POWER_IMPORT =
      new AttributeDescriptor<>(
              "powerImport",
              ValueType.NUMBER,
              new MetaItem<>(MetaItemType.LABEL, "Power import"),
              new MetaItem<>(MetaItemType.READ_ONLY),
              new MetaItem<>(MetaItemType.ACCESS_RESTRICTED_READ),
              new MetaItem<>(MetaItemType.RULE_STATE))
          .withUnits(UNITS_KILO, UNITS_WATT);

  public static final AttributeDescriptor<Double> POWER_EXPORT =
      new AttributeDescriptor<>(
              "powerExport",
              ValueType.NUMBER,
              new MetaItem<>(MetaItemType.LABEL, "Power export"),
              new MetaItem<>(MetaItemType.READ_ONLY),
              new MetaItem<>(MetaItemType.ACCESS_RESTRICTED_READ),
              new MetaItem<>(MetaItemType.RULE_STATE))
          .withUnits(UNITS_KILO, UNITS_WATT);

  public static final AttributeDescriptor<Double> ENERGY_IMPORT_TOTAL =
      new AttributeDescriptor<>(
              "energyImportTotal",
              ValueType.NUMBER,
              new MetaItem<>(MetaItemType.LABEL, "Total energy imported"),
              new MetaItem<>(MetaItemType.READ_ONLY),
              new MetaItem<>(MetaItemType.ACCESS_RESTRICTED_READ),
              new MetaItem<>(MetaItemType.RULE_STATE))
          .withUnits(UNITS_KILO, UNITS_WATT, UNITS_HOUR);

  public static final AttributeDescriptor<Double> ENERGY_EXPORT_TOTAL =
      new AttributeDescriptor<>(
              "energyExportTotal",
              ValueType.NUMBER,
              new MetaItem<>(MetaItemType.LABEL, "Total energy exported"),
              new MetaItem<>(MetaItemType.READ_ONLY),
              new MetaItem<>(MetaItemType.ACCESS_RESTRICTED_READ),
              new MetaItem<>(MetaItemType.RULE_STATE))
          .withUnits(UNITS_KILO, UNITS_WATT, UNITS_HOUR);

  public static final AttributeDescriptor<Double> ENERGY_NET_TOTAL =
      new AttributeDescriptor<>(
              "energyNetTotal",
              ValueType.NUMBER,
              new MetaItem<>(MetaItemType.LABEL, "Total energy (import-export)"),
              new MetaItem<>(MetaItemType.READ_ONLY),
              new MetaItem<>(MetaItemType.ACCESS_RESTRICTED_READ),
              new MetaItem<>(MetaItemType.RULE_STATE))
          .withUnits(UNITS_KILO, UNITS_WATT, UNITS_HOUR);

  public static final AttributeDescriptor<Double> GAS_IMPORT_TOTAL =
      new AttributeDescriptor<>(
              "gasImportTotal",
              ValueType.NUMBER,
              new MetaItem<>(MetaItemType.LABEL, "Total gas imported"),
              new MetaItem<>(MetaItemType.READ_ONLY),
              new MetaItem<>(MetaItemType.ACCESS_RESTRICTED_READ),
              new MetaItem<>(MetaItemType.RULE_STATE))
          .withUnits(UNITS_METRE, UNITS_CUBED);

  public static final AttributeDescriptor<Double> GAS_FLOW_RATE =
      new AttributeDescriptor<>(
              "gasFlowRate",
              ValueType.NUMBER,
              new MetaItem<>(MetaItemType.LABEL, "Gas flow rate"),
              new MetaItem<>(MetaItemType.READ_ONLY),
              new MetaItem<>(MetaItemType.ACCESS_RESTRICTED_READ),
              new MetaItem<>(MetaItemType.RULE_STATE))
          .withUnits(UNITS_METRE, UNITS_CUBED, UNITS_PER, UNITS_MINUTE);

  public static final AttributeDescriptor<Double> POWER_BASELINE =
      new AttributeDescriptor<>(
              "powerBaseline",
              ValueType.NUMBER,
              new MetaItem<>(MetaItemType.LABEL, "Power baseline"),
              new MetaItem<>(MetaItemType.READ_ONLY),
              new MetaItem<>(MetaItemType.ACCESS_RESTRICTED_READ),
              new MetaItem<>(MetaItemType.RULE_STATE),
              new MetaItem<>(MetaItemType.STORE_DATA_POINTS))
          .withUnits(UNITS_WATT);

  public static final AttributeDescriptor<Double> POWER_MINIMUM =
      new AttributeDescriptor<>(
              "powerMinimum",
              ValueType.NUMBER,
              new MetaItem<>(MetaItemType.LABEL, "Power minimum"),
              new MetaItem<>(MetaItemType.READ_ONLY),
              new MetaItem<>(MetaItemType.ACCESS_RESTRICTED_READ),
              new MetaItem<>(MetaItemType.RULE_STATE),
              new MetaItem<>(MetaItemType.STORE_DATA_POINTS))
          .withUnits(UNITS_WATT);

  public static final AttributeDescriptor<Double> ESTIMATED_SOLAR_CAPACITY =
      new AttributeDescriptor<>(
              "estimatedSolarCapacity",
              ValueType.NUMBER,
              new MetaItem<>(MetaItemType.LABEL, "Estimated solar capacity"),
              new MetaItem<>(MetaItemType.READ_ONLY),
              new MetaItem<>(MetaItemType.ACCESS_RESTRICTED_READ),
              new MetaItem<>(MetaItemType.RULE_STATE),
              new MetaItem<>(MetaItemType.STORE_DATA_POINTS))
          .withUnits(UNITS_KILO, UNITS_WATT);

  public static final AttributeDescriptor<Boolean> ESTIMATE_SOLAR_CAPACITY_MANUALLY =
      new AttributeDescriptor<>(
          "estimateSolarCapacityManually",
          ValueType.BOOLEAN,
          new MetaItem<>(MetaItemType.LABEL, "Estimate solar capacity manually"),
          new MetaItem<>(MetaItemType.ACCESS_RESTRICTED_READ),
          new MetaItem<>(MetaItemType.RULE_STATE));

  public static final AssetDescriptor<OurgridMeterAsset> DESCRIPTOR =
      new AssetDescriptor<>("power-plug", "ff9300", OurgridMeterAsset.class);

  protected OurgridMeterAsset() {}

  public OurgridMeterAsset(String name) {
    super(name);
  }

  public Optional<OurgridMeterAsset.ChallengeStatusValueType> getChallengeStatus() {
    return getAttributes().get(CHALLENGE_STATUS).flatMap(Attribute::getValue);
  }

  public OurgridMeterAsset setChallengeJoinStatus(boolean status) {
    getAttributes().getOrCreate(CHALLENGE_JOIN_BUTTON).setValue(status);
    return this;
  }

  public Optional<Integer> getCurrentChallengePoints() {
    return getAttributes().get(CHALLENGE_POINTS_CURRENT).flatMap(Attribute::getValue);
  }

  public Optional<Integer> getChallengesJoined() {
    return getAttributes().get(CHALLENGES_JOINED).flatMap(Attribute::getValue);
  }

  public Optional<DeviceCharacteristic[]> getHouseholdCharacteristics() {
    Optional<String> jsonString =
        getAttributes().get(HOUSEHOLD_ENERGY_CHARACTERISTICS).flatMap(Attribute::getValue);
    if (jsonString.isEmpty()) {
      return Optional.of(new DeviceCharacteristic[0]);
    }
    return ValueUtil.parse(jsonString.get(), DeviceCharacteristic[].class);
  }

  public OurgridMeterAsset setHouseholdCharacteristics(DeviceCharacteristic[] characteristics) {
    Optional<String> jsonString = ValueUtil.asJSON(characteristics);
    getAttributes().getOrCreate(HOUSEHOLD_ENERGY_CHARACTERISTICS).setValue(jsonString.orElse(""));
    return this;
  }

  @Override
  public OurgridMeterAsset setDeviceId(String value) {
    getAttributes().getOrCreate(DEVICE_ID).setValue(value);
    return this;
  }

  public OurgridMeterAsset setSmartmeterModel(String value) {
    getAttributes().getOrCreate(SMARTMETER_MODEL).setValue(value);
    return this;
  }

  public OurgridMeterAsset setSoftwareVersion(String value) {
    getAttributes().getOrCreate(SOFTWARE_VERSION).setValue(value);
    return this;
  }
}
