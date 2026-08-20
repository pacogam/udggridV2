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
import { type TemplateResult, html, css } from "lit";
import { customElement } from "lit/decorators.js";
import { OgDataPanel } from "../components/og-data-panel";
import { until } from "lit/directives/until.js";
import { InputType } from "@openremote/or-mwc-components/or-mwc-input";
import { Asset } from "model";

const styling = css`
  #panel-wrapper {
    padding: 16px 24px 16px 16px;
  }

  #homeautomation-container {
    position: relative;
  }

  #homeautomation-content {
    display: flex;
    align-items: center;
    gap: 24px;
  }

  #homeautomation-icon {
    font-size: var(--og-font-size-statistic-large);
    color: var(--og-color-success);
  }
`;

@customElement("panel-homeautomation-assetinfo")
export class PanelHomeAutomationAssetInfo extends OgDataPanel {
  public transparent = false;
  public rounded = true;

  static get styles() {
    return [...super.styles, styling];
  }

  protected async getPanelContent(): Promise<TemplateResult> {
    return html`
      <div id="homeautomation-container">
        <div id="homeautomation-content">
          <or-icon id="homeautomation-icon" icon="home-automation"></or-icon>
          <div style="display: flex; align-items: center; gap: 24px; flex: 1;">
            <div style="display: flex; flex-direction: column; justify-content: space-between; gap: 8px; width: 100%;">
              <span class="text-heading2" style="text-align: start;"> Device information </span>
              <div style="display: flex; flex-direction: column; gap: 16px; margin-top: 16px;">
                ${until(this._getDetailsContentTemplate())}
              </div>
            </div>
          </div>
        </div>
      </div>
    `;
  }

  protected async _getDetailsContentTemplate(): Promise<TemplateResult> {
    return html`
      <og-input
        type=${InputType.TEXT}
        value=${this.meterAsset?.id || "???"}
        readonly
        outlined
        label="Energy meter Asset ID"
      ></og-input>
      <og-input
        type=${InputType.TEXT}
        value=${this.districtAsset?.id || "???"}
        readonly
        outlined
        label="District Asset ID"
      ></og-input>
      <og-input
        type=${InputType.TEXT}
        value=${this.challengeAsset?.id || "???"}
        readonly
        outlined
        label="Challenges Asset ID"
      ></og-input>
      <og-input
        type=${InputType.TEXT}
        value=${this.peakPointsAsset?.id || "???"}
        readonly
        outlined
        label="Peak points Asset ID"
      ></og-input>
      <og-input
        type=${InputType.TEXT}
        value=${this.batteryAsset?.id || "???"}
        readonly
        outlined
        label="Battery Asset ID"
      ></og-input>
    `;
  }
}
