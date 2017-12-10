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

package io.barracks.membergateway.manager;

import io.barracks.commons.exceptions.BarracksServiceClientException;
import io.barracks.membergateway.client.DeviceServiceClient;
import io.barracks.membergateway.client.exception.DeviceServiceClientException;
import io.barracks.membergateway.model.DeviceConfiguration;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpServerErrorException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class DeviceConfigurationManagerTest {
    @Mock
    private DeviceServiceClient deviceServiceClient;
    @InjectMocks
    private DeviceConfigurationManager deviceConfigurationManager;

    @Test
    public void getConfiguration_whenSucceeds_shouldReturnConfiguration() throws Exception {
        // Given
        final String userId = UUID.randomUUID().toString();
        final String unitId = UUID.randomUUID().toString();
        final DeviceConfiguration expected = DeviceConfiguration.builder().build();
        doReturn(expected).when(deviceServiceClient).getDeviceConfiguration(userId, unitId);

        // When
        final DeviceConfiguration result = deviceConfigurationManager.getConfiguration(userId, unitId);

        // Then
        assertEquals(expected, result);
    }

    @Test
    public void getConfiguration_whenClientFails_shouldThrowException() throws Exception {
        // Given
        final String userId = UUID.randomUUID().toString();
        final String unitId = UUID.randomUUID().toString();
        doThrow(new DeviceServiceClientException(new HttpServerErrorException(HttpStatus.NOT_FOUND)))
                .when(deviceServiceClient).getDeviceConfiguration(userId, unitId);

        // Then When
        assertThatExceptionOfType(BarracksServiceClientException.class)
                .isThrownBy(() -> deviceConfigurationManager.getConfiguration(userId, unitId));
        verify(deviceServiceClient).getDeviceConfiguration(userId, unitId);
    }

    @Test
    public void setConfiguration_whenClientSucceeds_shouldReturnConfiguration() throws Exception {
        // Given
        final String userId = UUID.randomUUID().toString();
        final String unitId = UUID.randomUUID().toString();
        final DeviceConfiguration configuration = DeviceConfiguration.builder().build();
        doReturn(configuration).when(deviceServiceClient).setDeviceConfiguration(userId, unitId, configuration);

        // When
        DeviceConfiguration result = deviceConfigurationManager.setConfiguration(userId, unitId, configuration);

        // Then
        verify(deviceServiceClient).setDeviceConfiguration(userId, unitId, configuration);
        assertEquals(result, configuration);
    }

    @Test
    public void setConfiguration_whenDeviceServiceFails_shouldThrowException() throws Exception {
        // Given
        final String userId = UUID.randomUUID().toString();
        final String unitId = UUID.randomUUID().toString();
        final DeviceConfiguration configuration = DeviceConfiguration.builder().build();
        final Exception exception = new DeviceServiceClientException(new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR));
        doThrow(exception).when(deviceServiceClient).setDeviceConfiguration(userId, unitId, configuration);

        // Then When
        assertThatExceptionOfType(BarracksServiceClientException.class)
                .isThrownBy(() -> deviceConfigurationManager.setConfiguration(userId, unitId, configuration));
        verify(deviceServiceClient).setDeviceConfiguration(userId, unitId, configuration);
    }

}
