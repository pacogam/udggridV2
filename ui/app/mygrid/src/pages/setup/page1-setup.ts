import {AppStateKeyed, router} from '@openremote/or-app';
import {customElement, state} from 'lit/decorators.js';
import {TemplateResult, html} from 'lit';
import {when} from 'lit/directives/when.js';
import { map } from 'lit/directives/map.js';
import { until } from 'lit/directives/until.js';
import {Store} from '@reduxjs/toolkit';
import {InputType} from '@openremote/or-mwc-components/or-mwc-input';
import '../../components/og-loading';
import manager from '@openremote/core';
import {isAxiosError} from '@openremote/rest';
import {i18next} from '@openremote/or-translate';
import {GridAppStateKeyed, setUserAsset} from '../../util/og-state';
import {Asset} from 'model';
import {OgOnboardingPage, OnboardPage} from '../util/og-onboarding-page';
import {OgPageProvider} from '../util/og-page';
import {Task} from '@lit/task';
import rest from "rest";

export function page1SetupProvider(store: Store<GridAppStateKeyed>): OgPageProvider<AppStateKeyed> {
    return {
        name: 'setup',
        routes: [
            'setup'
        ],
        hideHeader: false,
        pageCreator: () => new Page1Setup(store)
    };
}

@customElement('page1-setup')
export class Page1Setup extends OgOnboardingPage {

    get name(): string {
        return 'setup';
    }

    stateChanged(state: GridAppStateKeyed): void {
        super.stateChanged(state);
        if (state.gridApp.userAssetId && this.currentPageIndex === 0) {
            console.debug('Asset from user is already present! Navigating from setup to home page.');
            router.navigate('');
        } else {
            this.userAsset = state.gridApp.assets.find(a => a.id === state.gridApp.userAssetId);
        }
    }

    @state()
    protected userAsset?: Asset;

    @state()
    protected dongleCode?: string;

    @state()
    protected pages: OnboardPage[] = [
        {
            getHeading: () => i18next.t('setup.page2-heading'),
            pageContent: (): TemplateResult => {
                return html`
                    <div style="padding: 64px 0; display: flex; flex-direction: column; gap: 32px; text-align: center;">
                        <span class="text-secondary">${i18next.t('setup.page2-text')}</span>
                        <div style="text-align: start;">
                            <og-input id="code-input" .type="${InputType.TEXT}" filled minLength="1" .label="${i18next.t('setup.page2-inputlabel')}" style="width: 100%;"
                                      .disabled="${this.userAsset}"
                                      @or-mwc-input-changed="${ev => this._onDongleCodeChange(ev)}"
                            ></og-input>
                        </div>
                        <div style="height: 25vh; display: flex; align-items: center; justify-content: center;">
                            ${this._connectDongleTask.render({
                                initial: () => html``,
                                pending: () => html`
                                    <img src="images/dots-loading.svg" />
                                `,
                                complete: (value) => html`
                                    ${when(value, () => html`
                                        ${when(this.userAsset, () => html`
                                            <panel-device-info .meterAsset="${this.userAsset}" .static="${true}" style="width: 100%;"></panel-device-info>
                                        `, () => html`
                                            <img src="images/dots-loading.svg"/>
                                        `)}
                                    `)}
                                `,
                                error: (error: Error) => {
                                    return html`
                                        <span class="text-secondary bold" style="color: var(--og-color-danger);">${error.message}</span>
                                    `;
                                },
                            })}
                        </div>
                    </div>
                `;
            },
            noBottomGraphic: true,
            getActionText: () => i18next.t('setup.page2-action'),
            getActionDisabled: () => !this.userAsset
        }
    ];

    protected onActionClick = async (): Promise<void> => {
        if (this.currentPageIndex === 1) {
            router.navigate('');
        } else {
            this.switchPage('next');
        }
    };

    protected getOnboardingPagesContent(pages: OnboardPage[]): TemplateResult {
        return html`
            ${map(pages, (p, index) => {
                return html`
                        <div slot="${index}" style="height: 100%; width: 100%; overflow: auto;">
                            ${until(p.pageContent(), html`
                                <og-loading></og-loading>
                            `)}
                        </div>
                    `;
            })}
        `;
    }

    /**
     * HTML callback when a user changes the "dongle code" input field.
     */
    protected _onDongleCodeChange(ev: CustomEvent) {
        this.dongleCode = ev.detail.value;
    }


    protected _connectDongleTask = new Task(this, {
        task: async ([dongleCode], {signal}) => {
            const success = await this.connectDevice(dongleCode);
            return success ? dongleCode : undefined;
        },
        args: () => [this.dongleCode]
    });


    // Main 'connect device' process method.
    // It links the device through the HTTP API, and queries the asset to be cached locally.
    // Several catches are in place to show an error message on screen.
    // When failing, it automatically navigates back to the previous page, aka the input page.
    protected async connectDevice(dongleCode: string, validate = true): Promise<boolean> {
        console.debug(`Trying to connect to ${dongleCode}`);
        if (!dongleCode) {
            return false;
        }

        // Validate code if necessary
        if (dongleCode && validate) {
            dongleCode = this.formatCode(dongleCode);
        }

        // Return error if invalid
        if (!dongleCode) {
            await new Promise(resolve => setTimeout(resolve, 500));
            throw new Error(`${i18next.t('error.setupCodeInvalid')}`);
        }

        console.debug(`Connecting to ${dongleCode}`);
        try {
            await rest.api.DeviceResource.linkDevice({
                deviceName: dongleCode,
                assetType: 'OurgridMeterAsset'
            });
        } catch (e) {
            console.error(e);
            if (isAxiosError(e)) {
                if (e.response.status === 404) {
                    throw new Error(i18next.t('error.setupDeviceNotFound'));
                } else if (e.response.status === 403) {
                    throw new Error(i18next.t('error.setupDeviceAlreadyLinked'));
                } else {
                    throw new Error(i18next.t('error.unknown'));
                }
            } else {
                throw new Error(i18next.t('error.unknown'));
            }
        }

        // Add a delay
        await new Promise(resolve => setTimeout(resolve, 1000));

        // Fetch and cache the asset
        // Error handling is done differently, since we don't expect it to fail.
        // If so, since the linking process already took place, and it 'blocks linking the same device', we recommend a browser refresh
        try {
            const asset = (await manager.rest.api.AssetResource.queryAssets({
                realm: {name: manager.displayRealm},
                types: ['OurgridMeterAsset']
            })).data[0];
            if (asset) {
                this._store.dispatch(setUserAsset(asset));
            } else {
                throw new Error();
            }

        } catch (e) {
            window.location.hash = "";
            window.location.reload();
        }

        return true;
    }

    protected formatCode(dongleCode: string): string | undefined {
        const splitted = dongleCode.split(' ');
        return splitted.find(s => {
            if (s.length === 32) {
                const split = s.split('-');
                return (split.length === 5 &&
                    split[0].length === 8 &&
                    split[1].length === 4 &&
                    split[2].length === 4 &&
                    split[3].length === 4 &&
                    split[4].length === 8
                );
            }
        });
    }
}
