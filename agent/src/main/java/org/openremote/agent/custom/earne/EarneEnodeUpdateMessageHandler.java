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

import org.openremote.agent.custom.earne.EarneEnodeUpdateMessage.AbstractAssetUpdate;
import org.openremote.agent.custom.earne.EarneEnodeUpdateMessage.ChargerCapabilities;
import org.openremote.agent.custom.earne.EarneEnodeUpdateMessage.ChargerChargeState;
import org.openremote.agent.custom.earne.EarneEnodeUpdateMessage.ChargerInformation;
import org.openremote.agent.custom.earne.EarneEnodeUpdateMessage.ChargerUpdate;
import org.openremote.agent.custom.earne.EarneEnodeUpdateMessage.HvacCapabilities;
import org.openremote.agent.custom.earne.EarneEnodeUpdateMessage.HvacInformation;
import org.openremote.agent.custom.earne.EarneEnodeUpdateMessage.HvacUpdate;
import org.openremote.agent.custom.earne.EarneEnodeUpdateMessage.VehicleCapabilities;
import org.openremote.agent.custom.earne.EarneEnodeUpdateMessage.VehicleChargeState;
import org.openremote.agent.custom.earne.EarneEnodeUpdateMessage.VehicleInformation;
import org.openremote.agent.custom.earne.EarneEnodeUpdateMessage.VehicleLocation;
import org.openremote.agent.custom.earne.EarneEnodeUpdateMessage.VehicleOdometer;
import org.openremote.agent.custom.earne.EarneEnodeUpdateMessage.VehicleSmartChargingPolicy;
import org.openremote.agent.custom.earne.EarneEnodeUpdateMessage.VehicleUpdate;
import org.openremote.agent.custom.ourgrid.OurgridChargerAsset;
import org.openremote.agent.custom.ourgrid.OurgridHvacAsset;
import org.openremote.agent.custom.ourgrid.OurgridMeterAsset;
import org.openremote.agent.custom.ourgrid.OurgridVehicleAsset;
import org.openremote.manager.asset.AssetStorageService;
import org.openremote.model.asset.Asset;
import org.openremote.model.asset.UserAssetLink;
import org.openremote.model.attribute.Attribute;
import org.openremote.model.protocol.ProtocolAssetService;
import org.openremote.model.query.AssetQuery;
import org.openremote.model.query.filter.RealmPredicate;

import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

import static org.openremote.agent.custom.earne.EarneMeterProtocol.LOG;

/**
 * Message handler for EARN-E Enode update messages.
 */
public class EarneEnodeUpdateMessageHandler extends AbstractEarneMessageHandler<EarneEnodeUpdateMessage> {

    public EarneEnodeUpdateMessageHandler(EarneEnodeAgent agent, ProtocolAssetService assetService, AssetStorageService assetStorageService) {
        super(EarneEnodeUpdateMessage.class, agent, assetService, assetStorageService);
    }

    protected void handleMessage(EarneEnodeUpdateMessage updateMessage) {
        AbstractAssetUpdate assetUpdate = updateMessage.getAssetUpdate();
        if (assetUpdate == null) {
            throw new IllegalArgumentException("Received EARN-E Enode update message without asset update: " + updateMessage);
        }

        String deviceId = assetUpdate.deviceId; // Asset name is unique EARN-E Enode device ID
        if (deviceId == null || deviceId.isBlank()) {
            throw new IllegalArgumentException("Received EARN-E Enode update message without deviceId: " + assetUpdate);
        }

        String assetId = generateAssetId(deviceId);

        switch (assetUpdate) {
            case ChargerUpdate chargerUpdate -> {
                OurgridChargerAsset chargerAsset = findOrCreateAsset(assetId, deviceId, updateMessage, () -> {
                    OurgridChargerAsset asset = new OurgridChargerAsset(deviceId);
                    asset.addOrReplaceAttributes(new Attribute<>(OurgridChargerAsset.ALLOW_AUTOMATIC_CONTROL_BUTTON, true));
                    return asset;
                });
                updateChargerAsset(assetId, deviceId, updateMessage, chargerAsset);
            }
            case HvacUpdate hvacUpdate -> {
                OurgridHvacAsset hvacAsset = findOrCreateAsset(assetId, deviceId, updateMessage, () -> {
                    OurgridHvacAsset asset = new OurgridHvacAsset(deviceId);
                    asset.addOrReplaceAttributes(new Attribute<>(OurgridHvacAsset.ALLOW_AUTOMATIC_CONTROL_BUTTON, true));
                    return asset;
                });
                updateHvacAsset(assetId, deviceId, updateMessage, hvacAsset);
            }
            case VehicleUpdate vehicleUpdate -> {
                OurgridVehicleAsset vehicleAsset = findOrCreateAsset(assetId, deviceId, updateMessage, () -> {
                    OurgridVehicleAsset asset = new OurgridVehicleAsset(deviceId);
                    asset.addOrReplaceAttributes(new Attribute<>(OurgridVehicleAsset.ALLOW_AUTOMATIC_CONTROL_BUTTON, true));
                    return asset;
                });
                updateVehicleAsset(assetId, deviceId, updateMessage, vehicleAsset);
            }
            default ->
                    throw new IllegalArgumentException("Received EARN-E Enode update message with unsupported asset update: " + assetUpdate);
        }
    }

