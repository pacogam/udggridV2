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
