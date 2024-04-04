package org.openremote.agent.custom.reschool;

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
public class ReschoolAgent extends Agent<ReschoolAgent, ReschoolProtocol, DefaultAgentLink> { // agent makes the fields in the UI
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


    public static final AgentDescriptor<ReschoolAgent, ReschoolProtocol, DefaultAgentLink> DESCRIPTOR = new AgentDescriptor<>(
            ReschoolAgent.class, ReschoolProtocol.class, DefaultAgentLink.class);

    protected ReschoolAgent() { // default constructor
    }

    public ReschoolAgent(String name){
        super (name);
    }

    @Override
    public ReschoolProtocol getProtocolInstance() {
        return new ReschoolProtocol(this);
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

    public ReschoolAgent setMeterParentId(String value) {
        getAttributes().getOrCreate(METER_SUM_ID).setValue(value);
        return this;
    }
    public ReschoolAgent setRabbitMqHost(String value) {
        getAttributes().getOrCreate(RABBITMQ_HOST).setValue(value);
        return this;
    }

    public ReschoolAgent setRabbitMqUsername(String value) {
        getAttributes().getOrCreate(RABBITMQ_USERNAME).setValue(value);
        return this;
    }

    public ReschoolAgent setRabbitMqPassword(String value) {
        getAttributes().getOrCreate(RABBITMQ_PASSWORD).setValue(value);
        return this;
    }

    public ReschoolAgent setRabbitMqVirtualhost(String value) {
        getAttributes().getOrCreate(RABBITMQ_VIRTUALHOST).setValue(value);
        return this;
    }

    public ReschoolAgent setRabbitMqQueue(String value) {
        getAttributes().getOrCreate(RABBITMQ_QUEUE).setValue(value);
        return this;
    }

}
