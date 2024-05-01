import {LitElement, TemplateResult, html, css} from "lit";
import {customElement, property, state} from "lit/decorators.js";
import { OurGridConfig } from "@openremote/model";
import { Task } from "@lit/task";
import { InputType } from "@openremote/or-mwc-components/or-mwc-input";
import { ListItem } from "@openremote/or-mwc-components/or-mwc-list";
import { when } from "lit/directives/when.js";
import {getAppStyle} from "./styles";
import "./components/og-city-list";
import "./components/og-city-input";
import {AltCity, City} from "./model";
import {navigateToCity} from "./util";

const styling = css`
    #wrapper {
        height: 100%;
        width: 100%;
        display: flex;
        flex-direction: column;
        justify-content: space-between;
        align-items: center;
        gap: 5vh;
        background: var(--og-color-primary-dark)
    }

    #content {
        flex: 1;
        width: calc(100% - 64px);
        padding: 16px 32px;
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
        gap: 2.5vh;
    }
`;

@customElement("city-selector-app")
export class CitySelectorApp extends LitElement {

    @property() // base url of the website, used for fetching the JSON files.
    public baseUrl: string;

    @property() // path in deployment folder of the OurGridConfig file.
    public ourGridConfigUrl: string;

    @property() // folder name of the language files; it is expecting (folder)/(language)/app.json
    public langFolder: string;

    @state() // state of search field value
    protected _citySearchText?: string;

    constructor(baseUrl?: string, configUrl?: string, langFolder?: string, lang?: string) {
        super();
        this.baseUrl = baseUrl || "";
        this.ourGridConfigUrl = configUrl || "/ourgrid_config.json";
        this.langFolder = langFolder || "locales";
        this.lang = lang;
    }

    connectedCallback() {
        super.connectedCallback();
        this.fetchConfig();
        this.fetchTranslations();
    }

    static get styles() {
        return [getAppStyle(), styling] as any;
    }

    protected render(): TemplateResult {
        return html`
            <div id="wrapper">
                <div id="header">
                    <!-- Empty content -->
                </div>
                <div id="content">
                    <span class="text-primary dark">${this._t("searchCityName")}</span>
                    <og-city-input id="search-field" .type="${InputType.TEXT}" style="width: 100%; max-width: 480px;"
                                   @city-input-changed="${(ev) => this._onCitySearch(ev)}"
                    ></og-city-input>
                    <div style="width: 100%; max-width: 480px; min-height: 300px;">
                        ${this.getCitySelectorTemplate()}
                    </div>
                </div>
                <div id="footer">
                    <!-- Empty content -->
                </div>
            </div>
        `;
    }

    /**
     * Function that returns a {@link TemplateResult} with a list of cities. ({@link OgCityList})
     * This list is filtered based on the search field, with a maximum of 5 items shown.
     * It internally uses the {@link Task} mechanism of showing different UI depending on its status.
     */
    protected getCitySelectorTemplate(): TemplateResult {
        return this._fetchSearchCitiesTask.render({
            initial: () => html`<og-loading></og-loading>`,
            pending: () => html`<og-loading></og-loading>`,
            complete: (cities) => {
                const filtered = !!this._citySearchText;
                const visibleCities = filtered ? this._getFilteredCities(cities, this._citySearchText) : [];
                const cityItems = visibleCities.map(city => {
                    return {
                        text: city.name,
                        value: city.name,
                        data: city
                    } as ListItem
                })
                return html`
                    <div>
                        ${when(this._citySearchText, () => html`
                            <og-city-list .listItems="${cityItems}"
                                         @or-mwc-list-changed="${(ev: CustomEvent) => this._onCitySelect(ev)}"
                            ></og-city-list>
                        `)}
                    </div>
                `;
            }
        })
    }

    /**
     * HTML Event callback for the search field, triggering on every character input.
     * It updates the local state, updating other areas of the UI.
     */
    protected _onCitySearch(ev: CustomEvent): void {
        this._citySearchText = ev.detail.value;
    }

    /**
     * HTML Event callback for {@link OgCityList} when a City is selected.
     * It looks up whether the City is present in the {@link OurGridGatewayConfig}, and navigates to it.
     */
    protected _onCitySelect(ev: CustomEvent) {
        const selected = (ev.detail[0] as ListItem).data as City;
        const ogCity = this._fetchConfigTask.value.gateway.cities.find(c => c.name === selected.name);
        if(ogCity) {
            navigateToCity(window, ogCity);
        } else {
            console.warn("This city does not have an OurGrid installation.")
        }
    }


    /* ------------------------------------- */

