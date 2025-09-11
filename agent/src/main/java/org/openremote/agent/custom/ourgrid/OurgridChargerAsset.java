/*
 * Copyright 2025, OpenRemote Inc.
 *
 * See the CONTRIBUTORS.txt file in the distribution for a
 * full listing of individual contributors.
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
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.openremote.agent.custom.ourgrid;

import jakarta.persistence.Entity;
import org.openremote.model.asset.AssetDescriptor;
import org.openremote.model.asset.impl.ElectricVehicleAsset;
import org.openremote.model.asset.impl.ElectricityChargerAsset;
import org.openremote.model.asset.impl.ThermostatAsset;
import org.openremote.model.attribute.MetaItem;
import org.openremote.model.value.AttributeDescriptor;
import org.openremote.model.value.MetaItemType;
import org.openremote.model.value.ValueType;

import static org.openremote.agent.custom.ourgrid.OurgridAssetUtil.withAddedMeta;

@Entity
public class OurgridChargerAsset extends ElectricityChargerAsset implements OurgridDeviceAsset {

    // New attributes
    public static final AttributeDescriptor<Boolean> ALLOW_AUTOMATIC_CONTROL_BUTTON = new AttributeDescriptor<>("allowAutomaticControlButton", ValueType.BOOLEAN,
            new MetaItem<>(MetaItemType.ACCESS_RESTRICTED_READ),
            new MetaItem<>(MetaItemType.ACCESS_RESTRICTED_WRITE),
            new MetaItem<>(MetaItemType.RULE_STATE)
    );

    public static final AttributeDescriptor<Boolean> ALLOW_STOP_CHARGING_BUTTON = new AttributeDescriptor<>("allowStopChargingButton", ValueType.BOOLEAN,
            new MetaItem<>(MetaItemType.ACCESS_RESTRICTED_READ),
            new MetaItem<>(MetaItemType.ACCESS_RESTRICTED_WRITE),
            new MetaItem<>(MetaItemType.RULE_STATE)
    );

    public static final AttributeDescriptor<Boolean> CHARGING = new AttributeDescriptor<>("charging", ValueType.BOOLEAN,
            new MetaItem<>(MetaItemType.ACCESS_RESTRICTED_READ),
            new MetaItem<>(MetaItemType.READ_ONLY)
    );

    public static final AttributeDescriptor<String> DEVICE_ID = new AttributeDescriptor<>("deviceId", ValueType.TEXT,
            new MetaItem<>(MetaItemType.READ_ONLY),
            new MetaItem<>(MetaItemType.ACCESS_RESTRICTED_READ)
    );

    public static final AttributeDescriptor<Boolean> START_CHARGING_CAPABLE = new AttributeDescriptor<>("startChargingCapable", ValueType.BOOLEAN,
            new MetaItem<>(MetaItemType.ACCESS_RESTRICTED_READ),
            new MetaItem<>(MetaItemType.READ_ONLY)
    );

    public static final AttributeDescriptor<Boolean> STOP_CHARGING_CAPABLE = new AttributeDescriptor<>("stopChargingCapable", ValueType.BOOLEAN,
            new MetaItem<>(MetaItemType.ACCESS_RESTRICTED_READ),
            new MetaItem<>(MetaItemType.READ_ONLY)
    );

    public static final AttributeDescriptor<String> TIMESTAMP = new AttributeDescriptor<>("timestamp", ValueType.TEXT,
            new MetaItem<>(MetaItemType.READ_ONLY),
            new MetaItem<>(MetaItemType.ACCESS_RESTRICTED_READ)
    );

    // Updated attributes
    public static final AttributeDescriptor<String> MANUFACTURER = ElectricityChargerAsset.MANUFACTURER.withMeta(withAddedMeta(ElectricityChargerAsset.MANUFACTURER.getMeta(),
                    new MetaItem<>(MetaItemType.ACCESS_RESTRICTED_READ)))
            .withOptional(false);

    public static final AttributeDescriptor<String> MODEL = ElectricityChargerAsset.MODEL.withMeta(withAddedMeta(ElectricityChargerAsset.MODEL.getMeta(), new MetaItem<>(
                    MetaItemType.ACCESS_RESTRICTED_READ)))
            .withOptional(false);

    public static final AttributeDescriptor<Double> POWER = ElectricityChargerAsset.POWER.withMeta(withAddedMeta(ElectricityChargerAsset.POWER.getMeta(),
            new MetaItem<>(MetaItemType.ACCESS_RESTRICTED_READ),
            new MetaItem<>(MetaItemType.RULE_STATE),
            new MetaItem<>(MetaItemType.STORE_DATA_POINTS)));

    public static final AttributeDescriptor<Boolean> SUPPORTS_EXPORT = ElectricityChargerAsset.SUPPORTS_EXPORT.withMeta(withAddedMeta(ElectricityChargerAsset.SUPPORTS_EXPORT.getMeta(),
            new MetaItem<>(MetaItemType.ACCESS_RESTRICTED_READ)));

    public static final AttributeDescriptor<Boolean> VEHICLE_CONNECTED = ElectricityChargerAsset.VEHICLE_CONNECTED.withMeta(withAddedMeta(ElectricityChargerAsset.VEHICLE_CONNECTED.getMeta(), new MetaItem<>(
                    MetaItemType.ACCESS_RESTRICTED_READ)));

    public static final AssetDescriptor<OurgridChargerAsset> DESCRIPTOR = new AssetDescriptor<>("ev-station", "8A293D", OurgridChargerAsset.class);

    protected OurgridChargerAsset() {
    }

    public OurgridChargerAsset(String name) {
        super(name);
    }

    @Override
    public OurgridChargerAsset setDeviceId(String value) {
        getAttributes().getOrCreate(DEVICE_ID).setValue(value);
        return this;
    }

}
