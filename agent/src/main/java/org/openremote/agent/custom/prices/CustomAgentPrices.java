package org.openremote.agent.custom.prices;

import jakarta.persistence.Entity;
import org.openremote.agent.custom.prices.CustomProtocolPrices;
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
public class CustomAgentPrices extends Agent<CustomAgentPrices, CustomProtocolPrices, DefaultAgentLink> {
  // Add custom agent attributes
  public static final AttributeDescriptor<Boolean> AGENT_DISABLED = new AttributeDescriptor<>("agentDisabled", ValueType.BOOLEAN,
    new MetaItem<>(MetaItemType.READ_ONLY)
  );

  public static final AttributeDescriptor<Double> PRICE_PVPC = new AttributeDescriptor<>("pricePvpc", ValueType.NUMBER,
    new MetaItem<>(MetaItemType.LABEL, "Price PVPC (€/kWh)"),
    new MetaItem<>(MetaItemType.READ_ONLY),
    new MetaItem<>(MetaItemType.STORE_DATA_POINTS)
  );

  public static final AttributeDescriptor<Double> PRICE3BANDS = new AttributeDescriptor<>("price3Timebands", ValueType.NUMBER,
    new MetaItem<>(MetaItemType.LABEL, "Price 3 Time bands (€/kWh)"),
    new MetaItem<>(MetaItemType.READ_ONLY),
    new MetaItem<>(MetaItemType.STORE_DATA_POINTS)
  );

  public static final AttributeDescriptor<Double> PRICE_SALE_SURPLUS = new AttributeDescriptor<>("priceSaleSurplus", ValueType.NUMBER,
    new MetaItem<>(MetaItemType.LABEL, "Price Surplus (€/kWh)"),
    new MetaItem<>(MetaItemType.READ_ONLY),
    new MetaItem<>(MetaItemType.STORE_DATA_POINTS)
  );

  public static final AttributeDescriptor<Double> PRICE_SPOT = new AttributeDescriptor<>("priceSpot", ValueType.NUMBER,
    new MetaItem<>(MetaItemType.LABEL, "Price SPOT (€/kWh)"),
    new MetaItem<>(MetaItemType.READ_ONLY),
    new MetaItem<>(MetaItemType.STORE_DATA_POINTS)
    //new MetaItem<>(MetaItemType.ACCESS_RESTRICTED_READ),
    //new MetaItem<>(MetaItemType.RULE_STATE)
  );

  public static final AgentDescriptor<CustomAgentPrices, CustomProtocolPrices, DefaultAgentLink> DESCRIPTOR = new AgentDescriptor<>(
    CustomAgentPrices.class, CustomProtocolPrices.class, DefaultAgentLink.class
  );


  protected CustomAgentPrices() {
  }

  public CustomAgentPrices(String name) {
    super(name);
  }

  public CustomProtocolPrices getProtocolInstance() {
    return new CustomProtocolPrices(this);
  }

}
