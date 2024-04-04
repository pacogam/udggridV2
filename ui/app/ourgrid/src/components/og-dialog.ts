import {DialogAction, OrMwcDialog, OrMwcDialogClosedEvent, OrMwcDialogOpenedEvent} from '@openremote/or-mwc-components/or-mwc-dialog';
import {css, html, TemplateResult, unsafeCSS} from 'lit';
import {customElement, property} from 'lit/decorators.js';
import {getAppStyle} from '../styles';
import manager, {DefaultColor4, Languages} from '@openremote/core';
import {Asset, Attribute, Challenge} from '@openremote/model';
import moment from 'moment';
import {until} from 'lit/directives/until.js';
import {InputType, OrMwcInput} from '@openremote/or-mwc-components/or-mwc-input';
import { styleMap } from 'lit/directives/style-map.js';
import { i18next } from '@openremote/or-translate';
import {Constants} from '../util/constants';
import {Defaults} from "../util/defaults";

const styling = css`
  :host {
    font-family: var(--og-font-family);
  }
  .mdc-dialog {
    z-index: 100;
  }

  .mdc-list {
    margin-bottom: 24px;
  }

  .mdc-dialog__container {
    width: calc(100vw - 48px);
    padding: 72px 0;
  }

  .mdc-dialog__surface {
    flex: 1;
    border-radius: var(--og-panel-border-radius) !important;
  }

  .mdc-dialog__title {
    padding: 24px 14px;
    font-size: var(--og-font-size-heading1);
    font-weight: var(--og-font-weight-heading1);
  }
`;

export interface OgDialogAction extends DialogAction {
}
export interface OgDialogCenterAction extends OgDialogAction {
    bgColorCSS?: string;
    textColorCSS?: string;
}

export function showDialog<T extends OgDialog>(dialog: T, hostElement?: HTMLElement): T {
    if (!hostElement) {
        hostElement = OrMwcDialog.DialogHostElement || document.body;
    }

    dialog.setOpen(true);
    dialog.addEventListener(OrMwcDialogOpenedEvent.NAME, ev => {
        ev.stopPropagation();
    });
    dialog.addEventListener(OrMwcDialogClosedEvent.NAME, ev => {
        ev.stopPropagation();
        window.setTimeout(() => {
            if (dialog.parentElement) {
                dialog.parentElement.removeChild(dialog);
            }
        }, 0);
    });
    hostElement.append(dialog);
    return dialog;
}

export function showLanguageDialog(languages: Languages, hostElement?: HTMLElement, show = true): OgDialog {
    const dialog = new OgDialog()
        .setHeading('language')
        .setDismissAction(null)
        .setStyles(html`
            <style>.selected {
                color: ${unsafeCSS(DefaultColor4)}
            }</style>`)
        .setActions(Object.entries(languages).map(([key, value]) => {
            return {
                content: html`
                    <div style="display: flex; align-items: center; gap: 12px;">
                        <img src="images/flag_${key}.svg" width="24px" height="18px"/>
                        <span class="${(key === manager.language) ? 'selected' : ''}" style="line-height: 100%;">
                                <or-translate style="line-height: 100%;" value="${value}"></or-translate>
                            </span>
                    </div>
                `,
                actionName: key,
                action: () => {
                    console.log(`Setting app language to '${key}'`);
                    manager.language = key;
                }
            };
        })) as OgDialog;
    return show ? showDialog(dialog, hostElement) : dialog;
}

export function showLastChallengeResultDialog(meterAsset: Asset, challengeAsset: Asset, hostElement?: HTMLElement, show = true): OgDialog {
    const challengeDuration: number = challengeAsset?.attributes?.[Constants.CHALLENGE_DURATION_ATTRIBUTE]?.value || Defaults.CHALLENGE_DURATION_MINUTES;
    const challengeWait: number = challengeAsset?.attributes?.[Constants.CHALLENGE_WAIT_ATTRIBUTE]?.value || Defaults.CHALLENGE_WAIT_MINUTES;
    const challengeInterval: number = challengeAsset?.attributes?.[Constants.CHALLENGE_POINT_INTERVAL_ATTRIBUTE]?.value || Defaults.CHALLENGE_INTERVAL_MINUTRES;
    const challengeMaxPointsAchievable = Math.round(challengeDuration / challengeInterval);
    const challengePointAttr: Attribute<any> | undefined = meterAsset?.attributes?.[Constants.CHALLENGE_POINT_CURRENT_ATTRIBUTE];
    const getDetailsHtml = async (): Promise<TemplateResult> => {
        if (!challengePointAttr) {
            return html`Error`;
        }
        const time = moment(challengePointAttr.timestamp).subtract(challengeDuration + challengeWait + 1, 'minutes');
        const data = (await manager.rest.api.DeviceChallengesResource.getHistory({
            startTimestamp: time.valueOf(),
            endTimestamp: new Date().getTime()
        })).data as Challenge[];
        const challenge = data[0];
        return html`
            <div class="text-primary bold" style="display: flex; flex-direction: column; gap: 6px; align-items: center;">
                <span style="color: var(--og-color-neutral)">${i18next.t('panel_challengeComplete.pointsEarned').replace('{{value}}', challenge.points || 0)}</span>
                <span style="color: var(--og-color-secondary)">
                    ${i18next.t('panel_challengeComplete.minutesJoined').replace('{{value}}', moment(challenge.endDate).diff(challenge.joinedAt, 'minutes').toString())}
                </span>
            </div>
        `;
    };
    const action = {
        content: 'panel_challengeComplete.action'
    } as OgDialogAction;
    const dialog = (new OgDialog()
        .setHeading('panel_challengeComplete.heading')
        .setDismissAction(null)
        .setContent(() => html`
            <div style="display: flex; flex-direction: column; gap: 24px;">
                <div class="text-primary dark">
                    <or-translate value="${'panel_challengeComplete.subtitle'}"></or-translate>
                </div>
                <div style="display: flex; flex-direction: column; align-items: center; gap: 18px;">
                    <div style="width: 100%;">
                        <og-challenge-progress dark noLabel noPadding .meterAsset=${meterAsset} .challengeAsset=${challengeAsset} style="color: var(--og-color-primary)" />
                    </div>
                    <div style="width: 80%">
                        <og-points .label="${false}" lines="2" centered .progress="${challengePointAttr.value || 0}" .max=${challengeMaxPointsAchievable}></og-points>
                    </div>
                    <div style="width: 100%; display: flex; justify-content: center;">
                        ${until(getDetailsHtml(), () => html`
                            <og-loading></og-loading>
                        `)}
                    </div>
                </div>
            </div>
        `) as OgDialog)
        .setDark(true)
        .setAlign('start')
        .setActionBtn(action);

    return show ? showDialog(dialog, hostElement) : dialog;
}

