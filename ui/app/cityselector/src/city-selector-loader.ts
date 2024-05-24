import {css, html, LitElement, TemplateResult} from "lit";
import {customElement} from "lit/decorators.js";
import {getAppStyle} from "./styles";
import "./components/og-loading";

const styling = css`
    #wrapper {
        height: 100%;
        width: 100%;
        display: flex;
        flex-direction: column;
        justify-content: center;
        align-items: center;
        gap: 5vh;
        background: var(--og-color-primary-dark)
    }
`;

@customElement("city-selector-loader")
export class CitySelectorLoader extends LitElement {

    static get styles() {
        return [getAppStyle(), styling] as any;
    }

    protected render(): TemplateResult {
        return html`
            <div id="wrapper">
                <og-loading></og-loading>
            </div>
        `;
    }
}