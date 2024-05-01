import {CitySelectorApp} from "./city-selector-app";
import {LOCALSTORAGE_KEY_CITY, navigateToCity} from "./util";
import {OurGridGatewayCity} from "@openremote/model";
import {CitySelectorLoader} from "./city-selector-loader";

declare var CONFIG_URL_PREFIX: string;
export const DEFAULT_LANGUAGE: string = 'en';

// Try and load the app config from JSON and if anything is found amalgamate it with default
const configURL = (CONFIG_URL_PREFIX || "") + "/ourgrid_config.json";
const langFolder = "locales";

// Check if city is already selected
const cityStr = window.localStorage.getItem(LOCALSTORAGE_KEY_CITY) as string | undefined;
if(cityStr) {
    const city = JSON.parse(cityStr) as OurGridGatewayCity;
    navigateToCity(window, city);

    // In case navigating takes long, we show a loading page
    const loader = new CitySelectorLoader();
    document.body.appendChild(loader);
}

// if not, load the selector app
else {
    const app = new CitySelectorApp(CONFIG_URL_PREFIX, configURL, langFolder, DEFAULT_LANGUAGE);
    document.body.appendChild(app);
}