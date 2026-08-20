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
import { css, html, type PropertyValues, type TemplateResult } from "lit";
import { customElement, state } from "lit/decorators.js";
import type { PanelAction } from "../components/og-panel";
import { i18next } from "@openremote/or-translate";
import type { Asset } from "@openremote/model";
import { manager, Util } from "@openremote/core";
import { when } from "lit/directives/when.js";
import moment from "moment";
import { OgMeterChallengeState } from "../util/util";
import { PanelPeakUsage } from "./panel-peak-usage";
import { until } from "lit/directives/until.js";
import { Defaults } from "../util/defaults";
import rest from "rest";

const styling = css`
  :host {
    --or-icon-fill: var(--og-color-neutral);
  }

  #details-container {
    display: flex;
    flex-direction: column;
    gap: 12px;
  }

  .details-item {
    display: flex;
    align-items: center;
    gap: 12px;
  }

  .text-heading {
    line-height: 100%; /*override*/
    max-width: unset;
  }
`;

@customElement("panel-peak-notification")
export class PanelPeakNotification extends PanelPeakUsage {
  @state()
  protected status?: OgMeterChallengeState = OgMeterChallengeState.JOIN_CHALLENGE;

  protected waitingForChallengeDeferred?: Util.Deferred<void>;
  protected countdownInterval?: NodeJS.Timeout;

  public heading = html`<or-translate value="panel_peakNotification.heading"></or-translate>`;
  public dark = true;
  public action: PanelAction = this.getDefaultAction();

  protected getDefaultAction(): PanelAction {
    return {
      text: "panel_peakNotification.buttonText", // uses auto-translate
      color: "var(--og-color-warning)",
      action: undefined,
    };
  }

  static get styles() {
    return [...super.styles, styling];
  }

  public disconnectedCallback() {
    super.disconnectedCallback();
    clearInterval(this.countdownInterval);
  }

  // Lit lifecycle; before update
  protected willUpdate(changedProps: PropertyValues) {
    super.willUpdate(changedProps);

    if (changedProps.has("status")) {
      // If the challenge is joined or active/started, we should resolve the promise
      if ([OgMeterChallengeState.JOINED_CHALLENGE, OgMeterChallengeState.ACTIVE_CHALLENGE].includes(this.status)) {
        this.waitingForChallengeDeferred?.resolve();
        this.waitingForChallengeDeferred = undefined;
      }

      // If challenge is 'joinable', put it back to default state.
      if (this.status === OgMeterChallengeState.JOIN_CHALLENGE) {
        this.setActionVisual(this.getDefaultAction());
      }

      // If challenge is joined, but not started yet...
      // Show a "SUCCESS" button for 3 seconds, and then change it to countdown.
      else if (this.status === OgMeterChallengeState.JOINED_CHALLENGE) {
        this.setActionVisual({
          text: i18next.t("panel_peakNotification.joinedText"),
          color: "var(--og-color-success)",
          disabled: true,
        });
        if (this.challengeAsset) {
          setTimeout(
            () => this.startActionCountdown(this.challengeAsset),
            Defaults.PEAK_NOTIFICATION_JOINED_TIMEOUT_MS
          );
        }
      }

      // If challenge became active, this panel should show a 'starting soon' message,
      // since we do not expect this panel to be shown during a challenge.
      else if (this.status === OgMeterChallengeState.ACTIVE_CHALLENGE) {
        this.setActionVisual({
          text: i18next.t("panel_peakNotification.startingSoon"),
          color: "var(--og-color-success)",
          disabled: true,
        });
      } else {
        console.error("Unknown status?");
      }
    }
  }

  // After every update; lifecycle method
  // Note: we intentionally update this.status AFTER update, so it will trigger willUpdate() logic again.
  // TODO: Maybe approach this differently, see note above.
  protected updated(changedProps: PropertyValues) {
    super.updated(changedProps);
    if (changedProps.has("meterAsset") && this.meterAsset) {
      this.status = this.meterAsset.attributes.challengeStatus?.value;
    }
  }

  /* --------------------------------------------------- */

