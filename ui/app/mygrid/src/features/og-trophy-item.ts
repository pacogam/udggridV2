import { LitElement, TemplateResult, html, css } from 'lit';
import { customElement, property } from 'lit/decorators.js';
import { when } from 'lit/directives/when.js';
import moment from 'moment';
import {getAppStyle} from '../styles';
import {i18next} from '@openremote/or-translate';

export interface TrophyItem {
    date: Date,
    points: number,
    savings?: number
}

const styling = css`
    #item-container {
      padding: 12px 0;
      display: flex;
      justify-content: space-between;
      align-items: center;
    }
  #item-statistic {
    display: flex;
    flex-direction: column;
  }
`;

@customElement('og-trophy-item')
export class OgTrophyItem extends LitElement {

    @property()
    protected item?: TrophyItem;

    @property()
    protected divider = false;

    @property()
    protected timeFormat = 'D MMMM YYYY - HH:mm';

    static styles = [getAppStyle(), styling];

    protected render(): TemplateResult {
        return when(this.item, () => {
            const formattedDate: string = moment(this.item.date).format(this.timeFormat);
            return html`
                <div id="item-container">
                    <div id="item-statistic">
                        <span class="text-secondary">${formattedDate}</span>
                        <span class="statistic-medium">
                            ${this.item.points}
                            <or-translate value="${this.item.points === 1 ? 'challenge.point' : 'challenge.points'}"></or-translate>
                        </span>
                    </div>
                    ${when(this.item.savings, () => html`
                        <div>
                            <span class="text-secondary">${this.item.savings} kWh ${i18next.t('saved')}</span>
                        </div>
                    `)}
                </div>
                ${when(this.divider, () => html`
                    <div style="border-bottom: 1px solid #D9D9D9;"></div>
                `)}
            `;
        });
    }
}
