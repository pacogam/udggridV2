import { TemplateResult, html } from 'lit';
import { customElement } from 'lit/decorators.js';
import '../features/og-challenge-progress';
import {OgDataPanel} from '../components/og-data-panel';

@customElement('panel-challenge-progress')
export class PanelChallengeProgress extends OgDataPanel {

    public fullWidth = true;
    public dark = true;

    protected async getPanelContent(): Promise<TemplateResult> {
        return html`
            <div>
                <og-challenge-progress dark .meterAsset=${this.meterAsset} .challengeAsset=${this.challengeAsset} />
            </div>
        `;
    }
}
