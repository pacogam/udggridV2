package org.openremote.agent.custom.ourgrid;

import jakarta.persistence.Entity;
import org.openremote.model.asset.Asset;
import org.openremote.model.asset.AssetDescriptor;
import org.openremote.model.attribute.MetaItem;
import org.openremote.model.value.AttributeDescriptor;
import org.openremote.model.value.MetaItemType;
import org.openremote.model.value.ValueType;

import java.util.Optional;

@Entity
public class OurgridSetupAsset extends Asset<OurgridSetupAsset> {
    public static final AttributeDescriptor<Boolean> CREATE_DISTRICT = new AttributeDescriptor<>("createDistrict", ValueType.BOOLEAN
    );

    public static final AttributeDescriptor<String> DISTRICT_NAME = new AttributeDescriptor<>("districtName", ValueType.TEXT
    );

    public static final AttributeDescriptor<String> INFO_FIELD = new AttributeDescriptor<>("infoField", ValueType.TEXT,
            new MetaItem<>(MetaItemType.MULTILINE),
            new MetaItem<>(MetaItemType.READ_ONLY)
    );


    public static final AssetDescriptor<OurgridSetupAsset> DESCRIPTOR = new AssetDescriptor<>("application-cog-outline", "000000", OurgridSetupAsset.class);

    protected OurgridSetupAsset() {
    }

    public OurgridSetupAsset(String name) {
        super(name);
    }

    public Optional<String> getDistrictName() {
        return getAttributes().getValue(DISTRICT_NAME);
    }
}
