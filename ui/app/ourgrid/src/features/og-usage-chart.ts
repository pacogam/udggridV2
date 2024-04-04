import {OrChart} from '@openremote/or-chart';
import {css, html} from 'lit';
import {customElement, property, state} from 'lit/decorators.js';
import manager from '@openremote/core';
import {Asset, AssetDatapointIntervalQueryFormula, AssetDatapointLTTBQuery, Attribute, DatapointInterval, ValueDatapoint} from '@openremote/model';
import {getAppStyle} from '../styles';
import moment from 'moment';
import {getStateColorByPowerValue, OgStateColor} from '../util/util';
import {showSnackbar} from '../components/og-snackbar';
import {i18next} from '@openremote/or-translate';
import { when } from 'lit/directives/when.js';

// Custom ChartJS plugin that fills the background based on district data.
// It fills the canvas with a RED or GREEN stateColor based on the power value.
// Includes multiple fills when the percentage changed over time, and the color should be different.
const backgroundPlugin = {
    id: 'customBackground',
    afterDraw: (chart, _, options) => {
        if (options.data) {
            const {ctx, chartArea, scales: {x}} = chart;
            ctx.save();
            ctx.globalCompositeOperation = 'destination-over';
            const nowPx = x.getPixelForValue(new Date().getTime());
            options.data.forEach((coords, index) => {
                if(coords.y) {
                    const pointPx = x.getPixelForValue(coords.x);
                    let nextPx;
                    if (options.data[index + 1] !== undefined) {
                        nextPx = x.getPixelForValue(options.data[index + 1].x);
                    }
                    let pixelsToNext;
                    if(nextPx !== undefined) {
                        pixelsToNext = ((nextPx < nowPx) ? nextPx : nowPx) - pointPx;
                    } else if(pointPx < nowPx) {
                        pixelsToNext = nowPx - pointPx;
                    }
                    // create rectangle
                    if(pixelsToNext) {
                        ctx.fillStyle = getStateColorByPowerValue(coords.y, options.threshold).toString();
                        ctx.fillRect(pointPx - 1, chartArea.top, pixelsToNext + 2, chartArea.height);
                    }
                }
            });
            ctx.restore();
        }
    }
};
const predictedBackgroundPlugin = {
    id: 'predictedBackground',
    afterDraw: (chart, _, options) => {
        if (!!options.data) {
            const {ctx, chartArea, scales: {x}} = chart;
            ctx.save();
            ctx.globalCompositeOperation = 'destination-over';
            const now = new Date().getTime();
            const nowPx = x.getPixelForValue(now);

            // Remove existing background
            options.images.forEach((img: HTMLImageElement) => {
                img.remove();
            });
            options.images = [];

            // Loop through each datapoint...
            options.data.forEach((coords, index) => {
                const start = coords.x;
                const end = options.data[index + 1]?.x;

                // If timestamp is later than NOW, or it is the current hour
                if(start >= now || end >= now) {
                    coords.y = coords.y || 0; // Y cannot be undefined, make 0 instead.
                    let startPx = x.getPixelForValue(start);
                    const endPx = x.getPixelForValue(end);

                    // If no endPx is present, it is the LAST datapoint. Make graph until the end
                    let width = 0;
                    if(startPx >= nowPx && endPx) {
                        width = endPx - startPx;
                    }
                    // If endPx is not present, since it is the LAST datapoint. Extend background to the end
                    if(!endPx) {
                        width = (chartArea.width + chartArea.left) - startPx;
                    }
                    // Else if startPx is before NOW, only start the background from NOW and onwards.
                    else if(startPx <= nowPx && endPx >= nowPx) {
                        width = endPx - nowPx - 1;
                        startPx = nowPx + 1;
                    }

                    // Start drawing/rendering background
                    const color = getStateColorByPowerValue(coords.y, options.threshold).toString();
                    const img = new Image();
                    if(color === OgStateColor.RED) {
                        img.src = 'images/diagonal-strokes-orange.svg';
                    } else {
                        img.src = 'images/diagonal-strokes-green.svg';
                    }
                    img.onload = () => {
                        ctx.fillStyle = (ctx.createPattern(img, 'repeat') || (color === OgStateColor.RED ? '#faa78d' : '#80d5a2'));
                        ctx.fillRect(startPx, chartArea.top, width, chartArea.height);
                    };
                    options.images.push(img);
                }
            });
            ctx.restore();
        }
    }
};

