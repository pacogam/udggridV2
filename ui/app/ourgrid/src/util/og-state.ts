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
import type { AppStateKeyed } from "@openremote/or-app";
import type { Asset, AttributeEvent, User } from "@openremote/model";
import { createSlice, type PayloadAction } from "@reduxjs/toolkit";
import manager, { Util } from "@openremote/core";

export interface GridAppState {
  language?: string;
  user?: User;
  serviceUser?: User;
  dark?: boolean;
  assets: Asset[];
  userAssetId?: string;
  batteryAssetId?: string;
  vehicleAssetId?: string;
  districtAssetId?: string;
  challengeAssetId?: string;
  peakPointsAssetId?: string;
}

export interface GridAppStateKeyed extends AppStateKeyed {
  gridApp: GridAppState;
}

const INITIAL_STATE: GridAppState = {
  assets: [],
};

const gridAppSlice = createSlice({
  name: "ourGridApp",
  initialState: INITIAL_STATE,
  reducers: {
    attributeEventReceived(state: GridAppState, action: PayloadAction<AttributeEvent>) {
      console.debug("Received an attribute event;", action.payload);
      const assets = state.assets;
      const attrEvent = action.payload;
      const attrName = attrEvent.ref.name;
      const assetId = attrEvent.ref.id;
      const index = assets.findIndex((asst) => asst.id === assetId);
      const asset = index >= 0 ? assets[index] : null;

      if (!asset) {
        return state;
      }

      assets[index] = Util.updateAsset({ ...asset }, attrEvent);
      return state;
    },
    setLanguage(state, action: PayloadAction<string>) {
      return {
        ...state,
        language: action.payload,
      };
    },
    setDark(state, action: PayloadAction<boolean>) {
      return {
        ...state,
        dark: action.payload,
      };
    },
    setUserData(state, action: PayloadAction<User>) {
      return {
        ...state,
        user: action.payload,
      };
    },
    setServiceUserData(state, action: PayloadAction<User>) {
      return {
        ...state,
        serviceUser: action.payload,
      };
    },
    setUserAsset(state, action: PayloadAction<Asset>) {
      return {
        ...state,
        assets: [...state.assets, action.payload],
        userAssetId: action.payload.id,
      };
    },
    removeUserAsset(state) {
      return {
        ...state,
        assets: state.assets.filter((a) => a.id !== state.userAssetId),
        userAssetId: null,
      };
    },
    setBatteryAsset(state, action: PayloadAction<Asset>) {
      return {
        ...state,
        assets: [...state.assets, action.payload],
        batteryAssetId: action.payload.id,
      };
    },
    removeBatteryAsset(state) {
      return {
        ...state,
        assets: state.assets.filter((a) => a.id !== state.batteryAssetId),
        batteryAssetId: null,
      };
    },
    setVehicleAsset(state, action: PayloadAction<Asset>) {
      return {
        ...state,
        assets: [...state.assets, action.payload],
        vehicleAssetId: action.payload.id,
      };
    },
    removeVehicleAsset(state) {
      return {
        ...state,
        assets: state.assets.filter((a) => a.id !== state.vehicleAssetId),
        vehicleAssetId: null,
      };
    },
    setDistrictAsset(state, action: PayloadAction<Asset>) {
      return {
        ...state,
        assets: [...state.assets, action.payload],
        districtAssetId: action.payload.id,
      };
    },
    removeDistrictAsset(state) {
      return {
        ...state,
        assets: state.assets.filter((a) => a.id !== state.districtAssetId),
        districtAssetId: null,
      };
    },
    setChallengeAsset(state, action: PayloadAction<Asset>) {
      return {
        ...state,
        assets: [...state.assets, action.payload],
        challengeAssetId: action.payload.id,
      };
    },
    removeChallengeAsset(state) {
      return {
        ...state,
        assets: state.assets.filter((a) => a.id !== state.challengeAssetId),
        challengeAssetId: null,
      };
    },
    setPeakPointsAsset(state, action: PayloadAction<Asset>) {
      return {
        ...state,
        assets: [...state.assets, action.payload],
        peakPointsAssetId: action.payload.id,
      };
    },
    removePeakPointsAsset(state) {
      return {
        ...state,
        assets: state.assets.filter((a) => a.id !== state.peakPointsAssetId),
        peakPointsAssetId: null,
      };
    },
    removeAllAssets(state) {
      return {
        ...state,
        assets: [],
      };
    },
  },
});
export const gridAppReducer = gridAppSlice.reducer;

export const {
  attributeEventReceived,
  setLanguage,
  setDark,
  setUserData,
  setServiceUserData,
  setUserAsset,
  removeUserAsset,
  setBatteryAsset,
  removeBatteryAsset,
  setVehicleAsset,
  removeVehicleAsset,
  setDistrictAsset,
  removeDistrictAsset,
  setChallengeAsset,
  removeChallengeAsset,
  setPeakPointsAsset,
  removePeakPointsAsset,
  removeAllAssets,
} = gridAppSlice.actions;

export const realmSelector = (state: GridAppStateKeyed) => state.app?.realm || manager.displayRealm;
export const assetsSelector = (state: GridAppStateKeyed) => state.gridApp.assets;
export const userAssetIdSelector = (state: GridAppStateKeyed) => state.gridApp.userAssetId;
export const batteryAssetIdSelector = (state: GridAppStateKeyed) => state.gridApp.batteryAssetId;
export const districtAssetIdSelector = (state: GridAppStateKeyed) => state.gridApp.districtAssetId;
export const challengeAssetIdSelector = (state: GridAppStateKeyed) => state.gridApp.challengeAssetId;
export const peakPointsAssetIdSelector = (state: GridAppStateKeyed) => state.gridApp.peakPointsAssetId;
