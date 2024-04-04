package org.openremote.agent.custom.reschool;

import org.openremote.model.asset.Asset;
import org.openremote.model.asset.AssetDescriptor;
import org.openremote.model.attribute.MetaItem;
import org.openremote.model.value.*;

import jakarta.persistence.Entity;

import java.util.Optional;

import static org.openremote.model.Constants.*;

@Entity
public class OurGridPeaksAsset extends Asset<OurGridPeaksAsset> {
    public static final AssetDescriptor<OurGridPeaksAsset> DESCRIPTOR = new AssetDescriptor<>("cube-outline", null, OurGridPeaksAsset.class);

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

    protected OurGridPeaksAsset() {
    }

    public OurGridPeaksAsset(String name) {
        super(name);
    }

    public OurGridPeaksAsset setTurnOnPeakPoints(Boolean value) {
        getAttributes().getOrCreate(TURN_ON_PEAK_POINTS).setValue(value);
        return this;
    }

    public OurGridPeaksAsset setConnectionQualityThreshold(Double value) {
        getAttributes().getOrCreate(CONNECTION_QUALITY_THRESHOLD).setValue(value);
        return this;
    }

    public OurGridPeaksAsset setPeakConsumptionThreshold(Double value) {
        getAttributes().getOrCreate(PEAK_CONSUMPTION_THRESHOLD).setValue(value);
        return this;
    }

    public OurGridPeaksAsset setPeakPeriods(String value) {
        getAttributes().getOrCreate(PEAK_PERIODS).setValue(value);
        return this;
    }

    public OurGridPeaksAsset setPeakPointsDay(Double value) {
        getAttributes().getOrCreate(PEAK_POINTS_DAY).setValue(value);
        return this;
    }
}