const styling = css`
  :host {
    height: 100%;
  }

  #chart-wrapper {
    height: 100%;
    display: flex;
    flex-direction: column;
    gap: 12px;
  }

  #chart-title {
    padding: 0 4px;
    display: flex;
    justify-content: space-between;
    align-items: center;
    gap: 24px;
  }
`;

@customElement('og-usage-chart')
export class OgUsageChart extends OrChart {

    @property()
    protected districtAsset: Asset;

    @state() // regular and predicted datapoints of the district asset with power usage %
    protected powerPercentageDatapoints: ValueDatapoint<any>[][] = [];

    @state()
    protected _chart?: any;

    @state()
    protected _maxValue?: number;

    // Config of the chart
    public showLegend = false;
    public attributeControls = false;
    public timestampControls = false;
    public realm = manager.displayRealm;
    public colors = ['#4F2D39'];
    public chartOptions: any = this.getUsageChartOptions();

    public assetAttributes: [number, Attribute<any>][] = [];
    public datapointQuery: AssetDatapointLTTBQuery = {
        type: 'lttb',
        amountOfPoints: 100
    };

    static get styles() {
        return [...super.styles, getAppStyle(), styling];
    }

    // Lifecycle method that returns whether a render should take place or not.
    // For og-usage-chart, we want to prevent full component renders for data updates,
    // by executing other processes.
    shouldUpdate(changedProps: Map<string, any>): boolean {
        let cancelUpdate = false;

        // On chart (re)creation or district datapoints update, (re)apply the background styling
        if ((changedProps.has('_chart') || changedProps.has('powerPercentageDatapoints')) && !!this.powerPercentageDatapoints && !!this._chart) {
            this.applyBackgroundStyling(this._chart, this.powerPercentageDatapoints[0], this.powerPercentageDatapoints[1], this.districtAsset?.attributes['powerImportCriticalPercentage']?.value);
        }

        // If the 'list of assets' (only the meter asset) has been changed, cancel update.
        if(changedProps.has('assets') && changedProps.get('assets') !== undefined) {
            this._data = null;
            this._loadData();
            cancelUpdate = true;
        }

        // Cancel the update when only fields have changed that are irrelevant to the UI.
        if(changedProps.size === 1 && (changedProps.has('_chart') || changedProps.has('powerPercentageDatapoints'))) {
            cancelUpdate = true;
        }

        // Should the component update?
        if(cancelUpdate) {
            return false;
        } else {
            return super.shouldUpdate(changedProps);
        }
    }

    // Lifecycle method that triggers every update, but before rendering.
    willUpdate(changedProps: Map<string, any>) {
        if (changedProps.has('assets') && this.assets.length > 0) {
            this.assetAttributes = [[0, this.assets[0].attributes['power']]];
        }
        if (changedProps.has('_data') && this._data) {
            this.applyDataStyling(this._data);
        }
        return super.willUpdate(changedProps);
    }

    public render() {
        const day = Array.from(this.timePresetOptions)[0][1](new Date())[0];
        const calendarFormat = {
            sameDay: i18next.t('calendar.sameDay'),
            nextDay: i18next.t('calendar.nextDay'),
            lastDay: i18next.t('calendar.lastDay'),
            lastWeek: i18next.t('calendar.lastWeek'),
            sameElse: i18next.t('calendar.sameElse')
        };
        return html`
            <div id="chart-wrapper">
                <div id="chart-title">
                    <span class="text-secondary translucent" style="white-space: nowrap; overflow: hidden; text-overflow: ellipsis;">
                        ${moment(day).calendar(calendarFormat)} - ${moment(day).format('D MMMM YYYY')}
                    </span>
                    ${when(this._maxValue && this._maxValue > 1, () => html`
                        <span class="text-secondary translucent" style="padding-left: 4px; white-space: nowrap;">${`Max ${this._maxValue}W`}</span>
                    `)}
                </div>
                <div style="flex: 1; padding-right: 12px;">
                    ${super.render()}
                </div>
            </div>
        `;
    }


    /* ---------------------------------------- */


