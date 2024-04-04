import { LitElement, TemplateResult, html, css } from 'lit';
import { customElement, property } from 'lit/decorators.js';
import {InputType} from '@openremote/or-mwc-components/or-mwc-input';
import { when } from 'lit/directives/when.js';
import '@openremote/or-icon';
import './og-input';
import {getAppStyle} from '../styles';
import { styleMap } from 'lit/directives/style-map.js';

const styling = css`
  :host {
    background: transparent;
    position: absolute;
    width: 100%;
    z-index: 10;
  }

  #header-container {
    display: flex;
    align-items: center;
    justify-content: end;
  }
`;

@customElement('og-header')
export class OgHeader extends LitElement {

    @property()
    public loading = false;

    @property()
    public dark = false;

    static styles = [getAppStyle(), styling];

    protected render(): TemplateResult {
        const buttonStyles: {} = {
            '--og-color-primary': this.dark ? 'var(--og-color-primary-dark)' : undefined,
            '--or-icon-fill': this.dark ? 'var(--or-app-color1)' : 'var(--og-color-primary-dark)',
            '--og-mdc-fab-border-radius': '50% 0 50% 50%'
        };
        return html`
            <div id="header-container">
                ${when(!this.loading, () => html`
                    <div id="button-container">
                        <og-input type="${InputType.BUTTON}" icon="tune" action="${true}" style=${styleMap(buttonStyles)}
                                      @or-mwc-input-changed="${() => { this.dispatchEvent(new CustomEvent('menu')); }}"
                        ></og-input>
                    </div>
                `)}
            </div>
        `;
    }
}
