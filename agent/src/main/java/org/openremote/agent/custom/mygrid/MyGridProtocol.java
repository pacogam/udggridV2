package org.openremote.agent.custom.mygrid;

import java.util.*;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.openremote.agent.custom.ourgrid.OurgridBatteryAsset;
import org.openremote.agent.protocol.mqtt.MQTTAgent;
import org.openremote.agent.protocol.mqtt.MQTTAgentLink;
import org.openremote.agent.protocol.mqtt.MQTTMessage;
import org.openremote.agent.protocol.mqtt.MQTT_IOClient;
import org.openremote.model.Container;
import org.openremote.model.asset.Asset;
import org.openremote.model.asset.AssetEvent;
import org.openremote.model.asset.agent.ConnectionStatus;
import org.openremote.model.asset.agent.Protocol;
import org.openremote.model.attribute.Attribute;
import org.openremote.model.attribute.AttributeEvent;
import org.openremote.model.attribute.AttributeRef;
import org.openremote.model.attribute.MetaItem;
import org.openremote.model.auth.UsernamePassword;
import org.openremote.model.event.shared.SharedEvent;
import org.openremote.model.protocol.ProtocolAssetService;
import org.openremote.model.query.AssetQuery;
import org.openremote.model.syslog.SyslogCategory;
import org.openremote.model.util.ValueUtil;
import org.openremote.model.value.AttributeDescriptor;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.client.Entity;

import static org.openremote.model.value.MetaItemType.AGENT_LINK;
import static org.openremote.model.syslog.SyslogCategory.PROTOCOL;
import static org.openremote.container.web.WebTargetBuilder.createClient;

/**
 * Protocol implementation for integrating MyGrid batteries with OpenRemote/Ourgrid.
 * 
 * <p>This protocol provides the following functionality:</p>
 * 
 * <ul>
 *   <li>Establishes and manages connection to the MyGrid MQTT broker using configuration 
 *       from {@link MyGridAgent}</li>
 *   <li>Provisions {@link OurgridBatteryAsset} assets by polling the MyGrid OpenRemote HTTP API
 *       to retrieve assets of type {@link ModuleOneAsset} linked to the restricted MyGrid service user</li>
 *   <li>Automatically configures {@link MQTTAgentLink}s for each provisioned 
 *       {@link OurgridBatteryAsset}</li>
 * </ul>
 * 
 * <p><b>Note:</b> The current HTTP polling mechanism will be replaced with MQTT asset event subscriptions 
 * once <a href="https://github.com/openremote/openremote/issues/1902">issue #1902</a> is resolved.</p>
 * 
 * @see MyGridAgent
 * @see MyGridMQTTProtocol
 */
public class MyGridProtocol implements Protocol<MyGridAgent> {

    public static final String PROTOCOL_DISPLAY_NAME = "MyGrid";
    public static final String MYGRID_ASSET_TYPE = "ModuleOneAsset";

    private static final Logger LOG = SyslogCategory.getLogger(PROTOCOL, MyGridProtocol.class);

    protected MyGridAgent agent;
    protected MyGridMQTTProtocol mqttProtocol;
    protected MQTT_IOClient mqttClient;
    protected Container container;
    protected ScheduledExecutorService scheduledExecutor;
    protected ProtocolAssetService protocolAssetService;
    protected static final AtomicReference<ResteasyClient> resteasyClient = new AtomicReference<>();

    public MyGridProtocol(MyGridAgent agent) {
        this.agent = agent;

        MQTTAgent mqttAgent = new MQTTAgent(agent.getName());
        mqttAgent.setId(agent.getId());

        agent.getHost().ifPresent(mqttAgent::setHost);
        agent.getPort().ifPresent(mqttAgent::setPort);
        agent.getClientId().ifPresent(mqttAgent::setClientId);
        agent.isSecureMode().ifPresent(mqttAgent::setSecureMode);
        agent.getCertificateAlias().ifPresent(mqttAgent::setCertificateAlias);
        agent.getPublishQoS().ifPresent(mqttAgent::setPublishQos);
        agent.getSubscribeQoS().ifPresent(mqttAgent::setSubscribeQos);
        agent.getUsernamePassword().ifPresent(mqttAgent::setUsernamePassword);
        agent.getResumeSession().ifPresent(mqttAgent::setResumeSession);
        this.mqttProtocol = new MyGridMQTTProtocol(mqttAgent);
    }

