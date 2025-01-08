import {TemplateResult, html} from "lit";
import {customElement, query} from "lit/decorators.js";
import {OgDataPanel} from "../components/og-data-panel";
import {getStatisticTemplate} from "./panel-trophies";
import {DeviceCharacteristic, WellknownCharacteristics} from "model";
import manager from "@openremote/core";
import {when} from "lit/directives/when.js";
import {InputType, OrInputChangedEvent} from "@openremote/or-mwc-components/or-mwc-input";
import {getHeatPumpBrandAppUrl, getVehicleBrandAppUrl, OgHeatPumpBrand, OgVehicleBrand} from "../util/util";
import {Defaults} from '../util/defaults';
import {showSnackbar} from "../components/og-snackbar";
import {i18next} from "@openremote/or-translate";
import {OgInput} from "../components/og-input";
import { styleMap } from "lit/directives/style-map.js";
import rest from "rest";

@customElement('panel-challenge-tips')
export class PanelChallengeTips extends OgDataPanel {

    protected AUTOMATIC_CONTROL_ATTRIBUTE_NAME = "automaticControl";
    protected ACTION_BUTTON_ATTRIBUTE_NAME = "challengeActionButton";
    protected POWER_EXPORT_MAX_ATTRIBUTE_NAME = "powerExportMax";

    public heading = html`<or-translate value="panel_tips.heading"></or-translate>`;
    public subtitle = html`<or-translate value="panel_tips.subtitle"></or-translate>`;
    public expandable = true;
    public expanded = true;
    public dark = true;
    public fullWidth = true;

    @query('#battery-btn')
    protected _batteryButtonElem: OgInput;

