import {getListTemplate, ListItem, ListType, OrMwcList} from '@openremote/or-mwc-components/or-mwc-list';
import {TemplateResult, html, css} from 'lit';
import {customElement, property} from 'lit/decorators.js';
import {i18next} from '@openremote/or-translate';
import {styleMap} from 'lit/directives/style-map.js';
import {ifDefined} from 'lit/directives/if-defined.js';
import {classMap} from 'lit/directives/class-map.js';
import {getAppStyle} from "../styles";

export interface OgListItem extends ListItem {
    prefixImg?: string
}

const styling = css`
    .mdc-list-item__text {
        font-family: var(--og-font-family);
        font-size: var(--og-font-size-menu-options);
        font-weight: var(--og-font-weight-menu-options);
    }

    .mdc-list-item__graphic--dark {
        --or-icon-fill: var(--og-color-warning)
    }
`

// Extended class that uses OgListItem instead of ListItem to add extra fields.
// This sadly comes with a lot of duplicated code, because of how OrMwcList is built. :(
@customElement('og-mwc-list')
export class OgMwcList extends OrMwcList {

    public listItems?: (OgListItem | null)[];

    @property({type: Boolean})
    public dark = false;

    static get styles() {
        return [...super.styles, getAppStyle(), styling];
    }

    protected render() {
        const content = !this.listItems ? html`` : html`${this.listItems.map((listItem, index) =>
            this.getItemTemplate(listItem, index, (Array.isArray(this.values) ? this.values : this.values ? [this.values] : []), this.type)
        )}`;
        const isTwoLine = this.listItems && this.listItems.some(item => item && !!item.secondaryText);
        return getListTemplate(this.type, content, isTwoLine, undefined, ev => this._onSelected(ev));
    }


    /* ------------------------------------------------------ */

    // Duplicate of the exported function `getItemTemplate()` in or-mwc-list, but with minor edits.
    // To for example support prefix img instead of or-icon.
    protected getItemTemplate(item: OgListItem | null, index: number, selectedValues: any[], type: ListType, translate?: boolean, itemClickCallback?: (e: MouseEvent, item: OgListItem) => void) {
        if (item === null) {
            // Divider
            return html`
                <li role="separator" class="mdc-list-divider"></li>`;
        }

        const listItem = item as OgListItem;
        const multiSelect = type === ListType.MULTI_CHECKBOX || type === ListType.MULTI_TICK;
        const value = listItem.value;
        const isSelected = type !== ListType.PLAIN && selectedValues.length > 0 && selectedValues.some(v => v === value);
        const text = listItem.text ?? listItem.value;
        const secondaryText = listItem.secondaryText;
        let role: string | undefined = 'menuitem';
        let ariaSelected: string | undefined;
        let ariaChecked: string | undefined;
        let tabIndex: string | undefined;
        let textTemplate: TemplateResult | string = ``;
        let leftTemplate: TemplateResult | string = ``;
        let rightTemplate: TemplateResult | string = ``;
        let icon = listItem.icon;
        const prefixImg = listItem.prefixImg;
        let selectedClassName = 'mdc-list-item--selected';
        translate = translate || item.translate;

        if (multiSelect && type === ListType.MULTI_TICK) {
            icon = isSelected ? 'checkbox-marked' : 'checkbox-blank-outline';
        }

        if (type === ListType.MULTI_TICK || icon) {
            const classes = {
                'mdc-list-item__graphic': true,
                'mdc-list-item__graphic--dark': this.dark
            }
            leftTemplate = html`
                <span class="${classMap(classes)}">
                    <or-icon icon="${icon}"></or-icon>
                </span>
            `;
        } else if (prefixImg) {
            const classes = {
                'mdc-list-item__graphic': true,
                'mdc-list-item__graphic--dark': this.dark
            }
            leftTemplate = html`
                <span class="${classMap(classes)}">
                    <img src="${prefixImg}" width="24" height="24" alt="icon"/>
                </span>
            `;
        }

        if (listItem.trailingIcon) {
            rightTemplate = html`
                <span class="mdc-list-item__meta" aria-hidden="true">
                    <or-icon icon="${listItem.trailingIcon}"></or-icon>
                </span>
            `;
        }

        switch (type) {
            case ListType.SELECT:
                ariaSelected = isSelected ? 'true' : 'false';
                tabIndex = isSelected || ((!selectedValues || selectedValues.length === 0) && index === 0) ? '0' : undefined;
                role = 'option';
                break;
            case ListType.RADIO:
                ariaChecked = isSelected ? 'true' : 'false';
                role = 'radio';
                leftTemplate = html`
                    <span class="mdc-list-item__graphic">
                        <div class="mdc-radio">
                            <input class="mdc-radio__native-control" id="radio-item-${index + 1}" type="radio" value="${value}"/>
                            <div class="mdc-radio__background">
                                <div class="mdc-radio__outer-circle"></div>
                                <div class="mdc-radio__inner-circle"></div>
                            </div>
                        </div>
                    </span>
                `;
                break;
            case ListType.MULTI_CHECKBOX:
                ariaChecked = isSelected ? 'true' : 'false';
                role = 'checkbox';
                leftTemplate = html`
                    <div class="mdc-checkbox">
                        <input type="checkbox" class="mdc-checkbox__native-control"/>
                        <div class="mdc-checkbox__background">
                            <svg class="mdc-checkbox__checkmark" viewBox="0 0 24 24">
                                <path class="mdc-checkbox__checkmark-path" fill="none" d="M1.73,12.91 8.1,19.28 22.79,4.59"/>
                            </svg>
                            <div class="mdc-checkbox__mixedmark"></div>
                        </div>
                    </div>
                `;
                break;
            case ListType.MULTI_TICK:
                ariaChecked = isSelected ? 'true' : 'false';
                selectedClassName = 'mdc-list-item--selected';
                break;
            default:
                break;
        }

        if (text) {
            if (secondaryText !== undefined) {
                textTemplate = html`
                    <span class="mdc-list-item__text">
                        <span class="mdc-list-item__primary-text text-primary ${this.dark ? 'dark': undefined}">${translate ? i18next.t(text) : text}</span>
                        <span class="mdc-list-item__secondary-text text-secondary ${this.dark ? 'dark': undefined}">${translate ? i18next.t(secondaryText) : secondaryText}</span>
                    </span>
                `;
            } else {
                if (type === ListType.RADIO) {
                    textTemplate = html`<label class="mdc-list-item__text text-primary ${this.dark ? 'dark': undefined}" for="radio-item-${index + 1}">${translate ? i18next.t(text) : text}</label>`;
                } else {
                    textTemplate = html`<span class="mdc-list-item__text text-primary ${this.dark ? 'dark': undefined}" title="${translate ? i18next.t(text) : text}">${translate ? i18next.t(text) : text}</span>`;
                }
            }
        }

        return html`
            <li @click="${(e: MouseEvent) => {
                itemClickCallback && itemClickCallback(e, item);
            }}"
                style="${listItem.styleMap ? styleMap(listItem.styleMap) : ''}" class="mdc-list-item ${isSelected ? selectedClassName : ''}" role="${ifDefined(role)}" tabindex="${ifDefined(tabIndex)}"
                aria-checked="${ifDefined(ariaChecked)}" aria-selected="${ifDefined(ariaSelected)}" data-value="${value}">
                <span class="mdc-list-item__ripple"></span>
                ${leftTemplate}
                ${textTemplate}
                ${rightTemplate}
            </li>
        `;
    }
}