    // Extending _loadAttributeData() with getting power percentage datapoints from the district asset,
    // to change the background of the chart later.
    protected async _loadAttributeData(asset: Asset, attribute: Attribute<any>, color: string | undefined, from: number, to: number, predicted: boolean, label: string | undefined): Promise<any> {
        let data;
        try {
            data = super._loadAttributeData(asset, attribute, color, from, to, predicted, label);
        } catch (e) {
            console.error(e);
            showSnackbar(undefined, i18next.t('error.historicalAssetData'));
        }
        if (!predicted && this.districtAsset) {
            const datapoints: ValueDatapoint<any>[][] = [];
            try {
                const response = await manager.rest.api.AssetDatapointResource.getDatapoints(this.districtAsset.id, 'powerImportPercentage', {
                    type: 'interval',
                    interval: '1 hour',
                    formula: AssetDatapointIntervalQueryFormula.AVG,
                    gapFill: true,
                    fromTimestamp: from,
                    toTimestamp: to
                });
                datapoints.push(response.data);
                const responsePredicted = await manager.rest.api.AssetPredictedDatapointResource.getPredictedDatapoints(this.districtAsset.id, 'powerDistrict', {
                    type: 'interval',
                    interval: '1 hour',
                    formula: AssetDatapointIntervalQueryFormula.AVG,
                    gapFill: true,
                    fromTimestamp: from,
                    toTimestamp: to
                });
                const predictedPercentageData: ValueDatapoint<any>[] = responsePredicted.data.map(d => {
                    return {
                        x: d.x,
                        y: d.y / this.districtAsset.attributes['powerImportMax'].value * 100
                    } as ValueDatapoint<any>;
                });
                datapoints.push(predictedPercentageData);
            } catch (e) {
                console.error(e);
                showSnackbar(undefined, i18next.t('error.historicalDistrictData'));
            } finally {
                this.powerPercentageDatapoints = datapoints;
            }
        }
        return data;
    }

    // Override so the interval for 24 hours is four hours.
    protected _getInterval(diffInHours: number): [number, DatapointInterval] {
        if(diffInHours <= 24) {
            return [4, DatapointInterval.HOUR];
        } else {
            return super._getInterval(diffInHours);
        }
    }



    /* ---------------------------------------- */


    // Appending extra styling to the data such as line curve.
    protected applyDataStyling(data: any[]) {
        if (data.length !== 2) {
            console.error('Expected 2 datasets with current and predicted data.');
            return;
        }
        // Styling applied for both datasets
        data.forEach(value => {
            value.tension = 0.4;
            value.pointRadius = 0;
            value.cubicInterpolationMode = 'monotone';
        });
    }

    // Appending background styling according to datapoints of the district asset.
    // It appends the data and threshold to the ChartJS options configuration,
    // so the plugin (see top of file) can render a fill based on the coordinates.
    protected applyBackgroundStyling(chart: any, powerDatapoints: ValueDatapoint<any>[], predictedPoints: ValueDatapoint<any>[], threshold?: number) {
        chart.options.plugins.customBackground.data = powerDatapoints;
        chart.options.plugins.customBackground.threshold = threshold;
        chart.options.plugins.predictedBackground.data = predictedPoints;
        chart.options.plugins.predictedBackground.threshold = threshold;
        chart.update('none');
    }

    public setMin(val: number) {
        this.chartOptions.options.scales.y.min = val;
    }
    public setMax(val: number) {
        this.chartOptions.options.scales.y.max = val;
    }

    protected getUsageChartOptions(): any {
        return {
            options: {
                animation: false,
                plugins: {
                    tooltip: {
                        enabled: false
                    },
                    customBackground: {
                        color: null,
                        threshold: null,
                        data: []
                    },
                    predictedBackground: {
                        color: null,
                        threshold: null,
                        data: [],
                        images: []
                    }
                },
                hover: null,
                scales: {
                    x: {
                        time: {
                            displayFormats: {
                                hour: 'HH:mm'
                            }
                        },
                        grid: {
                            tickBorderDash: [1, 2, 3]
                        }
                    },
                    y: {
                        display: true,
                        grid: {
                            lineWidth: ({ tick }) => tick.value === 0 ? 1 : 0
                        },
                        ticks: {
                            display: false,
                            callback: (value, _index, values) => {
                                this._maxValue = values?.[values.length - 1]?.value;
                                return value;
                            }
                        }
                    }
                }
            },
            plugins: [backgroundPlugin, predictedBackgroundPlugin]
        };
    }
}
