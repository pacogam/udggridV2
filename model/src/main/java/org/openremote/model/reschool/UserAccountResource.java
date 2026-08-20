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
package org.openremote.model.reschool;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;
import org.openremote.model.http.RequestParams;
import org.openremote.model.security.User;

@Tag(name = "Reschool User")
@Path("reschool/user/account")
public interface UserAccountResource {

  /**
   * Removes the account from the current user. Used by the "DELETE YOUR ACCOUNT" button.
   *
   * @return HTTP status FORBIDDEN or OK
   */
  @DELETE
  Response deleteAccount(@BeanParam RequestParams requestParams);

  /**
   * Creates a new service user account that is equal to the current user If it already exists, it
   * will override the existing account.
   */
  @Path("serviceuser")
  @POST
  User createUpdateServiceUserAccount(
      @BeanParam RequestParams requestParams, @QueryParam("prefix") String prefix);

  /** Retrieves the service user account linked to the current user. */
  @Path("serviceuser")
  @GET
  User getServiceUserAccount(@BeanParam RequestParams requestParams);

  /** Deletes the service account linked to the current user. */
  @Path("serviceuser")
  @DELETE
  Response deleteServiceUserAccount(@BeanParam RequestParams requestParams);
}
