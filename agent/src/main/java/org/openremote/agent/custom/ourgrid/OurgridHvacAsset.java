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
package org.openremote.agent.custom.ourgrid;

import static org.openremote.agent.custom.ourgrid.OurgridAssetUtil.withAddedMeta;

import jakarta.persistence.Entity;
import org.openremote.model.asset.AssetDescriptor;
import org.openremote.model.asset.impl.ThermostatAsset;
import org.openremote.model.attribute.MetaItem;
import org.openremote.model.value.AttributeDescriptor;
import org.openremote.model.value.MetaItemType;
import org.openremote.model.value.ValueType;

@Entity
public class OurgridHvacAsset extends ThermostatAsset implements OurgridDeviceAsset {

  // New attributes
  public static final AttributeDescriptor<Boolean> ALLOW_AUTOMATIC_CONTROL_BUTTON =
      new AttributeDescriptor<>(
          "allowAutomaticControlButton",
          ValueType.BOOLEAN,
          new MetaItem<>(MetaItemType.ACCESS_RESTRICTED_READ),
          new MetaItem<>(MetaItemType.ACCESS_RESTRICTED_WRITE),
          new MetaItem<>(MetaItemType.RULE_STATE));

  public static final AttributeDescriptor<Boolean> ALLOW_STOP_HEATPUMP_BUTTON =
      new AttributeDescriptor<>(
          "allowStopHeatpumpButton",
          ValueType.BOOLEAN,
          new MetaItem<>(MetaItemType.ACCESS_RESTRICTED_READ),
          new MetaItem<>(MetaItemType.ACCESS_RESTRICTED_WRITE),
          new MetaItem<>(MetaItemType.RULE_STATE));

  public static final AttributeDescriptor<String> DEVICE_ID =
      new AttributeDescriptor<>(
          "deviceId",
          ValueType.TEXT,
          new MetaItem<>(MetaItemType.READ_ONLY),
          new MetaItem<>(MetaItemType.ACCESS_RESTRICTED_READ));

  public static final AttributeDescriptor<String> TIMESTAMP =
      new AttributeDescriptor<>(
          "timestamp",
          ValueType.TEXT,
          new MetaItem<>(MetaItemType.READ_ONLY),
          new MetaItem<>(MetaItemType.ACCESS_RESTRICTED_READ));

  // Updated attributes
  public static final AttributeDescriptor<String> MANUFACTURER =
      ThermostatAsset.MANUFACTURER
          .withMeta(
              withAddedMeta(
                  ThermostatAsset.MANUFACTURER.getMeta(),
                  new MetaItem<>(MetaItemType.ACCESS_RESTRICTED_READ)))
          .withOptional(false);

  public static final AttributeDescriptor<String> MODEL =
      ThermostatAsset.MODEL
          .withMeta(
              withAddedMeta(
                  ThermostatAsset.MODEL.getMeta(),
                  new MetaItem<>(MetaItemType.ACCESS_RESTRICTED_READ)))
          .withOptional(false);

  public static final AssetDescriptor<OurgridHvacAsset> DESCRIPTOR =
      new AssetDescriptor<>("hvac", "f5b324", OurgridHvacAsset.class);

  protected OurgridHvacAsset() {}

  public OurgridHvacAsset(String name) {
    super(name);
  }

  @Override
  public OurgridHvacAsset setDeviceId(String value) {
    getAttributes().getOrCreate(DEVICE_ID).setValue(value);
    return this;
  }
}
