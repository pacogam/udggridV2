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

@Tag(name = "Reschool Device")
@Path("reschool/device/challenges")
public interface DeviceChallengesResource {

  class DeviceJoinChallengeDetails implements Serializable {

    public String meterId;
    public String challengeId;

    public DeviceJoinChallengeDetails() {}

    public DeviceJoinChallengeDetails(String meterId, String challengeId) {
      this.meterId = meterId;
      this.challengeId = challengeId;
    }
  }

  class DeviceChallengeHistoryDetails implements Serializable {

    public long startTimestamp;
    public long endTimestamp;

    public DeviceChallengeHistoryDetails() {}

    public DeviceChallengeHistoryDetails(Integer startTimestamp, Integer endTimestamp) {
      this.startTimestamp = startTimestamp;
      this.endTimestamp = endTimestamp;
    }
  }

  @Path("join")
  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  Response joinChallenge(
      @BeanParam RequestParams params, DeviceJoinChallengeDetails joinChallengeDetails);

  @Path("current")
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  Challenge getCurrent(@BeanParam RequestParams params);

  @Path("history")
  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  Response getHistory(
      @BeanParam RequestParams params, DeviceChallengeHistoryDetails challengeHistoryDetails);
}