    @Override
    public String getProtocolName() {
        return PROTOCOL_DISPLAY_NAME;
    }

    @Override
    public String getProtocolInstanceUri() {
        return "mygrid://" + getAgent().getHost().orElse("-") + ":"
                + getAgent().getPort().map(Object::toString).orElse("-");
    }

    @Override
    public Map<AttributeRef, Attribute<?>> getLinkedAttributes() {
        return mqttProtocol.getLinkedAttributes();
    }

    @Override
    public void linkAttribute(String assetId, Attribute<?> attribute) throws Exception {
        mqttProtocol.linkAttribute(assetId, attribute);
    }

    @Override
    public void unlinkAttribute(String assetId, Attribute<?> attribute) throws Exception {
        mqttProtocol.unlinkAttribute(assetId, attribute);
    }

    @Override
    public MyGridAgent getAgent() {
        return agent;
    }

    @Override
    public void updateLinkedAttribute(AttributeRef attributeRef, Object value, long timestamp) {
        mqttProtocol.updateLinkedAttribute(attributeRef, value, timestamp);
    }

    @Override
    public void updateLinkedAttribute(AttributeRef attributeRef, Object value) {
        mqttProtocol.updateLinkedAttribute(attributeRef, value);
    }

    @Override
    public void setAssetService(ProtocolAssetService assetService) {
        this.protocolAssetService = assetService;
        mqttProtocol.setAssetService(assetService);
    }

    @Override
    public void processLinkedAttributeWrite(AttributeEvent event) {
        mqttProtocol.processLinkedAttributeWrite(event);
    }

    @Override
    public boolean onAgentAttributeChanged(AttributeEvent event) {
        return mqttProtocol.onAgentAttributeChanged(event);
    }


    @Override
    public void start(Container container) throws Exception {
        mqttProtocol.start(container);
        this.container = container;
        this.scheduledExecutor = container.getScheduledExecutor();

        this.mqttClient = this.mqttProtocol.getMQTTClient();

        if (mqttClient != null) {
           mqttClient.addConnectionStatusConsumer(this::onConnectionStatusChanged);
        }

        initResteasyClient();

        // We are currently using a polling mechanism to sync assets because of a MQTT issue: https://github.com/openremote/openremote/issues/1902
        // TODO: Replace this with MQTT asset event subscriptions when the issue is resolved
        scheduledExecutor.scheduleAtFixedRate(this::syncMyGridAssets, 1, 30, TimeUnit.SECONDS);

        LOG.info("MyGrid protocol started");
    }

    @Override
    public void stop(Container container) throws Exception {
        mqttProtocol.stop(container);
    }

    protected static void initResteasyClient() {
        synchronized (resteasyClient) {
            if (resteasyClient.get() == null) {
                resteasyClient.set(createClient(org.openremote.container.Container.SCHEDULED_EXECUTOR));
            }
        }
    }

    // Get all OurgridBatteryAssets from the local OpenRemote instance
    protected List<OurgridBatteryAsset> getBatteryAssets() {
        return protocolAssetService.findAssets(new AssetQuery().types(OurgridBatteryAsset.class))
            .stream()
            .map(asset -> (OurgridBatteryAsset) asset)
            .collect(Collectors.toList());
    }



    protected void onConnectionStatusChanged(ConnectionStatus status) {
        LOG.info("MyGrid protocol connection status changed: " + status);

        // if (status == ConnectionStatus.CONNECTED) {
        //     tryAddMQTTMessageConsumer(getAssetEventsTopic(), this::onMyGridAssetEvent, 3, 500);
        //     tryAddMQTTMessageConsumer(getAttributeEventsTopic(), this::onMyGridAttributeEvent, 3, 500);
        // }
    }