    /**
     * Internal function for filtering the Cities shown within the {@link OgCityList} UI.
     * Based on the available cities ({@link cities}) and the given search ({@link search}), it returns an array of cities.
     * The default maximum is 5.
     */
    protected _getFilteredCities(cities: City[], search: string, max = 5): City[] {
        let filtered: City[] = [];
        cities.forEach((city) => {
            if(filtered.length >= max) return;
            if(filtered.find(c => city.alt && city.alt.includes(c.name))) return;
            if(city.name.normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase().includes(search.toLowerCase().normalize('NFD').replace(/[\u0300-\u036f]/g, ''))) {
                filtered.push(city);
            }
        });
        return filtered;
    }


    /* ------------------------------------- */

    /**
     * Triggers the task to fetch the {@link OurGridConfig} JSON file from its deployment.
     * Returns its value once the fetching is complete.
     */
    public async fetchConfig(configUrl?: string): Promise<OurGridConfig> {
        const args = configUrl ? [configUrl] : undefined;
        await this._fetchConfigTask.run(args);
        return this._fetchConfigTask.value;
    }

    /**
     * Internal Lit {@link Task} meant for fetching the {@link OurGridConfig} JSON file from its deployment.
     * It acts as a reactive controller, and keeps its value cached.
     */
    protected _fetchConfigTask = new Task(this, {
        task: async ([configUrl], {signal}) => {
            return this._fetchOurGridConfig(configUrl, signal);
        },
        args: () => [this.ourGridConfigUrl]
    });

    /**
     * Internal asynchronous function to fetch the {@link OurGridConfig} JSON file.
     */
    protected async _fetchOurGridConfig(configUrl: string, signal?: AbortSignal): Promise<OurGridConfig> {
        const response = await fetch(configUrl, { signal });
        if(!response.ok) { throw new Error(response.statusText); }
        return await response.json() as OurGridConfig;
    }


    /* ------------------------------------- */

    /**
     * Triggers the task to fetch the `ourgrid_cities.json` file. It contains the list of cities appearing in the search results.
     * Returns its value once the fetching is complete.
     */
    public async fetchSearchCities(baseUrl?: string, config?: OurGridConfig): Promise<City[]> {
        const args = (baseUrl || config) ? [baseUrl, config].filter(v => v !== undefined) : undefined;
        await this._fetchSearchCitiesTask.run(args);
        return this._fetchSearchCitiesTask.value;
    }

    /**
     * Internal Lit {@link Task} meant for fetching the `ourgrid_cities.json` file. It contains the list of cities appearing in the search results.
     * It acts as a reactive controller, and keeps its value cached.
     */
    protected _fetchSearchCitiesTask = new Task(this, {
        task: async ([baseUrl, config], {signal}) => {
            const data = await this._fetchSearchCities(baseUrl as string, config as OurGridConfig, signal);
            const altCities = data.filter(c => c.alt).map(c => {
                return c.alt.split(',').map(alt => ({ name: alt, lang: c.lang, city: c.name } as AltCity));
            }).flat(1);
            return [...data, ...altCities].sort((a, b) => a.name.localeCompare(b.name));
        },
        args: () => [this.baseUrl, this._fetchConfigTask.value]
    })

    /**
     * Internal asynchronous function to fetch the `ourgrid_cities.json` file.
     */
    protected async _fetchSearchCities(baseUrl: string, config: OurGridConfig, signal?: AbortSignal): Promise<City[]> {
        if(config?.gateway) {
            const url = baseUrl + config.gateway.searchCitiesFile;
            const response = await fetch(url, { signal });
            if(!response.ok) { throw new Error(response.statusText); }
            const data = await response.json();
            return Object.keys(data).map(v => ({ name: v, ...data[v] }) as City);
        } else {
            console.warn("Could not find gateway config URL!")
            return [];
        }
    }


    /* ------------------------------------- */

    /**
     * Triggers the task to fetch the translations JSON file.
     * Returns its value once the fetching is complete.
     */
    public async fetchTranslations(folder?: string): Promise<Object> {
        const args = folder ? [folder] : undefined;
        await this._fetchTranslationsTask.run(args);
        return this._fetchTranslationsTask.value;
    }

    /**
     * Internal Lit {@link Task} meant for fetching the translations JSON file.
     * It acts as a reactive controller, and keeps its value cached.
     */
    protected _fetchTranslationsTask = new Task(this, {
        task: async ([folder, language], {signal}) => {
            return await this._fetchTranslationsFile(folder, language, undefined, signal);
        },
        args: () => [this.langFolder, this.lang]
    });

    /**
     * Internal asynchronous function to fetch the translations JSON file.
     */
    protected async _fetchTranslationsFile(folderName: string, language: string, fileName = "app.json", signal?: AbortSignal): Promise<Object> {
        const url = folderName + "/" + language + "/" + fileName;
        const response = await fetch(url, { signal });
        if(!response.ok) { throw new Error(response.statusText); }
        try {
            return await response.json() as Object;
        } catch (e) {
            console.error(e);
        }
    }

    /**
     * Internal function for translating text similar to i18next, but done manually here.
     */
    protected _t(key?: string): string | undefined {
        if(key) {
            return this._fetchTranslationsTask?.value?.[key];
        }
    }
}