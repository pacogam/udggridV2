package org.openremote.agent.custom.mygrid;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.logging.Logger;

import org.jboss.resteasy.client.jaxrs.ResteasyClient;
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

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.client.Entity;

import static org.openremote.model.value.MetaItemType.AGENT_LINK;
import static org.openremote.model.syslog.SyslogCategory.PROTOCOL;
import static org.openremote.container.web.WebTargetBuilder.createClient;

/**
 * Protocol implementation for integrating MyGrid batteries with
 * OpenRemote/Ourgrid.
 * 
 * <p>
 * This protocol provides the following functionality:
 * </p>
 * 
 * <ul>
 * <li>Establishes and manages connection to the MyGrid MQTT broker using
 * configuration
 * from {@link MyGridAgent}</li>
 * 
 * <li>Provisions {@link OurgridBatteryAsset} assets by initially querying the
 * MyGrid OpenRemote HTTP API during startup and every minute thereafter
 * to retrieve assets of type ModuleOneAsset linked to the restricted MyGrid
 * service user</li>
 * 
 * <li>Subscribes to asset events via MQTT to provision the assets if they don't
 * exist yet locally</li>
 * 
 * <li>Subscribes to attribute events via MQTT to forward the attribute events
 * to the internal message broker</li>
 * 
 * <li>Automatically configures {@link MQTTAgentLink}s for each provisioned
 * {@link OurgridBatteryAsset} for publishing specific attributes</li>
 * </ul>
 * 
 * @see MyGridAgent
 * @see MyGridMQTTProtocol
 */
public class MyGridProtocol implements Protocol<MyGridAgent> {

    public static final String PROTOCOL_DISPLAY_NAME = "MyGrid";
    public static final String MYGRID_ASSET_TYPE = "ModuleOneAsset";

    private static final Logger LOG = SyslogCategory.getLogger(PROTOCOL, MyGridProtocol.class);

    // Attribute names to process locally when received from the MyGrid MQTT broker
    private static final String[] SUBSCRIBED_ATTRIBUTES = {
            "power",
            "energyLevel",
            "powerSetpoint",
            "energyLevelPercentage",
    };

    private static final Map<String, String> OURGRID_TO_MYGRID_MAPPING = new HashMap<>();

    static {
        OURGRID_TO_MYGRID_MAPPING.put("allowAutomaticControlButton", "externalControl");
    }

    protected MyGridAgent agent;
    protected MyGridMQTTProtocol mqttProtocol;
    protected MQTT_IOClient mqttClient;
    protected Container container;
    protected ScheduledExecutorService scheduledExecutor;
    protected ProtocolAssetService protocolAssetService;
    protected static final AtomicReference<ResteasyClient> resteasyClient = new AtomicReference<>();

    // Timestamp of the last executed sync
    protected long lastSyncTimestamp = 0;

    // Interval used to prevent constant syncs when the client gets disconnected and
    // re-connected due to connection issues
    protected static final long MIN_INTERVAL_BETWEEN_SYNCS = 5000; // 5 seconds

    // Subscribed topics
    protected final Set<String> subscribedTopics = ConcurrentHashMap.newKeySet();

    public MyGridProtocol(MyGridAgent agent) {
        this.agent = agent;

        // Create the MQTT agent instance
        MQTTAgent mqttAgent = new MQTTAgent(agent.getName());
        mqttAgent.setId(agent.getId());

        agent.getHost().ifPresent(mqttAgent::setHost);
        agent.getPort().ifPresent(mqttAgent::setPort);
        agent.getClientId().ifPresent(mqttAgent::setClientId);
        agent.isSecureMode().ifPresent(mqttAgent::setSecureMode);
        agent.getCertificateAlias().ifPresent(mqttAgent::setCertificateAlias);
        agent.getUsernamePassword().ifPresent(mqttAgent::setUsernamePassword);

        // Create the MQTT protocol instance
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
        LOG.info("MyGrid protocol processing linked attribute write: " + event.getName() + " for asset "
                + event.getId());
        mqttProtocol.processLinkedAttributeWrite(event);
    }

    @Override
    public boolean onAgentAttributeChanged(AttributeEvent event) {
        return mqttProtocol.onAgentAttributeChanged(event);
    }