    // Provision a new OurgridBatteryAsset with the respective MQTT Agent links
    protected Optional<OurgridBatteryAsset> provisionBatteryAsset(String id) {
        if (id == null) {
            LOG.warning("Cannot build asset due to missing ID value");
            return Optional.empty();
        }

        // Skip if asset is already provisioned
        if (getBatteryAssets().stream().anyMatch(asset -> asset.getId().equals(id))) {
            return Optional.empty();
        }

        // We set the name to the ID, MQTT attribute events do contain the asset name.
        OurgridBatteryAsset batteryAsset = new OurgridBatteryAsset(id);
        batteryAsset.setId(id);
        batteryAsset.setParentId(this.agent.getId());

        // Power attribute agent links (subscribe)
        var powerAttribute = batteryAsset.getAttribute(OurgridBatteryAsset.POWER);
        if (powerAttribute.isPresent()) {
            var powerAttributeSubscribeTopic = getAttributeSubscribeTopic(id, OurgridBatteryAsset.POWER.getName());
            powerAttribute.get().addOrReplaceMeta(
                new MetaItem<>(AGENT_LINK, new MQTTAgentLink(this.agent.getId()).setSubscriptionTopic(powerAttributeSubscribeTopic))
            );
        }

        // Energy level attribute agent links (subscribe)
        var energyLevelAttribute = batteryAsset.getAttribute(OurgridBatteryAsset.ENERGY_LEVEL);
        if (energyLevelAttribute.isPresent()) {
            var energyLevelAttributeSubscribeTopic = getAttributeSubscribeTopic(id, OurgridBatteryAsset.ENERGY_LEVEL.getName());
            energyLevelAttribute.get().addOrReplaceMeta(
                new MetaItem<>(AGENT_LINK, new MQTTAgentLink(this.agent.getId()).setSubscriptionTopic(energyLevelAttributeSubscribeTopic))
            );
        }

        // Power setpoint attribute agent links (publish and subscribe)
        var powerSetpointAttribute = batteryAsset.getAttribute(OurgridBatteryAsset.POWER_SETPOINT);
        if (powerSetpointAttribute.isPresent()) {
            var powerSetpointAttributeSubscribeTopic = getAttributeSubscribeTopic(id, OurgridBatteryAsset.POWER_SETPOINT.getName());
            var powerSetpointAttributePublishTopic = getAttributePublishTopic(id, OurgridBatteryAsset.POWER_SETPOINT.getName());
            powerSetpointAttribute.get().addOrReplaceMeta(
                new MetaItem<>(AGENT_LINK, new MQTTAgentLink(this.agent.getId())
                .setPublishTopic(powerSetpointAttributePublishTopic)
                .setSubscriptionTopic(powerSetpointAttributeSubscribeTopic))
            );
        }

        // Merge the newly created battery asset
        try {
            protocolAssetService.mergeAsset(batteryAsset);
        } catch (Exception e) {
            LOG.warning("Failed to merge asset: " + batteryAsset.getId());
            return Optional.empty();
        }

        return Optional.of(batteryAsset);
    }

    // Construct the initial topic prefix based on the agent's config (example: mygrid/serviceuser)
    protected String getTopicPrefix() {
        String realm = this.getAgent().getMyGridRealm().orElseThrow(() -> 
            new IllegalArgumentException("MyGrid realm was not configured"));
        String clientId = this.getAgent().getClientId().orElseThrow(() -> 
            new IllegalArgumentException("Client ID was not configured"));
        return realm + "/" + clientId;
    }

    // Return the wildcard topic for asset events (example: mygrid/serviceuser/asset/#)
    protected String getAssetEventsTopic() {
        return getTopicPrefix() + "/asset/#";
    }

    // Return the wildcard topic for attribute events (example: mygrid/serviceuser/attribute/+/#)
    protected String getAttributeEventsTopic() {
        return getTopicPrefix() + "/attribute/+/#";
    }

