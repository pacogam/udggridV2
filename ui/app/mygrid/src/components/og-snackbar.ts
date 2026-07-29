import {OrMwcSnackbar, OrMwcSnackbarChangedEvent} from '@openremote/or-mwc-components/or-mwc-snackbar';
import { css } from 'lit';
import {getAppStyle} from '../styles';
import { customElement } from 'lit/decorators.js';

export function showSnackbar(hostElement: HTMLElement | undefined, text: string, buttonText?: string, buttonAction?: () => void): OgSnackbar {
    if (!hostElement) {
        hostElement = OgSnackbar.DialogHostElement || document.body;
    }

    const snackbar = new OgSnackbar();
    snackbar.text = text;
    snackbar.buttonText = buttonText;
    snackbar.buttonAction = buttonAction;
    snackbar.isOpen = true;
    snackbar.addEventListener(OrMwcSnackbarChangedEvent.NAME, (ev: OrMwcSnackbarChangedEvent) => {
        ev.stopPropagation();
        if (!ev.detail.opened) {
            window.setTimeout(() => {
                if (snackbar.parentElement) {
                    snackbar.parentElement.removeChild(snackbar);
                }
            }, 0);
        }
    });
    hostElement.append(snackbar);
    return snackbar;
}

const styling = css`
    .mdc-snackbar {
        z-index: 50;
    }
`;

@customElement("og-snackbar")
export class OgSnackbar extends OrMwcSnackbar {

    static get styles() {
        return [...super.styles, getAppStyle(), styling];
    }

}
