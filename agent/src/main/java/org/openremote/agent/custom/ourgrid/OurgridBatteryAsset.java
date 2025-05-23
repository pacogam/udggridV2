package org.openremote.agent.custom.ourgrid;

import jakarta.persistence.Entity;
import org.openremote.model.asset.AssetDescriptor;
import org.openremote.model.asset.impl.ElectricityBatteryAsset;
import org.openremote.model.attribute.AttributeExecuteStatus;
import org.openremote.model.attribute.MetaItem;
import org.openremote.model.attribute.MetaMap;
import org.openremote.model.value.AttributeDescriptor;
import org.openremote.model.value.MetaItemType;
import org.openremote.model.value.ValueType;

@Entity
public class OurgridBatteryAsset extends ElectricityBatteryAsset {
    public static final AttributeDescriptor<Boolean> ALLOW_AUTOMATIC_CONTROL_BUTTON = new AttributeDescriptor<>("allowAutomaticControlButton", ValueType.BOOLEAN,
            new MetaItem<>(MetaItemType.ACCESS_RESTRICTED_READ),
            new MetaItem<>(MetaItemType.ACCESS_RESTRICTED_WRITE),
            new MetaItem<>(MetaItemType.RULE_STATE)
    );

    public static final AttributeDescriptor<Boolean> ALLOW_DISCHARGING_BUTTON = new AttributeDescriptor<>("allowDischargingButton", ValueType.BOOLEAN,
            new MetaItem<>(MetaItemType.ACCESS_RESTRICTED_READ),
            new MetaItem<>(MetaItemType.ACCESS_RESTRICTED_WRITE),
            new MetaItem<>(MetaItemType.RULE_STATE)
    );

    public static final AttributeDescriptor<String> DEVICE_ID = new AttributeDescriptor<>("deviceId", ValueType.TEXT,
            new MetaItem<>(MetaItemType.ACCESS_RESTRICTED_READ),
            new MetaItem<>(MetaItemType.READ_ONLY),
            new MetaItem<>(MetaItemType.RULE_STATE)
    );

    public static final AttributeDescriptor<String> SOFTWARE_VERSION = new AttributeDescriptor<>("softwareVersion", ValueType.TEXT,
            new MetaItem<>(MetaItemType.ACCESS_RESTRICTED_READ),
            new MetaItem<>(MetaItemType.READ_ONLY),
            new MetaItem<>(MetaItemType.RULE_STATE)
    );

    /*
     * Sorry to whoever has to deal with this, but we need to pretty much add metaItems to all inherited attributes.
     *
     * Here is the regex I used to pull the attributes, by following the parent tree and pasting to a new file: (?s)(^\s*public\s+static\s+final\s+AttributeDescriptor<[^>]+>\s+)(\w+)\s*=.*?;
     * Here is the replacement string I used in IntelliJ: $1$2 = ElectricityBatteryAsset.$2.withMeta(withAddedMeta(ElectricityBatteryAsset.$2.getMeta(), new MetaItem<>(MetaItemType.ACCESS_RESTRICTED_READ, true)));
     *
     * And then I removed any duplicates going from the bottom up.
     */