    private OurgridMeterAsset findMeterAsset(String deviceId) {
        AssetQuery assetQuery = new AssetQuery()
                .select(new AssetQuery.Select().excludeAttributes())
                .realm(new RealmPredicate(agent.getRealm()))
                .types(OurgridMeterAsset.class)
                .attributeValue(OurgridMeterAsset.DEVICE_ID.getName(), deviceId);

        return (OurgridMeterAsset) assetService.findAssets(assetQuery)
                .stream()
                .findFirst().orElse(null);
    }

    private void copyParentUserAssetLinks(String parentId, String assetId) {
        if (parentId == null) {
            return;
        }

        List<UserAssetLink> parentUserAssetLinks = assetStorageService.findUserAssetLinks(agent.getRealm(), null, parentId);
        List<UserAssetLink> userAssetLinks = parentUserAssetLinks.stream().map(link -> new UserAssetLink(agent.getRealm(), link.getId().getUserId(), assetId)).toList();
        assetStorageService.storeUserAssetLinks(userAssetLinks);
    }

    private <T extends Asset<?>> T findOrCreateAsset(String assetId, String deviceId, EarneEnodeUpdateMessage updateMessage, Supplier<T> newAssetSupplier) {
        T existingAsset = assetService.findAsset(assetId);
        if (existingAsset != null) {
            return existingAsset;
        }

        OurgridMeterAsset meterAsset = findMeterAsset(updateMessage.getUserId());
        String parentId = meterAsset == null ? null : meterAsset.getId();

        T asset = newAssetSupplier.get();
        updateAssetIds(asset, assetId, deviceId, parentId);
        asset.setId(assetId);

        asset = assetService.mergeAsset(asset);

        if (meterAsset != null) {
            copyParentUserAssetLinks(parentId, assetId);
        }

        LOG.info(String.format("agentName='%s', agentId='%s'; Created %s: '%s'", agent.getName(), agent.getId(), asset.getAssetType(), deviceId));
        return asset;
    }

    private void updateChargerAsset(String assetId, String deviceId, EarneEnodeUpdateMessage updateMessage, OurgridChargerAsset chargerAsset) {
        ChargerUpdate charger = updateMessage.charger;
        ChargerCapabilities capabilities = charger.capabilities;

        ChargerInformation information = charger.information;
        if (capabilities.information.isCapable && information != null) {
            sendAttributeEvent(assetId, OurgridChargerAsset.MANUFACTURER, information.brand);
            sendAttributeEvent(assetId, OurgridChargerAsset.MODEL, information.model);
        } else {
            sendAttributeEvent(assetId, OurgridHvacAsset.MANUFACTURER, charger.vendor);
        }

        ChargerChargeState chargeState = charger.chargeState;
        if (capabilities.chargeState.isCapable && chargeState != null) {
            sendAttributeEvent(assetId, OurgridChargerAsset.CHARGING, chargeState.isCharging);
            sendAttributeEvent(assetId, OurgridChargerAsset.POWER, chargeState.chargeRate);
            sendAttributeEvent(assetId, OurgridChargerAsset.VEHICLE_CONNECTED, chargeState.isPluggedIn);
        }

        sendAttributeEvent(assetId, OurgridChargerAsset.START_CHARGING_CAPABLE, capabilities.startCharging.isCapable);
        sendAttributeEvent(assetId, OurgridChargerAsset.STOP_CHARGING_CAPABLE, capabilities.stopCharging.isCapable);
        sendAttributeEvent(assetId, OurgridChargerAsset.SUPPORTS_EXPORT, capabilities.startCharging.isCapable);

        sendAttributeEvent(assetId, OurgridChargerAsset.TIMESTAMP, updateMessage.createdAt.toString());

        LOG.finer(() -> String.format("agentName='%s', agentId='%s'; Updated Charger Asset: '%s'", agent.getName(), agent.getId(), deviceId));
    }

