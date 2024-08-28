import {css, html, LitElement, PropertyValues, TemplateResult } from 'lit';
import {customElement, property, state } from 'lit/decorators.js';
import { map } from 'lit/directives/map.js';
import {OgPanel} from './og-panel';
import {Asset, User} from '@openremote/model';
import {OgDataPanel} from './og-data-panel';
import {getAppStyle} from '../styles';
import { classMap } from 'lit/directives/class-map.js';
import { styleMap } from 'lit/directives/style-map.js';
import { when } from 'lit/directives/when.js';
import { until } from 'lit/directives/until.js';

const styling = css`
  :host {
    position: relative;
    height: 100%;
  }
  .panel {
    max-height: 0;
    overflow: hidden;
    transition-property: max-height;
    transition-timing-function: var(--og-easing-standard);
    transition-duration: 500ms; /*equal to MIN_ANIMATION_DURATION*/
  }
  .panel-dark {
    background: var(--og-panel-background, var(--og-color-primary-dark));
    color: var(--og-color-primary);
  }
  .panel-group {
    display: flex;
    flex-direction: column;
    overflow: hidden;
    /*gap: 8px;*/
  }
  #action-container {
    position: absolute;
    display: block;
    bottom: -24px;
    left: 24px;
    width: calc(100% - 48px);
    z-index: 3;
  }

  #action-button {
    --or-mwc-input-color: var(--og-color-danger);
    --og-color-primary: var(--og-color-primary);
    width: 100%;
  }
`;

const ANIMATION_DELAY = 1000;
const MIN_ANIMATION_DURATION = 500;
const ANIMATION_MS_MULTIPLIER = 3; // height of panel in pixels * (this value) = animation duration in milliseconds.
const ANIMATION_DELAY_DIVIDER = 5; // (animation duration in milliseconds) / (this value) = delay until next animation starts.
const ANIMATION_FALLBACK_HEIGHT = 1000000;


/*
    OG-PANEL-WRAPPER

    The idea of this 'wrapper' / grouping mechanism to control the rendering of OgPanels and apply animations.
    Especially in regard to exit animations, where you need to apply CSS classes, wait until the animation finishes, and then remove the element.
    Normally we would've used <slot> elements in some sort of way, but in that case animations wouldn't work either. (since they're loaded by the parent)

 */
@customElement('og-panel-wrapper')
export class OgPanelWrapper extends LitElement {

    @property() // list of unique HTML strings that will load as panels. Required to be extending on OgPanel.
    public panels: Set<string> = new Set<string>();

    @property({type: Object})
    public user: User;

    @property({type: Object})
    public meterAsset: Asset;

    @property({type: Object})
    public batteryAsset: Asset;

    @property({type: Object})
    public challengeAsset: Asset;

    @property({type: Object})
    public districtAsset: Asset;

    @property({type: Object})
    public peakPointsAsset: Asset;

    @property({type: Boolean})
    public dark = false;

    @state()
    protected loadedPanels: Map<string, OgPanel> = new Map<string, OgPanel>();

    // Key of the action panel, that always updates even if set to the same value.
    // This is an easy fix for "automatically updating the wrapper once the child action changes".
    @state({ hasChanged(_oldVal, _newVal) { return true; }})
    protected actionPanelKey: string;

    protected renderEventHandlers: Map<string, (ev) => Promise<void>> = new Map<string, (ev) => Promise<void>>();

    /* -------------------- */

    static get styles() {
        return [getAppStyle(), styling];
    }

    // On component removal, remove the EventListeners
    disconnectedCallback() {
        super.disconnectedCallback();
        this.renderEventHandlers.forEach((value, key) => {
            this.loadedPanels.get(key)?.removeEventListener('actionUpdate', value);
        });
    }

    protected willUpdate(changedProps: PropertyValues) {

        // Add or remove panels based on variable changes
        if(changedProps.has('panels') && this.panels) {
            this.processPanelChanges(this.panels);

            // Correct properties of child panels
            this.loadedPanels.forEach(panel => {
                panel.dark = true;
                panel.slotted = true;
            });
        }

        if(changedProps.has('loadedPanels') && this.loadedPanels) {
            const actionPanelIndex = Array.from(this.loadedPanels.values()).reverse().findIndex(p => p.action !== undefined);
            this.actionPanelKey = this.loadedPanels.keys()[actionPanelIndex];
        }

        // If meter-, battery-, or challenge data has changed, we also set the respective variables for data panels.
        if(changedProps.has('meterAsset') || changedProps.has('batteryAsset') || changedProps.has('challengeAsset')) {
            this.loadedPanels.forEach(panel => {
                if(panel instanceof OgDataPanel) {
                    panel.setUser(this.user).setMeterAsset(this.meterAsset).setBatteryAsset(this.batteryAsset).setChallengeAsset(this.challengeAsset).setDistrictAsset(this.districtAsset).setPeakPointsAsset(this.peakPointsAsset);
                }
            });
        }

        return super.willUpdate(changedProps);
    }


    // Method that adds or removes panels by comparing it to the loadedPanels.
    protected async processPanelChanges(panels: Set<string>) {
        const panelsArray = Array.from(panels);
        const addedPanels = panelsArray.filter(p => !this.loadedPanels.has(p));
        const removedPanels = Array.from(this.loadedPanels.keys()).filter(lp => !panelsArray.includes(lp));
        if(removedPanels.length > 0) {
            await this.removePanels(...removedPanels);
        }
        if(addedPanels.length > 0) {
            this.addPanels(...addedPanels);
        }
    }

