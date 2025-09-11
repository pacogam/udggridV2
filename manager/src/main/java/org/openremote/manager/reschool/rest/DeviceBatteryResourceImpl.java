package org.openremote.manager.reschool.rest;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.openremote.agent.custom.ourgrid.OurgridBatteryAsset;
import org.openremote.container.timer.TimerService;
import org.openremote.manager.asset.AssetProcessingService;
import org.openremote.manager.asset.AssetStorageService;
import org.openremote.manager.security.ManagerIdentityService;
import org.openremote.manager.web.ManagerWebResource;
import org.openremote.model.asset.Asset;
import org.openremote.model.asset.UserAssetLink;
import org.openremote.model.attribute.Attribute;
import org.openremote.model.attribute.AttributeEvent;
import org.openremote.model.http.RequestParams;
import org.openremote.model.query.AssetQuery;
import org.openremote.model.query.filter.RealmPredicate;
import org.openremote.model.reschool.DeviceBatteryResource;

import java.util.Collection;

import static jakarta.ws.rs.core.Response.Status.*;

public class DeviceBatteryResourceImpl extends ManagerWebResource implements DeviceBatteryResource {

    protected final AssetStorageService assetStorageService;
    protected final AssetProcessingService assetProcessingService;

    public DeviceBatteryResourceImpl(TimerService timerService, ManagerIdentityService identityService, AssetStorageService assetStorageService, AssetProcessingService assetProcessingService) {
        super(timerService, identityService);
        this.assetStorageService = assetStorageService;
        this.assetProcessingService = assetProcessingService;
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

        boolean hasAddedAttributes = false;

        Asset<?> batteryAsset = getBatteryById(details.meterId);

        // Create automaticControl attribute if necessary
        if(!batteryAsset.hasAttribute(OurgridBatteryAsset.ALLOW_AUTOMATIC_CONTROL_BUTTON)) {
            batteryAsset.addAttributes(new Attribute<>(OurgridBatteryAsset.ALLOW_AUTOMATIC_CONTROL_BUTTON));
            hasAddedAttributes = true;
        }

        // Also create the challengeActionButton attribute if necessary
        if(!batteryAsset.hasAttribute(OurgridBatteryAsset.ALLOW_DISCHARGING_BUTTON)) {
            batteryAsset.addAttributes(new Attribute<>(OurgridBatteryAsset.ALLOW_DISCHARGING_BUTTON));
            hasAddedAttributes = true;
        }

        // If any attributes have been added, merge the asset
        if(hasAddedAttributes) {
            assetStorageService.merge(batteryAsset);
        }

        // Publish the attribute events
        assetProcessingService.sendAttributeEvent(new AttributeEvent(batteryAsset.getId(), OurgridBatteryAsset.ALLOW_AUTOMATIC_CONTROL_BUTTON.getName(), details.automaticControl));
        assetProcessingService.sendAttributeEvent(new AttributeEvent(batteryAsset.getId(), OurgridBatteryAsset.ALLOW_DISCHARGING_BUTTON.getName(), details.automaticControl));

        return Response.ok().build();
    }

    @Override
    public Response actionButton(RequestParams params, ActionButtonDetails details) {
        if (!isAuthenticated()) {
            throw new WebApplicationException(UNAUTHORIZED);
        }

        Asset<?> batteryAsset = getBatteryById(details.meterId);
        if(!batteryAsset.hasAttribute(OurgridBatteryAsset.ALLOW_DISCHARGING_BUTTON)) {
            batteryAsset.addAttributes(new Attribute<>(OurgridBatteryAsset.ALLOW_DISCHARGING_BUTTON));
        }

        batteryAsset.getAttribute(OurgridBatteryAsset.ALLOW_DISCHARGING_BUTTON)
                .orElseThrow(() -> new WebApplicationException(NOT_FOUND))
                .setValue(details.buttonState);

        assetStorageService.merge(batteryAsset);

        return null;
    }

    /**
     * Function that queries the battery {@link Asset} from the database using {@link AssetStorageService}
     * Requires the user to be linked to the battery asset, and having the meterId as parent ID.
     * Throws a {@link WebApplicationException} if for example the user has no access.
     */
    protected Asset<?> getBatteryById(String meterId) throws WebApplicationException {

        // Query the battery asset, where the meterId is parent.
        // We ignore whether the user has access to the meter (access to the battery is enough).
        Asset<?> batteryAsset = assetStorageService.find(new AssetQuery()
                .realm(new RealmPredicate(getAuthenticatedRealmName()))
                .types(OurgridBatteryAsset.class)
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
