import {TemplateResult, html, css} from 'lit';
import {customElement, state} from 'lit/decorators.js';
import {i18next} from '@openremote/or-translate';
import manager from '@openremote/core';
import '@openremote/or-icon';
import {Store} from '@reduxjs/toolkit';
import {OgListItem} from '../components/og-mwc-list';
import '../components/og-mwc-list';
import {OrMwcListChangedEvent} from '@openremote/or-mwc-components/or-mwc-list';
import '../panels/panel-device-info';
import '../panels/panel-challenge-earnings';
import {AppStateKeyed, router} from '@openremote/or-app';
import {GridAppStateKeyed, removeUserAsset} from '../util/og-state';
import {ReschoolMeterAsset} from '../util/util';
import {when} from 'lit/directives/when.js';
import {guard} from 'lit/directives/guard.js';
import {OgPage, OgPageProvider} from './util/og-page';
import {Asset} from '@openremote/model';

export function pageMenuProvider(store: Store<GridAppStateKeyed>): OgPageProvider<AppStateKeyed> {
    return {
        name: 'menu',
        routes: ['menu'],
        pageCreator: () => new PageMenu(store)
    };
}

const styling = css`
  :host {
    z-index: 9;
    position: relative;
  }

  #menu-wrapper {
    position: absolute;
    width: 100%;
    max-width: 100%;
    right: 0;
  }

  .menu-content-wrapper {
    position: absolute;
    right: 0;
    height: 0;
    width: 0;
    opacity: 0;
    z-index: 11;
    overflow: hidden;
  }

  .menu-content-wrapper-closed {
    transition: opacity ease-in-out 0.2s, height 0s 0.2s, width 0s 0.2s !important; /*closing transition*/
    -webkit-transition: opacity ease-in-out 0.2s, height 0s 0.2s, width 0s 0.2s !important;
  }

  .menu-content-wrapper-opened {
    width: 100%;
    height: 100vh;
    opacity: 1;
    transition: opacity ease-in-out 0.3s 0.3s; /*open transition*/
    -webkit-transition: opacity ease-in-out 0.3s 0.3s;
  }

  .menu-content {
    height: 100vh;
    display: flex;
    flex-direction: column;
    overflow: auto;
  }

  .menu-container {
    flex: 1;
    padding: 12px;
    display: flex;
    flex-direction: column;
    justify-content: space-between;
    gap: 12px;
  }

  #top-graphic-container {
    padding: 0 12px;
  }

  #top-graphic {
    width: 70%;
    -webkit-transform: scaleX(-1);
    transform: scaleX(-1);
  }

  #bottom-graphic-container {
    padding: 0 12px;
    display: flex;
    justify-content: end;
  }

  #bottom-graphic {
    width: 70%;
    -webkit-transform: scaleY(-1);
    transform: scaleY(-1);
  }

  .menu-background-wrapper {
    position: relative;
    overflow: hidden;
    max-width: 100%;
    height: 0;
    transition: all ease-in-out 0.5s;
    -webkit-transition: all ease-in-out 0.5s;
  }
  
  .menu-background-wrapper-opened {
    height: 100vh;
  }
  
  .menu-background {
    position: absolute;
    right: 0;
    height: 0;
    width: 0;
    border-radius: 0 0 0 100%;
    background: var(--og-color-primary);
    transition: all ease-in-out 0.5s;
    -webkit-transition: all ease-in-out 0.5s;
    z-index: 10;
  }

  .menu-background-opened {
    height: 150vh;
    width: 150vh;
  }
`;

@customElement('page-menu')
export class PageMenu extends OgPage<GridAppStateKeyed> {

    @state()
    protected opened = false;

    @state()
    protected showDeviceCard = true;

    @state()
    protected userAsset?: ReschoolMeterAsset;

    @state()
    protected challengeAsset?: Asset;

    @state()
    protected currentPage?: string;

    @state()
    protected language?: string;

    static get styles() {
        return [...super.styles, styling];
    }

    get name(): string {
        return 'menu';
    }

    stateChanged(state: GridAppStateKeyed): void {
        this.userAsset = state.gridApp.assets.find(a => a.id === state.gridApp.userAssetId);
        this.challengeAsset = state.gridApp.assets.find(a => a.id === state.gridApp.challengeAssetId);
        this.currentPage = state.app.page;
        this.language = state.gridApp.language;
    }

