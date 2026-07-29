import {TemplateResult, html, css, nothing, PropertyValues} from 'lit';
import {customElement, property} from 'lit/decorators.js';
import './../features/og-usage-graphic';
import './../features/og-challenge-timer';
import './../components/og-statistic';
import {Asset} from '@openremote/model';
import {when} from 'lit/directives/when.js';
import {getStateColorByDistrictPowerUsage, getStateColorByPowerValue, OgMeterChallengeState, OgStateColor} from '../util/util';
import {classMap} from 'lit/directives/class-map.js';
import {styleMap} from 'lit/directives/style-map.js';
import {OgDataPanel} from '../components/og-data-panel';

const styling = css`
  :host {
    --or-icon-width: 32px;
    --or-icon-height: 32px;
  }

  #graphic-container {
    height: 100%;
    display: grid;
    grid-template-columns: 1fr;
    align-items: center;
    padding-bottom: 6px;
  }

  .statistic-wrapper {
    position: absolute; /* <- esto es clave para que flote encima del gráfico */
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    z-index: 5;
    display: flex;
    align-items: center;
    justify-content: center;
    pointer-events: none; /* permite clics a través si hace falta */
  }

  #statistic-container {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 4px;
    border-radius: 50%;
    margin-top: -6px;
    width: 35vw;
    height: 35vw;
    min-width: 180px;
    min-height: 180px;
    max-width: 250px;
    max-height: 250px;
  }

  .text-heading {
    font-size: 42px;
    transition: color 0.2s;
    -webkit-transition: color 0.2s;
  }

  .line-height {
    line-height: 125%;
  }
`;

const PLACEHOLDER_VALUE = 462;

@customElement('panel-usage-overview')
export class PanelUsageOverview extends OgDataPanel {

    public fullWidth = true;
    public fullHeight = true;

    @property()
    protected aspectRatio = '1/1';

    @property()
    protected staticAnimation = false;

    static get styles() {
        return [...super.styles, styling];
    }

    protected willUpdate(changedProps: PropertyValues) {
        const dark = this.challengeAsset?.attributes ? this.meterAsset?.attributes['challengeStatus']?.value === OgMeterChallengeState.ACTIVE_CHALLENGE : false;
        this.dark = dark;
        this.transparent = !dark;
        return super.willUpdate(changedProps);
    }

    protected async getPanelContent(): Promise<TemplateResult> {
        const attributes = this.meterAsset?.attributes;
        const districtAttributes = this.districtAsset?.attributes;
        const challengeAttributes = this.challengeAsset?.attributes;
        const graphicColor = districtAttributes ? getStateColorByDistrictPowerUsage(districtAttributes) : undefined;
        const isInChallenge = challengeAttributes ? this.meterAsset?.attributes['challengeStatus']?.value === OgMeterChallengeState.ACTIVE_CHALLENGE : false;
        const meterPower: number | undefined = attributes ? attributes['power']?.value : undefined;
        const meterPowerMax: number | undefined = isInChallenge && attributes ? attributes['challengePowerLimit']?.value : undefined;
        const meterPVPower: number | undefined = attributes ? attributes['pvpower']?.value : undefined;
        const meterPVPowerMax: number | undefined = undefined; // ajusta si hay límite
        const statisticColor = isInChallenge ? getStateColorByPowerValue(meterPower, meterPowerMax) : undefined;
        const statisticColorPV = isInChallenge ? getStateColorByPowerValue(meterPVPower, meterPVPowerMax) : undefined;
        this.updateComplete.then(() => this.dark = isInChallenge);
        return html`
            <div id="graphic-container">
                <!-- Primer gráfico + círculo de estadística -->
                <div style="position: relative; height: 350px; overflow: hidden;">
                <div class="statistic-wrapper">
                    ${when(isInChallenge, () => this.getTimerHTML(this.meterAsset, this.challengeAsset))}
                    ${this.getStatisticHTML(meterPower, meterPowerMax, statisticColor, isInChallenge)}
                </div>
                <og-usage-graphic .fill="${true}" .dark="${isInChallenge}" .color="${graphicColor}" .colorAnimation="${this.staticAnimation}"></og-usage-graphic>
                </div>

                <!-- Segundo gráfico + otro círculo con otros datos -->
                <div style="position: relative; height: 350px; overflow: hidden;">
                <div class="statistic-wrapper">
                    ${this.getStatisticHTML(meterPVPower, meterPVPowerMax, statisticColorPV, isInChallenge, 'solar-power')}
                </div>
                <og-usage-graphic .fill="${true}" .dark="${isInChallenge}" .color="${OgStateColor.GREEN}" .colorAnimation="${this.staticAnimation}"></og-usage-graphic>
                </div>
            </div>
        `;
    }

    protected getTimerHTML(userAsset?: Asset, challengeAsset?: Asset): TemplateResult {
        return html`
            <og-challenge-timer .userAsset="${userAsset}" .challengeAsset="${challengeAsset}" style="position: absolute;"></og-challenge-timer>
        `;
    }

    protected getStatisticHTML(power?: number, maxPower?: number, color?: OgStateColor, dark = false, icon = 'lightning-bolt'): TemplateResult {
        const customColor: string = color || (dark ? 'var(--og-color-primary)' : undefined);
        const headingClasses: {} = {
            'text-heading': true,
            'dark': dark
        };
        const iconStyles: {} = {
            '--or-icon-fill': customColor
        };
        return html`
            <div id="statistic-container" style="background: ${dark ? 'var(--og-color-primary-dark)' : 'var(--og-color-primary)'}; ${dark ? '' : 'border:solid 8px var(--og-background-shade);'}">
                <or-icon icon=${icon} style="${styleMap(iconStyles)}"></or-icon>
                ${when(power, () => html`
                    <og-statistic .value="${Math.round(power)}">
                        <span class="${classMap(headingClasses)} line-height" style="color: ${customColor || nothing}" />
                    </og-statistic>
                `, () => html`
                    <span class="${classMap(headingClasses)}" style="color: ${customColor || nothing}">${this.staticAnimation ? PLACEHOLDER_VALUE : '-'}</span>
                `)}
                <span class="bg text-tertiary medium" style="color: ${customColor || nothing}">Watt</span>
                ${when(maxPower, () => html`
                    <span class="bg text-secondary bold" style="color: var(--og-color-primary)">${Math.round(maxPower)}W Max</span>
                `)}
            </div>
        `;
    }
}
