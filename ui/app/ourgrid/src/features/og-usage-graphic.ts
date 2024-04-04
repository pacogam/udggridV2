import {css, html, LitElement, TemplateResult} from 'lit';
import {customElement, property, query} from 'lit/decorators.js';
import {styleMap} from 'lit/directives/style-map.js';
import {when} from 'lit/directives/when.js';
import {getAppStyle} from '../styles';
import {OgStateColor} from '../util/util';

const greenDotsVid = require('../../images/green_dots_light.mp4');
const redDotsVid = require('../../images/red_dots_light.mp4');
const darkGreenDotsVid = require('../../images/green_dots_dark.mp4');
const darkRedDotsVid = require('../../images/red_dots_dark.mp4');

export enum GraphicType {
    VIDEO_FULL, IMAGE_FULL, HEADER, FOOTER, HEADER_FOOTER
}

const styling = css`
  video {
    width: 100%;
  }

  @-webkit-keyframes breathe-opacity {
    0% {
      opacity: 1;
    }
    46% {
      opacity: 1;
    }
    50% {
      opacity: 0;
    }
    96% {
      opacity: 0;
    }
    100% {
      opacity: 1;
    }
  }
  @keyframes breathe-opacity {
    0% {
      opacity: 1;
    }
    46% {
      opacity: 1;
    }
    50% {
      opacity: 0;
    }
    96% {
      opacity: 0;
    }
    100% {
      opacity: 1;
    }
`;

@customElement('og-usage-graphic')
export class OgUsageGraphic extends LitElement {

    @property()
    protected type: GraphicType = GraphicType.VIDEO_FULL;

    @property()
    protected color: OgStateColor = OgStateColor.GREEN;

    @property() // Breathe animation from GREEN to RED in a loop. For static use. Might not work for all types.
    protected colorAnimation = false;

    @property() // Sets the background color
    protected dark = false;

    @property() // Fill to full width & height
    protected fill = false;

    @query('#fg-video')
    protected videoElem: HTMLVideoElement;

    static styles = [getAppStyle(), styling];

    protected render(): TemplateResult {
        const fullscreenStyling = styleMap({
            'object-fit': this.fill ? 'cover' : 'unset',
            'height': this.fill ? '100%' : 'auto',
            'width': '100%'
        });
        switch (this.type) {

            // Header image only, which is normally placed on top of the screen.
            case GraphicType.HEADER: {
                const headerFooterUrl = this.getHeaderFooterByColor(this.color, this.dark);
                return html`
                    <img src="${headerFooterUrl}" style="width: 100%;">
                `;
            }
            // Footer image only, which is normally placed at the bottom of the screen.
            case GraphicType.FOOTER: {
                const headerFooterUrl = this.getHeaderFooterByColor(this.color, this.dark);
                return html`
                    <img src="${headerFooterUrl}" style="width: 100%; rotate: 180deg;">
                `;
            }
            // Header AND footer images, which get static on the top and bottom of the viewport.
            case GraphicType.HEADER_FOOTER: {
                const headerFooterUrl = this.getHeaderFooterByColor(this.color, this.dark);
                return html`
                    <div style="height: 100%; display: flex; flex-direction: column; justify-content: space-between; padding: 16px;">
                        <img src="${headerFooterUrl}"/>
                        <img src="${headerFooterUrl}" style="rotate: 180deg;"/>
                    </div>
                `;
            }
            // Fullscreen image
            case GraphicType.IMAGE_FULL: {
                const imageUrl = this.getImageUrlByColor(this.color, this.dark);
                return html`
                    <div style="height: 100%;">
                        <img src="${imageUrl}" style="${fullscreenStyling}"/>
                    </div>
                `;
            }
            // Else, VIDEO_FULL is used, which is a video animation
            // If colorAnimation is set to 'true', it will transition between green/red every 5 seconds.
            default: {
                return this.getVideoTemplate(this.color, this.dark, this.fill, this.colorAnimation);
            }
        }
    }

    protected getVideoTemplate(color: OgStateColor = OgStateColor.GREEN, dark = false, fill = false, animate = false): TemplateResult {
        const videoUrl = this.getVideoUrlByColor(color, dark);
        const imageUrl = this.getImageUrlByColor(color, dark);
        const videoStyling = styleMap({
            'animation': animate ? 'breathe-opacity 4000ms infinite' : undefined,
            '-webkit-animation': animate ? 'breathe-opacity 4000ms infinite' : undefined,
            'object-fit': fill ? 'cover' : 'unset',
            'height': fill ? '100%' : 'auto',
            'position': animate ? 'absolute' : undefined,
            'left': animate ? '0' : undefined,
            'width': '100%'
        });

        // Switching video sources in HTML is apparently really tricky.
        // So we manually call video.load() after the render is complete.
        if(this.videoElem) {
            this.videoElem.pause();
            this.updateComplete.then(() => {
                this.videoElem?.load();
                this.videoElem?.play().catch(e => console.error(e));
            });
        } else {
            this.updateComplete.then(() => {
                this.videoElem?.play().catch(e => console.error(e));
            });
        }
        return html`
            <div style="height: 100%; position: relative;">
                <video id="fg-video" muted playsinline loop poster="${imageUrl}" style="${videoStyling}">
                    <source src="${videoUrl}" type="video/mp4">
                </video>
                ${when(animate, () => {
                    const redVideoUrl = this.getVideoUrlByColor(OgStateColor.RED, dark);
                    const redImageUrl = this.getImageUrlByColor(OgStateColor.RED, dark);
                    const backgroundStyling = styleMap({
                        'object-fit': fill ? 'cover' : 'unset',
                        'height': fill ? '100%' : 'auto',
                        'position': 'absolute',
                        'left': '0',
                        'width': '100%',
                        'z-index': '-1'
                    });
                    return html`
                        <video id="bg-video" autoplay muted playsinline loop poster="${redImageUrl}" style="${backgroundStyling}">
                            <source src="${redVideoUrl}" type="video/mp4">
                        </video>
                    `;
                })}
            </div>
        `;
    }

    protected getVideoUrlByColor(color: OgStateColor, dark = false) {
        switch (color) {
            case OgStateColor.RED: {
                return (dark ? darkRedDotsVid : redDotsVid);
            }
            default: {
                return (dark ? darkGreenDotsVid : greenDotsVid);
            }
        }
    }

    protected getImageUrlByColor(color: OgStateColor, dark = false): string {
        switch (color) {
            case OgStateColor.RED:
                return (dark ? 'images/red_dots_dark_frame.jpg' : 'images/red_dots_light_frame.jpg');
            default:
                return (dark ? 'images/green_dots_dark_frame.jpg' : 'images/green_dots_light_frame.jpg');
        }
    }

    // TODO: Support dark backgrounds & multiple colors
    protected getHeaderFooterByColor(_color: OgStateColor, _dark = false): string {
        return 'images/dots-onboarding.svg';
    }
}
