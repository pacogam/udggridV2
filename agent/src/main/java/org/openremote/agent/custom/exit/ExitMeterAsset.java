package org.openremote.agent.custom.exit;


//import org.openremote.agent.custom.ourgrid.OurgridMeterAsset;
import org.openremote.model.asset.Asset;
import org.openremote.model.asset.AssetDescriptor;
import org.openremote.model.attribute.Attribute;
import org.openremote.model.attribute.MetaItem;
//import org.openremote.model.exit.ShellyDeviceCharacteristic;
//import org.openremote.model.util.ValueUtil;
import org.openremote.model.value.AttributeDescriptor;
import org.openremote.model.value.MetaItemType;
import org.openremote.model.value.ValueDescriptor;
import org.openremote.model.value.ValueType;
import org.openremote.agent.custom.ourgrid.OurgridMeterAsset.ChallengeStatusValueType;
import org.openremote.agent.custom.ourgrid.OurgridMeterAsset.ConnectionStatusValueType;

import jakarta.persistence.Entity;

import java.util.Optional;

import static org.openremote.model.Constants.*;

@Entity
public class ExitMeterAsset extends Asset<ExitMeterAsset> {

  public static final AssetDescriptor<ExitMeterAsset> DESCRIPTOR = new AssetDescriptor<>("power-plug", "3d85c6", ExitMeterAsset.class); // set icon and colour of asset

  /*public enum ConnStatusValueType {
    connected,
    disconnected
  }*/

  /*public enum ConnStatusValueType {
    CONNECTED,
    DISCONNECTED
  }*/

  public static final ValueDescriptor<ConnectionStatusValueType> CONNECTION_STATUS_VALUE_TYPE = new ValueDescriptor<>("ConnectionStatusValueType", ConnectionStatusValueType.class);

  public static final AttributeDescriptor<ConnectionStatusValueType> CONNECTION_STATUS = new AttributeDescriptor<>("connectionStatus", CONNECTION_STATUS_VALUE_TYPE,
    new MetaItem<>(MetaItemType.LABEL, "   Connection status"),
    new MetaItem<>(MetaItemType.READ_ONLY),
    new MetaItem<>(MetaItemType.ACCESS_RESTRICTED_READ),
    new MetaItem<>(MetaItemType.RULE_STATE),
    new MetaItem<>(MetaItemType.STORE_DATA_POINTS)
  );

  /*public enum ChallengeStatusValueType {
    joinChallenge,
    joinedChallenge,
    activeChallenge,
    inactiveChallenge,
    noChallenge
  }*/

  //public static final ValueDescriptor<ExitMeterAsset.ChallengeStatusValueType> CHALLENGE_STATUS_VALUE_TYPE = new ValueDescriptor<>("challengeStatusValueType", ExitMeterAsset.ChallengeStatusValueType.class);
  public static final ValueDescriptor<ChallengeStatusValueType> CHALLENGE_STATUS_VALUE_TYPE = new ValueDescriptor<>("challengeStatusValueType", ChallengeStatusValueType.class);
  public static final AttributeDescriptor<ChallengeStatusValueType> CHALLENGE_STATUS = new AttributeDescriptor<>("challengeStatus", CHALLENGE_STATUS_VALUE_TYPE,
    new MetaItem<>(MetaItemType.LABEL, "   Challenge status"),
    new MetaItem<>(MetaItemType.READ_ONLY),
    new MetaItem<>(MetaItemType.ACCESS_RESTRICTED_READ),
    new MetaItem<>(MetaItemType.RULE_STATE)
  );

