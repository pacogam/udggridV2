import {TemplateResult, html, css, PropertyValues} from 'lit';
import {customElement, state} from 'lit/decorators.js';
import { InputType } from '@openremote/or-mwc-components/or-mwc-input';
import {OgDataPanel} from '../components/og-data-panel';
import {i18next} from '@openremote/or-translate';
import {manager} from "@openremote/core";
import {Realm} from "model";

const styling = css`
  iframe {
    min-height: 550px;
    width: 100%;
    border: none;
  }
`;

@customElement('panel-account-info')
export class PanelAccountInfo extends OgDataPanel {

    @state()
    protected _realms: Realm[] = [];

    static get styles() {
        return [...super.styles, styling];
    }

    protected firstUpdated(changedProps: PropertyValues) {
        manager.rest.api.RealmResource.getAccessible().then(response => {
            this._realms = response.data;
        })
        return super.firstUpdated(changedProps);
    }

    protected async getPanelContent(): Promise<TemplateResult> {
        const realm = this._realms?.find(r => r.name === this.user.realm)?.displayName || this.user.realm;
        return html`
            <div style="display: flex; flex-direction: column; gap: 12px;">
                <div style="width: 100%;">
                    <og-input .type="${InputType.TEXT}" label="${i18next.t('page-account.username')}" readonly .value="${this.user.username}" style="width: 100%;" />
                </div>
                <div style="width: 100%;">
                    <og-input .type="${InputType.TEXT}" label="${i18next.t('page-account.email')}" readonly .value="${this.user.email}" style="width: 100%;" />
                </div>
                <div style="width: 100%;">
                    <og-input .type="${InputType.TEXT}" label="${i18next.t('page-account.city')}" readonly .value="${realm}" style="width: 100%;" />
                </div>
            </div>
        `;
    }
}
