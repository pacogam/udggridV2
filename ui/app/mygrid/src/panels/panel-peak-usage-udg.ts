import {css, html, TemplateResult } from 'lit';
import { customElement } from 'lit/decorators.js';
import { when } from 'lit/directives/when.js';
import {OgDataPanel} from '../components/og-data-panel';
import {AssetModelUtil} from '@openremote/model';
import {Util} from '@openremote/core';
import {i18next} from '@openremote/or-translate';
import {OurgridMeterAsset} from '../util/util';
import {ExitChallengeAsset} from '../util/util';
import { until } from 'lit/directives/until.js';
import moment from 'moment';

const styling = css`
    .text-heading {
      line-height: 110%;
      max-width: 80vw;
    }
`;

@customElement('panel-peak-usage-udg')
export class PanelPeakUsageUdg extends OgDataPanel {

    public heading = html`<or-translate value="panel_peakUsage.heading"></or-translate>`;
    public dark = true;

    static get styles() {
        return [...super.styles, styling];
    }

    protected async getPanelContent(): Promise<TemplateResult> {
        return html`
            <div style="display: flex; flex-direction: column; gap: 24px;">
                ${until(this.getSummaryTemplate(this.meterAsset))}
            </div>
        `;
    }

    protected async getSummaryTemplate(meterAsset?: OurgridMeterAsset, challengeAsset?: ExitChallengeAsset): Promise<TemplateResult> {
        return html`
            <div>
                <span class="text-primary">
                    ${when(meterAsset?.attributes, () => {
                        //const descriptor = AssetModelUtil.getAttributeDescriptor('challengePowerLimit', meterAsset?.type);
                        //const powerGoal = Util.getAttributeValueAsString(meterAsset?.attributes['challengePowerLimit'], descriptor, meterAsset?.type, true, '0');
                        //const startTime = '19:00' //this.notificationAsset?.attributes['startTime']?.value;
                        //const endTime   = '20:00' //this.notificationAsset?.attributes['endTime']?.value;
                        const startString = challengeAsset.attributes['challengeStart']?.value;
                        const startTime = startString ? moment(startString).format('HH:mm') : '???';
                        const endString = challengeAsset.attributes['challengeEnd']?.value;
                        const endTime = endString ? moment(endString).format('HH:mm') : '???';
                        return html`
                            <span class="statistic-medium" style="color: var(--og-color-success); white-space: nowrap;">
                                ${i18next.t('panel_peakNotification_udg.reduceConsumption1')}
                            </span>
                            <span class="statistic-medium" style="color: var(--og-color-success); white-space: nowrap;">
                                ${i18next.t('panel_peakNotification_udg.reduceConsumption2', { start: startTime, end: endTime })}
                            </span>                            
                        `;
                    })}
                </span>
            </div>
        `;
    }
}
