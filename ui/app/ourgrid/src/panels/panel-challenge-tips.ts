import {PropertyValues, TemplateResult, html} from 'lit';
import {customElement, query} from 'lit/decorators.js';
import {OgDataPanel} from "../components/og-data-panel";
import {getStatisticTemplate} from './panel-trophies';
import {Asset, DeviceCharacteristic, WellknownCharacteristics} from '@openremote/model';
import manager from '@openremote/core';
import {when} from 'lit/directives/when.js';
import {InputType, OrInputChangedEvent} from '@openremote/or-mwc-components/or-mwc-input';
import {getHeatPumpBrandAppUrl, getVehicleBrandAppUrl, OgHeatPumpBrand, OgVehicleBrand} from '../util/util';
import {Defaults} from '../util/defaults';
import {showSnackbar} from "../components/og-snackbar";
import {i18next} from "@openremote/or-translate";
import {OgInput} from "../components/og-input";

@customElement('panel-challenge-tips')
export class PanelChallengeTips extends OgDataPanel {

    protected AUTOMATIC_CONTROL_ATTRIBUTE_NAME = "automaticControl";
    protected POWER_EXPORT_MAX_ATTRIBUTE_NAME = "powerExportMax";

    public heading = html`<or-translate value="panel_tips.heading"></or-translate>`;
    public subtitle = html`<or-translate value="panel_tips.subtitle"></or-translate>`;
    public expandable = true;
    public expanded = true;
    public dark = true;
    public fullWidth = true;

    // Duplicate cache of _batteryAsset to use outside state updates.
    // During the animation (which is played AFTER state update), we keep track of the (previous) value that is shown in the UI.
    protected _batteryAsset?: Asset;

    @query('#battery-btn')
    protected _batteryButtonElem: OgInput;

    connectedCallback() {
        super.connectedCallback();
        this._batteryAsset = this.batteryAsset;
    }

    protected shouldUpdate(changedProps: PropertyValues) {

        // If batteryAsset updates, we play an animation that slowly transitions the button
        if(changedProps.size === 1 && changedProps.has("batteryAsset")) {
            const automaticControl: boolean = this.batteryAsset?.attributes?.[this.AUTOMATIC_CONTROL_ATTRIBUTE_NAME]?.value || false;
            this._doButtonAnimation(
                this._batteryButtonElem,
                automaticControl ? i18next.t("panel_tips.action6-success-on") : i18next.t("panel_tips.action6-success-off"),
                automaticControl ? "var(--og-color-primary)" : undefined,
                automaticControl ? "var(--og-color-success)" : undefined,
                automaticControl ? i18next.t("panel_tips.action6-on") : i18next.t("panel_tips.action6-off"),
                "var(--og-color-primary-dark)",
                "var(--og-color-neutral)"
            ).then(() => {
                // Update the duplicate cache after the animation has played
                this._batteryAsset = this.batteryAsset;
            })
            return false;
        }
        return super.shouldUpdate(changedProps);
    }

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
                    
                    ${when(check(WellknownCharacteristics.BATTERY), () => {
                        const automaticControl: boolean = this.batteryAsset?.attributes?.[this.AUTOMATIC_CONTROL_ATTRIBUTE_NAME]?.value || false;
                        const powerExportMax: number = this.batteryAsset?.attributes?.[this.POWER_EXPORT_MAX_ATTRIBUTE_NAME]?.value || 1000;
                        const unknownBattery = !this.batteryAsset;
                        const hasButton = !!this.batteryAsset;
                        return html`
                            <div class="background-tint" style="position: relative; ${hasButton ? 'margin-bottom: 32px;' : undefined}">
                                <div style="padding: ${hasButton ? '24px 24px 48px 24px' : '24px'};">
                                    ${getStatisticTemplate('images/battery-power-charge.svg', true, html`
                                        <div style="display: flex; flex-direction: column;">
                                        <span class="statistic-medium" style="color: var(--og-color-danger)">
                                            ${`-${powerExportMax}W`}
                                        </span>
                                            <span class="text-primary">
                                                <or-translate value="${unknownBattery ? 'panel_tips.text6-alt' : automaticControl ? 'panel_tips.text6-on' : 'panel_tips.text6-off'}"></or-translate>
                                            </span>
                                        </div>
                                    `)}
                                </div>
                                ${when(hasButton, () => html`
                                    <div style="position: absolute; display: block; bottom: -24px; width: calc(100% - 48px); left: 24px;">
                                        <og-input id="battery-btn" .type="${InputType.BUTTON}" .label="${automaticControl ? 'panel_tips.action6-on' : 'panel_tips.action6-off'}" fullWidth raised rounded comfortable
                                                  style="width: 100%; --or-mwc-input-color: var(--og-color-neutral); --or-mwc-input-text-color: var(--og-color-primary-dark)"
                                                  @or-mwc-input-changed="${(ev: OrInputChangedEvent) => this._onAutomaticControlClick(ev)}"
                                        ></og-input>
                                    </div>
                                `)}
                            </div>
                        `;
                    })}

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

    protected _onAutomaticControlClick(_ev: OrInputChangedEvent, newValue?: boolean) {
        if(!newValue) {
            newValue = !(this._batteryAsset?.attributes?.[this.AUTOMATIC_CONTROL_ATTRIBUTE_NAME]?.value || false);
        }
        this._setAutomaticControl(newValue);
    }

    protected getBrandAppUrl(characteristic: DeviceCharacteristic): string | undefined {
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
    protected _setAutomaticControl(automaticControl: boolean): void {
        if (this.meterAsset) {

            manager.rest.api.DeviceBatteryResource.automaticControl({meterId: this.meterAsset.id, automaticControl: automaticControl}).then(() => {
                if (automaticControl) {
                    showSnackbar(undefined, i18next.t("panel_batteryInfo.turnOnSnackbar"));
                } else {
                    showSnackbar(undefined, i18next.t("panel_batteryInfo.turnOffSnackbar"));
                }
            }).catch(() => {
                showSnackbar(undefined, i18next.t("errorOccurred"));
            });
        } else {
            showSnackbar(undefined, i18next.t("errorOccurred"));
            console.warn("Could not toggle automatic control; assets are not cached correctly.");
        }
    }

    /**
     * Plays an animation in the button element provided. It inserts `text`, plays a small animation, waits 3 seconds, and inserts the `afterText`.
     * The other parameters like `colorCSS` can be used to customize the text- and background colors.
     */
    protected async _doButtonAnimation(elem: OgInput, text?: string, colorCSS?: string, bgColorCSS?: string, afterText?: string, afterColorCSS?: string, afterBgColorCSS?: string): Promise<void> {
        if(text) {
            elem.label = text;
        }
        if(colorCSS) {
            setTimeout(() => elem.style.setProperty("--or-mwc-input-text-color", colorCSS), 100);
        }
        if(bgColorCSS) {
            setTimeout(() => elem.style.setProperty("--or-mwc-input-color", bgColorCSS), 100);
        }
        if(afterText || afterBgColorCSS) {
            await new Promise((r) => setTimeout(r, 3000));
            if(afterText) {
                elem.label = afterText;
            }
            if(afterColorCSS) {
                setTimeout(() => elem.style.setProperty("--or-mwc-input-text-color", afterColorCSS), 100);
            }
            if(afterBgColorCSS) {
                setTimeout(() => elem.style.setProperty("--or-mwc-input-color", afterBgColorCSS), 100);
            }
        }
    }
}
