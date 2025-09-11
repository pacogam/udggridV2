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

import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.StringJoiner;

/**
 * The {@link EarneEnodeControlResponseMessage} is sent as response to a {@link EarneEnodeControlRequestMessage}.
 */
public class EarneEnodeControlResponseMessage {

    public String userId;
    public Instant timeStamp;
    public String response;
    public JsonNode payload;

    boolean isSuccess() {
        return "SUCCESS".equals(response);
    }

    EarneEnodeControlRequestMessage getPayload() {
        return EarneEnodeControlRequestMessage.fromJsonNode(payload);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", EarneEnodeControlResponseMessage.class.getSimpleName() + "[", "]")
                .add("userId='" + userId + "'")
                .add("timeStamp=" + timeStamp)
                .add("response='" + response + "'")
                .add("payload='" + getPayload() + "'")
                .toString();
    }
}
