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
import { getAppStyle } from "../styles";
import { map } from "lit/directives/map.js";
import { when } from "lit/directives/when.js";
import { animate } from "@lit-labs/motion";
import type { TrophyItem } from "../features/og-trophy-item";
import "../components/og-expandable";
import "../features/og-trophy-item";
import "./panel-challenge-earnings";
import { until } from "lit/directives/until.js";
import manager from "@openremote/core";
import moment from "moment";
import { OgDataPanel } from "../components/og-data-panel";
import type { Chip } from "../components/og-chips";
import { showSnackbar } from "../components/og-snackbar";
import { Constants } from "../util/constants";
import { Defaults } from "../util/defaults";
import rest from "rest";

export function getStatisticTemplate(
  prefixIcon?: string,
  isPath = false,
  leftContent?: TemplateResult,
  rightContent?: TemplateResult,
  expandContent?: TemplateResult,
  expandable = true
): TemplateResult {
  const content = html`
    <div style="display: flex; justify-content: space-between; align-items: center;">
      <div style="flex: 1; display: flex; align-items: center; gap: 20px;">
        ${when(prefixIcon, () =>
          isPath
            ? html`<img src="${prefixIcon}" alt="Statistic Icon" width="46" height="46" />`
            : html`<or-icon .icon="${prefixIcon}" style="--internal-or-icon-width: var(--or-icon-width, 46px);" />`
        )}
        <div>
          <div>${when(leftContent, () => leftContent)}</div>
          <div>${when(rightContent, () => rightContent)}</div>
        </div>
      </div>
    </div>
  `;
  if (!expandContent || !expandable) {
    return content;
  } else {
    return html`
            <og-expandable .header="${content}" .limitedHeight="${true}">${expandContent}</og-collapsible>
        `;
  }
}

const styling = css`
  #content-wrapper {
    display: flex;
    flex-direction: column;
    gap: 20px;
  }
`;

@customElement("panel-trophies")
export class PanelTrophies extends OgDataPanel {
  public heading = html`<or-translate value="panel_trophies.heading"></or-translate>`;
  public dotsGraphic = true;

  @state()
  protected trophies: TrophyItem[] = [];

  /* @state() (CURRENTLY COMMENTED OUT, DUE TO UI DESIGN DECISION)
    protected peakTrophies: TrophyItem[] = []; */

  @state()
  protected currentHistoryDate: Date = moment().toDate();

  @state()
  protected peakCurrentHistoryDate: Date = moment().toDate();

  @state()
  protected challengesLoading = false;

  @state()
  protected peakPointsLoading = false;

  static get styles() {
    return [...super.styles, getAppStyle(), styling];
  }

  // Lit lifecycle method during UI update
  protected willUpdate(changedProps: PropertyValues) {
    // If 'currentHistoryDate' goes down (for example by 1 week), we fetch the challenges that are not present yet.
    // These are then formatted into trophies and added onto the state array.
    if (changedProps.has("currentHistoryDate") && this.currentHistoryDate) {
      const historyDate = moment(this.currentHistoryDate)
        .subtract(Defaults.HISTORY_FETCH_AMOUNT, Defaults.HISTORY_FETCH_UNIT as any)
        .toDate();
      console.log(
        `Fetching challenge history between ${historyDate.toDateString()} and ${this.currentHistoryDate.toDateString()}`
      );
      this.fetchTrophies(historyDate, this.currentHistoryDate).then((trophies) => {
        this.trophies = this.trophies.concat(trophies);
      });
    }

    // Do the same for peak points. (CURRENTLY COMMENTED OUT, DUE TO UI DESIGN DECISION)
    /* if(changedProps.has('peakCurrentHistoryDate') && this.peakCurrentHistoryDate) {
            const historyDate = moment(this.peakCurrentHistoryDate).subtract(Defaults.HISTORY_FETCH_AMOUNT, (Defaults.HISTORY_FETCH_UNIT as any)).toDate();
            console.log(`Fetching peak history between ${historyDate.toDateString()} and ${this.currentHistoryDate.toDateString()}`);
            this.fetchPeaks(historyDate, this.peakCurrentHistoryDate).then(trophies => {
                this.peakTrophies = this.peakTrophies.concat(trophies);
            });
        } */

    return super.willUpdate(changedProps);
  }

  // Fetching challenges between start and end date.
  protected async fetchTrophies(start: Date, end: Date): Promise<TrophyItem[]> {
    const promise = rest.api.DeviceChallengesResource.getHistory({
      startTimestamp: start.getTime(),
      endTimestamp: end.getTime(),
    });
    promise.catch((e) => {
      console.error(e);
      showSnackbar(undefined, "error.challengeDataFailed");
    });
    const data = (await promise).data;
    return data.map((challenge) => ({ date: new Date(challenge.startDate), points: challenge.points }) as TrophyItem);
  }

