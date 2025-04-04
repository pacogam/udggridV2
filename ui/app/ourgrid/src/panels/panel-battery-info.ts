import {TemplateResult, html, css, PropertyValues } from "lit";
import { customElement, property, query, state} from "lit/decorators.js";
import {when} from "lit/directives/when.js";
import {styleMap} from "lit/directives/style-map.js";
import {Chip, OgChips} from "../components/og-chips";
import {i18next} from "@openremote/or-translate";
import "../components/og-chips";
import {OgDataPanel} from "../components/og-data-panel";
import {showSnackbar} from "../components/og-snackbar";
import {Asset} from "@openremote/model";
import manager from "@openremote/core";
import rest from "rest";
import {DeviceCharacteristic} from "model";

const styling = css`
    #panel-wrapper {
      padding: 16px;
    }
`;

@customElement("panel-battery-info")
export class PanelBatteryInfo extends OgDataPanel {

    protected AUTOMATIC_CONTROL_ATTRIBUTE_NAME = "automaticControl";

    public transparent = false;
    public rounded = true;

    @property({ type: Object })
    public info?: DeviceCharacteristic

    @state()
    protected _chips: Chip[] = [{
        leadingIcon: "toggle-switch-off",
        text: html`<or-translate value="panel_batteryInfo.autoCharge"></or-translate>`,
        loading: false,
        action: () => {
            const automaticControl = !this._chips[0].selected;
            this.setAutomaticControl(automaticControl);
        }
    }, {
        leadingIcon: 'delete-outline',
        text: html`<or-translate value="remove"></or-translate>`,
        disabled: true,
        action: () => this._onRemoveClick()
    }];

    @query('og-chips')
    protected _chipsElem?: OgChips;

    static get styles() {
        return [...super.styles, styling];
    }

    protected willUpdate(changedProps: PropertyValues) {

        // If the battery asset gets updated, correct the "automaticControl" button state
        if(changedProps.has("batteryAsset") && this.batteryAsset) {
            if(this.batteryAsset.attributes?.[this.AUTOMATIC_CONTROL_ATTRIBUTE_NAME]?.value) {
                this._updateAutomaticControlButtonState(this._isAutomaticControlEnabled(this.batteryAsset));
                this._updateRemoveButtonState(!this._canRemove(this.batteryAsset, this.info))
            }
        }
        // If the characteristic gets updated
        if(changedProps.has("info") && this.info) {
            this._updateRemoveButtonState(!this._canRemove(this.batteryAsset, this.info))
        }

        return super.willUpdate(changedProps);
    }

    protected async getPanelContent(): Promise<TemplateResult> {
        const hasBattery = this.batteryAsset || (this.info && this.info.shown)
        const iconStyles = {
            "font-size": "var(--og-font-size-statistic-large)",
            "color": hasBattery ? "var(--og-color-success)" : "var(--og-color-danger)"
        };
        return html`
            <div style="position: relative;">
                <div style="display: flex; align-items: center; gap: 24px;">
                    <div>
                        <or-icon icon="battery-charging" style="${styleMap(iconStyles)}"></or-icon>
                    </div>
                    <div>
                        <div style="display: flex; flex-direction: column; justify-content: space-between; gap: 8px;">
                            <span class="text-heading2" style="text-align: start;">
                                ${hasBattery ? i18next.t("panel_batteryInfo.yourBattery") : i18next.t("panel_batteryInfo.noBatteryFound")}
                            </span>
                            <div style="display: flex; flex-direction: column; gap: 2px;">
                                ${when(hasBattery, () => {
                                    const manufacturer = this.batteryAsset?.attributes["manufacturer"]?.value || this.info?.brand;
                                    const deviceId = this.batteryAsset?.attributes["deviceId"]?.value;
                                    const version = this.batteryAsset?.attributes["softwareVersion"]?.value;
                                    return html`
                                        <div style="display: flex; align-items: center; gap: 8px;">
                                            <or-icon style="font-size: var(--og-font-size-button-small)" icon="battery-high"></or-icon>
                                            <span class="text-tertiary" style="text-align: start;">
                                                <or-translate value="${manufacturer || 'panel_batteryInfo.unknownManufacturer'}"></or-translate>
                                            </span>
                                        </div>
                                        <div style="display: flex; align-items: center; gap: 8px;">
                                            <or-icon style="font-size: var(--og-font-size-button-small)" icon="meter-electric-outline"></or-icon>
                                            <span class="text-tertiary" style="text-align: start;">
                                                <or-translate value="panel_batteryInfo.unknown"></or-translate>
                                            </span>
                                        </div>
                                    `;
                                }, () => html`
                                    <or-translate value="panel_batteryInfo.noBatteryFound" style="margin-bottom: 20px;"></or-translate>
                                `)}
                            </div>
                            <div>
                            </div>
                        </div>
                    </div>
                </div>
                ${when(hasBattery, () => html`
                    <div style="display: flex; justify-content: end; margin: 12px -12px -12px -12px;">
                        <og-chips .chips="${this._chips}" outlined choice></og-chips>
                    </div>
                `)}
            </div>
        `;
    }

    /**
     * Updates the 'automaticControl' attribute on the battery asset using the HTTP API.
     * It uses the custom endpoint in {@link DeviceBatteryResource}, where all "user access checks" are performed.
     * Shows a snackbar afterwards, and will update the button state automatically.
     */
    protected setAutomaticControl(automaticControl: boolean): void {
        if (this.batteryAsset && this.meterAsset) {

            rest.api.DeviceBatteryResource.automaticControl({meterId: this.meterAsset.id, automaticControl: automaticControl}).then(() => {
                if (automaticControl) {
                    showSnackbar(undefined, i18next.t("panel_batteryInfo.turnOnSnackbar"));
                } else {
                    showSnackbar(undefined, i18next.t("panel_batteryInfo.turnOffSnackbar"));
                }
                this._updateAutomaticControlButtonState(automaticControl);

            }).catch(() => {
                showSnackbar(undefined, i18next.t("errorOccurred"));
            });
        } else {
            showSnackbar(undefined, i18next.t("errorOccurred"));
            console.warn("Could not toggle automatic control; assets are not cached correctly.");
        }
    }

    /**
     * Convenient function that updates the button state.
     */
    protected _updateAutomaticControlButtonState(automaticControl: boolean): void {
        this._chips[0].selected = automaticControl;
        this._chips[0].leadingIcon = automaticControl ? "toggle-switch" : "toggle-switch-off";
        this._chips = [...this._chips]; // trigger a UI update, by recreating the array. (as it also needs to trigger og-chips UI update)
    }

    protected _isAutomaticControlEnabled(batteryAsset: Asset) {
        if(batteryAsset?.attributes?.[this.AUTOMATIC_CONTROL_ATTRIBUTE_NAME]) {
            return Boolean(batteryAsset.attributes[this.AUTOMATIC_CONTROL_ATTRIBUTE_NAME].value);
        } else {
            return false;
        }
    }

    protected _updateRemoveButtonState(disabled: boolean): void {
        this._chips[0].disabled = !disabled; // If battery can be removed, 'automatic charging' chip should be disabled.
        this._chips[1].disabled = disabled;
        this._chips = [...this._chips]; // trigger a UI update, by recreating the array. (as it also needs to trigger og-chips UI update)
    }

    protected _canRemove(batteryAsset?: Asset, info?: DeviceCharacteristic): boolean {
        if(batteryAsset) {
            return false;
        } else {
            return true;
        }
    }

    protected _onRemoveClick() {
        this.dispatchEvent(new CustomEvent('request-remove'));
    }
}
