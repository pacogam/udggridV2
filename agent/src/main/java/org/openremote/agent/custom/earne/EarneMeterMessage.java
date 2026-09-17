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
import java.util.StringJoiner;

public class EarneMeterMessage {

  public static class GeoLocation {
    @JsonProperty("Latitude")
    public Double latitude;

    @JsonProperty("Longitude")
    public Double longitude;

    @Override
    public String toString() {
      return new StringJoiner(", ", GeoLocation.class.getSimpleName() + "[", "]")
          .add("latitude=" + latitude)
          .add("longitude=" + longitude)
          .toString();
    }
  }

  public Instant timestamp;
  public String swVersion = "";
  public String deviceId = "";
  public GeoLocation geo;
  public String model = "";
  public Double wifiRSSI;

  @JsonProperty("energy_delivered_tariff1")
  public Double energyDeliveredTariff1;

  @JsonProperty("energy_delivered_tariff2")
  public Double energyDeliveredTariff2;

  @JsonProperty("energy_returned_tariff1")
  public Double energyReturnedTariff1;

  @JsonProperty("energy_returned_tariff2")
  public Double energyReturnedTariff2;

  @JsonProperty("gas_delivered")
  public Double gasDelivered;

  @JsonProperty("power_delivered")
  public Double powerDelivered;

  @JsonProperty("power_returned")
  public Double powerReturned;

  @JsonProperty("voltage_l1")
  public Double voltageL1;

  @JsonProperty("voltage_l2")
  public Double voltageL2;

  @JsonProperty("voltage_l3")
  public Double voltageL3;

  @JsonProperty("current_l1")
  public Double currentL1;

  @JsonProperty("current_l2")
  public Double currentL2;

  @JsonProperty("current_l3")
  public Double currentL3;

  @Override
  public String toString() {
    return new StringJoiner(", ", EarneMeterMessage.class.getSimpleName() + "[", "]")
        .add("timestamp=" + timestamp)
        .add("swVersion='" + swVersion + "'")
        .add("deviceId='" + deviceId + "'")
        .add("geo=" + geo)
        .add("model='" + model + "'")
        .add("wifiRSSI=" + wifiRSSI)
        .add("energyDeliveredTariff1=" + energyDeliveredTariff1)
        .add("energyDeliveredTariff2=" + energyDeliveredTariff2)
        .add("energyReturnedTariff1=" + energyReturnedTariff1)
        .add("energyReturnedTariff2=" + energyReturnedTariff2)
        .add("gasDelivered=" + gasDelivered)
        .add("powerDelivered=" + powerDelivered)
        .add("powerReturned=" + powerReturned)
        .add("voltageL1=" + voltageL1)
        .add("voltageL2=" + voltageL2)
        .add("voltageL3=" + voltageL3)
        .add("currentL1=" + currentL1)
        .add("currentL2=" + currentL2)
        .add("currentL3=" + currentL3)
        .toString();
  }
}
