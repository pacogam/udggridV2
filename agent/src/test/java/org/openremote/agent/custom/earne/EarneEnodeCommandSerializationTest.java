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

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.Test;
import org.openremote.agent.custom.earne.EarneEnodeControlRequestMessage.ChargingControlCommand;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.openremote.agent.custom.earne.EarneEnodeControlRequestMessage.*;

/**
 * Tests the serialization of {@link EarneEnodeControlRequestMessage}s to JSON.
 */
public class EarneEnodeCommandSerializationTest {

    void assertJsonEquals(String expectedJson, EarneEnodeControlRequestMessage actual) throws JsonProcessingException {
        assertEquals(expectedJson.trim(), actual.toJson());
    }

    @Test
    void testChargingControlRequestMessageSerialization() throws JsonProcessingException {
        String json = """
                {"userId":"user123","deviceId":"device123","discipline":"ev","command":"START"}
                """;
        EarneEnodeControlRequestMessage message = new ChargingControlRequestMessage("user123", "device123", Discipline.EV, ChargingControlCommand.START);
        assertJsonEquals(json, message);

        json = """
                {"userId":"user456","deviceId":"device456","discipline":"hvac","command":"STOP"}
                """;
        message = new ChargingControlRequestMessage("user456", "device456", Discipline.HVAC, ChargingControlCommand.STOP);
        assertJsonEquals(json, message);
    }

    @Test
    void testHVACControlCommandSerialization() throws JsonProcessingException {
        String json = """
                {"userId":"user789","deviceId":"device789","discipline":"hvac","command":"OFF"}
                """;
        EarneEnodeControlRequestMessage message = new HVACControlRequestMessage("user789", "device789", HVACControlCommand.OFF);
        assertJsonEquals(json, message);

        json = """
                {"userId":"user111","deviceId":"device222","discipline":"hvac","command":"AUTO"}
                """;
        message = new HVACControlRequestMessage("user111", "device222", HVACControlCommand.AUTO);
        assertJsonEquals(json, message);

        json = """
                {"userId":"user321","deviceId":"device321","discipline":"hvac","command":"FOLLOW_SCHEDULE"}
                """;
        message = new HVACControlRequestMessage("user321", "device321", HVACControlCommand.FOLLOW_SCHEDULE);
        assertJsonEquals(json, message);

        message = new HVACControlRequestMessage("user444", "device555", HVACControlCommand.COOL, 19.1, null);
        json = """
                {"userId":"user444","deviceId":"device555","discipline":"hvac","command":"COOL","coolSetpoint":19.1}
                """;
        assertJsonEquals(json, message);

        message = new HVACControlRequestMessage("user212", "device212", HVACControlCommand.HEAT, null, 22.3);
        json = """
                {"userId":"user212","deviceId":"device212","discipline":"hvac","command":"HEAT","heatSetpoint":22.3}
                """;
        assertJsonEquals(json, message);
    }

    @Test
    void testUnlinkCommandSerialization() throws JsonProcessingException {
        String json = """
                {"userId":"user480","command":"unlink"}
                """;
        EarneEnodeControlRequestMessage message = new UnlinkControlRequestMessage("user480");
        assertJsonEquals(json, message);
    }
}
