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
export class Constants {
  // Attribute names
  static CHALLENGE_DURATION_ATTRIBUTE: string = "challengeDuration";
  static CHALLENGE_WAIT_ATTRIBUTE: string = "challengeWait";
  static CHALLENGE_POINT_CURRENT_ATTRIBUTE: string = "challengePointsCurrent";
  static CHALLENGE_POINT_INTERVAL_ATTRIBUTE: string = "challengeEarnPointInterval";
  static CHALLENGE_POINT_MAX_CURRENTLY_ATTRIBUTE: string = "challengePointsTotal";
  static CHALLENGE_POINTS_ATTRIBUTE: string = "challengePoints";
  static CHALLENGE_START_TIME_ATTRIBUTE: string = "challengeStart";
  static CHALLENGE_JOINED_ATTRIBUTE: string = "challengesJoined";
  static CHALLENGE_END_TIME_ATTRIBUTE: string = "challengeEnd";

  static METER_POWER_ATTRIBUTE: string = "power";
  static METER_POWER_MAX_ATTRIBUTE: string = "powerMax";
  static METER_PEAK_POINTS_ATTRIBUTE: string = "peakPoints";
  static METER_PEAK_DAY_POINTS_ATTRIBUTE: string = "peakPointsDay";

  // Local storage related
  static LOCALSTORAGE_LAST_CHALLENGE_COMPLETED_KEY: string = "lastChallengeCompleted";

  // URL parameters related
  static CHALLENGE_NOTIFICATION_PARAMS_NAME: string = "challengeNotification";

  // EARN-E / ENODE related
  static AUTHORIZE_EV_URL: string =
    "https://earne.welvaart-it.com/public/ourgrid/linkuser/ev/{meterId}/ourgridXmEmF76TSf0j4dsymsDeKDukB9f393hCpP1Cz4v7YpH2pdYjDwrf6ePg7";
}
