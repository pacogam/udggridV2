import {OgPage, OgPageProvider, PageAnimationType} from "../util/og-page";
import {InputType, OrInputChangedEvent, OrMwcInput} from "@openremote/or-mwc-components/or-mwc-input";
import {i18next} from "@openremote/or-translate";
import {GridAppStateKeyed} from "../../util/og-state";
import {Store} from '@reduxjs/toolkit';
import { customElement, query, state } from "lit/decorators.js";
import {css, html, TemplateResult } from "lit";
import {OgHeatPumpBrand, OurgridMeterAsset} from "../../util/util";
import {DeviceCharacteristic, WellknownCharacteristics} from "model";
import { until } from "lit/directives/until.js";
import {GraphicType} from "../../features/og-usage-graphic";
import { router } from "@openremote/or-app";
import rest from "rest";

export function pageAddHeatpumpProvider(store: Store<GridAppStateKeyed>): OgPageProvider<GridAppStateKeyed> {
    return {
        name: 'add-heatpump',
        routes: ['add-heatpump'],
        pageCreator: () => new PageAddHeatpump(store),
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
        -webkit-mask-image: url("images/house-temperature.svg");
        -webkit-mask-repeat: no-repeat;
        -webkit-mask-position: center;
        -webkit-mask-size: auto calc(var(--og-font-size-title) * 1.5);
        mask-image: url("images/house-temperature.svg");
        mask-repeat: no-repeat;
        mask-position: center;
        mask-size: auto calc(var(--og-font-size-title) * 1.5);
    }
    
    .heatpump-option-item {
        display: flex;
        gap: 8px;
    }
    
    .heatpump-option-item.vertical {
        flex-direction: column;
    }
    
    .heatpump-option-item.horizontal {
        justify-content: space-between;
        align-items: center;
    }

    .page-action {
        width: calc(100% - 32px);
        padding: 16px 32px;
    }
`;

@customElement("page-add-heatpump")
export class PageAddHeatpump extends OgPage<GridAppStateKeyed> {

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
    protected _selectedBrand?: OgHeatPumpBrand;

    @state()
    protected userAsset?: OurgridMeterAsset;

    @state()
    protected characteristics?: DeviceCharacteristic[];

    get name(): string {
        return "add-heatpump";
    }

    static get styles() {
        return [...super.styles, styling];
    }

    stateChanged(state: GridAppStateKeyed): void {
        this.userAsset = state.gridApp.assets.find(a => a.id === state.gridApp.userAssetId);
        const val = this.userAsset?.attributes?.[PageAddHeatpump.HOUSEHOLD_CHARACTERISTICS_ATTRIBUTE]?.value;
        const characteristics = val ? JSON.parse(val) as DeviceCharacteristic[] : undefined;
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
                <or-translate class="page-title text-heading" value="addHeatpump.intro"></or-translate>
                <div class="page-content">
                    ${until(this._getHeatpumpFormTemplate(), html`Loading...`)}
                </div>
                <og-input class="page-action" .type=${InputType.BUTTON} raised rounded fullWidth label="addHeatpump.addDevice"
                          ?disabled=${!this._isValid()}
                          @or-mwc-input-changed=${this._onHeatpumpAddClick}
                ></og-input>
            </div>
        `;
    }

    protected async _getHeatpumpFormTemplate(): Promise<TemplateResult> {
        return html`
            <div class="page-content-container">
                <div class="heatpump-option-item vertical">
                    <or-translate class="text-secondary bold" value="addHeatpump.selectBrand"></or-translate>
                    <og-input type=${InputType.SELECT} label=${i18next.t('addHeatpump.selectBrandPlaceholder')} style="width: 100%;"
                              .options=${[OgHeatPumpBrand.RESIDEO_HONEYWELL, OgHeatPumpBrand.TOON, OgHeatPumpBrand.NEST, OgHeatPumpBrand.OTHER]}
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

    protected _onHeatpumpAddClick(ev: OrInputChangedEvent) {
        if(this._isValid()) {
            const characteristics = this.characteristics || [];
            const heatpumpInfo = characteristics.find(c => c.id === WellknownCharacteristics.HEAT_PUMP);
            if(!heatpumpInfo) {
                // Create characteristics
                characteristics.push({id: WellknownCharacteristics.HEAT_PUMP, shown: true, brand: this._selectedBrand})
            } else {
                // Update characteristics
                heatpumpInfo.shown = true;
                heatpumpInfo.brand = this._selectedBrand;
            }

            // Save characteristics
            this._actionElem.label = "addHeatpump.saveSuccess";
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