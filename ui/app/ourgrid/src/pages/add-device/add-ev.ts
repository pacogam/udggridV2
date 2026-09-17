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
import type { GridAppStateKeyed } from "../../util/og-state";
import { InputType, type OrInputChangedEvent, type OrMwcInput } from "@openremote/or-mwc-components/or-mwc-input";
import type { Store } from "@reduxjs/toolkit";
import { customElement, query, state } from "lit/decorators.js";
import { when } from "lit/directives/when.js";
import { css, html, type TemplateResult } from "lit";
import { GraphicType } from "../../features/og-usage-graphic";
import { i18next } from "@openremote/or-translate";
import { router } from "@openremote/or-app";
import { until } from "lit/directives/until.js";
import { OgVehicleBrand, type OurgridMeterAsset } from "../../util/util";
import { Constants } from "../../util/constants";
import { type OgInputButtonGroupOption, OgSpecialInputType } from "../../components/og-input";
import { type DeviceCharacteristic, WellknownCharacteristics } from "model";
import rest from "rest";

export function pageAddEvProvider(store: Store<GridAppStateKeyed>): OgPageProvider<GridAppStateKeyed> {
  return {
    name: "add-ev",
    routes: ["add-ev"],
    pageCreator: () => new PageAddEv(store),
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
    height: 100%;
    display: flex;
    flex-direction: column;
    gap: 48px;
  }

  #page-icon {
    margin-top: -48px;
    height: calc(var(--og-font-size-title) * 1.5);
    width: calc(var(--og-font-size-title) * 1.5);
    background-color: var(--og-color-primary-dark);
    -webkit-mask-image: url("images/car-charging.svg");
    -webkit-mask-repeat: no-repeat;
    -webkit-mask-position: center;
    -webkit-mask-size: calc(var(--og-font-size-title) * 1.5) auto;
    mask-image: url("images/car-charging.svg");
    mask-repeat: no-repeat;
    mask-position: center;
    mask-size: calc(var(--og-font-size-title) * 1.5) auto;
  }

  .ev-option-item {
    display: flex;
    gap: 8px;
  }

  .ev-option-item.vertical {
    flex-direction: column;
  }

  .ev-option-item.horizontal {
    justify-content: space-between;
    align-items: center;
  }

  .page-action {
    width: calc(100% - 32px);
    padding: 16px 32px;
  }
