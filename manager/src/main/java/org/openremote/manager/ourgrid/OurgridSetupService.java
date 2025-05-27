package org.openremote.manager.ourgrid;

import org.apache.commons.io.IOUtils;
import org.openremote.agent.custom.ourgrid.*;
import org.openremote.container.persistence.PersistenceService;
import org.openremote.manager.asset.AssetProcessingService;
import org.openremote.manager.asset.AssetStorageService;
import org.openremote.manager.event.ClientEventService;
import org.openremote.model.Constants;
import org.openremote.model.Container;
import org.openremote.model.ContainerService;
import org.openremote.model.asset.AssetFilter;
import org.openremote.model.asset.impl.ElectricityProducerSolarAsset;
import org.openremote.model.asset.impl.ThingAsset;
import org.openremote.model.attribute.Attribute;
import org.openremote.model.attribute.AttributeEvent;
import org.openremote.model.attribute.MetaItem;
import org.openremote.model.query.AssetQuery;
import org.openremote.model.rules.RealmRuleset;
import org.openremote.model.syslog.SyslogCategory;
import org.openremote.model.util.UniqueIdentifierGenerator;
import org.openremote.model.value.MetaItemType;
import org.openremote.model.value.ValueType;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import static org.openremote.model.rules.Ruleset.Lang.GROOVY;
import static org.openremote.model.syslog.SyslogCategory.DATA;

public class OurgridSetupService implements ContainerService {
    protected static final Logger LOG = SyslogCategory.getLogger(DATA, OurgridSetupService.class.getName());

    protected AssetProcessingService assetProcessingService;
    protected AssetStorageService assetStorageService;
    protected ClientEventService clientEventService;
    protected PersistenceService persistenceService;
    protected ScheduledExecutorService scheduledExecutorService;


    @Override
    public void init(Container container) throws Exception {
        assetProcessingService = container.getService(AssetProcessingService.class);
        assetStorageService = container.getService(AssetStorageService.class);
        clientEventService = container.getService(ClientEventService.class);
        persistenceService = container.getService(PersistenceService.class);
        scheduledExecutorService = container.getScheduledExecutor();
    }

    @Override
    public void start(Container container) throws Exception {
        // List of asset types that are part of this service
        String[] assetTypes = {
                OurgridSetupAsset.DESCRIPTOR.getName()
        };

        // Listen to attribute events of listed asset types
        clientEventService.addSubscription(
                AttributeEvent.class,
                new AssetFilter<AttributeEvent>().setAssetTypes(assetTypes),
                this::processAttributeEvent);
    }

    @Override
    public void stop(Container container) throws Exception {
    }

    private void processAttributeEvent(AttributeEvent attributeEvent) {
        String assetType = attributeEvent.getAssetType();

        if (assetType.equals(OurgridSetupAsset.DESCRIPTOR.getName())) {
            processAttributeEventOurgridSetupAsset(attributeEvent);
        }
    }

    private void processAttributeEventOurgridSetupAsset(AttributeEvent attributeEvent) {
        String attributeName = attributeEvent.getName();
        String assetId = attributeEvent.getId();

        if (attributeName.equals(OurgridSetupAsset.CREATE_DISTRICT.getName())) {
            boolean checkboxValue = (Boolean) attributeEvent.getValue().orElse(false);

            // Get asset from database
            OurgridSetupAsset ourgridSetupAsset = (OurgridSetupAsset) assetStorageService.find(assetId);

            if (ourgridSetupAsset == null || !checkboxValue) {
                return;
            }

            // Add a 1-second delay before resetting checkbox for user-friendliness
            scheduledExecutorService.schedule(() -> assetProcessingService.sendAttributeEvent(new AttributeEvent(ourgridSetupAsset.getId(), OurgridSetupAsset.CREATE_DISTRICT, false), getClass().getSimpleName()), 1, TimeUnit.SECONDS);

            try {
                String infoFieldMessage = createDistrict(ourgridSetupAsset);

                // Add a 1-second delay to ensure the info field is updated after all assets and rules are merged
                scheduledExecutorService.schedule(() -> assetProcessingService.sendAttributeEvent(new AttributeEvent(ourgridSetupAsset.getId(), OurgridSetupAsset.INFO_FIELD, infoFieldMessage), getClass().getSimpleName()), 1, TimeUnit.SECONDS);
            } catch (Exception e) {
                LOG.warning(String.format("assetName='%s', assetId='%s'; An exception occurred during district creation; Exception: %s", ourgridSetupAsset.getName(), ourgridSetupAsset.getId(), e));
            }
        }
    }

