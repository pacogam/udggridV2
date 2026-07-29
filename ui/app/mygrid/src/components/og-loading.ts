import {MDCCircularProgress} from '@material/circular-progress';
import {LitElement, TemplateResult, html, css, unsafeCSS, PropertyValues} from 'lit';
import {customElement, property} from 'lit/decorators.js';
import {when} from 'lit/directives/when.js';
import {styleMap} from 'lit/directives/style-map.js';
import {getAppStyle} from '../styles';

const spinnerStyle = require('@material/circular-progress/dist/mdc.circular-progress.min.css');

// Since documentation on the Material spinner was hard to find, here is a link:
// https://github.com/material-components/material-components-web/tree/master/packages/mdc-circular-progress

const styling = css`
  .mdc-circular-progress__determinate-circle, .mdc-circular-progress__indeterminate-circle-graphic {
    stroke: var(--og-loading-color--internal);
  }
  .mdc-circular-progress__determinate-track {
    stroke: var(--og-loading-color-track--internal);
  }
`;

@customElement('og-loading')
export class OgLoading extends LitElement {

    @property() // TODO: Implement bar loader
    protected type: 'spinner' | 'bar' = 'spinner';

    @property() // whether it is "infinite loading" or following the progress property.
    protected determinate = false;

    @property() // auto switch of indicator color based on DARK mode. Use CSS and var(--og-loading-color) for full control of color
    protected dark = false;

    @property() // progress value of loader; between 0 and 1. Determinate should be set to true.
    protected progress = 0;

    @property() // thickness of the loader
    protected strokePx = 4;

    @property() // size of the loader
    protected size: 'small' | 'medium' | 'large' | number = 'large';

    protected loadingObj?: MDCCircularProgress;

    static get styles() {
        return [unsafeCSS(spinnerStyle), getAppStyle(), styling];
    }

    // Whether we should continue updating; lifecycle function.
    // For example, we cancel a UI render if only the label is changed by manually appending the HTML.
    protected shouldUpdate(changedProps: PropertyValues) {
        if(this.loadingObj) {
            if(changedProps.has('progress')) {
                this.loadingObj.progress = this.progress;
                changedProps.delete('progress');
            }
            if(changedProps.has('determinate')) {
                this.loadingObj.determinate = this.determinate;
                changedProps.delete('determinate');
            }
        }
        if(changedProps.size === 0) {
            return false;
        } else {
            return super.shouldUpdate(changedProps);
        }
    }

    // After first render; lifecycle function
    protected firstUpdated() {
        this.loadingObj = new MDCCircularProgress(this.shadowRoot?.querySelector('.mdc-circular-progress'));
        this.loadingObj.determinate = this.determinate;
        this.loadingObj.progress = this.progress;
    }

    protected render(): TemplateResult {
        return html`

            ${when(this.type === 'spinner', () => {
                const spinnerStyles = {
                    'width': `${this.getCircularWidthBySize(this.size)}px`,
                    'aspect-ratio': '1/1',
                    '--og-loading-color--internal': `var(--og-loading-color, ${this.dark ? 'var(--og-color-danger)' : 'var(--og-color-danger)'})`,
                    '--og-loading-color-track--internal': `var(--og-loading-color-track, ${this.dark ? 'var(--og-color-secondary)' : 'var(--og-color-secondary-dark)'})`
                };
                return html`
                    <div class="mdc-circular-progress" style="${styleMap(spinnerStyles)}" role="progressbar" aria-label="Progress Bar" aria-valuemin="0" aria-valuemax="1">
                        
                        <!-- When determinate (based on progress) -->
                        <div class="mdc-circular-progress__determinate-container">
                            <svg class="mdc-circular-progress__determinate-circle-graphic" viewBox="0 0 48 48" xmlns="http://www.w3.org/2000/svg">
                                <circle class="mdc-circular-progress__determinate-track" cx="24" cy="24" r="18" stroke-width="${this.strokePx}"/>
                                <circle class="mdc-circular-progress__determinate-circle" cx="24" cy="24" r="18" stroke-dasharray="113.097" stroke-dashoffset="113.097" stroke-width="${this.strokePx}"/>
                            </svg>
                        </div>
                        
                        <!-- When indeterminate (infinite loading) -->
                        <div class="mdc-circular-progress__indeterminate-container">
                            <div class="mdc-circular-progress__spinner-layer">
                                <div class="mdc-circular-progress__circle-clipper mdc-circular-progress__circle-left">
                                    <svg class="mdc-circular-progress__indeterminate-circle-graphic" viewBox="0 0 48 48" xmlns="http://www.w3.org/2000/svg">
                                        <circle cx="24" cy="24" r="18" stroke-dasharray="113.097" stroke-dashoffset="56.549" stroke-width="${this.strokePx}"/>
                                    </svg>
                                </div>
                                <div class="mdc-circular-progress__gap-patch">
                                    <svg class="mdc-circular-progress__indeterminate-circle-graphic" viewBox="0 0 48 48" xmlns="http://www.w3.org/2000/svg">
                                        <circle cx="24" cy="24" r="18" stroke-dasharray="113.097" stroke-dashoffset="56.549" stroke-width="${this.strokePx * 0.8}"/>
                                    </svg>
                                </div>
                                <div class="mdc-circular-progress__circle-clipper mdc-circular-progress__circle-right">
                                    <svg class="mdc-circular-progress__indeterminate-circle-graphic" viewBox="0 0 48 48" xmlns="http://www.w3.org/2000/svg">
                                        <circle cx="24" cy="24" r="18" stroke-dasharray="113.097" stroke-dashoffset="56.549" stroke-width="${this.strokePx}"/>
                                    </svg>
                                </div>
                            </div>
                        </div>
                    </div>
                `;
            })}
        `;
    }

    protected getCircularWidthBySize(size: 'small' | 'medium' | 'large' | number): number {
        if(typeof size === 'number') {
            return size;
        } else {
            switch (size) {
                case 'small': return 16;
                case 'medium': return 24;
                default: return 48;
            }
        }
    }
}
