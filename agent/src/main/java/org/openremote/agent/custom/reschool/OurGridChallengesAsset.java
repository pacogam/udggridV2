package org.openremote.agent.custom.reschool;

import org.openremote.model.asset.Asset;
import org.openremote.model.asset.AssetDescriptor;
import org.openremote.model.attribute.Attribute;
import org.openremote.model.attribute.MetaItem;
import org.openremote.model.value.*;

import jakarta.persistence.Entity;

import java.util.Optional;

import static org.openremote.model.Constants.*;

@Entity
public class OurGridChallengesAsset extends Asset<OurGridChallengesAsset> {
    public static final AssetDescriptor<OurGridChallengesAsset> DESCRIPTOR = new AssetDescriptor<>("cube-outline", null, OurGridChallengesAsset.class);

    public static final AttributeDescriptor<Boolean> TURN_ON_CHALLENGES = new AttributeDescriptor<>("turnOnChallenges", ValueType.BOOLEAN,
            new MetaItem<>(MetaItemType.LABEL, "      Turn on challenges"),
            new MetaItem<>(MetaItemType.RULE_STATE)
    );

    public static final AttributeDescriptor<Boolean> ACTIVATE_CHALLENGE_MANUALLY = new AttributeDescriptor<>("activateChallengeManually", ValueType.BOOLEAN,
            new MetaItem<>(MetaItemType.LABEL, "     Activate challenge manually"),
            new MetaItem<>(MetaItemType.RULE_STATE)
    );

    public enum ChallengeStatusGeneralValueType {
        joinedChallenge,
        activeChallenge,
        noChallenge
    }

    public static final ValueDescriptor<OurGridChallengesAsset.ChallengeStatusGeneralValueType> CHALLENGE_STATUS_GENERAL_VALUE_TYPE = new ValueDescriptor<>("challengeStatusGeneralValueType", OurGridChallengesAsset.ChallengeStatusGeneralValueType.class);

    public static final AttributeDescriptor<OurGridChallengesAsset.ChallengeStatusGeneralValueType> CHALLENGE_GENERAL_STATUS = new AttributeDescriptor<>("challengeGeneralStatus", CHALLENGE_STATUS_GENERAL_VALUE_TYPE,
            new MetaItem<>(MetaItemType.LABEL, "     Challenge general status"),
            new MetaItem<>(MetaItemType.READ_ONLY),
            new MetaItem<>(MetaItemType.RULE_STATE)
    );

    public static final AttributeDescriptor<String> CHALLENGE_START = new AttributeDescriptor<>("challengeStart", ValueType.TEXT,
            new MetaItem<>(MetaItemType.LABEL, "    Challenge start"),
            new MetaItem<>(MetaItemType.READ_ONLY),
            new MetaItem<>(MetaItemType.ACCESS_RESTRICTED_READ),
            new MetaItem<>(MetaItemType.RULE_STATE),
            new MetaItem<>(MetaItemType.STORE_DATA_POINTS)
    );

    public static final AttributeDescriptor<String> CHALLENGE_END = new AttributeDescriptor<>("challengeEnd", ValueType.TEXT,
            new MetaItem<>(MetaItemType.LABEL, "    Challenge end"),
            new MetaItem<>(MetaItemType.READ_ONLY),
            new MetaItem<>(MetaItemType.ACCESS_RESTRICTED_READ),
            new MetaItem<>(MetaItemType.RULE_STATE),
            new MetaItem<>(MetaItemType.STORE_DATA_POINTS)
    );

    public static final AttributeDescriptor<Long> CHALLENGE_WAIT = new AttributeDescriptor<>("challengeWait", ValueType.LONG,
            new MetaItem<>(MetaItemType.LABEL, "   Challenge wait"),
            new MetaItem<>(MetaItemType.ACCESS_RESTRICTED_READ),
            new MetaItem<>(MetaItemType.RULE_STATE)
    ).withUnits(UNITS_MINUTE);

