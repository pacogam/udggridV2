import {OrMwcList} from "@openremote/or-mwc-components/or-mwc-list";
import {customElement} from "lit/decorators.js";
import {getAppStyle} from "../styles";
import {css} from "lit";

const styling = css`
    .mdc-list {
        font-family: var(--og-font-family);
        font-size: var(--og-font-size-secondary);
        font-weight: var(--og-font-weight-secondary);
        color: var(--og-color-primary);
    }
`;

@customElement("og-city-list")
export class OgCityList extends OrMwcList {

    static get styles() {
        return [...super.styles, getAppStyle(), styling] as any;
    }


}