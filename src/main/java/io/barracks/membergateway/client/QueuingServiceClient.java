/*
 * MIT License
 *
 * Copyright (c) 2017 Barracks Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package io.barracks.membergateway.client;


import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Component
public class QueuingServiceClient {

    private final RabbitTemplate rabbitTemplate;
    private final RabbitAdmin rabbitAdmin;

    private String exchangeName;

    static final String QUEUE_NAME_PATTERN = "mqtt-subscription-%sqos1";

    @Autowired
    public QueuingServiceClient(
            @Value("${io.barracks.amqp.exchangename}") String exchangeName,
            RabbitAdmin rabbitAdmin,
            RabbitTemplate rabbitTemplate
    ) {
        this.rabbitAdmin = rabbitAdmin;
        this.exchangeName = exchangeName;
        this.rabbitTemplate = rabbitTemplate;
    }

    public void sendMessage(String apiKey, String unitId, String message) {
        final String routingKey = apiKey + "." + unitId;
        final String queueName = String.format(QUEUE_NAME_PATTERN, routingKey);

        final Map<String, Object> args = new HashMap<>();
        args.put("x-expires", 120960000);

        final Queue queue = new Queue(queueName, true, false, false, args);
        final Binding binding = new Binding(queueName, Binding.DestinationType.QUEUE, exchangeName, routingKey, new HashMap<>());
        rabbitAdmin.declareQueue(queue);
        rabbitAdmin.declareBinding(binding);

        final MessageProperties messageProperties = new MessageProperties();
        final Message rabbitMessage = new Message(message.getBytes(StandardCharsets.UTF_8), messageProperties);
        rabbitTemplate.send(exchangeName, routingKey, rabbitMessage);
    }

}