  public static final AttributeDescriptor<Integer> CHALLENGES_JOINED = new AttributeDescriptor<>("challengesJoined", ValueType.POSITIVE_INTEGER,
    new MetaItem<>(MetaItemType.LABEL, "  Challenges joined"),
    new MetaItem<>(MetaItemType.READ_ONLY),
    new MetaItem<>(MetaItemType.ACCESS_RESTRICTED_READ),
    new MetaItem<>(MetaItemType.RULE_STATE),
    new MetaItem<>(MetaItemType.STORE_DATA_POINTS)
  );

//    public static final ValueDescriptor<ExitMeterAsset.ConnStatusValueType> CONNECTION_STATUS_VALUE_TYPE = new ValueDescriptor<>("ConnStatusValueType", ExitMeterAsset.ConnStatusValueType.class);

//    public static final AttributeDescriptor<ExitMeterAsset.ConnStatusValueType> CONNECTION_STATUS = new AttributeDescriptor<>("connectionStatus", CONNECTION_STATUS_VALUE_TYPE,
//            new MetaItem<>(MetaItemType.LABEL, "   Connection status"),
//            new MetaItem<>(MetaItemType.READ_ONLY),
//            new MetaItem<>(MetaItemType.ACCESS_RESTRICTED_READ),
//            new MetaItem<>(MetaItemType.RULE_STATE),
//            new MetaItem<>(MetaItemType.STORE_DATA_POINTS)
//    );

//    public static final AttributeDescriptor<String> DEVICE_ID = new AttributeDescriptor<>("deviceId", ValueType.TEXT,
//            new MetaItem<>(MetaItemType.LABEL, "Shelly ID"),
//            new MetaItem<>(MetaItemType.READ_ONLY),
//            new MetaItem<>(MetaItemType.ACCESS_RESTRICTED_READ)
//    );

//    public static final AttributeDescriptor<String> TIMESTAMP = new AttributeDescriptor<>("timestamp", ValueType.TEXT,
//            new MetaItem<>(MetaItemType.LABEL, " Timestamp"),
//            new MetaItem<>(MetaItemType.READ_ONLY),
//            new MetaItem<>(MetaItemType.ACCESS_RESTRICTED_READ)
//    );

  public static final AttributeDescriptor<Double> ENERGY = new AttributeDescriptor<>("energyMinute", ValueType.NUMBER,
    new MetaItem<>(MetaItemType.LABEL, "Energy"),
    new MetaItem<>(MetaItemType.READ_ONLY),
    new MetaItem<>(MetaItemType.ACCESS_RESTRICTED_READ),
    new MetaItem<>(MetaItemType.RULE_STATE),
    new MetaItem<>(MetaItemType.STORE_DATA_POINTS)
  ).withUnits(UNITS_WATT, UNITS_MINUTE);

  public static final AttributeDescriptor<Double> RETURNED_ENERGY = new AttributeDescriptor<>("returnedEnergyMinute", ValueType.NUMBER,
    new MetaItem<>(MetaItemType.LABEL, "Returned Energy"),
    new MetaItem<>(MetaItemType.READ_ONLY),
    new MetaItem<>(MetaItemType.ACCESS_RESTRICTED_READ),
    new MetaItem<>(MetaItemType.RULE_STATE),
    new MetaItem<>(MetaItemType.STORE_DATA_POINTS)
  ).withUnits(UNITS_WATT, UNITS_MINUTE);

  public static final AttributeDescriptor<Double> TOTAL_ENERGY = new AttributeDescriptor<>("totalEnergy", ValueType.NUMBER,
    new MetaItem<>(MetaItemType.LABEL, "Total Energy"),
    new MetaItem<>(MetaItemType.READ_ONLY),
    new MetaItem<>(MetaItemType.ACCESS_RESTRICTED_READ),
    new MetaItem<>(MetaItemType.RULE_STATE),
    new MetaItem<>(MetaItemType.STORE_DATA_POINTS)
  ).withUnits(UNITS_WATT, UNITS_HOUR);

  public static final AttributeDescriptor<Double> TOTAL_RETURNED_ENERGY = new AttributeDescriptor<>("totalReturnedEnergy", ValueType.NUMBER,
    new MetaItem<>(MetaItemType.LABEL, "Total Returned Energy"),
    new MetaItem<>(MetaItemType.READ_ONLY),
    new MetaItem<>(MetaItemType.ACCESS_RESTRICTED_READ),
    new MetaItem<>(MetaItemType.RULE_STATE),
    new MetaItem<>(MetaItemType.STORE_DATA_POINTS)
  ).withUnits(UNITS_WATT, UNITS_HOUR);

