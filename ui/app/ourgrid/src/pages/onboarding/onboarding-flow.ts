import {customElement} from 'lit/decorators.js';
import {TemplateResult, html, css} from 'lit';
import {OgOnboardingPage, OnboardPage} from '../util/og-onboarding-page';
import {GridAppStateKeyed} from '../../util/og-state';
import {OgPageProvider} from '../util/og-page';
import {AppStateKeyed, router} from '@openremote/or-app';
import {Store} from '@reduxjs/toolkit';
import {onboardingOneProvider} from './page1-welcome';
import {onboardingTwoProvider} from './page2-peakusage';
import {onboardingThreeProvider} from './page3-currentusage';

export function pageOnboardingProvider(store: Store<GridAppStateKeyed>): OgPageProvider<AppStateKeyed> {
    return {
        name: 'onboarding',
        routes: [
            'onboarding'
        ],
        hideHeader: true,
        pageCreator: () => new OnboardingFlow(store),
        skipDataCheck: true
    };
}

const styling = css`
  #onboarding-container {
    padding: 0;
    max-height: 100vh;
  }
  #onboarding-title {
    padding: 16px;
    width: calc(100% - 32px);
  }
  #onboarding-topgraphic {
    bottom: 16px;
  }
  #onboarding-footer {
    padding: 16px;
    width: calc(100% - 32px);
  }
  #onboarding-bottomgraphic {
    top: 16px;
  }
  #onboarding-content {
    padding: 0 !important;
    width: 100% !important;
  }
`;

@customElement('onboarding-flow')
export class OnboardingFlow extends OgOnboardingPage {

    protected dots = false;

    protected padding = true;

    protected gesture = false;

    static get styles() {
        return [...super.styles, styling];
    }

    get name(): string {
        return 'Onboarding';
    }

    protected pages: OnboardPage[] = [
        {
            getHeading: () => 'appName',
            getActionText: () => 'continue',
            pageContent: (): TemplateResult => html`
                ${onboardingOneProvider(this._store).pageCreator()}
            `
        }, {
            getHeading: () => 'onboarding.page2-heading',
            noBottomGraphic: true,
            noPadding: true,
            pageContent: (): TemplateResult => {
                const page = onboardingTwoProvider(this._store).pageCreator();
                page.updateComplete.then(() => {
                    page.addEventListener('finish', () => this.switchPage('next'));
                });
                return html`${page}`;
            }
        }, {
            getActionText: () => 'continue',
            noTopGraphic: true,
            noBottomGraphic: true,
            pageContent: (): TemplateResult => html`
                ${onboardingThreeProvider(this._store).pageCreator()}
            `
        }

    ];

    protected onActionClick(index: number){
        if(index < 2) {
            this.switchPage('next');
        } else {
            window.localStorage.setItem('completedOnboarding', '1');
            router.navigate('home');
        }
    }
}
