package org.openremote.agent.custom.ourgrid;

import org.apache.commons.io.IOUtils;
import org.openremote.agent.protocol.AbstractProtocol;
import org.openremote.container.persistence.PersistenceService;
import org.openremote.model.Constants;
import org.openremote.model.Container;
import org.openremote.model.asset.Asset;
import org.openremote.model.asset.agent.DefaultAgentLink;
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

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import static org.openremote.model.rules.Ruleset.Lang.GROOVY;
import static org.openremote.model.syslog.SyslogCategory.PROTOCOL;

public class OurgridTemplateProtocol extends AbstractProtocol<OurgridTemplateAgent, DefaultAgentLink> {
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private PersistenceService persistenceService;

    public static final String PROTOCOL_DISPLAY_NAME = "OurgridTemplate";
    private static final Logger LOG = SyslogCategory.getLogger(PROTOCOL, OurgridTemplateProtocol.class);

    public OurgridTemplateProtocol(OurgridTemplateAgent agent) {
        super(agent);
    }

    @Override
    public String getProtocolName() {
        return PROTOCOL_DISPLAY_NAME;
    }

    @Override
    public String getProtocolInstanceUri() {
        return "ourgridTemplate://" + agent.getId();
    }

    @Override
    protected void doStart(Container container) throws Exception {
        persistenceService = container.getService(PersistenceService.class);

        final Runnable beeper = () -> {
            createDistrict();
        };
        scheduler.scheduleAtFixedRate(beeper, 3, 5, TimeUnit.SECONDS);
    }

    @Override
    protected void doStop(Container container) throws Exception {
        scheduler.shutdown();
    }

    @Override
    protected void doLinkAttribute(String assetId, Attribute<?> attribute, DefaultAgentLink agentLink) throws RuntimeException {

    }

    @Override
    protected void doUnlinkAttribute(String assetId, Attribute<?> attribute, DefaultAgentLink agentLink) {

    }

    @Override
    protected void doLinkedAttributeWrite(DefaultAgentLink agentLink, AttributeEvent event, Object processedValue) {

    }

    private void createDistrict() {
        boolean createDistrictButton = agent.getCreateDistrict().orElse(false);
        String districtName = agent.getDistrictName().orElse("");

        if (!createDistrictButton) {
            return;
        }

        // Initiate info field string
        String infoFieldMessage;

        // Check if district name is provided
        if (districtName.isBlank()) {
            infoFieldMessage = "District not created:\n" +
                    "  - Set district name";
            sendAttributeEvent(new AttributeEvent(agent.getId(), OurgridTemplateAgent.INFO_FIELD.getName(), infoFieldMessage, timerService.getCurrentTimeMillis()));
            sendAttributeEvent(new AttributeEvent(agent.getId(), OurgridTemplateAgent.CREATE_DISTRICT.getName(), false, timerService.getCurrentTimeMillis()));
            return;
        }

        String districtAssetName = "OurGrid " + districtName + " District";

        // Check if district already exists
        List<Asset<?>> ourgridDistrictAssets = assetService.findAssets(new AssetQuery()
                .types(OurgridDistrictAsset.class)
                .names(districtAssetName)
        );

        if (ourgridDistrictAssets.size() > 0) {
            infoFieldMessage = "District not created:\n" +
                    "  - \"" + districtAssetName + "\" already exists";
            sendAttributeEvent(new AttributeEvent(agent.getId(), OurgridTemplateAgent.INFO_FIELD.getName(), infoFieldMessage, timerService.getCurrentTimeMillis()));
            sendAttributeEvent(new AttributeEvent(agent.getId(), OurgridTemplateAgent.CREATE_DISTRICT.getName(), false, timerService.getCurrentTimeMillis()));
            return;
        }

        // Create OurGrid District Asset
        OurgridDistrictAsset ourgridDistrictAsset = new OurgridDistrictAsset(districtAssetName);
        ourgridDistrictAsset.setId(UniqueIdentifierGenerator.generateId()).setParent(agent);

        // Create OurGrid Meter Sum Asset
        OurgridMeterSumAsset ourgridMeterSumAsset = new OurgridMeterSumAsset("OurGrid " + districtName + " Household Meters");
        ourgridMeterSumAsset.setId(UniqueIdentifierGenerator.generateId()).setParent(ourgridDistrictAsset);

        // Create 1 Meter Asset
        OurgridMeterAsset ourgridMeterAsset = new OurgridMeterAsset("OurGrid " + districtName + " Meter 1");
        ourgridMeterAsset.setId(UniqueIdentifierGenerator.generateId()).setParent(ourgridMeterSumAsset);

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
        assetService.mergeAsset(ourgridDistrictAsset);
        assetService.mergeAsset(ourgridMeterSumAsset);
        assetService.mergeAsset(ourgridMeterAsset);
        assetService.mergeAsset(ourGridChallengesAsset);
        assetService.mergeAsset(ourGridPeaksAsset);
        assetService.mergeAsset(solarAsset);
        assetService.mergeAsset(researchAsset);


        // Setup rules
        try (InputStream inputStream = OurgridTemplateProtocol.class.getResourceAsStream("/ourgrid/rules/OurgridDistrictRules.groovy")) {
            if (inputStream != null) {
                String realmName = agent.getRealm();
                String rulesName = districtAssetName + " rules";

                String rules = IOUtils.toString(inputStream, StandardCharsets.UTF_8);

                rules = rules.replaceFirst("uniqueId1", ourgridDistrictAsset.getId());
                rules = rules.replaceFirst("uniqueId2", ourgridMeterSumAsset.getId());
                rules = rules.replaceFirst("uniqueId3", solarAsset.getId());
                rules = rules.replaceFirst("uniqueId4", researchAsset.getId());
                rules = rules.replaceFirst("uniqueId5", ourGridChallengesAsset.getId());
                rules = rules.replaceFirst("uniqueId6", ourGridPeaksAsset.getId());
                RealmRuleset districtRuleSet = new RealmRuleset(realmName, rulesName, GROOVY, rules);

                // Merge rules into database
                persistenceService.doReturningTransaction(entityManager -> entityManager.merge(districtRuleSet));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        infoFieldMessage = "Created district: \"" + districtAssetName + "\"\n" +
                "To do:\n" +
                "1) Drag district to your preferred location\n" +
                "2) Fill in missing input variables\n" +
                "  District Asset:\n" +
                "    - Number of households\n" +
                "    - Power import max (kW)\n" +
                "  Challenges Asset:\n" +
                "    - Total budget (€)\n" +
                "    - Total points year prediction manual\n" +
                "  Solar Production Asset:\n" +
                "    - Power export max (kW)\n" +
                "    - Location\n" +
                "3) Adjust the default input variables to your specific district requirements\n" +
                "4) Connect power meters manually or with the Earn-E Agent\n" +
                "5) Turn on challenges and peak points\n" +
                "6) Turn on dynamic solar capacity" +
                "\n" +
                "You can remove this setup agent after you have created a district";

        sendAttributeEvent(new AttributeEvent(agent.getId(), OurgridTemplateAgent.INFO_FIELD.getName(), infoFieldMessage, timerService.getCurrentTimeMillis()));
        sendAttributeEvent(new AttributeEvent(agent.getId(), OurgridTemplateAgent.CREATE_DISTRICT.getName(), false, timerService.getCurrentTimeMillis()));

        LOG.info("Agent='" + agent.getName() + "'; Created district: \"" + districtAssetName + "\"");
    }
}