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
import type { Asset, Attribute } from "@openremote/model";

export type OurgridMeterAsset = Asset;

export enum OgStateColor {
  GREEN = "#00AA45",
  RED = "#F44F1A",
}

export enum OgStateColorRGB {
  GREEN = "0, 170, 69",
  RED = "244, 79, 26",
}

export enum OgMeterChallengeState {
  JOIN_CHALLENGE = "joinChallenge",
  JOINED_CHALLENGE = "joinedChallenge",
  ACTIVE_CHALLENGE = "activeChallenge",
  NO_CHALLENGE = "noChallenge",
}

export enum OgMeterConnectedState {
  CONNECTED = "connected",
  DISCONNECTED = "disconnected",
}

export enum OgVehicleBrand {
  AUDI = "vehicleBrands.AUDI",
  HYUNDAI = "vehicleBrands.HYUNDAI",
  KIA = "vehicleBrands.KIA",
  OPEL = "vehicleBrands.OPEL",
  PEUGEOT = "vehicleBrands.PEUGEOT",
  RENAULT = "vehicleBrands.RENAULT",
  TESLA = "vehicleBrands.TESLA",
  VOLKSWAGEN = "vehicleBrands.VOLKSWAGEN",
  VOLKSWAGEN_ID = "vehicleBrands.VOLKSWAGEN_ID",
  VOLVO = "vehicleBrands.VOLVO",
  OTHER = "vehicleBrands.other",
}

export enum OgVehicleChargerBrand {
  TESLA = "vehicleChargerBrands.TESLA",
  VOLKSWAGEN_ID = "vehicleChargerBrands.VOLKSWAGEN_ID",
  OTHER = "vehicleChargerBrands.other",
}

export enum OgHeatPumpBrand {
  RESIDEO_HONEYWELL = "heatPumpBrands.RESIDEO_HONEYWELL",
  TOON = "heatPumpBrands.TOON",
  NEST = "heatPumpBrands.NEST",
  OTHER = "heatPumpBrands.other",
}

export enum OgBatteryBrand {
  MYGRID = "batteryBrands.MYGRID",
  LG = "batteryBrands.LG",
  SONNEN_BATTERIE = "batteryBrands.SONNEN_BATTERIE",
  TESLA_POWERWALL = "batteryBrands.TESLA_POWERWALL",
  OTHER = "batteryBrands.other",
}

export enum OgHomeAutomationBrand {
  HOME_ASSISTANT = "homeAutomationBrands.HOME_ASSISTANT",
  OPENHAB = "homeAutomationBrands.OPENHAB",
}

export function getVehicleBrandAppUrl(brand?: OgVehicleBrand, store?: "google" | "apple"): string | undefined {
  if (store === "google") {
    switch (brand) {
      case OgVehicleBrand.AUDI:
        return "https://play.google.com/store/apps/details?id=de.myaudi.mobile.assistant";
      case OgVehicleBrand.HYUNDAI:
        return "https://play.google.com/store/apps/details?id=com.hyundai.bluelink.eu.ux20";
      case OgVehicleBrand.KIA:
        return "https://play.google.com/store/apps/details?id=com.kia.oneapp.eu";
      case OgVehicleBrand.OPEL:
        return "https://play.google.com/store/apps/details?id=com.psa.mym.myopel";
      case OgVehicleBrand.PEUGEOT:
        return "https://play.google.com/store/apps/details?id=com.psa.mym.mypeugeot";
      case OgVehicleBrand.RENAULT:
        return "https://play.google.com/store/apps/details?id=com.renault.myrenault.one.fr";
      case OgVehicleBrand.TESLA:
        return "https://play.google.com/store/apps/details?id=com.teslamotors.tesla";
      case OgVehicleBrand.VOLKSWAGEN:
      case OgVehicleBrand.VOLKSWAGEN_ID:
        return "https://play.google.com/store/apps/details?id=com.vw.carnet.release";
      case OgVehicleBrand.VOLVO:
        return "https://play.google.com/store/apps/details?id=se.volvo.vcc";
      default:
    }
  } else if (store === "apple") {
    switch (brand) {
      case OgVehicleBrand.AUDI:
        return "https://apps.apple.com/nl/app/myaudi/id440464115";
      case OgVehicleBrand.HYUNDAI:
        return "https://apps.apple.com/nl/app/hyundai-bluelink-europe/id1565286187";
      case OgVehicleBrand.KIA:
        return "https://apps.apple.com/nl/app/kia-app/id6740517042";
      case OgVehicleBrand.OPEL:
        return "https://apps.apple.com/nl/app/myopel/id1439342035";
      case OgVehicleBrand.PEUGEOT:
        return "https://apps.apple.com/nl/app/mypeugeot-app/id1021587274";
      case OgVehicleBrand.RENAULT:
        return "https://apps.apple.com/nl/app/my-renault/id1440073013";
      case OgVehicleBrand.TESLA:
        return "https://apps.apple.com/nl/app/tesla/id582007913";
      case OgVehicleBrand.VOLKSWAGEN:
      case OgVehicleBrand.VOLKSWAGEN_ID:
        return "https://apps.apple.com/nl/app/volkswagen/id1517566572";
      case OgVehicleBrand.VOLVO:
        return "https://apps.apple.com/nl/app/volvo-cars/id439635293";
      default:
    }
  }
}

export function getHeatPumpBrandAppUrl(brand?: OgHeatPumpBrand, store?: "google" | "apple"): string | undefined {
  if (store === "google") {
    switch (brand) {
      case OgHeatPumpBrand.RESIDEO_HONEYWELL:
        return "https://play.google.com/store/apps/details?id=com.honeywell.android.lyric";
      case OgHeatPumpBrand.TOON:
        return "https://play.google.com/store/apps/details?id=com.quby.apps.hybrid";
      case OgHeatPumpBrand.NEST:
        return "https://play.google.com/store/apps/details?id=com.nest.android";
      default:
    }
  } else if (store === "apple") {
    switch (brand) {
      case OgHeatPumpBrand.RESIDEO_HONEYWELL:
        return "https://apps.apple.com/nl/app/resideo-smart-home/id880332077";
      case OgHeatPumpBrand.TOON:
        return "https://apps.apple.com/nl/app/toon/id1279188253";
      case OgHeatPumpBrand.NEST:
        return "https://apps.apple.com/nl/app/nest/id464988855";
      default:
    }
  }
}

export function getStateColorByDistrictPowerUsage(districtAttributes: { [p: string]: Attribute<any> }): OgStateColor {
  const powerPercentage: number | undefined = districtAttributes.powerImportPercentage?.value;
  const powerCriticalPercentage: number | undefined = districtAttributes.powerImportCriticalPercentage?.value;
  if (powerPercentage === undefined || powerCriticalPercentage === undefined) {
    console.error("Not all percentages were present.");
    return OgStateColor.RED;
  }
  return getStateColorByPowerValue(powerPercentage, powerCriticalPercentage);
}

export function getStateColorByPowerValue(percentage: number, threshold: number) {
  return percentage > threshold ? OgStateColor.RED : OgStateColor.GREEN;
}

export function getStateColorByPowerValueRGB(percentage: number, threshold: number) {
  return percentage > threshold ? OgStateColorRGB.RED : OgStateColorRGB.GREEN;
}

export async function doAnimation(elem: Element, cssClass: string, timeout: number): Promise<void> {
  elem.classList.add(cssClass);
  await new Promise((resolve) => setTimeout(resolve, timeout));
  elem.classList.remove(cssClass);
}