    public static final AttributeDescriptor<Long> CHALLENGE_DURATION = new AttributeDescriptor<>("challengeDuration", ValueType.LONG,
            new MetaItem<>(MetaItemType.LABEL, "   Challenge duration"),
            new MetaItem<>(MetaItemType.ACCESS_RESTRICTED_READ),
            new MetaItem<>(MetaItemType.RULE_STATE)
    ).withUnits(UNITS_MINUTE);

    public static final AttributeDescriptor<Integer> CHALLENGE_EARN_POINT_INTERVAL = new AttributeDescriptor<>("challengeEarnPointInterval", ValueType.POSITIVE_INTEGER,
            new MetaItem<>(MetaItemType.LABEL, "   Challenge earn point interval"),
            new MetaItem<>(MetaItemType.ACCESS_RESTRICTED_READ),
            new MetaItem<>(MetaItemType.RULE_STATE)
    ).withUnits(UNITS_MINUTE);

    public enum ChallengePowerLimitValueType {
        manual,
        constant,
        ladder
    }

    public static final ValueDescriptor<OurGridChallengesAsset.ChallengePowerLimitValueType> CHALLENGE_POWER_LIMIT_VALUE_TYPE = new ValueDescriptor<>("ChallengePowerLimitValueType", OurGridChallengesAsset.ChallengePowerLimitValueType.class);

    public static final AttributeDescriptor<OurGridChallengesAsset.ChallengePowerLimitValueType> CHALLENGE_DEFAULT_POWER_LIMIT_METHOD = new AttributeDescriptor<>("challengeDefaultPowerLimitMethod", CHALLENGE_POWER_LIMIT_VALUE_TYPE,
            new MetaItem<>(MetaItemType.LABEL, "  Challenge default power limit method"),
            new MetaItem<>(MetaItemType.RULE_STATE)
    );

    public static final AttributeDescriptor<Integer> CHALLENGE_POWER_LIMIT_PROMOTION_PERCENTAGE = new AttributeDescriptor<>("challengePowerLimitPromotion", ValueType.INTEGER,
            new MetaItem<>(MetaItemType.LABEL, "  Challenge power limit promotion/degradation (ladder)"),
            new MetaItem<>(MetaItemType.RULE_STATE)
    ).withUnits(UNITS_PERCENTAGE);

    public static final AttributeDescriptor<Integer> CHALLENGE_POWER_LIMIT_INTERVAL = new AttributeDescriptor<>("challengePowerLimitInterval", ValueType.INTEGER,
            new MetaItem<>(MetaItemType.LABEL, "  Challenge power limit interval (ladder)"),
            new MetaItem<>(MetaItemType.RULE_STATE)
    ).withUnits(UNITS_WATT);

    public static final AttributeDescriptor<Integer> CHALLENGE_POWER_LIMIT_TARGET = new AttributeDescriptor<>("challengePowerLimitTarget", ValueType.INTEGER,
            new MetaItem<>(MetaItemType.LABEL, "  Challenge power limit target (constant/ladder)"),
            new MetaItem<>(MetaItemType.RULE_STATE)
    ).withUnits(UNITS_WATT);

    public static final AttributeDescriptor<Integer> CHALLENGE_POWER_LIMIT_MINIMUM = new AttributeDescriptor<>("challengePowerLimitMinimum", ValueType.INTEGER,
            new MetaItem<>(MetaItemType.LABEL, "  Challenge power limit minimum (ladder)"),
            new MetaItem<>(MetaItemType.RULE_STATE)
    ).withUnits(UNITS_WATT);

    public static final AttributeDescriptor<Integer> CHALLENGE_POWER_LIMIT_MAXIMUM = new AttributeDescriptor<>("challengePowerLimitMaximum", ValueType.INTEGER,
            new MetaItem<>(MetaItemType.LABEL, "  Challenge power limit maximum (ladder)"),
            new MetaItem<>(MetaItemType.RULE_STATE)
    ).withUnits(UNITS_WATT);

