package org.openremote.model.reschool;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.openremote.model.http.RequestParams;

import java.io.Serializable;

@Tag(name = "Reschool Device")
@Path("reschool/device/challenges")
public interface DeviceChallengesResource {

    class DeviceJoinChallengeDetails implements Serializable {

        public String meterId;
        public String challengeId;

        public DeviceJoinChallengeDetails() {

        }
        public DeviceJoinChallengeDetails(String meterId, String challengeId) {
            this.meterId = meterId;
            this.challengeId = challengeId;
        }
    }

    class DeviceChallengeHistoryDetails implements Serializable {

        public long startTimestamp;
        public long endTimestamp;

        public DeviceChallengeHistoryDetails() {

        }
        public DeviceChallengeHistoryDetails(Integer startTimestamp, Integer endTimestamp) {
            this.startTimestamp = startTimestamp;
            this.endTimestamp = endTimestamp;
        }
    }

    @Path("join")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    Response joinChallenge(@BeanParam RequestParams params, DeviceJoinChallengeDetails joinChallengeDetails);

    @Path("current")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    Challenge getCurrent(@BeanParam RequestParams params);

    @Path("history")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    Response getHistory(@BeanParam RequestParams params, DeviceChallengeHistoryDetails challengeHistoryDetails);
}
