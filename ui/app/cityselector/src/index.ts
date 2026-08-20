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
import { CitySelectorApp } from "./city-selector-app";
import { LOCALSTORAGE_KEY_CITY, navigateToCity } from "./util";
import type { OurGridGatewayCity } from "model";
import { CitySelectorLoader } from "./city-selector-loader";

declare let CONFIG_URL_PREFIX: string;
export const DEFAULT_LANGUAGE: string = "nl";

// Try and load the app config from JSON and if anything is found amalgamate it with default
const configURL = (CONFIG_URL_PREFIX || "") + "/ourgrid_config.json";
const langFolder = "locales";

// Force redirect to specified URL, and skip city selector
/* if(FORCE_REDIRECT_URL) {
    navigateToCity(window, {
        ourGridUrl: FORCE_REDIRECT_URL,
        realm: FORCE_REDIRECT_REALM || "default"
    }, true);
} */

// Get query parameters
const urlParams = new URLSearchParams(window.location.search);
const redirect = urlParams.has("redirect") ? urlParams.get("redirect") === "true" : true;

// Check if city is already selected
const cityStr = window.localStorage.getItem(LOCALSTORAGE_KEY_CITY) as string | undefined;
if (redirect && cityStr) {
  const city = JSON.parse(cityStr) as OurGridGatewayCity;
  navigateToCity(window, city);

  // In case navigating takes long, we show a loading page
  const loader = new CitySelectorLoader();
  document.body.appendChild(loader);
}

// if not, load the selector app
else {
  const app = new CitySelectorApp(CONFIG_URL_PREFIX, configURL, langFolder, DEFAULT_LANGUAGE);
  document.body.appendChild(app);
}
