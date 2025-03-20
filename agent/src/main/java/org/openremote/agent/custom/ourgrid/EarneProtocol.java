package org.openremote.agent.custom.ourgrid;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;
import org.openremote.agent.protocol.AbstractProtocol;
import org.openremote.model.Container;
import org.openremote.model.asset.Asset;
import org.openremote.model.asset.agent.ConnectionStatus;
import org.openremote.model.asset.agent.DefaultAgentLink;
import org.openremote.model.attribute.Attribute;
import org.openremote.model.attribute.AttributeEvent;
import org.openremote.model.geo.GeoJSONPoint;
import org.openremote.model.syslog.SyslogCategory;
import org.openremote.model.util.UniqueIdentifierGenerator;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.Optional;
import java.util.logging.Logger;

import static org.openremote.model.syslog.SyslogCategory.PROTOCOL;

public class EarneProtocol extends AbstractProtocol<EarneAgent, DefaultAgentLink> {
    Connection connection;
    Channel channel;

    private static final Logger LOG = SyslogCategory.getLogger(PROTOCOL, EarneProtocol.class);

    public static final String PROTOCOL_DISPLAY_NAME = "Earne";

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
        connectToRabbitMQ();
    }


    @Override
    protected void doStop(Container container) throws Exception {
        disconnectFromRabbitMQ();
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

    private void connectToRabbitMQ() {
        // Get credentials from agent
        String host = agent.getRabbitMqHost().orElse("");
        String username = agent.getRabbitMqUsername().orElse("");
        String password = agent.getRabbitMqPassword().orElse("");
        String virtualHost = agent.getRabbitMqVirtualHost().orElse("");
        String queue = agent.getRabbitMqQueue().orElse("");

        ConnectionStatus connectionStatus;

        // Check if all credentials are filled in
        if (host.isBlank() || username.isBlank() || password.isBlank() || virtualHost.isBlank() || queue.isBlank()) {
            setConnectionStatus(ConnectionStatus.DISCONNECTED);
            return;
        }

        // Set connection and channel variables
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(host);
        factory.setUsername(username);
        factory.setPassword(password);
        factory.setVirtualHost(virtualHost);

        // Set automatic recovery
        factory.setAutomaticRecoveryEnabled(true);
        factory.setNetworkRecoveryInterval(10000);

        try {
            connection = factory.newConnection();
            channel = connection.createChannel();

            // Create a consumer to receive messages from the queue
            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
                try {
                    onMessageReceived(message);
                } catch (Exception e) {
                    LOG.info(String.format("agentName='%s', agentId='%s'; Message not processed: '%s'; Exception: %s", agent.getName(), agent.getId(), message, e));
                }
            };

            try {
                // Start consuming messages
                channel.basicConsume(queue, true, deliverCallback, consumerTag -> {
                });

                connectionStatus = ConnectionStatus.CONNECTED;
                LOG.info(String.format("agentName='%s', agentId='%s'; Connected to RabbitMQ host", agent.getName(), agent.getId()));
            } catch (Exception e) {
                connectionStatus = ConnectionStatus.ERROR;
                LOG.warning(String.format("agentName='%s', agentId='%s'; Established connection with RabbitMQ host but unable to connect to queue, check credentials; Exception: %s", agent.getName(), agent.getId(), e));

                disconnectFromRabbitMQ();
            }

        } catch (Exception e) {
            connectionStatus = ConnectionStatus.ERROR;
            LOG.warning(String.format("agentName='%s', agentId='%s'; Unable to establish connection with RabbitMQ host, check credentials; Exception: %s", agent.getName(), agent.getId(), e));
        }

        setConnectionStatus(connectionStatus);
    }

    private void disconnectFromRabbitMQ() {
        try {
            if (channel != null && channel.isOpen()) {
                channel.close();
            }

            if (connection != null && connection.isOpen()) {
                connection.close();
                LOG.info(String.format("agentName='%s', agentId='%s'; RabbitMQ connection closed", agent.getName(), agent.getId()));
            }

        } catch (Exception e) {
            LOG.info(String.format("agentName='%s', agentId='%s'; No RabbitMQ connection to close", agent.getName(), agent.getId()));
        }
    }

    private void onMessageReceived(String message) {
        HashMap<String, String> parsedMessage = parseMessage(message); // Parse incoming message from RabbitMQ

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

        Optional<Double> wifiRssi = Optional.of(parsedMessage.get("wifiRSSI")).map(str -> {
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

        // Calculate gas flow rate
        if (ourgridMeterAsset != null) {
            Optional<String> timestampPrevious = ourgridMeterAsset.getAttribute(OurgridMeterAsset.TIMESTAMP).flatMap(Attribute::getValue);

            if (!parsedMessage.get("timestamp").isBlank() && timestampPrevious.isPresent()) {
                long timestampCurrentSeconds = Instant.parse(parsedMessage.get("timestamp")).getEpochSecond();
                long timestampPreviousSeconds = Instant.parse(timestampPrevious.get()).getEpochSecond();

                long dt = timestampCurrentSeconds - timestampPreviousSeconds;

                if (dt <= 10) {
                    LOG.info(String.format("agentName='%s', agentId='%s'; Meter assetName='%s'; Time between messages < 10 seconds: dt='%s'", agent.getName(), agent.getId(), ourgridMeterAsset.getName(), dt));
                }

                Optional<Double> gasDeliveredPrevious = ourgridMeterAsset.getAttribute(OurgridMeterAsset.GAS_IMPORT_TOTAL).flatMap(Attribute::getValue);

                if (dt > 0 && gasDelivered.isPresent() && gasDeliveredPrevious.isPresent()) {
                    double dGas = gasDelivered.get() - gasDeliveredPrevious.get();
                    Optional<Double> gasFlowRate = Optional.of(Math.round(((dGas / dt) * 60) * 1000.0) / 1000.0); // Gas flow rate (m3/min), rounded to 3 decimals

                    if (dGas >= 0) {
                        sendAttributeEvent(new AttributeEvent(assetId, OurgridMeterAsset.GAS_FLOW_RATE.getName(), gasFlowRate.orElse(null), timerService.getCurrentTimeMillis()));
                    } else {
                        LOG.info(String.format("agentName='%s', agentId='%s'; Meter assetName='%s'; Gas-meter reset, negative dGas: '%s'", agent.getName(), agent.getId(), ourgridMeterAsset.getName(), dGas));
                    }
                }
            }
        } else {
            createOurgridMeterAsset(uniqueDeviceName, assetId, parsedMessage);
            return;
        }

        String modelPrevious = ourgridMeterAsset.getAttribute(OurgridMeterAsset.SMARTMETER_MODEL).flatMap(Attribute::getValue).orElse("");
        String swVersionPrevious = ourgridMeterAsset.getAttribute(OurgridMeterAsset.SOFTWARE_VERSION).flatMap(Attribute::getValue).orElse("");

        if (!parsedMessage.get("model").isBlank() && !modelPrevious.equals(parsedMessage.get("model"))) {
            sendAttributeEvent(new AttributeEvent(assetId, OurgridMeterAsset.SMARTMETER_MODEL.getName(), parsedMessage.get("model"), timerService.getCurrentTimeMillis()));
            LOG.info(String.format("agentName='%s', agentId='%s'; Meter Asset='%s'; Smart-meter model changed to: '%s'", agent.getName(), agent.getId(), ourgridMeterAsset.getName(), parsedMessage.get("model")));
        }

        if (!parsedMessage.get("swVersion").isBlank() && !swVersionPrevious.equals(parsedMessage.get("swVersion"))) {
            sendAttributeEvent(new AttributeEvent(assetId, OurgridMeterAsset.SOFTWARE_VERSION.getName(), parsedMessage.get("swVersion"), timerService.getCurrentTimeMillis()));
            LOG.info(String.format("agentName='%s', agentId='%s'; Meter Asset='%s'; Earn-E device software version update: '%s'", agent.getName(), agent.getId(), ourgridMeterAsset.getName(), parsedMessage.get("swVersion")));
        }

        if (parsedMessage.get("timestamp").isBlank()) {
            LOG.info(String.format("agentName='%s', agentId='%s'; Meter Asset='%s'; Missing timestamp, message: '%s'", agent.getName(), agent.getId(), ourgridMeterAsset.getName(), message));
        }

        sendAttributeEvent(new AttributeEvent(assetId, OurgridMeterAsset.TIMESTAMP.getName(), parsedMessage.get("timestamp"), timerService.getCurrentTimeMillis()));

        energyExported.ifPresent(value -> sendAttributeEvent(new AttributeEvent(assetId, OurgridMeterAsset.ENERGY_EXPORT_TOTAL.getName(), value, timerService.getCurrentTimeMillis())));
        energyImported.ifPresent(value -> sendAttributeEvent(new AttributeEvent(assetId, OurgridMeterAsset.ENERGY_IMPORT_TOTAL.getName(), value, timerService.getCurrentTimeMillis())));
        energyNet.ifPresent(value -> sendAttributeEvent(new AttributeEvent(assetId, OurgridMeterAsset.ENERGY_NET_TOTAL.getName(), value, timerService.getCurrentTimeMillis())));
        gasDelivered.ifPresent(value -> sendAttributeEvent(new AttributeEvent(assetId, OurgridMeterAsset.GAS_IMPORT_TOTAL.getName(), value, timerService.getCurrentTimeMillis())));
        power.ifPresent(value -> sendAttributeEvent(new AttributeEvent(assetId, OurgridMeterAsset.POWER.getName(), value, timerService.getCurrentTimeMillis())));
        powerDelivered.ifPresent(value -> sendAttributeEvent(new AttributeEvent(assetId, OurgridMeterAsset.POWER_IMPORT.getName(), value, timerService.getCurrentTimeMillis())));
        powerReturned.ifPresent(value -> sendAttributeEvent(new AttributeEvent(assetId, OurgridMeterAsset.POWER_EXPORT.getName(), value, timerService.getCurrentTimeMillis())));
        wifiRssi.ifPresent(value -> sendAttributeEvent(new AttributeEvent(assetId, OurgridMeterAsset.WIFI_SIGNAL.getName(), value, timerService.getCurrentTimeMillis())));
    }


    public HashMap<String, String> parseMessage(String message) {
//        System.out.println("message:\n" + message);
        HashMap<String, String> parsedMessage = new HashMap<>();

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

            parsedMessage.put("timestamp", timestamp);
            parsedMessage.put("swVersion", swVersion);
            parsedMessage.put("deviceId", deviceId);
            parsedMessage.put("model", model);
            parsedMessage.put("wifiRSSI", wifiRssi);
            parsedMessage.put("energy_delivered_tariff1", energyDeliveredTariff1);
            parsedMessage.put("energy_delivered_tariff2", energyDeliveredTariff2);
            parsedMessage.put("energy_returned_tariff1", energyReturnedTariff1);
            parsedMessage.put("energy_returned_tariff2", energyReturnedTariff2);
            parsedMessage.put("gas_delivered", gasDelivered);
            parsedMessage.put("power_delivered", powerDelivered);
            parsedMessage.put("power_returned", powerReturned);
            parsedMessage.put("latitude", latitude);
            parsedMessage.put("longitude", longitude);

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
            LOG.info(String.format("agentName='%s', agentId='%s'; Message not parsed: '%s'", agent.getId(), agent.getName(), message));
        }

        return parsedMessage;
    }

    private void createOurgridMeterAsset(String assetName, String assetId, HashMap<String, String> parsedMessage) {
        OurgridMeterAsset ourgridMeterAsset = new OurgridMeterAsset(assetName); // Create new Meter Asset
        ourgridMeterAsset.setId(assetId); // Set asset ID (required)

        String parentId = agent.getMeterParentId().orElse(""); // Get parent ID

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
                LOG.info(String.format("agentName='%s', agentId='%s'; assetName='%s'; Invalid Geographic Coordinates. Longitude: '%s' Latitude: '%s'",
                        agent.getName(), agent.getId(), ourgridMeterAsset.getName(), parsedMessage.get("longitude"), parsedMessage.get("latitude")));
            }
        }

        ourgridMeterAsset.setDeviceId(parsedMessage.get("deviceId")); // Unique Device ID will be used to connect between Earn-E and OpenRemote app
        ourgridMeterAsset.setSmartmeterModel(parsedMessage.get("model")); // Smart meter model
        ourgridMeterAsset.setSoftwareVersion(parsedMessage.get("swVersion")); // Software version of Earn-E device

        assetService.mergeAsset(ourgridMeterAsset); // Merge asset into database

        LOG.info(String.format("agentName='%s', agentId='%s'; Created Meter Asset: '%s'", agent.getName(), agent.getId(), assetName));
    }
}