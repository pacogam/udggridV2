/*
 * Copyright 2025, OpenRemote Inc.
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
package org.openremote.agent.custom.earne;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.openremote.manager.asset.AssetStorageService;
import org.openremote.model.asset.Asset;
import org.openremote.model.attribute.Attribute;
import org.openremote.model.attribute.AttributeEvent;
import org.openremote.model.protocol.ProtocolAssetService;
import org.openremote.model.util.ValueUtil;
import org.openremote.model.value.AttributeDescriptor;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public abstract class AbstractEarneMessageHandlerTest {

    public static class AttributeEventMatcher implements ArgumentMatcher<AttributeEvent> {

        private AttributeEvent left;

        AttributeEventMatcher(AttributeEvent left) {
            this.left = left;
        }

        @Override
        public boolean matches(AttributeEvent right) {
            return left.getRef().equals(right.getRef()) && (left == null || left.getValue().equals(right.getValue()));
        }
    }

    @Mock
    ProtocolAssetService assetServiceMock;

    @Mock
    AssetStorageService assetStorageServiceMock;

    @BeforeAll
    static void beforeAll() {
        ValueUtil.initialise(null);
    }

    abstract String getTestDataPathPrefix();

    void assertAssetAttributeValues(Asset<?> asset, Map<AttributeDescriptor<?>, Object> values) {
        for (Map.Entry<AttributeDescriptor<?>, Object> entry : values.entrySet()) {
            Optional<?> actualValue = asset.getAttribute(entry.getKey()).flatMap(Attribute::getValue);
            if (entry.getValue() == Optional.empty()) {
                assertEquals(entry.getValue(), actualValue);
            } else {
                assertEquals(entry.getValue(), actualValue.orElseThrow(() -> new AssertionError("Expected attribute '" + entry.getKey().getName() + "' to have a value, but it was empty.")));
            }
        }
    }

    String readFile(String fileName) throws IOException {
        String path = getTestDataPathPrefix() + fileName;
        URL url = getClass().getClassLoader().getResource(path);
        Objects.requireNonNull(url, "Could not find file: " + path);
        return Files.readString(Path.of(url.getPath()));
    }

    <T> void verifyAttributeEventSend(String assetId, AttributeDescriptor<T> descriptor, T value) {
        AttributeEventMatcher matcher = new AttributeEventMatcher(new AttributeEvent(assetId, descriptor, value));
        verify(assetServiceMock, times(1)).sendAttributeEvent(argThat(matcher));
    }

    <T> void verifyAttributeEventNotSend(String assetId, AttributeDescriptor<T> descriptor) {
        AttributeEventMatcher matcher = new AttributeEventMatcher(new AttributeEvent(assetId, descriptor, null));
        verify(assetServiceMock, never()).sendAttributeEvent(argThat(matcher));
    }
}
