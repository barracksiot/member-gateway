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

import io.barracks.membergateway.client.DeviceServiceClient;
import io.barracks.membergateway.client.exception.DeviceServiceClientException;
import io.barracks.membergateway.model.DeviceEvent;
import io.barracks.membergateway.utils.DeviceEventUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.PagedResources;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class DeviceEventManagerTest {

    final Pageable pageable = new PageRequest(0, 20);
    @Mock
    private DeviceServiceClient deviceServiceClient;
    @InjectMocks
    private DeviceEventManager deviceEventManager;

    @Test
    public void getDeviceEvents_shouldReturnDeviceEventsReturnedByDeviceServiceClient() {
        // Given
        final String unitId = "unit1";
        final String userId = "user1";
        final List<DeviceEvent> deviceEvents = Arrays.asList(
                DeviceEventUtils.getDeviceEvent(),
                DeviceEventUtils.getDeviceEvent()
        );
        when(deviceServiceClient.getDeviceEvents(pageable, userId, unitId)).thenReturn(
                buildPagedResources(deviceEvents, pageable)
        );

        // When
        final Page<DeviceEvent> page = deviceEventManager.getPaginatedDeviceEvents(pageable, userId, unitId);

        // Then
        verify(deviceServiceClient).getDeviceEvents(pageable, userId, unitId);
        assertThat(page).isNotNull();
        assertThat(page.getContent()).isEqualTo(deviceEvents);
        assertThat(page.getTotalElements()).isEqualTo(deviceEvents.size());
    }

    @Test
    public void getDeviceHistory_whenDeviceServiceClientThrowsAnException_shouldThrowAnExceptionToo() {
        // Given
        final DeviceServiceClientException clientException = new DeviceServiceClientException(
                new HttpClientErrorException(HttpStatus.INTERNAL_SERVER_ERROR)
        );
        final String unitId = "unit1";
        final String userId = "user1";
        when(deviceServiceClient.getDeviceEvents(pageable, userId, unitId)).thenThrow(clientException);

        // Then When
        assertThatExceptionOfType(DeviceServiceClientException.class)
                .isThrownBy(() -> {
                    deviceEventManager.getPaginatedDeviceEvents(pageable, userId, unitId);
                });
        verify(deviceServiceClient).getDeviceEvents(pageable, userId, unitId);
    }

    private PagedResources<DeviceEvent> buildPagedResources(List<DeviceEvent> deviceEvents, Pageable pageable) {
        return new PagedResources<>(
                deviceEvents,
                new PagedResources.PageMetadata(
                        pageable.getPageSize(),
                        pageable.getPageNumber(),
                        deviceEvents.size()
                )
        );
    }
}
