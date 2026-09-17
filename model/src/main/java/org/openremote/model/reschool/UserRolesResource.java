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
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import org.openremote.model.http.RequestParams;

@Tag(name = "Reschool User")
@Path("reschool/user/roles")
public interface UserRolesResource {

  /**
   * Verifies whether the user roles are correct or not. The correct roles are the 'READ_ASSETS' and
   * 'RESTRICTED USER' role. This is a specification for the Reschool OurGrid app and is not
   * adjustable outside of the codebase.
   *
   * @return HTTP status FORBIDDEN or OK
   */
  @GET
  @Path("verify")
  Response verifyUserRoles(@BeanParam RequestParams requestParams);

  /**
   * Corrects the user roles, no matter if they are correct or not. The correct roles are the
   * 'READ_ASSETS' and 'RESTRICTED USER' role. This is a specification for the Reschool OurGrid app
   * and is not adjustable outside of the codebase.
   *
   * @return HTTP status OK
   */
  @POST
  @Path("correct")
  Response correctUserRoles(@BeanParam RequestParams requestParams);
}
