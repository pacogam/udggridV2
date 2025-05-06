import { customElement } from 'lit/decorators.js';
import {OgOnboardingPage, OnboardPage} from '../util/og-onboarding-page';
import {html, TemplateResult } from 'lit';
import {GridAppStateKeyed} from '../../util/og-state';
import {OgPageProvider} from '../util/og-page';
import {AppStateKeyed, router} from '@openremote/or-app';
import {Store} from '@reduxjs/toolkit';

export function pageConfirmPrivacyProvider(store: Store<GridAppStateKeyed>): OgPageProvider<AppStateKeyed> {
    return {
        name: 'confirm-privacy',
        routes: [
            'confirm-privacy'
        ],
        hideHeader: true,
        pageCreator: () => new ConfirmPrivacyPage(store),
        skipDataCheck: true
    };
}


@customElement('confirm-privacy-page')
export class ConfirmPrivacyPage extends OgOnboardingPage {

    get name(): string {
        return 'Privacy Statement confirmation';
    }

    protected pages: OnboardPage[] = [
        {
            getHeading: () => 'page-privacy.heading',
            getActionText: () => 'page-privacy.accept',
            noBottomGraphic: true,
            noPadding: true,
            pageContent: (): TemplateResult => {
                return html`
                    <div style="height: 100%;">
                        <span class="text-secondary">
                            <or-translate value="privacy-statement" style="white-space: pre-line; margin-bottom: 80px; text-align: left;"></or-translate>
                        </span>
                    </div>
                `;
            }
        }
    ];

    protected onActionClick(_index: number) {
        window.localStorage.setItem('acceptedPrivacy', '1');
        router.navigate('home');
    }
}
