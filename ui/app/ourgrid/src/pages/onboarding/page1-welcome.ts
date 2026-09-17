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

export function onboardingOneProvider(store: Store<GridAppStateKeyed>): OgPageProvider<AppStateKeyed> {
  return {
    name: "onboarding-1",
    routes: ["onboarding-1"],
    hideHeader: true,
    pageCreator: () => new Page1Welcome(store),
    skipDataCheck: true,
  };
}

const styling = css`
  #welcome-container {
    margin-top: 32px;
    display: flex;
    flex-direction: column;
    text-align: center;
    gap: 32px;
    pointer-events: none;
    padding: 16px 32px;
  }
`;

@customElement("page1-welcome")
export class Page1Welcome extends OgPage<GridAppStateKeyed> {
  static get styles() {
    return [...super.styles, styling];
  }

  get name(): string {
    return "Onboarding 1/3";
  }

  protected render(): TemplateResult {
    return html`
      <div id="welcome-container">
        <span class="text-primary">${i18next.t("onboarding.introText1")}</span>
        <span class="text-primary">${i18next.t("onboarding.introText2")}</span>
      </div>
    `;
  }
}
