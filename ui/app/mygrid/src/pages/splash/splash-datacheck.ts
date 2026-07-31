import {customElement} from "lit/decorators.js";
import {html, TemplateResult} from "lit";
import {Asset, ClientRole, User} from "@openremote/model";
import {isAxiosError} from "@openremote/rest";
import {i18next} from "@openremote/or-translate";
import manager from "@openremote/core";
import {GridAppStateKeyed, setBatteryAsset, setChallengeAsset, setDistrictAsset, setPeakPointsAsset, setUserAsset, setUserData, setVehicleAsset} from "../../util/og-state";
import {InputType} from "@openremote/or-mwc-components/or-mwc-input";
import {Store} from "@reduxjs/toolkit";
import {OgPageProvider} from "../util/og-page";
import {OgSplashPage, SplashStatus} from "../util/og-splash-page";
import rest from "rest";

export class NeedsOnboardingError extends Error {}

export class RequiresPrivacyConfirmationError extends Error {}

export class NoUserDataError extends Error {}

export class NoAssetLinkedError extends Error {}

export class NoDistrictAssetError extends Error {}

export class NoChallengeAssetError extends Error {}

export class NoPeakPointsAssetError extends Error {}

export function splashDataCheckProvider(store: Store<GridAppStateKeyed>): OgPageProvider<GridAppStateKeyed> {
    return {
        name: "splash-datacheck",
        routes: [],
        hideHeader: true,
        pageCreator: () => new SplashDatacheck(store)
    };
}

const MINIMUM_WAIT = 1500; // 1.5 seconds of wait

@customElement("page-splash-datacheck")
export class SplashDatacheck extends OgSplashPage {

    // Static asset name to use when fetching district asset
    /*protected RESCHOOL_DISTRICT_NAME = "Sporenburg";*/

    // Objects used during check
    protected user?: User;
    protected userAsset?: Asset;
    protected batteryAsset?: Asset;
    protected vehicleAsset?: Asset;
    protected districtAsset?: Asset;
    protected challengeAsset?: Asset;
    protected peakPointsAsset?: Asset;

    // State fields
    protected loadingPromise: Promise<void>;
    protected resolvePromise: (value: (void | PromiseLike<void>)) => void;
    protected rejectPromise: (reason?: any) => void;
    protected page: string;

    getLoadingPromise(): Promise<void> {
        return this.loadingPromise;
    }

    get name(): string {
        return "Checking data...";
    }

    connectedCallback() {
        super.connectedCallback();
        this.loadingPromise = new Promise<void>((resolve, reject) => {
            this.resolvePromise = resolve;
            this.rejectPromise = reject;
        });
        this.doChecks();
    }

    // Set user- and district asset based on cache if changed.
    stateChanged(state: GridAppStateKeyed): void {
        this.user = state.gridApp.user;
        this.userAsset = state.gridApp.assets.find(a => a.id === state.gridApp.userAssetId);
        this.batteryAsset = state.gridApp.assets.find(a => a.id === state.gridApp.batteryAssetId);
        this.vehicleAsset = state.gridApp.assets.find(a => a.id === state.gridApp.vehicleAssetId);
        this.districtAsset = state.gridApp.assets.find(a => a.id === state.gridApp.districtAssetId);
        this.challengeAsset = state.gridApp.assets.find(a => a.id === state.gridApp.challengeAssetId);
        this.page = state.app.page;
    }

    willUpdate(changedProps: Map<string, any>) {
        super.willUpdate(changedProps);
        if(changedProps.has("status") && this.status === SplashStatus.SUCCESS) {
            this.resolvePromise();
        }
    }

    getActionsTemplate(): TemplateResult {
        return html`
            <og-input id="loading-button" style="width: 100%; --or-mwc-input-color: var(--og-color-danger)" .type="${InputType.BUTTON}" label="tryAgain"
                      fullWidth unElevated rounded @or-mwc-input-changed="${() => this.onTryAgain()}"
            ></og-input>
            <og-input id="logout-button" style="width: 100%; --or-mwc-input-color: var(--og-background-tint); --or-icon-fill: var(--og-color-secondary)"
                      .type="${InputType.BUTTON}" icon="logout" label="logout" fullWidth unElevated rounded
                      @or-mwc-input-changed="${() => manager.logout()}"
            ></og-input>
        `;
    }

