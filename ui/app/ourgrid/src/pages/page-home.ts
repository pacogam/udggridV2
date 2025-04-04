import {css, html, PropertyValues, TemplateResult} from 'lit';
import {customElement, state} from 'lit/decorators.js';
import {AppStateKeyed} from '@openremote/or-app';
import {Store} from '@reduxjs/toolkit';
import {Asset, Attribute, Challenge} from 'model';
import '../components/og-panel-wrapper';
import '../panels/panel-usage-overview';
import '../panels/panel-challenge-progress';
import '../panels/panel-challenge-points';
import '../panels/panel-peak-notification';
import '../panels/panel-peak-usage';
import '../panels/panel-challenge-tips';
import '../panels/panel-usage-history';
import {GridAppStateKeyed, setDark} from '../util/og-state';
import {OgPage, OgPageProvider} from './util/og-page';
import {OgMeterChallengeState, OgMeterConnectedState} from '../util/util';
import {Constants} from '../util/constants';
import manager from '@openremote/core';
import moment from 'moment';
import {showChallengeMissedDialog, showLastChallengeResultDialog} from '../components/og-dialog';
import {Defaults} from '../util/defaults';
import rest from "rest";
import { guard } from 'lit/directives/guard.js';

export function pageHomeProvider(store: Store<GridAppStateKeyed>): OgPageProvider<AppStateKeyed> {
    return {
        name: 'home',
        routes: ['home'],
        pageCreator: () => new PageHome(store)
    };
}

const styling = css`
  .page-wrapper {
    width: 100%;
    height: 100vh;
    background: var(--og-color-primary);
  }
`;

@customElement('page-home')
export class PageHome extends OgPage<GridAppStateKeyed> {

    @state()
    protected loading = false;

    @state()
    protected meterAsset?: Asset;

    @state()
    protected batteryAsset?: Asset;

    @state()
    protected districtAsset?: Asset;

    @state()
    protected challengeAsset?: Asset;

    @state()
    protected language: string;

    @state()
    protected shownPanels: Set<string> = new Set<string>();

    /*@state()
    protected temp: boolean = true;*/

    get name(): string {
        return 'Home';
    }

    stateChanged(state: GridAppStateKeyed): void {
        this.meterAsset = state.gridApp.assets.find(a => a.id === state.gridApp.userAssetId);
        this.batteryAsset = state.gridApp.assets.find(a => a.id === state.gridApp.batteryAssetId);
        this.districtAsset = state.gridApp.assets.find(a => a.id === state.gridApp.districtAssetId);
        this.challengeAsset = state.gridApp.assets.find(a => a.id === state.gridApp.challengeAssetId);
        this.language = state.gridApp.language;

        return super.stateChanged(state);
    }

    async getLoadingPromise(prev?: string): Promise<void> {
        if(!prev) {
            // If new to the home page, force wait 1,5 seconds, so all data can properly load.
            await new Promise((r) => setTimeout(r, 1500));
        }
        return super.getLoadingPromise();
    }

    protected willUpdate(changedProps: PropertyValues) {
        if(changedProps.has('meterAsset') && this.meterAsset) {

            const connected = this.meterAsset.attributes['connectionStatus']?.value === OgMeterConnectedState.CONNECTED;
            const challengeStatus: OgMeterChallengeState = this.meterAsset.attributes['challengeStatus']?.value;
            const showJoinNotification = connected && [OgMeterChallengeState.JOIN_CHALLENGE, OgMeterChallengeState.JOINED_CHALLENGE].includes(challengeStatus);
            const isInChallenge = connected && challengeStatus === OgMeterChallengeState.ACTIVE_CHALLENGE;
            this._store.dispatch(setDark(isInChallenge)); // TODO: Improve this

            const panels = new Set<string>();
            if(showJoinNotification) {
                panels.add('panel-peak-notification');
            }
            if(isInChallenge) {
                panels.add('panel-challenge-progress');
                panels.add('panel-challenge-points');
                panels.add('panel-peak-usage');
                panels.add('panel-challenge-tips');
            }

            // Comparing sets
            const eqSet = (set1, set2) => set1.size === set2.size && [...set1].every(x => set2.has(x));
            if(!eqSet(this.shownPanels, panels)) {
                this.shownPanels = panels;
            }

            // Compare challenge status, and show 'challenge completed' panel if necessary
            const prevAsset: Asset | undefined = changedProps.get('meterAsset') as Asset | undefined;
            if(prevAsset && prevAsset.attributes?.['challengeStatus']?.value === OgMeterChallengeState.ACTIVE_CHALLENGE && challengeStatus === OgMeterChallengeState.NO_CHALLENGE) {
                this.showChallengeCompletedModal(this.meterAsset, this.challengeAsset);
            }
        }
    }