@customElement('og-dialog')
export class OgDialog extends OrMwcDialog {

    @property()
    protected dark: boolean;

    @property()
    protected align: 'start' | 'center' | 'end' = 'center';

    @property()
    protected actionBtn: OgDialogCenterAction;

    public setDark(state: boolean): this {
        this.dark = state;
        return this;
    }

    public setAlign(align: 'start' | 'center' | 'end'): this {
        this.align = align;
        return this;
    }

    public setActionBtn(action: OgDialogCenterAction): this {
        this.actionBtn = action;
        return this;
    }

    static get styles() {
        return [...super.styles, getAppStyle(), styling];
    }

    protected render() {
        const textStyling: {} = { 'color': this.dark ? 'var(--og-color-primary)' : 'var(--og-color-primary-dark)' };
        const bgStyling: {} = { 'background-color': this.dark ? 'var(--og-color-primary-dark)' : 'var(--og-color-primary)'};

        return html`
            ${typeof (this.styles) === 'string' ? html`
                <style>${this.styles}</style>` : this.styles || ``}

            <div id="dialog"
                 class="mdc-dialog"
                 role="alertdialog"
                 aria-modal="true"
                 aria-labelledby="dialog-title"
                 aria-describedby="dialog-content"
                 @MDCDialog:opened="${() => this._onDialogOpened()}"
                 @MDCDialog:closed="${(evt: any) => this._onDialogClosed(evt.detail.action)}">
                <div class="mdc-dialog__container" style="align-items: ${this.align}">
                    <div class="mdc-dialog__surface" tabindex="0" style="${styleMap(bgStyling)}">
                        ${typeof (this.heading) === 'string' ? html`
                                    <h2 class="mdc-dialog__title" id="dialog-title" style="${styleMap(textStyling)}">
                                        <or-translate value="${this.heading}"></or-translate>
                                    </h2>`
                                : this.heading ? html`<span class="mdc-dialog__title" id="dialog-title">${this.heading}</span>` : ``}
                        ${this.content ? html`
                            <div class="dialog-container mdc-dialog__content" id="dialog-content">
                                ${typeof this.content === 'function' ? this.content() : this.content}
                            </div>
                            <footer class="mdc-dialog__actions">
                                ${this.actions ? this.actions.map(action => {
                                    return html`
                                        <div class="mdc-button mdc-dialog__button" ?data-mdc-dialog-button-default="${action.default}"
                                             data-mdc-dialog-action="${action.disabled ? undefined : action.actionName}">
                                            ${typeof (action.content) === 'string' ? html`
                                                <or-mwc-input .type="${InputType.BUTTON}" @or-mwc-input-changed="${(ev: Event) => {
                                                    if ((ev.currentTarget as OrMwcInput).disabled) ev.stopPropagation();
                                                }}" .disabled="${action.disabled}" .label="${action.content}"></or-mwc-input>` : action.content}
                                        </div>`;
                                }) : ``}
                            </footer>
                            <!-- New action button -->
                            ${this.actionBtn ? html`
                                <div style="position: absolute; bottom: 24px; width: 100%; display: flex; justify-content: center; z-index: 10;">
                                    <div style="position: fixed; width: 70vw;">
                                        <og-input .type="${InputType.BUTTON}" raised rounded fullWidth
                                                  style="width: 100%; --or-mwc-input-color: ${this.actionBtn.bgColorCSS || 'var(--og-color-danger)'}; --or-mwc-input-text-color: ${this.actionBtn.textColorCSS || 'var(--og-color-primary)'};"
                                                  .disabled="${this.actionBtn.disabled}" .label="${this.actionBtn.content}"
                                                  @or-mwc-input-changed="${evt => {
                                                      this.close(evt);
                                                  }}"
                                        ></og-input>
                                    </div>
                                </div>
                            ` : ``}
                        ` : html`
                            <ul class="mdc-list ${this.avatar ? 'mdc-list--avatar-list' : ''}">
                                ${!this.actions ? `` : this.actions!.map((action, _index) => {
                                    return html`
                                        <li class="mdc-list-item" data-mdc-dialog-action="${action.actionName}"><span class="mdc-list-item__text">${action.content}</span></li>`;
                                })}
                            </ul>
                        `}
                    </div>
                </div>
                <div class="mdc-dialog__scrim"></div>
            </div>
        `;
    }
}
