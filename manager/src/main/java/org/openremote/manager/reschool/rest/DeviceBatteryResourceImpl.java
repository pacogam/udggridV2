package org.openremote.manager.reschool.rest;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.openremote.container.timer.TimerService;
import org.openremote.manager.asset.AssetStorageService;
import org.openremote.manager.security.ManagerIdentityService;
import org.openremote.manager.web.ManagerWebResource;
import org.openremote.model.asset.Asset;
import org.openremote.model.asset.UserAssetLink;
import org.openremote.model.asset.impl.ElectricityBatteryAsset;
import org.openremote.model.attribute.Attribute;
import org.openremote.model.attribute.AttributeMap;
import org.openremote.model.http.RequestParams;
import org.openremote.model.query.AssetQuery;
import org.openremote.model.query.filter.RealmPredicate;
import org.openremote.model.reschool.DeviceBatteryResource;
import org.openremote.model.value.ValueType;

import java.util.Collection;
import java.util.Optional;

import static jakarta.ws.rs.core.Response.Status.*;

public class DeviceBatteryResourceImpl extends ManagerWebResource implements DeviceBatteryResource {

    protected final AssetStorageService assetStorageService;

    public DeviceBatteryResourceImpl(TimerService timerService, ManagerIdentityService identityService, AssetStorageService assetStorageService) {
        super(timerService, identityService);
        this.assetStorageService = assetStorageService;
    }

    @Override
    public Asset<?> getBattery(RequestParams params, String meterId) {
        if (!isAuthenticated()) {
            throw new WebApplicationException(UNAUTHORIZED);
        }
        return getBatteryById(meterId);
    }

    @Override
    public Response automaticControl(RequestParams params, AutomaticControlDetails details) {
        if (!isAuthenticated()) {
            throw new WebApplicationException(UNAUTHORIZED);
        }

        Asset<?> batteryAsset = getBatteryById(details.meterId);
        if(!batteryAsset.hasAttribute("automaticControl")) {
            batteryAsset.addAttributes(new Attribute<>("automaticControl", ValueType.BOOLEAN));
        }

        batteryAsset.getAttribute("automaticControl").orElseThrow(() -> new WebApplicationException(NOT_FOUND)).setValue(details.automaticControl);

        assetStorageService.merge(batteryAsset);

        return Response.ok().build();
    }

    /**
     * Function that queries the battery {@link Asset} from the database using {@link AssetStorageService}
     * Requires the user to be linked to the battery asset, and having the meterId as parent ID.
     * Throws a {@link WebApplicationException} if for example the user has no access.
     */
    protected Asset<?> getBatteryById(String meterId) throws WebApplicationException {

        // Query the battery asset, where the meterId is parent.
        // We ignore whether the user has access to the meter (access to the battery is enough).
        // TODO: Adjust asset type
        Asset<?> batteryAsset = assetStorageService.find(new AssetQuery()
                .realm(new RealmPredicate(getAuthenticatedRealmName()))
                .types(ElectricityBatteryAsset.class)
                .parents(meterId)
        );
        if (batteryAsset == null) {
            throw new WebApplicationException(NOT_FOUND);
        }

        // Check if the battery is linked to the user requesting it.
        Collection<UserAssetLink> userLinksOfAsset = assetStorageService.findUserAssetLinks(batteryAsset.getRealm(), getUserId(), batteryAsset.getId());
        if (userLinksOfAsset == null || userLinksOfAsset.isEmpty()) {
            throw new WebApplicationException(FORBIDDEN);
        }

        return batteryAsset;
    }
}
