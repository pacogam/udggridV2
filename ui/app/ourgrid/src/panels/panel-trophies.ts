import {TemplateResult, html, css, PropertyValues} from 'lit';
import {customElement, state} from 'lit/decorators.js';
import {getAppStyle} from '../styles';
import {map} from 'lit/directives/map.js';
import {when} from 'lit/directives/when.js';
import {animate} from '@lit-labs/motion';
import {TrophyItem} from '../features/og-trophy-item';
import '../components/og-expandable';
import '../features/og-trophy-item';
import { until } from 'lit/directives/until.js';
import manager from '@openremote/core';
import moment from 'moment';
import {OgDataPanel} from '../components/og-data-panel';
import {Chip} from '../components/og-chips';
import {showSnackbar} from '../components/og-snackbar';
import {Constants} from '../util/constants';
import {Defaults} from '../util/defaults';

export function getStatisticTemplate(prefixIcon?: string, isPath = false, leftContent?: TemplateResult, rightContent?: TemplateResult, expandContent?: TemplateResult, expandable = true): TemplateResult {
    const content = html`
        <div style="display: flex; justify-content: space-between; align-items: center;">
            <div style="flex: 1; display: flex; align-items: center; gap: 24px;">
                ${when(prefixIcon, () => isPath
                        ? html`<img src="${prefixIcon}" alt="Statistic Icon" width="50" height="50"/>`
                        : html`<or-icon .icon="${prefixIcon}" style="--internal-or-icon-width: var(--or-icon-width, 50px);" />`
                )}
                <div>
                    <div>${when(leftContent, () => leftContent)}</div>
                    <div>${when(rightContent, () => rightContent)}</div>
                </div>
            </div>
        </div>
    `;
    if (!expandContent || !expandable) {
        return content;
    } else {
        return html`
            <og-expandable .header="${content}" .limitedHeight="${true}">${expandContent}</og-collapsible>
        `;
    }
}

const styling = css`
  #content-wrapper {
    display: flex;
    flex-direction: column;
    gap: 24px;
  }
`;

@customElement('panel-trophies')
export class PanelTrophies extends OgDataPanel {

    public heading = html`<or-translate value="panel_trophies.heading"></or-translate>`;
    public dotsGraphic = true;

    @state()
    protected trophies: TrophyItem[] = [];

    @state()
    protected peakTrophies: TrophyItem[] = [];

    @state()
    protected currentHistoryDate: Date = moment().toDate();

    @state()
    protected peakCurrentHistoryDate: Date = moment().toDate();

    @state()
    protected challengesLoading = false;

    @state()
    protected peakPointsLoading = false;

    static get styles() {
        return [...super.styles, getAppStyle(), styling];
    }

    // Lit lifecycle method during UI update
    protected willUpdate(changedProps: PropertyValues) {

        // If 'currentHistoryDate' goes down (for example by 1 week), we fetch the challenges that are not present yet.
        // These are then formatted into trophies and added onto the state array.
        if(changedProps.has('currentHistoryDate') && this.currentHistoryDate) {
            const historyDate = moment(this.currentHistoryDate).subtract(Defaults.HISTORY_FETCH_AMOUNT, (Defaults.HISTORY_FETCH_UNIT as any)).toDate();
            console.log(`Fetching challenge history between ${historyDate.toDateString()} and ${this.currentHistoryDate.toDateString()}`);
            this.fetchTrophies(historyDate, this.currentHistoryDate).then(trophies => {
                this.trophies = this.trophies.concat(trophies);
            });
        }

        if(changedProps.has('peakCurrentHistoryDate') && this.peakCurrentHistoryDate) {
            const historyDate = moment(this.peakCurrentHistoryDate).subtract(Defaults.HISTORY_FETCH_AMOUNT, (Defaults.HISTORY_FETCH_UNIT as any)).toDate();
            console.log(`Fetching peak history between ${historyDate.toDateString()} and ${this.currentHistoryDate.toDateString()}`);
            this.fetchPeaks(historyDate, this.peakCurrentHistoryDate).then(trophies => {
                this.peakTrophies = this.peakTrophies.concat(trophies);
            });
        }

        return super.willUpdate(changedProps);
    }

    // Fetching challenges between start and end date.
    protected async fetchTrophies(start: Date, end: Date): Promise<TrophyItem[]> {
        const promise = manager.rest.api.DeviceChallengesResource.getHistory({ startTimestamp: start.getTime(), endTimestamp: end.getTime() });
        promise.catch(e => {
            console.error(e);
            showSnackbar(undefined, 'error.challengeDataFailed');
        });
        const data = (await promise).data;
        return data.map(challenge => ({ date: new Date(challenge.startDate), points: challenge.points } as TrophyItem));
    }

