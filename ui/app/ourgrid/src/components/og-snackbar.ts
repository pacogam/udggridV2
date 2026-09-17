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
import { OrMwcSnackbar, OrMwcSnackbarChangedEvent } from "@openremote/or-mwc-components/or-mwc-snackbar";
import { css } from "lit";
import { getAppStyle } from "../styles";
import { customElement } from "lit/decorators.js";

export function showSnackbar(
  hostElement: HTMLElement | undefined,
  text: string,
  buttonText?: string,
  buttonAction?: () => void
): OgSnackbar {
  if (!hostElement) {
    hostElement = OgSnackbar.DialogHostElement || document.body;
  }

  const snackbar = new OgSnackbar();
  snackbar.text = text;
  snackbar.buttonText = buttonText;
  snackbar.buttonAction = buttonAction;
  snackbar.isOpen = true;
  snackbar.addEventListener(OrMwcSnackbarChangedEvent.NAME, (ev: OrMwcSnackbarChangedEvent) => {
    ev.stopPropagation();
    if (!ev.detail.opened) {
      window.setTimeout(() => {
        if (snackbar.parentElement) {
          snackbar.parentElement.removeChild(snackbar);
        }
      }, 0);
    }
  });
  hostElement.append(snackbar);
  return snackbar;
}

const styling = css`
  .mdc-snackbar {
    z-index: 50;
  }
`;

@customElement("og-snackbar")
export class OgSnackbar extends OrMwcSnackbar {
  static get styles() {
    return [...super.styles, getAppStyle(), styling];
  }
}
