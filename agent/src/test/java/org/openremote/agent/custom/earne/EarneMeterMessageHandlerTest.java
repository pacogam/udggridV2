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
import org.openremote.agent.custom.ourgrid.OurgridMeterAsset;
import org.openremote.model.asset.Asset;
import org.openremote.model.asset.impl.BuildingAsset;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class EarneMeterMessageHandlerTest extends AbstractEarneMessageHandlerTest {

  static final String AGENT_ASSET_ID = "m1g1bR4qCcLyxnjUSnSMjt";
  static final String METER_ASSET_ID = "6RnyhhSZF9q1s9sNUdffnX";
  static final String METER_PARENT_ID = "jJl5pILlJtXjUcmI3J6oh0";

  @Mock EarneMeterAgent agentMock;

  @Mock EarneMeterProtocol protocolMock;

  @Captor ArgumentCaptor<Asset<OurgridMeterAsset>> assetCapture;

  EarneMeterMessageHandler handler;

  @BeforeEach
  void beforeEach() {
    when(agentMock.getName()).thenReturn("Earn-E Meter Agent Mock");
    when(agentMock.getId()).thenReturn(AGENT_ASSET_ID);
    when(agentMock.getProtocolInstance()).thenReturn(protocolMock);

    handler = new EarneMeterMessageHandler(agentMock, assetServiceMock, assetStorageServiceMock);
  }

  @Override
  String getTestDataPathPrefix() {
    return "earne/meter/";
  }

  void assertNewMeterCreated(Asset<OurgridMeterAsset> asset, String parentId) {
    String deviceId = "11111111-aaaa-2222-bbbb-33333333";

    assertEquals(METER_ASSET_ID, asset.getId());
    assertEquals(deviceId, asset.getAssetName());
    assertEquals(OurgridMeterAsset.class, asset.getAssetClass());
    assertEquals(parentId, asset.getParentId());

    assertAssetAttributeValues(
        asset,
        Map.ofEntries(
            entry(OurgridMeterAsset.DEVICE_ID, deviceId),
            entry(OurgridMeterAsset.LOCATION, Optional.empty()),
            entry(OurgridMeterAsset.SMARTMETER_MODEL, "XMX5LGBBLA1231231231"),
            entry(OurgridMeterAsset.SOFTWARE_VERSION, "228")));
  }

  private void verifyNewMeterAttributeEventsSend() {
    verifyAttributeEventSend(METER_ASSET_ID, OurgridMeterAsset.ENERGY_EXPORT_TOTAL, 21623.667d);
    verifyAttributeEventSend(METER_ASSET_ID, OurgridMeterAsset.ENERGY_IMPORT_TOTAL, 3580.247d);
    verifyAttributeEventSend(METER_ASSET_ID, OurgridMeterAsset.ENERGY_NET_TOTAL, -18043.42d);
    verifyAttributeEventSend(METER_ASSET_ID, OurgridMeterAsset.GAS_IMPORT_TOTAL, 1010.10101d);
    verifyAttributeEventSend(METER_ASSET_ID, OurgridMeterAsset.POWER, -3434.343434d);
    verifyAttributeEventSend(METER_ASSET_ID, OurgridMeterAsset.POWER_EXPORT, 3.434343434d);
    verifyAttributeEventSend(METER_ASSET_ID, OurgridMeterAsset.POWER_IMPORT, 0.0d);
    verifyAttributeEventSend(METER_ASSET_ID, OurgridMeterAsset.TIMESTAMP, "2025-05-30T14:33:17Z");
    verifyAttributeEventSend(METER_ASSET_ID, OurgridMeterAsset.WIFI_SIGNAL, -74.0d);

    verifyAttributeEventNotSend(METER_ASSET_ID, OurgridMeterAsset.GAS_FLOW_RATE);
    verifyAttributeEventNotSend(METER_ASSET_ID, OurgridMeterAsset.SMARTMETER_MODEL);
    verifyAttributeEventNotSend(METER_ASSET_ID, OurgridMeterAsset.SOFTWARE_VERSION);
  }

  @Test
  void testNullMessage() {
    assertThrows(IllegalArgumentException.class, () -> handler.handleMessage((String) null));
  }

  @Test
  void testEmptyMessage() {
    assertThrows(IllegalArgumentException.class, () -> handler.handleMessage(""));
    assertThrows(IllegalArgumentException.class, () -> handler.handleMessage("{}"));

    verify(protocolMock, never()).sendAttributeEvent(any(), any(), any());
    verify(assetServiceMock, never()).mergeAsset(any(Asset.class));
  }

  @Test
  void testExistingMeterAssetUpdatedUsingCompleteMessage() throws IOException {
    OurgridMeterAsset existingAsset = new OurgridMeterAsset("testName");
    existingAsset.getAttributes().setValue(OurgridMeterAsset.TIMESTAMP, "2025-05-30T14:00:00Z");
    existingAsset.getAttributes().setValue(OurgridMeterAsset.GAS_IMPORT_TOTAL, 1000.0d);

    when(assetServiceMock.findAsset(METER_ASSET_ID)).thenReturn(existingAsset);

    handler.handleMessage(readFile("meter-message.json"));

    verify(assetServiceMock, never()).mergeAsset(any(Asset.class));

    verifyAttributeEventSend(METER_ASSET_ID, OurgridMeterAsset.ENERGY_EXPORT_TOTAL, 21623.667d);
    verifyAttributeEventSend(METER_ASSET_ID, OurgridMeterAsset.ENERGY_IMPORT_TOTAL, 3580.247d);
    verifyAttributeEventSend(METER_ASSET_ID, OurgridMeterAsset.ENERGY_NET_TOTAL, -18043.42d);
    verifyAttributeEventSend(METER_ASSET_ID, OurgridMeterAsset.GAS_FLOW_RATE, 0.303d);
    verifyAttributeEventSend(METER_ASSET_ID, OurgridMeterAsset.GAS_IMPORT_TOTAL, 1010.10101d);
    verifyAttributeEventSend(METER_ASSET_ID, OurgridMeterAsset.POWER, -3434.343434d);
    verifyAttributeEventSend(METER_ASSET_ID, OurgridMeterAsset.POWER_EXPORT, 3.434343434d);
    verifyAttributeEventSend(METER_ASSET_ID, OurgridMeterAsset.POWER_IMPORT, 0.0d);
    verifyAttributeEventSend(
        METER_ASSET_ID, OurgridMeterAsset.SMARTMETER_MODEL, "XMX5LGBBLA1231231231");
    verifyAttributeEventSend(METER_ASSET_ID, OurgridMeterAsset.SOFTWARE_VERSION, "228");
    verifyAttributeEventSend(METER_ASSET_ID, OurgridMeterAsset.TIMESTAMP, "2025-05-30T14:33:17Z");
    verifyAttributeEventSend(METER_ASSET_ID, OurgridMeterAsset.WIFI_SIGNAL, -74.0d);
  }

  @Test
  void testNewMeterAssetCreatedUsingCompleteMessageWithMeterParent() throws IOException {
    when(assetServiceMock.findAsset(METER_ASSET_ID)).thenReturn(null);
    when(agentMock.getMeterParentId()).thenReturn(Optional.of(METER_PARENT_ID));
    when(assetServiceMock.findAsset(METER_PARENT_ID)).thenReturn(new BuildingAsset("Office"));
    when(assetServiceMock.mergeAsset(any(Asset.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    handler.handleMessage(readFile("meter-message.json"));

    verify(assetServiceMock).mergeAsset(assetCapture.capture());
    verify(assetServiceMock, times(1)).mergeAsset(any(Asset.class));

    Asset<OurgridMeterAsset> asset = assetCapture.getValue();
    assertNewMeterCreated(asset, METER_PARENT_ID);

    verifyNewMeterAttributeEventsSend();
  }

  @Test
  void testNewMeterAssetCreatedUsingCompleteMessageWithoutMeterParent() throws IOException {
    when(assetServiceMock.findAsset(METER_ASSET_ID)).thenReturn(null);
    when(assetServiceMock.mergeAsset(any(Asset.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    handler.handleMessage(readFile("meter-message.json"));

    verify(assetServiceMock).mergeAsset(assetCapture.capture());
    verify(assetServiceMock, times(1)).mergeAsset(any(Asset.class));

    Asset<OurgridMeterAsset> asset = assetCapture.getValue();
    assertNewMeterCreated(asset, AGENT_ASSET_ID);

    verifyNewMeterAttributeEventsSend();
  }

  @Test
  void testNewMeterAssetCreatedUsingMessageWithUnknownProperties() throws IOException {
    when(assetServiceMock.findAsset(METER_ASSET_ID)).thenReturn(null);
    when(assetServiceMock.mergeAsset(any(Asset.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    handler.handleMessage(readFile("meter-message-unknowns.json"));

    verify(assetServiceMock).mergeAsset(assetCapture.capture());
    verify(assetServiceMock, times(1)).mergeAsset(any(Asset.class));

    Asset<OurgridMeterAsset> asset = assetCapture.getValue();
    assertNewMeterCreated(asset, AGENT_ASSET_ID);

    verifyNewMeterAttributeEventsSend();
  }
}
