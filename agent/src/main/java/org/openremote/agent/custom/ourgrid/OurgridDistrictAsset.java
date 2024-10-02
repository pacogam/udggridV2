package org.openremote.agent.custom.ourgrid;

import jakarta.persistence.Entity;
import org.openremote.model.asset.Asset;
import org.openremote.model.asset.AssetDescriptor;
import org.openremote.model.attribute.MetaItem;
import org.openremote.model.value.AttributeDescriptor;
import org.openremote.model.value.ForecastConfigurationWeightedExponentialAverage;
import org.openremote.model.value.MetaItemType;
import org.openremote.model.value.ValueType;

import static org.openremote.model.Constants.*;

@Entity
public class OurgridDistrictAsset extends Asset<OurgridDistrictAsset> {

    static ForecastConfigurationWeightedExponentialAverage forecastConfig = new ForecastConfigurationWeightedExponentialAverage(
            new ForecastConfigurationWeightedExponentialAverage.ExtendedPeriodAndDuration("P7D"),
            3,
            new ForecastConfigurationWeightedExponentialAverage.ExtendedPeriodAndDuration("PT15M"),
            96
    );

    public static final AttributeDescriptor<Integer> NUMBER_OF_HOUSEHOLDS = new AttributeDescriptor<>("numberOfHouseholds", ValueType.POSITIVE_INTEGER,
            new MetaItem<>(MetaItemType.LABEL, "Number of households"),
            new MetaItem<>(MetaItemType.RULE_STATE)
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
            new MetaItem<>(MetaItemType.STORE_DATA_POINTS),
            new MetaItem<>(MetaItemType.DATA_POINTS_MAX_AGE_DAYS, 62)
    );

    public static final AttributeDescriptor<Integer> ACTIVE_PERIOD = new AttributeDescriptor<>("activePeriod", ValueType.POSITIVE_INTEGER,
            new MetaItem<>(MetaItemType.LABEL, "Active Period"),
            new MetaItem<>(MetaItemType.RULE_STATE)
    ).withUnits(UNITS_MINUTE);

    public static final AttributeDescriptor<Integer> NUMBER_OF_ACTIVE_POWER_READINGS = new AttributeDescriptor<>("numberOfActivePowerReadings", ValueType.POSITIVE_INTEGER,
            new MetaItem<>(MetaItemType.LABEL, "  Number of active power readings"),
            new MetaItem<>(MetaItemType.READ_ONLY),
            new MetaItem<>(MetaItemType.RULE_STATE),
            new MetaItem<>(MetaItemType.STORE_DATA_POINTS),
            new MetaItem<>(MetaItemType.DATA_POINTS_MAX_AGE_DAYS, 62)
    );

    public static final AttributeDescriptor<Integer> NUMBER_OF_DEVICES = new AttributeDescriptor<>("numberOfDevices", ValueType.POSITIVE_INTEGER,
            new MetaItem<>(MetaItemType.LABEL, "  Number of OurGrid meters"),
            new MetaItem<>(MetaItemType.READ_ONLY),
            new MetaItem<>(MetaItemType.RULE_STATE),
            new MetaItem<>(MetaItemType.STORE_DATA_POINTS),
            new MetaItem<>(MetaItemType.DATA_POINTS_MAX_AGE_DAYS, 62)
    );

    public static final AttributeDescriptor<Double> ESTIMATED_SOLAR_CAPACITY_DISTRICT = new AttributeDescriptor<>("estimatedSolarCapacityDistrict", ValueType.NUMBER,
            new MetaItem<>(MetaItemType.LABEL, " Estimated solar capacity district"),
            new MetaItem<>(MetaItemType.READ_ONLY),
            new MetaItem<>(MetaItemType.RULE_STATE),
            new MetaItem<>(MetaItemType.STORE_DATA_POINTS),
            new MetaItem<>(MetaItemType.DATA_POINTS_MAX_AGE_DAYS, 62)
    ).withUnits(UNITS_KILO, UNITS_WATT);

