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

import io.barracks.membergateway.client.exception.DeviceServiceClientException;
import io.barracks.membergateway.client.util.PageableHelper;
import io.barracks.membergateway.manager.DeviceEventManager;
import io.barracks.membergateway.model.DeviceEvent;
import io.barracks.membergateway.utils.DeviceEventUtils;
import io.barracks.membergateway.utils.RandomPrincipal;
import org.hamcrest.collection.IsMapContaining;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.client.HttpServerErrorException;

import java.net.URI;
import java.security.Principal;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@RunWith(SpringRunner.class)
@BarracksResourceTest(controllers = DeviceEventResource.class)
public class DeviceEventResourceTest {
    @Autowired
    private MockMvc mvc;

    @MockBean
    private DeviceEventManager deviceEventManager;

    @Autowired
    private DeviceEventResource deviceEventResource;

    private Principal principal;

    @Before
    public void setUp() throws Exception {
        this.principal = new RandomPrincipal();
    }

    @Test
    public void getDeviceEvents_whenRequestIsBad_shouldReturnBadRequest() throws Exception {
        // Given
        final String unitId = "unitId";
        final Pageable pageable = new PageRequest(0, 20);
        when(deviceEventManager.getPaginatedDeviceEvents(pageable, principal.getName(), unitId)).thenThrow(new DeviceServiceClientException(new HttpServerErrorException(HttpStatus.BAD_REQUEST)));

        // When
        final ResultActions result = mvc.perform(
                MockMvcRequestBuilders.get(URI.create("/devices/" + unitId + "/events"))
                        .accept(MediaTypes.HAL_JSON)
                        .contentType(MediaTypes.HAL_JSON)
                        .principal(principal)
        );

        // Then
        verify(deviceEventManager).getPaginatedDeviceEvents(pageable, principal.getName(), unitId);
        result.andExpect(status().isBadRequest());
    }

    @Test
    public void getDeviceHistory_whenRequestIsPost_shouldReturn405Code() throws Exception {
        // When
        final ResultActions result = mvc.perform(
                MockMvcRequestBuilders.post("/devices/aUnitId/events").principal(principal)
        );

        // Then
        result.andExpect(status().isMethodNotAllowed());
    }

    @Test
    public void getDeviceEvents_whenRequestIsValid_shouldReturn200CodeAndAListOfDeviceEvents() throws Exception {
        // Given
        final Pageable pageable = new PageRequest(0, 20);
        final String unitId = "unitId1";
        final DeviceEvent deviceEvent1 = DeviceEventUtils.buildDeviceEvent(unitId);
        final DeviceEvent deviceEvent2 = DeviceEventUtils.buildDeviceEvent(unitId);
        final List<DeviceEvent> deviceEvents = Arrays.asList(
                deviceEvent1, deviceEvent2
        );
        final Page<DeviceEvent> managerResponse = new PageImpl<>(deviceEvents, pageable, deviceEvents.size());
        when(deviceEventManager.getPaginatedDeviceEvents(pageable, principal.getName(), unitId)).thenReturn(managerResponse);

        // When
        final ResultActions result = mvc.perform(
                MockMvcRequestBuilders.get("/devices/" + unitId + "/events")
                        .accept(MediaType.APPLICATION_JSON_UTF8)
                        .principal(principal)
        );

        // Then
        verify(deviceEventManager).getPaginatedDeviceEvents(pageable, principal.getName(), unitId);
        result.andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("$._embedded.events", hasSize(deviceEvents.size())))
                .andExpect(jsonPath("$._embedded.events[0]").value(IsMapContaining.hasEntry("unitId", deviceEvent1.getUnitId())))
                .andExpect(jsonPath("$._embedded.events[0]").value(IsMapContaining.hasEntry("versionId", deviceEvent1.getVersionId())))
                .andExpect(jsonPath("$._embedded.events[0]").value(IsMapContaining.hasKey("receptionDate")))
                .andExpect(jsonPath("$._embedded.events[0]").value(IsMapContaining.hasEntry("deviceIP", deviceEvent1.getDeviceIP())))
                .andExpect(jsonPath("$._embedded.events[1]").value(IsMapContaining.hasEntry("unitId", deviceEvent2.getUnitId())))
                .andExpect(jsonPath("$._embedded.events[1]").value(IsMapContaining.hasEntry("versionId", deviceEvent2.getVersionId())))
                .andExpect(jsonPath("$._embedded.events[1]").value(IsMapContaining.hasKey("receptionDate")))
                .andExpect(jsonPath("$._embedded.events[1]").value(IsMapContaining.hasEntry("deviceIP", deviceEvent2.getDeviceIP())));
    }

    @Test
    public void getDeviceHistory_whenRequestWithParametersIsValid_shouldReturn200CodeAndAListOfDeviceEvent() throws Exception {
        // Given
        final Pageable pageable = new PageRequest(1, 4);
        final String unitId = "unitId1";
        final DeviceEvent deviceEvent1 = DeviceEventUtils.buildDeviceEvent(unitId);
        final DeviceEvent deviceEvent2 = DeviceEventUtils.buildDeviceEvent(unitId);
        final List<DeviceEvent> deviceEvents = Arrays.asList(
                deviceEvent1, deviceEvent2
        );
        final Page<DeviceEvent> managerResponse = new PageImpl<>(deviceEvents, pageable, deviceEvents.size());
        when(deviceEventManager.getPaginatedDeviceEvents(pageable, principal.getName(), unitId)).thenReturn(managerResponse);

        // When
        final ResultActions result = mvc.perform(
                MockMvcRequestBuilders.get("/devices/" + unitId + "/events?" + PageableHelper.toUriQuery(pageable))
                        .accept(MediaTypes.HAL_JSON)
                        .principal(principal)
        );

        // Then
        verify(deviceEventManager).getPaginatedDeviceEvents(pageable, principal.getName(), unitId);
        result.andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaTypes.HAL_JSON))
                .andExpect(jsonPath("$._embedded.events", hasSize(deviceEvents.size())))

                .andExpect(jsonPath("$._embedded.events[0]").value(IsMapContaining.hasEntry("unitId", deviceEvent1.getUnitId())))
                .andExpect(jsonPath("$._embedded.events[0]").value(IsMapContaining.hasEntry("versionId", deviceEvent1.getVersionId())))
                .andExpect(jsonPath("$._embedded.events[0]").value(IsMapContaining.hasKey("receptionDate")))
                .andExpect(jsonPath("$._embedded.events[0]").value(IsMapContaining.hasEntry("deviceIP", deviceEvent1.getDeviceIP())))

                .andExpect(jsonPath("$._embedded.events[1]").value(IsMapContaining.hasEntry("unitId", deviceEvent2.getUnitId())))
                .andExpect(jsonPath("$._embedded.events[1]").value(IsMapContaining.hasEntry("versionId", deviceEvent2.getVersionId())))
                .andExpect(jsonPath("$._embedded.events[1]").value(IsMapContaining.hasKey("receptionDate")))
                .andExpect(jsonPath("$._embedded.events[1]").value(IsMapContaining.hasEntry("deviceIP", deviceEvent2.getDeviceIP())));

    }
}