  protected async fetchPeaks(start: Date, end: Date): Promise<TrophyItem[]> {
    const pointsPerDay = this.peakPointsAsset?.attributes?.[Constants.METER_PEAK_DAY_POINTS_ATTRIBUTE]?.value || 1;
    const data = (
      await manager.rest.api.AssetDatapointResource.getDatapoints(
        this.meterAsset.id,
        Constants.METER_PEAK_POINTS_ATTRIBUTE,
        {
          type: "all",
          fromTimestamp: start.getTime(),
          toTimestamp: end.getTime(),
        }
      )
    ).data;
    return data.map(
      (datapoint) => ({ date: moment(datapoint.x).subtract(1, "day").toDate(), points: pointsPerDay }) as TrophyItem
    );
  }

  /* ------------------------------------------ */

  // Panel HTML render
  // Uses guard() directive to only render once trophies list (or asset data) has changed.
  protected async getPanelContent(): Promise<TemplateResult> {
    const activeChallengePoints = this.meterAsset?.attributes?.[Constants.CHALLENGE_POINTS_ATTRIBUTE]?.value || 0;
    const challengesJoined = this.meterAsset?.attributes?.[Constants.CHALLENGE_JOINED_ATTRIBUTE]?.value || 0;
    const peakPoints = this.meterAsset?.attributes?.[Constants.METER_PEAK_POINTS_ATTRIBUTE]?.value || 0;
    const peakPointsPerDay = this.peakPointsAsset?.attributes?.[Constants.METER_PEAK_DAY_POINTS_ATTRIBUTE]?.value || 1;
    return html`
      <div id="content-wrapper">
        <div class="menu-earnings-card">
          <panel-challenge-earnings
            .meterAsset="${this.meterAsset}"
            .challengeAsset="${this.challengeAsset}"
          ></panel-challenge-earnings>
        </div>
        <div class="content-container" ${animate()}>
          ${getStatisticTemplate(
            "images/yellow-star.svg",
            true,
            html`
              <span class="text-primary bold">${challengesJoined}</span>
              <span class="text-primary"><or-translate value="panel_trophies.challengesJoined"></or-translate></span>
            `,
            html`
              <span class="text-primary">${activeChallengePoints}</span>
              <span class="text-primary"><or-translate value="panel_trophies.pointsEarned"></or-translate></span>
            `,
            html`
              ${until(this.getTrophiesTemplate("challenges", this.trophies, this.challengesLoading, () => this.onLoadMoreClick(), "D MMMM YYYY - HH:mm"))}
            `,
            this.trophies?.length > 0
          )}
        </div>
        <div class="content-container" ${animate()}>
          ${getStatisticTemplate(
            "images/green-star.svg",
            true,
            html`
              <span class="text-primary bold">${peakPoints * peakPointsPerDay}</span>
              <span class="text-primary"><or-translate value="panel_trophies.totalPeaksAvoided"></or-translate></span>
            `,
            html`
              <span class="text-primary">${peakPoints}</span>
              <span class="text-primary"><or-translate value="panel_trophies.peakPointsEarned"></or-translate></span>
            `,
            html`
              <div style="padding: 8px 16px;">
                <p class="text-secondary">
                  <or-translate value="panel_trophies.peakTutorial"></or-translate>
                </p>
              </div>
            `,
            true
          )}
        </div>
      </div>
    `;
  }

  protected async getTrophiesTemplate(
    type: "challenges" | "peak",
    trophies: TrophyItem[],
    loading: boolean,
    onLoadMore: () => void,
    timeFormat?: string
  ): Promise<TemplateResult> {
    const chips: Chip[] = [
      {
        text: html`<or-translate value="panel_trophies.loadMore"></or-translate>`,
        loading,
        action: () => onLoadMore(),
      },
    ];
    return html`
      <div style="padding-top: 8px;">
        ${when(
          trophies.length === 0,
          () => html`
            <div style="display: flex; justify-content: center; padding: 16px;">
              <span
                ><or-translate
                  value="${type === "challenges" ? "panel_trophies.noChallengesFound" : "panel_trophies.noPeaksFound"}"
                ></or-translate
              ></span>
            </div>
          `,
          () => html`
            ${map(
              trophies,
              (item) => html`
                <og-trophy-item .item="${item}" divider="${true}" .timeFormat="${timeFormat}"></og-trophy-item>
              `
            )}
          `
        )}
      </div>
      <div style="display: flex; justify-content: center; margin-top: 12px;">
        <og-chips .chips="${chips}" .outlined="${true}"></og-chips>
      </div>
    `;
  }

  // When attempting to load more, we decrease currentHistoryDate (for example by 1 week).
  // This will trigger the fetching of the challenges during willUpdate();
  protected onLoadMoreClick() {
    this.currentHistoryDate = moment(this.currentHistoryDate)
      .subtract(Defaults.HISTORY_FETCH_AMOUNT, Defaults.HISTORY_FETCH_UNIT as any)
      .toDate();

    // 'fake' loading mechanism for visualization
    this.challengesLoading = true;
    setTimeout(() => (this.challengesLoading = false), 500);
  }

  protected onLoadMorePeaksClick() {
    this.peakCurrentHistoryDate = moment(this.peakCurrentHistoryDate)
      .subtract(Defaults.HISTORY_FETCH_AMOUNT, Defaults.HISTORY_FETCH_UNIT as any)
      .toDate();

    // 'fake' loading mechanism for visualization
    this.peakPointsLoading = true;
    setTimeout(() => (this.peakPointsLoading = false), 500);
  }
}
