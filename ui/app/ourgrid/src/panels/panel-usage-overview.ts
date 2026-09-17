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
import { type TemplateResult, html, css, nothing, type PropertyValues } from "lit";
import { customElement, property } from "lit/decorators.js";
import "./../features/og-usage-graphic";
import "./../features/og-challenge-timer";
import "./../components/og-statistic";
import type { Asset } from "@openremote/model";
import { when } from "lit/directives/when.js";
import {
  getStateColorByDistrictPowerUsage,
  getStateColorByPowerValue,
  OgMeterChallengeState,
  type OgStateColor,
} from "../util/util";
import { classMap } from "lit/directives/class-map.js";
import { styleMap } from "lit/directives/style-map.js";
import { OgDataPanel } from "../components/og-data-panel";

const styling = css`
  :host {
    --or-icon-width: 32px;
    --or-icon-height: 32px;
  }

  #graphic-container {
    height: 100%;
    display: grid;
    grid-template-columns: 1fr;
    align-items: center;
    padding-bottom: 6px;
  }

  #graphic-container div {
    grid-row-start: 1;
    grid-column-start: 1;
  }

  #statistic-wrapper {
    z-index: 5;
    display: flex;
    align-items: center;
    justify-content: center;
  }

  #statistic-container {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 4px;
    border-radius: 50%;
    margin-top: -6px;
    width: 35vw;
    height: 35vw;
    min-width: 180px;
    min-height: 180px;
    max-width: 250px;
    max-height: 250px;
  }

  .text-heading {
    font-size: 42px;
    transition: color 0.2s;
    -webkit-transition: color 0.2s;
  }

  .line-height {
    line-height: 125%;
  }
`;

const PLACEHOLDER_VALUE = 462;

@customElement("panel-usage-overview")
export class PanelUsageOverview extends OgDataPanel {
  public fullWidth = true;
  public fullHeight = true;

  @property()
  protected aspectRatio = "1/1";

  @property()
  protected staticAnimation = false;

  static get styles() {
    return [...super.styles, styling];
  }

  protected willUpdate(changedProps: PropertyValues) {
    const dark = this.challengeAsset?.attributes
      ? this.meterAsset?.attributes.challengeStatus?.value === OgMeterChallengeState.ACTIVE_CHALLENGE
      : false;
    this.dark = dark;
    this.transparent = !dark;
    return super.willUpdate(changedProps);
  }

  protected async getPanelContent(): Promise<TemplateResult> {
    const attributes = this.meterAsset?.attributes;
    const districtAttributes = this.districtAsset?.attributes;
    const challengeAttributes = this.challengeAsset?.attributes;
    const graphicColor = districtAttributes ? getStateColorByDistrictPowerUsage(districtAttributes) : undefined;
    const isInChallenge = challengeAttributes
      ? this.meterAsset?.attributes.challengeStatus?.value === OgMeterChallengeState.ACTIVE_CHALLENGE
      : false;
    const meterPower: number | undefined = attributes ? attributes.power?.value : undefined;
    const meterPowerMax: number | undefined =
      isInChallenge && attributes ? attributes.challengePowerLimit?.value : undefined;
    const statisticColor = isInChallenge ? getStateColorByPowerValue(meterPower, meterPowerMax) : undefined;
    this.updateComplete.then(() => (this.dark = isInChallenge));
    return html`
      <div id="graphic-container">
        <div id="statistic-wrapper">
          ${when(isInChallenge, () => this.getTimerHTML(this.meterAsset, this.challengeAsset))}
          ${this.getStatisticHTML(meterPower, meterPowerMax, statisticColor, isInChallenge)}
        </div>
        <div style="aspect-ratio: ${this.aspectRatio}; overflow: hidden;">
          <og-usage-graphic
            .dark="${isInChallenge}"
            .color="${graphicColor}"
            .colorAnimation="${this.staticAnimation}"
          ></og-usage-graphic>
        </div>
      </div>
    `;
  }

  protected getTimerHTML(userAsset?: Asset, challengeAsset?: Asset): TemplateResult {
    return html`
      <og-challenge-timer
        .userAsset="${userAsset}"
        .challengeAsset="${challengeAsset}"
        style="position: absolute;"
      ></og-challenge-timer>
    `;
  }

  protected getStatisticHTML(power?: number, maxPower?: number, color?: OgStateColor, dark = false): TemplateResult {
    const customColor: string = color || (dark ? "var(--og-color-primary)" : undefined);
    const headingClasses: {} = {
      "text-heading": true,
      dark,
    };
    const iconStyles: {} = {
      "--or-icon-fill": customColor,
    };
    return html`
      <div
        id="statistic-container"
        style="background: ${dark ? "var(--og-color-primary-dark)" : "var(--og-color-primary)"}; ${dark ? "" : "border:solid 8px var(--og-background-shade);"}"
      >
        <or-icon icon="lightning-bolt" style="${styleMap(iconStyles)}"></or-icon>
        ${when(
          power,
          () => html`
            <og-statistic .value="${Math.round(power)}">
              <span class="${classMap(headingClasses)} line-height" style="color: ${customColor || nothing}" />
            </og-statistic>
          `,
          () => html`
            <span class="${classMap(headingClasses)}" style="color: ${customColor || nothing}"
              >${this.staticAnimation ? PLACEHOLDER_VALUE : "-"}</span
            >
          `
        )}
        <span class="bg text-tertiary medium" style="color: ${customColor || nothing}">Watt</span>
        ${when(
          maxPower,
          () => html`
            <span class="bg text-secondary bold" style="color: var(--og-color-primary)"
              >${Math.round(maxPower)}W Max</span
            >
          `
        )}
      </div>
    `;
  }
}
