import {css, html, TemplateResult} from 'lit';
import {customElement, property} from 'lit/decorators.js';
import {OgPanel} from '../components/og-panel';
import {Asset} from '@openremote/model';

type NotificationLevel = 'info' | 'warning' | 'error';

@customElement('panel-notification')
export class PanelNotification extends OgPanel {

  @property({type: Object})
  notificationAsset?: Asset;

  public heading = html`Avis`;
  public dark = false;

  static get styles() {
    return [
      ...super.styles,
      css`
        .box {
          padding: 12px 16px;
          border-radius: 12px;
          display: flex;
          justify-content: space-between;
          align-items: center;
          gap: 12px;
        }

        .info {
          background: #e6f4ff;
          color: #003a8f;
        }

        .warning {
          background: #fff7e6;
          color: #874d00;
        }

        .error {
          background: #fff1f0;
          color: #a8071a;
        }

        button {
          background: transparent;
          border: none;
          font-size: 1.2em;
          cursor: pointer;
        }
      `
    ];
  }

  protected async getPanelContent(): Promise<TemplateResult>  {
    if (!this.notificationAsset?.attributes) {
      return html``;
    }

    const msg = 'esto es un mensaje' //this.notificationAsset.attributes['message']?.value;
    const level = 'info' //this.notificationAsset.attributes['level']?.value as NotificationLevel;
    const visible = true //this.notificationAsset.attributes['visible']?.value;

    if (!msg || !visible) {
      return html``;
    }

    return html`
      <div class="box ${level ?? 'info'}">
        <span>${msg}</span>
        <button @click=${this.hide}>✕</button>
      </div>
    `;
  }

  private hide() {
    if (this.notificationAsset) {
      this.notificationAsset.attributes['visible'].value = false;
      this.requestUpdate();
    }
  }
}