    protected async getHouseholdCharacteristics(): Promise<Map<string, DeviceCharacteristic>> {
        try {
            const data = (await rest.api.DeviceCharacteristicsResource.getCharacteristics()).data;
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
                    
                    ${when(check(WellknownCharacteristics.BATTERY), () => {
                        const automaticControl: boolean = this.batteryAsset?.attributes?.[this.AUTOMATIC_CONTROL_ATTRIBUTE_NAME]?.value || false;
                        const challengeActionButton: boolean = this.batteryAsset?.attributes?.[this.ACTION_BUTTON_ATTRIBUTE_NAME]?.value || false;
                        const isManuallyActivated: boolean = automaticControl !== challengeActionButton;
                        const powerExportMaxKW: number = this.batteryAsset?.attributes?.[this.POWER_EXPORT_MAX_ATTRIBUTE_NAME]?.value || 1;
                        const unknownBattery = !this.batteryAsset;
                        const hasButton = !!this.batteryAsset;
                        let label: string;
                        if(unknownBattery) {
                            label = "panel_tips.text6-alt";
                        } else if(automaticControl) {
                            label = isManuallyActivated ? "panel_tips.text6-on-inactive" : "panel_tips.text6-on";
                        } else {
                            label = isManuallyActivated ? "panel_tips.text6-off-active" : "panel_tips.text6-off";
                        }
                        return html`
                            <div class="background-tint" style="position: relative; ${hasButton ? 'margin-bottom: 32px;' : undefined}">
                                <div style="padding: ${hasButton ? '24px 24px 48px 24px' : '24px'};">
                                    ${getStatisticTemplate('images/battery-power-charge.svg', true, html`
                                        <div style="display: flex; flex-direction: column;">
                                        <span class="statistic-medium" style="color: var(--og-color-danger)">
                                            ${`-${Math.round(powerExportMaxKW * 1000)}W`}
                                        </span>
                                            <span class="text-primary">
                                                <or-translate value="${label}"></or-translate>
                                            </span>
                                        </div>
                                    `)}
                                </div>
                                ${when(hasButton, () => html`
                                    <div style="position: absolute; display: block; bottom: -24px; width: calc(100% - 48px); left: 24px;">
                                        ${this._getBatteryBtnTemplate(automaticControl, challengeActionButton, isManuallyActivated)}
                                    </div>
                                `)}
                            </div>
                        `;
                    })}

                    ${when(check(WellknownCharacteristics.VEHICLE_CHARGER), () => {
                        const chargerCharacteristic: DeviceCharacteristic | undefined = characteristics.get(WellknownCharacteristics.VEHICLE_CHARGER);
                        const vehicleCharacteristic: DeviceCharacteristic | undefined = characteristics.get(WellknownCharacteristics.ELECTRIC_VEHICLE);
                        const brandUrl = this._getBrandAppUrl(vehicleCharacteristic);
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
                        const brandUrl = this._getBrandAppUrl(heatPumpCharacteristic);
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

    protected _onAutomaticControlClick(_ev: OrInputChangedEvent, newValue?: boolean) {
        if(!newValue) {
            newValue = !(this.batteryAsset?.attributes?.[this.AUTOMATIC_CONTROL_ATTRIBUTE_NAME]?.value || false);
        }
        this._setActionButtonAttribute(newValue);
    }

    /**
     * Returns a template with a button for manually controlling the charging of the battery.
     * Depending on, for example, {@link automaticControl} and {@link isManuallyActivated}`, it will use a different color and text.
     */
    protected _getBatteryBtnTemplate(automaticControl: boolean, challengeActionButton: boolean, isManuallyActivated: boolean) {
        let label: string;
        let styles: any;
        if(isManuallyActivated) {
            label = automaticControl ? 'panel_tips.action6-success-off' : 'panel_tips.action6-success-on';
            styles = {
                "width": "100%",
                "--or-mwc-input-color": automaticControl ? "var(--og-color-secondary)" : "var(--og-color-success)",
                "--or-mwc-input-text-color": "var(--og-color-primary)"
            };
        } else {
            label = automaticControl ? 'panel_tips.action6-on' : 'panel_tips.action6-off';
            styles = {
                "width": "100%",
                "--or-mwc-input-color": "var(--og-color-neutral)",
                "--or-mwc-input-text-color": "var(--og-color-primary-dark)"
            };
        }
        const readonly = isManuallyActivated;
        return html`
            <og-input id="battery-btn" .type="${InputType.BUTTON}" fullWidth raised rounded comfortable
                      .label="${label}" .readonly="${readonly}"
                      style="${styleMap(styles)}"
                      @or-mwc-input-changed="${(ev: OrInputChangedEvent) => this._onAutomaticControlClick(ev)}"
            ></og-input>
        `;
    }

    /**
     * Internal function that returns the brand URL, based on the console device.
     */
    protected _getBrandAppUrl(characteristic: DeviceCharacteristic): string | undefined {
        const store: 'google' | 'apple' | undefined = manager.console.isMobile ? (manager.console.shellAndroid ? 'google' : (manager.console.shellApple ? 'apple' : undefined)) : 'google';
        switch (characteristic?.id) {
            case WellknownCharacteristics.ELECTRIC_VEHICLE: return getVehicleBrandAppUrl(characteristic.brand as OgVehicleBrand | undefined, store);
            case WellknownCharacteristics.HEAT_PUMP: return getHeatPumpBrandAppUrl(characteristic.brand as OgHeatPumpBrand | undefined, store);
            default: return;
        }
    }

    /**
     * Updates the 'automaticControl' attribute on the battery asset using the HTTP API.
     * It uses the custom endpoint in {@link DeviceBatteryResource}, where all "user access checks" are performed.
     * Shows a snackbar afterward, and will update the button state automatically.
     */
    protected _setActionButtonAttribute(newState: boolean): void {
        if (this.meterAsset) {

            rest.api.DeviceBatteryResource.actionButton({meterId: this.meterAsset.id, buttonState: newState}).then(() => {
                if (newState) {
                    showSnackbar(undefined, i18next.t("panel_tips.action6-success-on"));
                } else {
                    showSnackbar(undefined, i18next.t("panel_tips.action6-success-off"));
                }
            }).catch(() => {
                showSnackbar(undefined, i18next.t("errorOccurred"));
            });
        } else {
            showSnackbar(undefined, i18next.t("errorOccurred"));
            console.warn("Could not toggle automatic control; assets are not cached correctly.");
        }
    }
}
