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

package io.barracks.membergateway.rest;


import com.fasterxml.jackson.core.JsonProcessingException;
import io.barracks.membergateway.manager.MessageManager;
import io.barracks.membergateway.security.UserAuthentication;
import io.barracks.membergateway.utils.RandomPrincipal;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.security.Principal;
import java.util.UUID;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class MessageResourceTest {

    @Mock
    private MessageManager messageManager;

    private MessageResource messageResource;

    private Principal principal = new RandomPrincipal();

    @Before
    public void setUp() {
        messageResource = new MessageResource(messageManager);
    }

    @Test
    public void sendMessage_whenAllIsFine_shouldCallManager() {
        //Given
        final String unitId = UUID.randomUUID().toString();
        final String message = UUID.randomUUID().toString();
        final String apiKey = ((UserAuthentication)principal).getDetails().getApiKey();
        doNothing().when(messageManager).sendMessage(apiKey, unitId, message);

        //When
        messageResource.sendMessage(message, unitId, principal);

        //Then
        verify(messageManager).sendMessage(apiKey, unitId, message);
    }
}
