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
import { OgPanel } from "../components/og-panel";
import { type TemplateResult, html } from "lit";
import { customElement } from "lit/decorators.js";

@customElement("panel-privacy-statement")
export class PanelPrivacyStatement extends OgPanel {
  protected async getPanelContent(): Promise<TemplateResult> {
    return html`
      <div style="margin: 0 8px 64px 8px">
        <span class="text-secondary">
          <or-translate value="privacy-statement" style="white-space: pre-line; text-align: left;"></or-translate>
        </span>
      </div>
    `;
  }
}