    public static final AttributeDescriptor<Double> POWER_DISTRICT = new AttributeDescriptor<>("powerDistrict", ValueType.NUMBER,
            new MetaItem<>(MetaItemType.LABEL, " Net power district"),
            new MetaItem<>(MetaItemType.HAS_PREDICTED_DATA_POINTS),
            new MetaItem<>(MetaItemType.READ_ONLY),
            new MetaItem<>(MetaItemType.ACCESS_RESTRICTED_READ),
            new MetaItem<>(MetaItemType.RULE_STATE),
            new MetaItem<>(MetaItemType.STORE_DATA_POINTS),
            new MetaItem<>(MetaItemType.DATA_POINTS_MAX_AGE_DAYS, 62)
    ).withUnits(UNITS_KILO, UNITS_WATT);

    public static final AttributeDescriptor<Double> POWER_CONSUMPTION_DISTRICT = new AttributeDescriptor<>("powerConsumptionDistrict", ValueType.NUMBER,
            new MetaItem<>(MetaItemType.LABEL, " Power consumption district"),
            new MetaItem<>(MetaItemType.READ_ONLY),
            new MetaItem<>(MetaItemType.RULE_STATE),
            new MetaItem<>(MetaItemType.STORE_DATA_POINTS),
            new MetaItem<>(MetaItemType.DATA_POINTS_MAX_AGE_DAYS, 62),
            new MetaItem<>(MetaItemType.FORECAST, forecastConfig)
    ).withUnits(UNITS_KILO, UNITS_WATT);

    public static final AttributeDescriptor<Double> POWER_SOLAR_DISTRICT = new AttributeDescriptor<>("powerSolarDistrict", ValueType.NUMBER,
            new MetaItem<>(MetaItemType.LABEL, " Solar power production district"),
            new MetaItem<>(MetaItemType.READ_ONLY),
            new MetaItem<>(MetaItemType.RULE_STATE),
            new MetaItem<>(MetaItemType.STORE_DATA_POINTS),
            new MetaItem<>(MetaItemType.DATA_POINTS_MAX_AGE_DAYS, 62)
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
            new MetaItem<>(MetaItemType.STORE_DATA_POINTS),
            new MetaItem<>(MetaItemType.DATA_POINTS_MAX_AGE_DAYS, 62)
    ).withUnits(UNITS_PERCENTAGE);

    public static final AttributeDescriptor<Integer> POWER_IMPORT_CRITICAL_PERCENTAGE = new AttributeDescriptor<>("powerImportCriticalPercentage", ValueType.INTEGER,
            new MetaItem<>(MetaItemType.LABEL, "Power import critical percentage"),
            new MetaItem<>(MetaItemType.ACCESS_RESTRICTED_READ),
            new MetaItem<>(MetaItemType.RULE_STATE)
    ).withUnits(UNITS_PERCENTAGE);

    public static final AttributeDescriptor<Boolean> TURN_ON_DYNAMIC_SOLAR_CAPACITY = new AttributeDescriptor<>("turnOnDynamicSolarCapacity", ValueType.BOOLEAN,
            new MetaItem<>(MetaItemType.LABEL, "Turn on dynamic solar capacity"),
            new MetaItem<>(MetaItemType.RULE_STATE)
    );


    public static final AssetDescriptor<OurgridDistrictAsset> DESCRIPTOR = new AssetDescriptor<>("city", "64404c", OurgridDistrictAsset.class); // set icon and colour of asset

    protected OurgridDistrictAsset() {
    }

    public OurgridDistrictAsset(String name) {
        super(name);
    }


    public OurgridDistrictAsset setActivePeriod(Integer value) {
        getAttributes().getOrCreate(ACTIVE_PERIOD).setValue(value);
        return this;
    }

    public OurgridDistrictAsset setNumberOfHouseholds(Integer value) {
        getAttributes().getOrCreate(NUMBER_OF_HOUSEHOLDS).setValue(value);
        return this;
    }

    public OurgridDistrictAsset setPowerImportCriticalPercentage(Integer value) {
        getAttributes().getOrCreate(POWER_IMPORT_CRITICAL_PERCENTAGE).setValue(value);
        return this;
    }

    public OurgridDistrictAsset setPowerImportMax(Double value) {
        getAttributes().getOrCreate(POWER_IMPORT_MAX).setValue(value);
        return this;
    }

    public OurgridDistrictAsset setChallengesAssetId(String value) {
        getAttributes().getOrCreate(CHALLENGES_ASSET_ID).setValue(value);
        return this;
    }

    public OurgridDistrictAsset setPeaksAssetId(String value) {
        getAttributes().getOrCreate(PEAKS_ASSET_ID).setValue(value);
        return this;
    }
}