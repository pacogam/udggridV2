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
import { OgPage, type OgPageProvider, PageAnimationType } from "../util/og-page";
import { type GridAppStateKeyed, setServiceUserData } from "../../util/og-state";
import { InputType, type OrInputChangedEvent, type OrMwcInput } from "@openremote/or-mwc-components/or-mwc-input";
import type { Store } from "@reduxjs/toolkit";
import { customElement, query, state } from "lit/decorators.js";
import { css, html, type TemplateResult } from "lit";
import { GraphicType } from "../../features/og-usage-graphic";
import { i18next } from "@openremote/or-translate";
import { router } from "@openremote/or-app";
import { until } from "lit/directives/until.js";
import { OgHomeAutomationBrand, type OurgridMeterAsset } from "../../util/util";
import { type DeviceCharacteristic, WellknownCharacteristics } from "model";
import rest from "rest";

export function pageAddHomeAutomationProvider(store: Store<GridAppStateKeyed>): OgPageProvider<GridAppStateKeyed> {
  return {
    name: "add-homeautomation",
    routes: ["add-homeautomation"],
    pageCreator: () => new PageAddHomeAutomation(store),
    skipDataCheck: true,
    hideHeader: true,
  };
}

const styling = css`
  .page-wrapper {
    width: 100%;
    height: 100vh;
    background: var(--og-color-primary);
    display: flex;
    flex-direction: column;
    align-items: center;
  }

  .page-back-icon {
    position: absolute;
    left: 0;
    z-index: 5;
    --or-icon-fill: var(--og-color-primary-dark);
    --og-mdc-fab-size: 62px;
    --og-mdc-fab-border-radius: 0 50% 50% 50%;
    --og-mdc-fab-transition: all ease 0.5s;
  }

  .page-graphic {
    width: calc(100% - 32px);
    margin-top: 12px;
  }

  .page-title {
    margin-top: 16px;
  }

  .page-content {
    flex: 1;
    width: calc(100% - 32px);
    padding: 64px 16px 16px 16px;
  }

  .page-content-container {
    display: flex;
    flex-direction: column;
    gap: 48px;
  }

  #page-icon {
    margin-top: -48px;
    height: calc(var(--og-font-size-title) * 1.5);
    width: calc(var(--og-font-size-title) * 1.5);
    font-size: calc(var(--og-font-size-title) * 1.5);
  }

  .homeautomation-option-item {
    display: flex;
    gap: 8px;
  }

  .homeautomation-option-item.vertical {
    flex-direction: column;
  }

  .homeautomation-option-item.horizontal {
    justify-content: space-between;
    align-items: center;
  }

  .page-action {
    width: calc(100% - 32px);
    padding: 16px 32px;
  }
`;

@customElement("page-add-homeautomation")
export class PageAddHomeAutomation extends OgPage<GridAppStateKeyed> {
  static HOUSEHOLD_CHARACTERISTICS_ATTRIBUTE = "householdEnergyCharacteristics";

  getAnimationEnterType = () => PageAnimationType.SWIPE_LEFT;
  getAnimationExitType = (newPage) => {
    switch (newPage) {
      case "devices":
        return PageAnimationType.FADE;
      default:
        return PageAnimationType.SWIPE_RIGHT;
    }
  };

  @query(".page-action")
  protected _actionElem?: OrMwcInput;

  @state()
  protected _selectedBrand?: OgHomeAutomationBrand;

  @state()
  protected userAsset?: OurgridMeterAsset;

  @state()
  protected characteristics?: DeviceCharacteristic[];

  get name(): string {
    return "add-homeautomation";
  }

  static get styles() {
    return [...super.styles, styling];
  }

  stateChanged(state: GridAppStateKeyed): void {
    this.userAsset = state.gridApp.assets.find((a) => a.id === state.gridApp.userAssetId);
    const characteristics = JSON.parse(
      this.userAsset?.attributes?.[PageAddHomeAutomation.HOUSEHOLD_CHARACTERISTICS_ATTRIBUTE]?.value
    ) as DeviceCharacteristic[] | undefined;
    if (characteristics && JSON.stringify(characteristics) !== JSON.stringify(this.characteristics)) {
      this.characteristics = characteristics;
    }
    return super.stateChanged(state);
  }

