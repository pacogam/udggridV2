import {GridAppStateKeyed} from '../util/og-state';
import {TemplateResult, html, css} from 'lit';
import {customElement, state} from 'lit/decorators.js';
import {AppStateKeyed} from '@openremote/or-app';
import {Store} from '@reduxjs/toolkit';
import '../panels/panel-privacy-statement';
import {OgPage, OgPageProvider} from './util/og-page';

export function pagePrivacyProvider(store: Store<GridAppStateKeyed>): OgPageProvider<AppStateKeyed> {
    return {
        name: 'privacy',
        routes: ['privacy'],
        pageCreator: () => new PagePrivacy(store),
        skipDataCheck: true
    };
}

const styling = css`
  .page-wrapper {
    width: 100%;
    height: 100vh;
    background: var(--og-color-primary);
  }
`;

@customElement('page-privacy')
export class PagePrivacy extends OgPage<GridAppStateKeyed> {

    @state()
    protected language: string;

    get name(): string {
        return 'privacy';
    }

    stateChanged(state: GridAppStateKeyed): void {
        this.language = state.gridApp.language;
    }

    static get styles() {
        return [...super.styles, styling];
    }

    protected render(): TemplateResult {
        return html`
            <div class="page-wrapper">
                <div style="display: flex; flex-direction: column; align-items: center; padding: 16px;">
                    <div style="width: 100%; display: flex; flex-direction: column; align-items: center; margin-bottom: 7.5%;">
                        <img src="images/dots-onboarding.svg" style="width: 100%;"/>
                        <span class="text-heading" style="margin-top: -10%; text-align: center; max-width: 65vw;">
                            <or-translate value="page-privacy.heading"></or-translate>
                        </span>
                    </div>
                    <div style="width: 100%;">
                        <panel-privacy-statement fullWidth="${true}" .language="${this.language}"></panel-privacy-statement>
                    </div>
                </div>
            </div>
        `;
    }
}
