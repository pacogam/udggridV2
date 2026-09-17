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
import { CountUp } from "countup.js";
import { html, LitElement, type PropertyValues } from "lit";
import { customElement, property, queryAssignedElements } from "lit/decorators.js";

@customElement("og-statistic")
export class OgStatistic extends LitElement {
  @property()
  public value?: number;

  @queryAssignedElements()
  protected slotElems?: HTMLElement[];

  protected countObj?: CountUp;

  // shouldUpdate lifecycle function
  // We cancel UI update when only the value has changed, since we let the CountUp library to process it.
  protected shouldUpdate(changedProps: PropertyValues): boolean {
    if (changedProps.size === 1 && this.countObj) {
      if (changedProps.has("value") && this.value !== undefined) {
        this.countObj.update(this.value);
        return false;
      }
    }
    return super.shouldUpdate(changedProps);
  }

  // After first update lifecycle function
  protected firstUpdated(changedProps: PropertyValues) {
    super.firstUpdated(changedProps);
    this.initCounter();
  }

  protected initCounter() {
    if (this.slotElems && this.slotElems.length > 0) {
      this.countObj = new CountUp(this.slotElems[0], this.value || 0);
      if (!this.countObj.error) {
        this.countObj.start();
      } else {
        console.error(this.countObj.error);
      }
    } else {
      console.error("Could not initialize counter. No slot element could be found!");
    }
  }

  protected render() {
    return html`
      <div id="count-wrapper">
        <slot id="count-elem"></slot>
      </div>
    `;
  }
}
