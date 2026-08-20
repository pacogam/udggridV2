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
import { LitElement, html, css, type TemplateResult, type PropertyValues } from "lit";
import { customElement, property, query } from "lit/decorators.js";
import { when } from "lit/directives/when.js";
import { styleMap } from "lit/directives/style-map.js";
import { classMap } from "lit/directives/class-map.js";
import { getAppStyle } from "../styles";
import { InputType } from "@openremote/or-mwc-components/or-mwc-input";
import type { OgInput } from "./og-input";
import { until } from "lit/directives/until.js";

export class OgPanelRenderEvent extends CustomEvent<void> {
  public static readonly NAME = "render";

  constructor() {
    super(OgPanelRenderEvent.NAME, {
      bubbles: true,
      composed: true,
    });
  }
}

export class OgPanelActionUpdateEvent extends CustomEvent<PanelAction> {
  public static readonly NAME = "actionUpdate";

  constructor(action: PanelAction) {
    super(OgPanelActionUpdateEvent.NAME, {
      bubbles: true,
      composed: true,
      detail: action,
    });
  }
}

export interface PanelAction {
  text: string;
  color?: string;
  disabled?: boolean;
  loading?: boolean;
  error?: string;
  action?: () => Promise<boolean>;
}

const styling = css`
  #panel-wrapper {
    padding: 24px;
    position: relative;
  }

  #panel-container {
    display: flex;
    flex-direction: column;
    gap: 24px;
  }

  #header-wrapper {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

  .header-container {
    display: flex;
    flex-direction: column;
  }

  .header-container--graphic {
    margin-top: 10px;
  }

  #header-graphic-container {
    margin-left: -8%;
    display: flex;
    justify-content: end;
  }

  #header-graphic-img {
    width: 100%;
    height: 120%;
    max-width: 400px;
  }

  #action-container {
    position: absolute;
    display: block;
    bottom: -24px;
    width: calc(100% - 48px);
    z-index: 3;
  }

  #action-button {
    --or-mwc-input-color: var(--og-color-danger);
    --og-color-primary: var(--og-color-primary);
    width: 100%;
  }
`;

@customElement("og-panel")
export abstract class OgPanel extends LitElement {
  @property() // main title
  public heading?: string | TemplateResult;

  @property()
  public headingStyle?: "title" | "heading" | "primary" = "heading";

  @property() // optional subtitle
  public subtitle?: string | TemplateResult;

  @property() // whether the panel should be collapsed, and is expandable to show the content
  public expandable = false;

  @property({ type: Boolean, reflect: true }) // inverts colors; background is dark and text is white.
  public dark = false;

  @property() // removes horizontal padding
  public fullWidth = false;

  @property() // removes vertical padding
  public fullHeight = false;

  @property({ type: Object /*, reflect: true */ }) // adds action button to the bottom of the panel with the config specified.
  public action?: PanelAction;

  @property() // state of expandable panel
  public expanded = false;

  @property() // show dots graphic on top of the panel
  public dotsGraphic = false;

  @property()
  public transparent = true;

  @property()
  public rounded = false;

  @property() // Should be set to TRUE if inside <og-panel-wrapper>
  public slotted = false;

  @query("og-input")
  protected ogInput: OgInput;

  static styles = [getAppStyle(), styling];

  protected updated(changedProps: PropertyValues) {
    super.updated(changedProps);
    if (changedProps.has("action") && this.action) {
      this.dispatchEvent(new OgPanelActionUpdateEvent(this.action));
    }
  }

  /* --------------------------------------------------------------------- */

  // Default content, should be overridden by individual panels.
  protected abstract getPanelContent(): Promise<TemplateResult>;

