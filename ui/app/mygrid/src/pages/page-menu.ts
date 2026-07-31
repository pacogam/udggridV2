import {TemplateResult, html, css} from 'lit';
import {customElement, state} from 'lit/decorators.js';
import {i18next} from '@openremote/or-translate';
import manager from '@openremote/core';
import {Store} from '@reduxjs/toolkit';
import {OgListItem} from '../components/og-mwc-list';
import '../components/og-mwc-list';
import {OrMwcListChangedEvent} from '@openremote/or-mwc-components/or-mwc-list';
import {AppStateKeyed} from '@openremote/or-app';
import {GridAppStateKeyed} from '../util/og-state';
import {guard} from 'lit/directives/guard.js';
import {OgPage, OgPageProvider} from './util/og-page';

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
        transition: opacity ease-in-out 0.1s, height 0s 0.1s, width 0s 0.1s !important; /*closing transition*/
        -webkit-transition: opacity ease-in-out 0.1s, height 0s 0.1s, width 0s 0.1s !important;
    }

    .menu-content-wrapper-opened {
        width: 100%;
        height: 100vh;
        opacity: 1;
        transition: opacity ease-in-out 0.2s 0.2s; /*open transition*/
        -webkit-transition: opacity ease-in-out 0.2s 0.2s;
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
        gap: 24px;
    }

    #top-graphic-container {
        padding: 0 12px;
    }

    #top-graphic {
        width: 100%;
        /*-webkit-transform: scaleX(-1);
        transform: scaleX(-1);*/
    }

    #bottom-graphic-container {
        padding: 0 12px;
        display: flex;
        justify-content: center;
    }

    #bottom-graphic {
        width: 100%;
        /*-webkit-transform: scaleY(-1);
        transform: scaleY(-1);*/
    }

    .menu-background-wrapper {
        position: relative;
        overflow: hidden;
        max-width: 100%;
        height: 0;
        transition: all ease-in-out 0.2s;
        -webkit-transition: all ease-in-out 0.2s;
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
        transition: all ease-in-out 0.2s;
        -webkit-transition: all ease-in-out 0.2s;
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
        this.currentPage = state.app.page;
        this.language = state.gridApp.language;
        return super.stateChanged(state);
    }

    protected willUpdate(changedProps: Map<string, any>) {
        super.willUpdate(changedProps);
        if (changedProps.has('currentPage')) {
            this.toggle(false);
        }
    }

    /* -------------------- */

    public toggle(state?: boolean) {
        this.opened = (state ?? !this.opened);
    }

    protected _onMenuSelect(ev: OrMwcListChangedEvent) {
        switch (ev.detail[0].value) {
            case 'home': {
                this.dispatchEvent(new CustomEvent('navigate', {detail: 'home'}));
                return;
            }
            case 'account': {
                this.dispatchEvent(new CustomEvent('navigate', {detail: 'account'}));
                return;
            }
            case 'devices': {
                this.dispatchEvent(new CustomEvent('navigate', {detail: 'devices'}));
                return;
            }
            case 'language': {
                this.dispatchEvent(new CustomEvent('language'));
                return;
            }
            /*case 'characteristics': {
                this.dispatchEvent(new CustomEvent('navigate', {detail: 'characteristics'}));
                return;
            }*/
            case 'intro': {
                this.dispatchEvent(new CustomEvent('navigate', {detail: 'onboarding'}));
                /*window.localStorage.setItem('completedOnboarding', '0');
                router.navigate('');
                window.location.reload();*/
                return;
            }
            default: {
                return;
            }
        }
    }

    protected render(): TemplateResult {
        const items: OgListItem[] = [
            {icon: 'home', text: i18next.t('home'), value: 'home'},
            {icon: 'home-battery-outline', text: i18next.t('devices'), value: 'devices'},
            {icon: 'account', text: i18next.t('account'), value: 'account'},
            {icon: 'web', text: i18next.t('language'), value: 'language'},
            /*{icon: 'meter-gas', text: i18next.t('houseCharacteristics'), value: 'characteristics'},*/
            {icon: 'help-circle-outline', text: i18next.t('intro'), value: 'intro'},
        ];
        return html`
            <div id="menu-wrapper">
                <div class="menu-content-wrapper ${this.opened ? 'menu-content-wrapper-opened' : 'menu-content-wrapper-closed'}">
                    ${guard([this.currentPage, this.language], () => html`
                        <div class="menu-content">
                            
                            <!-- Top container -->
                            <div class="menu-container" style="background: var(--og-color-primary-dark)">
                                <div class="menu-container" style="gap: 36px;">
                                    <div style="flex: 1; display: flex; flex-direction: column; gap: 12px; margin-top:24px;">
                                        <og-mwc-list dark .values="${this.currentPage}" .listItems="${items}" @or-mwc-list-changed="${(ev: OrMwcListChangedEvent) => this._onMenuSelect(ev)}"></og-mwc-list>
                                    </div>
                                </div>
                                <div id="bottom-graphic-container">
                                    <img id="bottom-graphic" src="images/dots-menu-white-bottom.svg"/>
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
}
