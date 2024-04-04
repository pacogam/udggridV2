package org.openremote.agent.custom.reschool;

import org.openremote.model.asset.Asset;
import org.openremote.model.asset.AssetDescriptor;
import org.openremote.model.attribute.MetaItem;
import org.openremote.model.value.*;

import jakarta.persistence.Entity;

import static org.openremote.model.Constants.*;

@Entity
public class ReschoolDistrictAsset extends Asset<ReschoolDistrictAsset> {
    public static final AssetDescriptor<ReschoolDistrictAsset> DESCRIPTOR = new AssetDescriptor<>("city", null, ReschoolDistrictAsset.class); // set icon and colour of asset

    static ForecastConfigurationWeightedExponentialAverage forecastConfig = new ForecastConfigurationWeightedExponentialAverage(
            new ForecastConfigurationWeightedExponentialAverage.ExtendedPeriodAndDuration("P7D"),
            3,
            new ForecastConfigurationWeightedExponentialAverage.ExtendedPeriodAndDuration("PT15M"),
            96
    );

    public static final AttributeDescriptor<Integer> NUMBER_OF_HOUSEHOLDS = new AttributeDescriptor<>("numberOfHouseholds", ValueType.POSITIVE_INTEGER,
            new MetaItem<>(MetaItemType.LABEL, "Number of households"),
            new MetaItem<>(MetaItemType.RULE_STATE),
            new MetaItem<>(MetaItemType.STORE_DATA_POINTS)
    );

    public static final AttributeDescriptor<String> CHALLENGES_ASSET_ID = new AttributeDescriptor<>("challengesAssetId", ValueType.TEXT,
            new MetaItem<>(MetaItemType.LABEL, "Challenges Asset ID"),
            new MetaItem<>(MetaItemType.ACCESS_RESTRICTED_READ)
    );

    public static final AttributeDescriptor<String> PEAKS_ASSET_ID = new AttributeDescriptor<>("peaksAssetId", ValueType.TEXT,
            new MetaItem<>(MetaItemType.LABEL, "Peaks Asset ID"),
            new MetaItem<>(MetaItemType.ACCESS_RESTRICTED_READ)
    );

    public static final AttributeDescriptor<Double> POWER_CORRECTION_FACTOR = new AttributeDescriptor<>("powerCorrectionFactor", ValueType.NUMBER,
            new MetaItem<>(MetaItemType.LABEL, "Power correction factor"),
            new MetaItem<>(MetaItemType.READ_ONLY),
            new MetaItem<>(MetaItemType.RULE_STATE),
            new MetaItem<>(MetaItemType.STORE_DATA_POINTS)
    );

    public static final AttributeDescriptor<Integer> ACTIVE_PERIOD = new AttributeDescriptor<>("activePeriod", ValueType.POSITIVE_INTEGER,
            new MetaItem<>(MetaItemType.LABEL, "Active Period"),
            new MetaItem<>(MetaItemType.RULE_STATE)
    ).withUnits(UNITS_MINUTE);

    public static final AttributeDescriptor<Integer> NUMBER_OF_ACTIVE_POWER_READINGS = new AttributeDescriptor<>("numberOfActivePowerReadings", ValueType.POSITIVE_INTEGER,
            new MetaItem<>(MetaItemType.LABEL, "  Number of active power readings"),
            new MetaItem<>(MetaItemType.READ_ONLY),
            new MetaItem<>(MetaItemType.RULE_STATE),
            new MetaItem<>(MetaItemType.STORE_DATA_POINTS)
    );

    public static final AttributeDescriptor<Integer> NUMBER_OF_DEVICES = new AttributeDescriptor<>("numberOfDevices", ValueType.POSITIVE_INTEGER,
            new MetaItem<>(MetaItemType.LABEL, "  Number of Earn-E devices"),
            new MetaItem<>(MetaItemType.READ_ONLY),
            new MetaItem<>(MetaItemType.RULE_STATE),
            new MetaItem<>(MetaItemType.STORE_DATA_POINTS)
    );

