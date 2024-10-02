package org.openremote.agent.custom.ourgrid;

import jakarta.persistence.Entity;
import org.openremote.model.asset.Asset;
import org.openremote.model.asset.AssetDescriptor;
import org.openremote.model.attribute.MetaItem;
import org.openremote.model.value.AttributeDescriptor;
import org.openremote.model.value.MetaItemType;
import org.openremote.model.value.ValueType;

import static org.openremote.model.Constants.*;

@Entity
public class OurgridMeterSumAsset extends Asset<OurgridMeterSumAsset> {

    public static final AttributeDescriptor<Double> POWER = new AttributeDescriptor<>("power", ValueType.NUMBER,
            new MetaItem<>(MetaItemType.LABEL, "Power"),
            new MetaItem<>(MetaItemType.READ_ONLY),
            new MetaItem<>(MetaItemType.RULE_STATE),
            new MetaItem<>(MetaItemType.STORE_DATA_POINTS)
    ).withUnits(UNITS_WATT);

    public static final AttributeDescriptor<Double> ENERGY_IMPORT_TOTAL = new AttributeDescriptor<>("energyImportTotal", ValueType.NUMBER,
            new MetaItem<>(MetaItemType.LABEL, "Total energy imported"),
            new MetaItem<>(MetaItemType.READ_ONLY),
            new MetaItem<>(MetaItemType.RULE_STATE)
    ).withUnits(UNITS_KILO, UNITS_WATT, UNITS_HOUR);

    public static final AttributeDescriptor<Double> ENERGY_EXPORT_TOTAL = new AttributeDescriptor<>("energyExportTotal", ValueType.NUMBER,
            new MetaItem<>(MetaItemType.LABEL, "Total energy exported"),
            new MetaItem<>(MetaItemType.READ_ONLY),
            new MetaItem<>(MetaItemType.RULE_STATE)
    ).withUnits(UNITS_KILO, UNITS_WATT, UNITS_HOUR);

    public static final AttributeDescriptor<Double> ENERGY_NET_TOTAL = new AttributeDescriptor<>("energyNetTotal", ValueType.NUMBER,
            new MetaItem<>(MetaItemType.LABEL, "Total energy (import-export)"),
            new MetaItem<>(MetaItemType.READ_ONLY),
            new MetaItem<>(MetaItemType.RULE_STATE)
    ).withUnits(UNITS_KILO, UNITS_WATT, UNITS_HOUR);

    public static final AttributeDescriptor<Double> GAS_IMPORT_TOTAL = new AttributeDescriptor<>("gasImportTotal", ValueType.NUMBER,
            new MetaItem<>(MetaItemType.LABEL, "Total gas imported"),
            new MetaItem<>(MetaItemType.READ_ONLY),
            new MetaItem<>(MetaItemType.RULE_STATE)
    ).withUnits(UNITS_METRE, UNITS_CUBED);

    public static final AttributeDescriptor<Double> GAS_FLOW_RATE = new AttributeDescriptor<>("gasFlowRate", ValueType.NUMBER,
            new MetaItem<>(MetaItemType.LABEL, "Gas flow rate"),
            new MetaItem<>(MetaItemType.READ_ONLY),
            new MetaItem<>(MetaItemType.RULE_STATE)
    ).withUnits(UNITS_METRE, UNITS_CUBED, UNITS_PER, UNITS_MINUTE);

    public static final AttributeDescriptor<Double> ESTIMATED_SOLAR_CAPACITY = new AttributeDescriptor<>("estimatedSolarCapacity", ValueType.NUMBER,
            new MetaItem<>(MetaItemType.LABEL, "Estimated solar capacity (active meters)"),
            new MetaItem<>(MetaItemType.READ_ONLY),
            new MetaItem<>(MetaItemType.RULE_STATE)
    ).withUnits(UNITS_KILO, UNITS_WATT);


    public static final AssetDescriptor<OurgridMeterSumAsset> DESCRIPTOR = new AssetDescriptor<>("power-plug", "ff9300", OurgridMeterSumAsset.class);

    protected OurgridMeterSumAsset() {
    }

    public OurgridMeterSumAsset(String name) {
        super(name);
    }
}