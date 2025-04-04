import {OgPage, OgPageProvider, PageAnimationType} from "../util/og-page";
import {GridAppStateKeyed} from "../../util/og-state";
import {InputType, OrInputChangedEvent, OrMwcInput} from "@openremote/or-mwc-components/or-mwc-input";
import {Store} from '@reduxjs/toolkit';
import { customElement, query, state } from "lit/decorators.js";
import {css, html, TemplateResult } from "lit";
import {GraphicType} from "../../features/og-usage-graphic";
import {i18next} from "@openremote/or-translate";
import { router } from "@openremote/or-app";
import { until } from "lit/directives/until.js";
import {OgVehicleBrand, OurgridMeterAsset} from "../../util/util";
import {OgInputButtonGroupOption, OgSpecialInputType} from "../../components/og-input";
import {DeviceCharacteristic, WellknownCharacteristics} from "model";
import rest from "rest";

export function pageAddEvProvider(store: Store<GridAppStateKeyed>): OgPageProvider<GridAppStateKeyed> {
    return {
        name: 'add-ev',
        routes: ['add-ev'],
        pageCreator: () => new PageAddEv(store),
        skipDataCheck: true,
        hideHeader: true
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

    static HOUSEHOLD_CHARACTERISTICS_ATTRIBUTE = 'householdEnergyCharacteristics';

    getAnimationEnterType = () => PageAnimationType.SWIPE_LEFT;
    getAnimationExitType = (newPage) => {
        switch(newPage) {
            case 'devices': return PageAnimationType.FADE;
            default: return PageAnimationType.SWIPE_RIGHT;
        }
    }

    @query('.page-action')
    protected _actionElem?: OrMwcInput;

    @state()
    protected _selectedBrand?: OgVehicleBrand;

    @state()
    protected _hasCharger?: boolean;

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
        this.userAsset = state.gridApp.assets.find(a => a.id === state.gridApp.userAssetId);
        const characteristics = JSON.parse(this.userAsset?.attributes?.[PageAddEv.HOUSEHOLD_CHARACTERISTICS_ATTRIBUTE]?.value) as DeviceCharacteristic[] | undefined;
        if (characteristics && JSON.stringify(characteristics) !== JSON.stringify(this.characteristics)) {
            this.characteristics = characteristics;
        }
        return super.stateChanged(state);
    }

    render() {
        return html`
            <div class="page-wrapper">
                <og-input class="page-back-icon" type=${InputType.BUTTON} action icon="chevron-left" @or-mwc-input-changed=${this._onBackClick}></og-input>
                <div class="page-graphic">
                    <og-usage-graphic id="onboarding-topgraphic" .type="${GraphicType.HEADER}" small="true"></og-usage-graphic>
                </div>
                <div id="page-icon"></div>
                <or-translate class="page-title text-heading" value="addEv.intro"></or-translate>
                <div class="page-content">
                    ${until(this._getEvFormTemplate(), html`Loading...`)}
                </div>
                <og-input class="page-action" .type=${InputType.BUTTON} raised rounded fullWidth label="addEv.addDevice"
                          ?disabled=${!this._isValid()}
                          @or-mwc-input-changed=${this._onEvAddClick}
                ></og-input>
            </div>
        `;
    }

    protected _toggleOptions: OgInputButtonGroupOption[] = [
        {icon: 'close', iconColors: {active: 'var(--og-color-primary)', inactive: 'var(--og-color-warning)'}, borderColors: {active: 'var(--og-color-warning)', inactive: 'var(--og-color-warning)'}, fillColors: {active: 'var(--og-color-warning)'}},
        {icon: 'check', iconColors: {active: 'var(--og-color-primary)', inactive: 'var(--og-color-success)'}, borderColors: {active: 'var(--og-color-success)', inactive: 'var(--og-color-success)'}, fillColors: {active: 'var(--og-color-success)'}}
    ];

    protected async _getEvFormTemplate(): Promise<TemplateResult> {
        return html`
            <div class="page-content-container">
                <div class="ev-option-item vertical">
                    <or-translate class="text-secondary bold" value="addEv.selectBrand"></or-translate>
                    <og-input type=${InputType.SELECT} label=${i18next.t('addEv.selectBrandPlaceholder')} style="width: 100%;"
                              .options=${[OgVehicleBrand.TESLA, OgVehicleBrand.VOLKSWAGEN_ID, OgVehicleBrand.OTHER]}
                              .value=${this._selectedBrand}
                              @or-mwc-input-changed=${this._onBrandSelect}
                    ></og-input>
                </div>
                <div class="ev-option-item horizontal">
                    <span class="text-secondary bold"><or-translate value="panel_characteristics.question_vehicleCharger"/></span>
                    <og-input .type=${OgSpecialInputType.BUTTON_GROUP} value="" .options="${this._toggleOptions}" .value=${this._hasCharger}
                              @or-mwc-input-changed=${this._onChargerToggle}
                    ></og-input>
                </div>
            </div>
        `
    }

    protected _isValid() {
        return this._selectedBrand && this._hasCharger !== undefined
    }

    protected _onBrandSelect(ev: OrInputChangedEvent) {
        this._selectedBrand = ev.detail.value;
    }

    protected _onChargerToggle(ev: OrInputChangedEvent) {
        this._hasCharger = ev.detail.value;
    }

    protected _onEvAddClick(ev: OrInputChangedEvent) {
        if(this._isValid()) {
            const characteristics = this.characteristics || [];
            const evInfo = characteristics.find(c => c.id === WellknownCharacteristics.ELECTRIC_VEHICLE);
            const chargerInfo = characteristics.find(c => c.id === WellknownCharacteristics.VEHICLE_CHARGER);
            if(!evInfo) {
                // Create Ev characteristics
                characteristics.push({id: WellknownCharacteristics.ELECTRIC_VEHICLE, shown: true, brand: this._selectedBrand});
            } else {
                // Update Ev characteristics
                evInfo.shown = true;
                evInfo.brand = this._selectedBrand;
            }
            if(!chargerInfo) {
                // Create charger characteristics
                characteristics.push({
                    id: WellknownCharacteristics.VEHICLE_CHARGER,
                    shown: this._hasCharger || false,
                    brand: (this._hasCharger ? this._selectedBrand : undefined)
                });
            } else {
                // Update charger characteristics
                chargerInfo.shown = this._hasCharger || false;
                if(this._hasCharger) chargerInfo.brand = this._selectedBrand;
            }

            // Save characteristics
            this._actionElem.label = "addEv.saveSuccess";
            this._actionElem.style.setProperty('--or-mwc-input-color', 'var(--og-color-success');
            rest.api.DeviceCharacteristicsResource.setCharacteristics({characteristics: characteristics}).finally(() => {
                setTimeout(() => router.navigate('devices'), 1000);
            })
        }
    }

    protected _onBackClick(ev: OrInputChangedEvent) {
        router.navigate('add-device');
    }
}