/*
 * Copyright 2021, OpenRemote Inc.
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
package org.openremote.manager.ourgrid.mqtt;

import io.netty.buffer.ByteBuf;
import org.apache.activemq.artemis.spi.core.protocol.RemotingConnection;
import org.keycloak.KeycloakSecurityContext;
import org.openremote.manager.mqtt.MQTTHandler;
import org.openremote.model.protocol.mqtt.Topic;
import org.openremote.model.syslog.SyslogCategory;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.logging.Logger;

import static org.openremote.model.syslog.SyslogCategory.API;

/**
 * This handler provides a workaround to allow Home Assistant to connect to the OpenRemote MQTT broker.
 */
public class HomeAssistantMQTTHandler extends MQTTHandler {

    public static final int PRIORITY = Integer.MIN_VALUE + 1;
    private static final Logger LOG = SyslogCategory.getLogger(API, HomeAssistantMQTTHandler.class);
    private static final String HOMEASSISTANT_TOPIC = "homeassistant";
    @Override
    public int getPriority() {
        return PRIORITY;
    }

    @Override
    public boolean topicMatches(Topic topic) {
        return topic.toString().startsWith("homeassistant/");
    }

    @Override
    protected Logger getLogger() {
        return LOG;
    }


    // Always return reject, don't need Home Assistant to subscribe to any topics
    @Override
    public boolean canSubscribe(RemotingConnection connection, KeycloakSecurityContext securityContext, Topic topic) {
        return false;
    }

    // Override checkCanPublish to directly call canPublish
    @Override
    public boolean checkCanPublish(RemotingConnection connection, KeycloakSecurityContext securityContext, Topic topic) {
        return canPublish(connection, securityContext, topic);
    }


    // Allow always for home assistant topic
    @Override
    public boolean canPublish(RemotingConnection connection, KeycloakSecurityContext securityContext, Topic topic) {
        return true;
    }


    // Never called since we reject any sub requests for the home assistant topic
    @Override
    public void onSubscribe(RemotingConnection connection, Topic topic) {
    }

    // Never called since we don't have subscriptions to manage
    @Override
    public void onUnsubscribe(RemotingConnection connection, Topic topic) {
    }


    @Override
    public Set<String> getPublishListenerTopics() {
        return Set.of(
            HOMEASSISTANT_TOPIC + "/" + TOKEN_MULTI_LEVEL_WILDCARD
        );
    }


    // Override handlesTopic to directly call topicMatches
    @Override
    public boolean handlesTopic(Topic topic) {
        return topicMatches(topic);
    }


    // Do nothing, just allow publishes for homeassistant/
    @Override
    public void onPublish(RemotingConnection connection, Topic topic, ByteBuf body) {
    }
  
}
