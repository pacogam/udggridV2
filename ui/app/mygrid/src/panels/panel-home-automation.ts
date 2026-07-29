import {TemplateResult, html, css} from "lit";
import {customElement, property, state} from "lit/decorators.js";
import {when} from "lit/directives/when.js";
import {i18next} from "@openremote/or-translate";
import "../components/og-chips";
import {OgDataPanel} from "../components/og-data-panel";
import {until} from "lit/directives/until.js";
import {DeviceCharacteristic} from "model";
import {Chip} from "../components/og-chips";

const styling = css`
    #panel-wrapper {
        padding: 16px;
    }

    #homeautomation-container {
        position: relative;
    }

    #homeautomation-content {
        display: flex;
        align-items: center;
        gap: 24px;
    }

    #homeautomation-icon {
        font-size: var(--og-font-size-statistic-large);
        color: var(--og-color-success);
    }
`;

@customElement("panel-homeautomation-info")
export class PanelHomeAutomationInfo extends OgDataPanel {

    public transparent = false;
    public rounded = true;

    @property({ type: Object })
    public homeAutomationInfo?: DeviceCharacteristic;

    @state()
    protected _chips: Chip[] = [{
        leadingIcon: 'information-outline',
        text: html`<or-translate value="panel_homeAutomationInfo.info"></or-translate>`,
        loading: false,
        action: () => this._onInfoClick()
    }, {
        leadingIcon: 'delete-outline',
        text: html`<or-translate value="remove"></or-translate>`,
        loading: false,
        action: () => this._onRemoveClick()
    }];

    static get styles() {
        return [...super.styles, styling];
    }

    protected async getPanelContent(): Promise<TemplateResult> {
        const brand = this.homeAutomationInfo.brand;
        return html`
            <div id="homeautomation-container">
                <div id="homeautomation-content">
                    <or-icon id="homeautomation-icon" icon="home-automation"></or-icon>
                    <div style="display: flex; align-items: center; gap: 24px;">
                        <div>
                            <div style="display: flex; flex-direction: column; justify-content: space-between; gap: 8px;">
                                <span class="text-heading2" style="text-align: start;">
                                    ${brand ? i18next.t("panel_homeAutomationInfo.yourHomeAutomation") : i18next.t("panel_HomeAutomationInfo.noHomeAutomationFound")}
                                </span>
                                ${when(this.homeAutomationInfo, () => html`
                                    <div style="display: flex; flex-direction: column; gap: 2px;">
                                        ${until(this._getDetailsContentTemplate(this.homeAutomationInfo))}
                                    </div>
                                `)}
                            </div>
                        </div>
                    </div>
                </div>
                ${when(this.homeAutomationInfo, () => html`
                    <div style="display: flex; justify-content: end; margin: 12px -12px -12px -12px;">
                        <og-chips .chips="${this._chips}" outlined choice></og-chips>
                    </div>
                `)}
            </div>
        `;
    }

    protected async _getDetailsContentTemplate(homeAutomationInfo: DeviceCharacteristic): Promise<TemplateResult> {
        return html`
            <div style="display: flex; align-items: center; gap: 8px;">
                <or-icon style="font-size: var(--og-font-size-button-small)" icon="factory"></or-icon>
                <or-translate value=${homeAutomationInfo.brand || 'panel_homeAutomationInfo.unknownBrand'} style="text-align: start"></or-translate>
            </div>
        `;
    }

    protected _onInfoClick() {
        this.dispatchEvent(new CustomEvent('request-info'));
    }

    protected _onRemoveClick() {
        this.dispatchEvent(new CustomEvent('request-remove'));
    }
}
