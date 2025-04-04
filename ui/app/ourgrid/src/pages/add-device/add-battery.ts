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
import {OgBatteryBrand, OurgridMeterAsset} from "../../util/util";
import {DeviceCharacteristic, WellknownCharacteristics} from "model";
import rest from "rest";

export function pageAddBatteryProvider(store: Store<GridAppStateKeyed>): OgPageProvider<GridAppStateKeyed> {
    return {
        name: 'add-battery',
        routes: ['add-battery'],
        pageCreator: () => new PageAddBattery(store),
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
        font-size: calc(var(--og-font-size-title) * 1.5);
    }
    
    .battery-option-item {
        display: flex;
        gap: 8px;
    }
    
    .battery-option-item.vertical {
        flex-direction: column;
    }
    
    .battery-option-item.horizontal {
        justify-content: space-between;
        align-items: center;
    }

    .page-action {
        width: calc(100% - 32px);
        padding: 16px 32px;
    }
`;

@customElement("page-add-battery")
export class PageAddBattery extends OgPage<GridAppStateKeyed> {

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
    protected _selectedBrand?: OgBatteryBrand;

    @state()
    protected userAsset?: OurgridMeterAsset;

    @state()
    protected characteristics?: DeviceCharacteristic[];

    get name(): string {
        return "add-battery";
    }

    static get styles() {
        return [...super.styles, styling];
    }

    stateChanged(state: GridAppStateKeyed): void {
        this.userAsset = state.gridApp.assets.find(a => a.id === state.gridApp.userAssetId);
        const characteristics = JSON.parse(this.userAsset?.attributes?.[PageAddBattery.HOUSEHOLD_CHARACTERISTICS_ATTRIBUTE]?.value) as DeviceCharacteristic[] | undefined;
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
                <or-icon id="page-icon" icon="battery-charging"></or-icon>
                <or-translate class="page-title text-heading" value="addBattery.intro"></or-translate>
                <div class="page-content">
                    ${until(this._getBatteryFormTemplate(), html`Loading...`)}
                </div>
                <og-input class="page-action" .type=${InputType.BUTTON} raised rounded fullWidth label="addBattery.addDevice"
                          ?disabled=${!this._isValid()}
                          @or-mwc-input-changed=${this._onBatteryAddClick}
                ></og-input>
            </div>
        `;
    }

    protected async _getBatteryFormTemplate(): Promise<TemplateResult> {
        return html`
            <div class="page-content-container">
                <div class="battery-option-item vertical">
                    <or-translate class="text-secondary bold" value="addBattery.selectBrand"></or-translate>
                    <og-input type=${InputType.SELECT} label=${i18next.t('addBattery.selectBrandPlaceholder')} style="width: 100%;"
                              .options=${[OgBatteryBrand.MYGRID, OgBatteryBrand.LG, OgBatteryBrand.SONNEN_BATTERIE, OgBatteryBrand.TESLA_POWERWALL, OgBatteryBrand.OTHER]}
                              .value=${this._selectedBrand}
                              @or-mwc-input-changed=${this._onBrandSelect}
                    ></og-input>
                </div>
            </div>
        `
    }

    protected _isValid() {
        return !!this._selectedBrand;
    }

    protected _onBrandSelect(ev: OrInputChangedEvent) {
        this._selectedBrand = ev.detail.value;
    }

    protected _onBatteryAddClick(ev: OrInputChangedEvent) {
        if(this._isValid()) {
            const characteristics = this.characteristics || [];
            const batteryInfo = characteristics.find(c => c.id === WellknownCharacteristics.BATTERY);
            if(!batteryInfo) {
                // Create characteristics
                characteristics.push({id: WellknownCharacteristics.BATTERY, shown: true, brand: this._selectedBrand});
            } else {
                // Update characteristics
                batteryInfo.shown = true;
                batteryInfo.brand = this._selectedBrand;
            }
            // Save characteristics
            this._actionElem.label = "addBattery.saveSuccess";
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