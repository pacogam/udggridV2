/*
 * Copyright 2021, OpenRemote Inc.
 *
 * See the CONTRIBUTORS.txt file in the distribution for a
 * full listing of individual contributors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.openremote.manager.setup.custom;

import org.openremote.manager.setup.AbstractKeycloakSetup;
import org.openremote.model.Container;
import org.openremote.model.security.Realm;
import org.openremote.model.util.TextUtil;

import static org.openremote.container.util.MapAccess.getString;
import static org.openremote.model.Constants.MASTER_REALM;

public class CustomKeycloakSetup extends AbstractKeycloakSetup {

    public static final String CUSTOM_USER_PASSWORD = "CUSTOM_USER_PASSWORD";
    public static final String CUSTOM_USER_PASSWORD_DEFAULT = "custom";

    protected final String customUserPassword;

    public CustomKeycloakSetup(Container container, boolean isProduction) {
        super(container);

        customUserPassword = getString(container.getConfig(), CUSTOM_USER_PASSWORD, CUSTOM_USER_PASSWORD_DEFAULT);

        if (isProduction && TextUtil.isNullOrEmpty(customUserPassword)) {
            throw new IllegalStateException("Custom user password must be supplied in production");
        }
    }

    @Override
    public void onStart() throws Exception {

        Realm realmMaster = keycloakProvider.getRealm(MASTER_REALM);
        realmMaster.setAccountTheme("openremote");
        realmMaster.setLoginTheme("openremote");
        realmMaster.setEmailTheme("openremote");
        keycloakProvider.updateRealm(realmMaster);

        // Create custom realm
        Realm realmAmsterdam = createRealm("amsterdam", "Amsterdam", true);
        keycloakProvider.updateRealm(realmAmsterdam);

    }

    protected Realm createRealm(String realmName, String displayName, boolean rememberMe) {
        Realm realm = new Realm();
        realm.setName(realmName);
        realm.setDisplayName(displayName);
        realm.setRememberMe(rememberMe);
        realm.setRegistrationAllowed(true);
        realm.setDuplicateEmailsAllowed(false);
        realm.setRegistrationEmailAsUsername(true);
        realm.setResetPasswordAllowed(true);
        realm.setAccountTheme("ourgrid");
        realm.setLoginTheme("ourgrid");
        realm.setEmailTheme("ourgrid");
        realm.setEnabled(true);
        realm = keycloakProvider.createRealm(realm);

        // After creation, update "login with email" config
        realm.setLoginWithEmail(true);
        keycloakProvider.updateRealm(realm);

        return realm;
    }
}
