import {css, html, TemplateResult } from 'lit';
import { customElement } from 'lit/decorators.js';
import { when } from 'lit/directives/when.js';
import {OgDataPanel} from '../components/og-data-panel';
import {AssetModelUtil} from '@openremote/model';
import {Util} from '@openremote/core';
import {i18next} from '@openremote/or-translate';
import {ReschoolMeterAsset} from '../util/util';
import { until } from 'lit/directives/until.js';

const styling = css`
    .text-heading {
      line-height: 110%;
      max-width: 80vw;
    }
`;

@customElement('panel-peak-usage')
export class PanelPeakUsage extends OgDataPanel {

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

    protected async getSummaryTemplate(meterAsset?: ReschoolMeterAsset): Promise<TemplateResult> {
        return html`
            <div>
                <span class="text-primary">
                    <or-translate style="display: inline;" value="panel_peakNotification.primaryText"></or-translate>
                    ${when(meterAsset?.attributes, () => {
                        const descriptor = AssetModelUtil.getAttributeDescriptor('challengePowerLimit', meterAsset?.type);
                        const powerGoal = Util.getAttributeValueAsString(meterAsset?.attributes['challengePowerLimit'], descriptor, meterAsset?.type, true, '0');
                        return html`
                            <span class="statistic-medium" style="color: var(--og-color-success); white-space: nowrap;">
                                ${i18next.t('panel_peakNotification.wattToSpare', { 'value': powerGoal })}
                            </span>
                        `;
                    })}
                </span>
            </div>
        `;
    }
}
