import {InputType, OrMwcInput} from "@openremote/or-mwc-components/or-mwc-input";
import {css} from "lit";
import {customElement, property} from "lit/decorators.js";
import {getAppStyle} from "../styles";

const styling = css`
    .mdc-button {
        font-family: var(--og-font-family);
        font-size: var(--og-font-size-button-text);
        font-weight: var(--og-city-input-font-weight, var(--og-font-weight-button-text));
        letter-spacing: normal;
        text-transform: initial;
        transition: background 0.2s;
        -webkit-transition: background 0.2s;
        height: 48px;
    }

    .mdc-text-field {
        align-items: center;
    }
    
    .mdc-text-field input {
        font-family: var(--og-font-family);
        font-size: var(--og-font-size-button-text);
        font-weight: var(--og-city-input-font-weight, var(--og-font-weight-primary));
        letter-spacing: normal;
        text-transform: initial;
    }

    .mdc-text-field .mdc-text-field__input {
        caret-color: white;
    }

    .mdc-button--outlined:not(:disabled), .mdc-text-field--outlined:not(:disabled) {
        border: var(--og-city-input-border, 2px solid var(--og-color-primary));
    }

    .mdc-button--rounded:not(:disabled), .or-mwc-input--rounded {
        border-radius: 24px !important;
    }

    .mdc-text-field:not(:disabled) .mdc-text-field__input {
        text-align: center;
        color: var(--og-city-input-color, var(--og-color-primary));
    }

    .mdc-text-field:not(:disabled) .mdc-text-field__input::placeholder {
        color: var(--og-city-input-color, var(--og-color-primary));
        opacity: 0.5;
    }

    .mdc-button:not(:disabled) {
        color: var(--og-city-input-color, var(--og-color-primary));
    }

    .mdc-button--error {
        --mdc-theme-primary: var(--og-color-error);
    }
`;

/**
 * Simple wrapper that reproduces event bubbling by redispatching the @change event.
 */
@customElement("og-city-input")
export class OgCityInput extends OrMwcInput {

    @property()
    public svgIcon?: string;

    static get styles() {
        return [...super.styles, getAppStyle(), styling] as any;
    }

    firstUpdated(_changedProperties) {
        super.firstUpdated(_changedProperties);
        if(this.type === InputType.TEXT) {
            this.shadowRoot.getElementById("elem")?.addEventListener('input', (ev) => {
                this.dispatchEvent(new CustomEvent('city-input-changed', { detail: { value: (ev.target as HTMLInputElement).value} }));
            });
            if(this.svgIcon) {
                const svgElem = document.createElement("img");
                svgElem.src = `images/${this.svgIcon}.svg`;
                svgElem.alt = this.svgIcon;
                svgElem.height = 24;
                svgElem.width = 24;
                svgElem.style.position = "absolute";
                svgElem.style.right = "12px";
                this.shadowRoot.querySelector(".mdc-text-field")?.appendChild(svgElem);
            }
        } else if(this.type === InputType.BUTTON) {
            if(this.svgIcon) {
                const svgElem = document.createElement("img");
                svgElem.src = `images/${this.svgIcon}.svg`;
                svgElem.alt = this.svgIcon;
                svgElem.height = 24;
                svgElem.width = 24;
                svgElem.style.position = "absolute";
                svgElem.style.right = "12px";
                this.shadowRoot.querySelector(".mdc-button")?.appendChild(svgElem);
            }
        }
    }
}