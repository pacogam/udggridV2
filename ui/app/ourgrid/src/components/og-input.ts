import {customElement, property, query} from 'lit/decorators.js';
import {css, html, nothing, PropertyValues, TemplateResult} from 'lit';
import {InputType, OrInputChangedEvent, OrMwcInput} from '@openremote/or-mwc-components/or-mwc-input';
import {getAppStyle} from '../styles';
import {classMap} from 'lit/directives/class-map.js';
import {when} from 'lit/directives/when.js';
import {styleMap} from 'lit/directives/style-map.js';

const styling = css`
    .mdc-button {
        font-family: var(--og-font-family);
        font-size: var(--og-font-size-button-text);
        font-weight: var(--og-font-weight-button-text);
        letter-spacing: normal;
        text-transform: initial;
        transition: background 0.2s;
        -webkit-transition: background 0.2s;
        height: 48px;
    }

    .mdc-button--outlined {
        border-width: 2px;
    }

    .mdc-button--outlined:not(:disabled) {
        border-color: var(--og-color-secondary);
    }

    .mdc-button--outlined:disabled {
        border-color: var(--mdc-theme-primary);
        color: var(--mdc-theme-primary);
    }

    .mdc-button--error {
        --mdc-theme-primary: var(--og-color-error);
    }

    .mdc-text-field--filled:not(.mdc-text-field--disabled) {
        background-color: var(--og-background-shade);
    }

    .mdc-fab:hover, .mdc-fab:focus {
        box-shadow: none;
    }

    .mdc-fab {
        box-shadow: none;
        background: var(--og-mdc-fab-background, var(--og-color-primary));
        width: 64px;
        height: 64px;
        border-radius: var(--og-mdc-fab-border-radius, 50%) !important;
        transition: var(--og-mdc-fab-transition);
    }

    .mdc-fab__icon {
        width: 32px !important;
        height: 32px !important;
        font-size: 32px !important;
    }

    .mdc-notched-outline__leading {
        border-color: var(--og-color-secondary) !important;
    }

    .mdc-notched-outline__notch {
        border-color: var(--og-color-secondary) !important;
    }

    .mdc-notched-outline__trailing {
        border-color: var(--og-color-secondary) !important;
    }

    .mdc-button-grouped {
        padding: 0;
        min-width: 0;
        aspect-ratio: 1/1;
        width: 44px; /*4px smaller than Material spec*/
        height: auto;
    }

    .mdc-select--filled:not(.mdc-select--disabled) .mdc-select__anchor {
        background-color: var(--og-background-shade);
    }
`;

export enum OgSpecialInputType {
    BUTTON_GROUP = 'button-group'
}

type OgInputType = InputType & OgSpecialInputType;

export interface OgInputButtonGroupOption {
    icon: string,
    iconColors?: {
        active?: string,
        inactive?: string
    },
    fillColors?: {
        active?: string,
        inactive?: string
    },
    borderColors?: {
        active?: string,
        inactive?: string
    }
}

@customElement('og-input')
export class OgInput extends OrMwcInput {

    @property({type: String})
    public type?: OgInputType;

    @property({type: Boolean}) // puts a loading indicator in the BUTTON type
    public loading = false;

    @property() // string that overrides text in BUTTON type
    public error?: string;

    @property({type: Boolean})
    public doTranslate = true;

    @query('.mdc-button__label')
    protected buttonLabelElem?: HTMLElement;

    static get styles() {
        return [...super.styles, getAppStyle(), styling];
    }

    // Whether we should continue updating; lifecycle function.
    // For example, we cancel a UI render if only the label is changed by manually appending the HTML.
    protected shouldUpdate(changedProps: PropertyValues): boolean {

        if (changedProps.size === 1) {
            if (changedProps.has('label') && this.label && this.buttonLabelElem) {
                this.onButtonLabelChange();
                return false;
            }
        }
        return super.shouldUpdate(changedProps);
    }

    // Instead of replacing label text instantly, we transition during 200ms manually.
    protected onButtonLabelChange() {
        if (this.buttonLabelElem) {
            this.buttonLabelElem.animate([{opacity: 1}, {opacity: 0}], {duration: 200, fill: 'both'});
            setTimeout(() => {
                this.buttonLabelElem.innerText = this.label;
                this.buttonLabelElem.animate([{opacity: 0}, {opacity: 1}], {duration: 200, fill: 'both'});
            }, 200);
        }
    }

