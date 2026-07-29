import {OgPage, OgPageProvider, PageAnimationType} from './util/og-page';
import {GridAppStateKeyed, realmSelector, setServiceUserData} from '../util/og-state';
import {customElement, state} from 'lit/decorators.js';
import {html, css, TemplateResult, PropertyValues} from 'lit';
import {InputType, OrInputChangedEvent} from "@openremote/or-mwc-components/or-mwc-input";
import '../panels/panel-home-automation-credentials';
import {Store} from '@reduxjs/toolkit';
import {GraphicType} from '../features/og-usage-graphic';
import {router} from '@openremote/or-app';
import { until } from 'lit/directives/until.js';
import {OgHomeAutomationBrand, OurgridMeterAsset} from "../util/util";
import {Asset, DeviceCharacteristic, User, WellknownCharacteristics} from "model";
import rest from "rest";
import ogManager from "../util/og-manager";
import {showSnackbar} from "../components/og-snackbar";
import {resolveOgMarkdown} from "../util/directives";
import { when } from 'lit/directives/when.js';
import {i18next} from "@openremote/or-translate";

// @ts-ignore
import HomeAssistantMd_EN from "../docs/guide-homeassistant-en.md";
// @ts-ignore
import HomeAssistantMd_NL from "../docs/guide-homeassistant-nl.md";
// @ts-ignore
import OpenHABMd_EN from "../docs/guide-openhab-en.md";
// @ts-ignore
import OpenHABMd_NL from "../docs/guide-openhab-nl.md";

export function pageHomeAutomationProvider(store: Store<GridAppStateKeyed>): OgPageProvider<GridAppStateKeyed> {
    return {
        name: 'homeautomation',
        routes: ['homeautomation'],
        pageCreator: () => new PageHomeAutomation(store),
        skipDataCheck: true
    };
}

const styling = css`
    .page-wrapper {
        width: 100%;
        height: 100vh;
        background: var(--og-color-primary);
        display: flex;
        flex-direction: column;
        align-items: center;
    }

    .page-back-icon {
        position: absolute;
        left: 0;
        z-index: 12;
        --or-icon-fill: var(--og-color-primary-dark);
        --og-mdc-fab-size: 62px;
        --og-mdc-fab-border-radius: 0 50% 50% 50%;
        --og-mdc-fab-transition: all ease 0.5s;
    }

    .page-graphic {
        width: calc(100% - 32px);
        margin-top: 12px;
    }

    .page-title {
        margin-top: -40px;
    }

    .page-content {
        flex: 1;
        width: calc(100% - 32px);
        padding: 64px 16px 16px 16px;
    }
    
    .page-content-container {
        display: flex;
        flex-direction: column;
        gap: 16px;
    }
    
    #homeautomation-install-guide {
        overflow: hidden;
        margin-bottom: 48px;
    }
    
    pre:has(code) {
        position: relative;
        display: flex;
        padding: 16px;
        background: var(--og-background-shade);
        border-radius: calc(var(--og-panel-border-radius) / 2);
        max-height: 200px;
        margin-left: -40px;
    }
    
    pre > code {
        flex: 1;
        overflow: scroll;
    }
    
    pre:has(code) > header {
        position: absolute;
        right: 0;
        top: 0;
        padding: 4px 8px;
        font-family: var(--og-font-family), serif;
        font-size: var(--og-font-size-button-small);
        font-weight: bold;
        color: black;
        background: #d9d9d9;
        border-radius: 0 calc(var(--og-panel-border-radius) / 2) 0 0;
    }

    table {
        display: block;
        overflow: scroll;
    }
    
    th, td {
        white-space: nowrap;
        padding: 1px 12px 1px 0;
    }
`;

@customElement('page-homeautomation')
export class PageHomeAutomation extends OgPage<GridAppStateKeyed> {

    static HOUSEHOLD_CHARACTERISTICS_ATTRIBUTE = 'householdEnergyCharacteristics';

    getAnimationEnterType = () => PageAnimationType.SWIPE_LEFT;
    getAnimationExitType = () => PageAnimationType.SWIPE_RIGHT;

    @state()
    protected serviceUser?: User;

    @state()
    protected userAsset?: OurgridMeterAsset;

    @state()
    protected districtAsset?: Asset;

    @state()
    protected batteryAsset?: Asset;

    @state()
    protected challengeAsset?: Asset;

    @state()
    protected peakPointsAsset?: Asset;

    @state()
    protected characteristics?: DeviceCharacteristic[];

    @state()
    protected _language?: string;

    protected clickableElements: Element[] = [];

    get name(): string {
        return 'homeautomation';
    }

    static get styles() {
        return [...super.styles, styling];
    }

