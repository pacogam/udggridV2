import {TemplateResult, html, css} from 'lit';
import {customElement} from 'lit/decorators.js';
import { InputType } from '@openremote/or-mwc-components/or-mwc-input';
import {OgDataPanel} from '../components/og-data-panel';
import {i18next} from '@openremote/or-translate';

const styling = css`
  iframe {
    min-height: 550px;
    width: 100%;
    border: none;
  }
`;

@customElement('panel-account-info')
export class PanelAccountInfo extends OgDataPanel {

    static get styles() {
        return [...super.styles, styling];
    }

    protected async getPanelContent(): Promise<TemplateResult> {
        return html`
            <div style="display: flex; flex-direction: column; gap: 12px;">
                <div style="width: 100%;">
                    <og-input .type="${InputType.TEXT}" label="${i18next.t('page-account.username')}" readonly .value="${this.user.username}" style="width: 100%;" />
                </div>
                <div style="width: 100%;">
                    <og-input .type="${InputType.TEXT}" label="${i18next.t('page-account.email')}" readonly .value="${this.user.email}" style="width: 100%;" />
                </div>
            </div>
        `;
    }

    /*protected async getPanelContent(): Promise<TemplateResult> {
        return html`
            <div style="padding: 0 16px;">
                <iframe src="${`${manager.keycloakUrl}/realms/${manager.getRealm()}/account/#/personal-info`}"></iframe>
            </div>
        `;
    }*/
}
