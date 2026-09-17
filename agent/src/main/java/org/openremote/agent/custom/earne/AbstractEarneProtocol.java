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

import static org.openremote.model.syslog.SyslogCategory.PROTOCOL;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import java.io.IOException;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;
import java.util.stream.Stream;
import org.openremote.agent.protocol.AbstractProtocol;
import org.openremote.manager.asset.AssetStorageService;
import org.openremote.model.Container;
import org.openremote.model.asset.agent.ConnectionStatus;
import org.openremote.model.asset.agent.DefaultAgentLink;
import org.openremote.model.attribute.Attribute;
import org.openremote.model.attribute.AttributeEvent;
import org.openremote.model.syslog.SyslogCategory;
import org.openremote.model.value.AttributeDescriptor;

/** Abstract base class for EARN-E protocol implementations. */
public abstract class AbstractEarneProtocol<T extends AbstractEarneAgent<T, ?, DefaultAgentLink>>
    extends AbstractProtocol<T, DefaultAgentLink> {

  static final Logger LOG = SyslogCategory.getLogger(PROTOCOL, AbstractEarneProtocol.class);

  static final ObjectMapper OBJECT_MAPPER = createObjectMapper();

  protected AssetStorageService assetStorageService;

  private static ObjectMapper createObjectMapper() {
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    objectMapper.registerModule(new JavaTimeModule());
    objectMapper.setDefaultPropertyInclusion(JsonInclude.Include.NON_NULL);
    objectMapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
    return objectMapper;
  }

  // RabbitMQ variables
  protected Connection connection;
  protected Channel channel;

  public AbstractEarneProtocol(T agent) {
    super(agent);
  }

  @Override
  public void start(Container container) throws Exception {
    assetStorageService = container.getService(AssetStorageService.class);
    super.start(container);
  }

  @Override
  protected void doStart(Container container) {
    connectToRabbitMQ();
  }

  @Override
  protected void doStop(Container container) {
    disconnectFromRabbitMQ();
  }

  @Override
  protected void doLinkAttribute(String assetId, Attribute<?> attribute, DefaultAgentLink agentLink)
      throws RuntimeException {}

  @Override
  protected void doUnlinkAttribute(
      String assetId, Attribute<?> attribute, DefaultAgentLink agentLink) {}

  @Override
  protected void doLinkedAttributeWrite(
      DefaultAgentLink agentLink, AttributeEvent event, Object processedValue) {}

  protected String getRabbitMqHost() {
    return agent.getRabbitMqHost().orElse("");
  }

  protected String getRabbitMqUsername() {
    return agent.getRabbitMqUsername().orElse("");
  }

  protected String getRabbitMqPassword() {
    return agent.getRabbitMqPassword().orElse("");
  }

  protected String getRabbitMqVirtualHost() {
    return agent.getRabbitMqVirtualHost().orElse("");
  }

  protected boolean isValidConfiguration() {
    return Stream.of(
            getRabbitMqHost(),
            getRabbitMqUsername(),
            getRabbitMqPassword(),
            getRabbitMqVirtualHost())
        .noneMatch(String::isBlank);
  }

  protected Connection createConnection() throws IOException, TimeoutException {
    ConnectionFactory factory = new ConnectionFactory();
    factory.setHost(getRabbitMqHost());
    factory.setUsername(getRabbitMqUsername());
    factory.setPassword(getRabbitMqPassword());
    factory.setVirtualHost(getRabbitMqVirtualHost());

    // Set automatic recovery
    factory.setAutomaticRecoveryEnabled(true);
    factory.setNetworkRecoveryInterval(10000);

    return factory.newConnection();
  }

  protected void connectToRabbitMQ() {
    if (!isValidConfiguration()) {
      setConnectionStatus(ConnectionStatus.DISCONNECTED);
      return;
    }

    ConnectionStatus connectionStatus;

    try {
      connection = createConnection();
      channel = connection.createChannel();

      try {
        consumeMessages();
        connectionStatus = ConnectionStatus.CONNECTED;
        LOG.info(
            String.format(
                "agentName='%s', agentId='%s'; Connected to RabbitMQ host",
                agent.getName(), agent.getId()));
      } catch (IOException | RuntimeException e) {
        connectionStatus = ConnectionStatus.ERROR;
        LOG.warning(
            String.format(
                "agentName='%s', agentId='%s'; Established connection with RabbitMQ host but unable to connect to queue, check credentials; Exception: %s",
                agent.getName(), agent.getId(), e));

        disconnectFromRabbitMQ();
      }
    } catch (IOException | TimeoutException | RuntimeException e) {
      connectionStatus = ConnectionStatus.ERROR;
      LOG.warning(
          String.format(
              "agentName='%s', agentId='%s'; Unable to establish connection with RabbitMQ host, check credentials; Exception: %s",
              agent.getName(), agent.getId(), e));
    }

    setConnectionStatus(connectionStatus);
  }

  protected void disconnectFromRabbitMQ() {
    try {
      if (channel != null && channel.isOpen()) {
        channel.close();
      }

      if (connection != null && connection.isOpen()) {
        connection.close();
        LOG.info(
            String.format(
                "agentName='%s', agentId='%s'; RabbitMQ connection closed",
                agent.getName(), agent.getId()));
      }

    } catch (IOException | TimeoutException e) {
      LOG.info(
          String.format(
              "agentName='%s', agentId='%s'; No RabbitMQ connection to close",
              agent.getName(), agent.getId()));
    }
  }

  protected abstract void consumeMessages() throws IOException;

  <V> void sendAttributeEvent(String assetId, AttributeDescriptor<V> descriptor, V value) {
    sendAttributeEvent(new AttributeEvent(assetId, descriptor, value));
  }
}
