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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.StringJoiner;

import static org.openremote.agent.custom.earne.EarneMeterProtocol.OBJECT_MAPPER;

public class EarneEnodeControlRequestMessage {

    // Common classes

    public interface Command {
        String toString();
    }

    public enum Discipline {
        CHARGER, EV, HVAC;

        @JsonCreator
        public static Discipline fromString(String s) {
            return Discipline.valueOf(s.toUpperCase());
        }

        @JsonValue
        public String toLower() {
            return name().toLowerCase();
        }
    }

    // Charger/EV control command classes

    public enum ChargingControlCommand implements Command {
        START, STOP;
    }

    public static class ChargingControlRequestMessage extends EarneEnodeControlRequestMessage {
        @Deprecated // Default constructor for Jackson deserialization only
        public ChargingControlRequestMessage() {
        }

        public ChargingControlRequestMessage(String userId, String deviceId, Discipline discipline, ChargingControlCommand command) {
            super(userId, deviceId, discipline, command);
        }
    }

    // HVAC control command classes

    public enum HVACControlCommand implements Command {
        AUTO, COOL, HEAT, FOLLOW_SCHEDULE, OFF;
    }

    public static class HVACControlRequestMessage extends EarneEnodeControlRequestMessage {
        Double coolSetpoint;
        Double heatSetpoint;

        @Deprecated // Default constructor for Jackson deserialization only
        public HVACControlRequestMessage() {
        }

        public HVACControlRequestMessage(String userId, String deviceId, HVACControlCommand command) {
            super(userId, deviceId, Discipline.HVAC, command);
        }

        public HVACControlRequestMessage(String userId, String deviceId, HVACControlCommand command, Double coolSetpoint, Double heatSetpoint) {
            super(userId, deviceId, Discipline.HVAC, command);
            this.coolSetpoint = coolSetpoint;
            this.heatSetpoint = heatSetpoint;
        }

        @Override
        public String toString() {
            return new StringJoiner(", ", HVACControlRequestMessage.class.getSimpleName() + "[", "]")
                    .add("userId='" + userId + "'")
                    .add("deviceId='" + deviceId + "'")
                    .add("discipline=" + discipline)
                    .add("command='" + command + "'")
                    .add("coolSetpoint=" + coolSetpoint)
                    .add("heatSetpoint=" + heatSetpoint)
                    .toString();
        }
    }

    // Common fields
    public String userId;
    public String deviceId;
    public Discipline discipline;
    public String command;

    @Deprecated // Default constructor for Jackson deserialization only
    EarneEnodeControlRequestMessage() {
    }

    private EarneEnodeControlRequestMessage(String userId, String deviceId, Discipline discipline, Command command) {
        this.userId = userId;
        this.deviceId = deviceId;
        this.discipline = discipline;
        this.command = command.toString();
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", EarneEnodeControlRequestMessage.class.getSimpleName() + "[", "]")
                .add("userId='" + userId + "'")
                .add("deviceId='" + deviceId + "'")
                .add("discipline=" + discipline)
                .add("command='" + command + "'")
                .toString();
    }

    public static EarneEnodeControlRequestMessage fromJsonNode(JsonNode node) {
        if (node == null) {
            return null;
        }

        JsonNode disciplineNode = node.get("discipline");
        if (disciplineNode == null || disciplineNode.isNull()) {
            return null;
        }

        try {
            Discipline discipline = Discipline.fromString(disciplineNode.asText());
            Class<?> commandClass = discipline == Discipline.HVAC ? HVACControlRequestMessage.class : ChargingControlRequestMessage.class;
            return (EarneEnodeControlRequestMessage) OBJECT_MAPPER.convertValue(node, commandClass);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
