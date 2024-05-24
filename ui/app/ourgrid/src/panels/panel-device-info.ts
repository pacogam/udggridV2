import {TemplateResult, html, PropertyValues, css } from 'lit';
import { customElement, property, state} from 'lit/decorators.js';
import {when} from 'lit/directives/when.js';
import {styleMap} from 'lit/directives/style-map.js';
import {Chip} from '../components/og-chips';
import {i18next} from '@openremote/or-translate';
import '../components/og-chips';
import {OgDialog, OgDialogAction, showDialog} from '../components/og-dialog';
import manager from '@openremote/core';
import {router} from '@openremote/or-app';
import {OgDataPanel} from '../components/og-data-panel';

const styling = css`
    #panel-wrapper {
      padding: 16px;
    }
`;

@customElement('panel-device-info')
export class PanelDeviceInfo extends OgDataPanel {

    public transparent = false;
    public rounded = true;

    @property()
    protected static = false;

    @state()
    protected removingState = false;

    static get styles() {
        return [...super.styles, styling];
    }

    protected willUpdate(_changedProps: PropertyValues) {
        super.willUpdate(_changedProps);
        if(!this.meterAsset) {
            this.action = {
                text: i18next.t('panel_deviceInfo.buttonText'),
                action: async () => {
                    console.log('Navigating to setup...');
                    router.navigate('setup');
                    return true;
                }
            };
        } else {
            this.action = null;
        }
    }

    protected async getPanelContent(): Promise<TemplateResult> {
        const iconStyles = {
            'font-size': 'var(--og-font-size-statistic-large)',
            'color': this.meterAsset !== undefined ? 'var(--og-color-success)' : 'var(--og-color-danger)'
        };
        return html`
            <div style="position: relative;">
                <div style="display: flex; align-items: center; gap: 24px;">
                    <div>
                        <or-icon icon="${this.meterAsset ? 'power-plug' : 'power-plug-off'}" style="${styleMap(iconStyles)}"></or-icon>
                    </div>
                    <div>
                        <div style="display: flex; flex-direction: column; justify-content: space-between; gap: 8px;">
                            <span class="text-heading2" style="text-align: start;">${this.meterAsset ? i18next.t('panel_deviceInfo.yourDevice') : i18next.t('panel_deviceInfo.noDeviceFound')}</span>
                            <div style="display: flex; flex-direction: column; gap: 2px;">
                                ${when(this.meterAsset, () => {
                                    const model = this.meterAsset.attributes['smartmeterModel']?.value;
                                    const deviceId = this.meterAsset.attributes['deviceId']?.value;
                                    const version = this.meterAsset.attributes['softwareVersion']?.value;
                                    return html`
                                        <div style="display: flex; align-items: center; gap: 8px;">
                                            <or-icon style="font-size: var(--og-font-size-button-small)" icon="domain"></or-icon>
                                            <span class="text-tertiary" style="text-align: start;">
                                                ${when(model, () => model, () => html`<or-translate value="panel_deviceInfo.unknownModel"></or-translate>`)}
                                            </span>
                                        </div>
                                        <div style="display: flex; align-items: center; gap: 8px;">
                                            <or-icon style="font-size: var(--og-font-size-button-small)" icon="identifier"></or-icon>
                                            <span class="text-tertiary" style="text-align: start;">
                                                ${when(deviceId, () => deviceId, () => html`<or-translate value="panel_deviceInfo.unknownID"></or-translate>`)}
                                            </span>
                                        </div>
                                        <div style="display: flex; align-items: center; gap: 8px;">
                                            <or-icon style="font-size: var(--og-font-size-button-small)" icon="download"></or-icon>
                                            <span class="text-tertiary" style="text-align: start;">
                                                ${when(version, () => html`
                                                    <or-translate value="panel_deviceInfo.version"></or-translate>
                                                    ${version}
                                                `, () => html`
                                                    <or-translate value="panel_deviceInfo.unknownVersion"></or-translate>
                                                `)}
                                            </span>
                                        </div>
                                    `;
                                }, () => html`
                                    <or-translate value="panel_deviceInfo.noDeviceText" style="margin-bottom: 20px;"></or-translate>
                                `)}
                            </div>
                            <div>
                            </div>
                        </div>
                    </div>
                </div>
                ${when(this.meterAsset && !this.static, () => {
                    const chips: Chip[] = [
                        /*{leadingIcon: 'cloud-refresh-outline', text: i18next.t('panel_deviceInfo.refreshData'), action: () => this.onReloadClick()},*/
                        {leadingIcon: 'delete-outline', text: html`<or-translate value="remove"></or-translate>`, loading: this.removingState, action: () => this.onRemoveClick()}
                    ];
                    return html`
                        <div style="display: flex; justify-content: end; margin: 12px -12px -12px -12px;">
                            <og-chips .chips="${chips}" .outlined="${true}"></og-chips>
                        </div>
                    `;
                })}
            </div>
        `;
    }

    protected onReloadClick() {
        this.reloadDevice(this.meterAsset.name);
    }

    protected reloadDevice(_deviceName: string) {
        this.dispatchEvent(new CustomEvent('reload'));
    }

    protected onRemoveClick() {
        this.promptDeviceDelete();
    }

    protected promptDeviceDelete() {
        const dialogActions: OgDialogAction[] = [
            {
                actionName: 'cancel',
                content: i18next.t('cancel')
            },
            {
                default: true,
                actionName: 'ok',
                content: i18next.t('remove'),
                action: () => {
                    this.removeDevice(this.meterAsset.name);
                }
            }
        ];
        showDialog(new OgDialog()
            .setHeading('areYouSure')
            .setDismissAction(null)
            .setContent(html`
                ${i18next.t('panel_deviceInfo.removePromptText')}
            `)
            .setActions(dialogActions) as OgDialog
        );
    }

    protected async removeDevice(deviceName: string): Promise<void> {
        this.removingState = true;
        await manager.rest.api.DeviceResource.removeDevice({ deviceName: deviceName });
        await new Promise(resolve => setTimeout(resolve, 500));
        this.removingState = false;
        this.dispatchEvent(new CustomEvent('remove'));
    }
}
