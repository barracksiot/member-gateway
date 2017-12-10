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

import io.barracks.commons.test.PagedResourcesUtils;
import io.barracks.membergateway.manager.DeviceEventManager;
import io.barracks.membergateway.model.DeviceEvent;
import io.barracks.membergateway.utils.RandomPrincipal;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.PagedResources;

import java.security.Principal;
import java.util.Arrays;
import java.util.UUID;

import static io.barracks.membergateway.utils.DeviceEventUtils.getDeviceEvent;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class DeviceEventResourceTest {

    @Mock
    private DeviceEventManager manager;

    private PagedResourcesAssembler<DeviceEvent> assembler = PagedResourcesUtils.getPagedResourcesAssembler();

    private DeviceEventResource deviceEventResource;

    private Principal principal = new RandomPrincipal();

    @Before
    public void setUp() {
        deviceEventResource = new DeviceEventResource(manager, assembler);
    }

    @Test
    public void getDeviceEvents_shouldCallManager_andReturnResult() {
        // Given
        final String userId = principal.getName();
        final String unitId = UUID.randomUUID().toString();
        final Pageable pageable = new PageRequest(0, 10);
        final Page<DeviceEvent> expected = new PageImpl<>(
                Arrays.asList(
                        getDeviceEvent(),
                        getDeviceEvent()
                )
        );
        doReturn(expected).when(manager).getPaginatedDeviceEvents(pageable, userId, unitId);

        // When
        final PagedResources result = deviceEventResource.getDeviceEvents(pageable, principal, unitId);

        // Then
        verify(manager).getPaginatedDeviceEvents(pageable, userId, unitId);
        assertThat(result).isEqualTo(assembler.toResource(expected));
    }
}
