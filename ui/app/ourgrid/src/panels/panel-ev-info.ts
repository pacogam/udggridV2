import {TemplateResult, html, css, PropertyValues } from "lit";
import { customElement, property, query, state} from "lit/decorators.js";
import {when} from "lit/directives/when.js";
import {Chip, OgChips} from "../components/og-chips";
import {i18next} from "@openremote/or-translate";
import "../components/og-chips";
import {OgDataPanel} from "../components/og-data-panel";
import {Asset, DeviceCharacteristic} from "model";
import { until } from "lit/directives/until.js";
import rest from "rest";
import {showSnackbar} from "../components/og-snackbar";
import {OgDialog, showDialog} from "../components/og-dialog";
import {Constants} from "../util/constants";

const styling = css`
    #panel-wrapper {
      padding: 16px;
    }
    #ev-container {
        position: relative;
    }
    #ev-content {
        display: flex;
        align-items: center;
        gap: 24px;
    }
    #ev-icon {
        height: var(--og-font-size-statistic-large);
        width: var(--og-font-size-statistic-large);
        -webkit-mask-image: url("images/car-charging.svg");
        -webkit-mask-repeat: no-repeat;
        -webkit-mask-position: center;
        -webkit-mask-size: var(--og-font-size-statistic-large) auto;
        mask-image: url("images/car-charging.svg");
        mask-repeat: no-repeat;
        mask-position: center;
        mask-size: var(--og-font-size-statistic-large) auto;
    }
`;

@customElement("panel-ev-info")
export class PanelEvInfo extends OgDataPanel {

    protected AUTOMATIC_CONTROL_ATTRIBUTE_NAME = "allowAutomaticControlButton";

    public transparent = false;
    public rounded = true;

    @property({ type: Object })
    public evInfo?: DeviceCharacteristic;

    @property({ type: Object })
    public chargerInfo?: DeviceCharacteristic;

    @state()
    protected _chips: Chip[] = [{
        leadingIcon: "toggle-switch-off",
        text: html`<or-translate value="panel_evInfo.autoCharge"></or-translate>`,
        loading: false,
        action: () => {
            const automaticControl = !this._chips[0].selected;
            this.setAutomaticControl(automaticControl);
        }
    }, {
        leadingIcon: 'delete-outline',
        text: html`<or-translate value="remove"></or-translate>`,
        loading: false,
        action: () => this._onRemoveClick()
    }];

    @query('og-chips')
    protected _chipsElem?: OgChips;

    static get styles() {
        return [...super.styles, styling];
    }

    protected willUpdate(changedProps: PropertyValues) {

        // If the vehicle asset gets updated, correct the "automaticControl" button state
        if (changedProps.has("vehicleAsset") && this.vehicleAsset) {
            if (this.vehicleAsset.attributes?.[this.AUTOMATIC_CONTROL_ATTRIBUTE_NAME]?.value !== undefined) {
                this._updateAutomaticControlButtonState(this._isAutomaticControlEnabled(this.vehicleAsset));
            }
        }
    }

    protected async getPanelContent(): Promise<TemplateResult> {
        const hasEv = this.vehicleAsset || (this.evInfo && this.evInfo.shown);
        return html`
            <div id="ev-container">
                <div id="ev-content">
                    <div id="ev-icon" style="background-color: ${this.evInfo || this.vehicleAsset ? 'var(--og-color-success)' : 'var(--og-color-danger)'}"></div>
                    <div style="display: flex; align-items: center; gap: 24px;">
                        <div>
                            <div style="display: flex; flex-direction: column; justify-content: space-between; gap: 8px;">
                                <or-translate class="text-heading2" style="text-align: start;" value=${hasEv ? 'panel_evInfo.yourVehicle' : 'panel_evInfo.noVehicleFound'}></or-translate>
                                ${when(hasEv, () => html`
                                    <div style="display: flex; flex-direction: column; gap: 2px;">
                                        ${when(this.vehicleAsset,
                                                () => until(this._getVehicleDetailsTemplate(this.vehicleAsset)),
                                                () => until(this._getCharacteristicsDetailsTemplate(this.evInfo, this.chargerInfo))
                                        )}
                                    </div>
                                `)}
                            </div>
                        </div>
                    </div>
                </div>
                ${when(hasEv, () => html`
                    <div style="display: flex; justify-content: end; margin: 12px -12px -12px -12px;">
                        <og-chips .chips="${this._chips}" outlined choice></og-chips>
                    </div>
                `)}
            </div>
        `;
    }

