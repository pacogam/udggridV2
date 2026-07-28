package org.openremote.agent.custom.gisce;


import jakarta.persistence.Entity;
import org.openremote.model.asset.Asset;
import org.openremote.model.asset.AssetDescriptor;
import org.openremote.model.attribute.MetaItem;
import org.openremote.model.value.AttributeDescriptor;
import org.openremote.model.value.MetaItemType;
import org.openremote.model.value.ValueType;

import static org.openremote.model.Constants.*;
import static org.openremote.model.Constants.UNITS_HOUR;

@Entity
public class CustomAssetGisce extends Asset<CustomAssetGisce> {
  // Add custom asset attributes
  public static final AttributeDescriptor<Double> ENERGY_R1 = new AttributeDescriptor<>("energyR1", ValueType.NUMBER,
    new MetaItem<>(MetaItemType.LABEL, "Energy R1"),
    new MetaItem<>(MetaItemType.READ_ONLY),
    new MetaItem<>(MetaItemType.STORE_DATA_POINTS)
  ).withUnits(UNITS_WATT, UNITS_HOUR);

  public static final AttributeDescriptor<Double> ENERGY_R2 = new AttributeDescriptor<>("energyR2", ValueType.NUMBER,
    new MetaItem<>(MetaItemType.LABEL, "Energy R2"),
    new MetaItem<>(MetaItemType.READ_ONLY),
    new MetaItem<>(MetaItemType.STORE_DATA_POINTS)
  ).withUnits(UNITS_WATT, UNITS_HOUR);

  public static final AttributeDescriptor<Double> ENERGY_R3 = new AttributeDescriptor<>("energyR3", ValueType.NUMBER,
    new MetaItem<>(MetaItemType.LABEL, "Energy R3"),
    new MetaItem<>(MetaItemType.READ_ONLY),
    new MetaItem<>(MetaItemType.STORE_DATA_POINTS)
  ).withUnits(UNITS_WATT, UNITS_HOUR);

  public static final AttributeDescriptor<Double> ENERGY_R4 = new AttributeDescriptor<>("energyR4", ValueType.NUMBER,
    new MetaItem<>(MetaItemType.LABEL, "Energy R4"),
    new MetaItem<>(MetaItemType.READ_ONLY),
    new MetaItem<>(MetaItemType.STORE_DATA_POINTS)
  ).withUnits(UNITS_WATT, UNITS_HOUR);

  public static final AttributeDescriptor<Double> ENERGY_IMPORT = new AttributeDescriptor<>("energyImport", ValueType.NUMBER,
    new MetaItem<>(MetaItemType.LABEL, "Energy imported"),
    new MetaItem<>(MetaItemType.READ_ONLY),
    new MetaItem<>(MetaItemType.STORE_DATA_POINTS)
    //new MetaItem<>(MetaItemType.ACCESS_RESTRICTED_READ),
    //new MetaItem<>(MetaItemType.RULE_STATE)
  ).withUnits(UNITS_WATT, UNITS_HOUR);

  public static final AttributeDescriptor<Double> ENERGY_EXPORT = new AttributeDescriptor<>("energyExport", ValueType.NUMBER,
    new MetaItem<>(MetaItemType.LABEL, "Energy exported"),
    new MetaItem<>(MetaItemType.READ_ONLY),
    new MetaItem<>(MetaItemType.STORE_DATA_POINTS)
    //new MetaItem<>(MetaItemType.ACCESS_RESTRICTED_READ),
    //new MetaItem<>(MetaItemType.RULE_STATE)
  ).withUnits(UNITS_WATT, UNITS_HOUR);

  public static final AttributeDescriptor<Double> ENERGY_IMPORT_FIX = new AttributeDescriptor<>("energyImportFix", ValueType.NUMBER,
    new MetaItem<>(MetaItemType.LABEL, "Energy imported fix"),
    new MetaItem<>(MetaItemType.READ_ONLY),
    new MetaItem<>(MetaItemType.STORE_DATA_POINTS)
  ).withUnits(UNITS_WATT, UNITS_HOUR);

  public static final AttributeDescriptor<Double> ENERGY_IMPORT_FACT = new AttributeDescriptor<>("energyImportFact", ValueType.NUMBER,
    new MetaItem<>(MetaItemType.LABEL, "Energy imported fact"),
    new MetaItem<>(MetaItemType.READ_ONLY),
    new MetaItem<>(MetaItemType.STORE_DATA_POINTS)
  ).withUnits(UNITS_WATT, UNITS_HOUR);

  public static final AttributeDescriptor<Double> ENERGY_EXPORT_FIX = new AttributeDescriptor<>("energyExportFix", ValueType.NUMBER,
    new MetaItem<>(MetaItemType.LABEL, "Energy exported fix"),
    new MetaItem<>(MetaItemType.READ_ONLY),
    new MetaItem<>(MetaItemType.STORE_DATA_POINTS)
  ).withUnits(UNITS_WATT, UNITS_HOUR);

  public static final AttributeDescriptor<Double> ENERGY_EXPORT_FACT = new AttributeDescriptor<>("energyExportFact", ValueType.NUMBER,
    new MetaItem<>(MetaItemType.LABEL, "Energy exported fact"),
    new MetaItem<>(MetaItemType.READ_ONLY),
    new MetaItem<>(MetaItemType.STORE_DATA_POINTS)
  ).withUnits(UNITS_WATT, UNITS_HOUR);

  public static final AttributeDescriptor<Double> ENERGY_R1_FACT = new AttributeDescriptor<>("enrgyR1Fact", ValueType.NUMBER,
    new MetaItem<>(MetaItemType.LABEL, "Energy R1 fact"),
    new MetaItem<>(MetaItemType.READ_ONLY),
    new MetaItem<>(MetaItemType.STORE_DATA_POINTS)
  ).withUnits(UNITS_WATT, UNITS_HOUR);

  public static final AttributeDescriptor<Double> ENERGY_R2_FACT = new AttributeDescriptor<>("energyR2Fact", ValueType.NUMBER,
    new MetaItem<>(MetaItemType.LABEL, "Energy R2 fact"),
    new MetaItem<>(MetaItemType.READ_ONLY),
    new MetaItem<>(MetaItemType.STORE_DATA_POINTS)
  ).withUnits(UNITS_WATT, UNITS_HOUR);

  public static final AttributeDescriptor<Double> ENERGY_R3_FACT = new AttributeDescriptor<>("energyR3Fact", ValueType.NUMBER,
    new MetaItem<>(MetaItemType.LABEL, "Energy R3 fact"),
    new MetaItem<>(MetaItemType.READ_ONLY),
    new MetaItem<>(MetaItemType.STORE_DATA_POINTS)
  ).withUnits(UNITS_WATT, UNITS_HOUR);

  public static final AttributeDescriptor<Double> ENERGY_R4_FACT = new AttributeDescriptor<>("energyR4Fact", ValueType.NUMBER,
    new MetaItem<>(MetaItemType.LABEL, "Energy R4 fact"),
    new MetaItem<>(MetaItemType.READ_ONLY),
    new MetaItem<>(MetaItemType.STORE_DATA_POINTS)
  ).withUnits(UNITS_WATT, UNITS_HOUR);


  public static final AssetDescriptor<CustomAssetGisce> DESCRIPTOR = new AssetDescriptor<>("flash", "EABB4D", CustomAssetGisce.class);

  protected CustomAssetGisce() {
  }

  public CustomAssetGisce(String name) {
    super(name);
  }
}
