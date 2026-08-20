/*
 * Copyright 2025, OpenRemote Inc.
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
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package org.openremote.agent.custom.earne;

import jakarta.persistence.Entity;
import java.util.Optional;
import org.openremote.model.asset.agent.AgentDescriptor;
import org.openremote.model.asset.agent.DefaultAgentLink;
import org.openremote.model.attribute.MetaItem;
import org.openremote.model.value.AttributeDescriptor;
import org.openremote.model.value.MetaItemType;
import org.openremote.model.value.ValueType;

/** EARN-E Meter Agent. */
@Entity
public class EarneMeterAgent
    extends AbstractEarneAgent<EarneMeterAgent, EarneMeterProtocol, DefaultAgentLink> {

  public static final AttributeDescriptor<String> RABBITMQ_METER_QUEUE =
      new AttributeDescriptor<>(
          "rabbitMqQueue",
          ValueType.TEXT,
          new MetaItem<>(MetaItemType.LABEL, "RabbitMQ Meter Queue"));

  public static final AttributeDescriptor<String> METER_PARENT_ID =
      new AttributeDescriptor<>(
          "meterParentId", ValueType.TEXT, new MetaItem<>(MetaItemType.LABEL, "Meter Parent ID"));

  public static final AgentDescriptor<EarneMeterAgent, EarneMeterProtocol, DefaultAgentLink>
      DESCRIPTOR =
          new AgentDescriptor<>(
              EarneMeterAgent.class, EarneMeterProtocol.class, DefaultAgentLink.class);

  protected EarneMeterAgent() {
    super();
  }

  public EarneMeterAgent(String name) {
    super(name);
  }

  @Override
  public EarneMeterProtocol getProtocolInstance() {
    return new EarneMeterProtocol(this);
  }

  public Optional<String> getMeterParentId() {
    return getAttributes().getValue(METER_PARENT_ID);
  }

  public Optional<String> getRabbitMqMeterQueue() {
    return getAttributes().getValue(RABBITMQ_METER_QUEUE);
  }
}
