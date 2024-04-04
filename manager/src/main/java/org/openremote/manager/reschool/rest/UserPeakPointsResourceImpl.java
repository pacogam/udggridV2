package org.openremote.manager.reschool.rest;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.openremote.agent.custom.reschool.OurGridPeaksAsset;
import org.openremote.container.timer.TimerService;
import org.openremote.manager.asset.AssetStorageService;
import org.openremote.manager.security.ManagerIdentityService;
import org.openremote.manager.web.ManagerWebResource;
import org.openremote.model.asset.Asset;
import org.openremote.model.asset.UserAssetLink;
import org.openremote.model.http.RequestParams;
import org.openremote.model.query.AssetQuery;
import org.openremote.model.query.filter.RealmPredicate;
import org.openremote.model.reschool.UserPeakPointsResource;

import java.util.Collection;
import java.util.Collections;

import static jakarta.ws.rs.core.Response.Status.*;

public class UserPeakPointsResourceImpl extends ManagerWebResource implements UserPeakPointsResource {

    protected final AssetStorageService assetStorageService;

    public UserPeakPointsResourceImpl(TimerService timerService, ManagerIdentityService identityService, AssetStorageService assetStorageService) {
        super(timerService, identityService);
        this.assetStorageService = assetStorageService;
    }

    @Override
    public Response verifyPeakPointsAsset(RequestParams requestParams) {
        if (!isAuthenticated()) {
            throw new WebApplicationException(UNAUTHORIZED);
        }
        if(!userHasLinkedPeakPointsAsset(getAuthenticatedRealmName(), getUserId())) {
            throw new WebApplicationException(NOT_FOUND);
        } else {
            return Response.ok().build();
        }
    }

    @Override
    public Response linkPeakPointsAsset(RequestParams requestParams, UserPeakPointsResource.LinkPeakPointsDetails details) {
        if (!isAuthenticated()) {
            throw new WebApplicationException(UNAUTHORIZED);
        }

        // If the asset name exists within the realm
        Asset<?> asset = assetStorageService.find(new AssetQuery()
                .select(new AssetQuery.Select().excludeAttributes())
                .realm(new RealmPredicate(getAuthenticatedRealmName()))
                .types(OurGridPeaksAsset.class)
                .ids(details.assetId)
        );
        if (asset == null) {
            throw new WebApplicationException("Peak points asset could not be found", NOT_FOUND);
        }

        // If the user is not already connected to a device...
        if(userHasLinkedPeakPointsAsset(asset.getRealm(), getUserId())) {
            throw new WebApplicationException("User is already linked to a peak points asset", CONFLICT);
        }

        // Link user to the asset
        assetStorageService.storeUserAssetLinks(Collections.singletonList(new UserAssetLink(asset.getRealm(), getUserId(), asset.getId())));

        return Response.ok().build();
    }

    @Override
    public Response removePeakPointsAsset(RequestParams requestParams) {
        if (!isAuthenticated()) {
            throw new WebApplicationException(UNAUTHORIZED);
        }

        Collection<Asset<?>> linkedPeakPointsAssetsOfUser = getLinkedPeakPointsAssetsOfUser(getAuthenticatedRealmName(), getUserId());
        if (linkedPeakPointsAssetsOfUser.isEmpty()) {
            throw new WebApplicationException("User is not linked to any peak points asset.", NOT_FOUND);
        }

        linkedPeakPointsAssetsOfUser.forEach((Asset<?> a) ->
                assetStorageService.deleteUserAssetLinks(a.getId())
        );

        return Response.ok().build();
    }



    /* ----------------------------------------------------- */

    protected boolean userHasLinkedPeakPointsAsset(String realm, String userId) {
        return !getLinkedPeakPointsAssetsOfUser(realm, userId).isEmpty();
    }

    protected Collection<Asset<?>> getLinkedPeakPointsAssetsOfUser(String realm, String userId) {

        Collection<UserAssetLink> userAssetLinks = assetStorageService.findUserAssetLinks(realm, userId, null);
        String[] assetIds = userAssetLinks.stream().map(l -> l.getId().getAssetId()).toArray(String[]::new);
        return assetStorageService.findAll(new AssetQuery()
                .select(new AssetQuery.Select().excludeAttributes())
                .realm(new RealmPredicate(realm))
                .types(OurGridPeaksAsset.class)
                .ids(assetIds)
        );
    }
}
