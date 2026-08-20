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

import com.rabbitmq.client.DeliverCallback;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;
import org.openremote.model.Container;
import org.openremote.model.syslog.SyslogCategory;

/** EARN-E meter protocol implementation. */
public class EarneMeterProtocol extends AbstractEarneProtocol<EarneMeterAgent> {

  static final Logger LOG = SyslogCategory.getLogger(PROTOCOL, EarneMeterProtocol.class);

  public static final String PROTOCOL_DISPLAY_NAME = "Earne";

  // Message handlers
  private EarneMeterMessageHandler meterMessageHandler;

  public EarneMeterProtocol(EarneMeterAgent agent) {
    super(agent);
  }

  @Override
  public String getProtocolName() {
    return PROTOCOL_DISPLAY_NAME;
  }

  @Override
  public String getProtocolInstanceUri() {
    return "earne://" + agent.getId();
  }

  @Override
  protected void doStart(Container container) {
    meterMessageHandler = new EarneMeterMessageHandler(agent, assetService, assetStorageService);
    super.doStart(container);
  }

  protected String getRabbitMqMeterQueue() {
    return agent.getRabbitMqMeterQueue().orElse("");
  }

  @Override
  protected boolean isValidConfiguration() {
    return super.isValidConfiguration() && !getRabbitMqMeterQueue().isBlank();
  }

  @Override
  protected void consumeMessages() throws IOException {
    DeliverCallback deliverCallback =
        (consumerTag, delivery) -> {
          String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
          try {
            meterMessageHandler.handleMessage(message);
          } catch (RuntimeException e) {
            LOG.info(
                String.format(
                    "agentName='%s', agentId='%s'; Meter message not processed: '%s'; Exception: %s",
                    agent.getName(), agent.getId(), message, e));
          }
        };

    channel.basicConsume(getRabbitMqMeterQueue(), true, deliverCallback, consumerTag -> {});
  }
}
