package org.openremote.agent.custom.gisce;


import jakarta.persistence.Entity;
import jakarta.persistence.criteria.CriteriaBuilder;
import org.openremote.model.asset.agent.Agent;
import org.openremote.model.asset.agent.AgentDescriptor;
import org.openremote.model.asset.agent.DefaultAgentLink;
import org.openremote.model.attribute.MetaItem;
import org.openremote.model.value.AttributeDescriptor;
import org.openremote.model.value.MetaItemType;
import org.openremote.model.value.ValueType;

import java.util.Optional;

import static org.openremote.model.Constants.*;

@Entity
public class CustomAgentGisce extends Agent<CustomAgentGisce, CustomProtocolGisce, DefaultAgentLink> {
  // Add custom agent attributes
  public static final AttributeDescriptor<String> BASE_URI = new AttributeDescriptor<>("baseURL", ValueType.HTTP_URL);
  public static final AttributeDescriptor<Boolean> AGENT_DISABLED = new AttributeDescriptor<>("agentDisabled", ValueType.BOOLEAN,
    new MetaItem<>(MetaItemType.READ_ONLY)
  );
//    public static final AttributeDescriptor<String> URL_ACCESS_TOKEN = new AttributeDescriptor<>("urlAccessToken", ValueType.TEXT,
//            new MetaItem<>(MetaItemType.READ_ONLY)
//    );
//    public static final AttributeDescriptor<String> REQUEST_PROPERTY_VALUE = new AttributeDescriptor<>("requestPropertyValue", ValueType.TEXT,
//            new MetaItem<>(MetaItemType.READ_ONLY)
//    );
//    public static final AttributeDescriptor<String> URL_POST_REQUEST = new AttributeDescriptor<>("urlPostRequest", ValueType.TEXT,
//            new MetaItem<>(MetaItemType.READ_ONLY)
//    );

  public static final AttributeDescriptor<Double> ENERGY_R1 = new AttributeDescriptor<>("energyR1", ValueType.NUMBER,
    new MetaItem<>(MetaItemType.LABEL, "Energy R1"),
    new MetaItem<>(MetaItemType.READ_ONLY),
    new MetaItem<>(MetaItemType.STORE_DATA_POINTS)
  ).withUnits(UNITS_KILO, UNITS_WATT, UNITS_HOUR);

  public static final AttributeDescriptor<Double> ENERGY_R2 = new AttributeDescriptor<>("energyR2", ValueType.NUMBER,
    new MetaItem<>(MetaItemType.LABEL, "Energy R2"),
    new MetaItem<>(MetaItemType.READ_ONLY),
    new MetaItem<>(MetaItemType.STORE_DATA_POINTS)
  ).withUnits(UNITS_KILO, UNITS_WATT, UNITS_HOUR);

  public static final AttributeDescriptor<Double> ENERGY_R3 = new AttributeDescriptor<>("energyR3", ValueType.NUMBER,
    new MetaItem<>(MetaItemType.LABEL, "Energy R3"),
    new MetaItem<>(MetaItemType.READ_ONLY),
    new MetaItem<>(MetaItemType.STORE_DATA_POINTS)
  ).withUnits(UNITS_KILO, UNITS_WATT, UNITS_HOUR);

  public static final AttributeDescriptor<Double> ENERGY_R4 = new AttributeDescriptor<>("energyR4", ValueType.NUMBER,
    new MetaItem<>(MetaItemType.LABEL, "Energy R4"),
    new MetaItem<>(MetaItemType.READ_ONLY),
    new MetaItem<>(MetaItemType.STORE_DATA_POINTS)
  ).withUnits(UNITS_KILO, UNITS_WATT, UNITS_HOUR);

  public static final AttributeDescriptor<Double> ENERGY_IMPORT = new AttributeDescriptor<>("energyImport", ValueType.NUMBER,
    new MetaItem<>(MetaItemType.LABEL, "Energy imported"),
    new MetaItem<>(MetaItemType.READ_ONLY),
    new MetaItem<>(MetaItemType.STORE_DATA_POINTS)
    //new MetaItem<>(MetaItemType.ACCESS_RESTRICTED_READ),
    //new MetaItem<>(MetaItemType.RULE_STATE)
  ).withUnits(UNITS_KILO, UNITS_WATT, UNITS_HOUR);