    // Lifecycle render method
    // Only overriding BUTTON type, that is mostly copied from OrMwcInput render(),
    // with the additions of a loading indicator and error message
    protected render(): TemplateResult {
        if (this.type === InputType.BUTTON) {
            const onMouseDown = (ev: MouseEvent) => {
                if (this.disabled || this.readonly || this.loading) {
                    ev.stopPropagation();
                }
            };
            const onMouseUp = (ev: MouseEvent) => {
                if (this.disabled || this.readonly || this.loading) {
                    ev.stopPropagation();
                    return;
                }

                this.dispatchEvent(new OrInputChangedEvent(true, null));
            };
            const onClick = (ev: MouseEvent) => {
                if (this.disabled || this.readonly || this.loading) {
                    ev.stopPropagation();
                }
            };

            const isIconButton = !this.action && !this.label;
            const classes = {
                'mdc-icon-button': isIconButton,
                'mdc-fab': !isIconButton && this.action,
                'mdc-fab--extended': !isIconButton && this.action && !!this.label,
                'mdc-fab--mini': !isIconButton && this.action && (this.compact || this.comfortable),
                'mdc-button': !isIconButton && !this.action,
                'mdc-button--raised': !isIconButton && !this.action && this.raised,
                'mdc-button--unelevated': !isIconButton && !this.action && this.unElevated,
                'mdc-button--outlined': !isIconButton && !this.action && this.outlined,
                'mdc-button--rounded': !isIconButton && !this.action && this.rounded,
                'mdc-button--fullwidth': this.fullWidth,
                'mdc-button--loading': this.loading,
                'mdc-button--error': this.error
            };
            return html`
                <button id="component" class="${classMap(classes)}"
                        ?readonly="${this.readonly || this.loading || this.error}"
                        ?disabled="${this.disabled}"
                        @click="${(ev: MouseEvent) => onClick(ev)}"
                        @mousedown="${(ev: MouseEvent) => onMouseDown(ev)}" @mouseup="${(ev: MouseEvent) => onMouseUp(ev)}">
                    ${!isIconButton ? html`
                        <div class="mdc-button__ripple"></div>` : ``}
                    ${this.icon ? html`
                        <or-icon class="${isIconButton ? '' : this.action ? 'mdc-fab__icon' : 'mdc-button__icon'}" aria-hidden="true" icon="${this.icon}"></or-icon>` : ``}
                    ${(this.label || this.error) ? html`
                        <span class="${this.action ? 'mdc-fab__label' : 'mdc-button__label'}" style="${this.loading ? 'visibility: hidden' : nothing}">
                            ${this.doTranslate ? html`
                                <or-translate .value="${this.error ? this.error : this.label}"></or-translate>` : html`${this.error ? this.error : this.label}`}
                        </span>
                    ` : ``}
                    ${this.loading ? html`<span style="position: absolute;"><og-loading size="medium" style="display: flex; --og-loading-color: var(--mdc-theme-primary);"></og-loading></span>` : ``}
                    ${!isIconButton && this.iconTrailing ? html`
                        <or-icon class="${this.action ? 'mdc-fab__icon' : 'mdc-button__icon'}" aria-hidden="true" icon="${this.iconTrailing}"></or-icon>` : ``}
                </button>
            `;

        } else if (this.type === OgSpecialInputType.BUTTON_GROUP) {
            const onClick = (ev: MouseEvent, index: number) => {
                if (this.disabled || this.readonly || this.loading) {
                    ev.stopPropagation();
                    return;
                }

                this.dispatchEvent(new OrInputChangedEvent(index, null));
            };
            return html`
                <div style="display: flex; gap: 8px;">
                    ${this.options.map((item: string | OgInputButtonGroupOption, index) => {
                        const isString = typeof item === 'string';
                        const buttonStyles: {} = {
                            'width': '34px',
                            'border-radius': '50%',
                            'border-color': !isString ? (this.value === index ? item.borderColors?.active : item.borderColors?.inactive) : undefined,
                            'background-color': !isString ? (this.value === index ? item.fillColors?.active : item.fillColors?.inactive) : undefined
                        };
                        const iconStyles: {} = {
                            '--or-icon-fill': !isString ? (this.value === index ? item.iconColors?.active : item.iconColors?.inactive) : undefined
                        };
                        return html`
                            <button class="mdc-button mdc-button--outlined mdc-button-grouped" style="${styleMap(buttonStyles)}" @click="${(ev: MouseEvent) => onClick(ev, index)}">
                                ${when(this.loading, () => html`
                                    <span style="position: absolute;"><og-loading size="medium" style="display: flex; --og-loading-color: var(--mdc-theme-primary);"></og-loading></span>
                                `, () => html`
                                    <or-icon aria-hidden="true" icon="${isString ? item : item.icon}" style="${styleMap(iconStyles)}"></or-icon>
                                `)}
                            </button>
                        `;
                    })}
                </div>
            `;


        } else {
            return super.render();
        }
    }
}
