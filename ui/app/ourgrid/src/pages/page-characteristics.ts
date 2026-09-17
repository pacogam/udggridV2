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
import { OgPage, type OgPageProvider } from "./util/og-page";
import type { GridAppStateKeyed } from "../util/og-state";
import { customElement, query, state } from "lit/decorators.js";
import type { Store } from "@reduxjs/toolkit";
import { css, html, type TemplateResult } from "lit";
import "./../features/og-characteristics-settings";
import type { Asset, DeviceCharacteristic } from "model";
import { InputType } from "@openremote/or-mwc-components/or-mwc-input";
import type { OgCharacteristicsUpdateEvent } from "../features/og-characteristics-settings";
import manager from "@openremote/core";
import type { OgInput } from "../components/og-input";
import { i18next } from "@openremote/or-translate";
import { Defaults } from "../util/defaults";
import rest from "rest";

export function pageCharacteristicsProvider(store: Store<GridAppStateKeyed>): OgPageProvider<GridAppStateKeyed> {
  return {
    name: "characteristics",
    routes: ["characteristics"],
    pageCreator: () => new PageCharacteristics(store),
  };
}

const styling = css`
  .page-wrapper {
    width: 100%;
    height: 100vh;
    background: var(--og-color-primary);
  }
`;

const HOUSEHOLD_CHARACTERISTICS_ATTRIBUTE = "householdEnergyCharacteristics";

@customElement("page-characteristics")
export class PageCharacteristics extends OgPage<GridAppStateKeyed> {
  @state()
  protected meterAsset?: Asset;

  @state()
  protected characteristics?: DeviceCharacteristic[];

  protected newCharacteristics?: DeviceCharacteristic[];

  @state()
  protected valid = true;

  @query("#submit-button")
  protected submitButton?: OgInput;

  get name(): string {
    return "characteristics";
  }

  stateChanged(state: GridAppStateKeyed): void {
    this.meterAsset = state.gridApp.assets.find((a) => a.id === state.gridApp.userAssetId);
    const characteristics = JSON.parse(this.meterAsset?.attributes?.[HOUSEHOLD_CHARACTERISTICS_ATTRIBUTE]?.value) as
      DeviceCharacteristic[] | undefined;
    if (characteristics && JSON.stringify(characteristics) !== JSON.stringify(this.characteristics)) {
      this.characteristics = characteristics;
    }
    super.stateChanged(state);
  }

  static get styles() {
    return [...super.styles, styling];
  }

  protected render(): TemplateResult {
    return html`
      <div class="page-wrapper">
        <div style="height: 100%; display: flex; flex-direction: column; align-items: center;">
          <div
            style="width: calc(100% - 32px); padding: 16px; display: flex; flex-direction: column; align-items: center;"
          >
            <img src="images/dots-onboarding.svg" style="width: 100%;" />
            <span class="text-heading" style="margin-top: -10%; text-align: center; max-width: 65vw;">
              <or-translate value="houseCharacteristics"></or-translate>
            </span>
          </div>
          <div style="flex: 1; width: calc(100% - 64px); padding: 32px;">
            <div>
              <og-characteristics-settings
                .characteristics="${this.characteristics}"
                @characteristics-changed="${(ev) => this.onCharacteristicsUpdate(ev)}"
              ></og-characteristics-settings>
            </div>
          </div>
          <div style="width: calc(100% - 32px); padding: 16px;">
            <og-input
              id="submit-button"
              .type="${InputType.BUTTON}"
              raised
              rounded
              comfortable
              fullWidth
              label="save"
              style="width: 100%; --or-mwc-input-color: var(--og-color-success); --or-icon-fill: var(--og-color-primary)"
              .disabled="${!this.valid}"
              icon="content-save"
              @or-mwc-input-changed="${(ev) => this.onSaveButton(ev)}"
            ></og-input>
          </div>
        </div>
      </div>
    `;
  }

  protected onCharacteristicsUpdate(ev: OgCharacteristicsUpdateEvent) {
    this.valid = ev.detail.valid;
    if (ev.detail.valid) {
      this.newCharacteristics = ev.detail.characteristics;
    }
  }

  protected onSaveButton(_ev: CustomEvent) {
    if (this.newCharacteristics && this.valid) {
      this.submitButton.loading = true;
      rest.api.DeviceCharacteristicsResource.setCharacteristics({
        characteristics: this.newCharacteristics,
      })
        .then(() => {
          this.submitButton.icon = "check-bold";
          this.submitButton.label = i18next.t("success");
          setTimeout(() => {
            this.submitButton.icon = "content-save";
            this.submitButton.label = i18next.t("save");
          }, Defaults.HOUSEHOLD_CHARACTERISTICS_SAVE_BUTTON_TIMEOUT_MS);
        })
        .catch((e) => {
          console.error(e);
          this.submitButton.error = i18next.t("tryAgain");
          setTimeout(() => {
            this.submitButton.error = undefined;
          }, Defaults.HOUSEHOLD_CHARACTERISTICS_SAVE_BUTTON_TIMEOUT_MS);
        })
        .finally(() => {
          this.submitButton.loading = false;
        });
    }
  }
}