    protected onTryAgain() {
        this.doChecks();
    }



    /* ------------------------------------------------- */


    // Initial fetching that tries if the user asset and the district asset can be fetched.
    // Depending on the result, it will show an error or redirect to a page (see willUpdate() method)
    protected async doChecks() {
        this.status = SplashStatus.LOADING;
        this.statusText = null;

        if(!manager.authenticated) {
            manager.login();
        }

        // Execute necessary checks
        try {

            /*if(localStorage.getItem("completedOnboarding") == '0') {
                throw new NeedsOnboardingError("Requires onboarding");
            }*/

            if(localStorage.getItem("acceptedPrivacy") == null) {
                throw new RequiresPrivacyConfirmationError("Requires privacy confirmation");
            }

            const minWaitPromise = new Promise(resolve => setTimeout(resolve, MINIMUM_WAIT));

            const user = await this.checkUserData();
            await this.verifyUserRoles();
            const meterAsset = await this.checkUserAsset(user.id);
            await this.checkBatteryAsset(meterAsset.id);
            await this.checkVehicleAsset(user.id);
            await this.checkDistrictLink();
            const districtAsset = await this.checkDistrictAsset(user.id);
            const challengeAssetId = await this.checkChallengeAssetId(districtAsset);
            await this.checkChallengeAssetLink(challengeAssetId);
            await this.checkChallengeAsset(user.id, challengeAssetId);
            const peakPointsAssetId = await this.checkPeakPointsAssetId(districtAsset);
            await this.checkPeakPointsAssetLink(peakPointsAssetId);
            await this.checkPeakPointsAsset(user.id, peakPointsAssetId);

            // wait for minimum time to exceed. (will skip if already past MINIMUM_WAIT)
            await minWaitPromise;

            this.status = SplashStatus.SUCCESS;

        } catch (e) {
            console.warn(e);
            if(e instanceof NoAssetLinkedError || e instanceof NeedsOnboardingError || e instanceof RequiresPrivacyConfirmationError) {
                this.rejectPromise(e);
            } else {
                this.status = SplashStatus.FAILED;
            }
        }
    }


    /* ------------------------------------------------------ */
    // CHECK 1:     Fetch and check user data
    //              Making sure we have all user details such as username,
    //              first/last name and email address

    protected async checkUserData(): Promise<User> {
        try {
            return await this.fetchUserData();
        } catch (e) {
            if(isAxiosError(e)) {
                if(e.response.status === 403) {
                    this.statusText = i18next.t("error.userDataPermission");
                } else {
                    this.statusText = i18next.t("error.userDataFailed");
                }
            } else if(e instanceof NoUserDataError) {
                this.statusText = e.message;
            } else {
                this.statusText = i18next.t("error.unknown");
            }
            throw e;
        }
    }

    protected async fetchUserData(delay?: number): Promise<User> {
        if(!this.user) {
            const userData = (await manager.rest.api.UserResource.getCurrent()).data;
            if(!userData?.id) {
                throw new NoUserDataError(i18next.t("error.userDataFailed"));
            }
            // Add additional delay if necessary
            if(delay !== undefined) {
                await new Promise(resolve => setTimeout(resolve, delay));
            }
            // cache the user data
            this._store.dispatch(setUserData(userData));
            return userData;
        } else {
            return this.user;
        }
    }


    /* ----------------------------------------------------- */
    // CHECK 2:     Fetch and check the roles of the user
    //

    protected async verifyUserRoles(delay?: number): Promise<void> {
        const needsRestrictedRole = !manager.hasRealmRole("restricted_user");
        const needsReadAssetsRole = !manager.hasRole(ClientRole.READ_ASSETS);
        const needsWriteAttributesRole = !manager.hasRole(ClientRole.WRITE_ATTRIBUTES);
        if(needsRestrictedRole || needsReadAssetsRole || needsWriteAttributesRole) {
            try {
                await rest.api.UserRolesResource.verifyUserRoles();
                if(delay !== undefined) { await new Promise(resolve => setTimeout(resolve, delay)); }
            } catch (e) {
                if(isAxiosError(e)) {

                    // If 'verify roles request' responds with FORBIDDEN,
                    // correct the user roles, and wait 500ms to let the manager/keycloak process possible changes.
                    if(e.response.status === 403) {
                        await rest.api.UserRolesResource.correctUserRoles();
                        await new Promise(resolve => setTimeout(resolve, 250));

                    // If responding with 400, the user does not have access to OurGrid. (for example due to being in master realm)
                    } else if(e.response.status === 400) {
                        this.statusText = i18next.t("error.noOurGridAccess");
                        throw e;
                    }

                } else {
                    this.statusText = i18next.t("error.unknown");
                    throw e;
                }
            }
        }
    }


