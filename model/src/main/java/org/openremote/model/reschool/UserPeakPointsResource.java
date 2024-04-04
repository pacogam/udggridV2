package org.openremote.model.reschool;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.openremote.model.http.RequestParams;

import java.io.Serializable;

@Tag(name = "Reschool User")
@Path("reschool/user/peakpoints")
public interface UserPeakPointsResource {

    class LinkPeakPointsDetails implements Serializable {
        public LinkPeakPointsDetails() {

        }

        public LinkPeakPointsDetails(String assetId) {
            this.assetId = assetId;
        }

        public String assetId;
    }

    /**
     * Verifies if the user is connected to any peak points' asset.
     * @return HTTP status NOT_FOUND or OK
     */
    @POST
    @Path("verify")
    Response verifyPeakPointsAsset(@BeanParam RequestParams requestParams);

    /**
     * Links the user to a peak points asset by id (see {@link UserPeakPointsResource.LinkPeakPointsDetails}).
     * If the user is already linked to a peak points asset, it will return a 409 'CONFLICT' error.
     * @return HTTP status NOT_FOUND, CONFLICT or OK
     */
    @POST
    @Path("link")
    @Consumes(MediaType.APPLICATION_JSON)
    Response linkPeakPointsAsset(@BeanParam RequestParams requestParams, UserPeakPointsResource.LinkPeakPointsDetails details);

    /**
     * Removes the user from the peak points asset it is linked to.
     * If the user is not linked to a peak points asset yet, it will return a 404 'NOT FOUND' error.
     * @return HTTP status NOT_FOUND or OK
     */
    @DELETE
    @Path("link")
    Response removePeakPointsAsset(@BeanParam RequestParams requestParams);
}
