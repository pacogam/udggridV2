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
import { OrMwcList } from "@openremote/or-mwc-components/or-mwc-list";
import { customElement } from "lit/decorators.js";
import { getAppStyle } from "../styles";
import { css } from "lit";

const styling = css`
  .mdc-list {
    font-family: var(--og-font-family);
    font-size: var(--og-font-size-secondary);
    font-weight: var(--og-font-weight-primary);
    color: var(--og-color-primary-dark);
    max-height: var(--og-city-list-max-height, 35vh);
    overflow: hidden auto;
    padding: 8px;
  }
`;

@customElement("og-city-list")
export class OgCityList extends OrMwcList {
  static get styles() {
    return [...super.styles, getAppStyle(), styling] as any;
  }
}
