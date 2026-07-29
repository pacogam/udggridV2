import {TemplateResult, html, css} from "lit";
import {customElement, property, state} from "lit/decorators.js";
import {when} from "lit/directives/when.js";
import {i18next} from "@openremote/or-translate";
import {OgDataPanel} from "../components/og-data-panel";
import {until} from "lit/directives/until.js";
import {InputType} from '@openremote/or-mwc-components/or-mwc-input';
import {User} from "model";

const styling = css`
    #panel-wrapper {
        padding: 16px 24px 16px 16px;
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
    }
`;

@customElement("panel-homeautomation-credentials")
export class PanelHomeAutomationCredentials extends OgDataPanel {

    public transparent = false;
    public rounded = true;

    @property({type: Object})
    public serviceUser?: User;

    @state()
    protected _passwordVisible = false;

    static get styles() {
        return [...super.styles, styling];
    }

    protected async getPanelContent(): Promise<TemplateResult> {
        return html`
            <div id="homeautomation-container">
                <div id="homeautomation-content">
                    <or-icon id="homeautomation-icon" icon="home-automation" style="color: ${this.serviceUser ? 'var(--og-color-success)' : 'var(--og-color-danger)'}"></or-icon>
                    <div style="display: flex; align-items: center; gap: 24px; flex: 1;">
                        <div style="display: flex; flex-direction: column; justify-content: space-between; gap: 8px; width: 100%;">
                            <span class="text-heading2" style="text-align: start;">
                                <or-translate .value=${this.serviceUser ? 'panel_homeAutomationInfo.credentials' : 'panel_homeAutomationInfo.noHomeAutomationFound'}></or-translate>
                            </span>
                            ${when(this.serviceUser, () => html`
                                <div style="display: flex; flex-direction: column; gap: 16px; margin-top: 16px;">
                                    ${until(this._getDetailsContentTemplate(this.serviceUser))}
                                </div>
                            `)}
                        </div>
                    </div>
                </div>
            </div>
        `;
    }

    protected async _getDetailsContentTemplate(serviceUser: User): Promise<TemplateResult> {
        return html`
            <og-input type=${InputType.TEXT} value=${serviceUser.username} outlined readonly label="Client ID"></og-input>
            <og-input type=${InputType.TEXT} value=${serviceUser.secret} outlined readonly label="Client Secret"></og-input>
        `;
    }
}
