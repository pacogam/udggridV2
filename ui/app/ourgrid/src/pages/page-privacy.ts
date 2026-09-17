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
import type { GridAppStateKeyed } from "../util/og-state";
import { type TemplateResult, html, css } from "lit";
import { customElement, state } from "lit/decorators.js";
import type { AppStateKeyed } from "@openremote/or-app";
import type { Store } from "@reduxjs/toolkit";
import "../panels/panel-privacy-statement";
import { OgPage, type OgPageProvider } from "./util/og-page";

export function pagePrivacyProvider(store: Store<GridAppStateKeyed>): OgPageProvider<AppStateKeyed> {
  return {
    name: "privacy",
    routes: ["privacy"],
    pageCreator: () => new PagePrivacy(store),
    skipDataCheck: true,
  };
}

const styling = css`
  .page-wrapper {
    width: 100%;
    height: 100vh;
    background: var(--og-color-primary);
  }
`;

@customElement("page-privacy")
export class PagePrivacy extends OgPage<GridAppStateKeyed> {
  @state()
  protected language: string;

  get name(): string {
    return "privacy";
  }

  stateChanged(state: GridAppStateKeyed): void {
    this.language = state.gridApp.language;
  }

  static get styles() {
    return [...super.styles, styling];
  }

  protected render(): TemplateResult {
    return html`
      <div class="page-wrapper">
        <div style="display: flex; flex-direction: column; align-items: center; padding: 16px;">
          <div style="width: 100%; display: flex; flex-direction: column; align-items: center;">
            <img src="images/dots-onboarding.svg" style="width: 100%;" />
            <or-translate
              class="text-heading"
              value="page-privacy.heading"
              style="margin-top: -10%; text-align: center; max-width: 65vw;"
            ></or-translate>
          </div>
          <div style="width: 100%;">
            <panel-privacy-statement fullWidth="${true}" .language="${this.language}"></panel-privacy-statement>
          </div>
        </div>
      </div>
    `;
  }
}
