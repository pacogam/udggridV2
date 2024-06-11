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

import org.apache.commons.io.IOUtils;
import org.openremote.agent.custom.reschool.*;
import org.openremote.container.util.UniqueIdentifierGenerator;
import org.openremote.manager.setup.ManagerSetup;
import org.openremote.model.Constants;
import org.openremote.model.Container;
import org.openremote.model.asset.impl.CityAsset;
import org.openremote.model.asset.impl.ElectricityProducerSolarAsset;
import org.openremote.model.asset.impl.ThingAsset;
import org.openremote.model.attribute.Attribute;
import org.openremote.model.attribute.MetaItem;
import org.openremote.model.geo.GeoJSONPoint;
import org.openremote.model.rules.RealmRuleset;
import org.openremote.model.value.MetaItemType;
import org.openremote.model.value.ValueType;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.openremote.model.rules.Ruleset.Lang.GROOVY;

public class CustomManagerSetup extends ManagerSetup {

    public CustomManagerSetup(Container container) {
        super(container);
    }

    @Override
    public void onStart() throws Exception {
        super.onStart();

        //// Cities participating in project ////
        CityAsset cityAmsterdam = new CityAsset("Amsterdam")
                .setRealm(Constants.MASTER_REALM)
                .setLocation(new GeoJSONPoint(4.8936, 52.3727));
        cityAmsterdam = assetStorageService.merge(cityAmsterdam);


        //// Realm Amsterdam ////

        // Setup assets
        ReschoolDistrictAsset districtSporenburg = new ReschoolDistrictAsset("Sporenburg")
                .setRealm("amsterdam")
                .setId(UniqueIdentifierGenerator.generateId("amsterdam_sporenburg"))
                .setNumberOfHouseholds(500)
                .setPowerImportMax(1000.0)
                .setPowerImportCriticalPercentage(80);
        districtSporenburg = assetStorageService.merge(districtSporenburg);

        ReschoolMeterSumAsset reschoolMeterSumAsset = new ReschoolMeterSumAsset("Meters Sum")
                .setRealm("amsterdam")
                .setId(UniqueIdentifierGenerator.generateId("amsterdam_sporenburg_meters"))
                .setParent(districtSporenburg);
        reschoolMeterSumAsset = assetStorageService.merge(reschoolMeterSumAsset);

        ElectricityProducerSolarAsset solarSporenburg = new ElectricityProducerSolarAsset("Solar Production")
                .setRealm("amsterdam")
                .setId(UniqueIdentifierGenerator.generateId("amsterdam_sporenburg_solar"))
                .setParent(districtSporenburg)
                .setIncludeForecastSolarService(true)
                .setSetActualSolarValueWithForecast(true)
                .setPanelAzimuth(0)
                .setPanelOrientation(ElectricityProducerSolarAsset.PanelOrientation.SOUTH)
                .setPanelPitch(0)
                .setPowerExportMax(342.0)
                .setLocation(new GeoJSONPoint(4.94501, 52.37454));

        solarSporenburg.addOrReplaceAttributes(
                new Attribute<>("power", ValueType.NUMBER)
                        .addOrReplaceMeta(
                                new MetaItem<>(MetaItemType.READ_ONLY)
                        ),
                new Attribute<>("powerForecast", ValueType.NUMBER)
                        .addOrReplaceMeta(
                                new MetaItem<>(MetaItemType.READ_ONLY),
                                new MetaItem<>(MetaItemType.RULE_STATE),
                                new MetaItem<>(MetaItemType.STORE_DATA_POINTS)
                        )
        );
        solarSporenburg = assetStorageService.merge(solarSporenburg);

        OurGridChallengesAsset challengesAsset = new OurGridChallengesAsset("Challenges Asset")
                .setRealm("amsterdam")
                .setId(UniqueIdentifierGenerator.generateId("challengesAsset"))
                .setParent(districtSporenburg)
                .setTurnOnChallenges(true)
                .setChallengeWait(15L)
                .setChallengeDuration(60L)
                .setChallengeEarnPointInterval(6)
                .setChallengeDefaultPowerLimitMethod(OurGridChallengesAsset.ChallengePowerLimitValueType.valueOf("ladder"))
                .setChallengePowerLimitInterval(100)
                .setChallengePowerLimitMaximum(3000)
                .setChallengePowerLimitMinimum(2000)
                .setChallengePowerLimitPromotion(20)
                .setChallengePowerLimitTarget(2000);
        challengesAsset = assetStorageService.merge(challengesAsset);

        String peakPeriods = """
                [
                  {
                    "peak_period_start": "7:00",
                    "peak_period_end": "9:00"
                  },
                  {
                    "peak_period_start": "17:00",
                    "peak_period_end": "19:00"
                  }
                ]""";

        OurGridPeaksAsset peaksAsset = new OurGridPeaksAsset("Peaks Asset")
                .setRealm("amsterdam")
                .setId(UniqueIdentifierGenerator.generateId("peaksAsset"))
                .setParent(districtSporenburg);
//                .setTurnOnPeakPoints(true)
//                .setConnectionQualityThreshold(95.0)
//                .setPeakConsumptionThreshold(16.7)
//                .setPeakPeriods(peakPeriods)
//                .setPeakPointsDay(2.0);
        peaksAsset = assetStorageService.merge(peaksAsset);

        ThingAsset researchAsset1 = new ThingAsset("Research Asset")
                .setRealm("amsterdam")
                .setId(UniqueIdentifierGenerator.generateId("research_asset_1"))
                .setParent(districtSporenburg);

        researchAsset1.addOrReplaceAttributes(
                new Attribute<>("netPowerForecast", ValueType.NUMBER)
                        .addOrReplaceMeta(
                                new MetaItem<>(MetaItemType.READ_ONLY),
                                new MetaItem<>(MetaItemType.RULE_STATE),
                                new MetaItem<>(MetaItemType.STORE_DATA_POINTS),
                                new MetaItem<>(MetaItemType.UNITS, Constants.units(Constants.UNITS_KILO, Constants.UNITS_WATT))
                        )
        );
        researchAsset1 = assetStorageService.merge(researchAsset1);

        districtSporenburg
                .setChallengesAssetId(challengesAsset.getId())
                .setPeaksAssetId(peaksAsset.getId());
        districtSporenburg = assetStorageService.merge(districtSporenburg);


        // Setup agents
        final String RABBITMQ_HOST = "RABBITMQ_HOST";
        final String RABBITMQ_USERNAME = "RABBITMQ_USERNAME";
        final String RABBITMQ_PASSWORD = "RABBITMQ_PASSWORD";
        final String RABBITMQ_VIRTUALHOST = "RABBITMQ_VIRTUALHOST";
        final String RABBITMQ_QUEUE = "RABBITMQ_QUEUE";

        String host = System.getenv(RABBITMQ_HOST);
        String username = System.getenv(RABBITMQ_USERNAME);
        String password = System.getenv(RABBITMQ_PASSWORD);
        String virtualhost = System.getenv(RABBITMQ_VIRTUALHOST);
        String queue = System.getenv(RABBITMQ_QUEUE);

        ReschoolAgent reschoolAgentSporenburg = new ReschoolAgent("Earn-E Agent Sporenburg")
                .setRealm("amsterdam")
                .setMeterParentId(reschoolMeterSumAsset.getId())
                .setRabbitMqHost(host)
                .setRabbitMqUsername(username)
                .setRabbitMqPassword(password)
                .setRabbitMqVirtualhost(virtualhost)
                .setRabbitMqQueue(queue);
        reschoolAgentSporenburg = assetStorageService.merge(reschoolAgentSporenburg);


        // Setup rules
        try (InputStream inputStream = CustomManagerSetup.class.getResourceAsStream("/reschool/rules/SporenburgRules.groovy")) {
            String rules = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
            RealmRuleset sporenburgRuleSet = new RealmRuleset(
                    "amsterdam",
                    "Sporenburg rules",
                    GROOVY,
                    rules
            );
            rulesetStorageService.merge(sporenburgRuleSet);
        }
    }
}
