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

import jakarta.persistence.Entity;
import java.util.Optional;
import org.openremote.agent.protocol.mqtt.MQTTAgent;
import org.openremote.agent.protocol.mqtt.MQTTAgentLink;
import org.openremote.model.asset.agent.Agent;
import org.openremote.model.asset.agent.AgentDescriptor;
import org.openremote.model.value.AttributeDescriptor;
import org.openremote.model.value.ValueType;

/**
 * Custom agent implementation that extends the base Agent class for MyGrid integration.
 *
 * @see MyGridProtocol
 * @see MyGridMQTTProtocol
 */
@Entity
public class MyGridAgent extends Agent<MyGridAgent, MyGridProtocol, MQTTAgentLink> {

  public static final AgentDescriptor<MyGridAgent, MyGridProtocol, MQTTAgentLink> DESCRIPTOR =
      new AgentDescriptor<>(MyGridAgent.class, MyGridProtocol.class, MQTTAgentLink.class);

  public static final AttributeDescriptor<String> HOST = MQTTAgent.HOST;
  public static final AttributeDescriptor<Integer> PORT = MQTTAgent.PORT;
  public static final AttributeDescriptor<String> CLIENT_ID = MQTTAgent.CLIENT_ID;
  public static final AttributeDescriptor<String> MYGRID_REALM =
      new AttributeDescriptor<>("MyGridRealm", ValueType.TEXT).withOptional(false);
  public static final AttributeDescriptor<String> CLIENT_CERTIFICATE_ALIAS =
      MQTTAgent.CLIENT_CERTIFICATE_ALIAS;
  public static final AttributeDescriptor<Boolean> SECURE_MODE = MQTTAgent.SECURE_MODE;

  // hydration
  protected MyGridAgent() {}

  public MyGridAgent(String name) {
    super(name);
  }

  public Optional<String> getMqttHost() {
    return getAttributes().getValue(HOST);
  }

  public Optional<Integer> getPort() {
    return getAttributes().getValue(PORT);
  }

  public Optional<String> getCertificateAlias() {
    return getAttributes().getValue(CLIENT_CERTIFICATE_ALIAS);
  }

  public Optional<String> getClientId() {
    return getAttributes().getValue(CLIENT_ID);
  }

  public Optional<String> getMyGridRealm() {
    return getAttributes().getValue(MYGRID_REALM);
  }

  public Optional<Boolean> getSecureMode() {
    return getAttributes().getValue(SECURE_MODE);
  }

  public Optional<Boolean> isSecureMode() {
    return getAttributes().getValue(SECURE_MODE);
  }

  @Override
  public MyGridProtocol getProtocolInstance() {
    return new MyGridProtocol(this);
  }
}
