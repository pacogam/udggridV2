import {TemplateResult, html, css } from "lit";
import { customElement, property, query, state} from "lit/decorators.js";
import {when} from "lit/directives/when.js";
import {Chip, OgChips} from "../components/og-chips";
import {i18next} from "@openremote/or-translate";
import "../components/og-chips";
import {OgDataPanel} from "../components/og-data-panel";
import {DeviceCharacteristic} from "model";
import { until } from "lit/directives/until.js";

const styling = css`
    #panel-wrapper {
      padding: 16px;
    }
    #heatpump-container {
        position: relative;
    }
    #heatpump-content {
        display: flex;
        align-items: center;
        gap: 24px;
    }
    #heatpump-icon {
        height: var(--og-font-size-statistic-large);
        width: var(--og-font-size-statistic-large);
        -webkit-mask-image: url("images/house-temperature.svg");
        -webkit-mask-repeat: no-repeat;
        -webkit-mask-position: center;
        -webkit-mask-size: auto var(--og-font-size-statistic-large);
        mask-image: url("images/house-temperature.svg");
        mask-repeat: no-repeat;
        mask-position: center;
        mask-size: auto var(--og-font-size-statistic-large);
    }
`;

@customElement("panel-heatpump-info")
export class PanelHeatpumpInfo extends OgDataPanel {

    public transparent = false;
    public rounded = true;

    @property({ type: Object })
    public info?: DeviceCharacteristic

    @state()
    protected _chips: Chip[] = [{
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

    protected async getPanelContent(): Promise<TemplateResult> {
        return html`
            <div id="heatpump-container">
                <div id="heatpump-content">
                    <div id="heatpump-icon" style="background-color: ${this.info ? 'var(--og-color-success)' : 'var(--og-color-danger)'}"></div>
                    <div style="display: flex; align-items: center; gap: 24px;">
                        <div>
                            <div style="display: flex; flex-direction: column; justify-content: space-between; gap: 8px;">
                            <span class="text-heading2" style="text-align: start;">
                                ${this.info ? i18next.t("panel_heatpumpInfo.yourHeatpump") : i18next.t("panel_heatpumpInfo.noHeatpumpFound")}
                            </span>
                            ${when(this.info, () => html`
                                <div style="display: flex; flex-direction: column; gap: 2px;">
                                    ${until(this._getDetailsContentTemplate(this.info))}
                                </div>
                            `)}
                            </div>
                        </div>
                    </div>
                </div>
                ${when(this.info, () => html`
                    <div style="display: flex; justify-content: end; margin: 12px -12px -12px -12px;">
                        <og-chips .chips="${this._chips}" outlined choice></og-chips>
                    </div>
                `)}
            </div>
        `;
    }

    protected async _getDetailsContentTemplate(characteristic: DeviceCharacteristic): Promise<TemplateResult> {
        const brand = characteristic.brand;
        return html`
            <div style="display: flex; align-items: center; gap: 8px;">
                <or-icon style="font-size: var(--og-font-size-button-small)" icon="heat-pump"></or-icon>
                <span class="text-tertiary" style="text-align: start;">
                    <or-translate value='panel_heatpumpInfo.thermostat'></or-translate>
                </span>
            </div>
            <div style="display: flex; align-items: center; gap: 8px;">
                <or-icon style="font-size: var(--og-font-size-button-small)" icon="thermostat"></or-icon>
                <span class="text-tertiary" style="text-align: start;">
                    <or-translate value=${brand || 'panel_heatpumpInfo.unknownBrand'}></or-translate>
                </span>
            </div>
        `
    }

    protected _onRemoveClick() {
        this.dispatchEvent(new CustomEvent('request-remove'));
    }
}
