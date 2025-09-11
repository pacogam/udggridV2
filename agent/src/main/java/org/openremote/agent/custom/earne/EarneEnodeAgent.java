/*
 * Copyright 2025, OpenRemote Inc.
 *
 * See the CONTRIBUTORS.txt file in the distribution for a
 * full listing of individual contributors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.openremote.agent.custom.earne;

import jakarta.persistence.Entity;
import org.openremote.model.asset.agent.AgentDescriptor;
import org.openremote.model.asset.agent.DefaultAgentLink;
import org.openremote.model.attribute.MetaItem;
import org.openremote.model.value.AttributeDescriptor;
import org.openremote.model.value.MetaItemType;
import org.openremote.model.value.ValueType;

import java.util.Optional;

/**
 * EARN-E Enode Agent.
 */
@Entity
public class EarneEnodeAgent extends AbstractEarneAgent<EarneEnodeAgent, EarneEnodeProtocol, DefaultAgentLink> {

    public static final AttributeDescriptor<String> RABBITMQ_CONTROL_QUEUE = new AttributeDescriptor<>("rabbitMqControlQueue", ValueType.TEXT,
            new MetaItem<>(MetaItemType.LABEL, "RabbitMQ Control Queue")
    );

    public static final AttributeDescriptor<String> RABBITMQ_UPDATE_QUEUE = new AttributeDescriptor<>("rabbitMqDataQueue", ValueType.TEXT,
            new MetaItem<>(MetaItemType.LABEL, "RabbitMQ Update Queue")
    );

    public static final AgentDescriptor<EarneEnodeAgent, EarneEnodeProtocol, DefaultAgentLink> DESCRIPTOR = new AgentDescriptor<>(EarneEnodeAgent.class, EarneEnodeProtocol.class, DefaultAgentLink.class);

    protected EarneEnodeAgent() {
    }

    public EarneEnodeAgent(String name) {
        super(name);
    }

    @Override
    public EarneEnodeProtocol getProtocolInstance() {
        return new EarneEnodeProtocol(this);
    }

    public Optional<String> getRabbitMqControlQueue() {
        return getAttributes().getValue(RABBITMQ_CONTROL_QUEUE);
    }

    public Optional<String> getRabbitMqUpdateQueue() {
        return getAttributes().getValue(RABBITMQ_UPDATE_QUEUE);
    }

}