  public static final AttributeDescriptor<Double> POWER = new AttributeDescriptor<>("power", ValueType.NUMBER,
    new MetaItem<>(MetaItemType.LABEL, "Power"),
    new MetaItem<>(MetaItemType.READ_ONLY),
    new MetaItem<>(MetaItemType.ACCESS_RESTRICTED_READ),
    new MetaItem<>(MetaItemType.RULE_STATE),
    new MetaItem<>(MetaItemType.STORE_DATA_POINTS)
  ).withUnits(UNITS_WATT);

  public static final AttributeDescriptor<Double> REACTIVE_POWER = new AttributeDescriptor<>("reactivePower", ValueType.NUMBER,
    new MetaItem<>(MetaItemType.LABEL, "Reactive Power"),
    new MetaItem<>(MetaItemType.READ_ONLY),
    new MetaItem<>(MetaItemType.ACCESS_RESTRICTED_READ),
    new MetaItem<>(MetaItemType.RULE_STATE),
    new MetaItem<>(MetaItemType.STORE_DATA_POINTS)
  ).withUnits(UNITS_WATT);

  public static final AttributeDescriptor<Double> VOLTAGE = new AttributeDescriptor<>("voltage", ValueType.NUMBER,
    new MetaItem<>(MetaItemType.LABEL, "Voltage"),
    new MetaItem<>(MetaItemType.READ_ONLY),
    new MetaItem<>(MetaItemType.ACCESS_RESTRICTED_READ),
    new MetaItem<>(MetaItemType.RULE_STATE),
    new MetaItem<>(MetaItemType.STORE_DATA_POINTS)
  ).withUnits(UNITS_VOLT);

  public static final AttributeDescriptor<Double> BATT_POWER_SYSTEM = new AttributeDescriptor<>("batteryPoweredSystem", ValueType.NUMBER,
    new MetaItem<>(MetaItemType.LABEL, "Battery-Powered System"),
    new MetaItem<>(MetaItemType.READ_ONLY),
    new MetaItem<>(MetaItemType.ACCESS_RESTRICTED_READ),
    new MetaItem<>(MetaItemType.RULE_STATE),
    new MetaItem<>(MetaItemType.STORE_DATA_POINTS)
  ).withUnits(UNITS_WATT);

  public static final AttributeDescriptor<Double> PV_POWER = new AttributeDescriptor<>("pvpower", ValueType.NUMBER,
    new MetaItem<>(MetaItemType.LABEL, "PV Power"),
    new MetaItem<>(MetaItemType.READ_ONLY),
    new MetaItem<>(MetaItemType.ACCESS_RESTRICTED_READ),
    new MetaItem<>(MetaItemType.RULE_STATE),
    new MetaItem<>(MetaItemType.STORE_DATA_POINTS)
  ).withUnits(UNITS_WATT);

  public static final AttributeDescriptor<Double> PV_PERCENTAGE= new AttributeDescriptor<>("PVPercentage", ValueType.NUMBER,
    new MetaItem<>(MetaItemType.LABEL, "PV Percentage"),
    new MetaItem<>(MetaItemType.READ_ONLY),
    new MetaItem<>(MetaItemType.ACCESS_RESTRICTED_READ),
    new MetaItem<>(MetaItemType.RULE_STATE)
  ).withUnits(UNITS_PERCENTAGE);

  protected ExitMeterAsset() {
  }

  public ExitMeterAsset(String name) {
    super(name);
  }

  public Optional<ChallengeStatusValueType> getChallengeStatus() {
    return getAttributes().get(CHALLENGE_STATUS).flatMap(Attribute::getValue);
  }

  public Optional<Integer> getChallengesJoined() {
    return getAttributes().get(CHALLENGES_JOINED).flatMap(Attribute::getValue);
  }
//    public ExitMeterAsset setDeviceId(String value) {
//        getAttributes().getOrCreate(DEVICE_ID).setValue(value);
//        return this;
//    }

}
