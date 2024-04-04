import {css, html, TemplateResult} from 'lit';
import {customElement} from 'lit/decorators.js';
import {OgDataPanel} from '../components/og-data-panel';
import {Util} from '@openremote/core';
import {Defaults} from '../util/defaults';

const challengePointsTotalAttribute = 'totalPoints';
const challengeExchangeRateAttribute = 'challengePointsExchangeRate';

const styling = css`
    #panel-wrapper {
      padding: 16px;
    }
`;

@customElement('panel-challenge-earnings')
export class PanelChallengeEarnings extends OgDataPanel {

    public transparent = false;
    public rounded = true;

    static get styles() {
        return [...super.styles, styling];
    }

    protected async getPanelContent(): Promise<TemplateResult> {
        const exchangeRate: number = this.challengeAsset?.attributes?.[challengeExchangeRateAttribute]?.value || 0;
        const parsedExchangeRate = Util.resolveUnits(['EUR'], exchangeRate.toFixed(Defaults.POINT_EXCHANGE_RATE_DECIMALS));
        const pointsTotal = this.meterAsset?.attributes?.[challengePointsTotalAttribute]?.value || 0;
        const earnings = Util.resolveUnits(['EUR'], (pointsTotal * exchangeRate).toFixed(Defaults.POINT_EARNINGS_DECIMALS));
        return html`
            <div style="display: flex; flex-direction: column; gap: 6px;">
                <div style="display: flex; justify-content: space-between; align-items: center;">
                    <div style="display: flex; align-items: center; gap: 6px;">
                        <div style="width: 24px; height: 24px; display: flex; justify-content: center; align-items: center;">
                            <img src="images/star.svg" style="width: 22px;" />
                        </div>
                        <span class="text-tertiary bold"><or-translate value="panel_earnings.totalPoints" /></span>
                    </div>
                    <span>${pointsTotal}</span>
                </div>
                <div style="display: flex; justify-content: space-between; align-items: center;">
                    <div style="display: flex; align-items: center; gap: 6px;">
                        <or-icon icon="finance"></or-icon>
                        <span class="text-tertiary bold"><or-translate value="panel_earnings.exchangeRate" /></span>
                    </div>
                    <span>${parsedExchangeRate} <or-translate value="panel_earnings.perPoint" /></span>
                </div>
                <div style="display: flex; justify-content: space-between; align-items: center;">
                    <div style="display: flex; align-items: center; gap: 6px;">
                        <or-icon icon="piggy-bank-outline"></or-icon>
                        <span class="text-tertiary bold"><or-translate value="panel_earnings.expectedEarnings" /></span>
                    </div>
                    <span>${earnings}</span>
                </div>
            </div>
        `;
    }
}
