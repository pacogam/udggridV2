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
import { type TemplateResult, html, css } from "lit";
import { i18next } from "@openremote/or-translate";
import { OgOnboardingPage, type OnboardPage } from "../util/og-onboarding-page";
import type { GridAppStateKeyed } from "../../util/og-state";
import type { OgPageProvider } from "../util/og-page";
import type { AppStateKeyed } from "@openremote/or-app";
import type { Store } from "@reduxjs/toolkit";

const page1Vid = require("../../../images/graphlines1-onboarding.mp4");
const page2Vid = require("../../../images/graphlines2-onboarding.mp4");
const page3Vid = require("../../../images/graphlines3-onboarding.mp4");

export function onboardingTwoProvider(store: Store<GridAppStateKeyed>): OgPageProvider<AppStateKeyed> {
  return {
    name: "onboarding-2",
    routes: ["onboarding-2"],
    hideHeader: true,
    pageCreator: () => new Page2PeakUsage(store),
    skipDataCheck: true,
  };
}

const styling = css`
  #onboarding-content {
    padding: 32px 0 !important;
    width: 100% !important;
  }
  .peakusage-container {
    height: calc(100% - 32px);
    text-align: center;
    display: flex;
    flex-direction: column;
    justify-content: space-evenly;
    padding: 16px;
  }
`;

@customElement("page2-peakusage")
export class Page2PeakUsage extends OgOnboardingPage {
  protected dots = true;

  protected gesture = true;

  static get styles() {
    return [...super.styles, styling];
  }

  get name(): string {
    return "Onboarding 2/3";
  }

  protected render(): any {
    return super.render();
  }

  protected pages: OnboardPage[] = [
    {
      noTopGraphic: true,
      noBottomGraphic: true,
      getActionText: () => i18next.t("continue"),
      pageContent: (): TemplateResult => html`
        <div class="peakusage-container">
          <div style="overflow: hidden;">
            <video
              autoplay
              muted
              playsinline
              loop
              poster="images/graphlines1-onboarding-frame.jpg"
              style="width: 100%; margin: -30% 0;"
            >
              <source src="${page1Vid}" type="video/mp4" />
            </video>
          </div>
          <!--<img src="images/graphlines1-onboarding.svg" style="width: 100%;" />-->
          <span class="text-primary">${i18next.t("onboarding.page2-text1")}</span>
        </div>
      `,
    },
    {
      noTopGraphic: true,
      noBottomGraphic: true,
      getActionText: () => i18next.t("continue"),
      pageContent: (): TemplateResult => html`
        <div class="peakusage-container">
          <div style="overflow: hidden;">
            <video autoplay muted playsinline loop style="width: 100%; margin: -15% 0;">
              <source src="${page2Vid}" type="video/mp4" />
            </video>
          </div>
          <!--<img src="images/graphlines2-onboarding.svg" style="width: 100%;" />-->
          <span class="text-primary">${i18next.t("onboarding.page2-text2")}</span>
        </div>
      `,
    },
    {
      noTopGraphic: true,
      noBottomGraphic: true,
      getActionText: () => i18next.t("continue"),
      pageContent: (): TemplateResult => html`
        <div class="peakusage-container">
          <div style="overflow: hidden;">
            <video
              autoplay
              muted
              playsinline
              loop
              poster="images/graphlines3-onboarding-frame.jpg"
              style="width: 100%; margin: -15% 0;"
            >
              <source src="${page3Vid}" type="video/mp4" />
            </video>
          </div>
          <!--<img src="images/graphlines3-onboarding.svg" style="width: 100%;" />-->
          <span class="text-primary">${i18next.t("onboarding.page2-text3")}</span>
        </div>
      `,
    },
  ];

  protected onActionClick(index: number): void {
    if (index < 2) {
      this.switchPage("next");
    } else {
      this.dispatchEvent(new CustomEvent("finish"));
    }
  }
}
