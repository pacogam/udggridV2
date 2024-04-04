import {OgPage, OgPageProvider, PageAnimationType} from './util/og-page';
import {GridAppStateKeyed} from '../util/og-state';
import {customElement} from 'lit/decorators.js';
import {html, css, TemplateResult} from 'lit';
import '../panels/panel-account-password';
import {Store} from '@reduxjs/toolkit';
import {GraphicType} from '../features/og-usage-graphic';
import {i18next} from '@openremote/or-translate';
import {router} from '@openremote/or-app';

export function pageAccountPasswordProvider(store: Store<GridAppStateKeyed>): OgPageProvider<GridAppStateKeyed> {
    return {
        name: 'account-password',
        routes: ['account-password'],
        pageCreator: () => new PageAccountPassword(store),
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
    justify-content: space-between;
  }
`;

@customElement('page-account-password')
export class PageAccountPassword extends OgPage<GridAppStateKeyed> {

    animationEnterType = PageAnimationType.SWIPE_LEFT;
    animationExitType = PageAnimationType.SWIPE_RIGHT;

    get name(): string {
        return 'account-password';
    }


    static get styles() {
        return [...super.styles, styling];
    }

    protected render(): TemplateResult {
        return html`
            <div class="page-wrapper">
                <div style="width: calc(100% - 32px); padding: 16px; display: flex; flex-direction: column; align-items: center; margin-bottom: 7.5%;">
                    <og-usage-graphic id="onboarding-topgraphic" .type="${GraphicType.HEADER}" style="width: 100%;"></og-usage-graphic>
                    <span class="text-heading" style="margin-top: -10%; text-align: center; max-width: 65vw;">${i18next.t('page-account.changePassword')}</span>
                </div>
                <div style="flex: 1;">
                    <panel-account-password fullWidth="${true}" .fullHeight="${true}"></panel-account-password>
                </div>
                <div style="padding: 16px;">
                    <div style="display: flex; justify-content: center;">
                        <a style="text-decoration: underline;" @click="${() => router.navigate('account')}">Back to Account page</a>
                    </div>
                    <og-usage-graphic id="onboarding-topgraphic" .type="${GraphicType.FOOTER}"></og-usage-graphic>
                </div>
            </div>
        `;
    }
}
