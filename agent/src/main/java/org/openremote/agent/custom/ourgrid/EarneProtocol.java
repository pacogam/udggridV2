package org.openremote.agent.custom.ourgrid;

import org.openremote.agent.protocol.AbstractProtocol;
import org.openremote.container.util.UniqueIdentifierGenerator;
import org.openremote.model.Container;
import org.openremote.model.asset.Asset;
import org.openremote.model.asset.agent.ConnectionStatus;
import org.openremote.model.asset.agent.DefaultAgentLink;
import org.openremote.model.attribute.*;
import org.openremote.model.geo.GeoJSONPoint;
import org.openremote.model.syslog.SyslogCategory;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import static org.openremote.model.syslog.SyslogCategory.PROTOCOL;

public class EarneProtocol extends AbstractProtocol<EarneAgent, DefaultAgentLink> {
    private String hostPrevious = "";
    private String usernamePrevious = "";
    private String passwordPrevious = "";
    private String virtualhostPrevious = "";
    private String queuePrevious = "";

    private boolean disableButtonPrevious = false;
    private boolean reconnect = true;
    private boolean connectionStatus = false;
    private long connectionStartTime = 0;
    private Connection connection;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    private final HashMap<String, Long> activeEarneDevices = new HashMap<>();
    private int activeEarneDevicesSizePrevious = 0;


    public static final String PROTOCOL_DISPLAY_NAME = "Earne";
    private static final Logger LOG = SyslogCategory.getLogger(PROTOCOL, EarneProtocol.class);

