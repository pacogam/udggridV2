import {css, html, PropertyValues, TemplateResult, unsafeCSS} from 'lit';
import {customElement, property, query, state} from 'lit/decorators.js';
import {AppConfig, DEFAULT_LANGUAGES, OrApp, Page, router} from '@openremote/or-app';
import manager, {DefaultColor2, DefaultColor3, DefaultColor4} from '@openremote/core';
import {updateMetadata} from 'pwa-helpers/metadata';
import {when} from 'lit/directives/when.js';
import {guard} from 'lit/directives/guard.js';
import {repeat} from 'lit/directives/repeat.js';
import {PageMenu, pageMenuProvider} from '../pages/page-menu';
import {i18next} from '@openremote/or-translate';
import {showLanguageDialog} from '../components/og-dialog';
import {NeedsOnboardingError, NoAssetLinkedError, RequiresPrivacyConfirmationError, splashDataCheckProvider} from '../pages/splash/splash-datacheck';
import {OgPage, OgPageProvider, PageAnimationType} from '../pages/util/og-page';
import {
    assetsSelector,
    attributeEventReceived,
    batteryAssetIdSelector,
    challengeAssetIdSelector,
    districtAssetIdSelector,
    GridAppStateKeyed,
    realmSelector,
    setLanguage,
    userAssetIdSelector
} from './og-state';
import {Asset} from '@openremote/model';
import {OgManager} from './og-manager';
import {Defaults} from "./defaults";
import '../components/og-header';

const styling = css`
  :host {
    --or-app-color2: ${unsafeCSS(DefaultColor2)};
    --or-app-color3: ${unsafeCSS(DefaultColor3)};
    --or-app-color4: ${unsafeCSS(DefaultColor4)};
    --or-console-primary-color: #4D9D2A;
    color: ${unsafeCSS(DefaultColor3)};
    fill: ${unsafeCSS(DefaultColor3)};
    font-size: 14px;

    height: 100vh;
    display: flex;
    flex: 1;
    flex-direction: column;
  }

  .main-content {
    display: flex;
    flex: 1;
    box-sizing: border-box;
    background-color: #F9F5F2;
    overflow: hidden; /*override*/
    animation: main-content-background-fadein ease-in-out 800ms; /* fade in background from HTML page default (dark) to the light color */
  }

  /* Fade in background from default HTML page color (dark) to default App color (light) */
  @keyframes main-content-background-fadein {
    0% {background-color: #4F2D39;}
    100% {background-color: #F9F5F2;}
  }

  /* Hide scrollbar for Chrome, Safari and Opera */

  #main-container::-webkit-scrollbar {
    display: none;
  }

  /* Hide scrollbar for IE, Edge and Firefox */

  #main-container {
    overflow: auto;
    max-width: 100vw;
    max-height: 100vh;
    -ms-overflow-style: none; /* IE and Edge */
    scrollbar-width: none; /* Firefox */
  }
    
  #main-container:has(page-splash-datacheck) {
      overflow: hidden;
  }

  main > * {
    /*display: flex;*/
    flex: 1;
    position: relative;
  }

  .desktop-hidden {
    display: none !important;
  }

  @media only screen and (max-width: 780px) {
    .desktop-hidden {
      display: inline-block !important;
    }
  }

  /* HEADER STYLES */

  og-header {
    transition: all 0.2s;
  }
  
`;

@customElement('og-app')
export class OgApp<S extends GridAppStateKeyed> extends OrApp<any> {

    public appConfigProvider?: (manager: OgManager) => AppConfig<S>;

    @property()
    protected _isMenuActive = false;

    @property()
    protected _headerShown = true;

    @state()
    protected _loading = false;

    @state() // the page content that should be displayed (first item in the array is active)
    protected _activePages: OgPage<any>[];

    @state()
    protected _dark = false;

    @query('#main-container')
    protected _mainContainerElem?: HTMLElement;

    @query('#loading-container')
    protected _loadingElem?: HTMLElement;

    @query('page-menu')
    protected _menu?: PageMenu;

    protected _attributeSubscriptionId: string;
    protected _timeout: NodeJS.Timeout;
    protected _assets: Asset[] = [];

    static get styles() {
        return styling;
    }

    connectedCallback() {
        super.connectedCallback();
        i18next.on('languageChanged', lang => {
            this._store.dispatch(setLanguage(lang));
        });
    }

    disconnectedCallback() {
        this.unsubscribeAssets();
        clearInterval(this._timeout);
        super.disconnectedCallback();
    }

    protected _handleVisibilityChange(ev: Event) {
        super._handleVisibilityChange(ev);
        if(manager.console?.isMobile && document.visibilityState === 'visible') {
            const exclusions = ['setup'];
            if(!exclusions.includes(this._page)) {
                window.location.reload();
            }
        }
    }

