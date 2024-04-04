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
     * Verifies whether the user roles are correct or not.
     * The correct roles are the 'READ_ASSETS' and 'RESTRICTED USER' role.
     * This is a specification for the Reschool OurGrid app and is not adjustable outside of the codebase.
     * @return HTTP status FORBIDDEN or OK
     */
    @GET
    @Path("verify")
    Response verifyUserRoles(@BeanParam RequestParams requestParams);

    /**
     * Corrects the user roles, no matter if they are correct or not.
     * The correct roles are the 'READ_ASSETS' and 'RESTRICTED USER' role.
     * This is a specification for the Reschool OurGrid app and is not adjustable outside of the codebase.
     * @return HTTP status OK
     */
    @POST
    @Path("correct")
    Response correctUserRoles(@BeanParam RequestParams requestParams);
}
