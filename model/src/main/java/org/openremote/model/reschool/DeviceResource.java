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
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.Serializable;
import org.openremote.model.http.RequestParams;

@Tag(name = "Reschool Device")
@Path("reschool/device")
public interface DeviceResource {

  class DeviceLinkDetails implements Serializable {
    public DeviceLinkDetails() {}

    public DeviceLinkDetails(String deviceName, String assetType) {
      this.deviceName = deviceName;
      this.assetType = assetType;
    }

    public String deviceName;
    public String assetType;
  }

  @Path("link")
  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  Response linkDevice(@BeanParam RequestParams requestParams, DeviceLinkDetails details);

  @Path("link")
  @DELETE
  @Consumes(MediaType.APPLICATION_JSON)
  Response removeDevice(@BeanParam RequestParams requestParams, DeviceLinkDetails details);
}
