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

import java.util.Optional;
import org.openremote.model.asset.agent.Agent;
import org.openremote.model.asset.agent.AgentLink;
import org.openremote.model.asset.agent.Protocol;
import org.openremote.model.attribute.MetaItem;
import org.openremote.model.value.AttributeDescriptor;
import org.openremote.model.value.MetaItemType;
import org.openremote.model.value.ValueType;

/** Abstract class for EARN-E agents. */
public abstract class AbstractEarneAgent<
        T extends Agent<T, U, V>, U extends Protocol<T>, V extends AgentLink<?>>
    extends Agent<T, U, V> {

  public static final AttributeDescriptor<String> RABBITMQ_HOST =
      new AttributeDescriptor<>(
          "rabbitMqHost", ValueType.TEXT, new MetaItem<>(MetaItemType.LABEL, "RabbitMQ Host"));

  public static final AttributeDescriptor<String> RABBITMQ_VIRTUAL_HOST =
      new AttributeDescriptor<>(
          "rabbitMqVirtualHost",
          ValueType.TEXT,
          new MetaItem<>(MetaItemType.LABEL, "RabbitMQ Project ID"));

  public static final AttributeDescriptor<String> RABBITMQ_USERNAME =
      new AttributeDescriptor<>(
          "rabbitMqUsername",
          ValueType.TEXT,
          new MetaItem<>(MetaItemType.LABEL, "RabbitMQ Username"));

  public static final AttributeDescriptor<String> RABBITMQ_PASSWORD =
      new AttributeDescriptor<>(
          "rabbitMqPassword",
          ValueType.TEXT,
          new MetaItem<>(MetaItemType.LABEL, "RabbitMQ Password"));

  public AbstractEarneAgent() {
    super();
  }

  public AbstractEarneAgent(String name) {
    super(name);
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
}
