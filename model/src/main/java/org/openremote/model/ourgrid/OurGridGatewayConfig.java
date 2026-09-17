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
package org.openremote.model.ourgrid;

public class OurGridGatewayConfig {

  /**
   * Location of the cities JSON file containing the cities that can be searched. Should be in a
   * hash format, such as;
   *
   * <pre>
   * {
   *     "My City Name": {
   *         "lang": "en"
   *     },
   *     "Another City": {
   *         "lang": "nl"
   *     }
   * }
   * </pre>
   */
  protected String searchCitiesFile;

  /** List of Cities with their respective URLs and realms the gateway should redirect to */
  protected OurGridGatewayCity[] cities;
}
