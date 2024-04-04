import {TemplateResult, html} from 'lit';
import {customElement} from 'lit/decorators.js';
import {OgPanel} from '../components/og-panel';
import {getStatisticTemplate} from './panel-trophies';
import {DeviceCharacteristic, WellknownCharacteristics} from '@openremote/model';
import manager from '@openremote/core';
import {when} from 'lit/directives/when.js';
import {InputType} from '@openremote/or-mwc-components/or-mwc-input';
import {getHeatPumpBrandAppUrl, getVehicleBrandAppUrl, OgHeatPumpBrand, OgVehicleBrand} from '../util/util';
import {Defaults} from '../util/defaults';

@customElement('panel-challenge-tips')
export class PanelChallengeTips extends OgPanel {

    public heading = html`<or-translate value="panel_tips.heading"></or-translate>`;
    public subtitle = html`<or-translate value="panel_tips.subtitle"></or-translate>`;
    public expandable = true;
    public expanded = true;
    public dark = true;
    public fullWidth = true;

    protected async getHouseholdCharacteristics(): Promise<Map<string, DeviceCharacteristic>> {
        try {
            const data = (await manager.rest.api.DeviceCharacteristicsResource.getCharacteristics()).data;
            return new Map<string, DeviceCharacteristic>(data.map(d => [d.id, d]));
        } catch (e) {
            console.error(e);
            return new Map<string, DeviceCharacteristic>();
        }
    }

    protected async getPanelContent(): Promise<TemplateResult> {
        const characteristics = await this.getHouseholdCharacteristics();
        const check = (key): boolean => {
            const characteristic = characteristics.get(key);
            return !characteristic || (characteristic.shown && !characteristic.active);
        };
        return html`
            <div style="padding: 24px 0;">
                <div style="display: flex; flex-direction: column; gap: 10px;">

                    ${when(check(WellknownCharacteristics.VEHICLE_CHARGER), () => {
                        const chargerCharacteristic: DeviceCharacteristic | undefined = characteristics.get(WellknownCharacteristics.VEHICLE_CHARGER);
                        const vehicleCharacteristic: DeviceCharacteristic | undefined = characteristics.get(WellknownCharacteristics.ELECTRIC_VEHICLE);
                        const brandUrl = this.getBrandAppUrl(vehicleCharacteristic);
                        const hasButton = chargerCharacteristic && vehicleCharacteristic && brandUrl !== undefined;
                        return html`
                            <div class="background-tint" style="position: relative; ${hasButton ? 'margin-bottom: 32px;' : undefined}">
                                <div style="padding: ${hasButton ? '24px 24px 48px 24px' : '24px'};">
                                    ${getStatisticTemplate('images/car-charging.svg', true, html`
                                        <div style="display: flex; flex-direction: column;">
                                            <span class="statistic-medium" style="color: var(--og-color-danger)">
                                                ${`-${chargerCharacteristic?.wattsSaved || Defaults.TIPS_VEHICLE_CHARGER_WATT_SAVED}W`}
                                            </span>
                                            <span class="text-primary"><or-translate value="panel_tips.text2"></or-translate></span>
                                        </div>
                                    `)}
                                </div>
                                ${when(hasButton, () => html`
                                    <div style="position: absolute; display: block; bottom: -24px; width: calc(100% - 48px); left: 24px;">
                                        <a href="${brandUrl}" target="_blank">
                                            <og-input .type="${InputType.BUTTON}" .label="${`goToVehicleBrandApp_${vehicleCharacteristic.brand}`}" fullWidth raised rounded comfortable
                                                      style="width: 100%; --or-mwc-input-color: var(--og-color-neutral); --or-mwc-input-text-color: var(--og-color-primary-dark)"
                                            ></og-input>
                                        </a>
                                    </div>
                                `)}
                            </div>
                        `;
                    })}

                    ${when(check(WellknownCharacteristics.HEAT_PUMP), () => {
                        const heatPumpCharacteristic: DeviceCharacteristic | undefined = characteristics.get(WellknownCharacteristics.HEAT_PUMP);
                        const brandUrl = this.getBrandAppUrl(heatPumpCharacteristic);
                        const hasButton = heatPumpCharacteristic && brandUrl !== undefined;
                        return html`
                            <div class="background-tint" style="position: relative; ${hasButton ? 'margin-bottom: 32px;' : undefined}">
                                <div style="padding: ${hasButton ? '24px 24px 48px 24px' : '24px'};">
                                    ${getStatisticTemplate('images/house-temperature.svg', true, html`
                                        <div style="display: flex; flex-direction: column;">
                                        <span class="statistic-medium" style="color: var(--og-color-danger)">
                                            ${`-${characteristics.get(WellknownCharacteristics.HEAT_PUMP)?.wattsSaved || Defaults.TIPS_HEAT_PUMP_WATT_SAVED}W`}
                                        </span>
                                            <span class="text-primary"><or-translate value="panel_tips.text4"></or-translate></span>
                                        </div>
                                    `)}
                                </div>
                                ${when(hasButton, () => html`
                                    <div style="position: absolute; display: block; bottom: -24px; width: calc(100% - 48px); left: 24px;">
                                        <a href="${brandUrl}" target="_blank">
                                            <og-input .type="${InputType.BUTTON}" .label="${`goToHeatPumpBrandApp_${heatPumpCharacteristic.brand}`}" fullWidth raised rounded comfortable
                                                      style="width: 100%; --or-mwc-input-color: var(--og-color-neutral); --or-mwc-input-text-color: var(--og-color-primary-dark)"
                                            ></og-input>
                                        </a>
                                    </div>
                                `)}
                            </div>
                        `;
                    })}

                    <div class="background-tint" style="padding: 24px;">
                        ${getStatisticTemplate('images/kettle-steam-outline.svg', true, html`
                            <div style="display: flex; flex-direction: column;">
                                <span class="statistic-medium" style="color: var(--og-color-danger)">-2000W</span>
                                <span class="text-primary"><or-translate value="panel_tips.text5"></or-translate></span>
                            </div>
                        `)}
                    </div>

                    <div class="background-tint" style="padding: 24px;">
                        ${getStatisticTemplate('images/washing-machine.svg', true, html`
                            <div style="display: flex; flex-direction: column;">
                                <span class="statistic-medium" style="color: var(--og-color-danger)">-2000W</span>
                                <span class="text-primary"><or-translate value="panel_tips.text3"></or-translate></span>
                            </div>
                        `)}
                    </div>

                    <div class="background-tint" style="padding: 24px;">
                        ${getStatisticTemplate('images/cooking.svg', true, html`
                            <div style="display: flex; flex-direction: column;">
                                <span class="statistic-medium" style="color: var(--og-color-danger)">-500W</span>
                                <span class="text-primary"><or-translate value="panel_tips.text1"></or-translate></span>
                            </div>
                        `)}
                    </div>
                </div>
            </div>
        `;
    }

    protected getBrandAppUrl(characteristic: DeviceCharacteristic): string | undefined {
        const store: 'google' | 'apple' | undefined = manager.console.isMobile ? (manager.console.shellAndroid ? 'google' : (manager.console.shellApple ? 'apple' : undefined)) : 'google';
        switch (characteristic.id) {
            case WellknownCharacteristics.ELECTRIC_VEHICLE: return getVehicleBrandAppUrl(characteristic.brand as OgVehicleBrand | undefined, store);
            case WellknownCharacteristics.HEAT_PUMP: return getHeatPumpBrandAppUrl(characteristic.brand as OgHeatPumpBrand | undefined, store);
            default: return;
        }
    }
}
