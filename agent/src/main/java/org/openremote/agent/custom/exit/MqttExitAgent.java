package org.openremote.agent.custom.exit;

import org.openremote.agent.protocol.mqtt.MQTTAgent;
import org.openremote.model.asset.agent.Agent;
import org.openremote.model.asset.agent.AgentDescriptor;
import org.openremote.model.asset.agent.DefaultAgentLink;
import org.openremote.model.attribute.MetaItem;
import org.openremote.model.value.AttributeDescriptor;
import org.openremote.model.value.MetaItemType;
import org.openremote.model.value.ValueType;

import jakarta.persistence.Entity;
import java.util.Optional;

//import static org.openremote.model.Constants.UNITS_MINUTE;


@Entity // makes table in database
public class MqttExitAgent extends Agent<MqttExitAgent, MqttExitProtocol, DefaultAgentLink> { // agent makes the fields in the UI
//    public static final AttributeDescriptor<Integer> ACTIVE_PERIOD = new AttributeDescriptor<>("activePeriod", ValueType.POSITIVE_INTEGER,
//            new MetaItem<>(MetaItemType.LABEL, "Active Period")
//    ).withUnits(UNITS_MINUTE);

//    public static final AttributeDescriptor<String> METER_SUM_ID = new AttributeDescriptor<>("meterParentId", ValueType.TEXT,
//            new MetaItem<>(MetaItemType.LABEL, "Meter Parent ID")
//    );

  //    public static final AttributeDescriptor<Integer> NUMBER_OF_ACTIVE_DEVICES = new AttributeDescriptor<>("numberOfActiveDevices", ValueType.INTEGER,
//            new MetaItem<>(MetaItemType.LABEL, "Number of active Shelly devices"),
//            new MetaItem<>(MetaItemType.READ_ONLY),
//            new MetaItem<>(MetaItemType.STORE_DATA_POINTS)
//    );
  public static final AttributeDescriptor<String> MQTT_HOST = new AttributeDescriptor<>("mqttHost", ValueType.TEXT,
    new MetaItem<>(MetaItemType.LABEL, "MQTT Host")
  );
  public static final AttributeDescriptor<String> MQTT_PORT = new AttributeDescriptor<>("mqttPort", ValueType.TEXT,
    new MetaItem<>(MetaItemType.LABEL, "MQTT Port")
  );
  public static final AttributeDescriptor<String> MQTT_USERNAME = new AttributeDescriptor<>("mqttUsername", ValueType.TEXT,
    new MetaItem<>(MetaItemType.LABEL, "MQTT Username")
  );
  public static final AttributeDescriptor<String> MQTT_PASSWORD = new AttributeDescriptor<>("mqttPassword", ValueType.TEXT,
    new MetaItem<>(MetaItemType.LABEL, "MQTT Password")
  );
  public static final AttributeDescriptor<String> MQTT_TOPIC = new AttributeDescriptor<>("mqttTopic", ValueType.TEXT,
    new MetaItem<>(MetaItemType.LABEL, "MQTT Topic")
  );
  public static final AttributeDescriptor<Boolean> RESUME_SESSION = new AttributeDescriptor<>("resumeSession", ValueType.BOOLEAN);
//    public static final AttributeDescriptor<String> RABBITMQ_VIRTUALHOST = new AttributeDescriptor<>("rabbitMqVirtualhost", ValueType.TEXT,
//            new MetaItem<>(MetaItemType.LABEL, "RabbitMQ Project ID")
//    );
//    public static final AttributeDescriptor<String> RABBITMQ_QUEUE = new AttributeDescriptor<>("rabbitMqQueue", ValueType.TEXT,
//            new MetaItem<>(MetaItemType.LABEL, "RabbitMQ Project Queue")
//    );


  public static final AgentDescriptor<MqttExitAgent, MqttExitProtocol, DefaultAgentLink> DESCRIPTOR = new AgentDescriptor<>(
    MqttExitAgent.class, MqttExitProtocol.class, DefaultAgentLink.class);

  protected MqttExitAgent() { // default constructor
  }

  public MqttExitAgent(String name){
    super (name);
  }

  @Override
  public MqttExitProtocol getProtocolInstance() {
    return new MqttExitProtocol(this);
  }

//    public Optional<Integer> getActivePeriod(){
//        return getAttributes().getValue(ACTIVE_PERIOD);
//    }

//    public Optional<String> getMeterSParentId(){
//        return getAttributes().getValue(METER_SUM_ID);
//    }

  public Optional<String> getMqttHost(){
    return getAttributes().getValue(MQTT_HOST);
  }

  public Optional<String> getMqttPort(){
    return getAttributes().getValue(MQTT_PORT);
  }

  public Optional<String> getMqttUsername(){
    return getAttributes().getValue(MQTT_USERNAME);
  }

  public Optional<String> getMqttPassword(){
    return getAttributes().getValue(MQTT_PASSWORD);
  }

  public Optional<String> getMqttTopic(){
    return getAttributes().getValue(MQTT_TOPIC);
  }

  public Optional<Boolean> isResumeSession() {
    return getAttributes().getValue(RESUME_SESSION);
  }

  public MqttExitAgent setResumeSession(boolean resumeSession) {
    getAttributes().getOrCreate(RESUME_SESSION).setValue(resumeSession);
    return this;
  }

//    public Optional<String> getRabbitMqVirtualhost(){
//        return getAttributes().getValue(RABBITMQ_VIRTUALHOST);
//    }

//    public Optional<String> getRabbitMqQueue(){
//        return getAttributes().getValue(RABBITMQ_QUEUE);
//    }

  //    public MqttExitAgent setMeterParentId(String value) {
//        getAttributes().getOrCreate(METER_SUM_ID).setValue(value);
//        return this;
//    }
  public MqttExitAgent setMqttHost(String value) {
    getAttributes().getOrCreate(MQTT_HOST).setValue(value);
    return this;
  }

  public MqttExitAgent setMqttPort(String value) {
    getAttributes().getOrCreate(MQTT_PORT).setValue(value);
    return this;
  }

  public MqttExitAgent setMqttUsername(String value) {
    getAttributes().getOrCreate(MQTT_USERNAME).setValue(value);
    return this;
  }

  public MqttExitAgent setMqttPassword(String value) {
    getAttributes().getOrCreate(MQTT_PASSWORD).setValue(value);
    return this;
  }

}