    public static final AttributeDescriptor<Integer> CHALLENGE_PARTICIPATION = new AttributeDescriptor<>("challengeParticipation", ValueType.POSITIVE_INTEGER,
            new MetaItem<>(MetaItemType.LABEL, " Challenge participation (# households)"),
            new MetaItem<>(MetaItemType.READ_ONLY),
            new MetaItem<>(MetaItemType.RULE_STATE),
            new MetaItem<>(MetaItemType.STORE_DATA_POINTS),
            new MetaItem<>(MetaItemType.DATA_POINTS_MAX_AGE_DAYS, 366)
    );

    public static final AttributeDescriptor<Double> CHALLENGE_PARTICIPATION_RATE = new AttributeDescriptor<>("challengeParticipationRate", ValueType.POSITIVE_NUMBER,
            new MetaItem<>(MetaItemType.LABEL, " Challenge participation rate"),
            new MetaItem<>(MetaItemType.READ_ONLY),
            new MetaItem<>(MetaItemType.RULE_STATE),
            new MetaItem<>(MetaItemType.STORE_DATA_POINTS),
            new MetaItem<>(MetaItemType.DATA_POINTS_MAX_AGE_DAYS, 366)
    ).withUnits(UNITS_PERCENTAGE);

    public static final AttributeDescriptor<Integer> CHALLENGE_POINTS_TOTAL = new AttributeDescriptor<>("challengePointsTotal", ValueType.POSITIVE_INTEGER,
            new MetaItem<>(MetaItemType.LABEL, " Challenge points total"),
            new MetaItem<>(MetaItemType.READ_ONLY),
            new MetaItem<>(MetaItemType.RULE_STATE),
            new MetaItem<>(MetaItemType.STORE_DATA_POINTS),
            new MetaItem<>(MetaItemType.DATA_POINTS_MAX_AGE_DAYS, 366)
    );

    public static final AttributeDescriptor<Integer> CHALLENGE_POINTS_MAX = new AttributeDescriptor<>("challengePointsMax", ValueType.POSITIVE_INTEGER,
            new MetaItem<>(MetaItemType.LABEL, " Challenge points per user (theoretical maximum)"),
            new MetaItem<>(MetaItemType.READ_ONLY),
            new MetaItem<>(MetaItemType.RULE_STATE),
            new MetaItem<>(MetaItemType.STORE_DATA_POINTS),
            new MetaItem<>(MetaItemType.DATA_POINTS_MAX_AGE_DAYS, 366)
    );

    public static final AttributeDescriptor<Long> CHALLENGE_POINT_TIMER_START = new AttributeDescriptor<>("challengePointTimerStart", ValueType.LONG,
            new MetaItem<>(MetaItemType.LABEL, " Challenge point timer start"),
            new MetaItem<>(MetaItemType.READ_ONLY),
            new MetaItem<>(MetaItemType.RULE_STATE),
            new MetaItem<>(MetaItemType.STORE_DATA_POINTS)
    ).withUnits(UNITS_MILLI, UNITS_SECOND);

    public static final AttributeDescriptor<Integer> CHALLENGE_BUDGET = new AttributeDescriptor<>("challengeBudged", ValueType.POSITIVE_INTEGER,
            new MetaItem<>(MetaItemType.LABEL, "Total budget (€)"),
            new MetaItem<>(MetaItemType.RULE_STATE)
    );

    public static final AttributeDescriptor<Integer> CHALLENGE_POINTS_YEAR_PREDICTION_MANUAL = new AttributeDescriptor<>("challengePointsYearPredictionManual", ValueType.POSITIVE_INTEGER,
            new MetaItem<>(MetaItemType.LABEL, "Total points year prediction manual"),
            new MetaItem<>(MetaItemType.RULE_STATE)
    );

