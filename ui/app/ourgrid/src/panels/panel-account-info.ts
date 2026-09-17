/*
 * Copyright 2026, OpenRemote Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
import { type TemplateResult, html, css, type PropertyValues } from "lit";
import { customElement, state } from "lit/decorators.js";
import { InputType } from "@openremote/or-mwc-components/or-mwc-input";
import { OgDataPanel } from "../components/og-data-panel";
import { i18next } from "@openremote/or-translate";
import { manager } from "@openremote/core";
import type { Realm } from "model";

const styling = css`
  iframe {
    min-height: 550px;
    width: 100%;
    border: none;
  }
`;

@customElement("panel-account-info")
export class PanelAccountInfo extends OgDataPanel {
  @state()
  protected _realms: Realm[] = [];

  static get styles() {
    return [...super.styles, styling];
  }

  protected firstUpdated(changedProps: PropertyValues) {
    manager.rest.api.RealmResource.getAccessible().then((response) => {
      this._realms = response.data;
    });
    return super.firstUpdated(changedProps);
  }

  protected async getPanelContent(): Promise<TemplateResult> {
    const realm = this._realms?.find((r) => r.name === this.user.realm)?.displayName || this.user.realm;
    return html`
      <div style="display: flex; flex-direction: column; gap: 12px;">
        <div style="width: 100%;">
          <og-input
            .type="${InputType.TEXT}"
            label="${i18next.t("page-account.username")}"
            readonly
            .value="${this.user.username}"
            style="width: 100%;"
          />
        </div>
        <div style="width: 100%;">
          <og-input
            .type="${InputType.TEXT}"
            label="${i18next.t("page-account.email")}"
            readonly
            .value="${this.user.email}"
            style="width: 100%;"
          />
        </div>
        <div style="width: 100%;">
          <og-input
            .type="${InputType.TEXT}"
            label="${i18next.t("page-account.city")}"
            readonly
            .value="${realm}"
            style="width: 100%;"
          />
        </div>
      </div>
    `;
  }
}
