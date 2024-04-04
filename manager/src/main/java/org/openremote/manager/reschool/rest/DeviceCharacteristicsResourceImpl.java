package org.openremote.manager.reschool.rest;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.openremote.agent.custom.reschool.ReschoolMeterAsset;
import org.openremote.container.timer.TimerService;
import org.openremote.manager.asset.AssetStorageService;
import org.openremote.manager.security.ManagerIdentityService;
import org.openremote.manager.web.ManagerWebResource;
import org.openremote.model.http.RequestParams;
import org.openremote.model.query.AssetQuery;
import org.openremote.model.query.filter.RealmPredicate;
import org.openremote.model.reschool.DeviceCharacteristic;
import org.openremote.model.reschool.DeviceCharacteristicsResource;

import java.util.Optional;

import static jakarta.ws.rs.core.Response.Status.*;

public class DeviceCharacteristicsResourceImpl extends ManagerWebResource implements DeviceCharacteristicsResource {

    protected final AssetStorageService assetStorageService;

    public DeviceCharacteristicsResourceImpl(TimerService timerService, ManagerIdentityService identityService, AssetStorageService assetStorageService) {
        super(timerService, identityService);
        this.assetStorageService = assetStorageService;
    }

    @Override
    public DeviceCharacteristic[] getCharacteristics(RequestParams params) {
        if (!isAuthenticated()) {
            throw new WebApplicationException(UNAUTHORIZED);
        }

        // Query the meter asset with the same linked user
        ReschoolMeterAsset meterAsset = (ReschoolMeterAsset) assetStorageService.find(new AssetQuery()
                .realm(new RealmPredicate(getAuthenticatedRealmName()))
                .types(ReschoolMeterAsset.class)
                .attributeNames(ReschoolMeterAsset.HOUSEHOLD_ENERGY_CHARACTERISTICS.getName())
                .userIds(getUserId())
        );
        if(meterAsset == null) {
            throw new WebApplicationException(NOT_FOUND);
        }

        Optional<DeviceCharacteristic[]> characteristics = meterAsset.getHouseholdCharacteristics();
        return characteristics.orElseGet(() -> new DeviceCharacteristic[0]);
    }

    @Override
    public Response setCharacteristics(RequestParams params, SetCharacteristicsDetails details) {
        if (!isAuthenticated()) {
            throw new WebApplicationException(UNAUTHORIZED);
        }
        if(details == null || details.characteristics == null) {
            throw new WebApplicationException(BAD_REQUEST);
        }

        // Query the meter asset with the same linked user
        ReschoolMeterAsset meterAsset = (ReschoolMeterAsset) assetStorageService.find(new AssetQuery()
                .realm(new RealmPredicate(getAuthenticatedRealmName()))
                .types(ReschoolMeterAsset.class)
                .attributeNames(ReschoolMeterAsset.HOUSEHOLD_ENERGY_CHARACTERISTICS.getName())
                .userIds(getUserId())
        );
        if(meterAsset == null) {
            throw new WebApplicationException(NOT_FOUND);
        }

        try {
            meterAsset = meterAsset.setHouseholdCharacteristics(details.characteristics);
            assetStorageService.merge(meterAsset);
        } catch (IllegalArgumentException e) {
            throw new WebApplicationException(INTERNAL_SERVER_ERROR);
        }

        return Response.ok().build();
    }
}