    // Return the topic for subscribing to a specific attribute value (example: mygrid/serviceuser/attributevalue/power/assetId)
    protected String getAttributeSubscribeTopic(String assetId, String attributeName) {
        return getTopicPrefix() + "/attributevalue/" + attributeName + "/" + assetId;
    }

    // Return the topic for publishing a specific attribute value (example: mygrid/serviceuser/writeattributevalue/power/assetId)
    protected String getAttributePublishTopic(String assetId, String attributeName) {
        return getTopicPrefix() + "/writeattributevalue/" + attributeName + "/" + assetId;
    }


    // Simple cache for the auth token
    record OAuthToken(String token, long expiresAtMillis) {}
    OAuthToken cachedToken;

    // Sync the relevant assets (ModuleOneAsset) from MyGrid to the local OpenRemote instance
    protected void syncMyGridAssets() {
        String myGridRealm = this.agent.getMyGridRealm().orElseThrow(() -> new IllegalArgumentException("Agent mygrid realm was not configured"));
        String url = "https://" + this.agent.getHost().orElseThrow(() -> new IllegalArgumentException("Agent host was not configured"));

        // Refresh the token if it is about to expire
        if (cachedToken == null || cachedToken.expiresAtMillis() < System.currentTimeMillis() + 10000) {
            Map<String, String> oAuthResponse = getMyGridOAuthToken(url, myGridRealm);

            if (oAuthResponse.isEmpty()) {
                LOG.severe("Failed to get auth token response");
                return;
            }

            String accessToken = oAuthResponse.get("access_token");
            long expiresAtMillis = System.currentTimeMillis() + Long.parseLong(oAuthResponse.get("expires_in")) * 1000;
            cachedToken = new OAuthToken(accessToken, expiresAtMillis);
        }

        List<Asset<?>> mygridAssets = getMyGridAssets(url, myGridRealm, cachedToken.token());

        mygridAssets.forEach(asset -> {
           provisionBatteryAsset(asset.getId());
        });
    }


    // Query the MyGrid OpenRemote API for the assets with the ModuleOneAsset type
    protected List<Asset<?>> getMyGridAssets(String url, String realm, String accessToken) {
        String assetQueryUrl = url + "/api/" + realm + "/asset/query";

        ResteasyClient client = resteasyClient.get();
        Map<String, Object> assetQuery = new HashMap<>();

        // Only query for ModuleOneAsset types
        assetQuery.put("types", Arrays.asList("ModuleOneAsset"));

        try (Response response = client.target(assetQueryUrl)
                .request()
                .header("Authorization", "Bearer " + accessToken)
                .build("POST", Entity.json(assetQuery))
                .invoke()) {
            
            if (response.getStatus() == Response.Status.OK.getStatusCode()) {
                return response.readEntity(new jakarta.ws.rs.core.GenericType<List<Asset<?>>>() {});
            }
            return Collections.emptyList();
        } catch (Exception e) {
            LOG.severe("Failed to query assets: " + e.getMessage());
            return Collections.emptyList();
        }
    }


    // Get the OAuth token from the MyGrid Keycloak instance
    protected Map<String, String> getMyGridOAuthToken(String url, String realm) {
        UsernamePassword usernamePassword = this.agent.getUsernamePassword().orElseThrow(() -> new IllegalArgumentException("Client secret was not configured"));
        String serviceUser = this.agent.getClientId().orElseThrow(() -> new IllegalArgumentException("Client ID was not configured"));
        String serviceUserSecret = usernamePassword.getPassword();

        Map<String, String> data = new HashMap<>();
        data.put("grant_type", "client_credentials");
        data.put("client_id", serviceUser);
        data.put("client_secret", serviceUserSecret);

        ResteasyClient client = resteasyClient.get();
        jakarta.ws.rs.core.MultivaluedHashMap<String, String> formData = new jakarta.ws.rs.core.MultivaluedHashMap<>();
        data.forEach((key, value) -> formData.add(key, value));
        
        try (Response response = client.target(url + "/auth/realms/" + realm + "/protocol/openid-connect/token")
                .request()
                .header("Content-Type", "application/x-www-form-urlencoded")
                .build("POST", Entity.form(formData))
                .invoke()) {
            
            if (response.getStatus() == Response.Status.OK.getStatusCode()) {
                return response.readEntity(new jakarta.ws.rs.core.GenericType<Map<String, String>>() {});
            }
            return Collections.emptyMap();
        } catch (Exception e) {
            LOG.severe("Failed to get auth token: " + e.getMessage());
            return Collections.emptyMap();
        }
    }

