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

    public transparent = false;
    public rounded = true;

    @property({ type: Object })
    public evInfo?: DeviceCharacteristic;

    @property({ type: Object })
    public chargerInfo?: DeviceCharacteristic;

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
            <div id="ev-container">
                <div id="ev-content">
                    <div id="ev-icon" style="background-color: ${this.evInfo ? 'var(--og-color-success)' : 'var(--og-color-danger)'}"></div>
                    <div style="display: flex; align-items: center; gap: 24px;">
                        <div>
                            <div style="display: flex; flex-direction: column; justify-content: space-between; gap: 8px;">
                                <span class="text-heading2" style="text-align: start;">
                                    ${this.evInfo ? i18next.t("panel_evInfo.yourVehicle") : i18next.t("panel_evInfo.noVehicleFound")}
                                </span>
                                ${when(this.evInfo, () => html`
                                    <div style="display: flex; flex-direction: column; gap: 2px;">
                                        ${until(this._getDetailsContentTemplate(this.evInfo, this.chargerInfo))}
                                    </div>
                                `)}
                            </div>
                        </div>
                    </div>
                </div>
                ${when(this.evInfo, () => html`
                    <div style="display: flex; justify-content: end; margin: 12px -12px -12px -12px;">
                        <og-chips .chips="${this._chips}" outlined choice></og-chips>
                    </div>
                `)}
            </div>
        `;
    }

    protected async _getDetailsContentTemplate(evInfo: DeviceCharacteristic, chargerInfo?: DeviceCharacteristic): Promise<TemplateResult> {
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
        `
    }

    protected _onRemoveClick() {
        this.dispatchEvent(new CustomEvent('request-remove'));
    }
}
