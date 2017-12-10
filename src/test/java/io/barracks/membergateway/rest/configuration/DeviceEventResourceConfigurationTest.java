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

package io.barracks.membergateway.rest.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.barracks.commons.test.PagedResourcesUtils;
import io.barracks.commons.util.Endpoint;
import io.barracks.membergateway.model.DeviceEvent;
import io.barracks.membergateway.rest.BarracksResourceTest;
import io.barracks.membergateway.rest.DeviceEventResource;
import io.barracks.membergateway.utils.DeviceEventUtils;
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
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.Arrays;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@BarracksResourceTest(controllers = DeviceEventResource.class, outputDir = "build/generated-snippets/events")
public class DeviceEventResourceConfigurationTest {

    private static final String BASE_URL = "https://not.barracks.io";
    private static final Endpoint GET_DEVICE_EVENTS_ENDPOINT = Endpoint.from(HttpMethod.GET, "/devices/{unitId}/events");

    @MockBean
    private DeviceEventResource deviceEventResource;
    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper objectMapper;

    private RandomPrincipal principal;

    @Before
    public void setUp() throws Exception {
        reset(deviceEventResource);
        this.principal = new RandomPrincipal();
    }

    @Test
    public void documentGetDeviceEvents() throws Exception {
        // Given
        final Endpoint endpoint = GET_DEVICE_EVENTS_ENDPOINT;
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        final String unitId = "unit-1";
        final DeviceEvent deviceEvent1 = DeviceEventUtils.getDeviceEvent();
        final DeviceEvent deviceEvent2 = DeviceEventUtils.getDeviceEvent();
        final Page<DeviceEvent> page = new PageImpl<>(Arrays.asList(deviceEvent1, deviceEvent2));
        final PagedResources<Resource<DeviceEvent>> deviceEvents = PagedResourcesUtils.<DeviceEvent>getPagedResourcesAssembler(BASE_URL).toResource(page);
        doReturn(deviceEvents).when(deviceEventResource).getDeviceEvents(any(Pageable.class), eq(principal), eq(unitId));

        // When
        final ResultActions result = mvc.perform(
                RestDocumentationRequestBuilders.request(endpoint.getMethod(), endpoint.getPath(), unitId)
                        .accept(MediaType.APPLICATION_JSON)
                        .principal(principal)
        );

        // Then
        result.andExpect(status().isOk())
                .andDo(document(
                        "list",
                        pathParameters(
                                parameterWithName("unitId").description("The unitId of the device to return the events for")
                        )
                ));
    }


    @Test
    public void getDevices_shouldCallResource_andReturnResult() throws Exception {
        // Given
        final Endpoint endpoint = GET_DEVICE_EVENTS_ENDPOINT;
        final Pageable pageable = new PageRequest(0, 10);
        final String unitId = "unit-1";
        final DeviceEvent deviceEvent1 = DeviceEventUtils.getDeviceEvent();
        final DeviceEvent deviceEvent2 = DeviceEventUtils.getDeviceEvent();
        final Page<DeviceEvent> page = new PageImpl<>(Arrays.asList(deviceEvent1, deviceEvent2));
        final PagedResources<Resource<DeviceEvent>> deviceEvents = PagedResourcesUtils.<DeviceEvent>getPagedResourcesAssembler(BASE_URL).toResource(page);
        doReturn(deviceEvents).when(deviceEventResource).getDeviceEvents(any(Pageable.class), eq(principal), eq(unitId));

        // When
        final ResultActions result = mvc.perform(
                request(endpoint.getMethod(), endpoint.withBase(BASE_URL).pageable(pageable).getURI(unitId))
                        .accept(MediaType.APPLICATION_JSON)
                        .principal(principal)
        );

        // Then
        verify(deviceEventResource).getDeviceEvents(pageable, principal, unitId);
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.events", hasSize(page.getNumberOfElements())));
    }
}
