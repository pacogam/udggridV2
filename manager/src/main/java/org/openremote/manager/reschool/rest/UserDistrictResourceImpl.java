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
import java.util.Collection;
import java.util.Collections;
import org.openremote.agent.custom.ourgrid.OurgridDistrictAsset;
import org.openremote.container.timer.TimerService;
import org.openremote.manager.asset.AssetStorageService;
import org.openremote.manager.security.ManagerIdentityService;
import org.openremote.manager.web.ManagerWebResource;
import org.openremote.model.asset.Asset;
import org.openremote.model.asset.UserAssetLink;
import org.openremote.model.http.RequestParams;
import org.openremote.model.query.AssetQuery;
import org.openremote.model.query.filter.RealmPredicate;
import org.openremote.model.reschool.UserDistrictResource;
import org.openremote.model.util.TextUtil;

public class UserDistrictResourceImpl extends ManagerWebResource implements UserDistrictResource {

  protected final AssetStorageService assetStorageService;

  public UserDistrictResourceImpl(
      TimerService timerService,
      ManagerIdentityService identityService,
      AssetStorageService assetStorageService) {
    super(timerService, identityService);
    this.assetStorageService = assetStorageService;
  }

  @Override
  public Response verifyDistrict(RequestParams requestParams) {
    if (!isAuthenticated()) {
      throw new WebApplicationException(UNAUTHORIZED);
    }
    if (!userHasLinkedDistrict(getAuthenticatedRealmName(), getUserId())) {
      throw new WebApplicationException(NOT_FOUND);
    } else {
      return Response.ok().build();
    }
  }

  @Override
  public Response linkDistrict(RequestParams requestParams, LinkDistrictDetails details) {
    if (!isAuthenticated()) {
      throw new WebApplicationException(UNAUTHORIZED);
    }

    // If the asset name exists within the realm
    AssetQuery query =
        new AssetQuery()
            .select(new AssetQuery.Select().excludeAttributes())
            .realm(new RealmPredicate(getAuthenticatedRealmName()))
            .types(OurgridDistrictAsset.class);
    if (!TextUtil.isNullOrEmpty(details.assetName)) {
      query.names(details.assetName);
    }
    query.limit = 1;

    Asset<?> asset = assetStorageService.find(query);
    if (asset == null) {
      throw new WebApplicationException("District could not be found", NOT_FOUND);
    }

    // If the user is not already connected to a device...
    if (userHasLinkedDistrict(asset.getRealm(), getUserId())) {
      throw new WebApplicationException("User is already linked to a district", CONFLICT);
    }

    // Link user to the asset
    assetStorageService.storeUserAssetLinks(
        Collections.singletonList(new UserAssetLink(asset.getRealm(), getUserId(), asset.getId())));

    return Response.ok().build();
  }

  @Override
  public Response removeUserDistrict(RequestParams requestParams) {
    if (!isAuthenticated()) {
      throw new WebApplicationException(UNAUTHORIZED);
    }

    Collection<Asset<?>> linkedDistrictsOfUser =
        getLinkedDistrictsOfUser(getAuthenticatedRealmName(), getUserId());
    if (linkedDistrictsOfUser.size() == 0) {
      throw new WebApplicationException("User is not linked to any district.", NOT_FOUND);
    }

    linkedDistrictsOfUser.forEach(
        (Asset<?> a) -> assetStorageService.deleteUserAssetLinks(a.getId()));

    return Response.ok().build();
  }

  /* ----------------------------------------------------- */

  protected boolean userHasLinkedDistrict(String realm, String userId) {
    return !getLinkedDistrictsOfUser(realm, userId).isEmpty();
  }

  protected Collection<Asset<?>> getLinkedDistrictsOfUser(String realm, String userId) {

    Collection<UserAssetLink> userLinksOfUser =
        assetStorageService.findUserAssetLinks(realm, userId, null);
    String[] assetIds =
        userLinksOfUser.stream().map(l -> l.getId().getAssetId()).toArray(String[]::new);
    return assetStorageService.findAll(
        new AssetQuery()
            .select(new AssetQuery.Select().excludeAttributes())
            .realm(new RealmPredicate(realm))
            .types(OurgridDistrictAsset.class)
            .ids(assetIds));
  }
}
