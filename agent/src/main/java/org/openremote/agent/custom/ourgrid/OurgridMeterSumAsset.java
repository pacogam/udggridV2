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
import org.openremote.model.asset.Asset;
import org.openremote.model.asset.AssetDescriptor;
import org.openremote.model.attribute.MetaItem;
import org.openremote.model.value.AttributeDescriptor;
import org.openremote.model.value.MetaItemType;
import org.openremote.model.value.ValueType;

@Entity
public class OurgridMeterSumAsset extends Asset<OurgridMeterSumAsset> {

  public static final AttributeDescriptor<Double> POWER =
      new AttributeDescriptor<>(
              "power",
              ValueType.NUMBER,
              new MetaItem<>(MetaItemType.LABEL, "Power"),
              new MetaItem<>(MetaItemType.READ_ONLY),
              new MetaItem<>(MetaItemType.RULE_STATE),
              new MetaItem<>(MetaItemType.STORE_DATA_POINTS))
          .withUnits(UNITS_WATT);

  public static final AttributeDescriptor<Double> ENERGY_IMPORT_TOTAL =
      new AttributeDescriptor<>(
              "energyImportTotal",
              ValueType.NUMBER,
              new MetaItem<>(MetaItemType.LABEL, "Total energy imported"),
              new MetaItem<>(MetaItemType.READ_ONLY),
              new MetaItem<>(MetaItemType.RULE_STATE))
          .withUnits(UNITS_KILO, UNITS_WATT, UNITS_HOUR);

  public static final AttributeDescriptor<Double> ENERGY_EXPORT_TOTAL =
      new AttributeDescriptor<>(
              "energyExportTotal",
              ValueType.NUMBER,
              new MetaItem<>(MetaItemType.LABEL, "Total energy exported"),
              new MetaItem<>(MetaItemType.READ_ONLY),
              new MetaItem<>(MetaItemType.RULE_STATE))
          .withUnits(UNITS_KILO, UNITS_WATT, UNITS_HOUR);

  public static final AttributeDescriptor<Double> ENERGY_NET_TOTAL =
      new AttributeDescriptor<>(
              "energyNetTotal",
              ValueType.NUMBER,
              new MetaItem<>(MetaItemType.LABEL, "Total energy (import-export)"),
              new MetaItem<>(MetaItemType.READ_ONLY),
              new MetaItem<>(MetaItemType.RULE_STATE))
          .withUnits(UNITS_KILO, UNITS_WATT, UNITS_HOUR);

  public static final AttributeDescriptor<Double> GAS_IMPORT_TOTAL =
      new AttributeDescriptor<>(
              "gasImportTotal",
              ValueType.NUMBER,
              new MetaItem<>(MetaItemType.LABEL, "Total gas imported"),
              new MetaItem<>(MetaItemType.READ_ONLY),
              new MetaItem<>(MetaItemType.RULE_STATE))
          .withUnits(UNITS_METRE, UNITS_CUBED);

  public static final AttributeDescriptor<Double> GAS_FLOW_RATE =
      new AttributeDescriptor<>(
              "gasFlowRate",
              ValueType.NUMBER,
              new MetaItem<>(MetaItemType.LABEL, "Gas flow rate"),
              new MetaItem<>(MetaItemType.READ_ONLY),
              new MetaItem<>(MetaItemType.RULE_STATE))
          .withUnits(UNITS_METRE, UNITS_CUBED, UNITS_PER, UNITS_MINUTE);

  public static final AttributeDescriptor<Double> ESTIMATED_SOLAR_CAPACITY =
      new AttributeDescriptor<>(
              "estimatedSolarCapacity",
              ValueType.NUMBER,
              new MetaItem<>(MetaItemType.LABEL, "Estimated solar capacity (active meters)"),
              new MetaItem<>(MetaItemType.READ_ONLY),
              new MetaItem<>(MetaItemType.RULE_STATE))
          .withUnits(UNITS_KILO, UNITS_WATT);

  public static final AssetDescriptor<OurgridMeterSumAsset> DESCRIPTOR =
      new AssetDescriptor<>("power-plug-outline", "ff9300", OurgridMeterSumAsset.class);

  protected OurgridMeterSumAsset() {}

  public OurgridMeterSumAsset(String name) {
    super(name);
  }
}
