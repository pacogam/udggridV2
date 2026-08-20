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

import static jakarta.ws.rs.core.Response.Status.*;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import java.util.Optional;
import org.openremote.agent.custom.ourgrid.OurgridMeterAsset;
import org.openremote.container.timer.TimerService;
import org.openremote.manager.asset.AssetStorageService;
import org.openremote.manager.security.ManagerIdentityService;
import org.openremote.manager.web.ManagerWebResource;
import org.openremote.model.http.RequestParams;
import org.openremote.model.query.AssetQuery;
import org.openremote.model.query.filter.RealmPredicate;
import org.openremote.model.reschool.DeviceCharacteristic;
import org.openremote.model.reschool.DeviceCharacteristicsResource;

public class DeviceCharacteristicsResourceImpl extends ManagerWebResource
    implements DeviceCharacteristicsResource {

  protected final AssetStorageService assetStorageService;

  public DeviceCharacteristicsResourceImpl(
      TimerService timerService,
      ManagerIdentityService identityService,
      AssetStorageService assetStorageService) {
    super(timerService, identityService);
    this.assetStorageService = assetStorageService;
  }

  @Override
  public DeviceCharacteristic[] getCharacteristics(RequestParams params) {
    if (!isAuthenticated()) {
      throw new WebApplicationException(UNAUTHORIZED);
    }

    // Query the meter asset with the same linked user
    OurgridMeterAsset meterAsset =
        (OurgridMeterAsset)
            assetStorageService.find(
                new AssetQuery()
                    .realm(new RealmPredicate(getAuthenticatedRealmName()))
                    .types(OurgridMeterAsset.class)
                    .attributeNames(OurgridMeterAsset.HOUSEHOLD_ENERGY_CHARACTERISTICS.getName())
                    .userIds(getUserId()));
    if (meterAsset == null) {
      throw new WebApplicationException(NOT_FOUND);
    }

    Optional<DeviceCharacteristic[]> characteristics = meterAsset.getHouseholdCharacteristics();
    return characteristics.orElseGet(() -> new DeviceCharacteristic[0]);
  }

  @Override
  public Response setCharacteristics(RequestParams params, SetCharacteristicsDetails details) {
    if (!isAuthenticated()) {
      throw new WebApplicationException(UNAUTHORIZED);
    }
    if (details == null || details.characteristics == null) {
      throw new WebApplicationException(BAD_REQUEST);
    }

    // Query the meter asset with the same linked user
    OurgridMeterAsset meterAsset =
        (OurgridMeterAsset)
            assetStorageService.find(
                new AssetQuery()
                    .realm(new RealmPredicate(getAuthenticatedRealmName()))
                    .types(OurgridMeterAsset.class)
                    .attributeNames(OurgridMeterAsset.HOUSEHOLD_ENERGY_CHARACTERISTICS.getName())
                    .userIds(getUserId()));
    if (meterAsset == null) {
      throw new WebApplicationException(NOT_FOUND);
    }

    try {
      meterAsset = meterAsset.setHouseholdCharacteristics(details.characteristics);
      assetStorageService.merge(meterAsset);
    } catch (IllegalArgumentException e) {
      throw new WebApplicationException(INTERNAL_SERVER_ERROR);
    }

    return Response.ok().build();
  }
}