    // Creates HTML elements based on the list of strings provided.
    // It will only load panels that are not present, and will automatically set asset data if inheriting from OgDataPanel.
    public addPanels(...panelNames: string[]): OgPanel | OgPanel[] {
        console.log(`Adding ${panelNames.length} panels...`);
        const addedPanels = panelNames
            .filter(p => !this.loadedPanels.has(p))
            .map(p => {
                let panel: OgPanel = document.createElement(p) as OgPanel;
                if(panel instanceof OgDataPanel) {
                    panel = panel
                        .setUser(this.user)
                        .setMeterAsset(this.meterAsset)
                        .setBatteryAsset(this.batteryAsset)
                        .setChallengeAsset(this.challengeAsset)
                        .setDistrictAsset(this.districtAsset)
                        .setPeakPointsAsset(this.peakPointsAsset);
                }
                this.loadedPanels.set(p, panel);

                // Adding listener for action updates
                const func = async (_ev) => { this.actionPanelKey = p; };
                this.renderEventHandlers.set(p, func);
                panel.addEventListener('actionUpdate', func);

                return panel;
        });
        this.requestUpdate('loadedPanels');

        // After UI is rendered, wait for X amount of seconds, and do enter animation after.
        this.updateComplete.then(() => {
            setTimeout(() => { 
                this.doPanelEnterAnimation(...addedPanels);
            }, ANIMATION_DELAY);
        });
        return addedPanels;
    }


    public async removePanels(...panelNames: string[]) {
        console.log(`Removing ${panelNames.length} panels...`);
        const removedNames = panelNames.filter(key => this.loadedPanels.has(key));
        const removedPanels = panelNames.map(key => this.loadedPanels.get(key));

        // Before removing them from loadedPanels, do exit animation
        await this.doPanelExitAnimation(...removedPanels);

        // Remove panels
        removedNames.forEach(p => {
            this.loadedPanels.delete(p);
            this.renderEventHandlers.delete(p);
        });
        this.requestUpdate('loadedPanels');

        return removedPanels;
    }

    protected render(): TemplateResult {
        const classes = {
            'panel': true,
            'panel-dark': this.dark
        };
        const actionPanel = this.loadedPanels.get(this.actionPanelKey);
        return html`
            <div style="position: relative;">
                <div class="panel-group">
                    ${map(this.loadedPanels, (panel, index) => {
                        const panelStyles: {} = {
                            'margin-bottom': index !== (this.loadedPanels.size - 1) ? '-12px' : undefined
                        };
                        return html`
                            <div class=${classMap(classes)} style=${styleMap(panelStyles)}>
                                ${panel[1]}
                            </div>
                        `;
                    })}
                </div>
                ${when(actionPanel, () => {
                    return html`
                        ${until(actionPanel.getActionTemplate())}
                        <div style="margin-bottom: 48px;"></div>
                    `;
                })}
            </div>
        `;
    }


    protected getAnimationDuration(elementHeight?: number): number {
        return Math.max((elementHeight || MIN_ANIMATION_DURATION) * ANIMATION_MS_MULTIPLIER, MIN_ANIMATION_DURATION);
    }

    protected async doPanelEnterAnimation(...panels: OgPanel[]) {

        // Get longest duration
        const sortedPanels = panels.sort((a, b) => b.offsetHeight - a.offsetHeight);
        const longestDuration = this.getAnimationDuration(sortedPanels[0].offsetHeight);

        // Set max-height to the height the panel has currently
        for(const p of sortedPanels) {
            const duration = this.getAnimationDuration(p.offsetHeight);
            p.parentElement.style.transitionDuration = `${duration}ms`;
            p.parentElement.style.maxHeight = (p.offsetHeight || ANIMATION_FALLBACK_HEIGHT) + 'px';
            await new Promise(resolve => setTimeout(resolve, duration / ANIMATION_DELAY_DIVIDER));
            setTimeout(() => {
                p.parentElement.style.overflow = 'visible'; // allow overflow after animation is done
            }, duration);
        }

        // make sure all animations are done
        await new Promise(resolve => setTimeout(resolve, longestDuration));

        // Set max-height to 100%, to allow height growth of the panel and responsive behavior
        // Set max-height to the height the panel has currently
        panels.forEach(p => {
            p.parentElement.style.maxHeight = '100%';
        });
        await new Promise(resolve => setTimeout(resolve, longestDuration)); // wait until animation is done
    }


    protected async doPanelExitAnimation(...panels: OgPanel[]) {

        // Get longest duration
        const sortedPanels = panels.sort((a, b) => b.offsetHeight - a.offsetHeight);
        const longestDuration = this.getAnimationDuration(sortedPanels[0].offsetHeight);

        // Set max-height to the height the panel has currently
        panels.forEach(p => p.parentElement.style.maxHeight = (p.offsetHeight || ANIMATION_FALLBACK_HEIGHT) + 'px');
        await new Promise(resolve => setTimeout(resolve, longestDuration)); // wait until animation is done

        // Set max-height to 0px
        panels.forEach(p => {
            p.parentElement.style.overflow = 'hidden';
            p.parentElement.style.maxHeight = '0px';
        });
        await new Promise(resolve => setTimeout(resolve, longestDuration)); // wait until animation is done
    }
}