    private String createDistrict(OurgridSetupAsset ourgridSetupAsset) {
        String districtName = ourgridSetupAsset.getDistrictName().orElse("");

        // Initiate info field string
        String infoFieldMessage;

        // Check if district name is provided
        if (districtName.isBlank()) {
            infoFieldMessage = "District not created:\n" +
                    "  - Set district name";
            return infoFieldMessage;
        }

        String districtAssetName = "OurGrid " + districtName + " District";

        // Check if district already exists
        List<OurgridDistrictAsset> ourgridDistrictAssets = assetStorageService
                .findAll(new AssetQuery().types(OurgridDistrictAsset.class).names(districtAssetName))
                .stream()
                .map(asset -> (OurgridDistrictAsset) asset)
                .toList();

        if (!ourgridDistrictAssets.isEmpty()) {
            infoFieldMessage = "District not created:\n" +
                    "  - \"" + districtAssetName + "\" already exists";
            return infoFieldMessage;
        }


        // Create District Asset
        OurgridDistrictAsset ourgridDistrictAsset = new OurgridDistrictAsset(districtAssetName);
        ourgridDistrictAsset.setId(UniqueIdentifierGenerator.generateId()).setParent(ourgridSetupAsset);

        // Create Meter Sum Asset
        OurgridMeterSumAsset ourgridMeterSumAsset = new OurgridMeterSumAsset("OurGrid " + districtName + " Household Meters");
        ourgridMeterSumAsset.setId(UniqueIdentifierGenerator.generateId()).setParent(ourgridDistrictAsset);

        // Create Meter Asset
        OurgridMeterAsset ourgridMeterAsset = new OurgridMeterAsset("OurGrid " + districtName + " Meter 1");
        ourgridMeterAsset.setId(UniqueIdentifierGenerator.generateId()).setParent(ourgridMeterSumAsset);

        // Create Battery Asset
        OurgridBatteryAsset ourgridBatteryAsset = new OurgridBatteryAsset("OurGrid " + districtName + " Battery 1");
        ourgridBatteryAsset.setId(UniqueIdentifierGenerator.generateId()).setParent(ourgridMeterAsset);

        // Create Challenges Asset
        OurgridChallengesAsset ourGridChallengesAsset = new OurgridChallengesAsset("OurGrid " + districtName + " Challenges");
        ourGridChallengesAsset.setId(UniqueIdentifierGenerator.generateId()).setParent(ourgridDistrictAsset);

        // Create Peaks Asset
        OurgridPeaksAsset ourGridPeaksAsset = new OurgridPeaksAsset("OurGrid " + districtName + " Peaks");
        ourGridPeaksAsset.setId(UniqueIdentifierGenerator.generateId()).setParent(ourgridDistrictAsset);

        // Create Solar Asset
        ElectricityProducerSolarAsset solarAsset = new ElectricityProducerSolarAsset("OurGrid " + districtName + " Solar Production");
        solarAsset.setId(UniqueIdentifierGenerator.generateId()).setParent(ourgridDistrictAsset);

        solarAsset.addOrReplaceAttributes(
                new Attribute<>("location", ValueType.GEO_JSON_POINT)
                        .addOrReplaceMeta(
                                new MetaItem<>(MetaItemType.RULE_STATE)
                        ),
                new Attribute<>("powerForecast", ValueType.NUMBER)
                        .addOrReplaceMeta(
                                new MetaItem<>(MetaItemType.HAS_PREDICTED_DATA_POINTS),
                                new MetaItem<>(MetaItemType.READ_ONLY),
                                new MetaItem<>(MetaItemType.RULE_STATE),
                                new MetaItem<>(MetaItemType.STORE_DATA_POINTS)
                        )
        );

        // Create Research Asset
        ThingAsset researchAsset = new ThingAsset("OurGrid " + districtName + " Research");
        researchAsset.setId(UniqueIdentifierGenerator.generateId()).setParent(ourgridDistrictAsset);

        researchAsset.addOrReplaceAttributes(
                new Attribute<>("netPowerForecast", ValueType.NUMBER)
                        .addOrReplaceMeta(
                                new MetaItem<>(MetaItemType.DATA_POINTS_MAX_AGE_DAYS, 366),
                                new MetaItem<>(MetaItemType.READ_ONLY),
                                new MetaItem<>(MetaItemType.RULE_STATE),
                                new MetaItem<>(MetaItemType.STORE_DATA_POINTS),
                                new MetaItem<>(MetaItemType.UNITS, Constants.units(Constants.UNITS_KILO, Constants.UNITS_WATT))
                        )
        );


        // Set default values District Asset
        ourgridDistrictAsset
                .setActivePeriod(5)
                .setPowerImportCriticalPercentage(80)
                .setChallengesAssetId(ourGridChallengesAsset.getId())
                .setPeaksAssetId(ourGridPeaksAsset.getId());

        // Set default values Challenges Asset
        ourGridChallengesAsset
                .setChallengeDuration(60)
                .setChallengeEarnPointInterval(6)
                .setChallengeWait(15)
                .setChallengeDefaultPowerLimitMethod(OurgridChallengesAsset.ChallengePowerLimitValueType.valueOf("ladder"))
                .setChallengePowerLimitInterval(100)
                .setChallengePowerLimitMaximum(3000)
                .setChallengePowerLimitMinimum(2000)
                .setChallengePowerLimitPromotion(20)
                .setChallengePowerLimitTarget(2000);

        // Set default values Peaks Asset
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

        ourGridPeaksAsset
                .setConnectionQualityThreshold(80.0)
                .setPeakConsumptionThreshold(20.0)
                .setPeakPeriods(peakPeriods)
                .setPeakPointsDay(2.0);

        // Set default values Solar Asset
        solarAsset
                .setIncludeForecastSolarService(true)
                .setPanelAzimuth(0)
                .setPanelOrientation(ElectricityProducerSolarAsset.PanelOrientation.SOUTH)
                .setPanelPitch(0);


        // Merge assets into database
        assetStorageService.merge(ourgridDistrictAsset);
        assetStorageService.merge(ourgridMeterSumAsset);
        assetStorageService.merge(ourgridMeterAsset);
        assetStorageService.merge(ourgridBatteryAsset);
        assetStorageService.merge(ourGridChallengesAsset);
        assetStorageService.merge(ourGridPeaksAsset);
        assetStorageService.merge(solarAsset);
        assetStorageService.merge(researchAsset);


        // Setup rules
        String realmName = ourgridSetupAsset.getRealm();
        String rulesName1 = "OurGrid: " + districtName + " District rules";
        String rulesName2 = "OurGrid: " + districtName + " District Batteries rules";

        try (InputStream inputStream = OurgridSetupService.class.getResourceAsStream("/ourgrid/rules/OurgridDistrictRules.groovy")) {
            if (inputStream != null) {
                String rules = IOUtils.toString(inputStream, StandardCharsets.UTF_8);

                rules = rules.replaceFirst("setId1", ourgridDistrictAsset.getId());
                rules = rules.replaceFirst("setId2", ourgridMeterSumAsset.getId());
                rules = rules.replaceFirst("setId3", solarAsset.getId());
                rules = rules.replaceFirst("setId4", researchAsset.getId());
                rules = rules.replaceFirst("setId5", ourGridChallengesAsset.getId());
                rules = rules.replaceFirst("setId6", ourGridPeaksAsset.getId());
                RealmRuleset districtRuleSet = new RealmRuleset(realmName, rulesName1, GROOVY, rules);

                // Merge rules into database
                persistenceService.doReturningTransaction(entityManager -> entityManager.merge(districtRuleSet));
            }
        } catch (Exception e) {
            LOG.warning(String.format("assetName='%s', assetId='%s'; Rule '%s' was not created for district '%s'; Exception: %s", rulesName1 , ourgridSetupAsset.getName(), ourgridSetupAsset.getId(), districtAssetName, e));
        }

        try (InputStream inputStream = OurgridSetupService.class.getResourceAsStream("/ourgrid/rules/OurgridBatteriesRules.groovy")) {
            if (inputStream != null) {

                String rules = IOUtils.toString(inputStream, StandardCharsets.UTF_8);

                rules = rules.replaceFirst("setId1", ourgridMeterSumAsset.getId());
                RealmRuleset districtRuleSet = new RealmRuleset(realmName, rulesName2, GROOVY, rules);

                // Merge rules into database
                persistenceService.doReturningTransaction(entityManager -> entityManager.merge(districtRuleSet));
            }
        } catch (Exception e) {
            LOG.warning(String.format("assetName='%s', assetId='%s'; Rule '%s' was not created for district '%s'; Exception: %s", rulesName2 , ourgridSetupAsset.getName(), ourgridSetupAsset.getId(), districtAssetName, e));
        }

        infoFieldMessage = "Created district \"" + districtAssetName + "\":\n" +
                "\n" +
                "1) Drag district to your preferred location\n" +
                "2) Fill in missing input variables\n" +
                "     District Asset:\n" +
                "      - Number of households\n" +
                "      - Power import max (kW)\n" +
                "     Challenges Asset:\n" +
                "      - Total budget (€)\n" +
                "      - Total points year prediction manual\n" +
                "     Solar Production Asset:\n" +
                "      - Power export max (kW)\n" +
                "      - Location\n" +
                "3) Adjust the default input variables to your specific requirements\n" +
                "4) Connect power meters manually or with the Earn-E Agent\n" +
                "5) Connect or remove batteries\n" +
                "6) Turn on challenges and peak points\n" +
                "7) Turn on dynamic solar capacity\n" +
                "\n" +
                "You can delete this setup asset after you have created your district";

        LOG.info(String.format("assetName='%s', assetId='%s'; Created district: '%s'", ourgridSetupAsset.getName(), ourgridSetupAsset.getId(), districtAssetName));

        return infoFieldMessage;
    }
}