import {TemplateResult, html, css, PropertyValues} from 'lit';
import {customElement, property, queryAll} from 'lit/decorators.js';
import {getAppStyle} from '../styles';
import {TimePresetCallback} from '@openremote/or-chart';
import {map} from 'lit/directives/map.js';
import moment from 'moment';
import '../features/og-usage-chart';
import '../components/og-swipable';
import {guard} from 'lit/directives/guard.js';
import {until} from 'lit/directives/until.js';
import {OgDataPanel} from '../components/og-data-panel';
import manager, {Util} from "@openremote/core";
import {Constants} from "../util/constants";
import {AssetDatapointIntervalQueryFormula} from "@openremote/model";
import {OgUsageChart} from "../features/og-usage-chart";

const styling = css`
  og-usage-chart {
    min-height: 250px;
  }
`;

@customElement('panel-usage-history')
export class PanelUsageHistory extends OgDataPanel {

    public heading = html`<or-translate value="panel_usageHistory.heading"></or-translate>`;
    public subtitle = html`<or-translate value="panel_usageHistory.subtitle"></or-translate>`;
    public fullWidth = true;

    protected options: Map<string, TimePresetCallback>[];
    protected lazyLoadDeferreds: Map<number, Util.Deferred<any>> = new Map();
    protected currentMin: number;
    protected currentMax: number;

    @queryAll('og-usage-chart')
    protected chartElems: NodeList;

    static get styles() {
        return [...super.styles, getAppStyle(), styling];
    }

    public connectedCallback() {
        super.connectedCallback();
        this.options = [
            new Map([['7daysago', _date => [moment(new Date()).subtract(7, 'day').startOf('day').toDate(), moment(new Date()).subtract(7, 'day').endOf('day').toDate()]]]),
            new Map([['6daysago', _date => [moment(new Date()).subtract(6, 'day').startOf('day').toDate(), moment(new Date()).subtract(6, 'day').endOf('day').toDate()]]]),
            new Map([['5daysago', _date => [moment(new Date()).subtract(5, 'day').startOf('day').toDate(), moment(new Date()).subtract(5, 'day').endOf('day').toDate()]]]),
            new Map([['4daysago', _date => [moment(new Date()).subtract(4, 'day').startOf('day').toDate(), moment(new Date()).subtract(4, 'day').endOf('day').toDate()]]]),
            new Map([['3daysago', _date => [moment(new Date()).subtract(3, 'day').startOf('day').toDate(), moment(new Date()).subtract(3, 'day').endOf('day').toDate()]]]),
            new Map([['2daysago', _date => [moment(new Date()).subtract(2, 'day').startOf('day').toDate(), moment(new Date()).subtract(2, 'day').endOf('day').toDate()]]]),
            new Map([['yesterday', _date => [moment(new Date()).subtract(1, 'day').startOf('day').toDate(), moment(new Date()).subtract(1, 'day').endOf('day').toDate()]]]),
            new Map([['today', _date => [moment(new Date()).startOf('day').toDate(), moment(new Date()).endOf('day').toDate()]]]),
            new Map([['tomorrow', _date => [moment(new Date()).add(1, 'day').startOf('day').toDate(), moment(new Date()).add(1, 'day').endOf('day').toDate()]]])
        ];
    }

    protected firstUpdated(changedProps: PropertyValues) {
        super.firstUpdated(changedProps);

        // Update min/max values based on first and last date;
        if(this.options) {
            const startDate = Array.from(this.options[0])[0][1](undefined)[0];
            const endDate = Array.from(this.options[this.options.length - 1])[0][1](undefined)[1];
            this.tryUpdateChartValues(this.meterAsset.id, Constants.METER_POWER_ATTRIBUTE, [startDate, endDate]);
        }
    }

    protected async getPanelContent(): Promise<TemplateResult> {
        const nowMs = new Date().getTime();
        const selected = 7;
        return html`
            <div>
                <!-- Swipe container of all charts; where 'today' is selected first. -->
                <og-swipable .dots="${false}" .arrows="${true}" .selected="${selected}" .size="${this.options.length}" @slide="${this._onSlide}">
                    ${map(this.options, (option, index) => {
                        const startEndValue = Array.from(option)[0][1](undefined);
                        const isSameDay = nowMs >= startEndValue[0].getTime() && nowMs <= startEndValue[1].getTime();

                        // Render chart, and put key in place to prevent unnecessary rerendering.
                        // Only the chart from the same day needs rerendering, so we give it a unique key each time. (current milliseconds in this case)
                        // Other days use the array index, so they will stay static and won't be rerendered over time.
                        const key = isSameDay ? nowMs : index;
                        return html`
                            ${guard([key], () => until(this.getChartContent(index, selected, option), html`<og-loading slot="${index}"></og-loading>`))}
                        `;
                    })}
                </og-swipable>
            </div>
        `;
    }