    public static final AttributeDescriptor<Integer> CHARGE_CYCLES = ElectricityBatteryAsset.CHARGE_CYCLES.withMeta(withAddedMeta(ElectricityBatteryAsset.CHARGE_CYCLES.getMeta(), new MetaItem<>(MetaItemType.ACCESS_RESTRICTED_READ, true)));
    public static final AttributeDescriptor<Boolean> SUPPORTS_EXPORT = ElectricityBatteryAsset.SUPPORTS_EXPORT.withMeta(withAddedMeta(ElectricityBatteryAsset.SUPPORTS_EXPORT.getMeta(), new MetaItem<>(MetaItemType.ACCESS_RESTRICTED_READ, true)));
    public static final AttributeDescriptor<Boolean> SUPPORTS_IMPORT = ElectricityBatteryAsset.SUPPORTS_IMPORT.withMeta(withAddedMeta(ElectricityBatteryAsset.SUPPORTS_IMPORT.getMeta(), new MetaItem<>(MetaItemType.ACCESS_RESTRICTED_READ, true)));
    public static final AttributeDescriptor<Double> ENERGY_LEVEL = ElectricityBatteryAsset.ENERGY_LEVEL.withMeta(withAddedMeta(ElectricityBatteryAsset.ENERGY_LEVEL.getMeta(), new MetaItem<>(MetaItemType.ACCESS_RESTRICTED_READ, true)));
    public static final AttributeDescriptor<Double> ENERGY_CAPACITY = ElectricityBatteryAsset.ENERGY_CAPACITY.withMeta(withAddedMeta(ElectricityBatteryAsset.ENERGY_CAPACITY.getMeta(), new MetaItem<>(MetaItemType.ACCESS_RESTRICTED_READ, true)));
    public static final AttributeDescriptor<Integer> ENERGY_LEVEL_PERCENTAGE = ElectricityBatteryAsset.ENERGY_LEVEL_PERCENTAGE.withMeta(withAddedMeta(ElectricityBatteryAsset.ENERGY_LEVEL_PERCENTAGE.getMeta(), new MetaItem<>(MetaItemType.ACCESS_RESTRICTED_READ, true)));
    public static final AttributeDescriptor<Integer> ENERGY_LEVEL_PERCENTAGE_MAX = ElectricityBatteryAsset.ENERGY_LEVEL_PERCENTAGE_MAX.withMeta(withAddedMeta(ElectricityBatteryAsset.ENERGY_LEVEL_PERCENTAGE_MAX.getMeta(), new MetaItem<>(MetaItemType.ACCESS_RESTRICTED_READ, true), new MetaItem<>(MetaItemType.RULE_STATE, true)));
    public static final AttributeDescriptor<Integer> ENERGY_LEVEL_PERCENTAGE_MIN = ElectricityBatteryAsset.ENERGY_LEVEL_PERCENTAGE_MIN.withMeta(withAddedMeta(ElectricityBatteryAsset.ENERGY_LEVEL_PERCENTAGE_MIN.getMeta(), new MetaItem<>(MetaItemType.ACCESS_RESTRICTED_READ, true)));
    public static final AttributeDescriptor<Integer[][]> ENERGY_LEVEL_SCHEDULE = ElectricityBatteryAsset.ENERGY_LEVEL_SCHEDULE.withMeta(withAddedMeta(ElectricityBatteryAsset.ENERGY_LEVEL_SCHEDULE.getMeta(), new MetaItem<>(MetaItemType.ACCESS_RESTRICTED_READ, true)));
    public static final AttributeDescriptor<AttributeExecuteStatus> FORCE_CHARGE = ElectricityBatteryAsset.FORCE_CHARGE.withMeta(withAddedMeta(ElectricityBatteryAsset.FORCE_CHARGE.getMeta(), new MetaItem<>(MetaItemType.ACCESS_RESTRICTED_READ, true)));
    public static final AttributeDescriptor<Double> POWER_SETPOINT = ElectricityBatteryAsset.POWER_SETPOINT.withMeta(withAddedMeta(ElectricityBatteryAsset.POWER_SETPOINT.getMeta(), new MetaItem<>(MetaItemType.ACCESS_RESTRICTED_READ, true), new MetaItem<>(MetaItemType.RULE_STATE, true)));
    public static final AttributeDescriptor<Double> POWER_IMPORT_MIN = ElectricityBatteryAsset.POWER_IMPORT_MIN.withMeta(withAddedMeta(ElectricityBatteryAsset.POWER_IMPORT_MIN.getMeta(), new MetaItem<>(MetaItemType.ACCESS_RESTRICTED_READ, true)));
    public static final AttributeDescriptor<Double> POWER_EXPORT_MIN = ElectricityBatteryAsset.POWER_EXPORT_MIN.withMeta(withAddedMeta(ElectricityBatteryAsset.POWER_EXPORT_MIN.getMeta(), new MetaItem<>(MetaItemType.ACCESS_RESTRICTED_READ, true)));
    public static final AttributeDescriptor<Double> CARBON_IMPORT = ElectricityBatteryAsset.CARBON_IMPORT.withMeta(withAddedMeta(ElectricityBatteryAsset.CARBON_IMPORT.getMeta(), new MetaItem<>(MetaItemType.ACCESS_RESTRICTED_READ, true)));


