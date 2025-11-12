package org.openremote.manager.reschool.rest;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.openremote.agent.custom.ourgrid.OurgridChargerAsset;
import org.openremote.agent.custom.ourgrid.OurgridHvacAsset;
import org.openremote.agent.custom.ourgrid.OurgridMeterAsset;
import org.openremote.agent.custom.ourgrid.OurgridVehicleAsset;
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
import org.openremote.model.util.ValueUtil;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static jakarta.ws.rs.core.Response.Status.*;

public class DeviceResourceImpl extends ManagerWebResource implements DeviceResource {

    private static final Set<Class<? extends Asset<?>>> ENODE_ASSET_TYPES = Set.of(OurgridChargerAsset.class, OurgridHvacAsset.class, OurgridVehicleAsset.class);

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

        Class<? extends Asset<?>> assetTypeClass = ValueUtil.getAssetClass(details.assetType).orElseThrow(() -> new WebApplicationException("Invalid asset type: " + details.assetType, BAD_REQUEST));

        // Query the asset with the same device id.
        Asset<?> asset = assetStorageService.find(new AssetQuery()
                .select(new AssetQuery.Select().excludeAttributes())
                .realm(new RealmPredicate(getAuthenticatedRealmName()))
                .types(assetTypeClass)
                .attributes(
                        new AttributePredicate(OurgridMeterAsset.DEVICE_ID.getName(), new StringPredicate(AssetQuery.Match.EXACT, true, details.deviceName))
                )
        );
        if (asset == null) {
            throw new WebApplicationException(NOT_FOUND);
        }

        // If the device is already connected to a user...
        Collection<UserAssetLink> assetLinks = assetStorageService.findUserAssetLinks(asset.getRealm(), null, asset.getId());
        if (!assetLinks.isEmpty()) {
            if(assetLinks.stream().anyMatch(ua -> ua.getId().getUserId().equals(getUserId()))) {
                return Response.ok().build(); // already linked to the current user; return successfully.
            } else {
                throw new WebApplicationException("Asset is already linked to another user", FORBIDDEN);
            }
        }

        // If the user is not already connected to a device...
        Collection<UserAssetLink> userLinksOfUser = assetStorageService.findUserAssetLinks(asset.getRealm(), getUserId(), null);
        String[] assetIds = userLinksOfUser.stream().map(l -> l.getId().getAssetId()).toArray(String[]::new);
        Collection<Asset<?>> linkedAssetsOfUser = assetStorageService.findAll(new AssetQuery()
                .select(new AssetQuery.Select().excludeAttributes())
                .realm(new RealmPredicate(getAuthenticatedRealmName()))
                .types(assetTypeClass)
                .ids(assetIds)
        );
        if (!linkedAssetsOfUser.isEmpty()) {
            throw new WebApplicationException("User is already linked to a '" + details.assetType +  "' asset", CONFLICT);
        }

        // Link user to the asset
        assetStorageService.storeUserAssetLinks(List.of(new UserAssetLink(asset.getRealm(), getUserId(), asset.getId())));

        return Response.ok().build();
    }

    @Override
    @SuppressWarnings("unchecked")
    public Response removeDevice(RequestParams requestParams, DeviceLinkDetails details) {
        if (!isAuthenticated()) {
            throw new WebApplicationException(UNAUTHORIZED);
        }

        Class<? extends Asset<?>> assetTypeClass = ValueUtil.getAssetClass(details.assetType).orElseThrow(() -> new WebApplicationException("Invalid asset type: " + details.assetType, BAD_REQUEST));

        boolean isEnodeDevice = ENODE_ASSET_TYPES.contains(assetTypeClass);

        AssetQuery assetQuery = new AssetQuery()
                .select(new AssetQuery.Select().excludeAttributes())
                .realm(new RealmPredicate(getAuthenticatedRealmName()))
                .userIds(getUserId());

        if (isEnodeDevice) {
            // All Enode devices of the user are unlinked and deleted when removing any Enode device
            assetQuery.types(ENODE_ASSET_TYPES.toArray(Class[]::new));
        } else {
            assetQuery.types(assetTypeClass).names(details.deviceName);
        }

        List<Asset<?>> assets = assetStorageService.findAll(assetQuery);

        if (assets == null || assets.isEmpty()) {
            throw new WebApplicationException(NOT_FOUND);
        }

        // Delete the user asset link(s)
        List<UserAssetLink> userAssetLinks = assets.stream().map(asset -> new UserAssetLink(asset.getRealm(), getUserId(), asset.getId())).toList();
        try {
            assetStorageService.deleteUserAssetLinks(userAssetLinks);
        } catch (Exception e) {
            throw new WebApplicationException(NOT_FOUND);
        }

        // Delete all Enode assets
        if (isEnodeDevice) {
            List<String> assetIds = assets.stream().map(Asset::getId).toList();
            assetStorageService.delete(assetIds);
        }

        return Response.ok().build();
    }
}
