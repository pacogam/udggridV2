import {AppStateKeyed, Page, PageProvider} from '@openremote/or-app';
import {css} from 'lit';
import {getAppStyle} from '../../styles';
import {doAnimation} from '../../util/util';

export class OgPageProvider<S extends AppStateKeyed> implements PageProvider<S> {
    name: string;
    routes: string[];
    allowOffline?: boolean; // allow use during offline/disconnected state. Default is false.
    hideHeader?: boolean;
    skipDataCheck?: boolean;
    pageCreator: () => OgPage<S>;
}

export enum PageAnimationType {
    SWIPE_RIGHT, SWIPE_LEFT, FADE
}

const styling = css`
  /*:host {
    display: block;
    -webkit-animation: container-enter 300ms cubic-bezier(0.0, 0.0, 0.2, 1);
    -webkit-animation-fill-mode: forwards;
    animation: container-enter 300ms cubic-bezier(0.0, 0.0, 0.2, 1);
    animation-fill-mode: forwards;
  }*/
`;

export abstract class OgPage<S extends AppStateKeyed> extends Page<S> {

    protected readonly animationEnterType: PageAnimationType = PageAnimationType.FADE;
    protected readonly animationExitType: PageAnimationType = PageAnimationType.FADE;

    abstract get name(): string;

    async getLoadingPromise(prev?: string): Promise<void> {
        return;
    };

    static get styles(): any[] {
        return [getAppStyle(), styling];
    }

    stateChanged(_state: AppStateKeyed): void {
      // nothing here
    }

    public async doEnterAnimation(type?: PageAnimationType): Promise<void> {
        type = type || this.animationEnterType;
        await this.updateComplete;
        const elem = this.getAnimationElem();
        if(elem) {
            switch (type) {
                case PageAnimationType.SWIPE_LEFT: {
                    await doAnimation(elem, 'animate-swipeleft-enter', 300); break;
                }
                case PageAnimationType.SWIPE_RIGHT: {
                    await doAnimation(elem, 'animate-swiperight-enter', 300); break;
                }
                case PageAnimationType.FADE: {
                    await doAnimation(elem, 'animate-fade-enter', 200); break;
                }
                default: {
                    await doAnimation(elem, 'animate-fade-enter', 200); break;
                }
            }
        }
    }

    public async doExitAnimation(type?: PageAnimationType): Promise<void> {
        type = type || this.animationExitType;
        await this.updateComplete;
        const elem = this.getAnimationElem();
        if(elem) {
            switch (type) {
                case PageAnimationType.SWIPE_LEFT: {
                    await doAnimation(elem, 'animate-swipeleft-exit', 300); break;
                }
                case PageAnimationType.SWIPE_RIGHT: {
                    await doAnimation(elem, 'animate-swiperight-exit', 300); break;
                }
                case PageAnimationType.FADE: {
                    await doAnimation(elem, 'animate-fade-exit', 100); break;
                }
                default: {
                    await doAnimation(elem, 'animate-fade-exit', 100); break;
                }
            }
        }
    }

    protected getAnimationElem(): Element | undefined {
        const elem = this.shadowRoot?.firstElementChild;
        if(!elem) {
            console.error('Could not find animation element!');
        }
        return elem;
    }
}