    public EarneProtocol(EarneAgent agent) {
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
    protected void doStart(Container container) throws Exception {
        setConnectionStatus(ConnectionStatus.CONNECTING);

        final Runnable beeper = () -> {
            connectToRabbitMQ();
        };

        scheduler.scheduleAtFixedRate(beeper, 3, 5, TimeUnit.SECONDS);
    }

    @Override
    protected void doStop(Container container) throws Exception {
        disconnectFromRabbitMQ();
        scheduler.shutdown();
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


    private void disconnectFromRabbitMQ() {
        try {
            connection.close();
            LOG.info("Agent='" + agent.getName() + "'; RabbitMQ connection closed");
        } catch (Exception e) {
            LOG.info("Agent='" + agent.getName() + "'; No RabbitMQ connection to close");
        }
    }

    private void connectToRabbitMQ() {
        // Get credentials from agent
        String host = agent.getRabbitMqHost().orElse("");
        String username = agent.getRabbitMqUsername().orElse("");
        String password = agent.getRabbitMqPassword().orElse("");
        String virtualhost = agent.getRabbitMqVirtualhost().orElse("");
        String queue = agent.getRabbitMqQueue().orElse("");

        // Disable agent
        boolean disableButton = agent.isDisabled().orElse(false); // Button pressed = true -> agent is disabled

        if (disableButton && connectionStatus) {
            connectionStatus = false;
            setConnectionStatus(ConnectionStatus.DISABLED);
            LOG.info("Agent='" + agent.getName() + "'; Agent disabled");

            disconnectFromRabbitMQ();
            disconnectActiveDevices();
        } else if (disableButton && agent.getAgentStatus().isPresent() && agent.getAgentStatus().get() == ConnectionStatus.ERROR) {
            setConnectionStatus(ConnectionStatus.DISABLED);
            LOG.info("Agent='" + agent.getName() + "'; Agent disabled");
            LOG.info("Agent='" + agent.getName() + "'; No RabbitMQ connection to close");
        } else if (!disableButton && disableButtonPrevious) {
            reconnect = true;
            LOG.info("Agent='" + agent.getName() + "'; Agent enabled");
        }

        // Check if credentials have changed
        if (!host.equals(hostPrevious) || !username.equals(usernamePrevious) || !password.equals(passwordPrevious) || !virtualhost.equals(virtualhostPrevious) || !queue.equals(queuePrevious)) {
            reconnect = true;
        }

        // Disconnect from host when there are new credentials
        if (connectionStatus && reconnect) {
            connectionStatus = false;
            disconnectFromRabbitMQ();
            disconnectActiveDevices();
        }

        // Connect to host
        if (!connectionStatus && !disableButton) {
            connectionStatus = true;

            try {
                ConnectionFactory factory = new ConnectionFactory();
                factory.setHost(host);
                factory.setUsername(username);
                factory.setPassword(password);
                factory.setVirtualHost(virtualhost);
                connection = factory.newConnection();
                Channel channel = connection.createChannel();

                DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                    String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
                    try {
                        onMessageReceived(message);
                    } catch (Exception e) {
                        LOG.info("Agent='" + agent.getName() + "'; Message not processed: '" + message + "'; Exception: " + e);
                    }
                };

                try {
                    channel.basicConsume(queue, true, deliverCallback, consumerTag -> {
                    });
                } catch (Exception e) {
                    connectionStatus = false;

                    if (reconnect) {
                        setConnectionStatus(ConnectionStatus.ERROR);
                        LOG.warning("Agent='" + agent.getName() + "'; Established connection with RabbitMQ host but unable to connect to queue, check credentials; Exception: " + e);
                    }
                    connection.close();
                }

            } catch (Exception e) {
                connectionStatus = false;

                if (reconnect) {
                    setConnectionStatus(ConnectionStatus.ERROR);
                    LOG.warning("Agent='" + agent.getName() + "'; Unable to establish connection with RabbitMQ host, check credentials; Exception: " + e);
                }
            }

            if (connectionStatus) {
                setConnectionStatus(ConnectionStatus.CONNECTED);
                LOG.info("Agent='" + agent.getName() + "'; Connected to RabbitMQ host");

                sendAttributeEvent(new AttributeEvent(agent.getId(), EarneAgent.NUMBER_OF_ACTIVE_DEVICES.getName(), activeEarneDevices.size(), timerService.getCurrentTimeMillis()));
                connectionStartTime = timerService.getCurrentTimeMillis();
            }
            reconnect = false;
        }

        // Get number of active devices
        int activePeriodMinutes = agent.getActivePeriod().orElse(5); // Number of minutes after which Earn-E device is considered inactive

        if (connectionStatus) {
            checkActiveDevices(activePeriodMinutes);
        }

        if (agent.getActivePeriod().isEmpty()) {
            sendAttributeEvent(new AttributeEvent(agent.getId(), EarneAgent.ACTIVE_PERIOD.getName(), activePeriodMinutes, timerService.getCurrentTimeMillis()));
        }

        disableButtonPrevious = disableButton;

        hostPrevious = host;
        usernamePrevious = username;
        passwordPrevious = password;
        virtualhostPrevious = virtualhost;
        queuePrevious = queue;
    }

    private void onMessageReceived(String message) {
        HashMap<String, String> parsedMessage = parseMessage(message); // Parse incoming message from RabbitMQ and create key-value pairs in a HashMap

        Optional<Double> wifiRssi = Optional.of(parsedMessage.get("wifiRSSI")).map(str -> { // Convert String to value
            try {
                return Double.valueOf(str);
            } catch (Exception e) {
                return null;
            }
        });

        Optional<Double> energyDeliveredTariff1 = Optional.of(parsedMessage.get("energy_delivered_tariff1")).map(str -> {
            try {
                return Double.valueOf(str);
            } catch (Exception e) {
                return null;
            }
        });

        Optional<Double> energyDeliveredTariff2 = Optional.of(parsedMessage.get("energy_delivered_tariff2")).map(str -> {
            try {
                return Double.valueOf(str);
            } catch (Exception e) {
                return null;
            }
        });

        Optional<Double> energyReturnedTariff1 = Optional.of(parsedMessage.get("energy_returned_tariff1")).map(str -> {
            try {
                return Double.valueOf(str);
            } catch (Exception e) {
                return null;
            }
        });

        Optional<Double> energyReturnedTariff2 = Optional.of(parsedMessage.get("energy_returned_tariff2")).map(str -> {
            try {
                return Double.valueOf(str);
            } catch (Exception e) {
                return null;
            }
        });

        Optional<Double> powerDelivered = Optional.of(parsedMessage.get("power_delivered")).map(str -> {
            try {
                return Double.valueOf(str);
            } catch (Exception e) {
                return null;
            }
        });

        Optional<Double> powerReturned = Optional.of(parsedMessage.get("power_returned")).map(str -> {
            try {
                return Double.valueOf(str);
            } catch (Exception e) {
                return null;
            }
        });

        Optional<Double> gasDelivered = Optional.of(parsedMessage.get("gas_delivered")).map(str -> {
            try {
                return Double.valueOf(str);
            } catch (Exception e) {
                return null;
            }
        });

        Optional<Double> energyImported = Optional.empty();
        Optional<Double> energyExported = Optional.empty();
        Optional<Double> energyNet = Optional.empty();
        Optional<Double> power = Optional.empty();
        Optional<Double> powerCalculated = Optional.empty();
        Optional<Double> gasFlowRate = Optional.empty();

        if (energyDeliveredTariff1.isPresent() && energyDeliveredTariff2.isPresent()) {
            energyImported = Optional.of(Math.round((energyDeliveredTariff1.get() + energyDeliveredTariff2.get()) * 1000.0) / 1000.0); // E_imported_current, rounded to 3 decimals
        }

        if (energyReturnedTariff1.isPresent() && energyReturnedTariff2.isPresent()) {
            energyExported = Optional.of(Math.round((energyReturnedTariff1.get() + energyReturnedTariff2.get()) * 1000.0) / 1000.0); // E_exported_current, rounded to 3 decimals
        }

        if (energyImported.isPresent() && energyExported.isPresent()) {
            energyNet = Optional.of(Math.round((energyImported.get() - energyExported.get()) * 1000.0) / 1000.0); // dE_current, rounded to 3 decimals
        }

        if (powerDelivered.isPresent() && powerReturned.isPresent()) {
            power = Optional.of(1000 * (powerDelivered.get() - powerReturned.get())); // Direct power readout
        }


        String uniqueDeviceName = parsedMessage.get("deviceId"); // Asset name is unique Earn-E device ID
        String assetId = UniqueIdentifierGenerator.generateId(agent.getRealm() + uniqueDeviceName); // Generate unique asset ID based on unique asset name
        OurgridMeterAsset ourgridMeterAsset = assetService.findAsset(assetId); // Find if asset already exists

        if (ourgridMeterAsset != null) {
            // Calculate "power calculated" and "gas flow rate"
            Optional<String> timestampPrevious = ourgridMeterAsset.getAttribute(OurgridMeterAsset.TIMESTAMP).flatMap(Attribute::getValue);
            Optional<Double> energyNetPrevious = ourgridMeterAsset.getAttribute(OurgridMeterAsset.ENERGY_NET_TOTAL).flatMap(Attribute::getValue);
            Optional<Double> gasDeliveredPrevious = ourgridMeterAsset.getAttribute(OurgridMeterAsset.GAS_IMPORT_TOTAL).flatMap(Attribute::getValue);

            if (!parsedMessage.get("timestamp").isBlank() && timestampPrevious.isPresent()) {  // Power and gas flow rate calculation
                long timestampCurrentSeconds = Instant.parse(parsedMessage.get("timestamp")).getEpochSecond();
                long timestampPreviousSeconds = Instant.parse(timestampPrevious.get()).getEpochSecond();

                long dt = timestampCurrentSeconds - timestampPreviousSeconds;

                if (dt <= 10) {
                    LOG.info("Agent='" + agent.getName() + "'; Meter Asset='" + ourgridMeterAsset.getName() + "'; Time between messages < 10 seconds: dt='" + dt + "'");
                }

                if (dt != 0 && gasDelivered.isPresent() && gasDeliveredPrevious.isPresent()) {
                    double dGas = gasDelivered.get() - gasDeliveredPrevious.get();
                    gasFlowRate = Optional.of(Math.round(((dGas / dt) * 60) * 1000.0) / 1000.0); // Gas flow rate (m3/min), rounded to 3 decimals

                    if (dGas >= 0) {
                        sendAttributeEvent(new AttributeEvent(assetId, OurgridMeterAsset.GAS_FLOW_RATE.getName(), gasFlowRate.orElse(null), timerService.getCurrentTimeMillis()));
                        if (gasFlowRate.get() > 1.0) {
                            LOG.info("Agent='" + agent.getName() + "'; Meter Asset='" + ourgridMeterAsset.getName() + "'; Gas Flow Rate > 1; " +
                                    "timestampCurrentSeconds: " + timestampCurrentSeconds + "; " +
                                    "timestampPreviousSeconds: " + timestampPreviousSeconds + "; " +
                                    "dt: " + dt + "; " +
                                    "gasDelivered: " + gasDelivered + "; " +
                                    "gasDeliveredPrevious: " + gasDeliveredPrevious + "; " +
                                    "dGas: " + dGas + "; " +
                                    "gasFlowRate: " + gasFlowRate + "; " +
                                    "energyNet: " + energyNet + "; " +
                                    "energyNetPrevious: " + energyNetPrevious + "; " +
                                    "powerCalculated: " + powerCalculated + "; "
                            );
                        }
                    } else {
                        LOG.info("Agent='" + agent.getName() + "'; Meter Asset='" + ourgridMeterAsset.getName() + "'; Gas-meter reset, negative dGas: '" + dGas + "'");
                    }
                }

            }
        } else {
            // Create new Meter Asset
            ourgridMeterAsset = new OurgridMeterAsset(uniqueDeviceName);
            ourgridMeterAsset.setId(assetId); // Set asset ID (required)

            // Set parent ID
            String parentId = agent.getMeterSParentId().orElse("");

            if (!parentId.isEmpty()) {
                Asset<?> parentAsset = assetService.findAsset(parentId);
                if (parentAsset != null) {
                    ourgridMeterAsset.setParentId(parentId); // Set given asset as parent
                } else {
                    ourgridMeterAsset.setParentId(agent.getId()); // Set agent as parent
                }
            } else {
                ourgridMeterAsset.setParentId(agent.getId()); // Set agent as parent
            }

            // Set coordinates
            if (!parsedMessage.get("longitude").isEmpty() && !parsedMessage.get("latitude").isEmpty()) { // If coordinate fields are not empty
                try {
                    double longitude = Double.parseDouble(parsedMessage.get("longitude")); // Convert string to double
                    double latitude = Double.parseDouble(parsedMessage.get("latitude"));

                    if (!(longitude == 0.0) && !(latitude == 0.0)) {
                        ourgridMeterAsset.setLocation(new GeoJSONPoint(longitude, latitude)); // Set location
                    }
                } catch (Exception e) {
                    LOG.info("Agent='" + agent.getName() + "'; Meter Asset='" + ourgridMeterAsset.getName() + "'; Invalid Geographic Coordinates. Longitude: '"
                            + parsedMessage.get("longitude") + "' Latitude: '" + parsedMessage.get("latitude") + "'");
                }
            }

            ourgridMeterAsset.setDeviceId(parsedMessage.get("deviceId")); // Unique Device ID will be used to connect between Earn-E and OpenRemote app
            ourgridMeterAsset.setSmartmeterModel(parsedMessage.get("model")); // Smart meter model
            ourgridMeterAsset.setSoftwareVersion(parsedMessage.get("swVersion")); // Software version of Earn-E device

            assetService.mergeAsset(ourgridMeterAsset); // Create asset

            LOG.info("Agent='" + agent.getName() + "'; Created Meter Asset: '" + uniqueDeviceName + "'");
        }

        String modelPrevious = ourgridMeterAsset.getAttribute(OurgridMeterAsset.SMARTMETER_MODEL).flatMap(Attribute::getValue).orElse("");
        String swVersionPrevious = ourgridMeterAsset.getAttribute(OurgridMeterAsset.SOFTWARE_VERSION).flatMap(Attribute::getValue).orElse("");

        if (!parsedMessage.get("model").isBlank() && !modelPrevious.equals(parsedMessage.get("model"))) {
            sendAttributeEvent(new AttributeEvent(assetId, OurgridMeterAsset.SMARTMETER_MODEL.getName(), parsedMessage.get("model"), timerService.getCurrentTimeMillis()));
            LOG.info("Agent='" + agent.getName() + "'; Meter Asset='" + ourgridMeterAsset.getName() + "'; Smart-meter model changed to: '" + parsedMessage.get("model") + "'");
        }

        if (!parsedMessage.get("swVersion").isBlank() && !swVersionPrevious.equals(parsedMessage.get("swVersion"))) {
            sendAttributeEvent(new AttributeEvent(assetId, OurgridMeterAsset.SOFTWARE_VERSION.getName(), parsedMessage.get("swVersion"), timerService.getCurrentTimeMillis()));
            LOG.info("Agent='" + agent.getName() + "'; Meter Asset='" + ourgridMeterAsset.getName() + "'; Earn-E device software version update: '" + parsedMessage.get("swVersion") + "'");
        }

        if (parsedMessage.get("timestamp").isBlank()) {
            LOG.info("Agent='" + agent.getName() + "'; Meter Asset='" + ourgridMeterAsset.getName() + "'; No timestamp, message: '" + message + "'");
        }

        sendAttributeEvent(new AttributeEvent(assetId, OurgridMeterAsset.TIMESTAMP.getName(), parsedMessage.get("timestamp"), timerService.getCurrentTimeMillis()));

        wifiRssi.ifPresent(value -> sendAttributeEvent(new AttributeEvent(assetId, OurgridMeterAsset.WIFI_SIGNAL.getName(), value, timerService.getCurrentTimeMillis())));
        energyImported.ifPresent(value -> sendAttributeEvent(new AttributeEvent(assetId, OurgridMeterAsset.ENERGY_IMPORT_TOTAL.getName(), value, timerService.getCurrentTimeMillis())));
        energyExported.ifPresent(value -> sendAttributeEvent(new AttributeEvent(assetId, OurgridMeterAsset.ENERGY_EXPORT_TOTAL.getName(), value, timerService.getCurrentTimeMillis())));
        energyNet.ifPresent(value -> sendAttributeEvent(new AttributeEvent(assetId, OurgridMeterAsset.ENERGY_NET_TOTAL.getName(), value, timerService.getCurrentTimeMillis())));
        powerDelivered.ifPresent(value -> sendAttributeEvent(new AttributeEvent(assetId, OurgridMeterAsset.POWER_IMPORT.getName(), value, timerService.getCurrentTimeMillis())));
        powerReturned.ifPresent(value -> sendAttributeEvent(new AttributeEvent(assetId, OurgridMeterAsset.POWER_EXPORT.getName(), value, timerService.getCurrentTimeMillis())));
        power.ifPresent(value -> sendAttributeEvent(new AttributeEvent(assetId, OurgridMeterAsset.POWER.getName(), value, timerService.getCurrentTimeMillis())));
        gasDelivered.ifPresent(value -> sendAttributeEvent(new AttributeEvent(assetId, OurgridMeterAsset.GAS_IMPORT_TOTAL.getName(), value, timerService.getCurrentTimeMillis())));

        activeEarneDevices.put(parsedMessage.get("deviceId"), timerService.getCurrentTimeMillis());
    }

    public HashMap<String, String> parseMessage(String message) {
//        System.out.println("message:\n" + message);
        HashMap<String, String> map = new HashMap<>();

        try {
            String messageContent = message.substring(1, message.length() - 1); // Remove leading and trailing curly braces
            String[] pairs = messageContent.split("," + "(?![^{]*})" + "(?=([^\"]*\"[^\"]*\")*[^\"]*$)"); // Split data based on delimiter "," but not between curly brackets {} and double quotes ""

            String timestamp = "";
            String swVersion = "";
            String deviceId = "";
            String model = "";
            String wifiRssi = "";
            String energyDeliveredTariff1 = "";
            String energyDeliveredTariff2 = "";
            String energyReturnedTariff1 = "";
            String energyReturnedTariff2 = "";
            String gasDelivered = "";
            String powerDelivered = "";
            String powerReturned = "";
            String latitude = "";
            String longitude = "";

            for (String pair : pairs) {
                String[] keyValue = pair.split(":", 2); // Split name and value based on the first occurrence of delimiter ":"

                String key = keyValue[0];
                String value = keyValue[1];
//                System.out.println("key   = " + key);
//                System.out.println("value = " + value);

                switch (key) {
                    case "\"timestamp\"" -> timestamp = value.substring(1, value.length() - 1);
                    case "\"swVersion\"" -> swVersion = value;
                    case "\"deviceId\"" -> deviceId = value.substring(1, value.length() - 1);
                    case "\"model\"" -> model = value.substring(1, value.length() - 1);

                    case "\"wifiRSSI\"" -> wifiRssi = value;
                    case "\"energy_delivered_tariff1\"" -> energyDeliveredTariff1 = value;
                    case "\"energy_delivered_tariff2\"" -> energyDeliveredTariff2 = value;
                    case "\"energy_returned_tariff1\"" -> energyReturnedTariff1 = value;
                    case "\"energy_returned_tariff2\"" -> energyReturnedTariff2 = value;
                    case "\"gas_delivered\"" -> gasDelivered = value;
                    case "\"power_delivered\"" -> powerDelivered = value;
                    case "\"power_returned\"" -> powerReturned = value;

                    case "\"geo\"" -> {
                        try {
                            String[] geoValues = value.substring(1, value.length() - 1).replaceAll("\"", "").split(","); // Extract the coordinates
                            latitude = geoValues[0].split(":")[1];
                            longitude = geoValues[1].split(":")[1];
                        } catch (Exception e) {
                            LOG.info("Missing coordinates: " + value);
                        }
                    }
                }
            }

            map.put("timestamp", timestamp);
            map.put("swVersion", swVersion);
            map.put("deviceId", deviceId);
            map.put("model", model);
            map.put("wifiRSSI", wifiRssi);
            map.put("energy_delivered_tariff1", energyDeliveredTariff1);
            map.put("energy_delivered_tariff2", energyDeliveredTariff2);
            map.put("energy_returned_tariff1", energyReturnedTariff1);
            map.put("energy_returned_tariff2", energyReturnedTariff2);
            map.put("gas_delivered", gasDelivered);
            map.put("power_delivered", powerDelivered);
            map.put("power_returned", powerReturned);
            map.put("latitude", latitude);
            map.put("longitude", longitude);

//            System.out.println(message);
//            System.out.println("Timestamp: " + timestamp);
//            System.out.println("Software Version: " + swVersion);
//            System.out.println("Device ID: " + deviceId);
//            System.out.println("Model: " + model);
//            System.out.println("WiFi RSSI: " + wifiRssi);
//            System.out.println("Energy Delivered Tariff 1: " + energyDeliveredTariff1);
//            System.out.println("Energy Delivered Tariff 2: " + energyDeliveredTariff2);
//            System.out.println("Energy Returned Tariff 1: " + energyReturnedTariff1);
//            System.out.println("Energy Returned Tariff 2: " + energyReturnedTariff2);
//            System.out.println("Power Delivered: " + powerDelivered);
//            System.out.println("Power Returned: " + powerReturned);
//            System.out.println("Gas Delivered: " + gasDelivered);
//            System.out.println("Latitude: " + latitude);
//            System.out.println("Longitude: " + longitude);
        } catch (Exception e) {
            LOG.info("Agent='" + agent.getName() + "'; Message not parsed: '" + message + "'");
        }

        return map;
    }

    private void checkActiveDevices(int activePeriodMinutes) {
        long activePeriodMillis = activePeriodMinutes * 60000L;
        activeEarneDevices.entrySet().removeIf(entry -> timerService.getCurrentTimeMillis() - entry.getValue() > activePeriodMillis);

        if (activeEarneDevices.size() != activeEarneDevicesSizePrevious) {
            sendAttributeEvent(new AttributeEvent(agent.getId(), EarneAgent.NUMBER_OF_ACTIVE_DEVICES.getName(), activeEarneDevices.size(), timerService.getCurrentTimeMillis()));
        }

        if (activeEarneDevices.size() == 0 && (timerService.getCurrentTimeMillis() - connectionStartTime) > activePeriodMillis) {
            reconnect = true;
            LOG.warning("Agent='" + agent.getName() + "'; No active devices during the last " + activePeriodMinutes + " minutes. Reconnecting to RabbitMQ host");
        }

        activeEarneDevicesSizePrevious = activeEarneDevices.size();
    }

    private void disconnectActiveDevices() {
        activeEarneDevices.clear();
        activeEarneDevicesSizePrevious = 0;
        sendAttributeEvent(new AttributeEvent(agent.getId(), EarneAgent.NUMBER_OF_ACTIVE_DEVICES.getName(), null, timerService.getCurrentTimeMillis()));
    }
}
