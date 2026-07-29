import {Manager} from "@openremote/core";

export class OgManager extends Manager {

}

export const ogManager = new OgManager(); // Needed for webpack bundling
export default ogManager;