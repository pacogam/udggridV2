package org.openremote.model.reschool;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.openremote.model.http.RequestParams;

import java.io.Serializable;

@Tag(name = "Reschool User")
@Path("reschool/user/challenges")
public interface UserChallengesResource {

    class LinkChallengesDetails implements Serializable {
        public LinkChallengesDetails() {

        }

        public LinkChallengesDetails(String assetId) {
            this.assetId = assetId;
        }

        public String assetId;
    }

    /**
     * Verifies if the user is connected to any challenges' asset.
     * @return HTTP status NOT_FOUND or OK
     */
    @POST
    @Path("verify")
    Response verifyChallengesAsset(@BeanParam RequestParams requestParams);

    /**
     * Links the user to a challenges asset by id (see {@link LinkChallengesDetails}).
     * If the user is already linked to a challenges asset, it will return a 409 'CONFLICT' error.
     * @return HTTP status NOT_FOUND, CONFLICT or OK
     */
    @POST
    @Path("link")
    @Consumes(MediaType.APPLICATION_JSON)
    Response linkChallengesAsset(@BeanParam RequestParams requestParams, LinkChallengesDetails details);

    /**
     * Removes the user from the challenges asset it is linked to.
     * If the user is not linked to a challenges asset yet, it will return a 404 'NOT FOUND' error.
     * @return HTTP status NOT_FOUND or OK
     */
    @DELETE
    @Path("link")
    Response removeChallengesAsset(@BeanParam RequestParams requestParams);
}
