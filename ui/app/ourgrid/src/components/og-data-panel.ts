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
import { customElement, property } from "lit/decorators.js";
import { OgPanel } from "./og-panel";
import type { Asset, User } from "@openremote/model";

@customElement("og-data-panel")
export abstract class OgDataPanel extends OgPanel {
  @property({ type: Object })
  protected user: User;

  @property({ type: Object })
  protected meterAsset: Asset;

  @property({ type: Object })
  protected batteryAsset: Asset;

  @property({ type: Object })
  protected vehicleAsset: Asset;

  @property({ type: Object })
  protected challengeAsset: Asset;

  @property({ type: Object })
  protected districtAsset: Asset;

  @property({ type: Object })
  protected peakPointsAsset: Asset;

  public setUser(user: User): this {
    this.user = user;
    return this;
  }

  public setMeterAsset(asset: Asset): this {
    this.meterAsset = asset;
    return this;
  }

  public setBatteryAsset(asset: Asset): this {
    this.batteryAsset = asset;
    return this;
  }

  public setVehicleAsset(asset: Asset): this {
    this.vehicleAsset = asset;
    return this;
  }

  public setChallengeAsset(asset: Asset): this {
    this.challengeAsset = asset;
    return this;
  }

  public setDistrictAsset(asset: Asset): this {
    this.districtAsset = asset;
    return this;
  }

  public setPeakPointsAsset(asset: Asset): this {
    this.peakPointsAsset = asset;
    return this;
  }
}