    protected willUpdate(changedProps: Map<string, any>) {
        super.willUpdate(changedProps);
        if(changedProps.has('currentPage')) {
            this.toggle(false);
            if(this.currentPage === 'setup') {
                this.showDeviceCard = false;
            } else {
                this.showDeviceCard = true;
            }
        }
    }

    /* -------------------- */

    public toggle(state?: boolean) {
        this.opened = (state ?? !this.opened);
    }

    protected _onMenuSelect(ev: OrMwcListChangedEvent) {
        switch (ev.detail[0].value) {
            case 'home': {
                this.dispatchEvent(new CustomEvent('navigate', { detail: 'home' }));
                return;
            }
            case 'account': {
                this.dispatchEvent(new CustomEvent('navigate', { detail: 'account' }));
                return;
            }
            case 'characteristics': {
                this.dispatchEvent(new CustomEvent('navigate', { detail: 'characteristics' }));
                return;
            }
            case 'language': {
                this.dispatchEvent(new CustomEvent('language'));
                return;
            }
            case 'intro': {
                window.localStorage.removeItem('completedOnboarding');
                router.navigate('');
                window.location.reload();
                return;
            }
            case 'privacy': {
                this.dispatchEvent(new CustomEvent('navigate', { detail: 'privacy' }));
                return;
            }
            case 'logout': {
                manager.logout();
                return;
            } default: {
                return;
            }
        }
    }

    protected render(): TemplateResult {
        const items: OgListItem[] = [
            {icon: 'home', text: i18next.t('home'), value: 'home'},
            {icon: 'account', text: i18next.t('account'), value: 'account'},
            {icon: 'meter-gas', text: i18next.t('houseCharacteristics'), value: 'characteristics'},
            {icon: 'web', text: i18next.t('language'), value: 'language'},
            {icon: 'help-circle-outline', text: i18next.t('intro'), value: 'intro' },
            {icon: 'book', text: i18next.t('privacyStatement'), value: 'privacy'},
            {prefixImg: 'images/logout.svg', text: i18next.t('logout'), value: 'logout'}
        ];
        return html`
            <div id="menu-wrapper">
                <div class="menu-content-wrapper ${this.opened ? 'menu-content-wrapper-opened' : 'menu-content-wrapper-closed'}">
                    ${guard([this.userAsset, this.currentPage, this.showDeviceCard, this.challengeAsset, this.language], () => html`
                        <div class="menu-content">
                            <div class="menu-container">
                                <div id="top-graphic-container">
                                    <img id="top-graphic" src="images/dots-heading-suffix.svg"/>
                                </div>
                                <div class="menu-container" style="gap: 36px;">
                                    ${when(this.showDeviceCard, () => html`
                                        <div style="display: flex; flex-direction: column; gap: 12px;">
                                            <div class="menu-asset-card">
                                                <panel-device-info .meterAsset="${this.userAsset}" .language="${this.language}"
                                                                   @remove="${() => this.onDeviceRemove()}"
                                                ></panel-device-info>
                                            </div>
                                            <div class="menu-earnings-card">
                                                <panel-challenge-earnings .meterAsset="${this.userAsset}" .challengeAsset="${this.challengeAsset}"></panel-challenge-earnings>
                                            </div>
                                        </div>
                                    `)}
                                    <div style="flex: 1;">
                                        <div>
                                            <span class="text-heading">${i18next.t('menu')}</span>
                                        </div>
                                        <og-mwc-list .values="${this.currentPage}" .listItems="${items}" @or-mwc-list-changed="${(ev: OrMwcListChangedEvent) => this._onMenuSelect(ev)}"></og-mwc-list>
                                    </div>
                                </div>
                                <div id="bottom-graphic-container">
                                    <img id="bottom-graphic" src="images/dots-heading-suffix.svg"/>
                                </div>
                            </div>
                        </div>
                    `)}
                </div>
                <div class="menu-background-wrapper ${this.opened ? 'menu-background-wrapper-opened' : ''}">
                    <div class="menu-background ${this.opened ? 'menu-background-opened' : ''}"></div>
                </div>
            </div>
        `;
    }


    // Method that is called AFTER the device of the user has been removed.
    // In this class we handle that the Asset is removed from the local store.
    protected onDeviceRemove() {
        console.log('Removing device from local store...');
        this._store.dispatch(removeUserAsset());
    }
}
