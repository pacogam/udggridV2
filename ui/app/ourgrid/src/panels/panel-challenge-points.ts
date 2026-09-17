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
import { type TemplateResult, html } from "lit";
import { customElement } from "lit/decorators.js";
import "../components/og-points";
import { OgDataPanel } from "../components/og-data-panel";
import { Constants } from "../util/constants";
import { Defaults } from "../util/defaults";

@customElement("panel-challenge-points")
export class PanelChallengePoints extends OgDataPanel {
  public dark = true;

  protected async getPanelContent(): Promise<TemplateResult> {
    const challengeDuration: number =
      this.challengeAsset?.attributes?.[Constants.CHALLENGE_DURATION_ATTRIBUTE]?.value ||
      Defaults.CHALLENGE_DURATION_MINUTES;
    const challengeInterval: number =
      this.challengeAsset?.attributes?.[Constants.CHALLENGE_POINT_INTERVAL_ATTRIBUTE]?.value ||
      Defaults.CHALLENGE_INTERVAL_MINUTRES;
    const challengeMaxPointsAchievable = Math.round(challengeDuration / challengeInterval);
    const challengeProgress = this.meterAsset?.attributes?.[Constants.CHALLENGE_POINT_CURRENT_ATTRIBUTE]?.value || 0;

    return html`
      <div>
        <og-points .progress="${challengeProgress}" .max=${challengeMaxPointsAchievable}></og-points>
      </div>
    `;
  }
}
