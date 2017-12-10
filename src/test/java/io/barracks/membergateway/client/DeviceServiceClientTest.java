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

package io.barracks.membergateway.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import io.barracks.commons.util.Endpoint;
import io.barracks.membergateway.client.exception.DeviceServiceClientException;
import io.barracks.membergateway.model.BarracksQuery;
import io.barracks.membergateway.model.Device;
import io.barracks.membergateway.model.DeviceEvent;
import io.barracks.membergateway.model.Filter;
import io.barracks.membergateway.utils.BarracksQueryUtils;
import io.barracks.membergateway.utils.FilterUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.PagedResources;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.client.MockRestServiceServer;

import java.io.UnsupportedEncodingException;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

@RunWith(SpringRunner.class)
@RestClientTest(DeviceServiceClient.class)
public class DeviceServiceClientTest {
    @Autowired
    private ObjectMapper mapper;
    @Value("${io.barracks.deviceservice.base_url}")
    private String baseUrl;
    @Autowired
    private MockRestServiceServer mockServer;
    @Autowired
    private DeviceServiceClient deviceServiceClient;

    @Value("classpath:io/barracks/membergateway/client/deviceEvents-empty.json")
    private Resource deviceEventsEmpty;
    @Value("classpath:io/barracks/membergateway/client/deviceEvents.json")
    private Resource deviceEvents;
    @Value("classpath:io/barracks/membergateway/device.json")
    private Resource device;
    @Value("classpath:io/barracks/membergateway/client/devices.json")
    private Resource devices;
    @Value("classpath:io/barracks/membergateway/client/filter.json")
    private Resource filter;
    @Value("classpath:io/barracks/membergateway/client/getFilter.json")
    private Resource getFilter;
    @Value("classpath:io/barracks/membergateway/client/filters.json")
    private Resource filters;
    @Value("classpath:io/barracks/membergateway/client/filters-empty.json")
    private Resource filtersEmpty;
    @Value("classpath:io/barracks/membergateway/client/updated-filter.json")
    private Resource updatedFilter;

    @Test
    public void getDeviceEvents_whenServiceFails_shouldThrowClientException() throws UnsupportedEncodingException {
        // Given
        final Endpoint endpoint = DeviceServiceClient.GET_DEVICE_EVENTS_ENDPOINT;
        final String userId = UUID.randomUUID().toString();
        final String unitId = UUID.randomUUID().toString();
        final Pageable pageable = new PageRequest(0, 10);
        mockServer.expect(method(endpoint.getMethod()))
                .andExpect(requestTo(endpoint.withBase(baseUrl).pageable(pageable).getURI(userId, unitId)))
                .andRespond(withBadRequest());

        // When
        assertThatExceptionOfType(DeviceServiceClientException.class)
                .isThrownBy(() -> {
                    deviceServiceClient.getDeviceEvents(pageable, userId, unitId);
                });
        mockServer.verify();
    }

    @Test
    public void getDeviceEvents_whenEmptyListReturnedByService_shouldReturnAnEmptyList() throws Exception {
        // Given
        final Endpoint endpoint = DeviceServiceClient.GET_DEVICE_EVENTS_ENDPOINT;
        final String userId = UUID.randomUUID().toString();
        final String unitId = UUID.randomUUID().toString();
        final Pageable pageable = new PageRequest(4, 15);
        mockServer.expect(method(endpoint.getMethod()))
                .andExpect(requestTo(endpoint.withBase(baseUrl).pageable(pageable).getURI(userId, unitId)))
                .andRespond(
                        withStatus(HttpStatus.OK)
                                .body(deviceEventsEmpty)
                                .contentType(MediaTypes.HAL_JSON)
                );

        // When
        final PagedResources<DeviceEvent> result = deviceServiceClient.getDeviceEvents(pageable, userId, unitId);

        // Then
        mockServer.verify();
        assertThat(result).isNotNull();
        assertThat(result.getContent())
                .isNotNull()
                .isEmpty();
    }

