package org.openremote.agent.custom.exit;


import org.openremote.agent.protocol.AbstractProtocol;
//import org.openremote.container.util.UniqueIdentifierGenerator;
import org.openremote.model.util.UniqueIdentifierGenerator;
import org.openremote.model.Container;
import org.openremote.model.asset.Asset;
import org.openremote.model.asset.agent.ConnectionStatus;
import org.openremote.model.asset.agent.DefaultAgentLink;
import org.openremote.model.attribute.*;
import org.openremote.model.geo.GeoJSONPoint;
import org.openremote.model.syslog.SyslogCategory;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.time.Instant;
import java.util.*;
import java.util.logging.Logger;

import static org.openremote.model.syslog.SyslogCategory.PROTOCOL;

public class MqttExitProtocol extends AbstractProtocol<MqttExitAgent, DefaultAgentLink> {
  private String hostPrevious = "";
  private String usernamePrevious = "";
  private String passwordPrevious = "";

  private boolean disableButtonPrevious = false;
  private boolean reconnect = true;
  private boolean connectionStatus = false;

  private final HashMap<String, Long> activeShellyDevices = new HashMap<>();
  private final MemoryPersistence persistence = new MemoryPersistence();

  public static final String PROTOCOL_DISPLAY_NAME = "Exit";
  private static final Logger LOG = SyslogCategory.getLogger(PROTOCOL, MqttExitProtocol.class);
  private int firstSlash;

  public MqttExitProtocol(MqttExitAgent agent) {
    super(agent);
  }

  @Override
  public String getProtocolName() {
    return PROTOCOL_DISPLAY_NAME;
  }

  @Override
  public String getProtocolInstanceUri() {
    return "exit://" + agent.getId();
  }

  @Override
  protected void doStart(Container container) throws Exception {
    setConnectionStatus(ConnectionStatus.CONNECTING);

//        final Runnable beeper = () -> {
    connectToMqtt();
//        };

//        scheduler.scheduleAtFixedRate(beeper, 3, 5, TimeUnit.SECONDS);
  }

  @Override
  protected void doStop(Container container) throws Exception {
    disconnectFromMqtt();
  }

  @Override
  protected void doLinkAttribute(String assetId, Attribute<?> attribute, DefaultAgentLink agentLink) throws RuntimeException {
  }

  @Override
  protected void doUnlinkAttribute(String assetId, Attribute<?> attribute, DefaultAgentLink agentLink) {
  }

  @Override
  protected void doLinkedAttributeWrite(DefaultAgentLink agentLink, AttributeEvent event, Object processedValue) {
  }

  private void disconnectFromMqtt() {
    try {
//            connection.close();
      LOG.info("Agent='" + agent.getName() + "'; MQTT connection closed");
    } catch (Exception e) {
      LOG.info("Agent='" + agent.getName() + "'; MQTT connection to close");
    }
  }

  private void connectToMqtt() {
    // Get credentials from agent
    String host = agent.getMqttHost().orElse("");
    String port = agent.getMqttPort().orElse("");
    String username = agent.getMqttUsername().orElse("");
    String password = agent.getMqttPassword().orElse("");
    String topic = agent.getMqttTopic().orElse("");
    if (topic.isEmpty() | port.isEmpty()) {
      setConnectionStatus(ConnectionStatus.DISCONNECTED);
      return;}
    String broker = "tcp://" + host + ":" + port;
    firstSlash = topic.indexOf('+');

    // Disable agent
    boolean disableButton = agent.isDisabled().orElse(false); // Button pressed = true -> agent is disabled

    if (disableButton && connectionStatus) {
      connectionStatus = false;
      setConnectionStatus(ConnectionStatus.DISABLED);
      LOG.info("Agent='" + agent.getName() + "'; Agent disabled");
      disconnectFromMqtt();
    } else if (disableButton && agent.getAgentStatus().isPresent() && agent.getAgentStatus().get() == ConnectionStatus.ERROR) {
      setConnectionStatus(ConnectionStatus.DISABLED);
      LOG.info("Agent='" + agent.getName() + "'; Agent disabled");
      LOG.info("Agent='" + agent.getName() + "'; No MQTT connection to close");
    } else if (!disableButton && disableButtonPrevious) {
      reconnect = true;
      LOG.info("Agent='" + agent.getName() + "'; Agent enabled");
    }

    // Check if credentials have changed
    if (!host.equals(hostPrevious) || !username.equals(usernamePrevious) || !password.equals(passwordPrevious) ) {
      reconnect = true;
    }

    // Disconnect from host when there are new credentials
    if (connectionStatus && reconnect) {
      connectionStatus = false;
      disconnectFromMqtt();
    }

    // Connect to host
    if (!connectionStatus && !disableButton) {
      connectionStatus = true;

      try {

        MqttClient client = new MqttClient(broker, username, persistence);
        MqttConnectOptions connOpts = new MqttConnectOptions();
        connOpts.setCleanSession(true);
        connOpts.setUserName(username);
        connOpts.setPassword(password.toCharArray());

        System.out.println("Connectant-se al broker: " + broker);
        client.connect(connOpts);
        System.out.println("Connectat");
        setConnectionStatus(ConnectionStatus.CONNECTED);

        client.subscribe(topic);
        client.setCallback(new MqttCallback() {
          @Override
          public void connectionLost(Throwable cause) {
            System.out.println("Connexió perduda amb el broker MQTT");
            setConnectionStatus(ConnectionStatus.DISCONNECTED);
            connectionStatus = false;
          }

          @Override
          public void messageArrived(String topic, MqttMessage message) throws Exception {
            String payload = new String(message.getPayload());
            long timestamp = Instant.now().getEpochSecond();
//                        System.out.println("TS:" + timestamp + " - Topic: " + topic + ": " + payload);
            try {
              onMessageReceived(topic.substring(firstSlash), payload, timestamp);
            } catch (Exception e) {
              LOG.info("Agent='" + agent.getName() + "'; Message not processed: '" + message + "'; Exception: " + e);
            }
          }

          @Override
          public void deliveryComplete(IMqttDeliveryToken token) {
          }
        });
      } catch (MqttException e) {
        e.printStackTrace();
        connectionStatus = false;
        if (reconnect) {
          setConnectionStatus(ConnectionStatus.ERROR);
          LOG.warning("Agent='" + agent.getName() + "'; Unable to establish connection with MQTT host, check credentials; Exception: " + e);
        }
      }

      if (connectionStatus) {
        setConnectionStatus(ConnectionStatus.CONNECTED);
        LOG.info("Agent='" + agent.getName() + "'; Connected to MQTT host");
//                sendAttributeEvent(new AttributeEvent(agent.getId(), MqttExitAgent.NUMBER_OF_ACTIVE_DEVICES.getName(), activeShellyDevices.size(), timerService.getCurrentTimeMillis()));
      }
      reconnect = false;
    }

    disableButtonPrevious = disableButton;

    hostPrevious = host;
    usernamePrevious = username;
    passwordPrevious = password;
  }

