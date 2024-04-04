import {OgPanel} from '../components/og-panel';
import {TemplateResult, html, css} from 'lit';
import {customElement} from 'lit/decorators.js';
import manager from '@openremote/core';

const styling = css`
  iframe {
    min-height: 480px;
    width: 100%;
    border: none;
  }
`;

@customElement('panel-account-password')
export class PanelAccountPassword extends OgPanel {

    static get styles() {
        return [...super.styles, styling];
    }

    protected async getPanelContent(): Promise<TemplateResult> {
        return html`
            <div style="padding: 0 16px;">
                <iframe src="${`${manager.keycloakUrl}/realms/${manager.getRealm()}/account/password`}"></iframe>
            </div>
        `;
    }
}
