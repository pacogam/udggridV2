import {css, html, LitElement, PropertyValues, TemplateResult } from 'lit';
import { customElement, property, state } from 'lit/decorators.js';
import {OgBatteryBrand, OgHeatPumpBrand, OgVehicleBrand} from '../util/util';
import {OgInputButtonGroupOption, OgSpecialInputType} from '../components/og-input';
import {i18next} from '@openremote/or-translate';
import {DeviceCharacteristic, WellknownCharacteristics} from '@openremote/model';
import { InputType } from '@openremote/or-mwc-components/or-mwc-input';
import {getAppStyle} from "../styles";

export interface OgCharacteristicsUpdateEventDetail {
    characteristics: DeviceCharacteristic[],
    valid: boolean
}

export class OgCharacteristicsUpdateEvent extends CustomEvent<OgCharacteristicsUpdateEventDetail> {

    public static readonly NAME = 'characteristics-changed';

    constructor(detail: OgCharacteristicsUpdateEventDetail) {
        super(OgCharacteristicsUpdateEvent.NAME, {
            bubbles: true,
            composed: true,
            detail
        });
    }
}

const styling = css`
  .characteristic-wrapper {
    width: 100%;
  }

  .characteristic-container {
    display: flex;
    flex-direction: column;
    gap: 36px;
  }

  .characteristic-item-group {
    display: flex;
    flex-direction: column;
    gap: 24px;
  }

  .characteristic-item {
    display: flex;
    justify-content: space-between;
    align-items: center;
    gap: 9px;
  }
    
  .characteristic-item > span {
    overflow: hidden;
    text-align: start;
  }
`;

@customElement('og-characteristics-settings')
export class OgCharacteristicsSettings extends LitElement {

    @property()
    public characteristics: DeviceCharacteristic[];

    @state()
    public solarState: number = undefined; // undefined, 0 = NO, 1 = YES.

    @state()
    public electricCarState: number = undefined; // undefined, 0 = NO, 1 = YES.

    @state()
    public electricCarBrand: OgVehicleBrand = undefined;

    @state()
    public vehicleChargerState: number = undefined; // undefined, 0 = NO, 1 = YES.

    /*@state()
    public vehicleChargerBrand: OgVehicleChargerBrand = undefined;*/

    @state()
    public heatPumpState: number = undefined; // undefined, 0 = NO, 1 = YES.

    @state()
    public heatPumpBrand: OgHeatPumpBrand = undefined;

    @state()
    public batteryState: number = undefined; // undefined, 0 = NO, 1 = YES.

    @state()
    public batteryBrand: OgBatteryBrand = undefined;

    protected options: OgInputButtonGroupOption[] = [
        {icon: 'close', iconColors: {active: 'var(--og-color-primary)', inactive: 'var(--og-color-warning)'}, borderColors: {active: 'var(--og-color-warning)', inactive: 'var(--og-color-warning)'}, fillColors: {active: 'var(--og-color-warning)'}},
        {icon: 'check', iconColors: {active: 'var(--og-color-primary)', inactive: 'var(--og-color-success)'}, borderColors: {active: 'var(--og-color-success)', inactive: 'var(--og-color-success)'}, fillColors: {active: 'var(--og-color-success)'}}
    ];

