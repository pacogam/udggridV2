package org.openremote.agent.custom.ourgrid;

import jakarta.persistence.Entity;
import org.openremote.model.asset.Asset;
import org.openremote.model.asset.AssetDescriptor;
import org.openremote.model.attribute.MetaItem;
import org.openremote.model.value.AttributeDescriptor;
import org.openremote.model.value.MetaItemType;
import org.openremote.model.value.ValueType;

import static org.openremote.model.Constants.UNITS_PERCENTAGE;

@Entity
public class OurgridPeaksAsset extends Asset<OurgridPeaksAsset> {

    public static final AttributeDescriptor<Boolean> TURN_ON_PEAK_POINTS = new AttributeDescriptor<>("turnOnPeakPoints", ValueType.BOOLEAN,
            new MetaItem<>(MetaItemType.LABEL, " Turn on peak points"),
            new MetaItem<>(MetaItemType.RULE_STATE)
    );

    public static final AttributeDescriptor<Double> CONNECTION_QUALITY_THRESHOLD = new AttributeDescriptor<>("connectionQualityThreshold", ValueType.POSITIVE_NUMBER,
            new MetaItem<>(MetaItemType.LABEL, "Connection quality threshold"),
            new MetaItem<>(MetaItemType.RULE_STATE)
    ).withUnits(UNITS_PERCENTAGE);

    public static final AttributeDescriptor<Double> PEAK_CONSUMPTION_THRESHOLD = new AttributeDescriptor<>("peakConsumptionThreshold", ValueType.POSITIVE_NUMBER,
            new MetaItem<>(MetaItemType.LABEL, "Peak household consumption threshold"),
            new MetaItem<>(MetaItemType.RULE_STATE)
    ).withUnits(UNITS_PERCENTAGE);

    public static final AttributeDescriptor<String> PEAK_PERIODS = new AttributeDescriptor<>("peakPeriods", ValueType.TEXT,
            new MetaItem<>(MetaItemType.LABEL, "Peak periods"),
            new MetaItem<>(MetaItemType.MULTILINE),
            new MetaItem<>(MetaItemType.RULE_STATE)
    );

    public static final AttributeDescriptor<Double> PEAK_POINTS_DAY = new AttributeDescriptor<>("peakPointsDay", ValueType.POSITIVE_NUMBER,
            new MetaItem<>(MetaItemType.LABEL, "Peak points that can be earned per day"),
            new MetaItem<>(MetaItemType.RULE_STATE)
    );

    public static final AttributeDescriptor<Double> PEAK_POINTS_TOTAL = new AttributeDescriptor<>("peakPointsTotal", ValueType.POSITIVE_NUMBER,
            new MetaItem<>(MetaItemType.LABEL, "Peak points total"),
            new MetaItem<>(MetaItemType.READ_ONLY),
            new MetaItem<>(MetaItemType.RULE_STATE),
            new MetaItem<>(MetaItemType.STORE_DATA_POINTS),
            new MetaItem<>(MetaItemType.DATA_POINTS_MAX_AGE_DAYS, 366)
    );


    public static final AssetDescriptor<OurgridPeaksAsset> DESCRIPTOR = new AssetDescriptor<>("star-outline", "00b33b", OurgridPeaksAsset.class);

    protected OurgridPeaksAsset() {
    }

    public OurgridPeaksAsset(String name) {
        super(name);
    }

    public OurgridPeaksAsset setTurnOnPeakPoints(Boolean value) {
        getAttributes().getOrCreate(TURN_ON_PEAK_POINTS).setValue(value);
        return this;
    }

    public OurgridPeaksAsset setConnectionQualityThreshold(Double value) {
        getAttributes().getOrCreate(CONNECTION_QUALITY_THRESHOLD).setValue(value);
        return this;
    }

    public OurgridPeaksAsset setPeakConsumptionThreshold(Double value) {
        getAttributes().getOrCreate(PEAK_CONSUMPTION_THRESHOLD).setValue(value);
        return this;
    }

    public OurgridPeaksAsset setPeakPeriods(String value) {
        getAttributes().getOrCreate(PEAK_PERIODS).setValue(value);
        return this;
    }

    public OurgridPeaksAsset setPeakPointsDay(Double value) {
        getAttributes().getOrCreate(PEAK_POINTS_DAY).setValue(value);
        return this;
    }
}
