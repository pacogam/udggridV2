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
import { OgDataPanel } from "../components/og-data-panel";
import { Util } from "@openremote/core";
import { Defaults } from "../util/defaults";

const challengePointsTotalAttribute = "totalPoints";
const challengeExchangeRateAttribute = "challengePointsExchangeRate";

const styling = css`
  #panel-wrapper {
    padding: 16px;
  }
`;

@customElement("panel-challenge-earnings")
export class PanelChallengeEarnings extends OgDataPanel {
  public transparent = false;
  public rounded = true;

  static get styles() {
    return [...super.styles, styling];
  }

  protected async getPanelContent(): Promise<TemplateResult> {
    const exchangeRate: number = this.challengeAsset?.attributes?.[challengeExchangeRateAttribute]?.value || 0;
    const parsedExchangeRate = Util.resolveUnits(["EUR"], exchangeRate.toFixed(Defaults.POINT_EXCHANGE_RATE_DECIMALS));
    const pointsTotal = this.meterAsset?.attributes?.[challengePointsTotalAttribute]?.value || 0;
    const earnings = Util.resolveUnits(["EUR"], (pointsTotal * exchangeRate).toFixed(Defaults.POINT_EARNINGS_DECIMALS));
    return html`
      <div style="display: flex; flex-direction: column; gap: 6px;">
        <div style="display: flex; justify-content: space-between; align-items: center;">
          <div style="display: flex; align-items: center; gap: 26px;">
            <div style="width: 24px; height: 24px; display: flex; justify-content: center; align-items: center;">
              <img src="images/star.svg" style="width: 22px;" />
            </div>
            <span class="text-primary"><or-translate value="panel_earnings.totalPoints" /></span>
          </div>
          <span>${pointsTotal}</span>
        </div>
        <div style="display: flex; justify-content: space-between; align-items: center;">
          <div style="display: flex; align-items: center; gap: 26px;">
            <or-icon icon="finance"></or-icon>
            <span class="text-primary"><or-translate value="panel_earnings.exchangeRate" /></span>
          </div>
          <span>${parsedExchangeRate}</span>
        </div>
        <div style="display: flex; justify-content: space-between; align-items: center;">
          <div style="display: flex; align-items: center; gap: 26px;">
            <or-icon icon="piggy-bank-outline"></or-icon>
            <span class="text-primary"><or-translate value="panel_earnings.expectedEarnings" /></span>
          </div>
          <span>${earnings}</span>
        </div>
      </div>
    `;
  }
}
