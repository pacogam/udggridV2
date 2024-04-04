package org.openremote.manager.reschool.rest;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.openremote.container.timer.TimerService;
import org.openremote.manager.security.ManagerIdentityService;
import org.openremote.manager.web.ManagerWebResource;
import org.openremote.model.http.RequestParams;
import org.openremote.model.reschool.UserAccountResource;

import static jakarta.ws.rs.core.Response.Status.*;

public class UserAccountResourceImpl extends ManagerWebResource implements UserAccountResource {

    public UserAccountResourceImpl(TimerService timerService, ManagerIdentityService identityService) {
        super(timerService, identityService);
    }

    @Override
    public Response deleteAccount(RequestParams requestParams) {
        if (!isAuthenticated()) {
            throw new WebApplicationException(UNAUTHORIZED);
        }
        if (getUserId() == null || getAuthenticatedRealmName() == null) {
            throw new WebApplicationException(FORBIDDEN);
        }

        try {
            identityService.getIdentityProvider().deleteUser(getAuthenticatedRealmName(), getUserId());
            return Response.ok().build();
        } catch (Exception e) {
            throw new WebApplicationException(INTERNAL_SERVER_ERROR);
        }
    }
}
