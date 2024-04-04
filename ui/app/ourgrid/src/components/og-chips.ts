import {LitElement, TemplateResult, html, css, unsafeCSS, PropertyValues} from 'lit';
import {customElement, property} from 'lit/decorators.js';
import {when} from 'lit/directives/when.js';
import {map} from 'lit/directives/map.js';
import {classMap} from 'lit/directives/class-map.js';
import {styleMap} from 'lit/directives/style-map.js';
import {MDCChipSet} from '@material/chips/deprecated';
import {getAppStyle} from '../styles';

const chipStyle = require('@material/chips/dist/mdc.chips.min.css');

// Since documentation on the Material chips was hard to find, here is a link:
// https://github.com/material-components/material-components-web/blob/master/packages/mdc-chips/deprecated/README.md
// Apparently the links to official documentation are incorrect, and we're using a Material 2 (deprecated) version.

export interface Chip {
    leadingIcon?: string,
    text: string | TemplateResult
    trailingIcon?: string
    color?: string
    loading?: boolean
    disabled?: boolean, // only functional, no visual changes
    action?: () => void
}

interface MDCChipEvent extends Event {
    detail: {
        chipId: string;
        selected?: boolean;
    }
}

const styling = css`
  .mdc-chip__text {
    font-family: var(--og-font-family);
    font-size: var(--og-font-size-button-small);
    font-weight: var(--og-font-weight-button-small);
  }
`;

@customElement('og-chips')
export class OgChips extends LitElement {

    @property()
    protected chips: Chip[] = [];

    @property()
    protected outlined = false;

    protected chipsObj: MDCChipSet;

    static get styles() {
        return [unsafeCSS(chipStyle), getAppStyle(), styling];
    }

    // After first lifecycle render...
    protected firstUpdated(changedProps: PropertyValues) {
        super.firstUpdated(changedProps);
        this.chipsObj = new MDCChipSet(this.shadowRoot?.querySelector('.mdc-chip-set'));
        this.chipsObj.listen('MDCChip:interaction', (ev: MDCChipEvent) => {
            const elem = ev.target as HTMLElement;
            const id: number = Number((elem.id).split('-')[1]);
            const chip = this.chips[id];
            if(!chip?.disabled && !chip?.loading && !!chip?.action) {
                chip.action();
            }
        });
    }

    protected render(): TemplateResult {
        const chipSetClasses = {
            'mdc-chip-set': true,
            'mdc-chip-set--input': true
        };
        return html`
            <div class="${classMap(chipSetClasses)}" role="grid">
                ${map(this.chips, (chip, index) => {
                    const chipStyles = {
                        'position': 'relative',
                        'background-color': this.outlined ? 'transparent' : undefined,
                        'border': this.outlined ? '1px solid rgba(0, 0, 0, 0.12)' : undefined,
                        'cursor': chip.loading || chip.disabled ? 'default' : undefined
                    };
                    const iconStyles = {
                        'opacity': chip.loading ? '0' : undefined,
                        'color': chip.color ? chip.color : 'rgba(0, 0, 0, 0.54)'
                    };
                    const textStyles = {
                        'opacity': chip.loading ? '0' : undefined
                    };
                    return html`
                        <div id="chip-${index}" class="mdc-chip" style="${styleMap(chipStyles)}" role="row">
                            ${when(!chip.disabled && !chip.loading, () => html`
                                <div class="mdc-chip__ripple"></div>
                            `)}
                            ${when(chip.leadingIcon, () => html`
                                <or-icon class="mdc-chip__icon mdc-chip__icon--leading" icon="${chip.leadingIcon}" style="${styleMap(iconStyles)}"></or-icon>
                            `)}
                            <span role="gridcell">
                                <span role="button" tabindex="${index}" class="mdc-chip__primary-action">
                                    ${when(chip.loading, () => html`
                                        <og-loading style="position: absolute; left: calc(50% - 8px); top: calc(50% - 8px);" size="small"></og-loading>
                                    `)}
                                    <span class="mdc-chip__text" style="${styleMap(textStyles)}">
                                        ${chip.text}
                                    </span>
                                </span>
                            </span>
                            ${when(chip.trailingIcon, () => html`
                                <span role="gridcell">
                                    <i class="material-icons mdc-chip__icon mdc-chip__icon--trailing" tabindex="-1" role="button">${chip.trailingIcon}</i>
                                </span>
                            `)}
                        </div>
                    `;
                })}
            </div>
        `;
    }
}