    protected void onConnectionStatusChanged(ConnectionStatus status) {
        LOG.info("MyGrid protocol connection status changed: " + status);

        // (Re)Subscribe to asset and attribute events when the MQTT connection is
        // established
        if (status == ConnectionStatus.CONNECTED) {
            LOG.info("MyGrid protocol connection established, subscribing to asset and attribute events");

            // Subscribe to asset and attribute events with an active retry mechanism
            tryAddMQTTMessageConsumer(getAssetEventsTopic(), this::onMyGridAssetEvent, 10, 5000);
            tryAddMQTTMessageConsumer(getAttributeEventsTopic(), this::onMyGridAttributeEvent, 10, 5000);

            // Syncing the assets from MyGrid (ModuleOneAsset) if the last sync was more
            // than MIN_INTERVAL_BETWEEN_SYNCS ago
            // This is to prevent constant syncs when the client gets disconnected and
            // re-connected.
            // Note: The client gets disconnected when UserAssetLinks change, thats why we
            // resync on reconnect.
            if (System.currentTimeMillis() - lastSyncTimestamp > MIN_INTERVAL_BETWEEN_SYNCS) {
                syncMyGridAssets();
                lastSyncTimestamp = System.currentTimeMillis();
            }

        } else {
            LOG.info("MyGrid protocol connection lost, unsubscribing from asset and attribute events");
            if (mqttClient != null) {
                mqttClient.removeAllMessageConsumers();
                subscribedTopics.clear();
            }
        }
    }

    @Override
    public void start(Container container) throws Exception {
        LOG.info("MyGrid protocol starting");
        mqttProtocol.start(container);

        this.container = container;
        this.scheduledExecutor = container.getScheduledExecutor();

        this.mqttClient = this.mqttProtocol.getMQTTClient();

        if (mqttClient != null) {
            mqttClient.addConnectionStatusConsumer(this::onConnectionStatusChanged);
        }

        // Initialize the RESTEasy client for HTTP requests
        initResteasyClient();

        LOG.info("MyGrid protocol started");
    }

    @Override
    public void stop(Container container) throws Exception {
        LOG.info("MyGrid protocol stopping");

        // Cleanup the MQTT client subscriptions
        if (mqttClient != null) {
            mqttClient.removeAllMessageConsumers();
            subscribedTopics.clear();
        }

        // Remove status consumer from the MQTT client
        if (mqttClient != null) {
            LOG.info("Removing connection status consumers from the MQTT client");
            mqttClient.removeAllConnectionStatusConsumers();
        }

        mqttProtocol.stop(container);

        LOG.info("MyGrid protocol stopped");
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
                .map(OurgridBatteryAsset.class::cast)
                .toList();
    }

    // Provision a new OurgridBatteryAsset with the respective MQTT Agent links
    protected void provisionBatteryAsset(Asset<?> asset) {
        if (asset.getId() == null) {
            LOG.warning("Cannot build asset due to missing ID value");
            return;
        }

        // Update the existing asset with new agent links if it exists
        var existingAsset = getBatteryAssets().stream().filter(a -> a.getId().equals(asset.getId())).findFirst()
                .orElse(null);
        if (existingAsset != null) {
            LOG.info("Updating agent links for existing battery asset: " + existingAsset.getId());

            // Add or replace the agent links
            addOrReplaceAgentLinks(existingAsset);

            try {
                protocolAssetService.mergeAsset(existingAsset);
                LOG.info("Successfully updated agent links for existing battery asset: " + existingAsset.getId());
            } catch (Exception e) {
                LOG.warning("Failed to merge battery asset with new agent links: " + existingAsset.getId());
            }
            return;
        }

        OurgridBatteryAsset batteryAsset = new OurgridBatteryAsset(asset.getId());
        batteryAsset.setId(asset.getId());
        batteryAsset.setParentId(this.agent.getId());

        // Update any existing attributes with the new values from the MyGrid asset
        Arrays.stream(SUBSCRIBED_ATTRIBUTES)
                .filter(attributeName -> batteryAsset.hasAttribute(attributeName) && asset.hasAttribute(attributeName))
                .forEach(attributeName -> asset.getAttribute(attributeName).flatMap(Attribute::getValue)
                        .ifPresent(value -> batteryAsset.getAttribute(attributeName)
                                .ifPresent(attribute -> attribute.setValue(value))));

        // Add or replace the agent links
        addOrReplaceAgentLinks(batteryAsset);

        // Merge the OurgridBatteryAsset into the local OpenRemote instance
        try {
            LOG.info("Provisioning new battery asset: " + batteryAsset.getId());
            protocolAssetService.mergeAsset(batteryAsset);
            LOG.info("Successfully provisioned new battery asset: " + batteryAsset.getId());
        } catch (Exception e) {
            LOG.warning("Failed to merge battery asset: " + batteryAsset.getId());
        }
    }

