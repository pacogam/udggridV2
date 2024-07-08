package org.openremote.agent.custom.ourgrid;

import jakarta.persistence.Entity;
import org.openremote.model.asset.agent.Agent;
import org.openremote.model.asset.agent.AgentDescriptor;
import org.openremote.model.asset.agent.DefaultAgentLink;
import org.openremote.model.attribute.Attribute;
import org.openremote.model.attribute.MetaItem;
import org.openremote.model.value.AttributeDescriptor;
import org.openremote.model.value.MetaItemType;
import org.openremote.model.value.ValueType;

import java.util.Optional;

@Entity
public class OurgridTemplateAgent extends Agent<OurgridTemplateAgent, OurgridTemplateProtocol, DefaultAgentLink> {

    public static final AttributeDescriptor<Boolean> AGENT_DISABLED = new AttributeDescriptor<>("agentDisabled", ValueType.BOOLEAN,
            new MetaItem<>(MetaItemType.READ_ONLY)
    );

    public static final AttributeDescriptor<Boolean> CREATE_DISTRICT = new AttributeDescriptor<>("createDistrict", ValueType.BOOLEAN
    );

    public static final AttributeDescriptor<String> DISTRICT_NAME = new AttributeDescriptor<>("districtName", ValueType.TEXT
    );

    public static final AttributeDescriptor<String> INFO_FIELD = new AttributeDescriptor<>("infoField", ValueType.TEXT,
            new MetaItem<>(MetaItemType.MULTILINE),
            new MetaItem<>(MetaItemType.READ_ONLY)
    );


    public static final AgentDescriptor<OurgridTemplateAgent, OurgridTemplateProtocol, DefaultAgentLink> DESCRIPTOR = new AgentDescriptor<>(
            OurgridTemplateAgent.class, OurgridTemplateProtocol.class, DefaultAgentLink.class
    );

    protected OurgridTemplateAgent() {

    }

    public OurgridTemplateAgent(String name) {
        super(name);
    }

    public OurgridTemplateProtocol getProtocolInstance() {
        return new OurgridTemplateProtocol(this);
    }

    public Optional<Boolean> getCreateDistrict() {
        return getAttributes().getValue(CREATE_DISTRICT);
    }

    public Optional<String> getDistrictName() {
        return getAttributes().getValue(DISTRICT_NAME);
    }
}