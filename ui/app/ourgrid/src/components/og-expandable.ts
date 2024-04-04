import { LitElement, TemplateResult, html, css } from 'lit';
import { customElement, property } from 'lit/decorators.js';
import { classMap } from 'lit/directives/class-map.js';
import {getAppStyle} from '../styles';

const styling = css`
  #header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

  .header-container {
    display: flex;
    flex-direction: column;
  }

  .chevron {
    --or-icon-width: 40px;
    transform: rotateX(0deg);
    transition: all ease-in-out 0.4s;
    -webkit-transition: all ease-in-out 0.4s;
  }

  .chevron-expanded {
    transform: rotateX(-180deg);
  }

  .slot-parent {
    overflow: hidden visible;
    max-height: 0;
    transition: max-height ease-in-out 0.4s;
    -webkit-transition: max-height ease-in-out 0.4s;
  }

  .slot-parent--expanded {
    max-height: 40vh;
  }
  .slot-parent--expanded--full {
    max-height: 200vh; /*'a very high value'*/
  }

  .slot-parent--fullwidth {
    padding: 0 24px;
    margin: 0 -24px;
  }
`;

@customElement('og-expandable')
export class OgExpandable extends LitElement {

    @property()
    protected header!: TemplateResult;

    @property()
    protected expanded = false;

    @property()
    protected fullWidth = false;

    @property()
    protected limitedHeight = false;

    static styles = [getAppStyle(), styling];

    protected render(): TemplateResult {
        const slotClasses = {
            'slot-parent--expanded': this.expanded,
            'slot-parent--expanded--full': this.expanded && !this.limitedHeight,
            'slot-parent--fullwidth': this.fullWidth
        };
        const iconClasses = {
            'chevron': true,
            'chevron-expanded': this.expanded
        };
        return html`
            <div>
                <div id="header" @click="${() => { this.expanded = !this.expanded; }}">
                    ${this.header}
                    <or-icon icon="chevron-down" class="${classMap(iconClasses)}"></or-icon>
                </div>
                <div class="slot-parent ${classMap(slotClasses)}">
                    <slot></slot>
                </div>
            </div>
        `;
    }
}