    /* ---------------------------------------- */
    // CHECK 3:     Fetch and check the linked asset of a user
    //              Which is the energy meter at the household, that is required
    //              for showing any data on the app.

    protected async checkUserAsset(userId: string): Promise<Asset> {
        try {
            return await this.fetchUserAsset(userId);
        } catch (e) {
            if(isAxiosError(e)) {
                if(e.response.status === 403) {
                    throw new NoAssetLinkedError(i18next.t("error.userAssetDataPermission"));
                } else {
                    this.statusText = i18next.t("error.userAssetDataFailed");
                }

            } else if(e instanceof NoAssetLinkedError) {
                this.statusText = e.message;

            } else {
                this.statusText = i18next.t("error.unknown");
            }
            throw e;
        }
    }

    protected async fetchUserAsset(userId: string, delay?: number): Promise<Asset> {
        if(!this.userAsset) {
            const assets = (await manager.rest.api.AssetResource.queryAssets({
                types: ["OurgridMeterAsset"],
                userIds: [userId],
                limit: 1
            })).data;
            if(assets === undefined || assets.length === 0) {
                throw new NoAssetLinkedError(i18next.t("error.userAssetFailed"));
            }
            // Add additional delay if necessary
            if(delay !== undefined) {
                await new Promise(resolve => setTimeout(resolve, delay));
            }
            // cache the asset and its ID.
            this._store.dispatch(setUserAsset(assets[0]));

            return assets[0];
        } else {
            return this.userAsset;
        }
    }


    /* ---------------------------------------- */
    // CHECK 4:     Fetch and check the linked battery asset of a user

    protected async checkBatteryAsset(meterId: string): Promise<Asset> {
        try {
            return await this.fetchBatteryAsset(meterId);
        } catch (e) {
            return;
        }
    }

    protected async fetchBatteryAsset(meterId: string, delay?: number): Promise<Asset> {
        if(!this.batteryAsset) {
            const asset = (await rest.api.DeviceBatteryResource.getBattery(meterId)).data;
            if(!asset) {
                return null;
            }

            // Add additional delay if necessary
            if(delay !== undefined) {
                await new Promise(resolve => setTimeout(resolve, delay));
            }
            // cache the asset and its ID.
            this._store.dispatch(setBatteryAsset(asset));

            return asset;
        } else {
            return this.batteryAsset;
        }
    }

    /* ---------------------------------------- */
    // CHECK 4:     Fetch and check the linked vehicle asset of a user

    protected async checkVehicleAsset(userId: string): Promise<Asset> {
        try {
            return await this.fetchVehicleAsset(userId);
        } catch (e) {
            return;
        }
    }

    protected async fetchVehicleAsset(userId: string, delay?: number): Promise<Asset> {
        if(!this.vehicleAsset) {
            const assets = (await rest.api.AssetResource.queryAssets({ userIds: [userId], types: ["OurgridVehicleAsset"], limit: 1 })).data;
            if(!assets || assets.length === 0) {
                return null;
            }

            // Add additional delay if necessary
            if(delay !== undefined) {
                await new Promise(resolve => setTimeout(resolve, delay));
            }
            // cache the asset and its ID.
            this._store.dispatch(setVehicleAsset(assets[0]));

            return assets[0];
        } else {
            return this.vehicleAsset;
        }
    }


    /* ---------------------------------------- */
    // CHECK 4:     Check the district link of the user

    protected async checkDistrictLink(delay?: number) {
        if(!this.districtAsset) {
            try {
                await rest.api.UserDistrictResource.verifyDistrict();
                if(delay !== undefined) { await new Promise(resolve => setTimeout(resolve, 250)); }
            } catch (e) {
                // If 'verify district request' responds with NOT_FOUND, no district is linked.
                // So, link the district, and wait 500ms to let the manager/keycloak process possible changes.
                if(isAxiosError(e) && e.response.status === 404) {
                    await this.linkDistrict(/*this.RESCHOOL_DISTRICT_NAME*/);
                    await new Promise(resolve => setTimeout(resolve, 250));
                } else {
                    this.statusText = i18next.t("error.unknown");
                    throw new Error("Unknown error during checking the district link");
                }
            }
        }
    }

