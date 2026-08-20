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
import { when } from "lit/directives/when.js";
import { i18next } from "@openremote/or-translate";
import "../components/og-chips";
import { OgDataPanel } from "../components/og-data-panel";
import { until } from "lit/directives/until.js";

const styling = css`
  #panel-wrapper {
    padding: 16px;
  }

  #solar-container {
    position: relative;
  }

  #solar-content {
    display: flex;
    align-items: center;
    gap: 24px;
  }

  #solar-icon {
    font-size: var(--og-font-size-statistic-large);
    color: var(--og-color-neutral);
  }
`;

@customElement("panel-solar-info")
export class PanelSolarInfo extends OgDataPanel {
  static SOLAR_CAPACITY_ATTRIBUTE_NAME = "estimatedSolarCapacity";

  public transparent = false;
  public rounded = true;

  static get styles() {
    return [...super.styles, styling];
  }

  protected async getPanelContent(): Promise<TemplateResult> {
    const solarCapacity = this.meterAsset?.attributes?.[PanelSolarInfo.SOLAR_CAPACITY_ATTRIBUTE_NAME]?.value;
    return html`
      <div id="solar-container">
        <div id="solar-content">
          <or-icon id="solar-icon" icon="weather-sunny"></or-icon>
          <div style="display: flex; align-items: center; gap: 24px;">
            <div>
              <div style="display: flex; flex-direction: column; justify-content: space-between; gap: 8px;">
                <span class="text-heading2" style="text-align: start;">
                  ${solarCapacity ? i18next.t("panel_solarInfo.yourSolar") : i18next.t("panel_solarInfo.noSolarFound")}
                </span>
                ${when(
                  solarCapacity,
                  () => html`
                    <div style="display: flex; flex-direction: column; gap: 2px;">
                      ${until(this._getDetailsContentTemplate(Number(solarCapacity)))}
                    </div>
                  `
                )}
              </div>
            </div>
          </div>
        </div>
      </div>
    `;
  }

  protected async _getDetailsContentTemplate(capacity: number): Promise<TemplateResult> {
    return html`
      <div style="display: flex; align-items: center; gap: 8px;">
        <or-icon style="font-size: var(--og-font-size-button-small)" icon="factory"></or-icon>
        <or-translate
          class="text-tertiary"
          value="panel_solarInfo.unknownManufacturer"
          style="text-align: start"
        ></or-translate>
      </div>
      <div style="display: flex; align-items: center; gap: 8px;">
        <or-icon style="font-size: calc(var(--og-font-size-button-small) - 0.05rem)" icon="solar-panel-large"></or-icon>
        <span class="text-tertiary" style="text-align: start;">
          ${i18next.t("panel_solarInfo.estimatedCapacity", { capacity })}
        </span>
      </div>
    `;
  }
}