  render() {
    return html`
      <div class="page-wrapper">
        <og-input
          class="page-back-icon"
          type=${InputType.BUTTON}
          action
          icon="chevron-left"
          @or-mwc-input-changed=${this._onBackClick}
        ></og-input>
        <div class="page-graphic">
          <og-usage-graphic id="onboarding-topgraphic" .type="${GraphicType.HEADER}" small="true"></og-usage-graphic>
        </div>
        <or-icon id="page-icon" icon="home-automation"></or-icon>
        <or-translate class="page-title text-heading" value="addHomeAutomation.intro"></or-translate>
        <div class="page-content">${until(this._getHomeAutomationFormTemplate(), html`Loading...`)}</div>
        <og-input
          class="page-action"
          .type=${InputType.BUTTON}
          raised
          rounded
          fullWidth
          label="addHomeAutomation.addDevice"
          ?disabled=${!this._isValid()}
          @or-mwc-input-changed=${this._onHomeAutomationAddClick}
        ></og-input>
      </div>
    `;
  }

  protected async _getHomeAutomationFormTemplate(): Promise<TemplateResult> {
    return html`
      <div class="page-content-container">
        <div class="homeautomation-option-item vertical">
          <or-translate class="text-secondary bold" value="addHomeAutomation.selectBrand"></or-translate>
          <og-input
            type=${InputType.SELECT}
            label=${i18next.t("addHomeAutomation.selectBrandPlaceholder")}
            style="width: 100%;"
            .options=${[OgHomeAutomationBrand.HOME_ASSISTANT, OgHomeAutomationBrand.OPENHAB]}
            .value=${this._selectedBrand}
            @or-mwc-input-changed=${this._onBrandSelect}
          ></og-input>
        </div>
      </div>
    `;
  }

  protected _isValid() {
    return !!this._selectedBrand;
  }

  protected _onBrandSelect(ev: OrInputChangedEvent) {
    this._selectedBrand = ev.detail.value;
  }

  protected _onHomeAutomationAddClick(ev: OrInputChangedEvent) {
    if (this._isValid()) {
      const characteristics = this.characteristics || [];
      const homeAutomationInfo = characteristics.find((c) => c.id === WellknownCharacteristics.HOME_AUTOMATION);
      if (!homeAutomationInfo) {
        // Create Home automation characteristics
        characteristics.push({ id: WellknownCharacteristics.HOME_AUTOMATION, shown: true, brand: this._selectedBrand });
      } else {
        // Update Home automation characteristics
        homeAutomationInfo.shown = true;
        homeAutomationInfo.brand = this._selectedBrand;
      }

      // Set username prefix foe service user
      let prefix: string | undefined;
      switch (this._selectedBrand) {
        case OgHomeAutomationBrand.HOME_ASSISTANT:
          prefix = "homeassistant";
          break;
        case OgHomeAutomationBrand.OPENHAB:
          prefix = "openhab";
          break;
        default:
          prefix = undefined;
          break;
      }

      // Create a service user
      rest.api.UserAccountResource.createUpdateServiceUserAccount({ prefix })
        .then((response) => {
          if (response.status === 200 && response.data) {
            setServiceUserData(response.data);
          } else {
            console.error("Could not update service user:", response);
          }
        })
        .catch((ex) => console.error(ex));

      // Save characteristics
      this._actionElem.label = "addHomeAutomation.saveSuccess";
      this._actionElem.style.setProperty("--or-mwc-input-color", "var(--og-color-success");
      rest.api.DeviceCharacteristicsResource.setCharacteristics({ characteristics }).finally(() => {
        setTimeout(() => router.navigate("homeautomation"), 1000);
      });
    }
  }

  protected _onBackClick(ev: OrInputChangedEvent) {
    router.navigate("add-device");
  }
}
