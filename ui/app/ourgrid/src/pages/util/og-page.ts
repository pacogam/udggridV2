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
import { type AppStateKeyed, Page, type PageProvider } from "@openremote/or-app";
import { css } from "lit";
import { getAppStyle } from "../../styles";
import { doAnimation } from "../../util/util";

export class OgPageProvider<S extends AppStateKeyed> implements PageProvider<S> {
  name: string;
  routes: string[];
  allowOffline?: boolean; // allow use during offline/disconnected state. Default is false.
  hideHeader?: boolean;
  skipDataCheck?: boolean;
  pageCreator: () => OgPage<S>;
}

export enum PageAnimationType {
  SWIPE_RIGHT = "SWIPE_RIGHT",
  SWIPE_LEFT = "SWIPE_LEFT",
  FADE = "FADE",
  SLOW_FADE = "SLOW_FADE",
}

const styling = css`
  /*:host {
    display: block;
    -webkit-animation: container-enter 300ms cubic-bezier(0.0, 0.0, 0.2, 1);
    -webkit-animation-fill-mode: forwards;
    animation: container-enter 300ms cubic-bezier(0.0, 0.0, 0.2, 1);
    animation-fill-mode: forwards;
  }*/
`;

export abstract class OgPage<S extends AppStateKeyed> extends Page<S> {
  protected readonly getAnimationEnterType: (oldPage?: string) => PageAnimationType = () => PageAnimationType.FADE;
  protected readonly getAnimationExitType: (newPage?: string) => PageAnimationType = () => PageAnimationType.FADE;

  abstract get name(): string;

  async getLoadingPromise(prev?: string): Promise<void> {}

  static get styles(): any[] {
    return [getAppStyle(), styling];
  }

  stateChanged(_state: AppStateKeyed): void {
    // nothing here
  }

  public async doEnterAnimation(type?: PageAnimationType, oldPage?: string): Promise<void> {
    type = type || this.getAnimationEnterType(oldPage);
    await this.updateComplete;
    const elem = this.getAnimationElem();
    if (elem) {
      console.debug(`Starting page enter animation '${type}' for page '${this.name}'`);
      switch (type) {
        case PageAnimationType.SWIPE_LEFT: {
          await doAnimation(elem, "animate-swipeleft-enter", 300);
          break;
        }
        case PageAnimationType.SWIPE_RIGHT: {
          await doAnimation(elem, "animate-swiperight-enter", 300);
          break;
        }
        case PageAnimationType.FADE: {
          await doAnimation(elem, "animate-fade-enter", 200);
          break;
        }
        case PageAnimationType.SLOW_FADE: {
          await doAnimation(elem, "animate-slowfade-enter", 200);
          break;
        }
        default: {
          await doAnimation(elem, "animate-fade-enter", 200);
          break;
        }
      }
      console.debug(`Finished page enter animation '${type}' for page '${this.name}'`);
    }
  }

  public async doExitAnimation(type?: PageAnimationType, newPage?: string): Promise<void> {
    type = type || this.getAnimationExitType(newPage);
    await this.updateComplete;
    const elem = this.getAnimationElem();
    if (elem) {
      console.debug(`Starting page exit animation '${type}' for page '${this.name}'`);
      switch (type) {
        case PageAnimationType.SWIPE_LEFT: {
          await doAnimation(elem, "animate-swipeleft-exit", 300);
          break;
        }
        case PageAnimationType.SWIPE_RIGHT: {
          await doAnimation(elem, "animate-swiperight-exit", 300);
          break;
        }
        case PageAnimationType.FADE: {
          await doAnimation(elem, "animate-fade-exit", 100);
          break;
        }
        case PageAnimationType.SLOW_FADE: {
          await doAnimation(elem, "animate-slowfade-exit", 100);
          break;
        }
        default: {
          await doAnimation(elem, "animate-fade-exit", 100);
          break;
        }
      }
      console.debug(`Finished page exit animation '${type}' for page '${this.name}'`);
    }
  }

  protected getAnimationElem(): Element | undefined {
    const elem = this.shadowRoot?.firstElementChild;
    if (!elem) {
      console.error("Could not find animation element!");
    }
    return elem;
  }
}