    stateChanged(state: GridAppStateKeyed) {
        super.stateChanged(state);
        this._assets = state.gridApp.assets;
        this._dark = state.gridApp.dark;

        // Once all assets are fetched (meter asset, district asset, and challenges asset),
        // we subscribe to attribute changes of the specific assets
        if(!this._attributeSubscriptionId) {
            if(!this._loading && userAssetIdSelector(state) && districtAssetIdSelector(state) && challengeAssetIdSelector(state) && state.gridApp.assets.length >= 3) {
                const assetIds = state.gridApp.assets.map(a => a.id);
                this._trySubscribeAssets(assetIds, manager.displayRealm);
            }
        }
    }

    protected _trySubscribeAssets(assetIds: string[], realm = manager.displayRealm) {
        if(!this._attributeSubscriptionId) {
            this.subscribeAssets(realm, assetIds).catch(e => console.error(e));
        }
    }

    protected shouldUpdate(changedProps: PropertyValues): boolean {
        const update = super.shouldUpdate(changedProps);
        if(changedProps.has('_page') && this._page) {

            // Detect whether it's the first page being loaded (so from 'undefined' to something)
            // If so, always redirect to the homepage. (except for several pages listed below)
            const exclusions = ['home', 'setup', 'onboarding', 'confirm-privacy'];
            if(!changedProps.get('_page') && !exclusions.includes(this._page)) {
                console.log('Redirecting to Homepage...');
                this.updateComplete.then(() => router.navigate('home'));

            // Else, just continue loading the page...
            } else {
                const provider = this.getPageProvider(this._page);
                const previous = changedProps.get('_page') as string | undefined;

                let forceAnimate = undefined;
                if(
                    !this._assets || this._assets.length === 0 || // if no assets are present
                    (previous?.toLowerCase() === 'setup' && this._page.toLowerCase() === 'home') || // or user has just completed the setup
                    (previous?.toLowerCase() === 'onboarding' && this._page.toLowerCase() === 'home') // or user has just completed the onboarding
                ) {
                    forceAnimate = true; // then force the 'animation'
                }

                if (provider) {
                    this.switchPage(provider, forceAnimate);
                } else {
                    console.error('Provider for the page could not be found.');
                }
            }
        }
        return update;
    }

    protected willUpdate(changedProps: PropertyValues) {

        // If _isMenuActive has been changed, make sure the menu component is aligned.
        if (changedProps.has('_isMenuActive')) {
            this._menu?.toggle(this._isMenuActive);
        }
        // When loading is complete, make a WS subscription to the OurGrid assets (if possible, and not set yet)
        if (changedProps.has('_loading') && this._loading === false) {
            const assetIds = assetsSelector(this.getState()).map(a => a.id);
            if(assetIds?.length > 0) {
                this._trySubscribeAssets(assetIds, manager.displayRealm);
            }
        }

        return super.willUpdate(changedProps);
    }

    // After 'every initialization', set the scroll listener to show/hide the menu button.
    // First updated couldn't be used here, since it should wait for 'initialised' to be true.
    protected updated(changedProps: Map<string, any>) {
        if (changedProps.has('_initialised') && this._initialised && this._mainContainerElem) {
            this._mainContainerElem.onscroll = () => {
                this._headerShown = this._mainContainerElem ? (this._mainContainerElem.scrollTop <= Defaults.HIDE_HEADER_FROM_HEIGHT_PX) : true;
            };
            // Also add listener for the 'language' button in the menu.
            if(this._menu) {
                this._menu.addEventListener('navigate', (ev: CustomEvent) => {
                    if(ev.detail && typeof ev.detail === 'string') {
                        router.navigate(ev.detail);
                    }
                    // TODO: Improve this to remove timeout, and use an "automatic close menu when switching page" system. It's now simply waiting 100 millis, guaranteeing it switched pages.
                    setTimeout(() => {
                        this._isMenuActive = false;
                    }, 100);
                });
                this._menu.addEventListener('language', () => {
                    this.showLanguageModal();
                });
            } else {
                console.error("Could not initialize 'language menu' listener.");
            }
        }

        // Copied over code from or-app, since using super.updated() will break page loading.
        if (changedProps.has('_activePage') && this._activePages?.[0]) {
            this.updateWindowTitle(this._activePages[0]);
        }
    }

    // Rendering HTML for the app.
    // Uses the Lit guard directive to only update the content when certain variables update.
    protected render(): TemplateResult {
        const pageProvider = this.appConfig?.pages.find(page => page.name === this._page) as OgPageProvider<any>;
        if (!this._initialised) {
            return html`
                <or-mwc-dialog id="app-modal"></or-mwc-dialog>
            `;
        } else {
            return html`

                <!-- Header and main menu -->
                ${guard([this.appConfig, this._headerShown, this._dark, this._initialised, this._loading, this._isMenuActive], () => html`
                    ${when(!pageProvider?.hideHeader, () => html`
                        <div id="header-container">
                            <og-header style="transform: translateY(${this._headerShown ? '0' : '-100%'})" .dark="${this._dark || this._isMenuActive}"
                                       .menuActive="${this._isMenuActive}" .loading="${!this._initialised || this._loading}"
                                       @menu="${() => this._isMenuActive = !this._isMenuActive}"
                            ></og-header>
                        </div>
                    `)}
                    ${guard([], () => html`
                        ${pageMenuProvider(this._store).pageCreator()}
                    `)}
                `)}

                <!-- Main content -->
                <main role="main" class="main-content d-none">

                    <div id="main-container">
                        ${when(this._activePages?.length, () => repeat(this._activePages, (item) => item.tagName, (item) => item))}
                    </div>

                </main>

                <slot></slot>
            `;
        }
    }