    public static final AttributeDescriptor<Double> POWER_DISTRICT = new AttributeDescriptor<>("powerDistrict", ValueType.NUMBER,
            new MetaItem<>(MetaItemType.LABEL, " Net power district"),
            new MetaItem<>(MetaItemType.READ_ONLY),
            new MetaItem<>(MetaItemType.ACCESS_RESTRICTED_READ),
            new MetaItem<>(MetaItemType.RULE_STATE),
            new MetaItem<>(MetaItemType.STORE_DATA_POINTS)
    ).withUnits(UNITS_KILO, UNITS_WATT);

    public static final AttributeDescriptor<Double> POWER_CONSUMPTION_DISTRICT = new AttributeDescriptor<>("powerConsumptionDistrict", ValueType.NUMBER,
            new MetaItem<>(MetaItemType.LABEL, " Power consumption district"),
            new MetaItem<>(MetaItemType.READ_ONLY),
            new MetaItem<>(MetaItemType.RULE_STATE),
            new MetaItem<>(MetaItemType.STORE_DATA_POINTS),
            new MetaItem<>(MetaItemType.FORECAST, forecastConfig)
    ).withUnits(UNITS_KILO, UNITS_WATT);

    public static final AttributeDescriptor<Double> POWER_SOLAR_DISTRICT = new AttributeDescriptor<>("powerSolarDistrict", ValueType.NUMBER,
            new MetaItem<>(MetaItemType.LABEL, " Solar power production district"),
            new MetaItem<>(MetaItemType.READ_ONLY),
            new MetaItem<>(MetaItemType.RULE_STATE),
            new MetaItem<>(MetaItemType.STORE_DATA_POINTS)
    ).withUnits(UNITS_KILO, UNITS_WATT);

    public static final AttributeDescriptor<Double> POWER_IMPORT_MAX = new AttributeDescriptor<>("powerImportMax", ValueType.NUMBER,
            new MetaItem<>(MetaItemType.LABEL, "Power import max"),
            new MetaItem<>(MetaItemType.ACCESS_RESTRICTED_READ),
            new MetaItem<>(MetaItemType.RULE_STATE)
    ).withUnits(UNITS_KILO, UNITS_WATT);

    public static final AttributeDescriptor<Integer> POWER_IMPORT_PERCENTAGE = new AttributeDescriptor<>("powerImportPercentage", ValueType.INTEGER,
            new MetaItem<>(MetaItemType.LABEL, "Power import percentage"),
            new MetaItem<>(MetaItemType.READ_ONLY),
            new MetaItem<>(MetaItemType.ACCESS_RESTRICTED_READ),
            new MetaItem<>(MetaItemType.RULE_STATE),
            new MetaItem<>(MetaItemType.STORE_DATA_POINTS)
    ).withUnits(UNITS_PERCENTAGE);

    public static final AttributeDescriptor<Integer> POWER_IMPORT_CRITICAL_PERCENTAGE = new AttributeDescriptor<>("powerImportCriticalPercentage", ValueType.INTEGER,
            new MetaItem<>(MetaItemType.LABEL, "Power import critical percentage"),
            new MetaItem<>(MetaItemType.ACCESS_RESTRICTED_READ),
            new MetaItem<>(MetaItemType.RULE_STATE)
    ).withUnits(UNITS_PERCENTAGE);


    protected ReschoolDistrictAsset() {
    }

    public ReschoolDistrictAsset(String name) {
        super(name);
    }

    public ReschoolDistrictAsset setNumberOfHouseholds(Integer value) {
        getAttributes().getOrCreate(NUMBER_OF_HOUSEHOLDS).setValue(value);
        return this;
    }

    public ReschoolDistrictAsset setPowerImportCriticalPercentage(Integer value) {
        getAttributes().getOrCreate(POWER_IMPORT_CRITICAL_PERCENTAGE).setValue(value);
        return this;
    }

    public ReschoolDistrictAsset setPowerImportMax(Double value) {
        getAttributes().getOrCreate(POWER_IMPORT_MAX).setValue(value);
        return this;
    }

    public ReschoolDistrictAsset setChallengesAssetId(String value) {
        getAttributes().getOrCreate(CHALLENGES_ASSET_ID).setValue(value);
        return this;
    }

    public ReschoolDistrictAsset setPeaksAssetId(String value) {
        getAttributes().getOrCreate(PEAKS_ASSET_ID).setValue(value);
        return this;
    }
}
