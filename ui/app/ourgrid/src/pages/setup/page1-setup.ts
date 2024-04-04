import {AppStateKeyed, router} from '@openremote/or-app';
import {customElement, state} from 'lit/decorators.js';
import {TemplateResult, html} from 'lit';
import {when} from 'lit/directives/when.js';
import {Store} from '@reduxjs/toolkit';
import { InputType } from '@openremote/or-mwc-components/or-mwc-input';
import '../../components/og-loading';
import '../../features/og-characteristics-settings';
import manager from '@openremote/core';
import {isAxiosError} from '@openremote/rest';
import {i18next} from '@openremote/or-translate';
import {GridAppStateKeyed, setUserAsset} from '../../util/og-state';
import {Asset, DeviceCharacteristic} from '@openremote/model';
import {OgOnboardingPage, OnboardPage} from '../util/og-onboarding-page';
import {OgPageProvider} from '../util/og-page';
import {OgCharacteristicsUpdateEvent} from '../../features/og-characteristics-settings';

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

    @state()
    protected userAsset?: Asset;

    @state()
    protected characteristics?: DeviceCharacteristic[];

    @state()
    protected dongleCode?: string;

    @state()
    protected errorText?: string;

    @state()
    protected pages: OnboardPage[] = [
        {
            getHeading: () => i18next.t('setup.page1-heading'),
            headingStyle: 'heading',
            noBottomGraphic: true,
            pageContent: (): TemplateResult => {
                return html`
                    <div style="display: flex; flex-direction: column; gap: 32px; text-align: center; position: relative;">
                        <span class="text-secondary">${i18next.t('setup.page1-text')}</span>
                        <div>
                            <og-characteristics-settings @characteristics-changed="${(ev: OgCharacteristicsUpdateEvent) => 
                                    this.characteristics = ev.detail.valid ? ev.detail.characteristics : undefined}"
                            ></og-characteristics-settings>
                        </div>
                    </div>
                `;
            },
            getActionText: () => i18next.t('setup.page1-action'),
            getActionDisabled: () => this.characteristics === undefined
        },
        {
            getHeading: () => i18next.t('setup.page2-heading'),
            headingStyle: 'heading',
            pageContent: (): TemplateResult => {
                return html`
                    <div style="padding: 64px 0; display: flex; flex-direction: column; gap: 32px; text-align: center;">
                        <span class="text-secondary">${i18next.t('setup.page2-text')}</span>
                        <div style="text-align: start;">
                            <og-input id="code-input" .type="${InputType.TEXT}" filled minLength="1" .label="${i18next.t('setup.page2-inputlabel')}" .value="${this.dongleCode}" style="width: 100%;"
                                      @or-mwc-input-changed="${ev => this.onDongleCodeChange(ev)}"
                            ></og-input>
                            ${when(this.errorText, () => html`
                                <span style="color: #b00020">${this.errorText}</span>
                            `)}
                        </div>
                    </div>
                `;
            },
            noBottomGraphic: true,
            getActionText: () => i18next.t('setup.page2-action'),
            getActionDisabled: () => true
        },
        {
            getHeading: () => i18next.t('setup.page3-heading'),
            headingStyle: 'heading',
            pageContent: (): TemplateResult => {
                return html`
                    <div style="padding: 64px 0; display: flex; flex-direction: column; gap: 32px; align-items: center;">
                        <span>${i18next.t('setup.page3-text')}</span>
                        <og-loading></og-loading>
                    </div>
                `;
            },
            noBottomGraphic: true,
            getActionText: () => i18next.t('setup.page3-action'),
            getActionDisabled: () => true
        },
        {
            getHeading: () => i18next.t('setup.page4-heading'),
            headingStyle: 'heading',
            pageContent: (): TemplateResult => {
                return html`
                    <div style="padding: 64px 0; display: flex; flex-direction: column; gap: 32px; align-items: center; width: 100%;">
                        <span>${i18next.t('setup.page4-text')}</span>
                        <panel-device-info .meterAsset="${this.userAsset}" .static="${true}" style="width: 100%;"></panel-device-info>
                    </div>
                `;
            },
            noBottomGraphic: true,
            getActionText: () => i18next.t('setup.page4-action')
        }
    ];

    // Overriding onActionClick to switch pages on click.
    protected onActionClick = async (): Promise<void> => {
        if(this.currentPageIndex === 3) {
            router.navigate('');
        } else {
            this.switchPage('next');
        }
    };

    get name(): string {
        return 'setup';
    }

    stateChanged(state: GridAppStateKeyed): void {
        super.stateChanged(state);
        if(state.gridApp.userAssetId && this.currentPageIndex === 0) {
            console.debug('Asset from user is already present! Navigating from setup to home page.');
            router.navigate('');
        } else {
            this.userAsset = state.gridApp.assets.find(a => a.id === state.gridApp.userAssetId);
        }
    }

    protected willUpdate(changedProps: Map<string, any>) {
        if(changedProps.has('dongleCode')) {
            if(this.dongleCode?.length > 0) {
                this.pages[1].getActionDisabled = () => false;
            } else {
                this.pages[1].getActionDisabled = () => true;
            }
            this.requestUpdate('pages');
        }
        if(changedProps.has('currentPageIndex')) {
            if(this.currentPageIndex === 2 && this.dongleCode) {
                this.connectDevice(this.dongleCode);
            }
        }
    }

    protected onDongleCodeChange(ev: CustomEvent) {
        this.dongleCode = ev.detail.value;
    }

    protected formatCode(dongleCode: string): string | undefined {
        const splitted = dongleCode.split(' ');
        return splitted.find(s => {
            if(s.length === 32) {
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


    // Main 'connect device' process method.
    // It links the device through the HTTP API, and queries the asset to be cached locally.
    // Several catches are in place to show an error message on screen.
    // When failing, it automatically navigates back to the previous page, aka the input page.
    protected async connectDevice(dongleCode: string, validate = true) {

        // Validate code if necessary
        if (dongleCode && validate) {
            dongleCode = this.formatCode(dongleCode);
        }

        // Return error if invalid
        if (!dongleCode) {
            console.error('The input code has an invalid format.');
            await new Promise(resolve => setTimeout(resolve, 500));
            this.setError(i18next.t('error.setupCodeInvalid'));
            this.switchPage('previous');
            return;
        }

        console.debug(`Connecting to ${dongleCode}`);
        try {
            await manager.rest.api.DeviceResource.linkDevice({
                deviceName: dongleCode
            });
        } catch (e) {
            console.error(e);
            await new Promise(resolve => setTimeout(resolve, 1000));
            if(isAxiosError(e)) {
                if(e.response.status === 404) {
                    this.setError(i18next.t('error.setupDeviceNotFound'));
                } else if(e.response.status === 403) {
                    this.setError(i18next.t('error.setupDeviceAlreadyLinked'));
                } else {
                    this.setError(i18next.t('error.unknown'));
                }
            } else {
                this.setError(i18next.t('error.unknown'));
            }
            console.debug('Navigating back because of linkDevice fail');
            this.switchPage('previous');
            return;
        }

        // Add a delay
        await new Promise(resolve => setTimeout(resolve, 1000));

        // Fetch and cache the asset
        // Error handling is done differently, since we don't expect it to fail.
        // If so, since the linking process already took place, and it 'blocks linking the same device', we recommend a browser refresh
        try {
            const asset = (await manager.rest.api.AssetResource.queryAssets({
                realm: { name: manager.displayRealm },
                types: ['ReschoolMeterAsset']
            })).data[0];
            if(asset) {
                this._store.dispatch(setUserAsset(asset));
                console.debug('Connected! Continuing to next page!');
                this.switchPage('next');

            } else {
                this.setError(i18next.t('error.setupDeviceServerError'));
                this.switchPage('previous');
                return;
            }

        } catch (e) {
            this.setError(i18next.t('error.setupDeviceServerError'));
            this.switchPage('previous');
            return;
        }

        // Upload characteristics using the fetched asset
        if(this.characteristics) {
            console.debug(`Uploading household characteristics...`);
            await manager.rest.api.DeviceCharacteristicsResource.setCharacteristics({ characteristics: this.characteristics }).catch((e) => {
                console.warn(e);
            });
        }
    }

    protected setError(errorMsg?: string) {
        this.errorText = errorMsg;
    }
}
