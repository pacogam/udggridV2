import { customElement, property } from 'lit/decorators.js';
import {OgPanel} from './og-panel';
import {Asset, User} from '@openremote/model';

@customElement('og-data-panel')
export abstract class OgDataPanel extends OgPanel {

    @property({type: Object})
    protected user: User;

    @property({type: Object})
    protected meterAsset: Asset;

    @property({type: Object})
    protected challengeAsset: Asset;

    @property({type: Object})
    protected districtAsset: Asset;

    @property({type: Object})
    protected peakPointsAsset: Asset;

    public setUser(user: User): this {
        this.user = user;
        return this;
    }

    public setMeterAsset(asset: Asset): this {
        this.meterAsset = asset;
        return this;
    }

    public setChallengeAsset(asset: Asset): this {
        this.challengeAsset = asset;
        return this;
    }

    public setDistrictAsset(asset: Asset): this {
        this.districtAsset = asset;
        return this;
    }

    public setPeakPointsAsset(asset: Asset): this {
        this.peakPointsAsset = asset;
        return this;
    }
}
