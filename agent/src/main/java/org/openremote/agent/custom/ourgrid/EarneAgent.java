package org.openremote.agent.custom.ourgrid;

import org.openremote.model.asset.agent.Agent;
import org.openremote.model.asset.agent.AgentDescriptor;
import org.openremote.model.asset.agent.DefaultAgentLink;
import org.openremote.model.attribute.MetaItem;
import org.openremote.model.value.AttributeDescriptor;
import org.openremote.model.value.MetaItemType;
import org.openremote.model.value.ValueType;

import jakarta.persistence.Entity;
import java.util.Optional;

import static org.openremote.model.Constants.UNITS_MINUTE;


@Entity // makes table in database
public class EarneAgent extends Agent<EarneAgent, EarneProtocol, DefaultAgentLink> { // agent makes the fields in the UI
    public static final AttributeDescriptor<Integer> ACTIVE_PERIOD = new AttributeDescriptor<>("activePeriod", ValueType.POSITIVE_INTEGER,
            new MetaItem<>(MetaItemType.LABEL, "Active Period")
    ).withUnits(UNITS_MINUTE);

    public static final AttributeDescriptor<String> METER_SUM_ID = new AttributeDescriptor<>("meterParentId", ValueType.TEXT,
            new MetaItem<>(MetaItemType.LABEL, "Meter Parent ID")
    );

    public static final AttributeDescriptor<Integer> NUMBER_OF_ACTIVE_DEVICES = new AttributeDescriptor<>("numberOfActiveDevices", ValueType.INTEGER,
            new MetaItem<>(MetaItemType.LABEL, "Number of active Earn-E devices"),
            new MetaItem<>(MetaItemType.READ_ONLY),
            new MetaItem<>(MetaItemType.STORE_DATA_POINTS)
    );
    public static final AttributeDescriptor<String> RABBITMQ_HOST = new AttributeDescriptor<>("rabbitMqHost", ValueType.TEXT,
            new MetaItem<>(MetaItemType.LABEL, "RabbitMQ Host")
    );
    public static final AttributeDescriptor<String> RABBITMQ_USERNAME = new AttributeDescriptor<>("rabbitMqUsername", ValueType.TEXT,
            new MetaItem<>(MetaItemType.LABEL, "RabbitMQ Username")
    );
    public static final AttributeDescriptor<String> RABBITMQ_PASSWORD = new AttributeDescriptor<>("rabbitMqPassword", ValueType.TEXT,
            new MetaItem<>(MetaItemType.LABEL, "RabbitMQ Password")
    );
    public static final AttributeDescriptor<String> RABBITMQ_VIRTUALHOST = new AttributeDescriptor<>("rabbitMqVirtualhost", ValueType.TEXT,
            new MetaItem<>(MetaItemType.LABEL, "RabbitMQ Project ID")
    );
    public static final AttributeDescriptor<String> RABBITMQ_QUEUE = new AttributeDescriptor<>("rabbitMqQueue", ValueType.TEXT,
            new MetaItem<>(MetaItemType.LABEL, "RabbitMQ Project Queue")
    );


    public static final AgentDescriptor<EarneAgent, EarneProtocol, DefaultAgentLink> DESCRIPTOR = new AgentDescriptor<>(
            EarneAgent.class, EarneProtocol.class, DefaultAgentLink.class);

    protected EarneAgent() { // default constructor
    }

    public EarneAgent(String name){
        super (name);
    }

    @Override
    public EarneProtocol getProtocolInstance() {
        return new EarneProtocol(this);
    }

    public Optional<Integer> getActivePeriod(){
        return getAttributes().getValue(ACTIVE_PERIOD);
    }

    public Optional<String> getMeterSParentId(){
        return getAttributes().getValue(METER_SUM_ID);
    }

    public Optional<String> getRabbitMqHost(){
        return getAttributes().getValue(RABBITMQ_HOST);
    }

    public Optional<String> getRabbitMqUsername(){
        return getAttributes().getValue(RABBITMQ_USERNAME);
    }

    public Optional<String> getRabbitMqPassword(){
        return getAttributes().getValue(RABBITMQ_PASSWORD);
    }

    public Optional<String> getRabbitMqVirtualhost(){
        return getAttributes().getValue(RABBITMQ_VIRTUALHOST);
    }

    public Optional<String> getRabbitMqQueue(){
        return getAttributes().getValue(RABBITMQ_QUEUE);
    }

    public EarneAgent setMeterParentId(String value) {
        getAttributes().getOrCreate(METER_SUM_ID).setValue(value);
        return this;
    }
    public EarneAgent setRabbitMqHost(String value) {
        getAttributes().getOrCreate(RABBITMQ_HOST).setValue(value);
        return this;
    }

    public EarneAgent setRabbitMqUsername(String value) {
        getAttributes().getOrCreate(RABBITMQ_USERNAME).setValue(value);
        return this;
    }

    public EarneAgent setRabbitMqPassword(String value) {
        getAttributes().getOrCreate(RABBITMQ_PASSWORD).setValue(value);
        return this;
    }

    public EarneAgent setRabbitMqVirtualhost(String value) {
        getAttributes().getOrCreate(RABBITMQ_VIRTUALHOST).setValue(value);
        return this;
    }

    public EarneAgent setRabbitMqQueue(String value) {
        getAttributes().getOrCreate(RABBITMQ_QUEUE).setValue(value);
        return this;
    }

}
