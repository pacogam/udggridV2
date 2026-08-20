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
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.Serializable;
import org.openremote.model.http.RequestParams;

@Tag(name = "Reschool User")
@Path("reschool/user/district")
public interface UserDistrictResource {

  class LinkDistrictDetails implements Serializable {
    public LinkDistrictDetails() {}

    public LinkDistrictDetails(String assetName) {
      this.assetName = assetName;
    }

    public String assetName;
  }

  /**
   * Verifies if the user is connected to any district.
   *
   * @return HTTP status NOT_FOUND or OK
   */
  @GET
  @Path("verify")
  Response verifyDistrict(@BeanParam RequestParams requestParams);

  /**
   * Links the user to a district asset by name (see {@link LinkDistrictDetails}). If the user is
   * already linked to a district, it will return a 409 'CONFLICT' error.
   *
   * @return HTTP status NOT_FOUND, CONFLICT or OK
   */
  @POST
  @Path("link")
  @Consumes(MediaType.APPLICATION_JSON)
  Response linkDistrict(@BeanParam RequestParams requestParams, LinkDistrictDetails details);

  /**
   * Removes the user from the district it is linked to. If the user is not linked to a district
   * yet, it will return a 404 'NOT FOUND' error.
   *
   * @return HTTP status NOT_FOUND or OK
   */
  @DELETE
  @Path("link")
  Response removeUserDistrict(@BeanParam RequestParams requestParams);
}