    // Add or replace the agent links for the given asset
    protected void addOrReplaceAgentLinks(OurgridBatteryAsset batteryAsset) {

        // power setpoint
        var powerSetpointAttribute = batteryAsset.getAttribute(OurgridBatteryAsset.POWER_SETPOINT);
        if (powerSetpointAttribute.isPresent()) {
            var powerSetpointAttributePublishTopic = getAttributePublishTopic(batteryAsset.getId(),
                    OurgridBatteryAsset.POWER_SETPOINT.getName());
            powerSetpointAttribute.get().addOrReplaceMeta(
                    new MetaItem<>(AGENT_LINK, new MQTTAgentLink(this.agent.getId())
                            .setPublishTopic(powerSetpointAttributePublishTopic)));
        }

        // allowAutomaticControlButton
        var allowAutomaticControlButtonAttribute = batteryAsset
                .getAttribute(OurgridBatteryAsset.ALLOW_AUTOMATIC_CONTROL_BUTTON);
        if (allowAutomaticControlButtonAttribute.isPresent()) {
            // Get the equivalent MyGrid attribute name for the publish topic
            var mygridAttributeName = OURGRID_TO_MYGRID_MAPPING
                    .get(OurgridBatteryAsset.ALLOW_AUTOMATIC_CONTROL_BUTTON.getName());
            var allowAutomaticControlButtonAttributePublishTopic = getAttributePublishTopic(batteryAsset.getId(),
                    mygridAttributeName);
            allowAutomaticControlButtonAttribute.get().addOrReplaceMeta(
                    new MetaItem<>(AGENT_LINK, new MQTTAgentLink(this.agent.getId())
                            .setPublishTopic(allowAutomaticControlButtonAttributePublishTopic).setUpdateOnWrite(true)));

        }

    }

    // Construct the initial topic prefix based on the agent's config (example:
    // mygrid/serviceuser)
    protected String getTopicPrefix() {
        String realm = this.getAgent().getMyGridRealm()
                .orElseThrow(() -> new IllegalArgumentException("MyGrid realm was not configured"));
        String clientId = this.getAgent().getClientId()
                .orElseThrow(() -> new IllegalArgumentException("Client ID was not configured"));
        return realm + "/" + clientId;
    }

    // Return the wildcard topic for asset events (example:
    // mygrid/serviceuser/asset/#)
    protected String getAssetEventsTopic() {
        return getTopicPrefix() + "/asset/#";
    }

    // Return the wildcard topic for attribute events (example:
    // mygrid/serviceuser/attribute/+/#)
    protected String getAttributeEventsTopic() {
        return getTopicPrefix() + "/attribute/+/#";
    }

    // Return the topic for publishing a specific attribute value
    protected String getAttributePublishTopic(String assetId, String attributeName) {
        return getTopicPrefix() + "/writeattributevalue/" + attributeName + "/" + assetId;
    }

    // Sync the relevant assets (ModuleOneAsset) from MyGrid to the local OpenRemote
    // instance
    protected void syncMyGridAssets() {
        LOG.info("Syncing battery assets (ModuleOneAsset) from MyGrid");

        String myGridRealm = this.agent.getMyGridRealm()
                .orElseThrow(() -> new IllegalArgumentException("Agent mygrid realm was not configured"));
        String url = "https://"
                + this.agent.getHost().orElseThrow(() -> new IllegalArgumentException("Agent host was not configured"));

        // Get the token before querying the assets
        Map<String, String> oAuthResponse = getMyGridOAuthToken(url, myGridRealm);

        if (oAuthResponse.isEmpty()) {
            LOG.severe("Failed to get auth token response while syncing battery assets (ModuleOneAsset) from MyGrid");
            return;
        }

        String accessToken = oAuthResponse.get("access_token");

        if (accessToken == null) {
            LOG.severe(
                    "Failed to get access token from OAuth response while syncing battery assets (ModuleOneAsset) from MyGrid");
            return;
        }

        // Query the assets
        List<Asset<?>> mygridAssets = getMyGridAssets(url, myGridRealm, accessToken);

        // Provision each asset
        mygridAssets.forEach(this::provisionBatteryAsset);
    }