    @Test
    public void getDeviceEvents_whenNotEmptyListReturnedByService_shouldReturnAListOfDeviceEvents() throws Exception {
        // Given
        final Endpoint endpoint = DeviceServiceClient.GET_DEVICE_EVENTS_ENDPOINT;
        final String userId = UUID.randomUUID().toString();
        final String unitId = UUID.randomUUID().toString();
        final JsonNode jsonObject = mapper.readTree(deviceEvents.getInputStream());
        final ArrayNode parsedDeviceEvents = ((ArrayNode) jsonObject.get("_embedded").get("events"));
        final Pageable pageable = new PageRequest(0, 10);
        mockServer.expect(method(endpoint.getMethod()))
                .andExpect(requestTo(endpoint.withBase(baseUrl).pageable(pageable).getURI(userId, unitId)))
                .andRespond(withSuccess().body(deviceEvents));

        // When
        final PagedResources<DeviceEvent> result = deviceServiceClient.getDeviceEvents(pageable, userId, unitId);

        // Then
        mockServer.verify();
        assertThat(result).isNotNull();
        assertThat(result.getContent())
                .isNotNull()
                .hasSize(parsedDeviceEvents.size());
    }

    @Test
    public void getDeviceByUserAndUnitId_whenSucceeds_shouldReturnDevice() throws Exception {
        // Given
        final Endpoint endpoint = DeviceServiceClient.GET_DEVICE_ENDPOINT;
        final String userId = UUID.randomUUID().toString();
        final String unitId = UUID.randomUUID().toString();
        final Device expected = mapper.readValue(device.getInputStream(), Device.class);
        mockServer.expect(method(endpoint.getMethod()))
                .andExpect(requestTo(endpoint.withBase(baseUrl).getURI(userId, unitId)))
                .andRespond(withSuccess().body(device));

        // When
        Device result = deviceServiceClient.getDeviceByUserIdAndUnitId(userId, unitId);

        // Then
        mockServer.verify();
        assertThat(result).isEqualTo(expected);
    }

    @Test
    public void getDeviceByUserIdAndUnitId_whenFails_shouldThrowException() throws UnsupportedEncodingException {
        // Given
        final Endpoint endpoint = DeviceServiceClient.GET_DEVICE_ENDPOINT;
        final String userId = UUID.randomUUID().toString();
        final String unitId = UUID.randomUUID().toString();

        // When
        mockServer.expect(method(endpoint.getMethod()))
                .andExpect(requestTo(endpoint.withBase(baseUrl).getURI(userId, unitId)))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        // Then
        assertThatExceptionOfType(DeviceServiceClientException.class)
                .isThrownBy(() -> deviceServiceClient.getDeviceByUserIdAndUnitId(userId, unitId));
        mockServer.verify();
    }

    @Test
    public void getDevicesWithQuery_whenRequestSucceed_shouldReturnDevices() throws Exception {
        // Given
        final Endpoint endpoint = DeviceServiceClient.GET_DEVICES_ENDPOINT;
        final String userId = UUID.randomUUID().toString();
        final Pageable pageable = new PageRequest(0, 10);
        final BarracksQuery query = BarracksQueryUtils.getQuery();
        final JsonNode jsonObject = mapper.readTree(devices.getInputStream());
        final ArrayNode parsedDevices = ((ArrayNode) jsonObject.get("_embedded").get("devices"));
        mockServer.expect(method(endpoint.getMethod()))
                .andExpect(requestTo(endpoint.withBase(baseUrl).pageable(pageable).getURI(userId, query.toJsonString())))
                .andRespond(withSuccess().body(devices));

        // When
        final PagedResources<Device> result = deviceServiceClient.getDevices(userId, pageable, query);

        // Then
        mockServer.verify();
        assertThat(result.getContent())
                .isNotNull()
                .hasSize(parsedDevices.size());
    }

    @Test
    public void getDevicesWithQuery_whenRequestFailed_shouldThrowException() throws Exception {
        // Given
        final Endpoint endpoint = DeviceServiceClient.GET_DEVICES_ENDPOINT;
        final String userId = UUID.randomUUID().toString();
        final Pageable pageable = new PageRequest(0, 10);
        final BarracksQuery query = BarracksQueryUtils.getQuery();
        mockServer.expect(method(endpoint.getMethod()))
                .andExpect(requestTo(endpoint.withBase(baseUrl).pageable(pageable).getURI(userId, query.toJsonString())))
                .andRespond(withServerError());

        // When / Then
        assertThatExceptionOfType(DeviceServiceClientException.class)
                .isThrownBy(() -> deviceServiceClient.getDevices(userId, pageable, query));
        mockServer.verify();
    }

