import {css, html, LitElement, PropertyValues } from 'lit';
import { customElement, property, query, state } from 'lit/decorators.js';
import {getAppStyle} from '../styles';
import {OurgridMeterAsset} from '../util/util';
import {Asset} from '@openremote/model';
import {OgLoading} from '../components/og-loading';
import moment from 'moment';

const styling = css`

`;

@customElement('og-challenge-timer')
export class OgChallengeTimer extends LitElement {

    @property()
    protected userAsset: OurgridMeterAsset;

    @property()
    protected challengeAsset: Asset;

    @state()
    protected color = 'blue';

    @state()
    protected progress = 0;

    @query('og-loading')
    protected loadingElem: OgLoading;

    protected challengeDurationMs?: number;
    protected challengeStartTimestamp?: number;
    protected challengeEndTimestamp?: number;
    protected lastResetTimestamp?: number;
    protected nextResetTimestamp?: number;
    protected timerInterval?: NodeJS.Timeout;

    static get styles() {
        return [getAppStyle(), styling];
    }

    // Lit lifecycle method to process variables during update
    protected willUpdate(changedProps: PropertyValues) {

        // Process challenge asset updates
        if(changedProps.has('challengeAsset') && this.challengeAsset) {
            const challengeStart = this.challengeAsset.attributes['challengeStart']?.value;
            if(challengeStart !== undefined) {
                this.challengeStartTimestamp = moment(challengeStart).toDate().getTime();
            }
            const challengeEnd = this.challengeAsset.attributes['challengeEnd']?.value;
            if(challengeEnd !== undefined) {
                this.challengeEndTimestamp = moment(challengeEnd).toDate().getTime();
            }
            const challengePointInterval = this.challengeAsset.attributes['challengeEarnPointInterval']?.value;
            if(challengePointInterval !== undefined) {
                this.challengeDurationMs = Number.parseInt(challengePointInterval, 10) * 60 * 1000;
            }
        }

        // Process meter asset updates;
        // updating color according to power goal, and parse time attributes to milliseconds.
        if(changedProps.has('userAsset') && this.userAsset) {
            this.color = this.getStrokeColorByStatus(this.userAsset.attributes['power']?.value, this.userAsset.attributes['challengePowerLimit']?.value);

            // Parse time attributes to milliseconds using Moment
            const endChallenge = this.challengeEndTimestamp || Number.MAX_SAFE_INTEGER;
            const timerStart = (this.userAsset.attributes['challengePointTimerStart']?.value as number | undefined) || Number.MAX_SAFE_INTEGER;
            const timerEnd = moment(timerStart).add(this.challengeDurationMs, 'milliseconds').toDate().getTime() || Number.MAX_SAFE_INTEGER;
            const lastResetTimestamp = Math.min(timerStart, endChallenge);
            const nextResetTimestamp = Math.min(timerEnd, endChallenge);

            // Change variables and reset timer if values have changed
            if(this.lastResetTimestamp !== lastResetTimestamp || this.nextResetTimestamp !== nextResetTimestamp) {
                this.lastResetTimestamp = lastResetTimestamp;
                this.nextResetTimestamp = nextResetTimestamp;
                this.resetTimer();
            }
        }
    }

    protected resetTimer() {
        console.log('Resetting challenge timer! (visually)');
        clearInterval(this.timerInterval);
        const relativeNext = (this.nextResetTimestamp - this.lastResetTimestamp);
        const timeout = relativeNext / 400; // every 0,25% of the circle
        this.timerInterval = setInterval(() => {
            const relativeNow = (new Date().getTime() - this.lastResetTimestamp);
            this.progress = Math.min(Math.max(relativeNow / relativeNext, 0), 1);
        }, timeout);
    }

    protected getStrokeColorByStatus(powerValue?: number, goal?: number) {
        if(powerValue && goal) {
            return (powerValue > goal) ? 'var(--og-color-warning)' : 'var(--og-color-success)';
        } else {
            return 'var(--og-color-error)';
        }
    }

    protected render() {
        return html`
            <div>
                <og-loading .determinate="${true}" .size="${256}" .strokePx="${3}" .progress="${this.progress}" style="--og-loading-color: ${this.color}"></og-loading>
            </div>
        `;
    }
}
