// Declare require method which we'll use for importing webpack resources (using ES6 imports will confuse typescript parser)
import {combineReducers, configureStore} from '@reduxjs/toolkit';
import {AppConfig, appReducer, RealmAppConfig} from '@openremote/or-app';
import {pageHomeProvider} from './pages/page-home';
import {OgApp} from './util/og-app';
import {MapType} from '@openremote/model';
import {page1SetupProvider} from './pages/setup/page1-setup';
import {gridAppReducer} from './util/og-state';
import {pageMenuProvider} from './pages/page-menu';
import {pageAccountProvider} from "./pages/page-account";
import {OgPageProvider} from "./pages/util/og-page";
import {pageOnboardingProvider} from "./pages/onboarding/onboarding-flow";
import {pagePrivacyProvider} from "./pages/page-privacy";
import {pageAccountPasswordProvider} from "./pages/page-account-password";
import {pageCharacteristicsProvider} from "./pages/page-characteristics";
import {pageConfirmPrivacyProvider} from "./pages/privacy/confirm-privacy-page";
import {pageDevicesProvider} from "./pages/page-devices";
import {pageAddDeviceProvider} from "./pages/add-device/add-device-select";
import {pageAddEvProvider} from "./pages/add-device/add-ev";
import {pageAddHeatpumpProvider} from "./pages/add-device/add-heatpump";
import {pageAddBatteryProvider} from "./pages/add-device/add-battery";
import {pageAddHomeAutomationProvider} from "./pages/add-device/add-homeautomation";
import {pageHomeAutomationProvider} from "./pages/page-homeautomation";

const rootReducer = combineReducers({
    app: appReducer,
    gridApp: gridAppReducer
});

type RootState = ReturnType<typeof rootReducer>;

export const store = configureStore({
    reducer: rootReducer
});

const ogApp = new OgApp(store);

export const DefaultPagesConfig: OgPageProvider<any>[] = [
    pageHomeProvider(store),
    pageMenuProvider(store),
    pageAccountProvider(store),
    pageAccountPasswordProvider(store),
    pageDevicesProvider(store),
    pageAddDeviceProvider(store),
    pageAddEvProvider(store),
    pageAddHeatpumpProvider(store),
    pageAddBatteryProvider(store),
    pageAddHomeAutomationProvider(store),
    pageCharacteristicsProvider(store),
    pageHomeAutomationProvider(store),
    pagePrivacyProvider(store),
    page1SetupProvider(store),
    pageOnboardingProvider(store)
];

if(localStorage.getItem('acceptedPrivacy') == null) {
    DefaultPagesConfig.push(pageConfirmPrivacyProvider(store));
}
/*if(localStorage.getItem('completedOnboarding') === '0'/!*null*!/){
    console.log("Adding onboarding page!");
    DefaultPagesConfig.push(pageOnboardingProvider(store));
}*/


export const DefaultRealmConfig: RealmAppConfig = {
    appTitle: 'Our Grid',
    styles: ':host > * {--or-app-color2: #F0F0F0; --or-app-color3: #22211f; --or-app-color4: #4F2D39; --or-app-color5: #CCCCCC;}',
    logo: '../images/logo.png',
    logoMobile: '../images/logo-mobile.png'
};

// Configure manager connection and i18next settings
ogApp.managerConfig = {
    loadTranslations: ['app', 'or'],
    autoLogin: true,
    mapType: MapType.VECTOR
};

export const DEFAULT_LANGUAGE: string = 'nl';

ogApp.appConfigProvider = (ogManager) => {

    // Configure app pages and per realm styling/settings
    const ogAppConfig: AppConfig<RootState> = {
        pages: [...DefaultPagesConfig],
        languages: {
            nl: 'dutch',
            en: 'english'
        },
        realms: {
            default: {...DefaultRealmConfig}
        }
    }

    // Check local storage for set language, otherwise use language set in config
    ogManager.console.retrieveData("LANGUAGE").then((value: string | undefined) => {
        ogManager.language = (value ? value : DEFAULT_LANGUAGE);
    }).catch(() => {
        ogManager.language = DEFAULT_LANGUAGE;
    });

    return ogAppConfig;
}

document.body.appendChild(ogApp);
