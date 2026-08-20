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
import { customElement, state } from "lit/decorators.js";
import type { GridAppStateKeyed } from "../../util/og-state";
import { OgPage, type OgPageProvider, PageAnimationType } from "../util/og-page";
import { InputType, type OrInputChangedEvent } from "@openremote/or-mwc-components/or-mwc-input";
import type { Store } from "@reduxjs/toolkit";
import { css, html, type TemplateResult } from "lit";
import { GraphicType } from "../../features/og-usage-graphic";
import { type Asset, type DeviceCharacteristic, WellknownCharacteristics } from "model";
import type { OurgridMeterAsset } from "../../util/util";
import { until } from "lit/directives/until.js";
import { map } from "lit/directives/map.js";
import { when } from "lit/directives/when.js";
import { router } from "@openremote/or-app";

export function pageAddDeviceProvider(store: Store<GridAppStateKeyed>): OgPageProvider<GridAppStateKeyed> {
  return {
    name: "add-device",
    routes: ["add-device"],
    pageCreator: () => new AddDeviceSelect(store),
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
    margin-top: -40px;
  }

  .page-content {
    flex: 1;
    width: calc(100% - 32px);
    padding: 64px 16px 16px 16px;
  }

  .page-content-container {
    display: flex;
    flex-direction: column;
  }

  .add-device-menu-divider {
    border-bottom: 1px solid #e0e0e0;
  }

  .add-device-menu-item {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 12px 0;
  }

  .add-device-menu-item-icons > * {
    width: var(--og-font-size-title);
    height: var(--og-font-size-title);
    display: flex;
    justify-content: end;
    font-size: var(--og-font-size-title);
  }

  .add-device-menu-item-icons > .svg-icon {
    background-color: var(--og-color-primary-dark);
    mask-repeat: no-repeat;
    mask-position: center;
    mask-size: var(--og-font-size-title) var(--og-font-size-title);
  }

  .add-device-menu-item-chevron {
    width: 36px;
    display: flex;
    justify-content: center;
  }
`;

export interface AddDeviceMenuItem {
  id: string;
  icon?: string;
  svg?: string;
  text: string;
}

@customElement("page-add-device-select")
export class AddDeviceSelect extends OgPage<GridAppStateKeyed> {
  static HOUSEHOLD_CHARACTERISTICS_ATTRIBUTE = "householdEnergyCharacteristics";

  getAnimationEnterType = (oldPage) => {
    switch (oldPage) {
      case "add-ev":
        return PageAnimationType.SWIPE_RIGHT;
      case "add-heatpump":
        return PageAnimationType.SWIPE_RIGHT;
      case "add-battery":
        return PageAnimationType.SWIPE_RIGHT;
      default:
        return PageAnimationType.SWIPE_LEFT;
    }
  };

  getAnimationExitType = (newPage) => {
    switch (newPage) {
      case "add-ev":
        return PageAnimationType.SWIPE_LEFT;
      case "add-heatpump":
        return PageAnimationType.SWIPE_LEFT;
      case "add-battery":
        return PageAnimationType.SWIPE_LEFT;
      default:
        return PageAnimationType.SWIPE_RIGHT;
    }
  };

  @state()
  protected userAsset?: OurgridMeterAsset;

  @state()
  protected batteryAsset?: Asset;

  @state()
  protected vehicleAsset?: Asset;

  @state()
  protected characteristics?: DeviceCharacteristic[];

  get name(): any {
    return "add-device";
  }

  stateChanged(state: GridAppStateKeyed): void {
    this.userAsset = state.gridApp.assets.find((a) => a.id === state.gridApp.userAssetId);
    this.batteryAsset = state.gridApp.assets.find((a) => a.id === state.gridApp.batteryAssetId);
    this.vehicleAsset = state.gridApp.assets.find((a) => a.id === state.gridApp.vehicleAssetId);
    const val = this.userAsset?.attributes?.[AddDeviceSelect.HOUSEHOLD_CHARACTERISTICS_ATTRIBUTE]?.value;
    const characteristics = val ? (JSON.parse(val) as DeviceCharacteristic[]) : undefined;
    if (characteristics && JSON.stringify(characteristics) !== JSON.stringify(this.characteristics)) {
      this.characteristics = characteristics;
    }
    return super.stateChanged(state);
  }

  static get styles() {
    return [...super.styles, styling];
  }

  render() {
    const heatPumpInfo = this.characteristics?.find((c) => c.id === WellknownCharacteristics.HEAT_PUMP);
    const evInfo = this.characteristics?.find((c) => c.id === WellknownCharacteristics.ELECTRIC_VEHICLE);
    const batteryInfo = this.characteristics?.find((c) => c.id === WellknownCharacteristics.BATTERY);
    const homeAutomationInfo = this.characteristics?.find((c) => c.id === WellknownCharacteristics.HOME_AUTOMATION);

    const hasElectricVehicle = (evInfo && evInfo.shown) || !!this.vehicleAsset;
    const hasHeatpump = heatPumpInfo && heatPumpInfo.shown;
    const hasBattery = (batteryInfo && batteryInfo.shown) || !!this.batteryAsset;
    const hasHomeAutomation = homeAutomationInfo && homeAutomationInfo.shown;
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
        <div class="page-title">
          <or-translate
            class="text-heading"
            value="addDevice.intro"
            style="text-align: center; max-width: 65vw;"
          ></or-translate>
        </div>
        <div class="page-content">
          <div class="page-content-container">
            ${until(this._getDeviceSelectMenuTemplate(hasElectricVehicle, hasHeatpump, hasBattery, hasHomeAutomation), html`Loading...`)}
          </div>
        </div>
      </div>
    `;
  }

  protected _onBackClick(ev: OrInputChangedEvent) {
    router.navigate("devices");
  }

  protected async _getDeviceSelectMenuTemplate(
    hasElectricVehicle: boolean,
    hasHeatpump: boolean,
    hasBattery: boolean,
    hasHomeAutomation: boolean
  ): Promise<TemplateResult> {
    const items: AddDeviceMenuItem[] = [];
    if (!hasElectricVehicle) items.push({ id: "ev", svg: "images/car-charging.svg", text: "addDevice.select-ev" });
    if (!hasHeatpump)
      items.push({ id: "heatpump", svg: "images/house-temperature.svg", text: "addDevice.select-heatpump" });
    if (!hasBattery) items.push({ id: "battery", icon: "battery-charging", text: "addDevice.select-battery" });
    if (!hasHomeAutomation)
      items.push({ id: "home-automation", icon: "home-automation", text: "addDevice.select-homeautomation" });
    return html`
      ${map(
        items,
        (item) => html`
          <div class="add-device-menu-item" @click=${() => this._onMenuSelect(item)}>
            <div class="add-device-menu-item-icons">
              ${when(item.icon, () => html` <or-icon icon=${item.icon}></or-icon>`)}
              ${when(item.svg, () => html` <div class="svg-icon" style="mask-image: url(${item.svg})"></div>`)}
            </div>
            <or-translate class="text-secondary bold" value=${item.text}></or-translate>
            <or-icon class="add-device-menu-item-chevron" icon="chevron-right"></or-icon>
          </div>
          <div class="add-device-menu-divider"></div>
        `
      )}
    `;
  }

  protected _onMenuSelect(item: AddDeviceMenuItem) {
    switch (item.id) {
      case "ev":
        router.navigate("add-ev");
        return;
      case "heatpump":
        router.navigate("add-heatpump");
        return;
      case "battery":
        router.navigate("add-battery");
        return;
      case "home-automation":
        router.navigate("add-homeautomation");
      default:
    }
  }
}
