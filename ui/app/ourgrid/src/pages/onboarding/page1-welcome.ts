import {OgPage, OgPageProvider} from '../util/og-page';
import {GridAppStateKeyed} from '../../util/og-state';
import {customElement} from 'lit/decorators.js';
import {TemplateResult, html, css} from 'lit';
import {AppStateKeyed} from '@openremote/or-app';
import {Store} from '@reduxjs/toolkit';
import {i18next} from '@openremote/or-translate';

export function onboardingOneProvider(store: Store<GridAppStateKeyed>): OgPageProvider<AppStateKeyed> {
    return {
        name: 'onboarding-1',
        routes: [
            'onboarding-1'
        ],
        hideHeader: true,
        pageCreator: () => new Page1Welcome(store),
        skipDataCheck: true
    };
}

const styling = css`
    #welcome-container {
      margin-top: 32px;
      display: flex;
      flex-direction: column;
      text-align: center;
      gap: 32px;
      pointer-events: none;
      padding: 16px 32px;
    }
`;

@customElement('page1-welcome')
export class Page1Welcome extends OgPage<GridAppStateKeyed> {

    static get styles() {
        return [...super.styles, styling];
    }

    get name(): string {
        return 'Onboarding 1/3';
    }

    protected render(): TemplateResult {
        return html`
            <div id="welcome-container">
                <span class="text-primary">${i18next.t('onboarding.introText1')}</span>
                <span class="text-primary">${i18next.t('onboarding.introText2')}</span>
            </div>
        `;
    }
}
