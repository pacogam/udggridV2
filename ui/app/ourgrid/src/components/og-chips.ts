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
import { LitElement, type TemplateResult, html, css, unsafeCSS, type PropertyValues } from "lit";
import { customElement, property } from "lit/decorators.js";
import { when } from "lit/directives/when.js";
import { map } from "lit/directives/map.js";
import { classMap } from "lit/directives/class-map.js";
import { styleMap } from "lit/directives/style-map.js";
import { MDCChipSet } from "@material/chips/deprecated";
import { getAppStyle } from "../styles";

const chipStyle = require("@material/chips/dist/mdc.chips.min.css");

// Since documentation on the Material chips was hard to find, here is a link:
// https://github.com/material-components/material-components-web/blob/master/packages/mdc-chips/deprecated/README.md
// Apparently the links to official documentation are incorrect, and we're using a Material 2 (deprecated) version.

export interface Chip {
  selected?: boolean;
  leadingIcon?: string;
  text: string | TemplateResult;
  trailingIcon?: string;
  color?: string;
  loading?: boolean;
  disabled?: boolean; // only functional, no visual changes
  action?: () => void;
}

interface MDCChipEvent extends Event {
  detail: {
    chipId: string;
    selected?: boolean;
  };
}

const styling = css`
  .mdc-chip__text {
    font-family: var(--og-font-family);
    font-size: var(--og-font-size-button-small);
    font-weight: var(--og-font-weight-button-small);
  }

  .mdc-chip .mdc-chip__ripple::before,
  .mdc-chip .mdc-chip__ripple::after {
    background-color: var(--mdc-ripple-color, rgba(0, 0, 0, 0.35));
  }

  .mdc-chip-set {
    justify-content: end;
  }
`;

@customElement("og-chips")
export class OgChips extends LitElement {
  @property({ type: Object })
  public chips: Chip[] = [];

  @property({ type: Boolean })
  public outlined = false;

  /**
   * Whether the chips should act as "toggleable", and listen to the 'selected' state of the {@link Chip}.
   */
  @property({ type: Boolean })
  public choice = false;

  protected chipsObj: MDCChipSet;

  static get styles() {
    return [unsafeCSS(chipStyle), getAppStyle(), styling];
  }

  // After first lifecycle render...
  protected firstUpdated(changedProps: PropertyValues) {
    super.firstUpdated(changedProps);
    this.chipsObj = new MDCChipSet(this.shadowRoot?.querySelector(".mdc-chip-set"));
    this.chipsObj.listen("MDCChip:interaction", (ev: MDCChipEvent) => {
      const elem = ev.target as HTMLElement;
      const id: number = Number(elem.id.split("-")[1]);
      const chip = this.chips[id];
      if (!chip?.disabled && !chip?.loading && !!chip?.action) {
        chip.action();
      }
    });
  }

  protected render(): TemplateResult {
    const chipSetClasses = {
      "mdc-chip-set": true,
      "mdc-chip-set--input": true,
    };
    return html`
      <div class="${classMap(chipSetClasses)}" role="grid">
        ${map(this.chips, (chip, index) => {
          const chipStyles = {
            position: "relative",
            background: this._getBackground(chip.selected, this.outlined),
            border: this._getBorder(chip.selected, this.outlined),
            cursor: chip.loading || chip.disabled ? "default" : undefined,
            opacity: chip.disabled ? "0.5" : undefined,
            transition: "all 200ms",
          };
          const rippleStyles = {
            background: this._getRippleBackground(chip.selected, this.outlined),
            transition: "all 200ms",
          };
          const iconStyles = {
            opacity: chip.loading ? "0" : undefined,
            color: chip.selected ? "var(--og-color-success)" : chip.color ? chip.color : "rgba(0, 0, 0, 0.54)",
          };
          const textStyles = {
            opacity: chip.loading ? "0" : undefined,
            color: this._getTextColor(chip.selected, this.outlined),
          };
          return html`
            <div id="chip-${index}" class="mdc-chip" style="${styleMap(chipStyles)}" role="row">
              ${when(
                !chip.disabled && !chip.loading,
                () => html` <div class="mdc-chip__ripple" style="${styleMap(rippleStyles)}"></div> `
              )}
              ${when(
                chip.leadingIcon,
                () => html`
                  <or-icon
                    class="mdc-chip__icon mdc-chip__icon--leading"
                    icon="${chip.leadingIcon}"
                    style="${styleMap(iconStyles)}"
                  ></or-icon>
                `
              )}
              <span role="gridcell">
                <span role="button" tabindex="${index}" class="mdc-chip__primary-action">
                  ${when(
                    chip.loading,
                    () => html`
                      <og-loading
                        style="position: absolute; left: calc(50% - 8px); top: calc(50% - 8px);"
                        size="small"
                      ></og-loading>
                    `
                  )}
                  <span class="mdc-chip__text" style="${styleMap(textStyles)}"> ${chip.text} </span>
                </span>
              </span>
              ${when(
                chip.trailingIcon,
                () => html`
                  <span role="gridcell">
                    <i class="material-icons mdc-chip__icon mdc-chip__icon--trailing" tabindex="-1" role="button"
                      >${chip.trailingIcon}</i
                    >
                  </span>
                `
              )}
            </div>
          `;
        })}
      </div>
    `;
  }

  protected _getBackground(selected = false, outlined = false): string | undefined {
    if (!outlined) {
      return undefined;
    } else {
      return "transparent";
    }
  }

  protected _getRippleBackground(selected = false, outlined = false): string | undefined {
    if (!outlined) {
      return undefined;
    } else {
      return selected ? "rgba(var(--og-color-success-rgb), 0.1)" : "transparent";
    }
  }

  protected _getBorder(selected = false, outlined = false): string | undefined {
    if (!outlined) {
      return undefined;
    } else {
      return selected ? "1px solid var(--og-color-success)" : "1px solid rgba(0, 0, 0, 0.12)";
    }
  }

  protected _getTextColor(selected = false, outlined = false): string | undefined {
    if (!outlined) {
      return undefined;
    } else {
      return selected ? "var(--og-color-success)" : undefined;
    }
  }
}
