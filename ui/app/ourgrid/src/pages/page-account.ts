import {GridAppStateKeyed} from '../util/og-state';
import {TemplateResult, html, css} from 'lit';
import {customElement, state} from 'lit/decorators.js';
import {router} from '@openremote/or-app';
import {Store} from '@reduxjs/toolkit';
import '../panels/panel-account-info';
import '../panels/panel-account-password';
import '../panels/panel-account-notifications';
import {i18next} from '@openremote/or-translate';
import {OgPage, OgPageProvider, PageAnimationType} from './util/og-page';
import {OgListItem} from '../components/og-mwc-list';
import {OrMwcListChangedEvent} from '@openremote/or-mwc-components/or-mwc-list';
import {OgDialog, OgDialogAction, showDialog} from '../components/og-dialog';
import {User} from '@openremote/model';
import {showSnackbar} from '../components/og-snackbar';
import manager from '@openremote/core';
import rest from "rest";

export function pageAccountProvider(store: Store<GridAppStateKeyed>): OgPageProvider<GridAppStateKeyed> {
    return {
        name: 'account',
        routes: ['account'],
        pageCreator: () => new PageAccount(store),
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

@customElement('page-account')
export class PageAccount extends OgPage<GridAppStateKeyed> {

    getAnimationEnterType = () => PageAnimationType.SWIPE_RIGHT;
    getAnimationExitType = () => PageAnimationType.SWIPE_LEFT;

    @state()
    protected user: User;

    get name(): string {
        return 'account';
    }

    stateChanged(state: GridAppStateKeyed): void {
        this.user = state.gridApp.user;
    }

    static get styles() {
        return [...super.styles, styling];
    }

    protected _onMenuSelect(ev: OrMwcListChangedEvent) {
        switch (ev.detail[0].value) {
            case 'password': {
                router.navigate('account-password');
                return;
            }
            case 'switch-city': {
                showDialog(new OgDialog()
                    .setHeading('areYouSure')
                    .setContent(html`<or-translate value="switchCityConfirm"></or-translate>`)
                    .setActions([
                        {actionName: 'cancel', content: 'cancel'},
                        {actionName: 'ok', content: 'switch', action: () => {
                            window.location.replace("https://ourgrid.openremote.app/cityselector/?redirect=false");
                        }}
                    ]) as any
                )
                return;
            }
            case 'logout': {
                manager.logout();
                return;
            }
            case 'privacy': {
                router.navigate('privacy');
                return;
            }
            case 'delete-account': {
                const action = () => {
                    this.deleteAccount(this.user?.id);
                };
                const dialogActions: OgDialogAction[] = [
                    {actionName: 'cancel', content: 'cancel'},
                    {actionName: 'delete', content: 'delete', action: action}
                ];
                showDialog(new OgDialog()
                    .setHeading(i18next.t('page-account.deleteAccount'))
                    .setContent(html`
                        <or-translate value="page-account.deleteAccountConfirm" style="white-space: pre-line; margin-top: -1.5rem;"></or-translate>
                    `)
                    .setDismissAction(null)
                    .setActions(dialogActions) as OgDialog
                );
            }
        }
    }

    protected render(): TemplateResult {
        const items: OgListItem[] = [
            /*{icon: 'key', text: i18next.t('page-account.changePassword'), value: 'password', trailingIcon: 'chevron-right'},*/
            {icon: 'city', text: i18next.t('page-account.switchCity'), value: 'switch-city'},
            {icon: 'delete', text: i18next.t('page-account.deleteAccount'), value: 'delete-account'}
            /*{icon: 'bell-badge', text: 'Notifications', value: 'notifications', trailingIcon: 'chevron-right'}*/
        ];
        const items2: OgListItem[] = [
            {icon: 'book', text: i18next.t('privacyStatement'), value: 'privacy'},
            {icon: 'logout', text: i18next.t('logout'), value: 'logout'}
        ];
        return html`
            <div class="page-wrapper">
                <div style="display: flex; flex-direction: column; align-items: center;">
                    <div style="width: calc(100% - 32px); padding: 16px; display: flex; flex-direction: column; align-items: center;">
                        <img src="images/dots-onboarding.svg" style="width: 100%;"/>
                        <span class="text-heading" style="margin-top: -10%; text-align: center; max-width: 65vw;">${i18next.t('page-account.heading')}</span>
                    </div>
                    <div style="width: 100%;">
                        <div>
                            <!-- Account information -->
                            <panel-account-info .user="${this.user}" style="margin-bottom: 36px;"></panel-account-info>
                            <!-- Divider -->
                            <div style="border-bottom: 1px solid #E0E0E0; margin: 0 32px;"></div>
                            <!-- Actions -->
                            <div style="padding: 16px;">
                                <og-mwc-list .listItems="${items2}" @or-mwc-list-changed="${(ev: OrMwcListChangedEvent) => this._onMenuSelect(ev)}"></og-mwc-list>
                            </div>                            <!-- Divider -->
                            <div style="border-bottom: 1px solid #E0E0E0; margin: 0 32px;"></div>
                            <!-- Actions -->
                            <div style="padding: 16px;">
                                <og-mwc-list .listItems="${items}" @or-mwc-list-changed="${(ev: OrMwcListChangedEvent) => this._onMenuSelect(ev)}"></og-mwc-list>
                            </div>
                            <div class="text-tertiary" style="display: flex; align-items:end; justify-content: center; gap: 8px;">
                                <span>
                                    <or-translate value="appName"></or-translate>
                                    v1.2.0
                                </span>
                            <div>
                            <div style="min-height: 100px;"></div>
                        </div>
                    </div>
                </div>
            </div>
        `;
    }

    protected deleteAccount(userId?: string) {
        const realmName = manager.displayRealm;
        if (!userId || !realmName) {
            showSnackbar(undefined, 'error.unknown');
        } else {
            rest.api.UserAccountResource.deleteAccount().then(r => {
                if (r.status === 200) {
                    manager.logout();
                }
            }).catch(e => {
                console.error(e);
                showSnackbar(undefined, 'error.unknown');
            });
        }
    }
}
