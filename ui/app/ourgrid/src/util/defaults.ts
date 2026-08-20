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
export class Defaults {
  // Challenge related
  static CHALLENGE_DURATION_MINUTES = 60;
  static CHALLENGE_INTERVAL_MINUTRES = 6;
  static CHALLENGE_WAIT_MINUTES = 15;

  // Challenge datapoint related
  static CHALLENGE_PROGRESS_DATAPOINT_INTERVAL_SECONDS = 30;
  static CHALLENGE_PROGRESS_DIGIT_AMOUNT = 3;

  // Challenge history related
  static HISTORY_FETCH_AMOUNT: number = 1;
  static HISTORY_FETCH_UNIT: string = "week";

  // Point earnings related
  static POINT_EXCHANGE_RATE_DECIMALS = 3;
  static POINT_EARNINGS_DECIMALS = 2;

  // Tips related
  static TIPS_HEAT_PUMP_WATT_SAVED: number = 3000;
  static TIPS_VEHICLE_CHARGER_WATT_SAVED: number = 7000;

  // Animation related
  static PEAK_NOTIFICATION_JOINED_TIMEOUT_MS = 3000;
  static HOUSEHOLD_CHARACTERISTICS_SAVE_BUTTON_TIMEOUT_MS = 5000;

  // Styling related
  static HIDE_HEADER_FROM_HEIGHT_PX: number = 240;
}
