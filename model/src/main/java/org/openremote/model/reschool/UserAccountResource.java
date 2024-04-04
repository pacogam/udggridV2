package org.openremote.model.reschool;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import org.openremote.model.http.RequestParams;

@Tag(name = "Reschool User")
@Path("reschool/user/account")
public interface UserAccountResource {

    /**
     * Removes the account from the current user. Used by the "DELETE YOUR ACCOUNT" button.
     * @return HTTP status FORBIDDEN or OK
     */
    @DELETE
    Response deleteAccount(@BeanParam RequestParams requestParams);
}
