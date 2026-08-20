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
import { OgPage, type OgPageProvider, PageAnimationType } from "./util/og-page";
import { type GridAppStateKeyed, removeUserAsset } from "../util/og-state";
import { customElement, state } from "lit/decorators.js";
import type { Store } from "@reduxjs/toolkit";
import { css, html, type TemplateResult } from "lit";
import { GraphicType } from "../features/og-usage-graphic";
import type { OurgridMeterAsset } from "../util/util";
import { type Asset, type DeviceCharacteristic, WellknownCharacteristics } from "model";
import { InputType } from "@openremote/or-mwc-components/or-mwc-input";
import { i18next } from "@openremote/or-translate";
import { when } from "lit/directives/when.js";
import { until } from "lit/directives/until.js";
import "../panels/panel-device-info";
import "../panels/panel-battery-info";
import "../panels/panel-heatpump-info";
import "../panels/panel-ev-info";
import "../panels/panel-solar-info";
import "../panels/panel-home-automation";
import { router } from "@openremote/or-app";
import rest from "rest";
import { OgDialog, type OgDialogAction, showDialog } from "../components/og-dialog";

export function pageDevicesProvider(store: Store<GridAppStateKeyed>): OgPageProvider<GridAppStateKeyed> {
  return {
    name: "devices",
    routes: ["devices"],
    pageCreator: () => new PageDevices(store),
    skipDataCheck: true,
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
    padding: 32px 16px 16px 16px;
    overflow-x: auto;
  }

  .page-content-container {
    display: flex;
    flex-direction: column;
    gap: 16px;
  }

  .page-action {
    width: calc(100% - 32px);
    padding: 0 16px;
  }

  .page-action > og-input {
    width: 100%;
    padding-bottom: 16px;
  }
`;

@customElement("page-devices")
export class PageDevices extends OgPage<GridAppStateKeyed> {
  static HOUSEHOLD_CHARACTERISTICS_ATTRIBUTE = "householdEnergyCharacteristics";
  static ESTIMATED_SOLAR_CAPACITY_ATTRIBUTE = "estimatedSolarCapacity";

  getAnimationEnterType = (oldPage: string) => {
    switch (oldPage) {
      case "add-device":
        return PageAnimationType.SWIPE_RIGHT;
      default:
        return PageAnimationType.FADE;
    }
  };

  getAnimationExitType = () => PageAnimationType.SWIPE_LEFT;

  @state()
  protected userAsset?: OurgridMeterAsset;

  @state()
  protected batteryAsset?: Asset;

  @state()
  protected vehicleAsset?: Asset;

  @state()
  protected challengeAsset?: Asset;

  @state()
  protected characteristics?: DeviceCharacteristic[];

  @state()
  protected language?: string;

  get name(): string {
    return "devices";
  }

  stateChanged(state: GridAppStateKeyed): void {
    this.userAsset = state.gridApp.assets.find((a) => a.id === state.gridApp.userAssetId);
    this.batteryAsset = state.gridApp.assets.find((a) => a.id === state.gridApp.batteryAssetId);
    this.vehicleAsset = state.gridApp.assets.find((a) => a.id === state.gridApp.vehicleAssetId);
    this.challengeAsset = state.gridApp.assets.find((a) => a.id === state.gridApp.challengeAssetId);
    this.language = state.gridApp.language;

    const characteristicsJson = this.userAsset?.attributes?.[PageDevices.HOUSEHOLD_CHARACTERISTICS_ATTRIBUTE]?.value;
    if (characteristicsJson) {
      const characteristics = JSON.parse(characteristicsJson);
      if (characteristics && JSON.stringify(characteristics) !== JSON.stringify(this.characteristics)) {
        this.characteristics = characteristics;
      }
    }

    return super.stateChanged(state);
  }

  static get styles() {
    return [...super.styles, styling];
  }

  render() {
    const batteryInfo = this.characteristics?.find((c) => c.id === WellknownCharacteristics.BATTERY);
    const heatPumpInfo = this.characteristics?.find((c) => c.id === WellknownCharacteristics.HEAT_PUMP);
    const evInfo = this.characteristics?.find((c) => c.id === WellknownCharacteristics.ELECTRIC_VEHICLE);
    const chargerInfo = this.characteristics?.find((c) => c.id === WellknownCharacteristics.VEHICLE_CHARGER);
    const homeAutomationInfo = this.characteristics?.find((c) => c.id === WellknownCharacteristics.HOME_AUTOMATION);
    const solarCapacity = this.userAsset?.attributes?.[PageDevices.ESTIMATED_SOLAR_CAPACITY_ATTRIBUTE]?.value as
      number | undefined;

    const showBattery = !!this.userAsset && (!!this.batteryAsset || (batteryInfo && batteryInfo.shown));
    const showEv = !!this.userAsset && (!!this.vehicleAsset || (evInfo && evInfo.shown));
    const showHeatpump = !!this.userAsset && heatPumpInfo && heatPumpInfo.shown;
    const showSolar = !!this.userAsset && solarCapacity && solarCapacity > 0;
    const showHomeAutomation = !!this.userAsset && homeAutomationInfo && homeAutomationInfo.shown;
    const removeButton = !this.userAsset || (showEv && showHeatpump && showBattery && showHomeAutomation);

    return html`
      <div class="page-wrapper">
        <div class="page-graphic">
          <og-usage-graphic id="onboarding-topgraphic" .type="${GraphicType.HEADER}" small="true"></og-usage-graphic>
        </div>
        <div class="page-title">
          <or-translate class="text-heading" value="devices"></or-translate>
        </div>
        <div class="page-content">
          <div class="page-content-container">
            ${when(true, () => until(this._getMeterTemplate()))}
            ${when(showEv, () => until(this._getEvTemplate(this.userAsset, this.vehicleAsset, evInfo, chargerInfo)))}
            ${when(showHeatpump, () => until(this._getHeatPumpTemplate(heatPumpInfo)))}
            ${when(showBattery, () => until(this._getBatteryTemplate(batteryInfo)))}
            ${when(showHomeAutomation, () => until(this._getHomeAutomationTemplate(homeAutomationInfo)))}
            ${when(showSolar, () => until(this._getSolarTemplate()))}
          </div>
        </div>
        <div class="page-action">
          ${when(
            !removeButton,
            () => html`
              <og-input
                .type=${InputType.BUTTON}
                raised
                rounded
                fullWidth
                label="deviceList.addDevice"
                @or-mwc-input-changed=${() => router.navigate("add-device")}
              ></og-input>
            `
          )}
        </div>
      </div>
    `;
  }

  protected async _getSolarTemplate(meterAsset = this.userAsset) {
    return html`<panel-solar-info .meterAsset=${meterAsset}></panel-solar-info>`;
  }

  protected async _getBatteryTemplate(
    info?: DeviceCharacteristic,
    batteryAsset = this.batteryAsset,
    meterAsset = this.userAsset,
    language = this.language
  ): Promise<TemplateResult> {
    return html`<panel-battery-info
      .info=${info}
      .batteryAsset=${batteryAsset}
      .meterAsset=${meterAsset}
      .language=${language}
      @request-remove=${() => this._requestBatteryRemoval(info)}
    ></panel-battery-info>`;
  }

  protected async _getMeterTemplate(meterAsset = this.userAsset, language = this.language): Promise<TemplateResult> {
    return html`<panel-device-info
      .meterAsset="${meterAsset}"
      .language="${meterAsset}"
      @remove="${() => this._afterMeterRemove()}"
    ></panel-device-info>`;
  }

  protected async _getHeatPumpTemplate(info: DeviceCharacteristic): Promise<TemplateResult> {
    return html`<panel-heatpump-info
      .info=${info}
      @request-remove=${() => this._requestHeatPumpRemoval(info)}
    ></panel-heatpump-info>`;
  }

  protected async _getEvTemplate(
    meterAsset = this.userAsset,
    vehicleAsset = this.vehicleAsset,
    info: DeviceCharacteristic,
    chargerInfo?: DeviceCharacteristic
  ): Promise<TemplateResult> {
    return html`<panel-ev-info
      .meterAsset=${meterAsset}
      .vehicleAsset=${vehicleAsset}
      .evInfo=${info}
      .chargerInfo=${chargerInfo}
      @request-remove=${() => this._requestEvRemoval(info)}
    ></panel-ev-info>`;
  }

  protected async _getHomeAutomationTemplate(info: DeviceCharacteristic): Promise<TemplateResult> {
    return html`<panel-homeautomation-info
      .homeAutomationInfo=${info}
      @request-info=${() => this._requestHomeAutomationInfo()}
      @request-remove=${() => this._requestHomeAutomationRemoval(info)}
    ></panel-homeautomation-info>`;
  }

  /**
   * Function that is called AFTER the meter of the user has been removed.
   * In this class we handle that the Asset is removed from the local store.
   */
  protected _afterMeterRemove() {
    console.log("Removing device from local store...");
    window.localStorage.removeItem("characteristics");
    this._store.dispatch(removeUserAsset());
  }

  /**
   * Function that requests removal of their heat pump. Prompts the user beforehand.
   */
  protected _requestHeatPumpRemoval(info: DeviceCharacteristic): void {
    const dialogActions: OgDialogAction[] = [
      { actionName: "cancel", content: "cancel" },
      { actionName: "delete", content: "delete", action: () => this._onDeviceRemove(info) },
    ];
    showDialog(
      new OgDialog()
        .setHeading(i18next.t("deviceList.deleteHeatPump"))
        .setContent(html`<or-translate value="deviceList.deleteHeatPumpConfirm"></or-translate>`)
        .setDismissAction(null)
        .setActions(dialogActions) as OgDialog
    );
  }

  /**
   * Function that requests removal of their electric vehicle. Prompts the user beforehand.
   */
  protected _requestEvRemoval(info: DeviceCharacteristic): void {
    const dialogActions: OgDialogAction[] = [
      { actionName: "cancel", content: "cancel" },
      { actionName: "delete", content: "delete", action: () => this._onDeviceRemove(info) },
    ];
    showDialog(
      new OgDialog()
        .setHeading(i18next.t("deviceList.deleteEv"))
        .setContent(html`<or-translate value="deviceList.deleteEvConfirm"></or-translate>`)
        .setDismissAction(null)
        .setActions(dialogActions) as OgDialog
    );
  }

  /**
   * Function that requests removal of their battery. Prompts the user beforehand.
   */
  protected _requestBatteryRemoval(info: DeviceCharacteristic): void {
    const dialogActions: OgDialogAction[] = [
      { actionName: "cancel", content: "cancel" },
      { actionName: "delete", content: "delete", action: () => this._onDeviceRemove(info) },
    ];
    showDialog(
      new OgDialog()
        .setHeading(i18next.t("deviceList.deleteBattery"))
        .setContent(html`<or-translate value="deviceList.deleteBatteryConfirm"></or-translate>`)
        .setDismissAction(null)
        .setActions(dialogActions) as OgDialog
    );
  }

  protected _requestHomeAutomationInfo(): void {
    router.navigate("homeautomation");
  }

  /**
   * Function that requests removal of their home automation integration. Prompts the user beforehand.
   * When the user agrees, it will also remove the service user of the home automation integration.
   */
  protected _requestHomeAutomationRemoval(info: DeviceCharacteristic): void {
    const dialogActions: OgDialogAction[] = [
      { actionName: "cancel", content: "cancel" },
      { actionName: "delete", content: "delete", action: () => this._onDeviceRemove(info) },
    ];
    showDialog(
      new OgDialog()
        .setHeading(i18next.t("deviceList.deleteHomeAutomation"))
        .setContent(html`<or-translate value="deviceList.deleteHomeAutomationConfirm"></or-translate>`)
        .setDismissAction(null)
        .setActions(dialogActions) as OgDialog
    );
  }

  protected async _onDeviceRemove(info: DeviceCharacteristic): Promise<void> {
    if (!info) {
      console.error("Could not remove device: No information provided");
      return;
    }

    let reload = false;
    if (info.id === WellknownCharacteristics.ELECTRIC_VEHICLE && this.vehicleAsset) {
      await rest.api.DeviceResource.removeDevice({
        deviceName: this.vehicleAsset.id,
        assetType: this.vehicleAsset.type,
      });
      reload = true;
    }

    if (info.id === WellknownCharacteristics.HOME_AUTOMATION) {
      await rest.api.UserAccountResource.deleteServiceUserAccount(); // Attempt to delete service user of home automation
    }
    await this._removeDevice(info);
    this.requestUpdate();

    if (reload) {
      window.location.reload();
    }
  }

  /**
   * Function that removes a device by modifying the {@link DeviceCharacteristic}.
   * Also removes the brand. Gets immediately submitted to the database using the HTTP API.
   */
  protected async _removeDevice(info: DeviceCharacteristic) {
    info.shown = false;
    info.brand = undefined;
    await rest.api.DeviceCharacteristicsResource.setCharacteristics({ characteristics: this.characteristics });
  }
}
