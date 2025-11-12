/*
 * Copyright 2025, OpenRemote Inc.
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
package org.openremote.agent.custom.earne;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.camel.builder.RouteBuilder;
import org.openremote.agent.custom.earne.EarneEnodeControlRequestMessage.ChargingControlCommand;
import org.openremote.agent.custom.earne.EarneEnodeControlRequestMessage.ChargingControlRequestMessage;
import org.openremote.agent.custom.earne.EarneEnodeControlRequestMessage.Discipline;
import org.openremote.agent.custom.earne.EarneEnodeControlRequestMessage.UnlinkControlCommand;
import org.openremote.agent.custom.earne.EarneEnodeControlRequestMessage.UnlinkControlRequestMessage;
import org.openremote.agent.custom.ourgrid.OurgridChargerAsset;
import org.openremote.agent.custom.ourgrid.OurgridHvacAsset;
import org.openremote.agent.custom.ourgrid.OurgridVehicleAsset;
import org.openremote.container.message.MessageBrokerService;
import org.openremote.manager.asset.AssetStorageService;
import org.openremote.manager.event.ClientEventService;
import org.openremote.model.PersistenceEvent;
import org.openremote.model.asset.Asset;
import org.openremote.model.asset.AssetFilter;
import org.openremote.model.attribute.AttributeEvent;
import org.openremote.model.event.Event;

import java.io.IOException;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.logging.Logger;

import static org.openremote.container.persistence.PersistenceService.HEADER_ENTITY_TYPE;
import static org.openremote.container.persistence.PersistenceService.PERSISTENCE_TOPIC;

/**
 * Sends EARN-E Enode control messages based on attribute and persistence events.
 * E.g. during challenges charging can be stopped/started based on an attribute on the charger/vehicle asset.
 * When EARN-E assets are deleted, an unlink control message is sent.
 */
public class EarneEnodeControlHandler extends RouteBuilder {

    private static final Logger LOG = Logger.getLogger(EarneEnodeControlHandler.class.getName());

    public static final String DEVICE_ID = "deviceId";
    public static final List<Class<? extends Asset>> EARNE_ENODE_ASSET_CLASSES = List.of(
            OurgridChargerAsset.class,
            OurgridHvacAsset.class,
            OurgridVehicleAsset.class
    );

    private final AssetStorageService assetStorageService;
    private final ClientEventService clientEventService;
    private final MessageBrokerService messageBrokerService;
    private final EarneEnodeProtocol earneProtocol;
    private final String routeId;

    public EarneEnodeControlHandler(AssetStorageService assetStorageService, ClientEventService clientEventService, MessageBrokerService messageBrokerService, EarneEnodeProtocol earneProtocol) {
        this.assetStorageService = assetStorageService;
        this.clientEventService = clientEventService;
        this.messageBrokerService = messageBrokerService;
        this.earneProtocol = earneProtocol;

        this.routeId = "Persistence-EarneEnodeControlHandler-" +  earneProtocol.getAgent().getId();
    }

    public void start() {
        AssetFilter<AttributeEvent> filter = new AssetFilter<AttributeEvent>()
                .setAssetClasses(EARNE_ENODE_ASSET_CLASSES)
                .setAttributeNames(OurgridChargerAsset.ALLOW_STOP_CHARGING_BUTTON.getName(), OurgridHvacAsset.ALLOW_STOP_HEATPUMP_BUTTON.getName(), OurgridVehicleAsset.ALLOW_STOP_CHARGING_BUTTON.getName());

        clientEventService.addSubscription(AttributeEvent.class, filter, this::onAttributeEvent);
        try {
            messageBrokerService.getContext().addRoutes(this);
        } catch (Exception e) {
            throw new IllegalStateException("Error while adding persistence route " + routeId, e);
        }
    }

