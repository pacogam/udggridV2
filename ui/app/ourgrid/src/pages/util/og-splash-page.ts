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
import { type TemplateResult, html, css } from "lit";
import { state } from "lit/decorators.js";
import { when } from "lit/directives/when.js";
import { classMap } from "lit/directives/class-map.js";
import { styleMap } from "lit/directives/style-map.js";
import { guard } from "lit/directives/guard.js";
import type { GridAppStateKeyed } from "../../util/og-state";
import { i18next } from "@openremote/or-translate";
import { OgPage } from "./og-page";
import { GraphicType } from "../../features/og-usage-graphic";

export enum SplashStatus {
  LOADING,
  FAILED,
  SUCCESS,
}

const styling = css`
  og-usage-graphic {
    height: 100%;
    width: 100%;
    transition: opacity 1s;
    -webkit-transition: opacity 1s;
    -webkit-animation: fade-in-fwd 0.6s cubic-bezier(0.39, 0.575, 0.565, 1) both;
    animation: fade-in-fwd 0.6s cubic-bezier(0.39, 0.575, 0.565, 1) both;
  }

  .loading-container {
    background: var(--og-color-primary);
    max-width: 50vw;
    min-height: 55vh;
    display: flex;
    flex-direction: column;
    align-items: center;
    text-align: center;
    padding: 16px 32px 32px 32px;
    gap: 32px;
  }

  .loading-container--dark {
    background: var(--og-color-primary-dark);
  }

  #loading-content {
    flex: 1;
    width: 100%;
    display: flex;
    flex-direction: column;
    justify-content: center;
    align-items: center;
    gap: 32px;
  }

  #loading-actions-container {
    width: 100%;
    display: flex;
    flex-direction: column;
    gap: 8px;
  }

  #text-title {
    -webkit-animation: fade-in-fwd 0.3s cubic-bezier(0.39, 0.575, 0.565, 1) 0.2s both;
    animation: fade-in-fwd 0.3s cubic-bezier(0.39, 0.575, 0.565, 1) 0.2s both;
  }

  #loading-indicator {
    -webkit-animation: fade-in-fwd 1.2s cubic-bezier(0.39, 0.575, 0.565, 1) 0.4s both;
    animation: fade-in-fwd 1.2s cubic-bezier(0.39, 0.575, 0.565, 1) 0.4s both;
  }

  #text-loading {
    text-align: center;
    -webkit-animation: fade-in-fwd 0.3s cubic-bezier(0.39, 0.575, 0.565, 1) 0.6s both;
    animation: fade-in-fwd 0.3s cubic-bezier(0.39, 0.575, 0.565, 1) 0.6s both;
  }

  #loading-button {
    -webkit-animation: fade-in-fwd 0.6s cubic-bezier(0.39, 0.575, 0.565, 1) 0.8s both;
    animation: fade-in-fwd 0.6s cubic-bezier(0.39, 0.575, 0.565, 1) 0.8s both;
  }

  #logout-button {
    -webkit-animation: fade-in-fwd 0.6s cubic-bezier(0.39, 0.575, 0.565, 1) 1s both;
    animation: fade-in-fwd 0.6s cubic-bezier(0.39, 0.575, 0.565, 1) 1s both;
  }
`;

export abstract class OgSplashPage extends OgPage<GridAppStateKeyed> {
  abstract get name();

  @state()
  protected dark = true;

  @state()
  protected status: SplashStatus = SplashStatus.LOADING;

  @state() // error/status text shown on the page (not translated)
  protected statusText?: string;

  abstract stateChanged(state: GridAppStateKeyed): void;

  abstract getActionsTemplate(): TemplateResult;

  static get styles() {
    return [...super.styles, styling];
  }

  protected render(): TemplateResult {
    const fullscreenClasses = {
      "fullscreen-container": true,
      "fullscreen-container--dark": this.dark,
    };
    const backgroundClasses = {
      "loading-container": true,
      "loading-container--dark": this.dark,
    };
    const titleClasses = {
      "text-title": true,
      dark: this.dark,
    };
    const statusStyles = {
      color: this.dark ? "var(--og-color-primary)" : undefined,
    };
    return html`
      ${guard([this.dark, this.status, this.statusText], () => {
        return html`
          <div class="${classMap(fullscreenClasses)}">
            <og-usage-graphic
              .type="${GraphicType.HEADER_FOOTER}"
              fill="${true}"
              .dark="${this.dark}"
            ></og-usage-graphic>

            <div style="position: absolute;">
              <div id="loading-container" class="${classMap(backgroundClasses)}">
                <!-- OurGrid title -->
                <span id="text-title" class="${classMap(titleClasses)}">${i18next.t("appName")}</span>

                <!-- Other content such as loading indicator, status text, and retry button -->
                <div id="loading-content">
                  ${when(
                    this.status === SplashStatus.LOADING || this.status === SplashStatus.SUCCESS,
                    () => html` <og-loading id="loading-indicator" .dark="${this.dark}"></og-loading> `
                  )}
                  ${when(
                    this.statusText,
                    () => html` <span id="text-loading" style="${styleMap(statusStyles)}">${this.statusText}</span> `
                  )}
                  ${when(
                    this.status === SplashStatus.FAILED,
                    () => html`
                      <div><!-- Extra whitespace --></div>
                      <div id="loading-actions-container">${this.getActionsTemplate()}</div>
                    `
                  )}
                </div>
              </div>
            </div>
          </div>
        `;
      })}
    `;
  }
}
