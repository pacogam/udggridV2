import {OgPanel} from '../components/og-panel';
import {TemplateResult, html} from 'lit';
import {customElement} from 'lit/decorators.js';

@customElement('panel-privacy-statement')
export class PanelPrivacyStatement extends OgPanel {

    protected async getPanelContent(): Promise<TemplateResult> {
        return html`
            <div style="margin: 0 8px 64px 8px">
                <span class="text-secondary">
                    <or-translate value="privacy-statement" style="white-space: pre-line; text-align: left;"></or-translate>
                </span>
            </div>
        `;
    }
}
