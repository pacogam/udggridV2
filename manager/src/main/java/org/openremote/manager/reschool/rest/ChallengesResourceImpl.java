package org.openremote.manager.reschool.rest;

import jakarta.ws.rs.core.Response;
import org.openremote.container.timer.TimerService;
import org.openremote.manager.asset.AssetStorageService;
import org.openremote.manager.security.ManagerIdentityService;
import org.openremote.manager.web.ManagerWebResource;
import org.openremote.model.http.RequestParams;
import org.openremote.model.reschool.ChallengesResource;

public class ChallengesResourceImpl extends ManagerWebResource implements ChallengesResource {

    protected final AssetStorageService assetStorageService;

    public ChallengesResourceImpl(TimerService timerService, ManagerIdentityService identityService, AssetStorageService assetStorageService) {
        super(timerService, identityService);
        this.assetStorageService = assetStorageService;
    }

    @Override
    public Response getCurrent(RequestParams params) {
        return null;
    }

    @Override
    public Response getHistoryTotal(RequestParams params) {
        return null;
    }
}
