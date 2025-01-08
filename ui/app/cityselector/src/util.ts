import {OurGridGatewayCity} from "model";

export const LOCALSTORAGE_KEY_CITY = "city"

export function navigateToCity(window: Window, city: OurGridGatewayCity, save = true): void {
    window.location.href = city.ourGridUrl + "/?realm=" + city.realm;
    if(save) {
        window.localStorage.setItem(LOCALSTORAGE_KEY_CITY, JSON.stringify(city));
    }
}