        // Helper method for subscribing to a mqtt topic with a delay between retries, providing a retry mechanism
        protected void tryAddMQTTMessageConsumer(String topic, Consumer<MQTTMessage<String>> consumer, int maxRetries, long delayMs) {
            if (maxRetries <= 0) {
                return;
            }
    
            scheduledExecutor.schedule(() -> {
                try {
                    boolean subscribed = mqttClient.addMessageConsumer(topic, consumer);
                    if (!subscribed) {
                        LOG.warning("Failed to subscribe to MyGrid asset events");
                        tryAddMQTTMessageConsumer(topic, consumer, maxRetries - 1, delayMs);
                    }
                } catch (Exception e) {
                    tryAddMQTTMessageConsumer(topic, consumer, maxRetries - 1, delayMs);
                }
            }, delayMs, TimeUnit.MILLISECONDS);
        }


    // protected void onMyGridAssetEvent(MQTTMessage<String> msg) {
    //     SharedEvent event = ValueUtil.parse(msg.getPayload(), SharedEvent.class).orElse(null);

    //     if (event instanceof AssetEvent assetEvent) {
    //         if (!assetEvent.getAssetType().equals(MYGRID_ASSET_TYPE)) {
    //             return; // Don't process unrelated asset types.
    //         }

    //         // Handle the asset event based on the cause
    //         switch (assetEvent.getCause()) {
    //             case UPDATE, CREATE :
    //                 Optional<OurgridBatteryAsset> batteryAsset = provisionBatteryAsset(assetEvent.getAsset().getId());
    //                 batteryAsset.ifPresent(ourgridBatteryAsset -> LOG.fine("Battery asset created: " + ourgridBatteryAsset.getId()));
    //                 break;
    //             case DELETE:
    //                 protocolAssetService.deleteAssets(assetEvent.getAsset().getId());
    //                 break;
    //             default:
    //                 break;
    //         }
    //     }
    // }

    // protected void onMyGridAttributeEvent(MQTTMessage<String> msg) {
    //     SharedEvent event = ValueUtil.parse(msg.getPayload(), SharedEvent.class).orElse(null);

    //     if (event instanceof AttributeEvent attributeEvent) {
    //         // AttributeEvent does not contain the asset type, so we need to do some extra work
    //         // Check whether the event attributeName is a valid battery asset attribute via reflection
    //         // We also assume the service user is only linked to valid assets, so this is a preventative check to prevent accidental misuse
    //         String[] validFields = Arrays.stream(OurgridBatteryAsset.class.getFields())
    //                 .filter(field -> AttributeDescriptor.class.isAssignableFrom(field.getType()))
    //                 .map(field -> {
    //                     try {
    //                         field.setAccessible(true);
    //                         AttributeDescriptor<?> descriptor = (AttributeDescriptor<?>) field.get(null);
    //                         return descriptor.getName();
    //                     } catch (IllegalAccessException e) {
    //                         return null;
    //                     }
    //                 })
    //                 .filter(Objects::nonNull)
    //                 .toArray(String[]::new);

    //         // Check if the attribute is considered valid e.g. present in the asset class we will provision
    //         if (Arrays.stream(validFields).noneMatch(field -> field.equals(attributeEvent.getName()))) {
    //             return; // Don't process unrelated attributes.
    //         }

    //         // Try and provision the battery asset
    //         Optional<OurgridBatteryAsset> batteryAsset = provisionBatteryAsset(attributeEvent.getRef().getId());
    //         batteryAsset.ifPresent(ourgridBatteryAsset -> LOG.fine("Battery asset created: " + ourgridBatteryAsset.getId()));
    //     }
    // }

}