    public static final AttributeDescriptor<Double> POWER = ElectricityBatteryAsset.POWER.withMeta(withAddedMeta(ElectricityBatteryAsset.POWER.getMeta(), new MetaItem<>(MetaItemType.ACCESS_RESTRICTED_READ, true), new MetaItem<>(MetaItemType.RULE_STATE, true)));
    public static final AttributeDescriptor<Double> POWER_IMPORT_MAX = ElectricityBatteryAsset.POWER_IMPORT_MAX.withMeta(withAddedMeta(ElectricityBatteryAsset.POWER_IMPORT_MAX.getMeta(), new MetaItem<>(MetaItemType.ACCESS_RESTRICTED_READ, true), new MetaItem<>(MetaItemType.RULE_STATE, true)));
    public static final AttributeDescriptor<Double> POWER_EXPORT_MAX = ElectricityBatteryAsset.POWER_EXPORT_MAX.withMeta(withAddedMeta(ElectricityBatteryAsset.POWER_EXPORT_MAX.getMeta(), new MetaItem<>(MetaItemType.ACCESS_RESTRICTED_READ, true)));
    public static final AttributeDescriptor<Double> ENERGY_IMPORT_TOTAL = ElectricityBatteryAsset.ENERGY_IMPORT_TOTAL.withMeta(withAddedMeta(ElectricityBatteryAsset.ENERGY_IMPORT_TOTAL.getMeta(), new MetaItem<>(MetaItemType.ACCESS_RESTRICTED_READ, true)));
    public static final AttributeDescriptor<Double> ENERGY_EXPORT_TOTAL = ElectricityBatteryAsset.ENERGY_EXPORT_TOTAL.withMeta(withAddedMeta(ElectricityBatteryAsset.ENERGY_EXPORT_TOTAL.getMeta(), new MetaItem<>(MetaItemType.ACCESS_RESTRICTED_READ, true)));
    public static final AttributeDescriptor<Integer> EFFICIENCY_IMPORT = ElectricityBatteryAsset.EFFICIENCY_IMPORT.withMeta(withAddedMeta(ElectricityBatteryAsset.EFFICIENCY_IMPORT.getMeta(), new MetaItem<>(MetaItemType.ACCESS_RESTRICTED_READ, true)));
    public static final AttributeDescriptor<Integer> EFFICIENCY_EXPORT = ElectricityBatteryAsset.EFFICIENCY_EXPORT.withMeta(withAddedMeta(ElectricityBatteryAsset.EFFICIENCY_EXPORT.getMeta(), new MetaItem<>(MetaItemType.ACCESS_RESTRICTED_READ, true)));
    public static final AttributeDescriptor<Double> TARIFF_IMPORT = ElectricityBatteryAsset.TARIFF_IMPORT.withMeta(withAddedMeta(ElectricityBatteryAsset.TARIFF_IMPORT.getMeta(), new MetaItem<>(MetaItemType.ACCESS_RESTRICTED_READ, true)));
    public static final AttributeDescriptor<Double> TARIFF_EXPORT = ElectricityBatteryAsset.TARIFF_EXPORT.withMeta(withAddedMeta(ElectricityBatteryAsset.TARIFF_EXPORT.getMeta(), new MetaItem<>(MetaItemType.ACCESS_RESTRICTED_READ, true)));


    public static final AssetDescriptor<OurgridBatteryAsset> DESCRIPTOR = new AssetDescriptor<>("numeric-1-circle", "00ed85", OurgridBatteryAsset.class);

    protected OurgridBatteryAsset() {
    }

    public OurgridBatteryAsset(String name) {
        super(name);
    }

    public static MetaMap withAddedMeta(MetaMap map, MetaItem<?>... items) {
        MetaMap newMap = new MetaMap(map);
        newMap.addAll(items);
        return newMap;
    }
}