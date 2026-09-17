/*
 * Copyright 2026, OpenRemote Inc.
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
package org.openremote.manager.reschool.rest;

import org.openremote.container.timer.TimerService;
import org.openremote.manager.asset.AssetProcessingService;
import org.openremote.manager.asset.AssetStorageService;
import org.openremote.manager.datapoint.AssetDatapointService;
import org.openremote.manager.security.ManagerIdentityService;
import org.openremote.manager.web.ManagerWebService;
import org.openremote.model.Container;
import org.openremote.model.ContainerService;

public class ReschoolRestService implements ContainerService {

  protected ManagerIdentityService identityService;
  protected AssetDatapointService datapointService;
  protected AssetStorageService assetStorageService;
  protected AssetProcessingService assetProcessingService;

  @Override
  public void init(Container container) throws Exception {
    ManagerWebService webService = container.getService(ManagerWebService.class);
    TimerService timerService = container.getService(TimerService.class);
    identityService = container.getService(ManagerIdentityService.class);
    datapointService = container.getService(AssetDatapointService.class);
    assetStorageService = container.getService(AssetStorageService.class);
    assetProcessingService = container.getService(AssetProcessingService.class);

    webService.addApiSingleton(
        new DeviceChallengesResourceImpl(
            timerService, identityService, datapointService, assetStorageService));

    webService.addApiSingleton(
        new DeviceResourceImpl(timerService, identityService, assetStorageService));

    webService.addApiSingleton(
        new DeviceBatteryResourceImpl(
            timerService, identityService, assetStorageService, assetProcessingService));

    webService.addApiSingleton(
        new UserChallengesResourceImpl(timerService, identityService, assetStorageService));

    webService.addApiSingleton(
        new UserDistrictResourceImpl(timerService, identityService, assetStorageService));

    webService.addApiSingleton(
        new DeviceCharacteristicsResourceImpl(timerService, identityService, assetStorageService));

    webService.addApiSingleton(
        new UserRolesResourceImpl(timerService, identityService, assetStorageService));

    webService.addApiSingleton(
        new UserPeakPointsResourceImpl(timerService, identityService, assetStorageService));

    webService.addApiSingleton(
        new UserAccountResourceImpl(timerService, identityService, assetStorageService));
  }

  @Override
  public void start(Container container) throws Exception {}

  @Override
  public void stop(Container container) throws Exception {}
}
