package org.openremote.model.reschool;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import org.openremote.model.http.RequestParams;

@Tag(name = "Reschool Challenges")
@Path("reschool/challenges")
public interface ChallengesResource {

    @Path("current")
    @GET
    Response getCurrent(@BeanParam RequestParams params);

    @Path("history")
    @GET
    Response getHistoryTotal(@BeanParam RequestParams params);
}