  protected render(): TemplateResult {
    this.dispatchEvent(new OgPanelRenderEvent());
    const wrapperStyles = {
      "padding-top": this.fullHeight ? "0" : undefined,
      "padding-bottom": this.fullHeight ? "0" : undefined,
      "border-radius": this.rounded ? "var(--og-panel-border-radius)" : "0",
      background: this.transparent
        ? "transparent"
        : this.dark
          ? "var(--og-color-primary-dark)"
          : "var(--og-background-shade)",
    };
    const header = html` ${until(this.getHeaderTemplate(this.heading, this.headingStyle, this.subtitle))} `;

    return html`
      <div id="panel-wrapper" style="${styleMap(wrapperStyles)}">
        <!-- (expandable) Heading with optional graphic -->
        <div id="panel-container">
          ${when(
            this.expandable,
            () => html`
              <og-expandable
                .expanded="${this.expanded}"
                .header="${header}"
                .fullWidth="${this.fullWidth}"
                style="--or-icon-fill: ${this.dark ? "var(--og-color-secondary)" : "black"}"
              >
                <div style="${this.fullWidth ? "margin: 0 -24px" : ""}">${until(this.getPanelContent())}</div>
              </og-expandable>
            `,
            () => html`
              ${when(this.heading || this.dotsGraphic, () => header)}
              <div id="panel-content" style="${this.fullWidth ? "margin: 0 -24px" : ""}">
                ${until(this.getPanelContent())}
              </div>
            `
          )}
        </div>

        <!-- Action button attached to the bottom to the panel -->
        ${when(!this.slotted && this.action, () => html`${until(this.getActionTemplate())}`)}
      </div>

      <!-- Extra bottom margin if an action is present. -->
      ${when(this.action, () => html` <div style="height: 1px; margin-top: 24px;"></div> `)}
    `;
  }

  protected async getHeaderTemplate(
    heading: string | TemplateResult,
    headingStyle: "heading" | "title" | "primary",
    subtitle?: string | TemplateResult
  ): Promise<TemplateResult> {
    const headerClasses = {
      "header-container": true,
      "header-container--graphic": this.dotsGraphic,
    };
    const textColorStyles: {} = {
      color: this.dark ? "var(--og-color-primary)" : "var(--og-color-primary-dark)",
    };
    const headingClasses: {} = {
      "text-title": headingStyle === "title",
      "text-heading": headingStyle === "heading",
      "text-primary": headingStyle === "primary",
    };
    const subtitleClasses: {} = {
      "text-subheading": headingStyle === "title",
      "text-subheading2": headingStyle === "heading",
      "text-secondary": headingStyle === "primary",
    };
    return html`
      <div id="header-wrapper">
        ${when(
          this.heading,
          () => html`
            <div class="${classMap(headerClasses)}">
              <span class="${classMap(headingClasses)}" style="${styleMap(textColorStyles)}"> ${heading} </span>
              ${when(
                subtitle,
                () => html`
                  <span class="${classMap(subtitleClasses)}" style="${styleMap(textColorStyles)}"> ${subtitle} </span>
                `
              )}
            </div>
          `
        )}
        ${when(
          this.dotsGraphic,
          () => html`
            <div id="header-graphic-container" style="flex: 1;">
              <img id="header-graphic-img" src="images/dots-heading-suffix.svg" />
            </div>
          `
        )}
      </div>
    `;
  }

  public async getActionTemplate(): Promise<TemplateResult> {
    const styles: {} = {
      "--or-mwc-input-color": this.action?.color,
      cursor: this.action?.loading || this.action?.disabled ? "wait" : undefined,
    };
    return html`
      <div id="action-container">
        <og-input
          id="action-button"
          type="${InputType.BUTTON}"
          raised
          rounded
          comfortable
          fullWidth
          .loading="${this.action?.loading}"
          .error="${this.action?.error}"
          .label="${this.action?.text}"
          style="${styleMap(styles)}"
          @or-mwc-input-changed="${() => this.onActionButtonClick()}"
        ></og-input>
      </div>
    `;
  }

  public async onActionButtonClick() {
    const readonly = this.action.loading || this.action.disabled;
    if (this.action.action !== undefined && !readonly) {
      this.action.loading = true;
      this.requestUpdate("action");
      this.action.action().finally(() => {
        this.action.loading = false;
        this.requestUpdate("action");
      });
    }
  }
}