  // Sets action button to the correct "in X minutes" text every second.
  // So it gives an idea of when the challenges start. We use the default MomentJS format at the moment.
  protected startActionCountdown(challengeAsset: Asset) {
    const startAttribute = challengeAsset?.attributes.challengeStart?.value;
    if (startAttribute) {
      clearInterval(this.countdownInterval);
      this.countdownInterval = setInterval(() => {
        if (moment().isAfter(startAttribute)) {
          this.setActionVisual({
            text: i18next.t("panel_peakNotification.startingSoon"),
            color: "var(--og-color-success)",
            disabled: true,
          });
          clearInterval(this.countdownInterval);
        } else {
          const diff = moment(startAttribute).diff(moment(), "seconds");
          const text =
            diff > 60
              ? i18next.t("panel_peakNotification.startingInFewMinutes", {
                  time: moment.duration(diff, "seconds").humanize(),
                })
              : i18next.t("panel_peakNotification.startingInFewSeconds", {
                  time: moment.duration(diff, "seconds").humanize(),
                });
          if (this.action.text !== text) {
            this.setActionVisual({ text, color: "var(--og-color-success)", disabled: true });
          }
        }
      }, 1000);
    } else {
      console.error("Countdown could not be started; no start date found.");
    }
  }

  protected setActionError(error: string) {
    this.action.error = error;
    this.requestUpdate("action");
  }

  protected setActionVisual(action: PanelAction) {
    this.action = action;
  }

  protected async getPanelContent(): Promise<TemplateResult> {
    return html`
      <div style="display: flex; flex-direction: column; gap: 24px;">
        <!-- Notification summary -->
        ${until(this.getSummaryTemplate(this.meterAsset))}

        <!-- Details such as the goal and the time -->
        <div id="details-container">
          <div class="details-item">
            <or-icon icon="clock-outline"></or-icon>
            ${when(
              this.challengeAsset?.attributes,
              () => {
                const startString = this.challengeAsset.attributes.challengeStart?.value;
                const startTime = startString ? moment(startString).format("HH:mm") : "???";
                const endString = this.challengeAsset.attributes.challengeEnd?.value;
                const endTime = endString ? moment(endString).format("HH:mm") : "???";
                return html`
                  <span class="text-primary bold" style="color: var(--og-color-neutral);">
                    ${startTime}
                    <or-translate value="to"></or-translate>
                    ${endTime}
                  </span>
                `;
              },
              () => html`
                <span class="text-primary bold">
                  <or-translate value="error"></or-translate>
                </span>
              `
            )}
          </div>
          <div class="details-item">
            <or-icon icon="star-circle-outline"></or-icon>
            ${when(
              this.challengeAsset?.attributes,
              () => {
                const challengeDuration: number | undefined = this.challengeAsset.attributes.challengeDuration?.value;
                const challengePointInterval: number | undefined =
                  this.challengeAsset.attributes.challengeEarnPointInterval?.value;
                const pointsString =
                  !!challengeDuration && !!challengePointInterval ? challengeDuration / challengePointInterval : "???";
                return html`
                  <span class="text-primary bold" style="color: var(--og-color-neutral);"
                    >${i18next.t("panel_peakNotification.maxEarnings", { points: pointsString })}</span
                  >
                `;
              },
              () => html`
                <span class="text-primary bold" style="color: var(--og-color-neutral)">
                  <or-translate value="error"></or-translate>
                </span>
              `
            )}
          </div>
        </div>
      </div>
    `;
  }

  // Override of og-panel to customize the "loading indicator" mechanism
  // TODO: Remove logic here, that is now present in willUpdate() ???
  public async onActionButtonClick() {
    if (this.meterAsset && this.challengeAsset) {
      const readonly = this.action.loading || this.action.disabled;
      if (!readonly || this.status === OgMeterChallengeState.JOIN_CHALLENGE) {
        this.setActionVisual({
          text: i18next.t("panel_peakNotification.joinedText"),
          color: "var(--og-color-success)",
          disabled: true,
        });
        this.doJoinChallenge().catch((reason) => {
          console.error(reason);
          this.setActionError(i18next.t("tryAgain"));
          setTimeout(() => {
            this.setActionVisual(this.getDefaultAction());
          }, Defaults.PEAK_NOTIFICATION_JOINED_TIMEOUT_MS);
        });
      } else {
        console.warn(`User tried joining the challenge, but status is '${this.status}'`);
      }
    } else {
      console.error("Tried joining challenge, but assets weren't found in cache!");
    }
  }

  // Main method of joining the challenge through the REST API.
  // Once executed, we wait for a WS message with the attribute update.
  // The waitingForChallengeDeferred promise is used at several places to keep track of the status, which we control from here.
  protected async doJoinChallenge() {
    this.waitingForChallengeDeferred = new Util.Deferred();

    await rest.api.DeviceChallengesResource.joinChallenge({
      meterId: this.meterAsset.id,
      challengeId: this.challengeAsset.id,
    });

    console.log("Waiting for challenge to come in through WS");
    await new Promise((resolve) => setTimeout(resolve, Defaults.PEAK_NOTIFICATION_JOINED_TIMEOUT_MS)); // Wait for a few seconds
    await this.waitingForChallengeDeferred?.promise;
    console.log("Challenge received over WS!");
  }
}
