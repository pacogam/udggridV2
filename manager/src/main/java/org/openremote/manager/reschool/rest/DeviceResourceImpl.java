package org.openremote.manager.reschool.rest;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.openremote.agent.custom.ourgrid.OurgridMeterAsset;
import org.openremote.container.timer.TimerService;
import org.openremote.manager.asset.AssetStorageService;
import org.openremote.manager.security.ManagerIdentityService;
import org.openremote.manager.web.ManagerWebResource;
import org.openremote.model.asset.Asset;
import org.openremote.model.asset.UserAssetLink;
import org.openremote.model.http.RequestParams;
import org.openremote.model.query.AssetQuery;
import org.openremote.model.query.filter.AttributePredicate;
import org.openremote.model.query.filter.RealmPredicate;
import org.openremote.model.query.filter.StringPredicate;
import org.openremote.model.reschool.DeviceResource;

import java.util.Collection;
import java.util.Collections;

import static jakarta.ws.rs.core.Response.Status.*;

public class DeviceResourceImpl extends ManagerWebResource implements DeviceResource {

    protected final AssetStorageService assetStorageService;

    public DeviceResourceImpl(TimerService timerService,
                              ManagerIdentityService identityService,
                              AssetStorageService assetStorageService) {
        super(timerService, identityService);
        this.assetStorageService = assetStorageService;
    }

    @Override
    public Response linkDevice(RequestParams requestParams, DeviceLinkDetails details) {
        if (!isAuthenticated()) {
            throw new WebApplicationException(UNAUTHORIZED);
        }

        // Query the asset with the same device id.
        Asset<?> meterAsset = assetStorageService.find(new AssetQuery()
                .select(new AssetQuery.Select().excludeAttributes())
                .realm(new RealmPredicate(getAuthenticatedRealmName()))
                .types(OurgridMeterAsset.class)
                .attributes(
                        new AttributePredicate(OurgridMeterAsset.DEVICE_ID.getName(), new StringPredicate(AssetQuery.Match.EXACT, true, details.deviceName))
                )
        );
        if (meterAsset == null) {
            throw new WebApplicationException(NOT_FOUND);
        }

        // If the device is already connected to a user...
        Collection<UserAssetLink> assetLinks = assetStorageService.findUserAssetLinks(meterAsset.getRealm(), null, meterAsset.getId());
        if (!assetLinks.isEmpty()) {
            if(assetLinks.stream().anyMatch(ua -> ua.getId().getUserId().equals(getUserId()))) {
                return Response.ok().build(); // already linked to the current user; return successfully.
            } else {
                throw new WebApplicationException("Asset is already linked to another user", FORBIDDEN);
            }
        }

        // If the user is not already connected to a device...
        Collection<UserAssetLink> userLinksOfUser = assetStorageService.findUserAssetLinks(meterAsset.getRealm(), getUserId(), null);
        String[] assetIds = userLinksOfUser.stream().map(l -> l.getId().getAssetId()).toArray(String[]::new);
        Collection<Asset<?>> linkedMetersOfUser = assetStorageService.findAll(new AssetQuery()
                .select(new AssetQuery.Select().excludeAttributes())
                .realm(new RealmPredicate(getAuthenticatedRealmName()))
                .types(OurgridMeterAsset.class)
                .ids(assetIds)
        );
        if (!linkedMetersOfUser.isEmpty()) {
            throw new WebApplicationException("User is already linked to a asset", CONFLICT);
        }

        // Link user to the asset
        assetStorageService.storeUserAssetLinks(Collections.singletonList(new UserAssetLink(meterAsset.getRealm(), getUserId(), meterAsset.getId())));

        return Response.ok().build();
    }

    @Override
    public Response removeDevice(RequestParams requestParams, DeviceLinkDetails details) {
        if (!isAuthenticated()) {
            throw new WebApplicationException(UNAUTHORIZED);
        }
        // Query the asset with the same device id.
        Asset<?> meterAsset = assetStorageService.find(new AssetQuery()
                .select(new AssetQuery.Select().excludeAttributes())
                .realm(new RealmPredicate(getAuthenticatedRealmName()))
                .types(OurgridMeterAsset.class)
                .names(details.deviceName)
        );
        if (meterAsset == null) {
            throw new WebApplicationException(NOT_FOUND);
        }

        // Delete the user asset link
        try {
            assetStorageService.deleteUserAssetLinks(Collections.singletonList(
                    new UserAssetLink(getAuthenticatedRealmName(), getUserId(), meterAsset.getId())
            ));
        } catch (Exception e) {
            throw new WebApplicationException(NOT_FOUND);
        }

        return Response.ok().build();
    }
}
