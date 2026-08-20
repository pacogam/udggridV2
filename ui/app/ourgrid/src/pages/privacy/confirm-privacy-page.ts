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
import { customElement } from "lit/decorators.js";
import { OgOnboardingPage, type OnboardPage } from "../util/og-onboarding-page";
import { html, type TemplateResult } from "lit";
import type { GridAppStateKeyed } from "../../util/og-state";
import type { OgPageProvider } from "../util/og-page";
import { type AppStateKeyed, router } from "@openremote/or-app";
import type { Store } from "@reduxjs/toolkit";

export function pageConfirmPrivacyProvider(store: Store<GridAppStateKeyed>): OgPageProvider<AppStateKeyed> {
  return {
    name: "confirm-privacy",
    routes: ["confirm-privacy"],
    hideHeader: true,
    pageCreator: () => new ConfirmPrivacyPage(store),
    skipDataCheck: true,
  };
}

@customElement("confirm-privacy-page")
export class ConfirmPrivacyPage extends OgOnboardingPage {
  get name(): string {
    return "Privacy Statement confirmation";
  }

  protected pages: OnboardPage[] = [
    {
      getHeading: () => "page-privacy.heading",
      getActionText: () => "page-privacy.accept",
      noBottomGraphic: true,
      noPadding: true,
      pageContent: (): TemplateResult => {
        return html`
          <div style="height: 100%;">
            <span class="text-secondary">
              <or-translate
                value="privacy-statement"
                style="white-space: pre-line; margin-bottom: 80px; text-align: left;"
              ></or-translate>
            </span>
          </div>
        `;
      },
    },
  ];

  protected onActionClick(_index: number) {
    window.localStorage.setItem("acceptedPrivacy", "1");
    router.navigate("home");
  }
}