    protected async linkDistrict(districtName?: string) {
        try {
            await rest.api.UserDistrictResource.linkDistrict({ assetName: districtName });
        } catch (e) {
            this.statusText = i18next.t("error.unknown");
            throw new Error("Unknown error when linking the district.");
        }
    }


    /* --------------------------------------------------------- */
    // CHECK 5:     Fetch and check the district asset linked to the user.
    //              This data is required for the app to function, and compare its household use
    //              with the district values.

    protected async checkDistrictAsset(userId: string): Promise<Asset> {
        try {
            return await this.fetchDistrictAsset(userId);
        } catch (e) {
            if(isAxiosError(e)) {
                if(e.response.status === 403) {
                    this.statusText = i18next.t("error.districtAssetDataPermission");
                } else {
                    this.statusText = i18next.t("error.districtAssetDataFailed");
                }
            } else if(e instanceof NoDistrictAssetError) {
                this.statusText = e.message;
            } else {
                this.statusText = i18next.t("error.unknown");
            }
            throw e;
        }
    }

    protected async fetchDistrictAsset(userId: string, delay = true): Promise<Asset> {
        if(!this.districtAsset) {
            const assets = (await manager.rest.api.AssetResource.queryAssets({
                types: ["OurgridDistrictAsset"],
                userIds: [userId],
                limit: 1
            })).data;
            if(assets === undefined || assets.length === 0) {
                throw new NoDistrictAssetError(i18next.t("error.districtAssetDataFailed"));
            }
            // Add additional delay if necessary
            if(delay) {
                await new Promise(resolve => setTimeout(resolve, 250));
            }
            // cache the asset and its ID.
            this._store.dispatch(setDistrictAsset(assets[0]));
            return assets[0];
        } else {
            return this.districtAsset;
        }
    }


    /* ---------------------------------------- */
    // CHECK 6:     Check the challenge link of the user

    protected async checkChallengeAssetId(districtAsset: Asset): Promise<string> {
        if(!this.challengeAsset) {
            if(districtAsset?.attributes) {
                const attr = districtAsset.attributes["challengesAssetId"];
                if(attr?.value) {
                    return attr.value;
                }
            }
            this.statusText = i18next.t("error.unknown");
            throw new Error("Unknown error during retrieval of the challenges.");
        } else {
            return this.challengeAsset.id;
        }
    }




    /* ---------------------------------------- */
    // CHECK 7:     Check the challenge link of the user

    protected async checkChallengeAssetLink(challengeAssetId: string, delay?: number) {
        if(!this.challengeAsset) {
            try {
                await rest.api.UserChallengesResource.verifyChallengesAsset();
                if(delay !== undefined) { await new Promise(resolve => setTimeout(resolve, delay)); }
            } catch (e) {
                // If 'verify challenge asset request' responds with NOT_FOUND, no challenge asset is linked.
                // So, link the challenge asset, and wait 500ms to let the manager/keycloak process possible changes.
                if(isAxiosError(e) && e.response.status === 404) {
                    await this.linkChallengeAsset(challengeAssetId);
                    await new Promise(resolve => setTimeout(resolve, 250));
                } else {
                    this.statusText = i18next.t("error.unknown");
                    throw new Error("Unknown error during checking the challenges.");
                }
            }
        }
    }

    protected async linkChallengeAsset(challengeAssetId: string) {
        try {
            await rest.api.UserChallengesResource.linkChallengesAsset({ assetId: challengeAssetId });
        } catch (e) {
            this.statusText = i18next.t("error.unknown");
            throw new Error("Unknown error when linking the challenges.");
        }
    }




    /* ---------------------------------------- */
    // CHECK 8:     Fetch and check the challenge asset. This data is required for the app to function.

    protected async checkChallengeAsset(userId: string, challengeId: string): Promise<void> {
        try {
            await this.fetchChallengeAsset(userId, challengeId);
        } catch (e) {
            if (isAxiosError(e)) {
                if (e.response.status === 403) {
                    this.statusText = i18next.t("error.challengeAssetDataPermission");
                } else {
                    this.statusText = i18next.t("error.challengeAssetDataFailed");
                }
            } else if (e instanceof NoChallengeAssetError) {
                this.statusText = e.message;
            } else {
                this.statusText = i18next.t("error.unknown");
            }
            throw e;
        }
    }

