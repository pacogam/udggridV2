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
import { css, html, type TemplateResult } from "lit";
import { customElement } from "lit/decorators.js";
import { when } from "lit/directives/when.js";
import { OgDataPanel } from "../components/og-data-panel";
import { AssetModelUtil } from "@openremote/model";
import { Util } from "@openremote/core";
import { i18next } from "@openremote/or-translate";
import type { OurgridMeterAsset } from "../util/util";
import { until } from "lit/directives/until.js";

const styling = css`
  .text-heading {
    line-height: 110%;
    max-width: 80vw;
  }
`;

@customElement("panel-peak-usage")
export class PanelPeakUsage extends OgDataPanel {
  public heading = html`<or-translate value="panel_peakUsage.heading"></or-translate>`;
  public dark = true;

  static get styles() {
    return [...super.styles, styling];
  }

  protected async getPanelContent(): Promise<TemplateResult> {
    return html`
      <div style="display: flex; flex-direction: column; gap: 24px;">
        ${until(this.getSummaryTemplate(this.meterAsset))}
      </div>
    `;
  }

  protected async getSummaryTemplate(meterAsset?: OurgridMeterAsset): Promise<TemplateResult> {
    return html`
      <div>
        <span class="text-primary">
          <or-translate style="display: inline;" value="panel_peakNotification.primaryText"></or-translate>
          ${when(meterAsset?.attributes, () => {
            const descriptor = AssetModelUtil.getAttributeDescriptor("challengePowerLimit", meterAsset?.type);
            const powerGoal = Util.getAttributeValueAsString(
              meterAsset?.attributes.challengePowerLimit,
              descriptor,
              meterAsset?.type,
              true,
              "0"
            );
            return html`
              <span class="statistic-medium" style="color: var(--og-color-success); white-space: nowrap;">
                ${i18next.t("panel_peakNotification.wattToSpare", { value: powerGoal })}
              </span>
            `;
          })}
        </span>
      </div>
    `;
  }
}
