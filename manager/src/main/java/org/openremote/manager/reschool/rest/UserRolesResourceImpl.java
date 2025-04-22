package org.openremote.manager.reschool.rest;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.openremote.container.timer.TimerService;
import org.openremote.manager.asset.AssetStorageService;
import org.openremote.manager.security.ManagerIdentityService;
import org.openremote.manager.web.ManagerWebResource;
import org.openremote.model.Constants;
import org.openremote.model.http.RequestParams;
import org.openremote.model.reschool.UserRolesResource;
import org.openremote.model.security.ClientRole;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static jakarta.ws.rs.core.Response.Status.*;

public class UserRolesResourceImpl extends ManagerWebResource implements UserRolesResource {


    // List of all roles that are applied during the correction process.
    protected static final String[] CORRECT_ROLES = new String[]{
            ClientRole.READ_ASSETS.getValue()
    };

    protected final AssetStorageService assetStorageService;

    public UserRolesResourceImpl(TimerService timerService, ManagerIdentityService identityService, AssetStorageService assetStorageService) {
        super(timerService, identityService);
        this.assetStorageService = assetStorageService;
    }

    @Override
    public Response verifyUserRoles(RequestParams requestParams) {
        if (!isAuthenticated()) {
            throw new WebApplicationException(UNAUTHORIZED);
        }
        if(getAuthenticatedRealmName().equals(Constants.MASTER_REALM)) {
            throw new WebApplicationException(BAD_REQUEST);
        }
        if(isSuperUser()) {
            throw new WebApplicationException(BAD_REQUEST);
        }

        // Get state of the READ_ASSETS and RESTRICTED_USER roles
        String[] userRealmRoles = identityService.getIdentityProvider().getUserRealmRoles(getAuthenticatedRealmName(), getUserId());
        boolean canReadAssets = Arrays.asList(userRealmRoles).contains(Constants.READ_ASSETS_ROLE);
        boolean isRestricted = Arrays.asList(userRealmRoles).contains(Constants.RESTRICTED_USER_REALM_ROLE);

        // Return status based on whether all roles are correct
        if (!canReadAssets || !isRestricted) {
            return Response.status(Response.Status.FORBIDDEN).build();
        } else {
            return Response.ok().build();
        }
    }

    @Override
    public Response correctUserRoles(RequestParams requestParams) {
        if (!isAuthenticated()) {
            throw new WebApplicationException(UNAUTHORIZED);
        }

        this.correctRoles(getAuthenticatedRealmName(), getUserId());

        return Response.ok().build();
    }



    /* ------------------------------------------------------- */

    // Updates the roles to be correct.
    // Checks and applies RESTRICTED_USER in a realm, and applies the CORRECT_ROLES to the user.
    protected void correctRoles(String realm, String userId) {

        Collection<String> userRealmRoles = new ArrayList<>(List.of(identityService.getIdentityProvider().getUserRealmRoles(realm, userId)));
        boolean isRestricted = userRealmRoles.contains(Constants.RESTRICTED_USER_REALM_ROLE);
        if(!isRestricted) {
            userRealmRoles.add(Constants.RESTRICTED_USER_REALM_ROLE);
            identityService.getIdentityProvider().updateUserRealmRoles(realm, userId, userRealmRoles.toArray(new String[0]));
        }
        // Correct client roles
        identityService.getIdentityProvider().updateUserClientRoles(realm, userId, Constants.KEYCLOAK_CLIENT_ID, CORRECT_ROLES);
    }
}
