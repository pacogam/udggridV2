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
import { LitElement, html, css, nothing } from "lit";
import { customElement, property } from "lit/decorators.js";
import { map } from "lit/directives/map.js";
import { when } from "lit/directives/when.js";
import { getAppStyle } from "../styles";

enum PointStatus {
  EMPTY,
  DISABLED,
  HALF,
  FULL,
}

const styling = css`
  #points-wrapper {
    display: flex;
    flex-direction: column;
    gap: 8px;
  }

  #points-container {
    display: flex;
    gap: 1vw;
    flex-wrap: wrap;
  }
`;

@customElement("og-points")
export class OgPoints extends LitElement {
  @property()
  protected min = 0;

  @property()
  protected max = 10;

  @property()
  protected progress = 5.5;

  @property()
  protected disabledAmount = 0;

  @property({ type: Boolean })
  protected centered = false;

  @property()
  protected label = true;

  @property()
  protected lines = 1;

  static styles = [getAppStyle(), styling];

  protected render() {
    const progressFloor = Math.floor(this.progress);
    const progressCeil = Math.ceil(this.progress);
    const points: PointStatus[] = [];
    for (let i = this.min; i < this.max; i++) {
      if (i < progressFloor) {
        points.push(PointStatus.FULL);
      } else if (i < progressCeil) {
        points.push(PointStatus.HALF);
      } else if (i > this.max - this.disabledAmount) {
        points.push(PointStatus.DISABLED);
      } else {
        points.push(PointStatus.EMPTY);
      }
    }
    return html`
      <div id="points-wrapper">
        <div id="points-container" style="${this.centered ? "justify-content: center;" : nothing}">
          ${map(points, (status, i) => {
            let src;
            switch (status) {
              case PointStatus.FULL: {
                src = "images/points-star-full.svg";
                break;
              }
              case PointStatus.HALF: {
                src = "images/points-star-half.svg";
                break;
              }
              case PointStatus.DISABLED: {
                src = "images/points-star-disabled.svg";
                break;
              }
              default: {
                src = "images/points-star-empty.svg";
                break;
              }
            }
            return html`
              <div style="flex: 0 0 ${(85 / points.length) * this.lines}%">
                <img src="${src}" alt="Progress Star ${i}" style="width: 100%; height: auto;" />
              </div>
            `;
          })}
        </div>
        ${when(
          this.label,
          () => html`
            <span class="text-primary" style="color: var(--og-color-neutral)">
              ${Math.min(Math.round((this.progress / this.max) * 100), 100)} or
              ${Math.max(Math.min(Math.round((this.progress / this.max) * 100), 100), 0)}<or-translate
                value="challenge.percentOfPointsEarned"
              ></or-translate>
            </span>
          `
        )}
      </div>
    `;
  }
}
