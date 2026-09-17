/*
 * Copyright 2025, OpenRemote Inc.
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
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package org.openremote.agent.custom.earne;

import static java.util.Map.entry;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.openremote.agent.custom.ourgrid.OurgridChargerAsset;
import org.openremote.agent.custom.ourgrid.OurgridHvacAsset;
import org.openremote.agent.custom.ourgrid.OurgridMeterAsset;
import org.openremote.agent.custom.ourgrid.OurgridVehicleAsset;
import org.openremote.model.asset.Asset;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class EarneEnodeUpdateMessageHandlerTest extends AbstractEarneMessageHandlerTest {

  static final String AGENT_ASSET_ID = "m1g1bR4qCcLyxnjUSnSMjt";
  static final String CHARGER_ASSET_ID = "30oeNuiuGG4t82lDXqNDhf";
  static final String HVAC_ASSET_ID = "5umPTtEk1q2CIoKX6i43Fd";
  static final String METER_ASSET_ID = "6RnyhhSZF9q1s9sNUdffnX";
  static final String VEHICLE_ASSET_ID = "3NuiVWvK7m7b7C6SytyMbd";

  OurgridMeterAsset meterAsset = new OurgridMeterAsset("meterAsset");

  @Mock EarneEnodeAgent agentMock;

  @Mock EarneEnodeProtocol protocolMock;

  @Captor ArgumentCaptor<Asset<OurgridChargerAsset>> chargerAssetCapture;

  @Captor ArgumentCaptor<Asset<OurgridHvacAsset>> hvacAssetCapture;

  @Captor ArgumentCaptor<Asset<OurgridVehicleAsset>> vehicleAssetCapture;

  EarneEnodeUpdateMessageHandler handler;

  @BeforeEach
  void beforeEach() {
    meterAsset.setId(METER_ASSET_ID);

    when(agentMock.getName()).thenReturn("Earn-E Enode Agent Mock");
    when(agentMock.getId()).thenReturn(AGENT_ASSET_ID);
    when(agentMock.getProtocolInstance()).thenReturn(protocolMock);

    handler =
        new EarneEnodeUpdateMessageHandler(agentMock, assetServiceMock, assetStorageServiceMock);

    meterAsset.setId(METER_ASSET_ID);
  }

  @Override
  String getTestDataPathPrefix() {
    return "earne/enode/";
  }

  @Test
  void testNullMessage() {
    assertThrows(IllegalArgumentException.class, () -> handler.handleMessage((String) null));
  }

  @Test
  void testEmptyMessage() {
    assertThrows(IllegalArgumentException.class, () -> handler.handleMessage(""));
    assertThrows(IllegalStateException.class, () -> handler.handleMessage("{}"));

    verify(protocolMock, never()).sendAttributeEvent(any(), any(), any());
    verify(assetServiceMock, never()).mergeAsset(any(Asset.class));
  }

  private void assertNewChargerCreated(Asset<OurgridChargerAsset> asset, String parentId) {
    String deviceId = "2211e263-0362-4235-83f4-887bdf3ee414";

    assertEquals(CHARGER_ASSET_ID, asset.getId());
    assertEquals(deviceId, asset.getAssetName());
    assertEquals(parentId, asset.getParentId());

    assertAssetAttributeValues(
        asset,
        Map.ofEntries(
            entry(OurgridChargerAsset.LOCATION, Optional.empty()),
            entry(OurgridChargerAsset.DEVICE_ID, deviceId)));
  }

  private void verifyChargerAttributeEventsSend() {
    verifyAttributeEventSend(CHARGER_ASSET_ID, OurgridChargerAsset.MANUFACTURER, "Zaptec");
    verifyAttributeEventSend(CHARGER_ASSET_ID, OurgridChargerAsset.MODEL, "Pro");
    verifyAttributeEventSend(CHARGER_ASSET_ID, OurgridChargerAsset.CHARGING, true);
    verifyAttributeEventSend(CHARGER_ASSET_ID, OurgridChargerAsset.POWER, 6.939d);
    verifyAttributeEventSend(CHARGER_ASSET_ID, OurgridChargerAsset.VEHICLE_CONNECTED, true);
    verifyAttributeEventSend(CHARGER_ASSET_ID, OurgridChargerAsset.START_CHARGING_CAPABLE, true);
    verifyAttributeEventSend(CHARGER_ASSET_ID, OurgridChargerAsset.STOP_CHARGING_CAPABLE, true);
    verifyAttributeEventSend(CHARGER_ASSET_ID, OurgridChargerAsset.SUPPORTS_EXPORT, true);
    verifyAttributeEventSend(
        CHARGER_ASSET_ID, OurgridChargerAsset.TIMESTAMP, "2020-04-07T17:04:26Z");
  }

  @Test
  void testExistingChargerAssetUpdatedUsingCompleteMessage() throws IOException {
    OurgridChargerAsset existingAsset = new OurgridChargerAsset("testName");
    existingAsset.getAttributes().setValue(OurgridChargerAsset.TIMESTAMP, "2025-05-30T14:00:00Z");

    when(assetServiceMock.findAssets(any())).thenReturn(List.of(meterAsset));
    when(assetServiceMock.findAsset(CHARGER_ASSET_ID)).thenReturn(existingAsset);

    handler.handleMessage(readFile("charger-message.json"));

    verify(assetServiceMock, times(1)).findAssets(any());
    verify(assetServiceMock, never()).mergeAsset(any(Asset.class));

    verifyChargerAttributeEventsSend();
  }

  @Test
  void testNewChargerAssetCreatedUsingCompleteMessageWithMeterParent() throws IOException {
    when(assetServiceMock.findAssets(any())).thenReturn(List.of(meterAsset));
    when(assetServiceMock.findAsset(CHARGER_ASSET_ID)).thenReturn(null);
    when(assetServiceMock.findAsset(METER_ASSET_ID)).thenReturn(meterAsset);
    when(assetServiceMock.mergeAsset(any(Asset.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    handler.handleMessage(readFile("charger-message.json"));

    verify(assetServiceMock, times(1)).findAssets(any());
    verify(assetServiceMock, times(1)).findAsset(CHARGER_ASSET_ID);
    verify(assetServiceMock, times(1)).findAsset(METER_ASSET_ID);
    verify(assetServiceMock).mergeAsset(chargerAssetCapture.capture());
    verify(assetServiceMock, times(1)).mergeAsset(any(Asset.class));

    Asset<OurgridChargerAsset> asset = chargerAssetCapture.getValue();
    assertNewChargerCreated(asset, METER_ASSET_ID);

    verifyChargerAttributeEventsSend();
  }

  @Test
  void testNewChargerAssetCreatedUsingCompleteMessageWithoutMeterParent() throws IOException {
    when(assetServiceMock.findAssets(any())).thenReturn(List.of());

    handler.handleMessage(readFile("charger-message.json"));

    verify(assetServiceMock, times(1)).findAssets(any());
    verify(assetServiceMock, never()).findAsset(CHARGER_ASSET_ID);
    verify(assetServiceMock, never()).mergeAsset(any(Asset.class));
    verify(assetServiceMock, never()).sendAttributeEvent(any());
  }

  @Test
  void testNewChargerAssetCreatedUsingMessageWithUnknownProperties() throws IOException {
    when(assetServiceMock.findAssets(any())).thenReturn(List.of(meterAsset));
    when(assetServiceMock.findAsset(CHARGER_ASSET_ID)).thenReturn(null);
    when(assetServiceMock.findAsset(METER_ASSET_ID)).thenReturn(meterAsset);
    when(assetServiceMock.mergeAsset(any(Asset.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    handler.handleMessage(readFile("charger-message-unknowns.json"));

    verify(assetServiceMock, times(1)).findAssets(any());
    verify(assetServiceMock, times(1)).findAsset(CHARGER_ASSET_ID);
    verify(assetServiceMock, times(1)).findAsset(METER_ASSET_ID);
    verify(assetServiceMock).mergeAsset(chargerAssetCapture.capture());
    verify(assetServiceMock, times(1)).mergeAsset(any(Asset.class));

    Asset<OurgridChargerAsset> asset = chargerAssetCapture.getValue();
    assertNewChargerCreated(asset, METER_ASSET_ID);

    verifyChargerAttributeEventsSend();
  }

  private void assertNewHvacCreated(Asset<OurgridHvacAsset> asset, String parentId) {
    String deviceId = "8f39fa8d-8f10-4984-a319-741dc23848c0";

    assertEquals(HVAC_ASSET_ID, asset.getId());
    assertEquals(deviceId, asset.getAssetName());
    assertEquals(parentId, asset.getParentId());

    assertAssetAttributeValues(
        asset,
        Map.ofEntries(
            entry(OurgridHvacAsset.DEVICE_ID, deviceId),
            entry(OurgridHvacAsset.LOCATION, Optional.empty())));
  }

  private void verifyHvacAttributeEventsSend() {
    verifyAttributeEventSend(HVAC_ASSET_ID, OurgridHvacAsset.MANUFACTURER, "ADAX");
    verifyAttributeEventSend(HVAC_ASSET_ID, OurgridHvacAsset.MODEL, "Neo Wi-Fi Skirting");
    verifyAttributeEventSend(HVAC_ASSET_ID, OurgridHvacAsset.TIMESTAMP, "2020-04-07T17:04:26Z");
  }

  @Test
  void testExistingHvacAssetUpdatedUsingCompleteMessage() throws IOException {
    OurgridHvacAsset existingAsset = new OurgridHvacAsset("testName");
    existingAsset.getAttributes().setValue(OurgridHvacAsset.TIMESTAMP, "2025-05-30T14:00:00Z");

    when(assetServiceMock.findAssets(any())).thenReturn(List.of(meterAsset));
    when(assetServiceMock.findAsset(HVAC_ASSET_ID)).thenReturn(existingAsset);

    handler.handleMessage(readFile("hvac-message.json"));

    verify(assetServiceMock, times(1)).findAssets(any());
    verify(assetServiceMock, never()).mergeAsset(any(Asset.class));

    verifyHvacAttributeEventsSend();
  }

  @Test
  void testNewHvacAssetCreatedUsingCompleteMessageWithMeterParent() throws IOException {
    when(assetServiceMock.findAssets(any())).thenReturn(List.of(meterAsset));
    when(assetServiceMock.findAsset(METER_ASSET_ID)).thenReturn(meterAsset);
    when(assetServiceMock.findAsset(HVAC_ASSET_ID)).thenReturn(null);
    when(assetServiceMock.mergeAsset(any(Asset.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    handler.handleMessage(readFile("hvac-message.json"));

    verify(assetServiceMock, times(1)).findAsset(METER_ASSET_ID);
    verify(assetServiceMock, times(1)).findAsset(HVAC_ASSET_ID);
    verify(assetServiceMock, times(1)).findAssets(any());
    verify(assetServiceMock).mergeAsset(hvacAssetCapture.capture());
    verify(assetServiceMock, times(1)).mergeAsset(any(Asset.class));

    Asset<OurgridHvacAsset> asset = hvacAssetCapture.getValue();
    assertNewHvacCreated(asset, METER_ASSET_ID);

    verifyHvacAttributeEventsSend();
  }

  @Test
  void testNewHvacAssetCreatedUsingCompleteMessageWithoutMeterParent() throws IOException {
    when(assetServiceMock.findAssets(any())).thenReturn(List.of());

    handler.handleMessage(readFile("hvac-message.json"));

    verify(assetServiceMock, times(1)).findAssets(any());
    verify(assetServiceMock, never()).findAsset(HVAC_ASSET_ID);
    verify(assetServiceMock, never()).mergeAsset(any(Asset.class));
    verify(assetServiceMock, never()).sendAttributeEvent(any());
  }

  @Test
  void testNewHvacAssetCreatedUsingMessageWithUnknownProperties() throws IOException {
    when(assetServiceMock.findAssets(any())).thenReturn(List.of(meterAsset));
    when(assetServiceMock.findAsset(HVAC_ASSET_ID)).thenReturn(null);
    when(assetServiceMock.findAsset(METER_ASSET_ID)).thenReturn(meterAsset);
    when(assetServiceMock.mergeAsset(any(Asset.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    handler.handleMessage(readFile("hvac-message-unknowns.json"));

    verify(assetServiceMock, times(1)).findAssets(any());
    verify(assetServiceMock, times(1)).findAsset(HVAC_ASSET_ID);
    verify(assetServiceMock, times(1)).findAsset(METER_ASSET_ID);
    verify(assetServiceMock).mergeAsset(hvacAssetCapture.capture());
    verify(assetServiceMock, times(1)).mergeAsset(any(Asset.class));

    Asset<OurgridHvacAsset> asset = hvacAssetCapture.getValue();
    assertNewHvacCreated(asset, METER_ASSET_ID);

    verifyHvacAttributeEventsSend();
  }

  private void assertNewVehicleCreated(Asset<OurgridVehicleAsset> asset, String parentId) {
    String deviceId = "5043a3ea-7aec-440b-96fc-622e2d6b2485";

    assertEquals(VEHICLE_ASSET_ID, asset.getId());
    assertEquals(deviceId, asset.getAssetName());
    assertEquals(parentId, asset.getParentId());

    assertAssetAttributeValues(
        asset,
        Map.ofEntries(
            entry(OurgridVehicleAsset.DEVICE_ID, deviceId),
            entry(OurgridVehicleAsset.LOCATION, Optional.empty())));
  }

  private void verifyVehicleAttributeEventsSend() {
    verifyAttributeEventSend(VEHICLE_ASSET_ID, OurgridVehicleAsset.MANUFACTURER, "Tesla");
    verifyAttributeEventSend(VEHICLE_ASSET_ID, OurgridVehicleAsset.MODEL, "Model Y");
    verifyAttributeEventSend(VEHICLE_ASSET_ID, OurgridVehicleAsset.MODEL_YEAR, 2023);
    verifyAttributeEventSend(VEHICLE_ASSET_ID, OurgridVehicleAsset.CHARGING, false);
    verifyAttributeEventSend(VEHICLE_ASSET_ID, OurgridVehicleAsset.CHARGER_CONNECTED, false);
    verifyAttributeEventSend(VEHICLE_ASSET_ID, OurgridVehicleAsset.ENERGY_CAPACITY, 70.79d);
    verifyAttributeEventSend(VEHICLE_ASSET_ID, OurgridVehicleAsset.ENERGY_LEVEL_PERCENTAGE, 65);
    verifyAttributeEventSend(VEHICLE_ASSET_ID, OurgridVehicleAsset.CHARGE_LIMIT, 80);
    verifyAttributeEventSend(VEHICLE_ASSET_ID, OurgridVehicleAsset.MILEAGE_CHARGED, 333.0d);
    verifyAttributeEventSend(VEHICLE_ASSET_ID, OurgridVehicleAsset.ODOMETER, 29838);
    verifyAttributeEventSend(VEHICLE_ASSET_ID, OurgridVehicleAsset.SMART_CHARGING_ENABLED, false);
    verifyAttributeEventSend(VEHICLE_ASSET_ID, OurgridVehicleAsset.SUPPORTS_IMPORT, true);
    verifyAttributeEventSend(VEHICLE_ASSET_ID, OurgridVehicleAsset.START_CHARGING_CAPABLE, true);
    verifyAttributeEventSend(VEHICLE_ASSET_ID, OurgridVehicleAsset.STOP_CHARGING_CAPABLE, true);
    verifyAttributeEventSend(
        VEHICLE_ASSET_ID, OurgridVehicleAsset.TIMESTAMP, "2025-08-05T15:29:55.298Z");
  }

  @Test
  void testExistingVehicleAssetUpdatedUsingCompleteMessage() throws IOException {
    OurgridVehicleAsset existingAsset = new OurgridVehicleAsset("testName");
    existingAsset.getAttributes().setValue(OurgridVehicleAsset.TIMESTAMP, "2025-05-30T14:00:00Z");

    when(assetServiceMock.findAssets(any())).thenReturn(List.of(meterAsset));
    when(assetServiceMock.findAsset(VEHICLE_ASSET_ID)).thenReturn(existingAsset);

    handler.handleMessage(readFile("vehicle-message.json"));

    verify(assetServiceMock, times(1)).findAssets(any());
    verify(assetServiceMock, never()).mergeAsset(any(Asset.class));

    verifyVehicleAttributeEventsSend();
  }

  @Test
  void testNewVehicleAssetCreatedUsingCompleteMessageWithMeterParent() throws IOException {
    when(assetServiceMock.findAssets(any())).thenReturn(List.of(meterAsset));
    when(assetServiceMock.findAsset(VEHICLE_ASSET_ID)).thenReturn(null);
    when(assetServiceMock.findAsset(METER_ASSET_ID)).thenReturn(meterAsset);
    when(assetServiceMock.mergeAsset(any(Asset.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    handler.handleMessage(readFile("vehicle-message.json"));

    verify(assetServiceMock, times(1)).findAssets(any());
    verify(assetServiceMock, times(1)).findAsset(VEHICLE_ASSET_ID);
    verify(assetServiceMock, times(1)).findAsset(METER_ASSET_ID);
    verify(assetServiceMock).mergeAsset(vehicleAssetCapture.capture());
    verify(assetServiceMock, times(1)).mergeAsset(any(Asset.class));

    Asset<OurgridVehicleAsset> asset = vehicleAssetCapture.getValue();
    assertNewVehicleCreated(asset, METER_ASSET_ID);

    verifyVehicleAttributeEventsSend();
  }

  @Test
  void testNewVehicleAssetCreatedUsingCompleteMessageWithoutMeterParent() throws IOException {
    when(assetServiceMock.findAssets(any())).thenReturn(List.of());

    handler.handleMessage(readFile("vehicle-message.json"));

    verify(assetServiceMock, times(1)).findAssets(any());
    verify(assetServiceMock, never()).findAsset(VEHICLE_ASSET_ID);
    verify(assetServiceMock, never()).mergeAsset(any(Asset.class));
    verify(assetServiceMock, never()).sendAttributeEvent(any());
  }

  @Test
  void testNewVehicleAssetCreatedUsingMessageWithUnknownProperties() throws IOException {
    when(assetServiceMock.findAssets(any())).thenReturn(List.of(meterAsset));
    when(assetServiceMock.findAsset(VEHICLE_ASSET_ID)).thenReturn(null);
    when(assetServiceMock.findAsset(METER_ASSET_ID)).thenReturn(meterAsset);
    when(assetServiceMock.mergeAsset(any(Asset.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    handler.handleMessage(readFile("vehicle-message-unknowns.json"));

    verify(assetServiceMock, times(1)).findAssets(any());
    verify(assetServiceMock, times(1)).findAsset(VEHICLE_ASSET_ID);
    verify(assetServiceMock, times(1)).findAsset(METER_ASSET_ID);
    verify(assetServiceMock).mergeAsset(vehicleAssetCapture.capture());
    verify(assetServiceMock, times(1)).mergeAsset(any(Asset.class));

    Asset<OurgridVehicleAsset> asset = vehicleAssetCapture.getValue();
    assertNewVehicleCreated(asset, METER_ASSET_ID);

    verifyVehicleAttributeEventsSend();
  }
}