    stateChanged(state) {
        this._language = state.gridApp.language;
        this.serviceUser = state.gridApp.serviceUser;
        this.userAsset = state.gridApp.assets.find(a => a.id === state.gridApp.userAssetId);
        this.districtAsset = state.gridApp.assets.find(a => a.id === state.gridApp.districtAssetId);
        this.batteryAsset = state.gridApp.assets.find(a => a.id === state.gridApp.batteryAssetId);
        this.challengeAsset = state.gridApp.assets.find(a => a.id === state.gridApp.challengeAssetId);
        this.peakPointsAsset = state.gridApp.assets.find(a => a.id === state.gridApp.peakPointsAssetId);
        const characteristics = JSON.parse(this.userAsset?.attributes?.[PageHomeAutomation.HOUSEHOLD_CHARACTERISTICS_ATTRIBUTE]?.value) as DeviceCharacteristic[] | undefined;
        if (characteristics && JSON.stringify(characteristics) !== JSON.stringify(this.characteristics)) {
            this.characteristics = characteristics;
        }
        return super.stateChanged(state);
    }

    willUpdate(changedProps: PropertyValues) {
        console.log(changedProps);
        return super.willUpdate(changedProps);
    }

    firstUpdated(changedProps: PropertyValues) {
        if(!this.serviceUser) {
            // If no linked service user is found, fetch it from the server.
            rest.api.UserAccountResource.getServiceUserAccount().then(response => {
                if(response.status === 200 && response.data !== undefined) {
                    this._store.dispatch(setServiceUserData(response.data));
                }
            });
        }
        return super.firstUpdated(changedProps);
    }

    protected render(): TemplateResult {
        const homeAutomationInfo = this.characteristics?.find(c => c.id === WellknownCharacteristics.HOME_AUTOMATION);
        const promise = this._getHomeAutomationContent(homeAutomationInfo.brand);
        const onCodeClick = (el: Element) => {
            if(navigator.clipboard) {
                navigator.clipboard.writeText(el.querySelector('code')?.textContent || '').then(() => {
                    showSnackbar(undefined, i18next.t('copySuccess')); // TODO: Translate
                });
            } else {
                console.warn("Could not copy code to clipboard. Please use the browser's copy function instead.")
            }
        }
        // After rendering Home Automation content, make code blocks clickable that copy the code
        promise.finally(() => setTimeout(() => {
            this.clickableElements.forEach(x => x.addEventListener('click', () => onCodeClick(x)))
            this.shadowRoot.querySelectorAll('pre:has(code)').forEach(el => {
                const header = document.createElement('header');
                header.textContent = i18next.t('copy');
                el.insertBefore(header, el.childNodes[0]);
                el.addEventListener('click', () => onCodeClick(el));
                this.clickableElements.push(el);
            })
        }));
        // Return the final HTML content
        return html`
            <div class="page-wrapper">
                <og-input class="page-back-icon" type=${InputType.BUTTON} action icon="chevron-left" @or-mwc-input-changed=${this._onBackClick}></og-input>
                <div class="page-graphic">
                    <og-usage-graphic id="onboarding-topgraphic" .type="${GraphicType.HEADER}" small></og-usage-graphic>
                </div>
                <div class="page-title">
                    <or-translate class="text-heading" value="page-homeAutomation.heading" style="text-align: center; max-width: 65vw;"></or-translate>
                </div>
                <div class="page-content">
                    <div class="page-content-container">
                        ${until(promise, html`Loading...`)}
                    </div>
                </div>
            </div>
        `;
    }

    protected _onBackClick(ev: OrInputChangedEvent) {
        router.navigate('devices');
    }

    protected async _getHomeAutomationContent(software?: OgHomeAutomationBrand): Promise<TemplateResult> {
        let markdown: string | undefined;
        switch (software) {
            case OgHomeAutomationBrand.HOME_ASSISTANT: markdown = this._processMarkdown(this._language === 'nl' ? HomeAssistantMd_NL : HomeAssistantMd_EN); break;
            case OgHomeAutomationBrand.OPENHAB: markdown = this._processMarkdown(this._language === 'nl' ? OpenHABMd_NL : OpenHABMd_EN); break;
            default: markdown = undefined; break;
        }
        return html`
            <panel-homeautomation-credentials .serviceUser=${this.serviceUser}></panel-homeautomation-credentials>
            ${when(this.serviceUser && markdown !== undefined, () => html`
                <div id="homeautomation-install-guide">
                    ${resolveOgMarkdown(markdown, { includeCodeBlockClassNames: true })}
                </div>
            `)}
        `;
    }

    protected _processMarkdown(markdown: string): string {
        const replacements = {
            '{{hostname}}': ogManager?.managerUrl || window.location.hostname || '???',
            '{{realm}}': realmSelector(this.getState()) || '???',
            '{{client_id}}': this.serviceUser?.username || '???',
            '{{client_secret}}': this.serviceUser?.secret || '???',
            '{{meter_asset_id}}': this.userAsset?.id || '???',
            '{{district_asset_id}}': this.districtAsset?.id || '???',
            '{{challenge_asset_id}}': this.challengeAsset?.id || '???',
            '{{peak_points_asset_id}}': this.peakPointsAsset?.id || '???',
            '{{battery_asset_id}}': this.batteryAsset?.id || '???'
        };
        for (const [key, value] of Object.entries(replacements)) {
            const regex = new RegExp(key, 'g');
            markdown = markdown.replace(regex, value);
        }
        return markdown;
    }
}
