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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import io.barracks.membergateway.client.util.PageableHelper;
import io.barracks.membergateway.manager.DeviceManager;
import io.barracks.membergateway.model.BarracksQuery;
import io.barracks.membergateway.model.Device;
import io.barracks.membergateway.utils.BarracksQueryUtils;
import io.barracks.membergateway.utils.DeviceUtils;
import io.barracks.membergateway.utils.RandomPrincipal;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.security.Principal;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@RunWith(SpringRunner.class)
@BarracksResourceTest(controllers = DeviceResource.class)
public class DeviceResourceTest {
    @Autowired
    private MockMvc mvc;

    @MockBean
    private DeviceManager deviceManager;

    @Autowired
    private ObjectMapper mapper;

    private Principal principal;

    private Pageable pageable = new PageRequest(0, 10);

    @Before
    public void setUp() throws Exception {
        this.principal = new RandomPrincipal();
    }

    @Test
    public void getDevice_shouldCallManagerAndReturnDevice() throws Exception {
        // Given
        final String unitId = UUID.randomUUID().toString();
        final Device device = DeviceUtils.buildDevice(unitId);
        final String expected = mapper.writeValueAsString(device);
        doReturn(device).when(deviceManager).getDeviceByUserIdAndUnitId(principal.getName(), unitId);

        // When
        final ResultActions result = mvc.perform(
                MockMvcRequestBuilders.get("/devices/{unitId}", unitId)
                        .accept(MediaType.APPLICATION_JSON_UTF8)
                        .principal(principal)
        );

        // Then
        verify(deviceManager).getDeviceByUserIdAndUnitId(principal.getName(), unitId);
        result.andExpect(status().isOk()).andExpect(content().json(expected));
    }

    @Test
    public void getDevices_shouldCallManagerAndReturnDevices_whenQueryIsNotPassed() throws Exception {
        // Given
        final Device unit1 = DeviceUtils.buildDevice("unit1");
        final Device unit2 = DeviceUtils.buildDevice("unit2");
        final Page<Device> page = new PageImpl<>(Lists.newArrayList(unit1, unit2));
        final BarracksQuery query = new BarracksQuery(null);
        when(deviceManager.getDevices(principal.getName(), pageable, query)).thenReturn(page);

        // When
        final ResultActions result = mvc.perform(
                MockMvcRequestBuilders.get("/devices?" + PageableHelper.toUriQuery(pageable))
                        .accept(MediaType.APPLICATION_JSON_UTF8)
                        .principal(principal)
        );

        // Then
        verify(deviceManager).getDevices(principal.getName(), pageable, query);
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.devices", hasSize(page.getNumberOfElements())))
                .andExpect(jsonPath("$._embedded.devices[0].unitId").value(unit1.getUnitId()))
                .andExpect(jsonPath("$._embedded.devices[1].unitId").value(unit2.getUnitId()));
    }

    @Test
    public void getDevices_shouldCallManagerAndReturnDevices_whenQueryIsPassed() throws Exception {
        // Given
        final Device unit1 = DeviceUtils.buildDevice("unit1");
        final Device unit2 = DeviceUtils.buildDevice("unit2");
        final Page<Device> page = new PageImpl<>(Lists.newArrayList(unit1, unit2));
        final BarracksQuery query = BarracksQueryUtils.getQuery();
        when(deviceManager.getDevices(principal.getName(), pageable, query)).thenReturn(page);

        // When
        final ResultActions result = mvc.perform(
                MockMvcRequestBuilders.get("/devices?query={query}&" + PageableHelper.toUriQuery(pageable), query.toJsonString())
                        .accept(MediaType.APPLICATION_JSON_UTF8)
                        .principal(principal)
        );

        // Then
        verify(deviceManager).getDevices(principal.getName(), pageable, query);
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.devices", hasSize(page.getNumberOfElements())))
                .andExpect(jsonPath("$._embedded.devices[0].unitId").value(unit1.getUnitId()))
                .andExpect(jsonPath("$._embedded.devices[1].unitId").value(unit2.getUnitId()));
    }

    @Test
    public void getDevices_shouldReturnBadRequest_whenQueryIsMalformed() throws Exception {
        // Given
        final String query = "{ coucou { ca } va? }";

        // When
        final ResultActions result = mvc.perform(
                MockMvcRequestBuilders.get("/devices?query={query}&" + PageableHelper.toUriQuery(pageable), query)
                        .accept(MediaType.APPLICATION_JSON_UTF8)
                        .principal(principal)
        );

        // Then
        verifyZeroInteractions(deviceManager);
        result.andExpect(status().isBadRequest());
    }

}