/*
 * Copyright 2026, OpenRemote Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package org.openremote.manager.reschool.rest;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.IntStream;
import org.openremote.agent.custom.ourgrid.OurgridChallengesAsset;
import org.openremote.agent.custom.ourgrid.OurgridMeterAsset;
import org.openremote.container.timer.TimerService;
import org.openremote.manager.asset.AssetStorageService;
import org.openremote.manager.datapoint.AssetDatapointService;
import org.openremote.manager.rules.RulesetStorageService;
import org.openremote.model.datapoint.ValueDatapoint;
import org.openremote.model.datapoint.query.AssetDatapointAllQuery;
import org.openremote.model.reschool.Challenge;

public class DeviceChallengesService {

  protected TimerService timerService;

  protected AssetStorageService assetStorageService;
  protected AssetDatapointService assetDatapointService;
  RulesetStorageService rulesetStorageService;

  public DeviceChallengesService(
      TimerService timerService,
      AssetStorageService assetStorageService,
      AssetDatapointService datapointService) {
    this.timerService = timerService;
    this.assetStorageService = assetStorageService;
    this.assetDatapointService = datapointService;
  }

  public Collection<Challenge> getMeterChallengeHistory(
      OurgridMeterAsset meterAsset,
      OurgridChallengesAsset challengesAsset,
      long startTime,
      long endTime)
      throws Exception {
    if (meterAsset == null || challengesAsset == null) {
      return new ArrayList<>();
    }

    // Query all challenges from the past
    Collection<Challenge> allChallenges =
        this.getTotalChallengeHistory(challengesAsset, startTime, endTime);

    // Query all timestamps of when a meter gains a point, because
    // at the start of the challenge this value gets updated as well.
    Collection<ValueDatapoint<?>> pointDatapoints =
        this.assetDatapointService.queryDatapoints(
            meterAsset.getId(),
            OurgridMeterAsset.CHALLENGE_POINTS.getName(),
            new AssetDatapointAllQuery(
                startTime, endTime) // TODO: Implement custom DatapointQuery which is more efficient
            );

    // Make a separate timestamp list for easier comparison
    Collection<Date> pointTimestamps =
        pointDatapoints.stream().map(dp -> new Date(dp.getTimestamp())).toList();

    // Loop through all challenges from the past, and check whether the timer has reset for the
    // meter.
    Collection<Challenge> joinedChallenges =
        allChallenges.stream()
            .filter(
                challenge ->
                    pointTimestamps.stream()
                        .anyMatch(
                            pointGainDate ->
                                (pointGainDate.after(challenge.getStartDate())
                                    && pointGainDate.before(challenge.getEndDate()))))
            .toList();

    // Get challenge wait to calculate the earliest moment a user can join the challenge
    Optional<Integer> challengeWaitMinutes = challengesAsset.getChallengeWait();
    if (challengeWaitMinutes.isEmpty()) {
      throw new RuntimeException("Challenge wait time is invalid");
    }
    // Aka startTime - challengeWaitMinutes in milliseconds - one additional minute
    long earliestJoinTime = startTime - (challengeWaitMinutes.get() * 60 * 1000) - 60000;

    Collection<ValueDatapoint<?>> joinDates =
        this.assetDatapointService.queryDatapoints(
            meterAsset.getId(),
            OurgridMeterAsset.CHALLENGES_JOINED.getName(),
            new AssetDatapointAllQuery(
                earliestJoinTime,
                endTime) // TODO: Implement custom DatapointQuery which is more efficient
            );

    // Add more details to the challenge data
    return joinedChallenges.stream()
        .map(
            (Challenge challenge) -> {

              // Use the queried CHALLENGE_POINTS to figure out the amount of points gained during
              // the challenge.
              // It compares the start- and end timestamps, and calculates the point difference.
              long startTimeMin = challenge.getStartDate().getTime();
              long endTimeMax =
                  challenge.getEndDate().getTime() + (60 * 1000); // Including a 60-second margin
              Double[] datapoints =
                  pointDatapoints.stream()
                      .filter(
                          dp ->
                              dp.getTimestamp() >= startTimeMin && dp.getTimestamp() <= endTimeMax)
                      .sorted(Comparator.comparing(ValueDatapoint::getTimestamp))
                      .map(dp -> (Double) dp.getValue())
                      .toArray(Double[]::new);

              int pointsGained =
                  (datapoints.length <= 1)
                      ? 0
                      : (datapoints[datapoints.length - 1].intValue() - datapoints[0].intValue());

              Optional<Date> joinDate =
                  joinDates.stream()
                      .filter(
                          dp ->
                              dp.getTimestamp() >= startTimeMin && dp.getTimestamp() <= endTimeMax)
                      .map(dp -> new Date(dp.getTimestamp()))
                      .findFirst();

              // Return challenge object
              return challenge.setPoints(pointsGained).setJoinDate(joinDate.orElse(null));
            })
        .toList();
  }

  public Collection<Challenge> getTotalChallengeHistory(
      OurgridChallengesAsset challengesAsset, long startTime, long endTime)
      throws RuntimeException {
    if (challengesAsset == null) {
      return new ArrayList<>();
    }

    var sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    // Query all start timestamps between start and end time
    List<Long> startTimestamps =
        this.assetDatapointService
            .queryDatapoints(
                challengesAsset.getId(),
                OurgridChallengesAsset.CHALLENGE_START.getName(),
                new AssetDatapointAllQuery(startTime, endTime))
            .stream()
            .map(
                (ValueDatapoint<?> dp) -> {
                  try {
                    return sdf.parse((String) dp.getValue()).getTime();
                  } catch (ParseException e) {
                    throw new RuntimeException(e);
                  }
                })
            .toList();

    // Query all end timestamps between start and end time
    List<Long> endTimestamps =
        this.assetDatapointService
            .queryDatapoints(
                challengesAsset.getId(),
                OurgridChallengesAsset.CHALLENGE_END.getName(),
                new AssetDatapointAllQuery(startTime, endTime))
            .stream()
            .map(
                (ValueDatapoint<?> dp) -> {
                  try {
                    return sdf.parse((String) dp.getValue()).getTime();
                  } catch (ParseException e) {
                    throw new RuntimeException(e);
                  }
                })
            .toList();

    // Return collection of challenges
    return IntStream.range(0, startTimestamps.size())
        .mapToObj(
            index ->
                new Challenge()
                    .setStartDate(
                        new Date(Objects.requireNonNullElse(startTimestamps.get(index), 0L)))
                    .setEndDate(new Date(Objects.requireNonNullElse(endTimestamps.get(index), 0L)))
                    .setInProgress(
                        startTimestamps.get(index) < timerService.getCurrentTimeMillis()
                            && endTimestamps.get(index) > timerService.getCurrentTimeMillis()))
        .toList();
  }
}
