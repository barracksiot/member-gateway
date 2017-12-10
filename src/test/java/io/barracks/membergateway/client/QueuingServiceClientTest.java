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


import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static io.barracks.membergateway.client.QueuingServiceClient.QUEUE_NAME_PATTERN;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class QueuingServiceClientTest {

    private QueuingServiceClient queuingServiceClient;

    private String exchangeName = "test_exchange";

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private RabbitAdmin rabbitAdmin;

    @Before
    public void setUp() {
        queuingServiceClient = new QueuingServiceClient(exchangeName, rabbitAdmin, rabbitTemplate);
    }

    @Test
    public void sendDataToQueue_whenServiceSucceeds_serverShouldBeCalled() {
        // Given
        final String message = UUID.randomUUID().toString();
        final String userId = UUID.randomUUID().toString();
        final String unitId = UUID.randomUUID().toString();
        final String routingKey = userId + "." + unitId;
        final String queueName = String.format(QUEUE_NAME_PATTERN, routingKey);
        final MessageProperties properties = new MessageProperties();
        final Message rabbitMessage = new Message(message.getBytes(StandardCharsets.UTF_8), properties);

        doNothing().when(rabbitAdmin).declareBinding(any());
        doReturn(queueName).when(rabbitAdmin).declareQueue(any());

        // When
        queuingServiceClient.sendMessage(userId, unitId, message);

        // Then
        verify(rabbitTemplate).send(exchangeName, routingKey, rabbitMessage);
        verify(rabbitAdmin).declareBinding(any());
        verify(rabbitAdmin).declareQueue(any());
    }

}
