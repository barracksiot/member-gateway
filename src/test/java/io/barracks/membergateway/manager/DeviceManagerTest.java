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

import com.google.common.collect.Lists;
import io.barracks.commons.test.PagedResourcesUtils;
import io.barracks.membergateway.client.DeviceServiceClient;
import io.barracks.membergateway.client.exception.DeviceServiceClientException;
import io.barracks.membergateway.model.BarracksQuery;
import io.barracks.membergateway.model.Device;
import io.barracks.membergateway.utils.BarracksQueryUtils;
import io.barracks.membergateway.utils.DeviceUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpServerErrorException;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class DeviceManagerTest {

    private final Pageable pageable = new PageRequest(0, 10);
    private final BarracksQuery query = BarracksQueryUtils.getQuery();
    @Mock
    private DeviceServiceClient deviceServiceClient;
    @InjectMocks
    private DeviceManager deviceManager;

    @Test
    public void getDevices_whenClientThrowException_shouldThrowItToo() {
        // Given
        final String userId = "1234567890";
        final Exception exception = new DeviceServiceClientException(
                new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR)
        );
        when(deviceServiceClient.getDevices(userId, pageable, query)).thenThrow(exception);

        // When / Then
        assertThatExceptionOfType(exception.getClass())
                .isThrownBy(() -> deviceManager.getDevices(userId, pageable, query));
    }

    @Test
    public void getDevices_whenClientReturnDevices_shouldReturnThemToo() {
        // Given
        final String userId = "1234567890";
        final List<Device> devices = Lists.newArrayList(
                DeviceUtils.getDevice(),
                DeviceUtils.getDevice()
        );
        when(deviceServiceClient.getDevices(userId, pageable, query))
                .thenReturn(PagedResourcesUtils.buildPagedResources(pageable, devices));

        // When
        final Page<Device> result = deviceManager.getDevices(userId, pageable, query);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.getContent()).containsAll(devices);
    }

    @Test
    public void getDevice_shouldCallClientAndReturnUnit() {
        // Given
        final String userId = UUID.randomUUID().toString();
        final String unitId = UUID.randomUUID().toString();
        final Device device = DeviceUtils.getDevice();
        when(deviceServiceClient.getDeviceByUserIdAndUnitId(userId, unitId)).thenReturn(device);

        // When
        final Device result = deviceManager.getDeviceByUserIdAndUnitId(userId, unitId);

        // Then
        assertThat(result).isEqualTo(device);
    }
}