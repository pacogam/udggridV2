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
import { OgPage, type OgPageProvider, PageAnimationType } from "./util/og-page";
import type { GridAppStateKeyed } from "../util/og-state";
import { customElement } from "lit/decorators.js";
import { html, css, type TemplateResult } from "lit";
import "../panels/panel-account-password";
import type { Store } from "@reduxjs/toolkit";
import { GraphicType } from "../features/og-usage-graphic";
import { i18next } from "@openremote/or-translate";
import { router } from "@openremote/or-app";

export function pageAccountPasswordProvider(store: Store<GridAppStateKeyed>): OgPageProvider<GridAppStateKeyed> {
  return {
    name: "account-password",
    routes: ["account-password"],
    pageCreator: () => new PageAccountPassword(store),
    skipDataCheck: true,
  };
}

const styling = css`
  .page-wrapper {
    width: 100%;
    height: 100vh;
    background: var(--og-color-primary);
    display: flex;
    flex-direction: column;
    justify-content: space-between;
  }
`;

@customElement("page-account-password")
export class PageAccountPassword extends OgPage<GridAppStateKeyed> {
  getAnimationEnterType = () => PageAnimationType.SWIPE_LEFT;
  getAnimationExitType = () => PageAnimationType.SWIPE_RIGHT;

  get name(): string {
    return "account-password";
  }

  static get styles() {
    return [...super.styles, styling];
  }

  protected render(): TemplateResult {
    return html`
      <div class="page-wrapper">
        <div
          style="width: calc(100% - 32px); padding: 16px; display: flex; flex-direction: column; align-items: center; margin-bottom: 7.5%;"
        >
          <og-usage-graphic
            id="onboarding-topgraphic"
            .type="${GraphicType.HEADER}"
            style="width: 100%;"
          ></og-usage-graphic>
          <span class="text-heading" style="margin-top: -10%; text-align: center; max-width: 65vw;"
            >${i18next.t("page-account.changePassword")}</span
          >
        </div>
        <div style="flex: 1;">
          <panel-account-password fullWidth="${true}" .fullHeight="${true}"></panel-account-password>
        </div>
        <div style="padding: 16px;">
          <div style="display: flex; justify-content: center;">
            <a style="text-decoration: underline;" @click="${() => router.navigate("account")}">Back to Account page</a>
          </div>
          <og-usage-graphic id="onboarding-topgraphic" .type="${GraphicType.FOOTER}"></og-usage-graphic>
        </div>
      </div>
    `;
  }
}
