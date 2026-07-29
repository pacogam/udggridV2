import { TemplateResult, html } from 'lit';
import { customElement } from 'lit/decorators.js';
import '../components/og-points';
import {OgDataPanel} from '../components/og-data-panel';
import {Constants} from '../util/constants';
import {Defaults} from '../util/defaults';

@customElement('panel-challenge-points')
export class PanelChallengePoints extends OgDataPanel {

    public dark = true;

    protected async getPanelContent(): Promise<TemplateResult> {
        const challengeDuration: number = this.challengeAsset?.attributes?.[Constants.CHALLENGE_DURATION_ATTRIBUTE]?.value || Defaults.CHALLENGE_DURATION_MINUTES;
        const challengeInterval: number = this.challengeAsset?.attributes?.[Constants.CHALLENGE_POINT_INTERVAL_ATTRIBUTE]?.value || Defaults.CHALLENGE_INTERVAL_MINUTRES;
        const challengeMaxPointsAchievable = Math.round(challengeDuration / challengeInterval);
        const challengeProgress = this.meterAsset?.attributes?.[Constants.CHALLENGE_POINT_CURRENT_ATTRIBUTE]?.value || 0;

        return html`
            <div>
                <og-points .progress="${challengeProgress}" .max=${challengeMaxPointsAchievable}></og-points>
            </div>
        `;
    }
}
