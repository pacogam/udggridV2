/*
 * Copyright 2025, OpenRemote Inc.
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
package org.openremote.agent.custom.earne;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.stream.Stream;

public class EarneEnodeUpdateMessage {

  // Common classes

  public abstract static class AbstractAssetUpdate {
    @JsonProperty("id")
    public String deviceId;

    public String userId;
    public String vendor;
    public Boolean isReachable;
    public Instant lastSeen;
    public List<String> scopes;

    protected StringJoiner addToStringJoiner(StringJoiner joiner) {
      return joiner
          .add("deviceId='" + deviceId + "'")
          .add("userId='" + userId + "'")
          .add("vendor='" + vendor + "'")
          .add("isReachable=" + isReachable)
          .add("lastSeen=" + lastSeen)
          .add("scopes=" + scopes);
    }

    @Override
    public String toString() {
      return addToStringJoiner(new StringJoiner(", ", getClass().getSimpleName() + "[", "]"))
          .toString();
    }
  }

  public static class User {
    public String id = "";

    @Override
    public String toString() {
      return new StringJoiner(", ", User.class.getSimpleName() + "[", "]")
          .add("id='" + id + "'")
          .toString();
    }
  }

  public static class Capability {
    public Boolean isCapable;
    public List<String> interventionIds;

    @Override
    public String toString() {
      return new StringJoiner(", ", Capability.class.getSimpleName() + "[", "]")
          .add("isCapable=" + isCapable)
          .add("interventionIds=" + interventionIds)
          .toString();
    }
  }

  public static class MinMaxRange {
    public Integer min;
    public Integer max;

    @Override
    public String toString() {
      return new StringJoiner(", ", MinMaxRange.class.getSimpleName() + "[", "]")
          .add("min=" + min)
          .add("max=" + max)
          .toString();
    }
  }

  // Charger classes

  public static class ChargerInformation {
    public String brand;
    public String model;
    public String serialNumber;
    public Integer year;

    @Override
    public String toString() {
      return new StringJoiner(", ", ChargerInformation.class.getSimpleName() + "[", "]")
          .add("brand='" + brand + "'")
          .add("model='" + model + "'")
          .add("serialNumber='" + serialNumber + "'")
          .add("year=" + year)
          .toString();
    }
  }

  public static class ChargerLocation {
    public String id;
    public Instant lastUpdated;

    @Override
    public String toString() {
      return new StringJoiner(", ", ChargerLocation.class.getSimpleName() + "[", "]")
          .add("id='" + id + "'")
          .add("lastUpdated=" + lastUpdated)
          .toString();
    }
  }

  public static class ChargerChargeState {
    public Boolean isPluggedIn;
    public Boolean isCharging;
    public Double chargeRate;
    public Instant lastUpdated;
    public Integer maxCurrent;
    public String powerDeliveryState;

    @Override
    public String toString() {
      return new StringJoiner(", ", ChargerChargeState.class.getSimpleName() + "[", "]")
          .add("isPluggedIn=" + isPluggedIn)
          .add("isCharging=" + isCharging)
          .add("chargeRate=" + chargeRate)
          .add("lastUpdated=" + lastUpdated)
          .add("maxCurrent=" + maxCurrent)
          .add("powerDeliveryState='" + powerDeliveryState + "'")
          .toString();
    }
  }

  public static class ChargerCapabilities {
    public Capability information;
    public Capability chargeState;
    public Capability startCharging;
    public Capability stopCharging;
    public Capability setMaxCurrent;

    @Override
    public String toString() {
      return new StringJoiner(", ", ChargerCapabilities.class.getSimpleName() + "[", "]")
          .add("information=" + information)
          .add("chargeState=" + chargeState)
          .add("startCharging=" + startCharging)
          .add("stopCharging=" + stopCharging)
          .add("setMaxCurrent=" + setMaxCurrent)
          .toString();
    }
  }

  public static class ChargerUpdate extends AbstractAssetUpdate {
    public ChargerInformation information;
    public ChargerLocation location;
    public ChargerChargeState chargeState;
    public ChargerCapabilities capabilities;

    @Override
    protected StringJoiner addToStringJoiner(StringJoiner joiner) {
      return super.addToStringJoiner(joiner)
          .add("information=" + information)
          .add("location=" + location)
          .add("chargeState=" + chargeState)
          .add("capabilities=" + capabilities);
    }
  }

  // HVAC classes

  public static class HvacInformation {
    public String brand;
    public String model;
    public String displayName;
    public String groupName;
    public String category;

    @Override
    public String toString() {
      return new StringJoiner(", ", HvacInformation.class.getSimpleName() + "[", "]")
          .add("brand='" + brand + "'")
          .add("model='" + model + "'")
          .add("displayName='" + displayName + "'")
          .add("groupName='" + groupName + "'")
          .add("category='" + category + "'")
          .toString();
    }
  }

  public static class HvacLocation {
    public String id;

    @Override
    public String toString() {
      return new StringJoiner(", ", HvacLocation.class.getSimpleName() + "[", "]")
          .add("id='" + id + "'")
          .toString();
    }
  }

  public static class HvacCapabilities {
    public List<String> capableModes;
    public MinMaxRange coolSetpointMinMaxRange;
    public MinMaxRange heatSetpointMinMaxRange;
    public MinMaxRange setpointDifferenceMinMaxRange;
    public Capability setFollowSchedule;
    public Capability setPermanentHold;

    @Override
    public String toString() {
      return new StringJoiner(", ", HvacCapabilities.class.getSimpleName() + "[", "]")
          .add("capableModes=" + capableModes)
          .add("coolSetpointMinMaxRange=" + coolSetpointMinMaxRange)
          .add("heatSetpointMinMaxRange=" + heatSetpointMinMaxRange)
          .add("setpointDifferenceMinMaxRange=" + setpointDifferenceMinMaxRange)
          .add("setFollowSchedule=" + setFollowSchedule)
          .add("setPermanentHold=" + setPermanentHold)
          .toString();
    }
  }

  public static class HvacThermostatState {
    public String mode;
    public Integer heatSetpoint;
    public Integer coolSetpoint;
    public String holdType;
    public Instant lastUpdated;

    @Override
    public String toString() {
      return new StringJoiner(", ", HvacThermostatState.class.getSimpleName() + "[", "]")
          .add("mode='" + mode + "'")
          .add("heatSetpoint=" + heatSetpoint)
          .add("coolSetpoint=" + coolSetpoint)
          .add("holdType='" + holdType + "'")
          .add("lastUpdated=" + lastUpdated)
          .toString();
    }
  }

  public static class HvacTemperatureState {
    public Double currentTemperature;
    public Boolean isActive;
    public Instant lastUpdated;

    @Override
    public String toString() {
      return new StringJoiner(", ", HvacTemperatureState.class.getSimpleName() + "[", "]")
          .add("currentTemperature=" + currentTemperature)
          .add("isActive=" + isActive)
          .add("lastUpdated=" + lastUpdated)
          .toString();
    }
  }

  public static class HvacUpdate extends AbstractAssetUpdate {
    public Double consumptionRate;
    public HvacInformation information;
    public HvacLocation location;
    public HvacCapabilities capabilities;
    public HvacThermostatState thermostatState;
    public HvacTemperatureState temperatureState;

    @Override
    protected StringJoiner addToStringJoiner(StringJoiner joiner) {
      return super.addToStringJoiner(joiner)
          .add("consumptionRate=" + consumptionRate)
          .add("information=" + information)
          .add("location=" + location)
          .add("capabilities=" + capabilities)
          .add("thermostatState=" + thermostatState)
          .add("temperatureState=" + temperatureState);
    }
  }

  // Vehicle classes

  public static class VehicleInformation {
    public String displayName;
    public String vin;
    public String brand;
    public String model;
    public Integer year;

    @Override
    public String toString() {
      return new StringJoiner(", ", VehicleInformation.class.getSimpleName() + "[", "]")
          .add("displayName='" + displayName + "'")
          .add("vin='" + vin + "'")
          .add("brand='" + brand + "'")
          .add("model='" + model + "'")
          .add("year=" + year)
          .toString();
    }
  }

  public static class VehicleChargeState {
    public Long batteryLevel;
    public Long range;
    public Boolean isPluggedIn;
    public Boolean isCharging;
    public Boolean isFullyCharged;
    public Double batteryCapacity;
    public Long chargeLimit;
    public Long chargeRate;
    public Long chargeTimeRemaining;
    public Instant lastUpdated;
    public Long maxCurrent;
    public String powerDeliveryState;
    public String pluggedInChargerId;

    @Override
    public String toString() {
      return new StringJoiner(", ", VehicleChargeState.class.getSimpleName() + "[", "]")
          .add("batteryLevel=" + batteryLevel)
          .add("range=" + range)
          .add("isPluggedIn=" + isPluggedIn)
          .add("isCharging=" + isCharging)
          .add("isFullyCharged=" + isFullyCharged)
          .add("batteryCapacity=" + batteryCapacity)
          .add("chargeLimit=" + chargeLimit)
          .add("chargeRate=" + chargeRate)
          .add("chargeTimeRemaining=" + chargeTimeRemaining)
          .add("lastUpdated=" + lastUpdated)
          .add("maxCurrent=" + maxCurrent)
          .add("powerDeliveryState='" + powerDeliveryState + "'")
          .add("pluggedInChargerId='" + pluggedInChargerId + "'")
          .toString();
    }
  }

  public static class VehicleLocation {
    public String id;
    public Double latitude;
    public Double longitude;
    public Instant lastUpdated;

    @Override
    public String toString() {
      return new StringJoiner(", ", VehicleLocation.class.getSimpleName() + "[", "]")
          .add("id='" + id + "'")
          .add("latitude=" + latitude)
          .add("longitude=" + longitude)
          .add("lastUpdated=" + lastUpdated)
          .toString();
    }
  }

  public static class VehicleSmartChargingPolicy {
    public Boolean isEnabled;
    public String deadline;
    public Long minimumChargeLimit;

    @Override
    public String toString() {
      return new StringJoiner(", ", VehicleSmartChargingPolicy.class.getSimpleName() + "[", "]")
          .add("isEnabled=" + isEnabled)
          .add("deadline='" + deadline + "'")
          .add("minimumChargeLimit=" + minimumChargeLimit)
          .toString();
    }
  }

  public static class VehicleOdometer {
    public Long distance;
    public Instant lastUpdated;

    @Override
    public String toString() {
      return new StringJoiner(", ", VehicleOdometer.class.getSimpleName() + "[", "]")
          .add("distance=" + distance)
          .add("lastUpdated=" + lastUpdated)
          .toString();
    }
  }

  public static class VehicleCapabilities {
    public Capability information;
    public Capability chargeState;
    public Capability location;
    public Capability odometer;
    public Capability setMaxCurrent;
    public Capability startCharging;
    public Capability stopCharging;
    public Capability smartCharging;

    @Override
    public String toString() {
      return new StringJoiner(", ", VehicleCapabilities.class.getSimpleName() + "[", "]")
          .add("information=" + information)
          .add("chargeState=" + chargeState)
          .add("location=" + location)
          .add("odometer=" + odometer)
          .add("setMaxCurrent=" + setMaxCurrent)
          .add("startCharging=" + startCharging)
          .add("stopCharging=" + stopCharging)
          .add("smartCharging=" + smartCharging)
          .toString();
    }
  }

  public static class VehicleUpdate extends AbstractAssetUpdate {
    public VehicleInformation information;
    public VehicleChargeState chargeState;
    public VehicleLocation location;
    public VehicleSmartChargingPolicy smartChargingPolicy;
    public VehicleOdometer odometer;
    public VehicleCapabilities capabilities;

    @Override
    protected StringJoiner addToStringJoiner(StringJoiner joiner) {
      return super.addToStringJoiner(joiner)
          .add("information=" + information)
          .add("chargeState=" + chargeState)
          .add("location=" + location)
          .add("smartChargingPolicy=" + smartChargingPolicy)
          .add("odometer=" + odometer)
          .add("capabilities=" + capabilities);
    }
  }

  // Fields

  public Instant createdAt;
  public String version = "";
  public String event = "";
  public User user;
  public ChargerUpdate charger;
  public HvacUpdate hvac;
  public VehicleUpdate vehicle;
  public List<String> updatedFields;

  public AbstractAssetUpdate getAssetUpdate() {
    List<AbstractAssetUpdate> updates =
        Stream.of(charger, hvac, vehicle).filter(Objects::nonNull).toList();
    if (updates.size() != 1) {
      throw new IllegalStateException(
          "Expected exactly one asset update, got " + updates.size() + ": " + this);
    }
    return updates.getFirst();
  }

  public String getUserId() {
    if (user == null) {
      throw new IllegalStateException("User is null in update message: " + this);
    }
    if (user.id.isBlank()) {
      throw new IllegalStateException("User ID not set in update message: " + this);
    }
    return user.id;
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", EarneEnodeUpdateMessage.class.getSimpleName() + "[", "]")
        .add("createdAt=" + createdAt)
        .add("version='" + version + "'")
        .add("event='" + event + "'")
        .add("user='" + user + "'")
        .add("charger=" + charger)
        .add("hvac=" + hvac)
        .add("vehicle=" + vehicle)
        .add("updatedFields=" + updatedFields)
        .toString();
  }
}