    public static final AttributeDescriptor<Integer> CHALLENGE_POINTS_YEAR_PREDICTION_AUTOMATIC = new AttributeDescriptor<>("challengePointsYearPredictionAutomatic", ValueType.POSITIVE_INTEGER,
            new MetaItem<>(MetaItemType.LABEL, "Challenge points year prediction automatic"),
            new MetaItem<>(MetaItemType.READ_ONLY),
            new MetaItem<>(MetaItemType.RULE_STATE),
            new MetaItem<>(MetaItemType.STORE_DATA_POINTS),
            new MetaItem<>(MetaItemType.DATA_POINTS_MAX_AGE_DAYS, 366)
    );

    public static final AttributeDescriptor<Double> CHALLENGE_POINTS_EXCHANGE_RATE = new AttributeDescriptor<>("challengePointsExchangeRate", ValueType.POSITIVE_NUMBER,
            new MetaItem<>(MetaItemType.LABEL, "Total points year exchange rate (€/point)"),
            new MetaItem<>(MetaItemType.READ_ONLY),
            new MetaItem<>(MetaItemType.ACCESS_RESTRICTED_READ),
            new MetaItem<>(MetaItemType.RULE_STATE)
    );


    protected OurGridChallengesAsset() {
    }

    public OurGridChallengesAsset(String name) {
        super(name);
    }

    public Optional<ChallengeStatusGeneralValueType> getStatus() {
        return getAttributes().get(CHALLENGE_GENERAL_STATUS).flatMap(Attribute::getValue);
    }

    public Optional<String> getStartDate() {
        return getAttributes().get(CHALLENGE_START).flatMap(Attribute::getValue);
    }

    public Optional<String> getEndDate() {
        return getAttributes().get(CHALLENGE_END).flatMap(Attribute::getValue);
    }

    public Optional<Long> getChallengeWait() {
        return getAttributes().get(CHALLENGE_WAIT).flatMap(Attribute::getValue);
    }

    public OurGridChallengesAsset setTurnOnChallenges(Boolean value) {
        getAttributes().getOrCreate(TURN_ON_CHALLENGES).setValue(value);
        return this;
    }

    public OurGridChallengesAsset setChallengeWait(Long value) {
        getAttributes().getOrCreate(CHALLENGE_WAIT).setValue(value);
        return this;
    }

    public OurGridChallengesAsset setChallengeDuration(Long value) {
        getAttributes().getOrCreate(CHALLENGE_DURATION).setValue(value);
        return this;
    }

    public OurGridChallengesAsset setChallengeEarnPointInterval(Integer value) {
        getAttributes().getOrCreate(CHALLENGE_EARN_POINT_INTERVAL).setValue(value);
        return this;
    }

    ////
    public OurGridChallengesAsset setChallengeDefaultPowerLimitMethod(OurGridChallengesAsset.ChallengePowerLimitValueType value) {
        getAttributes().getOrCreate(CHALLENGE_DEFAULT_POWER_LIMIT_METHOD).setValue(value);
        return this;
    }

    public OurGridChallengesAsset setChallengePowerLimitInterval(Integer value) {
        getAttributes().getOrCreate(CHALLENGE_POWER_LIMIT_INTERVAL).setValue(value);
        return this;
    }

    public OurGridChallengesAsset setChallengePowerLimitMaximum(Integer value) {
        getAttributes().getOrCreate(CHALLENGE_POWER_LIMIT_MAXIMUM).setValue(value);
        return this;
    }

    public OurGridChallengesAsset setChallengePowerLimitMinimum(Integer value) {
        getAttributes().getOrCreate(CHALLENGE_POWER_LIMIT_MINIMUM).setValue(value);
        return this;
    }

    public OurGridChallengesAsset setChallengePowerLimitPromotion(Integer value) {
        getAttributes().getOrCreate(CHALLENGE_POWER_LIMIT_PROMOTION_PERCENTAGE).setValue(value);
        return this;
    }

    public OurGridChallengesAsset setChallengePowerLimitTarget(Integer value) {
        getAttributes().getOrCreate(CHALLENGE_POWER_LIMIT_TARGET).setValue(value);
        return this;
    }
}
