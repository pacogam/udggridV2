package org.openremote.manager.reschool.rest;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.openremote.agent.custom.reschool.ReschoolDistrictAsset;
import org.openremote.container.timer.TimerService;
import org.openremote.manager.asset.AssetStorageService;
import org.openremote.manager.security.ManagerIdentityService;
import org.openremote.manager.web.ManagerWebResource;
import org.openremote.model.asset.Asset;
import org.openremote.model.asset.UserAssetLink;
import org.openremote.model.http.RequestParams;
import org.openremote.model.query.AssetQuery;
import org.openremote.model.query.filter.RealmPredicate;
import org.openremote.model.reschool.UserDistrictResource;

import java.util.Collection;
import java.util.Collections;

import static jakarta.ws.rs.core.Response.Status.*;

public class UserDistrictResourceImpl extends ManagerWebResource implements UserDistrictResource {

    protected final AssetStorageService assetStorageService;

    public UserDistrictResourceImpl(TimerService timerService, ManagerIdentityService identityService, AssetStorageService assetStorageService) {
        super(timerService, identityService);
        this.assetStorageService = assetStorageService;
    }

    @Override
    public Response verifyDistrict(RequestParams requestParams) {
        if (!isAuthenticated()) {
            throw new WebApplicationException(UNAUTHORIZED);
        }
        if(!userHasLinkedDistrict(getAuthenticatedRealmName(), getUserId())) {
            throw new WebApplicationException(NOT_FOUND);
        } else {
            return Response.ok().build();
        }
    }

    @Override
    public Response linkDistrict(RequestParams requestParams, LinkDistrictDetails details) {
        if (!isAuthenticated()) {
            throw new WebApplicationException(UNAUTHORIZED);
        }

        // If the asset name exists within the realm
        Asset<?> asset = assetStorageService.find(new AssetQuery()
                .select(new AssetQuery.Select().excludeAttributes())
                .realm(new RealmPredicate(getAuthenticatedRealmName()))
                .types(ReschoolDistrictAsset.class)
                .names(details.assetName)
        );
        if (asset == null) {
            throw new WebApplicationException("District could not be found", NOT_FOUND);
        }

        // If the user is not already connected to a device...
        if(userHasLinkedDistrict(asset.getRealm(), getUserId())) {
            throw new WebApplicationException("User is already linked to a district", CONFLICT);
        }

        // Link user to the asset
        assetStorageService.storeUserAssetLinks(Collections.singletonList(new UserAssetLink(asset.getRealm(), getUserId(), asset.getId())));

        return Response.ok().build();
    }

    @Override
    public Response removeUserDistrict(RequestParams requestParams) {
        if (!isAuthenticated()) {
            throw new WebApplicationException(UNAUTHORIZED);
        }

        Collection<Asset<?>> linkedDistrictsOfUser = getLinkedDistrictsOfUser(getAuthenticatedRealmName(), getUserId());
        if (linkedDistrictsOfUser.size() == 0) {
            throw new WebApplicationException("User is not linked to any district.", NOT_FOUND);
        }

        linkedDistrictsOfUser.forEach((Asset<?> a) ->
            assetStorageService.deleteUserAssetLinks(a.getId())
        );

        return Response.ok().build();
    }



    /* ----------------------------------------------------- */

    protected boolean userHasLinkedDistrict(String realm, String userId) {
        return getLinkedDistrictsOfUser(realm, userId).size() > 0;
    }

    protected Collection<Asset<?>> getLinkedDistrictsOfUser(String realm, String userId) {

        Collection<UserAssetLink> userLinksOfUser = assetStorageService.findUserAssetLinks(realm, userId, null);
        String[] assetIds = userLinksOfUser.stream().map(l -> l.getId().getAssetId()).toArray(String[]::new);
        return assetStorageService.findAll(new AssetQuery()
                .select(new AssetQuery.Select().excludeAttributes())
                .realm(new RealmPredicate(realm))
                .types(ReschoolDistrictAsset.class)
                .ids(assetIds)
        );
    }
}
