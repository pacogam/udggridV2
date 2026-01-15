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
import org.openremote.extension.energy.model.ElectricVehicleAsset;
import org.openremote.model.asset.AssetDescriptor;
import org.openremote.model.attribute.MetaItem;
import org.openremote.model.geo.GeoJSONPoint;
import org.openremote.model.value.AttributeDescriptor;
import org.openremote.model.value.MetaItemType;
import org.openremote.model.value.ValueConstraint;
import org.openremote.model.value.ValueType;

import static org.openremote.agent.custom.ourgrid.OurgridAssetUtil.withAddedMeta;
import static org.openremote.model.Constants.UNITS_MINUTE;
import static org.openremote.model.Constants.UNITS_PERCENTAGE;

@Entity
public class OurgridVehicleAsset extends ElectricVehicleAsset implements OurgridDeviceAsset {

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

    public static final AttributeDescriptor<Integer> CHARGE_LIMIT = new AttributeDescriptor<>("chargeLimit", ValueType.POSITIVE_INTEGER,
            new MetaItem<>(MetaItemType.ACCESS_RESTRICTED_READ),
            new MetaItem<>(MetaItemType.READ_ONLY))
            .withUnits(UNITS_PERCENTAGE).withConstraints(new ValueConstraint.Min(0), new ValueConstraint.Max(100));

    public static final AttributeDescriptor<Integer> CHARGE_TIME_REMAINING = new AttributeDescriptor<>("chargeTimeRemaining", ValueType.POSITIVE_INTEGER,
            new MetaItem<>(MetaItemType.ACCESS_RESTRICTED_READ),
            new MetaItem<>(MetaItemType.READ_ONLY))
            .withUnits(UNITS_MINUTE);

    public static final AttributeDescriptor<Boolean> CHARGING = new AttributeDescriptor<>("charging", ValueType.BOOLEAN,
            new MetaItem<>(MetaItemType.ACCESS_RESTRICTED_READ),
            new MetaItem<>(MetaItemType.READ_ONLY)
    );

    public static final AttributeDescriptor<String> DEVICE_ID = new AttributeDescriptor<>("deviceId", ValueType.TEXT,
            new MetaItem<>(MetaItemType.ACCESS_RESTRICTED_READ),
            new MetaItem<>(MetaItemType.READ_ONLY)
    );

    public static final AttributeDescriptor<Integer> MODEL_YEAR = new AttributeDescriptor<>("modelYear", ValueType.POSITIVE_INTEGER,
            new MetaItem<>(MetaItemType.ACCESS_RESTRICTED_READ),
            new MetaItem<>(MetaItemType.READ_ONLY)
    );

    public static final AttributeDescriptor<Boolean> SMART_CHARGING_ENABLED = new AttributeDescriptor<>("smartChargingEnabled", ValueType.BOOLEAN,
            new MetaItem<>(MetaItemType.ACCESS_RESTRICTED_READ),
            new MetaItem<>(MetaItemType.READ_ONLY)
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
            new MetaItem<>(MetaItemType.ACCESS_RESTRICTED_READ),
            new MetaItem<>(MetaItemType.READ_ONLY)
    );

    // Updated attributes

    public static final AttributeDescriptor<Boolean> CHARGER_CONNECTED = ElectricVehicleAsset.CHARGER_CONNECTED.withMeta(withAddedMeta(ElectricVehicleAsset.CHARGER_CONNECTED.getMeta(),
            new MetaItem<>(MetaItemType.ACCESS_RESTRICTED_READ)));

    public static final AttributeDescriptor<String> CHARGER_ID = ElectricVehicleAsset.CHARGER_ID.withMeta(withAddedMeta(ElectricVehicleAsset.CHARGER_ID.getMeta(),
            new MetaItem<>(MetaItemType.ACCESS_RESTRICTED_READ)));

    public static final AttributeDescriptor<Double> ENERGY_CAPACITY = ElectricVehicleAsset.ENERGY_CAPACITY.withMeta(withAddedMeta(ElectricVehicleAsset.ENERGY_CAPACITY.getMeta(),
            new MetaItem<>(MetaItemType.ACCESS_RESTRICTED_READ)));

    public static final AttributeDescriptor<Integer> ENERGY_LEVEL_PERCENTAGE = ElectricVehicleAsset.ENERGY_LEVEL_PERCENTAGE.withMeta(withAddedMeta(ElectricVehicleAsset.ENERGY_LEVEL_PERCENTAGE.getMeta(),
            new MetaItem<>(MetaItemType.ACCESS_RESTRICTED_READ)));

    public static final AttributeDescriptor<GeoJSONPoint> LOCATION = ElectricVehicleAsset.LOCATION.withMeta(withAddedMeta(ElectricVehicleAsset.LOCATION.getMeta(),
            new MetaItem<>(MetaItemType.ACCESS_RESTRICTED_READ)));

    public static final AttributeDescriptor<String> MANUFACTURER = ElectricVehicleAsset.MANUFACTURER.withMeta(withAddedMeta(ElectricVehicleAsset.MANUFACTURER.getMeta(),
                    new MetaItem<>(MetaItemType.ACCESS_RESTRICTED_READ)))
            .withOptional(false);

    public static final AttributeDescriptor<Double> MILEAGE_CHARGED = ElectricVehicleAsset.MILEAGE_CHARGED.withMeta(withAddedMeta(ElectricVehicleAsset.MILEAGE_CHARGED.getMeta(),
            new MetaItem<>(MetaItemType.ACCESS_RESTRICTED_READ)));

    public static final AttributeDescriptor<String> MODEL = ElectricVehicleAsset.MODEL.withMeta(withAddedMeta(ElectricVehicleAsset.MODEL.getMeta(), new MetaItem<>(
                    MetaItemType.ACCESS_RESTRICTED_READ)))
            .withOptional(false);

    public static final AttributeDescriptor<Integer> ODOMETER = ElectricVehicleAsset.ODOMETER.withMeta(withAddedMeta(ElectricVehicleAsset.ODOMETER.getMeta(),
            new MetaItem<>(MetaItemType.ACCESS_RESTRICTED_READ)));

    public static final AttributeDescriptor<Double> POWER = ElectricVehicleAsset.POWER.withMeta(withAddedMeta(ElectricVehicleAsset.POWER.getMeta(),
            new MetaItem<>(MetaItemType.ACCESS_RESTRICTED_READ),
            new MetaItem<>(MetaItemType.RULE_STATE),
            new MetaItem<>(MetaItemType.STORE_DATA_POINTS)));

    public static final AttributeDescriptor<Boolean> SUPPORTS_IMPORT = ElectricVehicleAsset.SUPPORTS_IMPORT.withMeta(withAddedMeta(ElectricVehicleAsset.SUPPORTS_IMPORT.getMeta(),
            new MetaItem<>(MetaItemType.ACCESS_RESTRICTED_READ)));

    public static final AssetDescriptor<OurgridVehicleAsset> DESCRIPTOR = new AssetDescriptor<>("car-electric", "49B0D8", OurgridVehicleAsset.class);

    protected OurgridVehicleAsset() {
    }

    public OurgridVehicleAsset(String name) {
        super(name);
    }

    @Override
    public OurgridVehicleAsset setDeviceId(String value) {
        getAttributes().getOrCreate(DEVICE_ID).setValue(value);
        return this;
    }

}
