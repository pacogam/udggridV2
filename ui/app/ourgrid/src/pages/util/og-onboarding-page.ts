import {TemplateResult, html, css} from 'lit';
import {property, query, state} from 'lit/decorators.js';
import {getAppStyle} from '../../styles';
import {InputType} from '@openremote/or-mwc-components/or-mwc-input';
import {styleMap} from 'lit/directives/style-map.js';
import {classMap} from 'lit/directives/class-map.js';
import {guard} from 'lit/directives/guard.js';
import {until} from 'lit/directives/until.js';
import {when} from 'lit/directives/when.js';
import {map} from 'lit/directives/map.js';
import '../../components/og-swipable';
import {GridAppStateKeyed} from '../../util/og-state';
import {OgSwipable} from '../../components/og-swipable';
import {OgPage} from './og-page';
import {GraphicType} from '../../features/og-usage-graphic';

const styling = css`
  #onboarding-wrapper {
    width: 100%;
    height: 100%;
    display: flex;
    flex-direction: column;
    overflow: hidden auto;
  }

  #onboarding-container {
    flex: 1;
    max-height: calc(100vh - 32px);
    display: flex;
    flex-direction: column;
    justify-content: space-between;
    align-items: center;
    padding: 16px;
  }
  #onboarding-title {
    display: flex;
    flex-direction: column;
    align-items: center;
    width: 100%;
  }
  #onboarding-topgraphic {
    width: inherit;
    position: absolute;
    bottom: 0;
  }
  #onboarding-content {
    flex: 1;
    width: calc(100% - 32px);
    overflow: auto;
    padding: 32px;
  }
  #onboarding-footer {
    width: 100%;
    display: flex;
    flex-direction: column;
    justify-content: end;
    gap: 18px;
  }
  #onboarding-bottomgraphic {
    width: inherit;
    position: absolute;
    bottom: 0;
  }
`;

export interface OnboardPage {
    getHeading?: () => string
    headingStyle?: 'title' | 'heading'
    pageContent: () => Promise<TemplateResult> | TemplateResult;
    noTopGraphic?: boolean
    noBottomGraphic?: boolean;
    noPadding?: boolean;
    getActionText?: () => string;
    getActionDisabled?: () => boolean;
}

export abstract class OgOnboardingPage extends OgPage<GridAppStateKeyed> {

    protected abstract pages: OnboardPage[];

    protected abstract onActionClick(index: number);

    abstract get name();

    @property()
    protected dots = true;

    @property()
    protected gesture = false;

    @property()
    protected vertical = false;

    @state() // center content horizontally and vertically. Only works when only 1 page is present
    protected centered = true;

    @state()
    protected currentPageIndex = 0;

    @property()
    protected language: string;

    @query('og-swipable')
    protected swipeElem: OgSwipable;

    stateChanged(state: GridAppStateKeyed): void {
        this.language = state.gridApp.language;
    }

    static get styles() {
        return [...super.styles, getAppStyle(), styling];
    }

    protected render() {
        const page = this.pages[this.currentPageIndex];
        const contentStyling = {
            'display': this.centered && this.pages.length === 1 ? 'flex' : undefined,
            'justify-content': this.centered && this.pages.length === 1 ? 'center' : undefined,
            'align-items': this.centered && this.pages.length === 1 ? 'center' : undefined,
            'text-align': this.centered && this.pages.length === 1 ? 'center' : undefined
        };
        const anyPageHasActionText = !!this.pages.find(p => !!p.getActionText);
        const isLargeFooter = (!page.noBottomGraphic && !!page.getActionText);
        const footerStyling = {
            'min-height': anyPageHasActionText ? '48px' : undefined,
            'height': isLargeFooter ? '20vh' : undefined,
            'position': isLargeFooter ? 'relative' : undefined,
            'overflow': 'hidden'
        };
        return html`
            <div id="onboarding-wrapper">
                <div id="onboarding-container">

                    <!-- Optional header content with title and/or graphic -->
                    ${when(!page.noTopGraphic || !!page.getHeading, () => html`
                        <div id="onboarding-title" style="height: 16vh; position: relative;">
                            ${when(!page.noTopGraphic, () => html`
                                <og-usage-graphic id="onboarding-topgraphic" .type="${GraphicType.HEADER}"></og-usage-graphic>
                            `)}
                            ${when(!!page.getHeading, () => {
                                const classes = {
                                    'text-title': page.headingStyle === undefined || page.headingStyle === 'title',
                                    'text-heading': page.headingStyle === 'heading'
                                };
                                return html`
                                    <span class="${classMap(classes)}" style="position: absolute; bottom: 0; --animate-offset: 0.2s; max-width: 65vw; text-align: center;">
                                        <or-translate value="${page.getHeading()}"></or-translate>
                                    </span>
                                `;
                            })}
                        </div>
                    `)}
                    
                    <!-- Main content with one or multiple pages using og-swipable -->
                    <div id="onboarding-content" style="${styleMap(contentStyling)}">
                        ${when(this.pages.length > 1, () => {
                            return html`
                                <og-swipable .dots="${this.dots}" .dotsClickable="${false}" .gesture="${this.gesture}" .vertical="${this.vertical}" .language="${this.language}"
                                             .size="${this.pages.length}" .selected="${this.currentPageIndex}" @slide="${ev => this.onSlide(ev)}">
                                    ${guard([this.pages, this.language, this.currentPageIndex], () => html`
                                        ${map(this.pages, (p, index) => {
                                            return html`
                                                <div slot="${index}" style="height: 100%; overflow: auto;">
                                                    ${until(p.pageContent(), html`
                                                        <og-loading></og-loading>
                                                    `)}
                                                </div>
                                            `;
                                        })}
                                    `)}
                                </og-swipable>
                            `;
                        }, () => html`
                            ${until(page.pageContent(), html`
                                <og-loading></og-loading>
                            `)}
                        `)}
                    </div>
                    
                    <!-- Optional footer content with action button and/or graphic. -->
                    ${when(!page.noBottomGraphic || !!page.getActionText, () => html`
                        <div id="onboarding-footer" style="${styleMap(footerStyling)}">
                            ${when(!page.noBottomGraphic, () => html`
                                <og-usage-graphic id="onboarding-bottomgraphic" .type="${GraphicType.FOOTER}"></og-usage-graphic>
                            `)}
                            ${when(typeof page.getActionText === 'function', () => {
                                const buttonStyling = {
                                    'width': !page.noBottomGraphic ? 'inherit' : undefined,
                                    'position': !page.noBottomGraphic ? 'absolute' : undefined,
                                    'bottom': !page.noBottomGraphic ? '16px' : undefined
                                };
                                return html`
                                    <og-input .type="${InputType.BUTTON}" fullWidth raised rounded .disabled="${typeof page.getActionDisabled === 'function' ? page.getActionDisabled() : false}" label="${page.getActionText()}"
                                              style="--or-app-color4: var(--og-color-success); ${styleMap(buttonStyling)}" @or-mwc-input-changed="${() => this.onActionClick(this.currentPageIndex)}"
                                    ></og-input>
                                `;
                            })}
                        </div>
                    `)}
                </div>
            </div>
        `;
    }

    // onSlide function that hooks into og-swipable,
    // It triggers every time the page of the onboarding has changed.
    protected onSlide(ev: CustomEvent) {
        this.currentPageIndex = ev.detail.value;
    }

    // Method that triggers functions of og-swipable to navigate between pages.
    protected switchPage(direction: 'previous' | 'next') {
        if (direction === 'next') {
            this.swipeElem.goToNextPage();
        } else {
            this.swipeElem.goToPreviousPage();
        }
    }
}