    // Method that switches pages using enter/exit animations.
    // Checks whether a loading check should be in place, and waits for it to finish.
    protected async switchPage(provider: OgPageProvider<any>, animate?: boolean) {
        const currentPage = this._activePages?.[0];
        const newPage = provider.pageCreator();
        if(animate === undefined) {
            animate = !this._isMenuActive;
        }
        console.info(`Navigating from ${currentPage?.name} to ${newPage.name}, with animation set to ${animate}`);

        // Exit old page
        if(currentPage && animate) {
            await currentPage.doExitAnimation(undefined, newPage.name);
        }

        this._loading = true;

        // Insert loading animation if necessary
        let loadingPage: OgPage<any> | undefined;
        if(!provider.skipDataCheck) {
            loadingPage = splashDataCheckProvider(this._store).pageCreator();
            this._setActivePage(loadingPage, true);
            this._loading = true;
            try {
                if(animate) await loadingPage.doEnterAnimation(!currentPage ? PageAnimationType.SLOW_FADE : undefined);
                await loadingPage.getLoadingPromise(); // wait for loading to finish
            } catch (e) {
                if(e instanceof NoAssetLinkedError && this._page !== 'setup') {
                    console.log('No asset found, redirecting to setup...');
                    router.navigate('setup');
                    return;
                } else if(e instanceof NeedsOnboardingError) {
                    router.navigate('onboarding');
                    return;
                } else if(e instanceof RequiresPrivacyConfirmationError) {
                    router.navigate('confirm-privacy');
                    return;
                }
            }
        }

        // Append new page as an HTML child
        this._setActivePage(newPage);

        // Waiting until the new page is properly loaded...
        await newPage.getLoadingPromise(currentPage?.name);

        // Play exit animation of the loading page
        if(loadingPage && animate) {
            await loadingPage.doExitAnimation();
        }

        // Remove loading page, and force new page
        this._setActivePage(newPage, true);
        this._loading = false;

        // Enter new page
        if(animate) await newPage.doEnterAnimation(undefined, currentPage?.name);
    }

    // Sets or appends a new page (this can be a regular page, or a splash loader)
    protected _setActivePage(page: OgPage<any>, force = false) {
        if(force) {
            this._activePages = [page];
        } else {
            this._activePages.push(page);
            this.requestUpdate('_activePages');
        }
    }

    protected getPageProvider(page: string): OgPageProvider<any> | undefined {
        return this.appConfig?.pages.find(p => p.name === page) as OgPageProvider<any>;
    }


    /* -------------------------------------------------- */

    // Triggered by the 'language' menu button,
    // to open the language selection popup.
    public showLanguageModal() {
        showLanguageDialog(this.appConfig?.languages || DEFAULT_LANGUAGES);
    }

    protected updateWindowTitle(page?: Page<any>) {
        if(page) {
            const appTitle = this._config?.appTitle || '';
            let pageTitle = (i18next.isInitialized ? i18next.t(appTitle) : appTitle);
            pageTitle += (i18next.isInitialized ? ' - ' + i18next.t(page.name) : ' - ' + page.name);
            updateMetadata({
                title: pageTitle,
                description: pageTitle
            });
        }
    }


    // The following code is based on page-map from the OpenRemote platform,
    // where we listen to changes of attributes using websocket, to change the values live.

    protected subscribeAssets = async (realm: string, assetIds: string[]) => {
        console.log(`Subscribing to assets; ${assetIds}`);

        try {

            // No longer connected or realm has changed
            if (!this.isConnected || realm !== realmSelector(this.getState())) {
                console.error('Could not subscribe to assets, since you are not connected to the realm.');
                return;
            }

            const attributeSubscriptionId = await manager.events.subscribeAttributeEvents(assetIds, false, event => {
                this._store.dispatch(attributeEventReceived(event));
            });

            // No longer connected or realm has changed
            if (!this.isConnected || realm !== realmSelector(this.getState())) {
                console.error('Unsubscribing from assets, since you are not connected to the realm.');
                manager.events.unsubscribe(attributeSubscriptionId);
                return;
            }

            this._attributeSubscriptionId = attributeSubscriptionId;

        } catch (e) {
            console.error('Failed to subscribe to assets', e);
        }
    };

    protected unsubscribeAssets = () => {
        if (this._attributeSubscriptionId) {
            manager.events.unsubscribe(this._attributeSubscriptionId);
            this._attributeSubscriptionId = null;
        }
    };
}
