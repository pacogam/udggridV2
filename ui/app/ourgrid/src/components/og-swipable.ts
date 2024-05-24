import {LitElement, TemplateResult, PropertyValues, html, css, unsafeCSS, nothing} from 'lit';
import {customElement, property, query, state} from 'lit/decorators.js';
import {map} from 'lit/directives/map.js';
import {when} from 'lit/directives/when.js';
import {range} from 'lit/directives/range.js';
import {classMap} from 'lit/directives/class-map.js';
import {guard} from 'lit/directives/guard.js';
import KeenSlider from 'keen-slider';
import {getAppStyle} from '../styles';
import {debounce} from 'lodash';

const sliderCss = require('keen-slider/keen-slider.css');
const styling = css`
  #swipable-wrapper {
    display: flex;
    flex-direction: column;
    justify-content: space-between;
    gap: 8px;
    height: 100%;
  }

  #slider {
    flex: 1;
    display: flex;
    overflow: hidden;
  }

  .swipable-arrow-container {
    height: 100%;
    width: 24px;
    display: flex;
    align-items: center;
    transition: opacity 0.2s;
  }

  #swipable-controls {
    display: flex;
    justify-content: center;
    gap: 16px;
  }

  .dot {
    height: 12px;
    aspect-ratio: 1/1;
    border-radius: 4px;
    background: var(--og-color-secondary);
  }

  .dot--clickable {
    cursor: pointer;
  }

  .dot--selected {
    background: var(--og-color-primary-dark);
  }
`;

@customElement('og-swipable')
export class OgSwipable extends LitElement {

    @property()
    protected selected = 0;

    @property() // amount of slides; should be equal to the amount of child elements.
    protected size = 3;

    @property() // enables / disables the swipe behaviour for the user
    protected gesture = true;

    @property()
    protected arrows = false;

    @property() // 'circle' controls on the bottom to navigate between slides
    protected dots = true;

    @property() // whether the 'circle' controls are interactible or not
    protected dotsClickable = true;

    @property()
    protected vertical = false;

    @property()
    protected language: string;

    @state()
    protected isSliding = false;

    @query('#slider')
    protected sliderElem?: HTMLElement;

    protected slider?: any;
    protected resizeObserver?: ResizeObserver;

    static styles = [getAppStyle(), unsafeCSS(sliderCss), styling];

    protected willUpdate(changedProps: PropertyValues) {
        super.willUpdate(changedProps);
        if(changedProps.has('selected')) {
            this.slider?.moveToIdx(this.selected); // move to slot of ID when 'selected' variable changes
        }
    }

    protected firstUpdated(changedProps: PropertyValues) {
        super.firstUpdated(changedProps);
        this.slider = new KeenSlider(this.sliderElem, {
            drag: this.gesture,
            initial: this.selected ? this.selected : 0,
            slides: {
                perView: 1
            },
            vertical: this.vertical
        });
        this.slider.on('slideChanged', opts => {
            const index = opts.track.details.rel;
            this.dispatchEvent(new CustomEvent('slide', { detail: { value: index }}));
            this.selectSlot(index);
        });
        this.slider.on('dragStarted', () => this.isSliding = true);
        this.slider.on('dragEnded', () => this.isSliding = false);
        this.resizeObserver = new ResizeObserver(debounce(() => {
            this.slider?.update();
        }, 200));
        this.resizeObserver.observe(this.shadowRoot.firstElementChild);
    }

    public goToPage(index: number) {
        this.selectSlot(index);
    }

    public goToPreviousPage() {
        this.selectSlot(this.selected - 1   );
    }

    public goToNextPage() {
        this.selectSlot(this.selected + 1);
    }

    protected onDotClick(index) {
        if(this.dotsClickable) {
            this.selectSlot(index);
        }
    }

    protected selectSlot(index: number) {
        if(index >= 0 && index < this.size) {
            this.selected = index;
        }
    }

    protected render(): TemplateResult {
        return html`
            <div id="swipable-wrapper">
                <div style="display: flex; align-items: center;">

                    ${when(this.arrows, () => html`
                        <div class="swipable-arrow-container" style="opacity: ${this.isSliding || (this.selected - 1 < 0) ? '0.3' : '1'}" @click="${() => this.goToPreviousPage()}">
                            ${when(true, () => html`
                                <or-icon icon="chevron-left"></or-icon>`)}
                        </div>
                    `)}

                    <!-- Slider -->
                    <div id="slider" style="${this.arrows ? 'margin: 0 -4px' : nothing}">
                        ${map(range(this.size), index => {
                            return html`
                                <div class="keen-slider__slide">
                                    <div style="display: flex; align-items: center; justify-content: center; height: 100%;">
                                        <slot name="${index}"></slot>
                                    </div>
                                </div>
                            `;
                        })}
                    </div>

                    ${when(this.arrows, () => html`
                        <div class="swipable-arrow-container" style="opacity: ${this.isSliding || (this.selected + 1 === this.size) ? '0.3' : '1'}" @click="${() => this.goToNextPage()}">
                            ${when(true, () => html`
                                <or-icon icon="chevron-right"></or-icon>`)}
                        </div>
                    `)}

                </div>

                <!-- Bottom controls -->
                ${guard([this.dots, this.size, this.dotsClickable, this.selected, this.language], () => html`
                    ${when(this.dots, () => html`
                        <div id="swipable-controls">
                            ${map(range(this.size), index => {
                                const classes = {
                                    'dot': true,
                                    'dot--clickable': this.dotsClickable,
                                    'dot--selected': index === this.selected
                                };
                                return html`
                                    <div class="${classMap(classes)}" @click="${() => this.onDotClick(index)}"></div>
                                `;
                            })}
                        </div>
                    `)}
                `)}
            </div>
        `;
    }
}
