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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.barracks.commons.test.PagedResourcesUtils;
import io.barracks.membergateway.manager.DeviceManager;
import io.barracks.membergateway.model.BarracksQuery;
import io.barracks.membergateway.model.Device;
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

import java.io.IOException;
import java.security.Principal;
import java.util.Arrays;
import java.util.UUID;

import static io.barracks.membergateway.utils.DeviceUtils.getDevice;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class DeviceResourceTest {

    @Mock
    private DeviceManager manager;

    @Mock
    private ObjectMapper objectMapper;

    private PagedResourcesAssembler<Device> assembler = PagedResourcesUtils.getPagedResourcesAssembler();

    private DeviceResource deviceResource;

    private Principal principal = new RandomPrincipal();

    @Before
    public void setUp() {
        deviceResource = new DeviceResource(objectMapper, manager, assembler);
    }

    @Test
    public void getDevicesWithQuery_shouldCallManager_andReturnResult() throws IOException {
        // Given
        final String userId = principal.getName();
        final Pageable pageable = new PageRequest(0, 10);
        final JsonNode jsonQuery = new ObjectNode(JsonNodeFactory.instance);
        final BarracksQuery query = new BarracksQuery(jsonQuery);
        final Page<Device> expected = new PageImpl<>(
                Arrays.asList(
                        getDevice(),
                        getDevice()
                )
        );
        doReturn(jsonQuery).when(objectMapper).readTree(query.toJsonString());
        doReturn(expected).when(manager).getDevices(userId, pageable, query);

        // When
        final PagedResources result = deviceResource.getDevices(query.toJsonString(), pageable, principal);

        // Then
        verify(manager).getDevices(userId, pageable, query);
        verify(objectMapper).readTree(query.toJsonString());
        assertThat(result).isEqualTo(assembler.toResource(expected));
    }

    @Test
    public void getDevicesWithNoQuery_shouldCallManager_andReturnResult() {
        // Given
        final String userId = principal.getName();
        final BarracksQuery emptyQuery = new BarracksQuery(null);
        final Pageable pageable = new PageRequest(0, 10);
        final Page<Device> expected = new PageImpl<>(
                Arrays.asList(
                        getDevice(),
                        getDevice()
                )
        );
        doReturn(expected).when(manager).getDevices(userId, pageable, emptyQuery);

        // When
        final PagedResources result = deviceResource.getDevices("", pageable, principal);

        // Then
        verify(manager).getDevices(userId, pageable, emptyQuery);
        assertThat(result).isEqualTo(assembler.toResource(expected));
    }

    @Test
    public void getDevice_shouldCallManager_andReturnResult() {
        // Given
        final String userId = principal.getName();
        final String unitId = UUID.randomUUID().toString();
        final Device expected = getDevice();
        doReturn(expected).when(manager).getDeviceByUserIdAndUnitId(userId, unitId);

        // When
        final Device result = deviceResource.getDevice(principal, unitId);

        // Then
        verify(manager).getDeviceByUserIdAndUnitId(userId, unitId);
        assertThat(result).isEqualTo(expected);
    }
}