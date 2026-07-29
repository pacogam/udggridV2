import {OgPage, OgPageProvider} from '../util/og-page';
import {GridAppStateKeyed} from '../../util/og-state';
import {customElement} from 'lit/decorators.js';
import {TemplateResult, html, css} from 'lit';
import {AppStateKeyed} from '@openremote/or-app';
import {Store} from '@reduxjs/toolkit';
import {i18next} from '@openremote/or-translate';
import '../../panels/panel-usage-overview';

export function onboardingThreeProvider(store: Store<GridAppStateKeyed>): OgPageProvider<AppStateKeyed> {
    return {
        name: 'onboarding-3',
        routes: [
            'onboarding-3'
        ],
        hideHeader: true,
        pageCreator: () => new Page3Currentusage(store),
        skipDataCheck: true
    };
}

const styling = css`
  #usage-container {
    height: 100%;
    display: flex;
    flex-direction: column;
    text-align: center;
    pointer-events: none;
  }

  #usage-text-container {
    flex: 1;
    display: flex;
    flex-direction: column;
    justify-content: center;
    gap: 32px;
    text-align: center;
    padding: 16px 32px;
  }
`;

@customElement('page3-currentusage')
export class Page3Currentusage extends OgPage<GridAppStateKeyed> {

    static get styles() {
        return [...super.styles, styling];
    }

    get name(): string {
        return 'Onboarding 3/3';
    }

    protected render(): TemplateResult {
        return html`
            <div id="usage-container">
                <div id="usage-animation-container">
                    <panel-usage-overview .noPadding="${true}" aspectRatio="1/1" .staticAnimation="${true}"></panel-usage-overview>
                </div>
                <div id="usage-text-container">
                    <span class="text-heading">${i18next.t('onboarding.page3-heading')}</span>
                    <span class="text-primary">${i18next.t('onboarding.page3-text')}</span>
                </div>
            </div>
        `;
    }
}