    protected willUpdate(changedProps: PropertyValues) {
        super.willUpdate(changedProps);
        if(changedProps.has('characteristics') && this.characteristics) {
            const solar = this.characteristics.find(c => c.id === WellknownCharacteristics.SOLAR_PANELS);
            if(solar) {
                this.solarState = solar.shown ? 1 : 0;
            }
            const electricCar = this.characteristics.find(c => c.id === WellknownCharacteristics.ELECTRIC_VEHICLE);
            if(electricCar) {
                this.electricCarState = electricCar.shown ? 1 : 0;
                this.electricCarBrand = electricCar.brand as OgVehicleBrand | undefined;
            }
            const vehicleCharger = this.characteristics.find(c => c.id === WellknownCharacteristics.VEHICLE_CHARGER);
            if(vehicleCharger) {
                this.vehicleChargerState = vehicleCharger.shown ? 1 : 0;
                /*this.vehicleChargerBrand = vehicleCharger.brand as OgVehicleChargerBrand | undefined;*/
            }
            const heatPump = this.characteristics.find(c => c.id === WellknownCharacteristics.HEAT_PUMP);
            if(heatPump) {
                this.heatPumpState = heatPump.shown ? 1 : 0;
                this.heatPumpBrand = heatPump.brand as OgHeatPumpBrand | undefined;
            }
        }

        if(!changedProps.has('characteristics') || changedProps.size > 1) {
            this.dispatchEvent(new OgCharacteristicsUpdateEvent({
                characteristics: [
                    { id: WellknownCharacteristics.SOLAR_PANELS, shown: this.solarState === 1 },
                    { id: WellknownCharacteristics.ELECTRIC_VEHICLE, brand: this.electricCarBrand, shown: this.electricCarState === 1 },
                    { id: WellknownCharacteristics.VEHICLE_CHARGER, /*brand: this.vehicleChargerBrand,*/ shown: this.vehicleChargerState === 1 },
                    { id: WellknownCharacteristics.HEAT_PUMP, brand: this.heatPumpBrand, shown: this.heatPumpState === 1 },
                    { id: WellknownCharacteristics.BATTERY, brand: this.batteryBrand, shown: this.batteryState === 1 }
                ],
                valid: this.isFormValid()
            }));
        }
    }

    protected isFormValid(): boolean {
        return (this.solarState !== undefined
            && this.electricCarState !== undefined
            && (this.electricCarState === 0 || this.electricCarBrand !== undefined)
            && this.vehicleChargerState !== undefined
            && this.heatPumpState !== undefined
            && (this.heatPumpState === 0 || this.heatPumpBrand !== undefined)
            && this.batteryState !== undefined
            && (this.batteryState === 0 || this.batteryBrand !== undefined)
        );
    }

    static get styles(): any[] {
        return [getAppStyle(), styling];
    }

    protected render(): TemplateResult {
        return html`
            <div class="characteristic-wrapper">
                <div class="characteristic-container">

                    <!-- Solar panels -->
                    <div class="characteristic-item">
                        <span class="text-secondary bold"><or-translate value="panel_characteristics.question_solarPanels"/></span>
                        <og-input .type=${OgSpecialInputType.BUTTON_GROUP} .value="${this.solarState}" .options="${this.options}"
                                  @or-mwc-input-changed="${ev => this.onSolarUpdate(ev)}"></og-input>
                    </div>

                    <!-- Electric vehicle -->
                    <div class="characteristic-item-group">
                        <div class="characteristic-item">
                            <span class="text-secondary bold"><or-translate value="panel_characteristics.question_electricVehicle"/></span>
                            <og-input .type=${OgSpecialInputType.BUTTON_GROUP} .value="${this.electricCarState}" .options="${this.options}"
                                      @or-mwc-input-changed="${ev => this.onElectricCarUpdate(ev)}"></og-input>
                        </div>
                        <div class="characteristic-item">
                            <og-input .type="${InputType.SELECT}" comfortable ?disabled="${this.electricCarState === 0}" style="width: 100%;"
                                      label="${this.electricCarState === 0 ? i18next.t('panel_characteristics.notApplicable') : i18next.t('panel_characteristics.select_electricVehicleBrand')}"
                                      .options="${[OgVehicleBrand.TESLA, OgVehicleBrand.VOLKSWAGEN_ID, OgVehicleBrand.OTHER]}" .value="${this.electricCarBrand}"
                                      @or-mwc-input-changed="${ev => this.onElectricCarBrandUpdate(ev)}"
                            ></og-input>
                        </div>
                    </div>

                    <!-- Charging station -->
                    <div class="characteristic-item-group">
                        <div class="characteristic-item">
                            <span class="text-secondary bold"><or-translate value="panel_characteristics.question_vehicleCharger"/></span>
                            <og-input .type=${OgSpecialInputType.BUTTON_GROUP} .value="${this.vehicleChargerState}" .options="${this.options}"
                                      @or-mwc-input-changed="${ev => this.onVehicleChargerUpdate(ev)}"></og-input>
                        </div>
                    </div>

                    <!-- Heat pump -->
                    <div class="characteristic-item-group">
                        <div class="characteristic-item">
                            <span class="text-secondary bold"><or-translate value="panel_characteristics.question_heatPump"/></span>
                            <og-input .type=${OgSpecialInputType.BUTTON_GROUP} .value="${this.heatPumpState}" .options="${this.options}"
                                      @or-mwc-input-changed="${ev => this.onHeatPumpUpdate(ev)}"></og-input>
                        </div>
                        <div class="characteristic-item">
                            <og-input .type="${InputType.SELECT}" comfortable ?disabled="${this.heatPumpState === 0}" style="width: 100%; position: relative; display: block;"
                                      label="${this.heatPumpState === 0 ? i18next.t('panel_characteristics.notApplicable') : i18next.t('panel_characteristics.select_heatPumpBrand')}"
                                      .options="${[OgHeatPumpBrand.RESIDEO_HONEYWELL, OgHeatPumpBrand.TOON, OgHeatPumpBrand.NEST, OgHeatPumpBrand.OTHER]}" .value="${this.heatPumpBrand}"
                                      @or-mwc-input-changed="${ev => this.onHeatPumpBrandUpdate(ev)}"
                            ></og-input>
                        </div>
                    </div>
                    
                    <!-- Battery -->
                    <div class="characteristic-item-group">
                        <div class="characteristic-item">
                            <span class="text-secondary bold"><or-translate value="panel_characteristics.question_battery"/></span>
                            <og-input .type=${OgSpecialInputType.BUTTON_GROUP} .value="${this.batteryState}" .options="${this.options}"
                                      @or-mwc-input-changed="${ev => this.onBatteryUpdate(ev)}"></og-input>
                        </div>
                        <div class="characteristic-item">
                            <og-input .type="${InputType.SELECT}" comfortable ?disabled="${this.batteryState === 0}" style="width: 100%; position: relative; display: block;"
                                      label="${this.batteryState === 0 ? i18next.t('panel_characteristics.notApplicable') : i18next.t('panel_characteristics.select_batteryBrand')}"
                                      .options="${[OgBatteryBrand.MYGRID, OgBatteryBrand.LG, OgBatteryBrand.SONNEN_BATTERIE, OgBatteryBrand.TESLA_POWERWALL, OgBatteryBrand.OTHER]}" .value="${this.batteryBrand}"
                                      @or-mwc-input-changed="${ev => this.onBatteryBrandUpdate(ev)}"
                            ></og-input>
                        </div>
                    </div>
                    
                    <div></div>
                    
                </div>
            </div>
        `;
    }

