/*
 * Copyright 2026, OpenRemote Inc.
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
package org.openremote.agent.custom.mygrid;

import org.openremote.agent.protocol.mqtt.MQTTAgent;
import org.openremote.agent.protocol.mqtt.MQTTProtocol;
import org.openremote.agent.protocol.mqtt.MQTT_IOClient;

/**
 * Custom MQTT protocol implementation that extends the base MQTT protocol for MyGrid integration.
 * Mostly required to call the MQTT protocol constructor via the MyGridProtocol;
 *
 * @see MyGridProtocol
 * @see MQTTProtocol
 */
public class MyGridMQTTProtocol extends MQTTProtocol {
  protected MyGridMQTTProtocol(MQTTAgent agent) {
    super(agent);
  }

  public MQTT_IOClient getMQTTClient() {
    return this.client;
  }
}