    protected async _getVehicleDetailsTemplate(vehicleAsset: Asset) {
        const brand = vehicleAsset.attributes?.["manufacturer"]?.value;
        const model = vehicleAsset.attributes?.["model"]?.value;
        return html`
            <div style="display: flex; align-items: center; gap: 8px;">
                <or-icon style="font-size: var(--og-font-size-button-small)" icon="domain"></or-icon>
                <or-translate class="text-tertiary" style="text-align: start;" value=${brand || 'panel_evInfo.unknownBrand'}></or-translate>
            </div>
            <div style="display: flex; align-items: center; gap: 8px;">
                <or-icon style="font-size: var(--og-font-size-button-small)" icon="car-back"></or-icon>
                <or-translate class="text-tertiary" style="text-align: start;" value=${model || "panel_evInfo.unknownModel"}></or-translate>
            </div>
        `;
    }

    protected async _getCharacteristicsDetailsTemplate(evInfo: DeviceCharacteristic, chargerInfo?: DeviceCharacteristic) {
        const brand = evInfo.brand;
        const hasCharger = chargerInfo.shown;
        return html`
            <div style="display: flex; align-items: center; gap: 8px;">
                <or-icon style="font-size: var(--og-font-size-button-small)" icon="car-back"></or-icon>
                <span class="text-tertiary" style="text-align: start;">
                    <or-translate value=${brand || 'panel_evInfo.unknownBrand'}></or-translate>
                </span>
            </div>
            <div style="display: flex; align-items: center; gap: 8px;">
                <or-icon style="font-size: var(--og-font-size-button-small)" icon="ev-station"></or-icon>
                <span class="text-tertiary" style="text-align: start;">
                    <or-translate value=${hasCharger ? 'panel_evInfo.hasCharger' : 'panel_evInfo.noCharger'}></or-translate>
                </span>
            </div>
        `;
    }

    /**
     * Updates the 'automaticControl' attribute
     */
    protected setAutomaticControl(automaticControl: boolean): void {

        // When user has a linked vehicle asset, update its attribute
        if (this.vehicleAsset) {
            rest.api.AssetResource.writeAttributeValues(
                [{ref: { id: this.vehicleAsset.id, name: this.AUTOMATIC_CONTROL_ATTRIBUTE_NAME}, value: automaticControl}]
            ).then(() => {
                if (automaticControl) {
                    showSnackbar(undefined, i18next.t("panel_evInfo.turnOnSnackbar"));
                } else {
                    showSnackbar(undefined, i18next.t("panel_evInfo.turnOffSnackbar"));
                }
                this._updateAutomaticControlButtonState(automaticControl);
            });

        // If no vehicle asset is connected, always prompt the user to "connect their device with EARN-E to enable automatic control"
        } else if (this.meterAsset?.attributes?.['deviceId']?.value) {
            showDialog(new OgDialog()
                .setHeading('panel_evInfo.autoCharge')
                .setContent(html`<or-translate value="panel_evInfo.autoChargeConfirm"></or-translate>`)
                .setDismissAction(null)
                .setActions([
                    {actionName: 'cancel', content: 'cancel'},
                    {actionName: 'connect', content: 'connect', action: () => {
                            window.location.href = Constants.AUTHORIZE_EV_URL.replace('{meterId}', this.meterAsset?.attributes?.['deviceId']?.value);
                        }}
                ]) as any
            );
        } else {

            // Something else (unexpected) went wrong..
            showSnackbar(undefined, i18next.t("errorOccurred"));
            console.warn("Could not toggle automatic control of EV; assets are not cached correctly.");
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

    protected _isAutomaticControlEnabled(vehicleAsset: Asset) {
        if(vehicleAsset?.attributes?.[this.AUTOMATIC_CONTROL_ATTRIBUTE_NAME]) {
            return Boolean(vehicleAsset.attributes[this.AUTOMATIC_CONTROL_ATTRIBUTE_NAME].value);
        } else {
            return false;
        }
    }

    protected _onRemoveClick() {
        this.dispatchEvent(new CustomEvent('request-remove'));
    }
}
