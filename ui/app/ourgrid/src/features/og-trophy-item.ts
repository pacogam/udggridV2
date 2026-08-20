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
import { LitElement, type TemplateResult, html, css } from "lit";
import { customElement, property } from "lit/decorators.js";
import { when } from "lit/directives/when.js";
import moment from "moment";
import { getAppStyle } from "../styles";
import { i18next } from "@openremote/or-translate";

export interface TrophyItem {
  date: Date;
  points: number;
  savings?: number;
}

const styling = css`
  #item-container {
    padding: 12px 0;
    display: flex;
    justify-content: space-between;
    align-items: center;
  }
  #item-statistic {
    display: flex;
    flex-direction: column;
  }
`;

@customElement("og-trophy-item")
export class OgTrophyItem extends LitElement {
  @property()
  protected item?: TrophyItem;

  @property()
  protected divider = false;

  @property()
  protected timeFormat = "D MMMM YYYY - HH:mm";

  static styles = [getAppStyle(), styling];

  protected render(): TemplateResult {
    return when(this.item, () => {
      const formattedDate: string = moment(this.item.date).format(this.timeFormat);
      return html`
        <div id="item-container">
          <div id="item-statistic">
            <span class="text-secondary">${formattedDate}</span>
            <span class="statistic-medium">
              ${this.item.points}
              <or-translate value="${this.item.points === 1 ? "challenge.point" : "challenge.points"}"></or-translate>
            </span>
          </div>
          ${when(
            this.item.savings,
            () => html`
              <div>
                <span class="text-secondary">${this.item.savings} kWh ${i18next.t("saved")}</span>
              </div>
            `
          )}
        </div>
        ${when(this.divider, () => html` <div style="border-bottom: 1px solid #D9D9D9;"></div> `)}
      `;
    });
  }
}
