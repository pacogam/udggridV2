import {OgPanel} from '../components/og-panel';
import {TemplateResult, html, css} from 'lit';
import {customElement} from 'lit/decorators.js';

const styling = css`
  .notification-wrapper {
    min-height: 360px;
    background: white;
  }
`;

@customElement('panel-account-notifications')
export class PanelAccountNotifications extends OgPanel {

    static get styles() {
        return [...super.styles, styling];
    }

    protected async getPanelContent(): Promise<TemplateResult> {
        return html`
            <div class="notification-wrapper">
                <span>Notification settings...</span>
            </div>
        `;
    }
}