  private void onMessageReceived(String topic, String message, Long timestamp) {
//        String name = topic.substring(topic.indexOf('/') + 1, topic.indexOf('/', topic.indexOf('/') + 1));
    String name = topic.substring(0, topic.indexOf('/'));

    int lastSlashIndex = topic.lastIndexOf('/');
    String variable = topic.substring(lastSlashIndex + 1);

    // Generate asset ID from JSON message
    String assetId = UniqueIdentifierGenerator.generateId(agent.getRealm() + name);
    // Find if asset already exists
    Asset<?> asset = assetService.findAsset(assetId);

    // Create automatically a new asset if it doesn't exist
    if (asset == null) {
      createAsset(name, assetId);
    }

    long timestampMillis = timestamp * 1000;
    // Add the time zone offset in milliseconds
    TimeZone localtimeZone = TimeZone.getDefault();
//            int offsetMillis = timeZone.getRawOffset();
    int offsetMillis = localtimeZone.getOffset(timestampMillis);
    long timestampMillisOffset = timestampMillis + offsetMillis;

    switch (variable) {
      case "energy":
//                datapointService.upsertValue(assetId, ExitMeterAsset.ENERGY.getName(), Double.parseDouble(message), timestampMillisOffset);
        sendAttributeEvent(new AttributeEvent(assetId, ExitMeterAsset.ENERGY.getName(), Double.parseDouble(message), timerService.getCurrentTimeMillis()));
        break;
      case "returned_energy":
//                datapointService.upsertValue(assetId, ExitMeterAsset.RETURNED_ENERGY.getName(), Double.parseDouble(message), timestampMillisOffset);
        sendAttributeEvent(new AttributeEvent(assetId, ExitMeterAsset.RETURNED_ENERGY.getName(), Double.parseDouble(message), timerService.getCurrentTimeMillis()));
        break;
      case "total":
//                datapointService.upsertValue(assetId, ExitMeterAsset.TOTAL_ENERGY.getName(), Double.parseDouble(message), timestampMillisOffset);
        sendAttributeEvent(new AttributeEvent(assetId, ExitMeterAsset.TOTAL_ENERGY.getName(), Double.parseDouble(message), timerService.getCurrentTimeMillis()));
        break;
      case "total_returned":
//                datapointService.upsertValue(assetId, ExitMeterAsset.TOTAL_RETURNED_ENERGY.getName(), Double.parseDouble(message), timestampMillisOffset);
        sendAttributeEvent(new AttributeEvent(assetId, ExitMeterAsset.TOTAL_RETURNED_ENERGY.getName(), Double.parseDouble(message), timerService.getCurrentTimeMillis()));
        break;
      case "power":
//                datapointService.upsertValue(assetId, ExitMeterAsset.POWER.getName(), Double.parseDouble(message), timestampMillisOffset);
        sendAttributeEvent(new AttributeEvent(assetId, ExitMeterAsset.POWER.getName(), Double.parseDouble(message), timerService.getCurrentTimeMillis()));
        break;
      case "reactive_power":
//                datapointService.upsertValue(assetId, ExitMeterAsset.REACTIVE_POWER.getName(), Double.parseDouble(message), timestampMillisOffset);
        sendAttributeEvent(new AttributeEvent(assetId, ExitMeterAsset.REACTIVE_POWER.getName(), Double.parseDouble(message), timerService.getCurrentTimeMillis()));
        break;
      case "voltage":
//                datapointService.upsertValue(assetId, ExitMeterAsset.VOLTAGE.getName(), Double.parseDouble(message), timestampMillisOffset);
        sendAttributeEvent(new AttributeEvent(assetId, ExitMeterAsset.VOLTAGE.getName(), Double.parseDouble(message), timerService.getCurrentTimeMillis()));
        break;
      default:
        System.out.println("Topic desconegut: " + variable);
        break;
    }

  }

  private void createAsset(String assetName, String assetId) {
    // Create new asset
    ExitMeterAsset exitMeterAsset = new ExitMeterAsset(assetName);
    // Set asset ID (required)
    exitMeterAsset.setId(assetId);
    // Set agent as parent of asset
    exitMeterAsset.setParentId(agent.getId());

    // Add asset to database
    assetService.mergeAsset(exitMeterAsset);

    LOG.info(agent.getType() + "='" + agent.getName() + "'; Created " + exitMeterAsset.getType() + ":'" + assetId + "'");
  }

}