    protected firstUpdated(changedProps: PropertyValues) {
        if(this.meterAsset && this.challengeAsset) {
            this.checkForChallengeFinishModal(this.meterAsset, this.challengeAsset).then(result => {
                if(result) {
                    this.showChallengeCompletedModal(this.meterAsset, this.challengeAsset);
                }
            });
        } else {
            console.warn('meterAsset and/or challengeAsset were not present during firstUpdated()');
        }

        const urlParams = new URLSearchParams(window.location.search);
        if(urlParams.has(Constants.CHALLENGE_NOTIFICATION_PARAMS_NAME) && urlParams.get(Constants.CHALLENGE_NOTIFICATION_PARAMS_NAME) === "true") {
            this.checkForChallengeMissedModal(this.challengeAsset);
        }

        return super.firstUpdated(changedProps);
    }

    // Returns true or false depending on whether a "challenge finished" modal should be shown
    protected async checkForChallengeFinishModal(meterAsset: Asset, challengeAsset: Asset): Promise<boolean> {
        const lastTimestamp: string | null = localStorage.getItem(Constants.LOCALSTORAGE_LAST_CHALLENGE_COMPLETED_KEY);

        const challengeDuration: number = challengeAsset?.attributes?.[Constants.CHALLENGE_DURATION_ATTRIBUTE]?.value || Defaults.CHALLENGE_DURATION_MINUTES;
        const challengeWait: number = challengeAsset?.attributes?.[Constants.CHALLENGE_WAIT_ATTRIBUTE]?.value || Defaults.CHALLENGE_WAIT_MINUTES;
        const challengePointAttr: Attribute<any> | undefined = meterAsset?.attributes?.[Constants.CHALLENGE_POINT_CURRENT_ATTRIBUTE];

        const time = moment(challengePointAttr.timestamp).subtract(challengeDuration + challengeWait + 1, 'minutes');
        const data = (await rest.api.DeviceChallengesResource.getHistory({
            startTimestamp: time.valueOf(),
            endTimestamp: new Date().getTime()
        })).data as Challenge[];

        if(data.length === 1) {
            if(lastTimestamp === null) {
                return true;
            } else if(new Date().getTime() > data[0].endDate && data[0].endDate > (Number.parseInt(lastTimestamp))) {
                return true;
            }
        }
        return false;
    }

    protected showChallengeCompletedModal(meterAsset: Asset, challengeAsset: Asset) {
        showLastChallengeResultDialog(meterAsset, challengeAsset);
        localStorage.setItem(Constants.LOCALSTORAGE_LAST_CHALLENGE_COMPLETED_KEY, new Date().getTime().toString());
    }

    protected async checkForChallengeMissedModal(challengeAsset?: Asset) {
        const challengeEnd = challengeAsset?.attributes?.[Constants.CHALLENGE_END_TIME_ATTRIBUTE]?.value;
        if(challengeEnd) {
            const endMoment = moment(challengeEnd);
            const now = moment();
            if(now.isAfter(endMoment)) {
                this.showChallengeMissedModal();
            }
        }
    }

    protected showChallengeMissedModal() {
        showChallengeMissedDialog();
        const urlParams = new URLSearchParams(window.location.search);
        urlParams.delete(Constants.CHALLENGE_NOTIFICATION_PARAMS_NAME);
        const newUrl = `${window.location.origin + window.location.pathname}?${urlParams.toString()}`;
        window.history.pushState({path: newUrl},'',newUrl);
    }

    static get styles() {
        return [...super.styles, styling];
    }

    protected render(): TemplateResult {
        const connected = this.meterAsset?.attributes['connectionStatus']?.value === OgMeterConnectedState.CONNECTED;
        const challengeStatus: OgMeterChallengeState = this.meterAsset?.attributes['challengeStatus']?.value;
        const isInChallenge = connected && challengeStatus === OgMeterChallengeState.ACTIVE_CHALLENGE;
        return html`
            <div class="page-wrapper">

                <panel-usage-overview .dark="${isInChallenge}" .meterAsset="${this.meterAsset}" .districtAsset="${this.districtAsset}" .challengeAsset="${this.challengeAsset}"></panel-usage-overview>
                
                <og-panel-wrapper .panels="${this.shownPanels}" dark .meterAsset="${this.meterAsset}" .batteryAsset="${this.batteryAsset}" .challengeAsset="${this.challengeAsset}" .districtAsset="${this.districtAsset}"></og-panel-wrapper>

                <div style="background: var(--og-background-shade)">
                    ${guard([this.language], () => html`
                        <panel-usage-history .meterAsset="${this.meterAsset}" .districtAsset="${this.districtAsset}" .language="${this.language}"></panel-usage-history>
                    `)}
                </div>

                <panel-trophies .meterAsset="${this.meterAsset}" .challengeAsset="${this.challengeAsset}"></panel-trophies>

                <!-- Bottom margin -->
                <div style="height: 1px; margin-top: 10vh;"></div>
            </div>
        `;
    }
}
