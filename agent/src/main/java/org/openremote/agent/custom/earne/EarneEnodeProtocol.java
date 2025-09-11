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

import com.rabbitmq.client.DeliverCallback;
import org.openremote.manager.event.ClientEventService;
import org.openremote.model.Container;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

/**
 * EARN-E Enode protocol implementation.
 */
public class EarneEnodeProtocol extends AbstractEarneProtocol<EarneEnodeAgent> {

    public static final String PROTOCOL_DISPLAY_NAME = "Earne Enode";

    private ClientEventService clientEventService;

    // Message handlers
    private EarneEnodeControlResponseMessageHandler controlResponseMessageHandler;
    private EarneEnodeUpdateMessageHandler updateMessageHandler;

    private EarneEnodeControlHandler controlHandler;

    private String controlQueue = "";

    public EarneEnodeProtocol(EarneEnodeAgent agent) {
        super(agent);
    }

    @Override
    public String getProtocolName() {
        return PROTOCOL_DISPLAY_NAME;
    }

    @Override
    public String getProtocolInstanceUri() {
        return "earneenode://" + agent.getId();
    }

    @Override
    public void start(Container container) throws Exception {
        clientEventService = container.getService(ClientEventService.class);
        super.start(container);
    }

    protected String getRabbitMqControlQueue() {
        return agent.getRabbitMqControlQueue().orElse("");
    }

    protected String getRabbitMqUpdateQueue() {
        return agent.getRabbitMqUpdateQueue().orElse("");
    }

    protected boolean isValidConfiguration() {
        return super.isValidConfiguration() && Stream.of(getRabbitMqControlQueue(), getRabbitMqUpdateQueue()).noneMatch(String::isBlank);
    }

    @Override
    protected void doStart(Container container) {
        controlResponseMessageHandler = new EarneEnodeControlResponseMessageHandler(agent, assetService, assetStorageService);
        updateMessageHandler = new EarneEnodeUpdateMessageHandler(agent, assetService, assetStorageService);
        controlHandler = new EarneEnodeControlHandler(assetStorageService, clientEventService, this);

        super.doStart(container);

        controlHandler.start();
    }

    @Override
    protected void doStop(Container container) {
        controlHandler.stop();
        super.doStop(container);
    }

    private void declareDurableQueue(String queueName) throws IOException {
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("durable", true); // TODO: Test if it is really needed because queueDeclare has a durable parameter.
        arguments.put("x-message-ttl", 60000); // 60 seconds
        arguments.put("x-queue-type", "classic");

        channel.queueDeclare(queueName, true, false, false, arguments);
    }

    protected void consumeMessages() throws IOException {
        this.controlQueue = getRabbitMqControlQueue();
        String updateQueue = getRabbitMqUpdateQueue();

        declareDurableQueue(controlQueue);
        declareDurableQueue(updateQueue);

        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
            boolean isControlResponse = message.contains("\"response\":");
            if (isControlResponse) {
                try {
                    controlResponseMessageHandler.handleMessage(message);
                } catch (RuntimeException e) {
                    LOG.info(String.format("agentName='%s', agentId='%s'; Control response message not processed: '%s'; Exception: %s", agent.getName(), agent.getId(), message, e));
                }
            } else {
                try {
                    updateMessageHandler.handleMessage(message);
                } catch (RuntimeException e) {
                    LOG.info(String.format("agentName='%s', agentId='%s'; Update message not processed: '%s'; Exception: %s", agent.getName(), agent.getId(), message, e));
                }
            }
        };

        channel.basicConsume(updateQueue, true, deliverCallback, consumerTag -> {
        });
    }

    public void publishControlMessage(String message) throws IOException {
        if (channel != null && !controlQueue.isBlank()) {
            channel.basicPublish("", controlQueue, null, message.getBytes(StandardCharsets.UTF_8));
            LOG.finer(() -> String.format("agentName='%s', agentId='%s'; Published control message: %s", agent.getName(), agent.getId(), message));
        } else {
            LOG.finer(() -> String.format("agentName='%s', agentId='%s'; No RabbitMQ connection to publish control message", agent.getName(), agent.getId()));
        }
    }
}

