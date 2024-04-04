package org.openremote.manager.reschool.rest;

import org.openremote.container.timer.TimerService;
import org.openremote.manager.asset.AssetStorageService;
import org.openremote.manager.datapoint.AssetDatapointService;
import org.openremote.manager.security.ManagerIdentityService;
import org.openremote.manager.web.ManagerWebService;
import org.openremote.model.Container;
import org.openremote.model.ContainerService;

public class ReschoolRestService implements ContainerService {

    protected ManagerIdentityService identityService;
    protected AssetDatapointService datapointService;
    protected AssetStorageService assetStorageService;

    @Override
    public void init(Container container) throws Exception {
        ManagerWebService webService = container.getService(ManagerWebService.class);
        TimerService timerService = container.getService(TimerService.class);
        identityService = container.getService(ManagerIdentityService.class);
        datapointService = container.getService(AssetDatapointService.class);
        assetStorageService = container.getService(AssetStorageService.class);

        webService.addApiSingleton(new DeviceChallengesResourceImpl(timerService, identityService, datapointService, assetStorageService));

        webService.addApiSingleton(new DeviceResourceImpl(timerService, identityService, assetStorageService));

        webService.addApiSingleton(new UserChallengesResourceImpl(timerService, identityService, assetStorageService));

        webService.addApiSingleton(new UserDistrictResourceImpl(timerService, identityService, assetStorageService));

        webService.addApiSingleton(new DeviceCharacteristicsResourceImpl(timerService, identityService, assetStorageService));

        webService.addApiSingleton(new UserRolesResourceImpl(timerService, identityService, assetStorageService));

        webService.addApiSingleton(new UserPeakPointsResourceImpl(timerService, identityService, assetStorageService));

        webService.addApiSingleton(new UserAccountResourceImpl(timerService, identityService));
    }

    @Override
    public void start(Container container) throws Exception {

    }

    @Override
    public void stop(Container container) throws Exception {

    }
}
