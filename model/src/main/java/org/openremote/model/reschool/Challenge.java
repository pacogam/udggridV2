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
package org.openremote.model.reschool;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Challenge {

  protected Date startDate;
  protected Date endDate;
  protected Date joinedAt;
  protected Boolean inProgress;
  protected Integer points;
  protected Integer pointsCurrentMax;
  protected Integer pointsTotalMax;

  public Challenge() {}

  public Challenge setStartDate(Date startDate) {
    this.startDate = startDate;
    return this;
  }

  public Challenge setStartDate(String stringDate) throws ParseException {
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    this.startDate = sdf.parse(stringDate);
    return this;
  }

  public Challenge setEndDate(Date endDate) {
    this.endDate = endDate;
    return this;
  }

  public Challenge setEndDate(String stringDate) throws ParseException {
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    this.endDate = sdf.parse(stringDate);
    return this;
  }

  public Challenge setJoinDate(Date joinedAt) {
    this.joinedAt = joinedAt;
    return this;
  }

  public Challenge setJoinDate(String stringDate) throws ParseException {
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    this.joinedAt = sdf.parse(stringDate);
    return this;
  }

  public Challenge setInProgress(Boolean progress) {
    this.inProgress = progress;
    return this;
  }

  public Challenge setPoints(Integer points) {
    this.points = points;
    return this;
  }

  public Challenge setPointsCurrentMax(Integer pointsCurrentMax) {
    this.pointsCurrentMax = pointsCurrentMax;
    return this;
  }

  public Challenge setPointsTotalMax(Integer pointsTotalMax) {
    this.pointsTotalMax = pointsTotalMax;
    return this;
  }

  public Date getStartDate() {
    return startDate;
  }

  public Date getEndDate() {
    return endDate;
  }

  public Date getJoinedAt() {
    return joinedAt;
  }

  public Boolean getInProgress() {
    return inProgress;
  }

  public Integer getPoints() {
    return points;
  }

  public Integer getPointsCurrentMax() {
    return pointsCurrentMax;
  }

  public Integer getPointsTotalMax() {
    return pointsTotalMax;
  }
}