  public static final AttributeDescriptor<Double> ENERGY_EXPORT = new AttributeDescriptor<>("energyExport", ValueType.NUMBER,
    new MetaItem<>(MetaItemType.LABEL, "Energy exported"),
    new MetaItem<>(MetaItemType.READ_ONLY),
    new MetaItem<>(MetaItemType.STORE_DATA_POINTS)
    //new MetaItem<>(MetaItemType.ACCESS_RESTRICTED_READ),
    //new MetaItem<>(MetaItemType.RULE_STATE)
  ).withUnits(UNITS_KILO, UNITS_WATT, UNITS_HOUR);

  public static final AttributeDescriptor<String> ID_SUPERVISOR = new AttributeDescriptor<>("idSupervisor", ValueType.TEXT,
    new MetaItem<>(MetaItemType.LABEL, "Supervisor meter ID")
  );
  //    public static final AttributeDescriptor<Double> TOTAL_DAILY_ENERGY = new AttributeDescriptor<>("totalDailyEnergy", ValueType.NUMBER,
//            new MetaItem<>(MetaItemType.LABEL, "Total Daily Energy"),
//            new MetaItem<>(MetaItemType.READ_ONLY),
//            new MetaItem<>(MetaItemType.STORE_DATA_POINTS),
//            new MetaItem<>(MetaItemType.HAS_PREDICTED_DATA_POINTS)
//    ).withUnits(UNITS_WATT, UNITS_HOUR);
  public static final AttributeDescriptor<Double> TOTAL_HOURLY_ENERGY = new AttributeDescriptor<>("totalHourlyEnergy", ValueType.NUMBER,
    new MetaItem<>(MetaItemType.LABEL, "Total Hourly Energy"),
    new MetaItem<>(MetaItemType.READ_ONLY),
    new MetaItem<>(MetaItemType.STORE_DATA_POINTS),
    new MetaItem<>(MetaItemType.HAS_PREDICTED_DATA_POINTS)
  ).withUnits(UNITS_WATT, UNITS_HOUR);
  public static final AttributeDescriptor<Double> TOTAL_RECORDS = new AttributeDescriptor<>("totalRecords", ValueType.NUMBER,
    new MetaItem<>(MetaItemType.LABEL, "Number of records (last 3 days)"),
    new MetaItem<>(MetaItemType.READ_ONLY),
    new MetaItem<>(MetaItemType.STORE_DATA_POINTS)
  );
  public static final AttributeDescriptor<Double> ACTIVE_DEVICES = new AttributeDescriptor<>("activeDevices", ValueType.NUMBER,
    new MetaItem<>(MetaItemType.LABEL, "Active devices"),
    new MetaItem<>(MetaItemType.READ_ONLY),
    new MetaItem<>(MetaItemType.STORE_DATA_POINTS)
  );
  public static final AttributeDescriptor<Boolean> ENABLE_FORECAST = new AttributeDescriptor<>("enableForecast", ValueType.BOOLEAN,
    new MetaItem<>(MetaItemType.LABEL, "Enable External Forecast")
  );

  public static final AgentDescriptor<CustomAgentGisce, CustomProtocolGisce, DefaultAgentLink> DESCRIPTOR = new AgentDescriptor<>(
    CustomAgentGisce.class, CustomProtocolGisce.class, DefaultAgentLink.class
  );


  protected CustomAgentGisce() {
  }

  public CustomAgentGisce(String name) {
    super(name);
  }

  public CustomProtocolGisce getProtocolInstance() {
    return new CustomProtocolGisce(this);
  }


  // Add get functions for quick attribute value access in protocol
//    public Optional<String> getUrlAccessToken() {
//        return getAttributes().getValue(URL_ACCESS_TOKEN);
//    }
//
//    public Optional<String> getRequestPropertyValue() {
//        return getAttributes().getValue(REQUEST_PROPERTY_VALUE);
//    }
//
//    public Optional<String> getUrlPostRequest() {
//        return getAttributes().getValue(URL_POST_REQUEST);
//    }

  public Optional<String> getIdSupervisor() {
    return getAttributes().getValue(ID_SUPERVISOR);
  }

  public Optional<String> getBaseURI() {
    return getAttributes().getValue(BASE_URI);
  }

  public Optional<Boolean> getEnableForecast() {
    return getAttributes().getValue(ENABLE_FORECAST);
  }

}