    public void stop() {
        clientEventService.removeSubscription(this::onAttributeEvent);
        try {
            messageBrokerService.getContext().removeRoute(routeId);
        } catch (Exception e) {
            throw new IllegalStateException("Error while removing persistence route " + routeId, e);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void configure() throws Exception {
        from(PERSISTENCE_TOPIC)
                .routeId(routeId)
                .filter(exchange -> {
                    Class<?> entityType = exchange.getIn().getHeader(HEADER_ENTITY_TYPE, Class.class);
                    return EARNE_ENODE_ASSET_CLASSES.stream().anyMatch(assetClass -> assetClass.isAssignableFrom(entityType));
                })
                .process(exchange -> {
                    PersistenceEvent<Asset<?>> persistenceEvent = (PersistenceEvent<Asset<?>>) exchange.getIn().getBody(PersistenceEvent.class);
                    if (persistenceEvent.getCause() == PersistenceEvent.Cause.DELETE) {
                        Asset<?> asset = persistenceEvent.getEntity();
                        String userId = getDeviceId(asset.getParentId());
                        if (!userId.isBlank()) {
                            sendControlRequestMessage(new UnlinkControlRequestMessage(userId));
                        } else {
                            LOG.warning(() -> String.format("Could not determine userId for unlinking %s %s", asset.getAssetType(), asset.getId()));
                        }
                    }
                });
    }

    private void onAttributeEvent(Event event) {
        AttributeEvent attributeEvent = (AttributeEvent) event;
        Class<? extends Asset> assetClass = ((AttributeEvent) event).getAssetClass();
        String attributeName = attributeEvent.getName();

        if (assetClass == OurgridChargerAsset.class && OurgridChargerAsset.ALLOW_STOP_CHARGING_BUTTON.getName().equals(attributeName)) {
            LOG.finer(() -> "Allow stop charging button changed");
            attributeEvent.getValue(Boolean.class).ifPresent(value -> withAttributeEvent(attributeEvent, (userId, deviceId) -> {
                ChargingControlCommand command = value ? ChargingControlCommand.STOP : ChargingControlCommand.START;
                sendControlRequestMessage(new ChargingControlRequestMessage(userId, deviceId, Discipline.CHARGER, command));
            }));
        } else if (assetClass == OurgridHvacAsset.class && OurgridHvacAsset.ALLOW_STOP_HEATPUMP_BUTTON.getName().equals(attributeName)) {
            LOG.finer(() -> "Allow stop heatpump button changed");
        } else if (assetClass == OurgridVehicleAsset.class && OurgridVehicleAsset.ALLOW_STOP_CHARGING_BUTTON.getName().equals(attributeName)) {
            LOG.finer(() -> "Allow stop EV charging button changed");
            attributeEvent.getValue(Boolean.class).ifPresent(value -> withAttributeEvent(attributeEvent, (userId, deviceId) -> {
                ChargingControlCommand command = value ? ChargingControlCommand.STOP : ChargingControlCommand.START;
                sendControlRequestMessage(new ChargingControlRequestMessage(userId, deviceId, Discipline.EV, command));
            }));
        }
    }

    private String getDeviceId(String assetId) {
        return getDeviceId(assetStorageService.find(assetId, Asset.class));
    }

    private String getDeviceId(Asset<?> asset) {
        if (asset == null) {
            return "";
        }
        return asset.getAttribute(DEVICE_ID).flatMap(v -> v.getValue(String.class)).orElse("");
    }

    private void withAttributeEvent(AttributeEvent event, BiConsumer<String, String> consumer) {
        String userId = getDeviceId(event.getParentId());
        String deviceId = getDeviceId(event.getId());

        if (userId.isBlank()) {
            LOG.finer(() -> String.format("Cannot determine userId using parent asset (%s). Event: %s", event.getParentId(), event));
        } else if (deviceId.isBlank()) {
            LOG.finer(() -> String.format("Cannot determine deviceId using asset (%s). Event: %s", event.getId(), event));
        } else {
            consumer.accept(userId, deviceId);
        }
    }

    private void sendControlRequestMessage(EarneEnodeControlRequestMessage message) {
        try {
            earneProtocol.publishControlMessage(message.toJson());
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(String.format("Could not serialize control request message to JSON: %s", message), e);
        } catch (IOException e) {
            throw new IllegalStateException(String.format("Could not publish control request message: %s", message), e);
        }
    }
}