    /**
     * HTML event callback of {@link OgSwipable} when a user 'swipes' from slide to a different slide.
     */
    protected _onSlide(ev: CustomEvent) {
        const selected = ev.detail.value as number;

        // If lazy loading, resolve the waiting deferreds of the previous, current and next slide
        this.lazyLoadDeferreds.get(selected - 1)?.resolve(null);
        this.lazyLoadDeferreds.get(selected)?.resolve(null);
        this.lazyLoadDeferreds.get(selected + 1)?.resolve(null);
    }

    /**
     * Returns a {@link Promise} of {@link TemplateResult}, containing the UI for the chart.
     * If {@link lazyLoad} is set to true, it will initially wait with rendering, until the user can (almost) see the slide.
     */
    protected async getChartContent(index: number, selected = 0, options: Map<String, TimePresetCallback>, lazyLoad = true): Promise<TemplateResult> {
        if(lazyLoad) {
            // Create deferred if not done yet
            if(!this.lazyLoadDeferreds.has(index)) {
                this.lazyLoadDeferreds.set(index, new Util.Deferred<any>());
            }
            // If selected (or next to the selected slide), directly load it by resolving the promise.
            if(selected === index || (selected + 1 === index) || (selected - 1 === index)) {
                this.lazyLoadDeferreds.get(index).resolve(null);
            }
            // Await deferred before loading UI (aka lazy loading)
            await this.lazyLoadDeferreds.get(index)?.promise;
        }

        // Return chart HTML
        return html`
            <og-usage-chart slot="${index}" .timePresetOptions="${options}" style="pointer-events: none;"
                            .assets="${this.meterAsset ? [this.meterAsset] : []}" .districtAsset="${this.districtAsset}"
            ></og-usage-chart>
        `;
    }

    protected tryUpdateChartValues(assetId: string, attributeName: string, startEndValue: [Date, Date]) {
        console.log("Checking if updating min/max value of charts is necessary.");
        const timestamps = startEndValue.map(d => d.getTime())
        const diff = timestamps[1] - timestamps[0];

        // Query max value
        manager.rest.api.AssetDatapointResource.getDatapoints(assetId, attributeName, {
            type: "interval",
            fromTimestamp: timestamps[0],
            toTimestamp: timestamps[1],
            gapFill: true,
            interval: `${diff * 1000} seconds`,
            formula: AssetDatapointIntervalQueryFormula.MAX,
        }).then(result => {
            const validPoint = result.data.find(d => d.y !== undefined && (this.currentMax === undefined || d.y > this.currentMax));
            if (validPoint) {
                this.updateChartsMaxValue(Math.ceil(validPoint.y / 100) * 100);
            }
        })

        // Query min value
        manager.rest.api.AssetDatapointResource.getDatapoints(assetId, attributeName, {
            type: "interval",
            fromTimestamp: timestamps[0],
            toTimestamp: timestamps[1],
            gapFill: true,
            interval: `${diff * 1000} seconds`,
            formula: AssetDatapointIntervalQueryFormula.MIN,
        }).then(result => {
            const validPoint = result.data.find(d => d.y !== undefined && (this.currentMin === undefined || d.y < this.currentMin));
            if (validPoint) {
                this.updateChartsMinValue(Math.floor(validPoint.y / 100) * 100);
            }
        })
    }

    protected updateChartsMaxValue(val: number) {
        this.updateComplete.then(() => {
            console.log(`Updating max value to ${val}`);
            this.currentMax = val;
            this.chartElems.forEach(node => {
                (node as OgUsageChart).setMax(val);
            });
        })
    }

    protected updateChartsMinValue(val: number) {
        this.updateComplete.then(() => {
            console.log(`Updating min value to ${val}`);
            this.currentMin = val;
            this.chartElems.forEach(node => {
                (node as OgUsageChart).setMin(val);
            });
        })
    }
}
