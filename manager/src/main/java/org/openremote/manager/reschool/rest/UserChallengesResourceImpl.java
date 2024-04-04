package org.openremote.manager.reschool.rest;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.openremote.agent.custom.reschool.OurGridChallengesAsset;
import org.openremote.container.timer.TimerService;
import org.openremote.manager.asset.AssetStorageService;
import org.openremote.manager.security.ManagerIdentityService;
import org.openremote.manager.web.ManagerWebResource;
import org.openremote.model.asset.Asset;
import org.openremote.model.asset.UserAssetLink;
import org.openremote.model.http.RequestParams;
import org.openremote.model.query.AssetQuery;
import org.openremote.model.query.filter.RealmPredicate;
import org.openremote.model.reschool.UserChallengesResource;

import java.util.Collection;
import java.util.Collections;

import static jakarta.ws.rs.core.Response.Status.*;

public class UserChallengesResourceImpl extends ManagerWebResource implements UserChallengesResource {

    protected final AssetStorageService assetStorageService;

    public UserChallengesResourceImpl(TimerService timerService, ManagerIdentityService identityService, AssetStorageService assetStorageService) {
        super(timerService, identityService);
        this.assetStorageService = assetStorageService;
    }

    @Override
    public Response verifyChallengesAsset(RequestParams requestParams) {
        if (!isAuthenticated()) {
            throw new WebApplicationException(UNAUTHORIZED);
        }
        if(!userHasLinkedChallengesAsset(getAuthenticatedRealmName(), getUserId())) {
            throw new WebApplicationException(NOT_FOUND);
        } else {
            return Response.ok().build();
        }
    }

    @Override
    public Response linkChallengesAsset(RequestParams requestParams, LinkChallengesDetails details) {
        if (!isAuthenticated()) {
            throw new WebApplicationException(UNAUTHORIZED);
        }

        // If the asset name exists within the realm
        Asset<?> asset = assetStorageService.find(new AssetQuery()
                .select(new AssetQuery.Select().excludeAttributes())
                .realm(new RealmPredicate(getAuthenticatedRealmName()))
                .types(OurGridChallengesAsset.class)
                .ids(details.assetId)
        );
        if (asset == null) {
            throw new WebApplicationException("Challenges asset could not be found", NOT_FOUND);
        }

        // If the user is not already connected to a device...
        if(userHasLinkedChallengesAsset(asset.getRealm(), getUserId())) {
            throw new WebApplicationException("User is already linked to a challenges asset", CONFLICT);
        }

        // Link user to the asset
        assetStorageService.storeUserAssetLinks(Collections.singletonList(new UserAssetLink(asset.getRealm(), getUserId(), asset.getId())));

        return Response.ok().build();
    }

    @Override
    public Response removeChallengesAsset(RequestParams requestParams) {
        if (!isAuthenticated()) {
            throw new WebApplicationException(UNAUTHORIZED);
        }

        Collection<Asset<?>> linkedChallengesAssetsOfUser = getLinkedChallengesAssetsOfUser(getAuthenticatedRealmName(), getUserId());
        if (linkedChallengesAssetsOfUser.size() == 0) {
            throw new WebApplicationException("User is not linked to any challenges asset.", NOT_FOUND);
        }

        linkedChallengesAssetsOfUser.forEach((Asset<?> a) ->
                assetStorageService.deleteUserAssetLinks(a.getId())
        );

        return Response.ok().build();
    }



    /* ----------------------------------------------------- */

    protected boolean userHasLinkedChallengesAsset(String realm, String userId) {
        return getLinkedChallengesAssetsOfUser(realm, userId).size() > 0;
    }

    protected Collection<Asset<?>> getLinkedChallengesAssetsOfUser(String realm, String userId) {

        Collection<UserAssetLink> userAssetLinks = assetStorageService.findUserAssetLinks(realm, userId, null);
        String[] assetIds = userAssetLinks.stream().map(l -> l.getId().getAssetId()).toArray(String[]::new);
        return assetStorageService.findAll(new AssetQuery()
                .select(new AssetQuery.Select().excludeAttributes())
                .realm(new RealmPredicate(realm))
                .types(OurGridChallengesAsset.class)
                .ids(assetIds)
        );
    }
}