    // Query the MyGrid OpenRemote API for the assets with the ModuleOneAsset type
    protected List<Asset<?>> getMyGridAssets(String url, String realm, String accessToken) {
        String assetQueryUrl = url + "/api/" + realm + "/asset/query";

        ResteasyClient client = resteasyClient.get();
        Map<String, Object> assetQuery = new HashMap<>();

        // Only query for ModuleOneAsset types
        assetQuery.put("types", List.of(MYGRID_ASSET_TYPE));

        try (Response response = client.target(assetQueryUrl)
                .request()
                .header("Authorization", "Bearer " + accessToken)
                .build("POST", Entity.json(assetQuery))
                .invoke()) {

            if (response.getStatus() == Response.Status.OK.getStatusCode()) {
                return response.readEntity(new jakarta.ws.rs.core.GenericType<>() {
                });
            }
            return Collections.emptyList();
        } catch (Exception e) {
            LOG.severe("Failed to query assets: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    // Get the OAuth token from the MyGrid Keycloak instance
    protected Map<String, String> getMyGridOAuthToken(String url, String realm) {
        UsernamePassword usernamePassword = this.agent.getUsernamePassword()
                .orElseThrow(() -> new IllegalArgumentException("Client secret was not configured"));
        String serviceUser = this.agent.getClientId()
                .orElseThrow(() -> new IllegalArgumentException("Client ID was not configured"));
        String serviceUserSecret = usernamePassword.getPassword();

        Map<String, String> data = new HashMap<>();
        data.put("grant_type", "client_credentials");
        data.put("client_id", serviceUser);
        data.put("client_secret", serviceUserSecret);

        ResteasyClient client = resteasyClient.get();
        jakarta.ws.rs.core.MultivaluedHashMap<String, String> formData = new jakarta.ws.rs.core.MultivaluedHashMap<>();
        data.forEach(formData::add);

        try (Response response = client.target(url + "/auth/realms/" + realm + "/protocol/openid-connect/token")
                .request()
                .header("Content-Type", "application/x-www-form-urlencoded")
                .build("POST", Entity.form(formData))
                .invoke()) {

            if (response.getStatus() == Response.Status.OK.getStatusCode()) {
                return response.readEntity(new jakarta.ws.rs.core.GenericType<Map<String, String>>() {
                });
            }
            return Collections.emptyMap();
        } catch (Exception e) {
            LOG.severe("Failed to get auth token: " + e.getMessage());
            return Collections.emptyMap();
        }
    }

    /*
     * Retry mechanism for subscribing to a MQTT topic with a delay between retries
     * and a max number of retries.
     * Prevents double subscriptions to the same topic.
     */
    protected void tryAddMQTTMessageConsumer(String topic, Consumer<MQTTMessage<String>> consumer, int maxRetries,
            long delayMs) {

        if (maxRetries <= 0) {
            LOG.warning("Max retries reached for subscribing to " + topic);
            return;
        }

        scheduledExecutor.schedule(() -> {
            try {
                if (subscribedTopics.contains(topic)) {
                    LOG.info("Already subscribed to " + topic);
                    return;
                }

                mqttClient.addMessageConsumer(topic, consumer);
                subscribedTopics.add(topic);
                LOG.info("Successfully subscribed to " + topic);
                LOG.info("Subscribed topics: " + subscribedTopics);
            } catch (Exception e) {
                LOG.warning("Error subscribing to " + topic + ": " + e.getMessage());
                tryAddMQTTMessageConsumer(topic, consumer, maxRetries - 1, delayMs);
            }
        }, delayMs, TimeUnit.MILLISECONDS);
    }

    /*
     * Asset Event Consumer
     * Process the asset events from the MyGrid MQTT broker
     * DELETE: delete the battery asset
     */
    protected void onMyGridAssetEvent(MQTTMessage<String> msg) {
        SharedEvent event = ValueUtil.parse(msg.getPayload(), SharedEvent.class).orElse(null);

        if (event instanceof AssetEvent assetEvent) {
            if (!assetEvent.getAssetType().equals(MYGRID_ASSET_TYPE)) {
                return; // Don't process unrelated asset types.
            }

            // Handle battery asset event for removal
            switch (assetEvent.getCause()) {
                case DELETE:
                    protocolAssetService.deleteAssets(assetEvent.getAsset().getId());
                    break;
                default:
                    break;
            }
        }
    }

    /*
     * Attribute Event Consumer
     * Process the attribute events from the MyGrid MQTT broker
     * Forward the attribute event to the internal message broker for processing
     * after mapping the attribute name to the equivalent Ourgrid attribute name
     */
    protected void onMyGridAttributeEvent(MQTTMessage<String> msg) {
        SharedEvent event = ValueUtil.parse(msg.getPayload(), SharedEvent.class).orElse(null);

        // Forward the attribute event to the internal message broker
        if (event instanceof AttributeEvent attributeEvent
                && Arrays.asList(SUBSCRIBED_ATTRIBUTES).contains(attributeEvent.getName())) {

            LOG.info("Processing external attribute event: " + attributeEvent.getName() + " for asset "
                    + attributeEvent.getId() + " with new value " + attributeEvent.getValue());

            // Ensure the attribute event is updated to the agent's realm
            attributeEvent.setRealm(this.agent.getRealm());
            protocolAssetService.sendAttributeEvent(attributeEvent);

        }
    }

}
