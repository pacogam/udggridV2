import {css, html, LitElement, nothing, TemplateResult} from 'lit';
import {customElement, property, query} from 'lit/decorators.js';
import {classMap} from 'lit/directives/class-map.js';
import {getAppStyle} from '../styles';
import {Asset, AssetDatapointIntervalQueryFormula, ValueDatapoint} from '@openremote/model';
import manager, {Util} from '@openremote/core';
import moment from 'moment';
import {until} from 'lit/directives/until.js';
import {i18next} from '@openremote/or-translate';
import {map} from 'lit/directives/map.js';
import {getStateColorByPowerValue, OgStateColor} from '../util/util';
import { when } from 'lit/directives/when.js';
import {Constants} from '../util/constants';
import {Defaults} from '../util/defaults';

const styling = css`
  .progress-header {
    display: flex;
    justify-content: space-between;
    padding: 0 10px;
  }
  .progress-header--noPadding {
    padding: 0;
  }

  .progress-container {
    background: var(--og-color-secondary);
    height: 32px;
    display: flex;
    text-align: center;
    align-items: center;
    padding: 0 10px;
    position: relative;
  }

  .progress-container--dark {
    background: var(--og-color-secondary-dark);
  }
  
  .progress-container--noPadding {
    padding: 0;
  }

  #progress-bar {
    flex: 1;
    height: 100%;
    display: flex;
    gap: 2px;
    transition: max-width 2s var(--og-easing-standard);
    -webkit-transition: max-width 2s var(--og-easing-standard);
  }

  .cursor-top {
    position: absolute;
    top: -6px;
    width: 4px;
    height: 8px;
    border-radius: 50%;
    background: var(--og-color-neutral);
  }

  .cursor-bar {
    position: absolute;
    bottom: -4px;
    left: -6px;
    width: 0;
    height: 0;
    border-left: 8px solid transparent;
    border-right: 8px solid transparent;
    border-bottom: 8px solid var(--og-color-neutral);
    border-radius: 2px;
  }

  .cursor-bottom {
    position: absolute;
    bottom: -28px;
    left: -32px;
    width: 64px;
    text-align: center;
  }
`;

@customElement('og-challenge-progress')
export class OgChallengeProgress extends LitElement {

    @property({type: Object})
    protected readonly meterAsset?: Asset;

    @property({type: Object})
    protected readonly challengeAsset?: Asset;

    @property({type: Boolean})
    public dark = false;

    @property({type: Boolean})
    public noLabel = false;

    @property({type: Boolean})
    public noPadding = false;

    @query('#progress-bar')
    protected progressBarElem: HTMLElement;

    static get styles() {
        return [getAppStyle(), styling];
    }

    protected render() {
        const containerClasses: {} = {
            'progress-container': true,
            'progress-container--dark': this.dark,
            'progress-container--noPadding': this.noPadding
        };

        // Parse attribute values to timestamps in ms
        const challengeDuration = this.challengeAsset?.attributes?.[Constants.CHALLENGE_DURATION_ATTRIBUTE]?.value || Defaults.CHALLENGE_DURATION_MINUTES;
        const challengeDurationMs = challengeDuration * 60 * 1000;
        const startAttributeValue = this.challengeAsset?.attributes?.[Constants.CHALLENGE_START_TIME_ATTRIBUTE]?.value ||  moment().subtract(challengeDuration, 'minutes');
        const endAttributeValue = this.challengeAsset?.attributes?.[Constants.CHALLENGE_END_TIME_ATTRIBUTE]?.value || moment();
        const startMoment = moment(startAttributeValue);
        const endMoment = moment(endAttributeValue);
        const now = moment();

        // Determine what percentage of the bar is left based on current time.
        let remainingWidthPercentage = 0;
        if(endMoment.isAfter(now)) {
            const percentage = Math.round((endMoment.valueOf() - now.valueOf()) / challengeDurationMs * 100);
            remainingWidthPercentage = Math.min(percentage, 100);
        }

        // Render html
        return html`
            <div id="progress-wrapper">
                <div class="${this.noPadding ? 'progress-header progress-header--noPadding' : 'progress-header'}">
                    <span class="text-secondary">${startMoment.format('HH:mm')}</span>
                    <span class="text-secondary">${endMoment.format('HH:mm')}</span>
                </div>
                <div id="progress-content">
                    <div id="progress-container" class=${classMap(containerClasses)}>
                        ${until(this.getProgressTemplate(startMoment.valueOf(), endMoment.valueOf()), html`<og-loading size="small"></og-loading>`)}
                        ${!this.noLabel ? until(this.getCursorTemplate(endMoment.valueOf())) : nothing}
                        ${when(remainingWidthPercentage > 0, () => html`
                            <div style="height: 100%; flex: 0 0 ${remainingWidthPercentage}%;" />
                        `)}
                    </div>
                </div>
            </div>
        `;
    }

