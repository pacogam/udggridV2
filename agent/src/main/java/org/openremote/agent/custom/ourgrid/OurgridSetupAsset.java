/*
 * Copyright 2026, OpenRemote Inc.
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
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package org.openremote.agent.custom.ourgrid;

import jakarta.persistence.Entity;
import java.util.Optional;
import org.openremote.model.asset.Asset;
import org.openremote.model.asset.AssetDescriptor;
import org.openremote.model.attribute.MetaItem;
import org.openremote.model.value.AttributeDescriptor;
import org.openremote.model.value.MetaItemType;
import org.openremote.model.value.ValueType;

@Entity
public class OurgridSetupAsset extends Asset<OurgridSetupAsset> {
  public static final AttributeDescriptor<Boolean> CREATE_DISTRICT =
      new AttributeDescriptor<>("createDistrict", ValueType.BOOLEAN);

  public static final AttributeDescriptor<String> DISTRICT_NAME =
      new AttributeDescriptor<>("districtName", ValueType.TEXT);

  public static final AttributeDescriptor<String> INFO_FIELD =
      new AttributeDescriptor<>(
          "infoField",
          ValueType.TEXT,
          new MetaItem<>(MetaItemType.MULTILINE),
          new MetaItem<>(MetaItemType.READ_ONLY));

  public static final AssetDescriptor<OurgridSetupAsset> DESCRIPTOR =
      new AssetDescriptor<>("application-cog-outline", "000000", OurgridSetupAsset.class);

  protected OurgridSetupAsset() {}

  public OurgridSetupAsset(String name) {
    super(name);
  }

  public Optional<String> getDistrictName() {
    return getAttributes().getValue(DISTRICT_NAME);
  }
}
