import {Asset, Attribute} from '@openremote/model';

export type OurgridMeterAsset = Asset;

export enum OgStateColor {
    GREEN = '#00AA45', RED = '#F44F1A'
}

export enum OgMeterChallengeState {
    JOIN_CHALLENGE = 'joinChallenge', JOINED_CHALLENGE = 'joinedChallenge', ACTIVE_CHALLENGE = 'activeChallenge', NO_CHALLENGE = 'noChallenge'
}

export enum OgMeterConnectedState {
    CONNECTED = 'connected', DISCONNECTED = 'disconnected'
}

export enum OgVehicleBrand {
    TESLA = 'vehicleBrands.TESLA', VOLKSWAGEN_ID = 'vehicleBrands.VOLKSWAGEN_ID', OTHER = 'vehicleBrands.other'
}

export enum OgVehicleChargerBrand {
    TESLA = 'vehicleChargerBrands.TESLA', VOLKSWAGEN_ID = 'vehicleChargerBrands.VOLKSWAGEN_ID', OTHER = 'vehicleChargerBrands.other'
}

export enum OgHeatPumpBrand {
    RESIDEO_HONEYWELL = 'heatPumpBrands.RESIDEO_HONEYWELL', TOON = 'heatPumpBrands.TOON', NEST = 'heatPumpBrands.NEST', OTHER = 'heatPumpBrands.other'
}

export enum OgBatteryBrand {
    MYGRID = 'batteryBrands.MYGRID', LG = 'batteryBrands.LG', SONNEN_BATTERIE = 'batteryBrands.SONNEN_BATTERIE', TESLA_POWERWALL = 'batteryBrands.TESLA_POWERWALL', OTHER = 'batteryBrands.other'
}

export function getVehicleBrandAppUrl(brand?: OgVehicleBrand, store?: 'google' | 'apple'): string | undefined {
    if (store === 'google') {
        switch (brand) {
            case OgVehicleBrand.TESLA:
                return 'https://play.google.com/store/apps/details?id=com.teslamotors.tesla';
            case OgVehicleBrand.VOLKSWAGEN_ID:
                return 'https://play.google.com/store/apps/details?id=com.vw.carnet.release';
            default:
                return;
        }
    } else if (store === 'apple') {
        switch (brand) {
            case OgVehicleBrand.TESLA:
                return 'https://apps.apple.com/nl/app/tesla/id582007913';
            case OgVehicleBrand.VOLKSWAGEN_ID:
                return 'https://apps.apple.com/nl/app/volkswagen/id1517566572';
            default:
                return;
        }
    }
}

export function getHeatPumpBrandAppUrl(brand?: OgHeatPumpBrand, store?: 'google' | 'apple'): string | undefined {
    if (store === 'google') {
        switch (brand) {
            case OgHeatPumpBrand.RESIDEO_HONEYWELL:
                return 'https://play.google.com/store/apps/details?id=com.honeywell.android.lyric';
            case OgHeatPumpBrand.TOON:
                return 'https://play.google.com/store/apps/details?id=com.quby.apps.hybrid';
            case OgHeatPumpBrand.NEST:
                return 'https://play.google.com/store/apps/details?id=com.nest.android';
            default:
                return;
        }
    } else if (store === 'apple') {
        switch (brand) {
            case OgHeatPumpBrand.RESIDEO_HONEYWELL:
                return 'https://apps.apple.com/nl/app/resideo-smart-home/id880332077';
            case OgHeatPumpBrand.TOON:
                return 'https://apps.apple.com/nl/app/toon/id1279188253';
            case OgHeatPumpBrand.NEST:
                return 'https://apps.apple.com/nl/app/nest/id464988855';
            default:
                return;
        }
    }
}

export function getStateColorByDistrictPowerUsage(districtAttributes: { [p: string]: Attribute<any> }): OgStateColor {
    const powerPercentage: number | undefined = districtAttributes['powerImportPercentage']?.value;
    const powerCriticalPercentage: number | undefined = districtAttributes['powerImportCriticalPercentage']?.value;
    if (powerPercentage === undefined || powerCriticalPercentage === undefined) {
        console.error('Not all percentages were present.');
        return OgStateColor.RED;
    }
    return getStateColorByPowerValue(powerPercentage, powerCriticalPercentage);
}

export function getStateColorByPowerValue(percentage: number, threshold: number) {
    return (percentage > threshold) ? OgStateColor.RED : OgStateColor.GREEN;
}

export async function doAnimation(elem: Element, cssClass: string, timeout: number): Promise<void> {
    elem.classList.add(cssClass);
    await new Promise(resolve => setTimeout(resolve, timeout));
    elem.classList.remove(cssClass);
}