    protected async getProgressTemplate(start: number, end: number): Promise<TemplateResult> {
        if(this.meterAsset) {
            try {
                // Fetch datapoint data between start and end.
                console.log("Calculating challenge progress..."); // TODO: Temporary debugging purposes
                const powerData = await this.getPowerDatapoints(this.meterAsset.id, 'power', start, end);
                console.log(powerData); // TODO: Temporary debugging purposes
                const maxValue = this.meterAsset.attributes?.[Constants.METER_POWER_MAX_ATTRIBUTE]?.value;

                // Grouping power values per color
                const powerBlocks = new Map<number, [number, OgStateColor]>();
                powerData.forEach(data => {
                    if(data.y !== undefined) {
                        const color = getStateColorByPowerValue(data.y, maxValue);

                        const lastValue = Array.from(powerBlocks.entries()).reverse()[0];
                        if(lastValue && lastValue[1][1] === color) {
                            const newVal = lastValue[1][0] + 1;
                            powerBlocks.set(lastValue[0], [newVal, lastValue[1][1]]);
                        } else {
                            powerBlocks.set(data.x, [1, color]);
                        }
                    }
                });

                // TODO: Temporary debugging purposes
                console.log(powerBlocks);

                // Render grouped color values with a percentage of the bar
                const dataAmount = powerData.filter(data => data.y !== undefined).length;
                const widthPercentage = (100 / dataAmount);

                // Animate to full width after 500ms
                setTimeout(() => {
                    if(this.progressBarElem) {
                        this.progressBarElem.style.maxWidth = '100%';
                    }
                }, 500);

                return html`
                    <div id="progress-bar" style="max-width: 0;">
                        ${map(powerBlocks, val => html`<div style="width: ${(widthPercentage * val[1][0]).toFixed(Defaults.CHALLENGE_PROGRESS_DIGIT_AMOUNT)}%; background: ${val[1][1]};"></div>`)}
                    </div>
                    <div style="position: absolute; top: 0; width: calc(100% - 20px); height: 8px; display: flex;">
                        ${map(powerData, _data => html`
                            <div style="flex: 1; border-right: 1px solid black;"></div>
                        `)}
                    </div>
                `;
            } catch (e) {
                return html`<span class="text-tertiary" style="flex: 1; color: ${this.dark ? 'var(--og-color-primary)' : 'var(--og-color-primary-dark)'}">${i18next.t('error.unknown')}</span>`;
            }
        } else {
            return html`<span class="text-tertiary" style="flex: 1; color: ${this.dark ? 'var(--og-color-primary)' : 'var(--og-color-primary-dark)'}">${i18next.t('error.unknown')}</span>`;
        }
    }

    /*protected getMaxValueByDatapoint(powerMaxDatapoints: ValueDatapoint<any>[], datapoint: ValueDatapoint<any>): number {
        return powerMaxDatapoints.filter(dp => (dp.x <= datapoint.x)).reverse()[0].y as number;
    }*/

    protected async getCursorTemplate(end: number): Promise<TemplateResult> {
        return html`
            <div style="height: 100%; flex: 0 0 4px; background: var(--og-color-neutral); position: relative;">
                <div class="cursor-top"></div>
                <div class="cursor-bar"></div>
                <div class="cursor-bottom">
                    ${when(new Date().getTime() <= end, () => html`
                        <span class="text-tertiary">${moment().format('HH:mm')}</span>
                    `)}
                </div>
            </div>
        `;
    }

    protected getPowerDatapoints(meterId: string, type: 'power' | 'max', startTimestamp = moment().subtract(1, 'hour').valueOf(), endTimestamp = moment().valueOf()): Promise<ValueDatapoint<any>[]> {
        const deferred = new Util.Deferred<ValueDatapoint<any>[]>();
        const diff = endTimestamp - startTimestamp;
        const intervalInSeconds = Math.max(diff / 100 / 1000, Defaults.CHALLENGE_PROGRESS_DATAPOINT_INTERVAL_SECONDS);
        const attribute: string = (type === 'power') ? Constants.METER_POWER_ATTRIBUTE : Constants.METER_POWER_MAX_ATTRIBUTE;
        if(meterId) {
            manager.rest.api.AssetDatapointResource.getDatapoints(meterId, attribute, {
                type: 'interval',
                fromTimestamp: startTimestamp,
                toTimestamp: endTimestamp,
                formula: AssetDatapointIntervalQueryFormula.MAX,
                gapFill: true,
                interval: `${intervalInSeconds} seconds`
            }).then(response => {
                deferred.resolve(response.data);
            }).catch(reason => {
                deferred.reject(reason);
            });
        } else {
            deferred.reject('Could not get datapoints for max power; meterId was not present.');
        }
        return deferred.promise;
    }
}