    protected async fetchPeaks(start: Date, end: Date): Promise<TrophyItem[]> {
        const pointsPerDay = this.peakPointsAsset?.attributes?.[Constants.METER_PEAK_DAY_POINTS_ATTRIBUTE]?.value || 1;
        const data = (await manager.rest.api.AssetDatapointResource.getDatapoints(this.meterAsset.id, Constants.METER_PEAK_POINTS_ATTRIBUTE, {
            type: 'all',
            fromTimestamp: start.getTime(),
            toTimestamp: end.getTime()
        })).data;
        return data.map(datapoint => ({ date: moment(datapoint.x).subtract(1, 'day').toDate(), points: pointsPerDay } as TrophyItem));
    }


    /* ------------------------------------------ */

    // Panel HTML render
    // Uses guard() directive to only render once trophies list (or asset data) has changed.
    protected async getPanelContent(): Promise<TemplateResult> {
        const activeChallengePoints = this.meterAsset?.attributes?.[Constants.CHALLENGE_POINTS_ATTRIBUTE]?.value || 0;
        const challengesJoined = this.meterAsset?.attributes?.[Constants.CHALLENGE_JOINED_ATTRIBUTE]?.value || 0;
        const peakPoints = this.meterAsset?.attributes?.[Constants.METER_PEAK_POINTS_ATTRIBUTE]?.value || 0;
        const peakPointsPerDay = this.peakPointsAsset?.attributes?.[Constants.METER_PEAK_DAY_POINTS_ATTRIBUTE]?.value || 1;
        return html`
            <div id="content-wrapper">
                <div class="content-container" ${animate()}>
                    ${getStatisticTemplate('images/yellow-star.svg', true, html`
                        <span class="text-heading2">${challengesJoined}</span>
                        <span class="text-primary"><or-translate value="panel_trophies.challengesJoined"></or-translate></span>
                    `, html`
                        <span class="text-primary">${activeChallengePoints}</span>
                        <span class="text-primary"><or-translate value="panel_trophies.pointsEarned"></or-translate></span>
                    `, html`
                        ${until(this.getTrophiesTemplate('challenges', this.trophies, this.challengesLoading, () => this.onLoadMoreClick(), 'D MMMM YYYY - HH:mm'))}
                    `, this.trophies?.length > 0)}
                </div>
                <div class="content-container" ${animate()}>
                    ${getStatisticTemplate('images/green-star.svg', true, html`
                        <span class="text-heading2">${peakPoints * peakPointsPerDay}</span>
                        <span class="text-primary"><or-translate value="panel_trophies.totalPeaksAvoided"></or-translate></span>
                    `, html`
                        <span class="text-primary">${peakPoints}</span>
                        <span class="text-primary"><or-translate value="panel_trophies.peakPointsEarned"></or-translate></span>
                    `, html`
                        ${until(this.getTrophiesTemplate('peak', this.peakTrophies, this.peakPointsLoading, () => this.onLoadMorePeaksClick(), 'D MMMM YYYY'))}
                    `, this.peakTrophies?.length > 0)}
                </div>
            </div>
        `;
    }

    protected async getTrophiesTemplate(type: 'challenges' | 'peak', trophies: TrophyItem[], loading: boolean, onLoadMore: () => void, timeFormat?: string): Promise<TemplateResult> {
        const chips: Chip[] = [
            {text: html`<or-translate value="panel_trophies.loadMore"></or-translate>`, loading: loading, action: () => onLoadMore()}
        ];
        return html`
            <div style="padding-top: 8px;">
                ${when(trophies.length === 0, () => html`
                    <div style="display: flex; justify-content: center; padding: 16px;">
                        <span><or-translate value="${type === 'challenges' ? 'panel_trophies.noChallengesFound' : 'panel_trophies.noPeaksFound'}"></or-translate></span>
                    </div>
                `, () => html`
                    ${map(trophies, item => html`
                        <og-trophy-item .item="${item}" divider="${true}" .timeFormat="${timeFormat}"></og-trophy-item>
                    `)}
                `)}
            </div>
            <div style="display: flex; justify-content: center; margin-top: 12px;">
                <og-chips .chips="${chips}" .outlined="${true}"></og-chips>
            </div>
        `;
    }

    // When attempting to load more, we decrease currentHistoryDate (for example by 1 week).
    // This will trigger the fetching of the challenges during willUpdate();
    protected onLoadMoreClick() {
        this.currentHistoryDate = moment(this.currentHistoryDate).subtract(Defaults.HISTORY_FETCH_AMOUNT, (Defaults.HISTORY_FETCH_UNIT as any)).toDate();

        // 'fake' loading mechanism for visualization
        this.challengesLoading = true;
        setTimeout(() => this.challengesLoading = false, 500);
    }

    protected onLoadMorePeaksClick() {
        this.peakCurrentHistoryDate = moment(this.peakCurrentHistoryDate).subtract(Defaults.HISTORY_FETCH_AMOUNT, (Defaults.HISTORY_FETCH_UNIT as any)).toDate();

        // 'fake' loading mechanism for visualization
        this.peakPointsLoading = true;
        setTimeout(() => this.peakPointsLoading = false, 500);
    }
}
