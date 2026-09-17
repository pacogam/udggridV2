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
import java.util.List;
import org.openremote.container.timer.TimerService;
import org.openremote.manager.asset.AssetStorageService;
import org.openremote.manager.security.ManagerIdentityService;
import org.openremote.manager.web.ManagerWebResource;
import org.openremote.model.Constants;
import org.openremote.model.asset.UserAssetLink;
import org.openremote.model.http.RequestParams;
import org.openremote.model.query.AssetQuery;
import org.openremote.model.query.UserQuery;
import org.openremote.model.query.filter.RealmPredicate;
import org.openremote.model.query.filter.StringPredicate;
import org.openremote.model.reschool.UserAccountResource;
import org.openremote.model.security.User;

public class UserAccountResourceImpl extends ManagerWebResource implements UserAccountResource {

  protected final AssetStorageService assetStorageService;

  public UserAccountResourceImpl(
      TimerService timerService,
      ManagerIdentityService identityService,
      AssetStorageService assetStorageService) {
    super(timerService, identityService);
    this.assetStorageService = assetStorageService;
  }

  @Override
  public Response deleteAccount(RequestParams requestParams) {
    if (!isAuthenticated()) {
      throw new WebApplicationException(UNAUTHORIZED);
    }
    if (getUserId() == null || getAuthenticatedRealmName() == null) {
      throw new WebApplicationException(FORBIDDEN);
    }

    try {
      identityService.getIdentityProvider().deleteUser(getAuthenticatedRealmName(), getUserId());
      return Response.ok().build();
    } catch (Exception e) {
      throw new WebApplicationException(INTERNAL_SERVER_ERROR);
    }
  }

  @Override
  public User getServiceUserAccount(RequestParams requestParams) {
    List<User> serviceUsers = this.queryServiceUserAccounts();
    if (serviceUsers.isEmpty()) {
      throw new WebApplicationException(NOT_FOUND);
    }
    return serviceUsers.getFirst();
  }

  @Override
  public User createUpdateServiceUserAccount(RequestParams requestParams, String prefix) {
    if (!isAuthenticated()) {
      throw new WebApplicationException(UNAUTHORIZED);
    }
    if (getUserId() == null || getAuthenticatedRealmName() == null) {
      throw new WebApplicationException(FORBIDDEN);
    }
    if (prefix == null) {
      prefix = "serviceuser";
    }

    try {
      // Copy the existing user into a new service user account
      User newUser = identityService.getIdentityProvider().getUser(getUserId());
      newUser.setId(null);
      newUser.setEmail(null);
      newUser.setUsername(prefix + "-" + getUserId());
      newUser.setServiceAccount(true);
      User serviceUser =
          identityService
              .getIdentityProvider()
              .createUpdateUser(getAuthenticatedRealmName(), newUser, null, true);

      // Update the roles of the new service user account
      String[] realmRoles =
          identityService
              .getIdentityProvider()
              .getUserRealmRoles(getAuthenticatedRealmName(), getUserId());
      identityService
          .getIdentityProvider()
          .updateUserRealmRoles(serviceUser.getRealm(), serviceUser.getId(), realmRoles);

      // Update client roles of the new service user account
      String[] clientRoles =
          identityService
              .getIdentityProvider()
              .getUserClientRoles(
                  getAuthenticatedRealmName(), getUserId(), Constants.KEYCLOAK_CLIENT_ID);
      identityService
          .getIdentityProvider()
          .updateUserClientRoles(
              serviceUser.getRealm(),
              serviceUser.getId(),
              Constants.KEYCLOAK_CLIENT_ID,
              clientRoles);

      // Update the linked assets
      List<UserAssetLink> assetLinks =
          assetStorageService.findUserAssetLinks(getAuthenticatedRealmName(), getUserId(), null);
      List<UserAssetLink> newAssetLinks =
          assetLinks.stream()
              .map(
                  l ->
                      new UserAssetLink(
                          serviceUser.getRealm(), serviceUser.getId(), l.getId().getAssetId()))
              .toList();
      assetStorageService.storeUserAssetLinks(newAssetLinks);

      // Query the new service user account again to retrieve the secret, and return it.
      return identityService.getIdentityProvider().getUser(serviceUser.getId());

    } catch (Exception e) {
      throw new WebApplicationException(INTERNAL_SERVER_ERROR);
    }
  }

  @Override
  public Response deleteServiceUserAccount(RequestParams requestParams) {
    if (!isAuthenticated()) {
      throw new WebApplicationException(UNAUTHORIZED);
    }
    if (getUserId() == null || getAuthenticatedRealmName() == null) {
      throw new WebApplicationException(FORBIDDEN);
    }
    try {
      // Query the service user accounts linked to the user
      List<User> serviceUsers = this.queryServiceUserAccounts();
      if (serviceUsers.isEmpty()) {
        return Response.notModified().build();
      }
      // Delete the users linked to the user
      for (User user : serviceUsers) {
        identityService.getIdentityProvider().deleteUser(getAuthenticatedRealmName(), user.getId());
      }
      return Response.ok().build();
    } catch (Exception e) {
      throw new WebApplicationException(INTERNAL_SERVER_ERROR);
    }
  }

  /** Internal function to query the service user accounts linked to the user. */
  protected List<User> queryServiceUserAccounts() {
    List<User> users =
        List.of(
            identityService
                .getIdentityProvider()
                .queryUsers(
                    new UserQuery()
                        .realm(new RealmPredicate(getAuthenticatedRealmName()))
                        .usernames(
                            new StringPredicate().match(AssetQuery.Match.END).value(getUserId()))));
    return users.stream().filter(User::isServiceAccount).toList();
  }
}
