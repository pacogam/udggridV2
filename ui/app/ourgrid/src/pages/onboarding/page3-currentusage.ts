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
import { OgPage, type OgPageProvider } from "../util/og-page";
import type { GridAppStateKeyed } from "../../util/og-state";
import { customElement } from "lit/decorators.js";
import { type TemplateResult, html, css } from "lit";
import type { AppStateKeyed } from "@openremote/or-app";
import type { Store } from "@reduxjs/toolkit";
import { i18next } from "@openremote/or-translate";
import "../../panels/panel-usage-overview";

export function onboardingThreeProvider(store: Store<GridAppStateKeyed>): OgPageProvider<AppStateKeyed> {
  return {
    name: "onboarding-3",
    routes: ["onboarding-3"],
    hideHeader: true,
    pageCreator: () => new Page3Currentusage(store),
    skipDataCheck: true,
  };
}

const styling = css`
  #usage-container {
    height: 100%;
    display: flex;
    flex-direction: column;
    text-align: center;
    pointer-events: none;
  }

  #usage-text-container {
    flex: 1;
    display: flex;
    flex-direction: column;
    justify-content: center;
    gap: 32px;
    text-align: center;
    padding: 16px 32px;
  }
`;

@customElement("page3-currentusage")
export class Page3Currentusage extends OgPage<GridAppStateKeyed> {
  static get styles() {
    return [...super.styles, styling];
  }

  get name(): string {
    return "Onboarding 3/3";
  }

  protected render(): TemplateResult {
    return html`
      <div id="usage-container">
        <div id="usage-animation-container">
          <panel-usage-overview
            .noPadding="${true}"
            aspectRatio="1/1"
            .staticAnimation="${true}"
          ></panel-usage-overview>
        </div>
        <div id="usage-text-container">
          <span class="text-heading">${i18next.t("onboarding.page3-heading")}</span>
          <span class="text-primary">${i18next.t("onboarding.page3-text")}</span>
        </div>
      </div>
    `;
  }
}
