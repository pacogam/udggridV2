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

import org.openremote.agent.custom.earne.EarneMeterMessage.GeoLocation;
import org.openremote.agent.custom.ourgrid.OurgridMeterAsset;
import org.openremote.manager.asset.AssetStorageService;
import org.openremote.model.attribute.Attribute;
import org.openremote.model.protocol.ProtocolAssetService;

import java.time.Duration;
import java.time.Instant;

import static org.openremote.agent.custom.earne.EarneMeterProtocol.LOG;

/**
 * Message handler for EARN-E meter messages.
 */
public class EarneMeterMessageHandler extends AbstractEarneMessageHandler<EarneMeterMessage> {

    private final EarneMeterAgent agent;

    public EarneMeterMessageHandler(EarneMeterAgent agent, ProtocolAssetService assetService, AssetStorageService assetStorageService) {
        super(EarneMeterMessage.class, agent, assetService, assetStorageService);
        this.agent = agent;
    }

    protected void handleMessage(EarneMeterMessage meterMessage) {
        String deviceId = meterMessage.deviceId; // Asset name is unique EARN-E device ID
        if (deviceId == null || deviceId.isBlank()) {
            throw new IllegalArgumentException("Received EARN-E Enode update message without deviceId: " + meterMessage);
        }

        String assetId = generateAssetId(deviceId);

        OurgridMeterAsset meterAsset = assetService.findAsset(assetId);
        if (meterAsset == null) {
            meterAsset = createMeterAsset(assetId, meterMessage);
        }
        updateMeterAsset(assetId, meterMessage, meterAsset);
    }

    private OurgridMeterAsset createMeterAsset(String assetId, EarneMeterMessage meterMessage) {
        String deviceId = meterMessage.deviceId;
        String parentId = agent.getMeterParentId().orElse(""); // Get parent ID

        OurgridMeterAsset meterAsset = new OurgridMeterAsset(deviceId); // Create new Meter Asset
        updateAssetIds(meterAsset, assetId, deviceId, parentId);

        GeoLocation geo = meterMessage.geo;
        if (geo != null) {
            updateAssetLocation(meterAsset, geo.latitude, geo.longitude);
        }

        meterAsset.setDeviceId(meterMessage.deviceId); // Unique Device ID will be used to connect between EARN-E and OpenRemote app
        meterAsset.setSmartmeterModel(meterMessage.model); // Smart meter model
        meterAsset.setSoftwareVersion(meterMessage.swVersion); // Software version of EARN-E device

        meterAsset = assetService.mergeAsset(meterAsset); // Merge asset into database
        LOG.info(String.format("agentName='%s', agentId='%s'; Created Meter Asset: %s", agent.getName(), agent.getId(), deviceId));
        return meterAsset;
    }