    protected onSolarUpdate(ev: CustomEvent) {
        this.solarState = ev.detail.value;
    }

    protected onElectricCarUpdate(ev: CustomEvent) {
        if (ev.detail.value === 0) {
            this.electricCarBrand = undefined;
        }
        this.electricCarState = ev.detail.value;
    }

    protected onElectricCarBrandUpdate(ev: CustomEvent) {
        if (this.electricCarState === undefined) {
            this.electricCarState = 1;
        }
        this.electricCarBrand = ev.detail.value as OgVehicleBrand;
    }

    protected onVehicleChargerUpdate(ev: CustomEvent) {
        /*if (ev.detail.value === 0) {
            this.vehicleChargerBrand = undefined;
        }*/
        this.vehicleChargerState = ev.detail.value;
    }

    /*protected onVehicleChargerBrandUpdate(ev: CustomEvent) {
        if (this.vehicleChargerState === undefined) {
            this.vehicleChargerState = 1;
        }
        this.vehicleChargerBrand = ev.detail.value as OgVehicleChargerBrand;
    }*/

    protected onHeatPumpUpdate(ev: CustomEvent) {
        if (ev.detail.value === 0) {
            this.heatPumpBrand = undefined;
        }
        this.heatPumpState = ev.detail.value;
    }

    protected onHeatPumpBrandUpdate(ev: CustomEvent) {
        if (this.heatPumpState === undefined) {
            this.heatPumpState = 1;
        }
        this.heatPumpBrand = ev.detail.value as OgHeatPumpBrand;
    }

    protected onBatteryUpdate(ev: CustomEvent) {
        if (ev.detail.value === 0) {
            this.batteryBrand = undefined;
        }
        this.batteryState = ev.detail.value;
    }

    protected onBatteryBrandUpdate(ev: CustomEvent) {
        if (this.batteryState === undefined) {
            this.batteryState = 1;
        }
        this.batteryBrand = ev.detail.value as OgBatteryBrand;
    }

}
