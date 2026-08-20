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
import org.openremote.model.asset.Asset;
import org.openremote.model.http.RequestParams;

@Tag(name = "Reschool Device")
@Path("reschool/device/battery")
public interface DeviceBatteryResource {

  class AutomaticControlDetails implements Serializable {

    public String meterId;
    public boolean automaticControl;

    public AutomaticControlDetails() {}

    public AutomaticControlDetails(String meterId, boolean automaticControl) {
      this.meterId = meterId;
      this.automaticControl = automaticControl;
    }
  }

  class ActionButtonDetails implements Serializable {

    public String meterId;
    public boolean buttonState;

    public ActionButtonDetails() {}

    public ActionButtonDetails(String meterId, boolean buttonState) {
      this.meterId = meterId;
      this.buttonState = buttonState;
    }
  }

  @GET
  @Path("{meterId}")
  @Produces(MediaType.APPLICATION_JSON)
  Asset<?> getBattery(@BeanParam RequestParams params, @PathParam("meterId") String meterId);

  @POST
  @Path("automaticControl")
  @Consumes(MediaType.APPLICATION_JSON)
  Response automaticControl(@BeanParam RequestParams params, AutomaticControlDetails details);

  @POST
  @Path("actionButton")
  @Consumes(MediaType.APPLICATION_JSON)
  Response actionButton(@BeanParam RequestParams params, ActionButtonDetails details);
}
