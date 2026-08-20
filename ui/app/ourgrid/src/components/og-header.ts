/*
 * Copyright 2026, OpenRemote Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
import { LitElement, type TemplateResult, html, css } from "lit";
import { customElement, property } from "lit/decorators.js";
import { InputType } from "@openremote/or-mwc-components/or-mwc-input";
import { when } from "lit/directives/when.js";
import "./og-input";
import { getAppStyle } from "../styles";
import { styleMap } from "lit/directives/style-map.js";

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

@customElement("og-header")
export class OgHeader extends LitElement {
  @property({ type: Boolean })
  public loading = false;

  @property({ type: Boolean })
  public dark = false;

  @property({ type: Boolean })
  public menuActive = false;

  static styles = [getAppStyle(), styling];

  protected render(): TemplateResult {
    const buttonStyles: {} = {
      "--og-color-primary": this.dark ? "var(--og-color-primary-dark)" : undefined,
      "--or-icon-fill": this.dark ? "var(--or-app-color1)" : "var(--og-color-primary-dark)",
      "--og-mdc-fab-border-radius": "50% 0 50% 50%",
      "--og-mdc-fab-transition": "all ease 0.5s",
      "--og-mdc-fab-background": this.dark ? "transparent" : undefined,
    };
    return html`
      <div id="header-container">
        ${when(
          !this.loading,
          () => html`
            <div id="button-container">
              <og-input
                type="${InputType.BUTTON}"
                .icon="${this.menuActive ? "close" : "menu"}"
                action="${true}"
                style=${styleMap(buttonStyles)}
                @or-mwc-input-changed="${() => {
                  this.dispatchEvent(new CustomEvent("menu"));
                }}"
              ></og-input>
            </div>
          `
        )}
      </div>
    `;
  }
}