    private void updateMeterAsset(String assetId, EarneMeterMessage meterMessage, OurgridMeterAsset meterAsset) {
        String deviceId = meterMessage.deviceId;

        Double energyDeliveredTariff1 = meterMessage.energyDeliveredTariff1;
        Double energyDeliveredTariff2 = meterMessage.energyDeliveredTariff2;
        Double energyReturnedTariff1 = meterMessage.energyReturnedTariff1;
        Double energyReturnedTariff2 = meterMessage.energyReturnedTariff2;
        Double powerDelivered = meterMessage.powerDelivered;
        Double powerReturned = meterMessage.powerReturned;
        Double gasDelivered = meterMessage.gasDelivered;
        Double wifiRssi = meterMessage.wifiRSSI;

        Double energyImported = null;
        if (energyDeliveredTariff1 != null && energyDeliveredTariff2 != null) {
            energyImported = Math.round((energyDeliveredTariff1 + energyDeliveredTariff2) * 1000.0) / 1000.0; // E_imported_current, rounded to 3 decimals
        }

        Double energyExported = null;
        if (energyReturnedTariff1 != null && energyReturnedTariff2 != null) {
            energyExported = Math.round((energyReturnedTariff1 + energyReturnedTariff2) * 1000.0) / 1000.0; // E_exported_current, rounded to 3 decimals
        }

        Double energyNet = null;
        if (energyImported != null && energyExported != null) {
            energyNet = Math.round((energyImported - energyExported) * 1000.0) / 1000.0; // dE_current, rounded to 3 decimals
        }

        Double power = null;
        if (powerDelivered != null && powerReturned != null) {
            power = 1000 * (powerDelivered - powerReturned); // Direct power readout
        }

        Instant timestamp = meterMessage.timestamp;

        // Calculate gas flow rate
        String timestampPrevious = meterAsset.getAttribute(OurgridMeterAsset.TIMESTAMP).flatMap(Attribute::getValue).orElse(null);

        if (timestamp != null && timestampPrevious != null) {
            long dt = Duration.between(Instant.parse(timestampPrevious), timestamp).toSeconds();
            if (dt <= 10) {
                LOG.info(String.format("agentName='%s', agentId='%s'; Meter assetName='%s'; Time between messages < 10 seconds: dt='%s'", agent.getName(), agent.getId(), meterAsset.getName(), dt));
            }

            Double gasDeliveredPrevious = meterAsset.getAttribute(OurgridMeterAsset.GAS_IMPORT_TOTAL).flatMap(Attribute::getValue).orElse(null);

            if (dt > 0 && gasDelivered != null && gasDeliveredPrevious != null) {
                double dGas = gasDelivered - gasDeliveredPrevious;
                double gasFlowRate = Math.round(((dGas / dt) * 60) * 1000.0) / 1000.0; // Gas flow rate (m3/min), rounded to 3 decimals

                if (dGas >= 0) {
                    sendAttributeEvent(assetId, OurgridMeterAsset.GAS_FLOW_RATE, gasFlowRate);
                } else {
                    LOG.info(String.format("agentName='%s', agentId='%s'; Meter assetName='%s'; Gas-meter reset, negative dGas: '%s'", agent.getName(), agent.getId(), meterAsset.getName(), dGas));
                }
            }
        }

        String model = meterMessage.model;
        String modelPrevious = meterAsset.getAttribute(OurgridMeterAsset.SMARTMETER_MODEL).flatMap(Attribute::getValue).orElse("");
        if (!model.isBlank() && !modelPrevious.equals(model)) {
            sendAttributeEvent(assetId, OurgridMeterAsset.SMARTMETER_MODEL, model);
            LOG.info(String.format("agentName='%s', agentId='%s'; Meter Asset='%s'; Smart-meter model changed to: '%s'", agent.getName(), agent.getId(), meterAsset.getName(), model));
        }

        String swVersion = meterMessage.swVersion;
        String swVersionPrevious = meterAsset.getAttribute(OurgridMeterAsset.SOFTWARE_VERSION).flatMap(Attribute::getValue).orElse("");
        if (!swVersion.isBlank() && !swVersionPrevious.equals(meterMessage.swVersion)) {
            sendAttributeEvent(assetId, OurgridMeterAsset.SOFTWARE_VERSION, swVersion);
            LOG.info(String.format("agentName='%s', agentId='%s'; Meter Asset='%s'; EARN-E device software version update: '%s'", agent.getName(), agent.getId(), meterAsset.getName(), swVersion));
        }

        if (timestamp == null) {
            LOG.info(String.format("agentName='%s', agentId='%s'; Meter Asset='%s'; Missing timestamp in message, uniqueDeviceName: '%s', assetId: '%s'", agent.getName(), agent.getId(), meterAsset.getName(), deviceId, assetId));
        }

        sendAttributeEvent(assetId, OurgridMeterAsset.TIMESTAMP, timestamp == null ? "" : timestamp.toString());

        sendAttributeEvent(assetId, OurgridMeterAsset.ENERGY_EXPORT_TOTAL, energyExported);
        sendAttributeEvent(assetId, OurgridMeterAsset.ENERGY_IMPORT_TOTAL, energyImported);
        sendAttributeEvent(assetId, OurgridMeterAsset.ENERGY_NET_TOTAL, energyNet);
        sendAttributeEvent(assetId, OurgridMeterAsset.GAS_IMPORT_TOTAL, gasDelivered);
        sendAttributeEvent(assetId, OurgridMeterAsset.POWER, power);
        sendAttributeEvent(assetId, OurgridMeterAsset.POWER_IMPORT, powerDelivered);
        sendAttributeEvent(assetId, OurgridMeterAsset.POWER_EXPORT, powerReturned);
        sendAttributeEvent(assetId, OurgridMeterAsset.WIFI_SIGNAL, wifiRssi);

        LOG.finer(() -> String.format("agentName='%s', agentId='%s'; Updated Meter Asset: '%s'", agent.getName(), agent.getId(), deviceId));
    }

}