    @Test
    public void createFilter_whenSucceeds_shouldReturnFilter() throws Exception {
        // Given
        final Endpoint endpoint = DeviceServiceClient.CREATE_FILTER_ENDPOINT;
        final String userId = UUID.randomUUID().toString();
        final Filter filter = FilterUtils.getFilter();
        final Filter expected = mapper.readValue(this.filter.getInputStream(), Filter.class);
        mockServer.expect(method(endpoint.getMethod()))
                .andExpect(requestTo(endpoint.withBase(baseUrl).getURI(userId)))
                .andExpect(content().string(mapper.writeValueAsString(filter)))
                .andRespond(withStatus(HttpStatus.CREATED).body(this.filter));

        // When
        final Filter result = deviceServiceClient.createFilter(userId, filter);

        // Then
        mockServer.verify();
        assertThat(result).isEqualTo(expected);
    }

    @Test
    public void createFilter_whenFails_shouldThrowException() throws Exception {
        // Given
        final String userId = UUID.randomUUID().toString();
        final Filter filter = FilterUtils.getFilter();
        final Endpoint endpoint = DeviceServiceClient.CREATE_FILTER_ENDPOINT;
        mockServer.expect(method(endpoint.getMethod()))
                .andExpect(requestTo(endpoint.withBase(baseUrl).getURI(userId)))
                .andRespond(withBadRequest());

        // Then / When
        assertThatExceptionOfType(DeviceServiceClientException.class)
                .isThrownBy(() -> deviceServiceClient.createFilter(userId, filter));
        mockServer.verify();
    }

    @Test
    public void updateFilter_whenSucceeds_shouldReturnFilter() throws Exception {
        // Given
        final Endpoint endpoint = DeviceServiceClient.UPDATE_FILTER_ENDPOINT;
        final String userId = UUID.randomUUID().toString();
        final JsonNode query = JsonNodeFactory.instance.objectNode().set("eq", JsonNodeFactory.instance.objectNode().put("key", "value"));
        Filter expected = mapper.readValue(this.filter.getInputStream(), Filter.class);
        expected = expected.toBuilder().query(query).build();

        mockServer.expect(method(endpoint.getMethod()))
                .andExpect(requestTo(endpoint.withBase(baseUrl).getURI(userId, expected.getName())))
                .andExpect(content().string(mapper.writeValueAsString(query)))
                .andRespond(withSuccess().body(this.updatedFilter));

        // When
        final Filter result = deviceServiceClient.updateFilter(userId, expected.getName(), query);

        // Then
        mockServer.verify();
        assertThat(result).isEqualTo(expected);
    }

    @Test
    public void updateFilter_whenFails_shouldThrowException() throws Exception {
        // Given
        final String userId = UUID.randomUUID().toString();
        final Filter filter = FilterUtils.getFilter();
        final JsonNode query = JsonNodeFactory.instance.objectNode().put("key", "value");
        final Endpoint endpoint = DeviceServiceClient.UPDATE_FILTER_ENDPOINT;
        mockServer.expect(method(endpoint.getMethod()))
                .andExpect(requestTo(endpoint.withBase(baseUrl).getURI(userId, filter.getName())))
                .andRespond(withBadRequest());

        // Then / When
        assertThatExceptionOfType(DeviceServiceClientException.class)
                .isThrownBy(() -> deviceServiceClient.updateFilter(userId, filter.getName(), query));
        mockServer.verify();
    }

    @Test
    public void getFilters_whenSucceeded_shouldReturnFilters() throws Exception {
        // Given
        final Endpoint endpoint = DeviceServiceClient.GET_FILTERS_ENDPOINT;
        final String userId = UUID.randomUUID().toString();
        final Pageable pageable = new PageRequest(0, 10);
        mockServer.expect(method(endpoint.getMethod()))
                .andExpect(requestTo(endpoint.withBase(baseUrl).pageable(pageable).getURI(userId)))
                .andRespond(withSuccess().body(filters));

        // When
        final PagedResources<Filter> result = deviceServiceClient.getFilters(userId, pageable);

        // Then
        mockServer.verify();
        assertThat(result).isNotEmpty();
    }

