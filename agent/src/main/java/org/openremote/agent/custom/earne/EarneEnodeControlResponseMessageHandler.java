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

import static org.openremote.agent.custom.earne.EarneMeterProtocol.LOG;

import org.openremote.manager.asset.AssetStorageService;
import org.openremote.model.protocol.ProtocolAssetService;

/** Message handler for EARN-E Enode control response messages. */
public class EarneEnodeControlResponseMessageHandler
    extends AbstractEarneMessageHandler<EarneEnodeControlResponseMessage> {

  public EarneEnodeControlResponseMessageHandler(
      EarneEnodeAgent agent,
      ProtocolAssetService assetService,
      AssetStorageService assetStorageService) {
    super(EarneEnodeControlResponseMessage.class, agent, assetService, assetStorageService);
  }

  protected void handleMessage(EarneEnodeControlResponseMessage controlResponseMessage) {
    if (controlResponseMessage.isSuccess()) {
      LOG.info(
          String.format(
              "agentName='%s', agentId='%s'; Earn-E control command succeeded. Command: %s",
              agent.getName(), agent.getId(), controlResponseMessage.payload));
    } else {
      LOG.info(
          String.format(
              "agentName='%s', agentId='%s'; Earn-E control command failed. Reason: %s; Command: %s",
              agent.getName(),
              agent.getId(),
              controlResponseMessage.response,
              controlResponseMessage.payload));
    }
  }
}
