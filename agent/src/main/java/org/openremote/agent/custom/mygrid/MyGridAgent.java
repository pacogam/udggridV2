package org.openremote.agent.custom.mygrid;

import org.openremote.model.asset.agent.Agent;
import org.openremote.model.asset.agent.AgentDescriptor;

import java.util.Optional;

import org.openremote.agent.protocol.mqtt.MQTTAgent;
import org.openremote.agent.protocol.mqtt.MQTTAgentLink;
import jakarta.persistence.Entity;
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

    public static final AgentDescriptor<MyGridAgent, MyGridProtocol, MQTTAgentLink> DESCRIPTOR = new AgentDescriptor<>(MyGridAgent.class, MyGridProtocol.class, MQTTAgentLink.class);

    public static final AttributeDescriptor<String> HOST = MQTTAgent.HOST;
    public static final AttributeDescriptor<Integer> PORT = MQTTAgent.PORT;
    public static final AttributeDescriptor<String> CLIENT_ID = MQTTAgent.CLIENT_ID;
    public static final AttributeDescriptor<String> MYGRID_REALM = new AttributeDescriptor<>("MyGridRealm", ValueType.TEXT).withOptional(false);
    public static final AttributeDescriptor<String> CLIENT_CERTIFICATE_ALIAS = MQTTAgent.CLIENT_CERTIFICATE_ALIAS;
    public static final AttributeDescriptor<Boolean> SECURE_MODE = MQTTAgent.SECURE_MODE;
    public static final AttributeDescriptor<Integer> PUBLISH_QOS = MQTTAgent.PUBLISH_QOS;
    public static final AttributeDescriptor<Integer> SUBSCRIBE_QOS = MQTTAgent.SUBSCRIBE_QOS;
    public static final AttributeDescriptor<Boolean> RESUME_SESSION = MQTTAgent.RESUME_SESSION;





    // hydration
    protected MyGridAgent() {
    }

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

    public Optional<Boolean> getResumeSession() {
        return getAttributes().getValue(RESUME_SESSION);
    }


    public Optional<Boolean> getSecureMode() {
        return getAttributes().getValue(SECURE_MODE);
    }

    public Optional<Integer> getPublishQoS() {
        return getAttributes().getValue(PUBLISH_QOS);
    }

    public Optional<Integer> getSubscribeQoS() {
        return getAttributes().getValue(SUBSCRIBE_QOS);
    }


    public Optional<Boolean> isSecureMode() {
        return getAttributes().getValue(SECURE_MODE);
    }
    
    

    @Override
    public MyGridProtocol getProtocolInstance() {
        return new MyGridProtocol(this);
    }
    
}
