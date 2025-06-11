package org.openremote.manager.reschool.rest;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.openremote.agent.custom.ourgrid.OurgridChallengesAsset;
import org.openremote.agent.custom.ourgrid.OurgridMeterAsset;
import org.openremote.container.timer.TimerService;
import org.openremote.manager.asset.AssetStorageService;
import org.openremote.manager.datapoint.AssetDatapointService;
import org.openremote.manager.security.ManagerIdentityService;
import org.openremote.manager.web.ManagerWebResource;
import org.openremote.model.asset.UserAssetLink;
import org.openremote.model.http.RequestParams;
import org.openremote.model.query.AssetQuery;
import org.openremote.model.query.filter.RealmPredicate;
import org.openremote.model.reschool.Challenge;
import org.openremote.model.reschool.DeviceChallengesResource;

import java.util.*;

import static jakarta.ws.rs.core.Response.Status.*;

public class DeviceChallengesResourceImpl extends ManagerWebResource implements DeviceChallengesResource {

    protected final DeviceChallengesService deviceChallengesService;
    protected final AssetStorageService assetStorageService;

    public DeviceChallengesResourceImpl(TimerService timerService, ManagerIdentityService identityService, AssetDatapointService datapointService, AssetStorageService assetStorageService) {
        super(timerService, identityService);
        this.assetStorageService = assetStorageService;
        this.deviceChallengesService = new DeviceChallengesService(timerService, assetStorageService, datapointService);
    }

    @Override
    public Response joinChallenge(RequestParams params, DeviceJoinChallengeDetails joinChallengeDetails) {
        if (!isAuthenticated()) {
            throw new WebApplicationException(UNAUTHORIZED);
        }

        // Query the meter asset with the same id.
        OurgridMeterAsset meterAsset = (OurgridMeterAsset) assetStorageService.find(new AssetQuery()
                .realm(new RealmPredicate(getAuthenticatedRealmName()))
                .types(OurgridMeterAsset.class)
                .ids(joinChallengeDetails.meterId)
        );
        if (meterAsset == null) {
            throw new WebApplicationException(NOT_FOUND);
        }

        // Check if the meter is linked to the user requesting it.
        Collection<UserAssetLink> userLinksOfAsset = assetStorageService.findUserAssetLinks(meterAsset.getRealm(), getUserId(), meterAsset.getId());
        if (userLinksOfAsset == null || userLinksOfAsset.isEmpty()) {
            throw new WebApplicationException(FORBIDDEN);
        }

        // Query the challenge asset with the same id.
        OurgridChallengesAsset challengeAsset = (OurgridChallengesAsset) assetStorageService.find(new AssetQuery()
                .select(new AssetQuery.Select().excludeAttributes())
                .realm(new RealmPredicate(getAuthenticatedRealmName()))
                .types(OurgridChallengesAsset.class)
        );
        if (challengeAsset == null) {
            throw new WebApplicationException(NOT_FOUND);
        }

        // Check if the meter has the possibility to join the challenge
        Optional<?> meterChallengeStatus = meterAsset.getChallengeStatus();
        if (meterChallengeStatus.isEmpty() || meterChallengeStatus.get() != OurgridMeterAsset.ChallengeStatusValueType.joinChallenge) {
            throw new WebApplicationException(FORBIDDEN);
        }


        // Join challenge
        try {
            meterAsset = meterAsset.setChallengeJoinStatus(true);
            assetStorageService.merge(meterAsset);

        } catch (Exception e) {
            throw new WebApplicationException(INTERNAL_SERVER_ERROR);
        }

        return Response.ok().build();
    }

    @Override
    public Challenge getCurrent(RequestParams params) {
        if (!isAuthenticated()) {
            throw new WebApplicationException(UNAUTHORIZED);
        }

        // Query the meter asset with the same linked user
        OurgridMeterAsset meterAsset = (OurgridMeterAsset) assetStorageService.find(new AssetQuery()
                .realm(new RealmPredicate(getAuthenticatedRealmName()))
                .types(OurgridMeterAsset.class)
                .userIds(getUserId())
        );
        if (meterAsset == null) {
            throw new WebApplicationException(NOT_FOUND);
        }

        // Query the challenge asset with the same linked user
        OurgridChallengesAsset challengesAsset = (OurgridChallengesAsset) assetStorageService.find(new AssetQuery()
                .realm(new RealmPredicate(getAuthenticatedRealmName()))
                .types(OurgridChallengesAsset.class)
                .userIds(getUserId())
        );
        if (challengesAsset == null) {
            throw new WebApplicationException(NOT_FOUND);
        }

        // Check if the meter has joined a challenge to return
        Optional<?> meterChallengeStatus = meterAsset.getChallengeStatus();
        Collection<OurgridMeterAsset.ChallengeStatusValueType> allowed = Arrays.asList(
                OurgridMeterAsset.ChallengeStatusValueType.activeChallenge,
                OurgridMeterAsset.ChallengeStatusValueType.joinedChallenge
        );
        if (meterChallengeStatus.isEmpty() || !allowed.contains((OurgridMeterAsset.ChallengeStatusValueType) meterChallengeStatus.get())) {
            throw new WebApplicationException(NOT_FOUND);
        }

        // Attribute checks
        if (challengesAsset.getStartDate().isEmpty() || challengesAsset.getEndDate().isEmpty()) {
            throw new WebApplicationException(INTERNAL_SERVER_ERROR);
        }

        // Get points earned of current challenge
        /*Integer points = deviceChallengesService.getCurrentPointsOfCurrentChallenge(meterAsset, challengesAsset);*/

        // Fill the challenge object to return
       /* try {
            return new Challenge()
                    .setStartDate(challengesAsset.getStartDate().get())
                    .setEndDate(challengesAsset.getEndDate().get())
                    .setPoints(points);

        } catch (ParseException e) {
            throw new WebApplicationException(INTERNAL_SERVER_ERROR);
        }*/
        return new Challenge();
    }

    @Override
    public Response getHistory(RequestParams params, DeviceChallengeHistoryDetails challengeHistoryDetails) {
        if (!isAuthenticated()) {
            throw new WebApplicationException(UNAUTHORIZED);
        }

        // Query the meter asset with the same linked user
        OurgridMeterAsset meterAsset = (OurgridMeterAsset) assetStorageService.find(new AssetQuery()
                .realm(new RealmPredicate(getAuthenticatedRealmName()))
                .types(OurgridMeterAsset.class)
                .userIds(getUserId())
        );
        if (meterAsset == null) {
            throw new WebApplicationException(NOT_FOUND);
        }

        // Query the challenge asset with the same linked user
        OurgridChallengesAsset challengesAsset = (OurgridChallengesAsset) assetStorageService.find(new AssetQuery()
                .realm(new RealmPredicate(getAuthenticatedRealmName()))
                .types(OurgridChallengesAsset.class)
                .userIds(getUserId())
        );
        if (challengesAsset == null) {
            throw new WebApplicationException(NOT_FOUND);
        }

        // Query all the challenges a user has joined in the past
        try {
            return Response.ok(this.deviceChallengesService
                    .getMeterChallengeHistory(meterAsset, challengesAsset, challengeHistoryDetails.startTimestamp, challengeHistoryDetails.endTimestamp)
                    .toArray(new Challenge[0])
            ).build();

        } catch (Exception e) {
            throw new WebApplicationException(e.getMessage(), INTERNAL_SERVER_ERROR);
        }
    }
}