    protected async fetchChallengeAsset(userId: string, challengeId: string, delay?: number): Promise<void> {
        if(!this.challengeAsset) {
            const assets = (await manager.rest.api.AssetResource.queryAssets({
                ids: [challengeId],
                types: ["OurgridChallengesAsset"],
                userIds: [userId],
                limit: 1
            })).data;
            if(assets === undefined || assets.length === 0) {
                throw new NoChallengeAssetError(i18next.t("error.challengeAssetDataFailed"));
            }
            // Add additional delay if necessary
            if(delay !== undefined) {
                await new Promise(resolve => setTimeout(resolve, delay));
            }
            // cache the asset and its ID.
            this._store.dispatch(setChallengeAsset(assets[0]));
        }
    }


    /* ---------------------------------------- */

    protected async checkPeakPointsAssetId(districtAsset: Asset): Promise<string> {
        if(!this.peakPointsAsset) {
            if(districtAsset?.attributes) {
                const attr = districtAsset.attributes["peaksAssetId"];
                if(attr?.value) {
                    return attr.value;
                }
            }
            this.statusText = i18next.t("error.unknown");
            throw new Error("Unknown error during retrieval of the peak points.");
        } else {
            return this.peakPointsAsset.id;
        }
    }

    protected async checkPeakPointsAssetLink(peakPointsAssetId: string, delay?: number) {
        if(!this.peakPointsAsset) {
            try {
                await rest.api.UserPeakPointsResource.verifyPeakPointsAsset();
                if(delay !== undefined) { await new Promise(resolve => setTimeout(resolve, delay)); }
            } catch (e) {
                // If 'verify peak points asset request' responds with NOT_FOUND, no peak points asset is linked.
                // So, link the peak points asset, and wait 500ms to let the manager/keycloak process possible changes.
                if(isAxiosError(e) && e.response.status === 404) {
                    await this.linkPeakPointsAsset(peakPointsAssetId);
                    await new Promise(resolve => setTimeout(resolve, 250));
                } else {
                    this.statusText = i18next.t("error.unknown");
                    throw new Error("Unknown error during checking the peak points.");
                }
            }
        }
    }

    protected async linkPeakPointsAsset(peakPointsAssetId: string) {
        try {
            await rest.api.UserPeakPointsResource.linkPeakPointsAsset({ assetId: peakPointsAssetId });
        } catch (e) {
            console.error(e);
            this.statusText = i18next.t("error.unknown");
            throw new Error("Unknown error when linking the peak points.");
        }
    }

    // CHECK 0:     Fetch and check the peak points asset. This data is required for the app to function.
    protected async checkPeakPointsAsset(userId: string, peakPointsId: string): Promise<void> {
        try {
            await this.fetchPeakPointsAsset(userId, peakPointsId);
        } catch (e) {
            if (isAxiosError(e)) {
                if (e.response.status === 403) {
                    this.statusText = i18next.t("error.peakPointsAssetDataPermission");
                } else {
                    this.statusText = i18next.t("error.peakPointsAssetDataFailed");
                }
            } else if (e instanceof NoPeakPointsAssetError) {
                this.statusText = e.message;
            } else {
                this.statusText = i18next.t("error.unknown");
            }
            throw e;
        }
    }

    protected async fetchPeakPointsAsset(userId: string, peakPointsId: string, delay?: number): Promise<void> {
        if(!this.peakPointsAsset) {
            const assets = (await manager.rest.api.AssetResource.queryAssets({
                ids: [peakPointsId],
                types: ["OurgridPeaksAsset"],
                userIds: [userId],
                limit: 1
            })).data;
            if(assets === undefined || assets.length === 0) {
                throw new NoPeakPointsAssetError(i18next.t("error.peakPointsAssetDataFailed"));
            }
            // Add additional delay if necessary
            if(delay !== undefined) {
                await new Promise(resolve => setTimeout(resolve, delay));
            }
            // cache the asset and its ID.
            this._store.dispatch(setPeakPointsAsset(assets[0]));
        }
    }
}
