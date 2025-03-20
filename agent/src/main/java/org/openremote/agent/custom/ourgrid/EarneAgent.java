package org.openremote.agent.custom.ourgrid;

import jakarta.persistence.Entity;
import org.openremote.model.asset.agent.Agent;
import org.openremote.model.asset.agent.AgentDescriptor;
import org.openremote.model.asset.agent.DefaultAgentLink;
import org.openremote.model.attribute.MetaItem;
import org.openremote.model.value.AttributeDescriptor;
import org.openremote.model.value.MetaItemType;
import org.openremote.model.value.ValueType;

import java.util.Optional;

@Entity
public class EarneAgent extends Agent<EarneAgent, EarneProtocol, DefaultAgentLink> {

    public static final AttributeDescriptor<String> METER_PARENT_ID = new AttributeDescriptor<>("meterParentId", ValueType.TEXT,
            new MetaItem<>(MetaItemType.LABEL, "Meter Parent ID")
    );

    public static final AttributeDescriptor<String> RABBITMQ_HOST = new AttributeDescriptor<>("rabbitMqHost", ValueType.TEXT,
            new MetaItem<>(MetaItemType.LABEL, "RabbitMQ Host")
    );

    public static final AttributeDescriptor<String> RABBITMQ_PASSWORD = new AttributeDescriptor<>("rabbitMqPassword", ValueType.TEXT,
            new MetaItem<>(MetaItemType.LABEL, "RabbitMQ Password")
    );
    public static final AttributeDescriptor<String> RABBITMQ_VIRTUAL_HOST = new AttributeDescriptor<>("rabbitMqVirtualHost", ValueType.TEXT,
            new MetaItem<>(MetaItemType.LABEL, "RabbitMQ Project ID")
    );

    public static final AttributeDescriptor<String> RABBITMQ_QUEUE = new AttributeDescriptor<>("rabbitMqQueue", ValueType.TEXT,
            new MetaItem<>(MetaItemType.LABEL, "RabbitMQ Project Queue")
    );

    public static final AttributeDescriptor<String> RABBITMQ_USERNAME = new AttributeDescriptor<>("rabbitMqUsername", ValueType.TEXT,
            new MetaItem<>(MetaItemType.LABEL, "RabbitMQ Username")
    );


    public static final AgentDescriptor<EarneAgent, EarneProtocol, DefaultAgentLink> DESCRIPTOR = new AgentDescriptor<>(EarneAgent.class, EarneProtocol.class, DefaultAgentLink.class);

    protected EarneAgent() {
    }

    public EarneAgent(String name) {
        super(name);
    }

    @Override
    public EarneProtocol getProtocolInstance() {
        return new EarneProtocol(this);
    }

    public Optional<String> getMeterParentId() {
        return getAttributes().getValue(METER_PARENT_ID);
    }

    public Optional<String> getRabbitMqHost() {
        return getAttributes().getValue(RABBITMQ_HOST);
    }

    public Optional<String> getRabbitMqUsername() {
        return getAttributes().getValue(RABBITMQ_USERNAME);
    }

    public Optional<String> getRabbitMqPassword() {
        return getAttributes().getValue(RABBITMQ_PASSWORD);
    }

    public Optional<String> getRabbitMqVirtualHost() {
        return getAttributes().getValue(RABBITMQ_VIRTUAL_HOST);
    }

    public Optional<String> getRabbitMqQueue() {
        return getAttributes().getValue(RABBITMQ_QUEUE);
    }
}