    @Test
    public void getFilters_whenSucceededAndNoFilter_shouldReturnEmptyList() throws Exception {
        // Given
        final Endpoint endpoint = DeviceServiceClient.GET_FILTERS_ENDPOINT;
        final String userId = UUID.randomUUID().toString();
        final Pageable pageable = new PageRequest(0, 10);
        mockServer.expect(method(endpoint.getMethod()))
                .andExpect(requestTo(endpoint.withBase(baseUrl).pageable(pageable).getURI(userId)))
                .andRespond(withSuccess().body(filtersEmpty));

        // When
        final PagedResources<Filter> result = deviceServiceClient.getFilters(userId, pageable);

        // Then
        mockServer.verify();
        assertThat(result).isNotNull().isEmpty();
    }

    @Test
    public void getFilters_whenFails_shouldThrowException() throws Exception {
        // Given
        final Endpoint endpoint = DeviceServiceClient.GET_FILTERS_ENDPOINT;
        final String userId = UUID.randomUUID().toString();
        final Pageable pageable = new PageRequest(0, 10);

        mockServer.expect(method(endpoint.getMethod()))
                .andExpect(requestTo(endpoint.withBase(baseUrl).pageable(pageable).getURI(userId)))
                .andRespond(withBadRequest());

        // ThenWhen
        assertThatExceptionOfType(DeviceServiceClientException.class)
                .isThrownBy(() -> deviceServiceClient.getFilters(userId, pageable));
        mockServer.verify();
    }

    @Test
    public void getFilter_whenSucceeded_shouldReturnFilter() throws Exception {
        // Given
        final Endpoint endpoint = DeviceServiceClient.GET_FILTER_ENDPOINT;
        final String userId = UUID.randomUUID().toString();
        final String filterName = UUID.randomUUID().toString();
        final Filter expected = mapper.readValue(this.getFilter.getInputStream(), Filter.class);
        mockServer.expect(method(endpoint.getMethod()))
                .andExpect(requestTo(endpoint.withBase(baseUrl).getURI(userId, filterName)))
                .andRespond(withSuccess().body(getFilter));

        // When
        final Filter result = deviceServiceClient.getFilterByUserIdAndName(userId, filterName);

        // Then
        mockServer.verify();
        assertThat(result).isEqualTo(expected);
    }

    @Test
    public void getFilter_whenFails_shouldThrowException() throws Exception {
        // Given
        final Endpoint endpoint = DeviceServiceClient.GET_FILTER_ENDPOINT;
        final String userId = UUID.randomUUID().toString();
        final String filterName = UUID.randomUUID().toString();

        mockServer.expect(method(endpoint.getMethod()))
                .andExpect(requestTo(endpoint.withBase(baseUrl).getURI(userId, filterName)))
                .andRespond(withBadRequest());

        // ThenWhen
        assertThatExceptionOfType(DeviceServiceClientException.class)
                .isThrownBy(() -> deviceServiceClient.getFilterByUserIdAndName(userId, filterName));
        mockServer.verify();
    }


    @Test
    public void deleteFilter_whenFails_shouldThrowException() throws Exception {
        // Given
        final Endpoint endpoint = DeviceServiceClient.DELETE_FILTER_ENDPOINT;
        final String userId = UUID.randomUUID().toString();
        final String filterName = UUID.randomUUID().toString();

        mockServer.expect(method(endpoint.getMethod()))
                .andExpect(requestTo(endpoint.withBase(baseUrl).getURI(userId, filterName)))
                .andRespond(withBadRequest());

        // ThenWhen
        assertThatExceptionOfType(DeviceServiceClientException.class)
                .isThrownBy(() -> deviceServiceClient.deleteFilter(userId, filterName));
        mockServer.verify();
    }

    @Test
    public void deleteFilter_whenSucceeds_shouldDeleteFilter() throws Exception {
        // Given
        final Endpoint endpoint = DeviceServiceClient.DELETE_FILTER_ENDPOINT;
        final String userId = UUID.randomUUID().toString();
        final String filterName = UUID.randomUUID().toString();

        mockServer.expect(method(endpoint.getMethod()))
                .andExpect(requestTo(endpoint.withBase(baseUrl).getURI(userId, filterName)))
                .andRespond(withNoContent());

        //When
        deviceServiceClient.deleteFilter(userId, filterName);

        // Then
        mockServer.verify();
    }

}
