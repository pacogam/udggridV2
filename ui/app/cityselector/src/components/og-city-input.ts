import {InputType, OrMwcInput} from "@openremote/or-mwc-components/or-mwc-input";
import { css } from "lit";
import {customElement} from "lit/decorators.js";
import {getAppStyle} from "../styles";

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

    .mdc-text-field--filled:not(:disabled) {
        background-color: var(--og-color-secondary-dark);
    }

    .mdc-button--outlined:not(:disabled) {
        border-color: var(--og-color-secondary);
    }

    .mdc-text-field:not(:disabled) .mdc-text-field__input {
        color: var(--og-color-primary);
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

    static get styles() {
        return [...super.styles, getAppStyle(), styling] as any;
    }

    firstUpdated(_changedProperties) {
        super.firstUpdated(_changedProperties);
        if(this.type === InputType.TEXT) {
            this.shadowRoot.getElementById("elem")?.addEventListener('input', (ev) => {
                this.dispatchEvent(new CustomEvent('city-input-changed', { detail: { value: (ev.target as HTMLInputElement).value} }));
            });
        }
    }
}