    private void updateHvacAsset(String assetId, String deviceId, EarneEnodeUpdateMessage updateMessage, OurgridHvacAsset hvacAsset) {
        HvacUpdate hvac = updateMessage.hvac;
        HvacCapabilities capabilities = hvac.capabilities;

        HvacInformation information = hvac.information;
        if (information != null) {
            sendAttributeEvent(assetId, OurgridHvacAsset.MANUFACTURER, information.brand);
            sendAttributeEvent(assetId, OurgridHvacAsset.MODEL, information.model);
        } else {
            sendAttributeEvent(assetId, OurgridHvacAsset.MANUFACTURER, hvac.vendor);
        }

        sendAttributeEvent(assetId, OurgridHvacAsset.TIMESTAMP, updateMessage.createdAt.toString());

        LOG.finer(() -> String.format("agentName='%s', agentId='%s'; Updated HVAC Asset: '%s'", agent.getName(), agent.getId(), deviceId));
    }

    private void updateVehicleAsset(String assetId, String deviceId, EarneEnodeUpdateMessage updateMessage, OurgridVehicleAsset vehicleAsset) {
        VehicleUpdate vehicle = updateMessage.vehicle;
        VehicleCapabilities capabilities = vehicle.capabilities;

        VehicleInformation information = vehicle.information;
        if (capabilities.information.isCapable && information != null) {
            sendAttributeEvent(assetId, OurgridVehicleAsset.MANUFACTURER, information.brand);
            sendAttributeEvent(assetId, OurgridVehicleAsset.MODEL, information.model);
            sendAttributeEvent(assetId, OurgridVehicleAsset.MODEL_YEAR, information.year);
        } else {
            sendAttributeEvent(assetId, OurgridVehicleAsset.MANUFACTURER, vehicle.vendor);
        }

        VehicleChargeState chargeState = vehicle.chargeState;
        if (capabilities.chargeState.isCapable && chargeState != null) {
            sendAttributeEvent(assetId, OurgridVehicleAsset.CHARGING, chargeState.isCharging);
            sendAttributeEvent(assetId, OurgridVehicleAsset.CHARGER_CONNECTED, chargeState.isPluggedIn);
            sendAttributeEvent(assetId, OurgridVehicleAsset.CHARGER_ID, chargeState.pluggedInChargerId);
            sendAttributeEvent(assetId, OurgridVehicleAsset.ENERGY_CAPACITY, chargeState.batteryCapacity);

            if (chargeState.batteryLevel != null) {
                sendAttributeEvent(assetId, OurgridVehicleAsset.ENERGY_LEVEL_PERCENTAGE, Math.toIntExact(chargeState.batteryLevel));
            }
            if (chargeState.chargeLimit != null) {
                sendAttributeEvent(assetId, OurgridVehicleAsset.CHARGE_LIMIT, Math.toIntExact(chargeState.chargeLimit));
            }
            if (chargeState.chargeRate != null) {
                sendAttributeEvent(assetId, OurgridVehicleAsset.POWER, Double.valueOf(chargeState.chargeRate));
            }
            if (chargeState.chargeTimeRemaining != null) {
                sendAttributeEvent(assetId, OurgridVehicleAsset.CHARGE_TIME_REMAINING, Math.toIntExact(chargeState.chargeTimeRemaining));
            }
            if (chargeState.range != null) {
                sendAttributeEvent(assetId, OurgridVehicleAsset.MILEAGE_CHARGED, Double.valueOf(chargeState.range));
            }
        }

        VehicleLocation location = vehicle.location;
        if (capabilities.location.isCapable && location != null) {
            if (updateAssetLocation(vehicleAsset, location.longitude, location.latitude)) {
                sendAttributeEvent(assetId, OurgridVehicleAsset.LOCATION, vehicleAsset.getLocation().orElse(null));
            }
        }

        VehicleOdometer odometer = vehicle.odometer;
        if (capabilities.odometer.isCapable && odometer != null) {
            sendAttributeEvent(assetId, OurgridVehicleAsset.ODOMETER, Math.toIntExact(odometer.distance));
        }

        VehicleSmartChargingPolicy smartChargingPolicy = vehicle.smartChargingPolicy;
        if (capabilities.smartCharging.isCapable && smartChargingPolicy != null) {
            sendAttributeEvent(assetId, OurgridVehicleAsset.SMART_CHARGING_ENABLED, smartChargingPolicy.isEnabled);
        }

        sendAttributeEvent(assetId, OurgridVehicleAsset.SUPPORTS_IMPORT, true);
        sendAttributeEvent(assetId, OurgridVehicleAsset.START_CHARGING_CAPABLE, capabilities.startCharging.isCapable);
        sendAttributeEvent(assetId, OurgridVehicleAsset.STOP_CHARGING_CAPABLE, capabilities.stopCharging.isCapable);

        sendAttributeEvent(assetId, OurgridVehicleAsset.TIMESTAMP, updateMessage.createdAt.toString());

        LOG.finer(() -> String.format("agentName='%s', agentId='%s'; Updated Vehicle Asset: '%s'", agent.getName(), agent.getId(), deviceId));
    }

}
