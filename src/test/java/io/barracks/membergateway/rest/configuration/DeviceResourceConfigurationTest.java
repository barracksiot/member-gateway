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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.barracks.commons.test.PagedResourcesUtils;
import io.barracks.commons.util.Endpoint;
import io.barracks.membergateway.model.Device;
import io.barracks.membergateway.rest.BarracksResourceTest;
import io.barracks.membergateway.rest.DeviceResource;
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

import static io.barracks.membergateway.utils.DeviceUtils.getDevice;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@BarracksResourceTest(controllers = DeviceResource.class, outputDir = "build/generated-snippets/devices")
public class DeviceResourceConfigurationTest {

    private static final String BASE_URL = "https://not.barracks.io";
    private static final Endpoint GET_DEVICES_ENDPOINT = Endpoint.from(HttpMethod.GET, "/devices");
    private static final Endpoint GET_DEVICE_ENDPOINT = Endpoint.from(HttpMethod.GET, "/devices/{unitId}");

    @MockBean
    private DeviceResource deviceResource;
    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper objectMapper;

    private RandomPrincipal principal;

    @Before
    public void setUp() throws Exception {
        reset(deviceResource);
        this.principal = new RandomPrincipal();
    }

    @Test
    public void documentGetDevices() throws Exception {
        // Given
        final Endpoint endpoint = GET_DEVICES_ENDPOINT;
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        final Device device1 = getDevice();
        final Device device2 = getDevice();
        final Page<Device> page = new PageImpl<>(Arrays.asList(device1, device2));
        final PagedResources<Resource<Device>> devices = PagedResourcesUtils.<Device>getPagedResourcesAssembler(BASE_URL).toResource(page);
        final JsonNode query = objectMapper.readTree("{ \"eq\" : { \"customClientData.isBeta\" : true } }");
        doReturn(devices).when(deviceResource).getDevices(eq(query.toString()), any(Pageable.class), eq(principal));

        // When
        final ResultActions result = mvc.perform(
                RestDocumentationRequestBuilders.request(endpoint.getMethod(), endpoint.withBase(BASE_URL).getURI())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(query.toString())
                        .principal(principal)
        );

        // Then
        result.andExpect(status().isOk())
                .andDo(document(
                        "list",
                        requestParameters(
                                parameterWithName("query").description("A query to filter devices").optional()
                        ))
                );
    }

    @Test
    public void getDevices_shouldCallResource_andReturnResult() throws Exception {
        // Given
        final Endpoint endpoint = GET_DEVICES_ENDPOINT;
        final Pageable pageable = new PageRequest(0, 10);
        final Device device1 = getDevice();
        final Device device2 = getDevice();
        final Page<Device> page = new PageImpl<>(Arrays.asList(device1, device2));
        final PagedResources<Resource<Device>> devices = PagedResourcesUtils.<Device>getPagedResourcesAssembler(BASE_URL).toResource(page);
        final JsonNode query = objectMapper.readTree("{ \"eq\" : { \"customClientData.isBeta\" : true } }");
        doReturn(devices).when(deviceResource).getDevices(query.toString(), pageable, principal);

        // When
        final ResultActions result = mvc.perform(
                request(endpoint.getMethod(), endpoint.withBase(BASE_URL).pageable(pageable).getURI())
                        .accept(MediaType.APPLICATION_JSON_UTF8)
                        .param("query", query.toString())
                        .principal(principal)
        );

        // Then
        verify(deviceResource).getDevices(query.toString(), pageable, principal);
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.devices", hasSize(page.getNumberOfElements())))
                .andExpect(jsonPath("$._embedded.devices[0].unitId").value(device1.getUnitId()))
                .andExpect(jsonPath("$._embedded.devices[1].unitId").value(device2.getUnitId()));
    }

    @Test
    public void documentGetDevice() throws Exception {
        final Endpoint endpoint = GET_DEVICE_ENDPOINT;
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        final String unitId = "unit-1";
        final Device device = getDevice().toBuilder().unitId(unitId).build();
        doReturn(device).when(deviceResource).getDevice(principal, unitId);

        // When
        final ResultActions result = mvc.perform(
                RestDocumentationRequestBuilders.request(endpoint.getMethod(), endpoint.getPath(), unitId)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .principal(principal)
        );

        // Then
        verify(deviceResource).getDevice(principal, unitId);
        result.andExpect(status().isOk())
                .andDo(
                        document(
                                "get",
                                pathParameters(
                                        parameterWithName("unitId").description("The unique unitId of the device to return")
                                ),
                                responseFields(
                                        fieldWithPath("unitId").description("The unitId of the device."),
                                        fieldWithPath("firstSeen").description("The first time the device contacted Barracks"),
                                        fieldWithPath("lastEvent").description("The last event received from the device")
                                )
                        )
                );
    }

    @Test
    public void getDevice_shouldCallResource_andReturnResult() throws Exception {
        // Given
        final Endpoint endpoint = GET_DEVICE_ENDPOINT;
        final String unitId = "unit-1";
        final Device expected = getDevice();

        doReturn(expected).when(deviceResource).getDevice(principal, unitId);

        // When
        final ResultActions result = mvc.perform(
                request(endpoint.getMethod(), endpoint.withBase(BASE_URL).getURI(unitId))
                        .accept(MediaType.APPLICATION_JSON_UTF8)
                        .principal(principal)
        );

        // Then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.unitId").value(expected.getUnitId()));
    }
}