`;

@customElement("page-add-ev")
export class PageAddEv extends OgPage<GridAppStateKeyed> {
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
  protected _selectedBrand?: OgVehicleBrand;

  @state()
  protected _hasCharger?: boolean;

  @state()
  protected _automaticControl?: boolean;

  @state()
  protected userAsset?: OurgridMeterAsset;

  @state()
  protected characteristics?: DeviceCharacteristic[];

  get name(): string {
    return "add-ev";
  }

  static get styles() {
    return [...super.styles, styling];
  }

  stateChanged(state: GridAppStateKeyed): void {
    this.userAsset = state.gridApp.assets.find((a) => a.id === state.gridApp.userAssetId);
    const characteristics = JSON.parse(
      this.userAsset?.attributes?.[PageAddEv.HOUSEHOLD_CHARACTERISTICS_ATTRIBUTE]?.value
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
        <div id="page-icon"></div>
        <or-translate class="page-title text-heading" value="addEv.intro"></or-translate>
        <div class="page-content">${until(this._getEvFormTemplate(), html`Loading...`)}</div>

        <!-- If error, show "back" button, otherwise show "connect device" or "add device" based on automaticControl is true/false -->
        ${when(
          !this._isError(),
          () => html`
            <og-input
              class="page-action"
              .type=${InputType.BUTTON}
              raised
              rounded
              fullWidth
              label=${this._automaticControl ? "addEv.connectDevice" : "addEv.addDevice"}
              ?disabled=${!this._isValid()}
              @or-mwc-input-changed=${this._onEvAddClick}
            ></og-input>
          `,
          () => html`
            <og-input
              class="page-action"
              .type=${InputType.BUTTON}
              raised
              rounded
              fullWidth
              label="addEv.goBack"
              @or-mwc-input-changed=${this._onBackClick}
            ></og-input>
          `
        )}
      </div>
    `;
  }

  protected _toggleOptions: OgInputButtonGroupOption[] = [
    {
      icon: "close",
      iconColors: { active: "var(--og-color-primary)", inactive: "var(--og-color-warning)" },
      borderColors: { active: "var(--og-color-warning)", inactive: "var(--og-color-warning)" },
      fillColors: { active: "var(--og-color-warning)" },
    },
    {
      icon: "check",
      iconColors: { active: "var(--og-color-primary)", inactive: "var(--og-color-success)" },
      borderColors: { active: "var(--og-color-success)", inactive: "var(--og-color-success)" },
      fillColors: { active: "var(--og-color-success)" },
    },
  ];

  protected async _getEvFormTemplate(): Promise<TemplateResult> {
    const chargerGroupValue = this._hasCharger === true ? 1 : this._hasCharger === false ? 0 : undefined;
    const automaticControlGroupValue =
      this._automaticControl === true ? 1 : this._automaticControl === false ? 0 : undefined;
    return html`
      <div class="page-content-container">
        <!-- Initial question if user has a car charger -->
        <div class="ev-option-item horizontal">
          <or-translate
            class="text-secondary bold"
            value="panel_characteristics.question_vehicleCharger"
          ></or-translate>
          <og-input
            .type=${OgSpecialInputType.BUTTON_GROUP}
            value=""
            .options="${this._toggleOptions}"
            .value=${chargerGroupValue}
            @or-mwc-input-changed=${this._onChargerToggle}
          ></og-input>
        </div>

        ${when(
          this._hasCharger,
          () => html`
            <!-- When charger is YES, show control to enable/disable automatic control -->
            <div class="ev-option-item horizontal">
              <or-translate class="text-secondary bold" value="addEv.question_automaticChargeControl"></or-translate>
              <og-input
                .type=${OgSpecialInputType.BUTTON_GROUP}
                value=""
                .options="${this._toggleOptions}"
                .value=${automaticControlGroupValue}
                @or-mwc-input-changed=${this._onAutomaticControlToggle}
              ></og-input>
            </div>

            ${when(
              this._automaticControl === false,
              () => html`
                <!-- Without automatic control: select brand -->
                <div class="ev-option-item vertical">
                  <or-translate class="text-secondary bold" value="addEv.selectBrand"></or-translate>
                  <og-input
                    type=${InputType.SELECT}
                    label=${i18next.t("addEv.selectBrandPlaceholder")}
                    style="width: 100%;"
                    .options=${[OgVehicleBrand.AUDI, OgVehicleBrand.HYUNDAI, OgVehicleBrand.KIA, OgVehicleBrand.OPEL, OgVehicleBrand.PEUGEOT, OgVehicleBrand.RENAULT, OgVehicleBrand.TESLA, OgVehicleBrand.VOLKSWAGEN_ID, OgVehicleBrand.VOLVO, OgVehicleBrand.OTHER]}
                    .value=${this._selectedBrand}
                    @or-mwc-input-changed=${this._onBrandSelect}
                  ></og-input>
                </div>
              `
            )}
          `,
          () =>
            when(
              this._hasCharger === false,
              () => html`
                <!-- No charger? Show error that you need a charger -->
                <div style="flex: 1; display: flex; text-align: center; justify-content: center; padding: 25% 0;">
                  <or-translate class="text-subheading" value="addEv.error_needCharger"></or-translate>
                </div>
              `
            )
        )}
      </div>
    `;
  }

  /**
   * Returns a boolean whether to display an "error" text (with button) or not.
   * @protected
   */
  protected _isError() {
    return this._hasCharger === false;
  }

  /**
   * Returns a boolean whether the form is valid or not, which normally corresponds with the 'button disabled' state.
   * If the user has a charger, we check if they use 'automatic control' or if they have selected a car brand.
   * When any of the requirements are NOT met, the form is invalid.
   * @protected
   */
  protected _isValid() {
    return this._hasCharger && (this._automaticControl || this._selectedBrand);
  }

  /**
   * Event callback for toggling the "Do you have a car charger" button.
   * @param ev - The respective `or-mwc-input` event
   * @protected
   */
  protected _onChargerToggle(ev: OrInputChangedEvent) {
    this._hasCharger = ev.detail.value === 1;
  }

  /**
   * Event callback for toggling the "Do you want to enable automatic control of charging?" button.
   * @param ev - The respective `or-mwc-input` event
   * @protected
   */
  protected _onAutomaticControlToggle(ev: OrInputChangedEvent) {
    this._automaticControl = ev.detail.value === 1;
  }

  /**
   * Event callback for selecting a car brand in the dropdown/select menu.
   * @param ev - The respective `or-mwc-input` event
   * @protected
   */
  protected _onBrandSelect(ev: OrInputChangedEvent) {
    this._selectedBrand = ev.detail.value;
  }

  /**
   * Event callback for the "Connect device" / "Add device" button.
   * If automatic control is enabled, we should link to EARN-E / ENODE.
   * If automatic control is disabled, we should add the EV to the user's household characteristics manually.'
   * @param ev - The respective `or-mwc-input` event
   * @protected
   */
  protected _onEvAddClick(ev: OrInputChangedEvent) {
    if (this._isValid()) {
      // If automatic control is ENABLED, we should link to EARN-E / ENODE
      if (this._automaticControl) {
        const meterId = this.userAsset?.attributes?.deviceId?.value;
        if (meterId) {
          window.location.href = Constants.AUTHORIZE_EV_URL.replace("{meterId}", meterId);
        } else {
          console.error("Could not authorize EV: No meterId found in user asset");
        }

        // If automatic control is DISABLED, we should add the EV to the user's household manually.
      } else {
        const characteristics = this.characteristics || [];
        const evInfo = characteristics.find((c) => c.id === WellknownCharacteristics.ELECTRIC_VEHICLE);
        const chargerInfo = characteristics.find((c) => c.id === WellknownCharacteristics.VEHICLE_CHARGER);
        if (!evInfo) {
          // Create Ev characteristics
          characteristics.push({
            id: WellknownCharacteristics.ELECTRIC_VEHICLE,
            shown: true,
            brand: this._selectedBrand,
          });
        } else {
          // Update Ev characteristics
          evInfo.shown = true;
          evInfo.brand = this._selectedBrand;
        }
        if (!chargerInfo) {
          // Create charger characteristics
          characteristics.push({
            id: WellknownCharacteristics.VEHICLE_CHARGER,
            shown: this._hasCharger || false,
            brand: this._hasCharger ? this._selectedBrand : undefined,
          });
        } else {
          // Update charger characteristics
          chargerInfo.shown = this._hasCharger || false;
          if (this._hasCharger) chargerInfo.brand = this._selectedBrand;
        }

        // Save characteristics
        this._actionElem.label = "addEv.saveSuccess";
        this._actionElem.style.setProperty("--or-mwc-input-color", "var(--og-color-success");
        rest.api.DeviceCharacteristicsResource.setCharacteristics({ characteristics }).finally(() => {
          setTimeout(() => router.navigate("devices"), 1000);
        });
      }
    }
  }

  /**
   * Event callback for the "Go back" button.
   * @param ev - The respective `or-mwc-input` event.
   * @protected
   */
  protected _onBackClick(ev: OrInputChangedEvent) {
    router.navigate("add-device");
  }
}
