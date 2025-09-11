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
import org.openremote.agent.custom.ourgrid.OurgridDeviceAsset;
import org.openremote.manager.asset.AssetStorageService;
import org.openremote.model.asset.Asset;
import org.openremote.model.attribute.AttributeEvent;
import org.openremote.model.geo.GeoJSONPoint;
import org.openremote.model.protocol.ProtocolAssetService;
import org.openremote.model.syslog.SyslogCategory;
import org.openremote.model.util.UniqueIdentifierGenerator;
import org.openremote.model.value.AttributeDescriptor;

import java.util.Optional;
import java.util.logging.Logger;

import static org.openremote.agent.custom.earne.EarneMeterProtocol.OBJECT_MAPPER;
import static org.openremote.model.syslog.SyslogCategory.PROTOCOL;

/**
 * Abstract base class for handling EARN-E JSON messages.
 * Deserializes JSON messages into a specific message type and provides methods to handle the message.
 *
 * @param <M> the message type
 */
public abstract class AbstractEarneMessageHandler<M> {

    private static final Logger LOG = SyslogCategory.getLogger(PROTOCOL, EarneMeterProtocol.class);

    protected final Class<M> messageClass;
    protected final Asset<?> agent;
    protected final ProtocolAssetService assetService;
    protected final AssetStorageService assetStorageService;

    public AbstractEarneMessageHandler(Class<M> messageClass, Asset<?> agent, ProtocolAssetService assetService, AssetStorageService assetStorageService) {
        this.messageClass = messageClass;
        this.agent = agent;
        this.assetService = assetService;
        this.assetStorageService = assetStorageService;
    }

    public void handleMessage(String json) {
        if (json == null || json.isBlank()) {
            throw new IllegalArgumentException("Message must not be null or blank");
        }

        String messageName = messageClass.getSimpleName();
        LOG.finer(() -> String.format("agentName='%s', agentId='%s'; Handling %s: '%s'", agent.getName(), agent.getId(), messageName, json));

        M message;
        try {
            message = OBJECT_MAPPER.readValue(json, messageClass);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Exception while deserializing " + messageName + ": " + json, e);
        }

        LOG.fine(() -> String.format("agentName='%s', agentId='%s'; Deserialized %s: '%s'", agent.getName(), agent.getId(), messageName, message));

        handleMessage(message);
    }

    protected String generateAssetId(String deviceId) {
        return UniqueIdentifierGenerator.generateId(agent.getRealm() + deviceId);
    }

    protected void updateAssetIds(Asset<?> asset, String assetId, String deviceId, String parentId) {
        if (!(asset instanceof OurgridDeviceAsset deviceAsset)) {
            throw new IllegalArgumentException("Asset must be of type " + OurgridDeviceAsset.class.getSimpleName());
        }
        if (assetId == null || assetId.isBlank()) {
            throw new IllegalArgumentException("Asset ID must not be null or blank");
        }
        if (deviceId == null || deviceId.isBlank()) {
            throw new IllegalArgumentException("Device ID must not be null or blank");
        }

        asset.setId(assetId);
        deviceAsset.setDeviceId(deviceId);

        if (parentId != null && !parentId.isBlank()) {
            Asset<?> parentAsset = assetService.findAsset(parentId);
            if (parentAsset != null) {
                asset.setParentId(parentId);
            } else {
                LOG.finer(() -> String.format("agentName='%s', agentId='%s'; Parent asset '%s' not found", agent.getName(), agent.getId(), parentId));
                asset.setParentId(agent.getId());
            }
        } else {
            LOG.finer(() -> String.format("agentName='%s', agentId='%s'; No parent asset provided for asset '%s'", agent.getName(), agent.getId(), asset.getId()));
            asset.setParentId(agent.getId());
        }
    }

    protected boolean updateAssetLocation(Asset<?> asset, Double longitude, Double latitude) {
        if (longitude != null && latitude != null) {
            if (longitude != 0.0 && latitude != 0.0) {
                Optional<GeoJSONPoint> location = asset.getLocation();
                if (location.isPresent()) {
                    GeoJSONPoint point = location.get();
                    if (point.getX() == longitude && point.getY() == latitude) {
                        return false;
                    }
                }

                asset.setLocation(new GeoJSONPoint(longitude, latitude));
                return true;
            } else {
                LOG.finer(() -> String.format("agentName='%s', agentId='%s'; Invalid coordinates provided for asset '%s': longitude=%f, latitude=%f", agent.getName(), agent.getId(), asset.getId(), longitude, latitude));
            }
        } else {
            LOG.finer(() -> String.format("agentName='%s', agentId='%s'; No coordinates provided for asset '%s'", agent.getName(), agent.getId(), asset.getId()));
        }
        return false;
    }

    protected abstract void handleMessage(M message);

    <T> void sendAttributeEvent(String assetId, AttributeDescriptor<T> descriptor, T value) {
        if (value != null) {
            assetService.sendAttributeEvent(new AttributeEvent(assetId, descriptor, value));
        }
    }

}
