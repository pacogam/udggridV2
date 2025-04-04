import {AppStateKeyed} from '@openremote/or-app';
import {Asset, AttributeEvent, User} from '@openremote/model';
import {createSlice, PayloadAction} from '@reduxjs/toolkit';
import manager, {Util} from '@openremote/core';

export interface GridAppState {
    language?: string;
    user?: User;
    dark?: boolean;
    assets: Asset[];
    userAssetId?: string;
    batteryAssetId?: string;
    districtAssetId?: string;
    challengeAssetId?: string;
    peakPointsAssetId?: string;
}

export interface GridAppStateKeyed extends AppStateKeyed {
    gridApp: GridAppState;
}

const INITIAL_STATE: GridAppState = {
    assets: []
};

const gridAppSlice = createSlice({
    name: 'ourGridApp',
    initialState: INITIAL_STATE,
    reducers: {
        attributeEventReceived(state: GridAppState, action: PayloadAction<AttributeEvent>) {
            console.debug("Received an attribute event;", action.payload);
            const assets = state.assets;
            const attrEvent = action.payload;
            const attrName = attrEvent.ref.name;
            const assetId = attrEvent.ref.id;
            const index = assets.findIndex(asst => asst.id === assetId);
            const asset = index >= 0 ? assets[index] : null;

            if (!asset) {
                return state;
            }

            assets[index] = Util.updateAsset({...asset}, attrEvent);
            return state;
        },
        setLanguage(state, action: PayloadAction<string>) {
            return {
                ...state,
                language: action.payload
            };
        },
        setDark(state, action: PayloadAction<boolean>) {
            return {
                ...state,
                dark: action.payload
            };
        },
        setUserData(state, action: PayloadAction<User>) {
            return {
                ...state,
                user: action.payload
            };
        },
        setUserAsset(state, action: PayloadAction<Asset>) {
            return {
                ...state,
                assets: [...state.assets, action.payload],
                userAssetId: action.payload.id
            };
        },
        removeUserAsset(state) {
            return {
                ...state,
                assets: state.assets.filter(a => a.id === state.batteryAssetId),
                userAssetId: null
            };
        },
        setBatteryAsset(state, action: PayloadAction<Asset>) {
            return {
                ...state,
                assets: [...state.assets, action.payload],
                batteryAssetId: action.payload.id
            };
        },
        removeBatteryAsset(state) {
            return {
                ...state,
                assets: state.assets.filter(a => a.id === state.batteryAssetId),
                batteryAssetId: null
            };
        },
        setDistrictAsset(state, action: PayloadAction<Asset>) {
            return {
                ...state,
                assets: [...state.assets, action.payload],
                districtAssetId: action.payload.id
            };
        },
        removeDistrictAsset(state) {
            return {
                ...state,
                assets: state.assets.filter(a => a.id === state.districtAssetId),
                districtAssetId: null
            };
        },
        setChallengeAsset(state, action: PayloadAction<Asset>) {
            return {
                ...state,
                assets: [...state.assets, action.payload],
                challengeAssetId: action.payload.id
            }
        },
        removeChallengeAsset(state) {
            return {
                ...state,
                assets: state.assets.filter(a => a.id === state.challengeAssetId),
                challengeAssetId: null
            }
        },
        setPeakPointsAsset(state, action: PayloadAction<Asset>) {
            return {
                ...state,
                assets: [...state.assets, action.payload],
                peakPointsAssetId: action.payload.id
            }
        },
        removePeakPointsAsset(state) {
            return {
                ...state,
                assets: state.assets.filter(a => a.id === state.peakPointsAssetId),
                peakPointsAssetId: null
            }
        },
        removeAllAssets(state) {
            return {
                ...state,
                assets: []
            };
        }
    }
});
export const gridAppReducer = gridAppSlice.reducer;

export const {
    attributeEventReceived,
    setLanguage,
    setDark,
    setUserData,
    setUserAsset,
    removeUserAsset,
    setBatteryAsset,
    removeBatteryAsset,
    setDistrictAsset,
    removeDistrictAsset,
    setChallengeAsset,
    removeChallengeAsset,
    setPeakPointsAsset,
    removePeakPointsAsset,
    removeAllAssets
} = gridAppSlice.actions;

export const realmSelector = (state: GridAppStateKeyed) => state.app?.realm || manager.displayRealm;
export const assetsSelector = (state: GridAppStateKeyed) => state.gridApp.assets;
export const userAssetIdSelector = (state: GridAppStateKeyed) => state.gridApp.userAssetId;
export const batteryAssetIdSelector = (state: GridAppStateKeyed) => state.gridApp.batteryAssetId;
export const districtAssetIdSelector = (state: GridAppStateKeyed) => state.gridApp.districtAssetId;
export const challengeAssetIdSelector = (state: GridAppStateKeyed) => state.gridApp.challengeAssetId;
export const peakPointsAssetIdSelector = (state: GridAppStateKeyed) => state.gridApp.peakPointsAssetId;
