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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import io.barracks.commons.exceptions.BarracksServiceClientException;
import io.barracks.commons.util.Endpoint;
import io.barracks.membergateway.client.exception.DeviceServiceClientException;
import io.barracks.membergateway.manager.entity.SegmentStatus;
import io.barracks.membergateway.model.*;
import io.barracks.membergateway.utils.BarracksQueryUtils;
import io.barracks.membergateway.utils.FilterUtils;
import io.barracks.membergateway.utils.SegmentUtils;
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
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.client.MockRestServiceServer;

import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.Assert.assertEquals;
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
    @Value("classpath:io/barracks/membergateway/client/deviceConfiguration.json")
    private Resource deviceConfiguration;
    @Value("classpath:io/barracks/membergateway/client/device.json")
    private Resource device;
    @Value("classpath:io/barracks/membergateway/client/devices.json")
    private Resource devices;
    @Value("classpath:io/barracks/membergateway/client/segment.json")
    private Resource segment;
    @Value("classpath:io/barracks/membergateway/client/segments.json")
    private Resource segments;
    @Value("classpath:io/barracks/membergateway/client/segmentList.json")
    private Resource segmentList;
    @Value("classpath:io/barracks/membergateway/client/segmentOrder.json")
    private Resource segmentOrder;
    @Value("classpath:io/barracks/membergateway/client/filter.json")
    private Resource filter;
    @Value("classpath:io/barracks/membergateway/client/filters.json")
    private Resource filters;
    @Value("classpath:io/barracks/membergateway/client/filters-empty.json")
    private Resource filtersEmpty;

    @Test
    public void getDeviceEvents_whenServiceFails_shouldThrowClientException() throws UnsupportedEncodingException {
        // Given
        final Endpoint endpoint = DeviceServiceClient.GET_DEVICE_EVENTS_ENDPOINT;
        final String userId = UUID.randomUUID().toString();
        final String unitId = UUID.randomUUID().toString();
        final Pageable pageable = new PageRequest(0, 10);
        mockServer.expect(method(endpoint.getMethod()))
                .andExpect(requestTo(endpoint.withBase(baseUrl).pageable(pageable).getURI(unitId, userId)))
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
                .andExpect(requestTo(endpoint.withBase(baseUrl).pageable(pageable).getURI(unitId, userId)))
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
        final Pageable pageable = new PageRequest(0, 10);
        mockServer.expect(method(endpoint.getMethod()))
                .andExpect(requestTo(endpoint.withBase(baseUrl).pageable(pageable).getURI(unitId, userId)))
                .andRespond(withSuccess().body(deviceEvents));

        // When
        final PagedResources<DeviceEvent> result = deviceServiceClient.getDeviceEvents(pageable, userId, unitId);

        // Then
        mockServer.verify();
        final ArrayNode deviceEvents = ((ArrayNode) jsonObject.get("_embedded").get("deviceEvents"));
        assertThat(result).isNotNull();
        assertThat(result.getContent())
                .isNotNull()
                .hasSize(deviceEvents.size());
    }

    @Test
    public void getConfiguration_shouldReturnConfiguration_whenRequestSucceeds() throws Exception {
        final Endpoint endpoint = DeviceServiceClient.GET_DEVICE_CONFIGURATION_ENDPOINT;
        final String userId = UUID.randomUUID().toString();
        final String unitId = UUID.randomUUID().toString();
        final DeviceConfiguration expected = DeviceConfiguration.builder().build();
        mockServer.expect(method(endpoint.getMethod()))
                .andExpect(requestTo(endpoint.withBase(baseUrl).getURI(unitId, userId)))
                .andRespond(withSuccess().body(deviceConfiguration));

        // When
        final DeviceConfiguration result = deviceServiceClient.getDeviceConfiguration(userId, unitId);

        // Then
        mockServer.verify();
        assertEquals(result, expected);
    }

    @Test
    public void getConfiguration_shouldThrowException_whenRequestFails() throws Exception {
        final Endpoint endpoint = DeviceServiceClient.GET_DEVICE_CONFIGURATION_ENDPOINT;
        final String userId = UUID.randomUUID().toString();
        final String unitId = UUID.randomUUID().toString();
        mockServer.expect(method(endpoint.getMethod()))
                .andExpect(requestTo(endpoint.withBase(baseUrl).getURI(unitId, userId)))
                .andRespond(withServerError());

        // Then When
        assertThatExceptionOfType(BarracksServiceClientException.class)
                .isThrownBy(() -> deviceServiceClient.getDeviceConfiguration(userId, unitId));
        mockServer.verify();
    }


    @Test
    public void setConfiguration_shouldReturnConfiguration_whenRequestSucceeds() throws Exception {
        final Endpoint endpoint = DeviceServiceClient.CREATE_DEVICE_CONFIGURATION_ENDPOINT;
        final String userId = UUID.randomUUID().toString();
        final String unitId = UUID.randomUUID().toString();
        final DeviceConfiguration requested = DeviceConfiguration.builder().build();
        final DeviceConfiguration expected = DeviceConfiguration.builder().build();
        mockServer.expect(method(endpoint.getMethod()))
                .andExpect(requestTo(endpoint.withBase(baseUrl).getURI(unitId, userId)))
                .andRespond(withSuccess().body(deviceConfiguration));

        // When
        final DeviceConfiguration result = deviceServiceClient.setDeviceConfiguration(userId, unitId, requested);

        // Then
        mockServer.verify();
        assertEquals(result, expected);
    }

    @Test
    public void setConfiguration_shouldThrowException_whenRequestFails() throws Exception {
        // Given
        final Endpoint endpoint = DeviceServiceClient.CREATE_DEVICE_CONFIGURATION_ENDPOINT;
        final String userId = UUID.randomUUID().toString();
        final String unitId = UUID.randomUUID().toString();
        final DeviceConfiguration requested = DeviceConfiguration.builder().build();
        mockServer.expect(method(endpoint.getMethod()))
                .andExpect(requestTo(endpoint.withBase(baseUrl).getURI(unitId, userId)))
                .andRespond(withServerError());

        // Then When
        assertThatExceptionOfType(BarracksServiceClientException.class)
                .isThrownBy(() -> deviceServiceClient.setDeviceConfiguration(userId, unitId, requested));
        mockServer.verify();
    }

    @Test
    public void createSegment_whenSucceeds_shouldReturnSegment() throws Exception {
        // Given
        final Endpoint endpoint = DeviceServiceClient.CREATE_SEGMENT_ENDPOINT;
        final Segment segment = SegmentUtils.getSegment();
        final Segment expected = mapper.readValue(this.segment.getInputStream(), Segment.class);
        mockServer.expect(method(endpoint.getMethod()))
                .andExpect(requestTo(endpoint.withBase(baseUrl).getURI()))
                .andExpect(content().string(mapper.writeValueAsString(segment)))
                .andRespond(withSuccess().body(this.segment));

        // When
        Segment result = deviceServiceClient.createSegment(segment);

        // Then
        mockServer.verify();
        assertThat(result).isEqualTo(expected);
    }

    @Test
    public void createSegment_whenFails_shouldThrowException() throws Exception {
        // Given
        final Endpoint endpoint = DeviceServiceClient.CREATE_SEGMENT_ENDPOINT;
        final Segment segment = SegmentUtils.getSegment();
        mockServer.expect(method(endpoint.getMethod()))
                .andExpect(requestTo(endpoint.withBase(baseUrl).getURI()))
                .andExpect(content().string(mapper.writeValueAsString(segment)))
                .andRespond(withBadRequest());

        // ThenWhen
        assertThatExceptionOfType(DeviceServiceClientException.class)
                .isThrownBy(() -> deviceServiceClient.createSegment(segment));
        mockServer.verify();
    }

    @Test
    public void updateSegment_whenSucceeds_shouldReturnSegment() throws Exception {
        // Given
        final Endpoint endpoint = DeviceServiceClient.UPDATE_SEGMENT_ENDPOINT;
        final String segmentId = UUID.randomUUID().toString();
        final Segment segment = SegmentUtils.getSegment();
        final Segment expected = mapper.readValue(this.segment.getInputStream(), Segment.class);
        mockServer.expect(method(endpoint.getMethod()))
                .andExpect(requestTo(endpoint.withBase(baseUrl).getURI(segmentId)))
                .andExpect(content().string(mapper.writeValueAsString(segment)))
                .andRespond(withSuccess().body(this.segment));

        // When
        Segment result = deviceServiceClient.updateSegment(segmentId, segment);

        // Then
        mockServer.verify();
        assertThat(result).isEqualTo(expected);
    }

    @Test
    public void updateSegment_whenFails_shouldThrowException() throws Exception {
        // Given
        final Endpoint endpoint = DeviceServiceClient.UPDATE_SEGMENT_ENDPOINT;
        final String segmentId = UUID.randomUUID().toString();
        final Segment segment = SegmentUtils.getSegment();
        mockServer.expect(method(endpoint.getMethod()))
                .andExpect(requestTo(endpoint.withBase(baseUrl).getURI(segmentId)))
                .andExpect(content().string(mapper.writeValueAsString(segment)))
                .andRespond(withBadRequest());

        // ThenWhen
        assertThatExceptionOfType(DeviceServiceClientException.class)
                .isThrownBy(() -> deviceServiceClient.updateSegment(segmentId, segment));
        mockServer.verify();
    }

    @Test
    public void getSegment_whenSucceeds_shouldReturnSegment() throws Exception {
        // Given
        final Endpoint endpoint = DeviceServiceClient.GET_SEGMENT_ENDPOINT;
        final String segmentId = UUID.randomUUID().toString();
        final Segment expected = mapper.readValue(this.segment.getInputStream(), Segment.class);
        mockServer.expect(method(endpoint.getMethod()))
                .andExpect(requestTo(endpoint.withBase(baseUrl).getURI(segmentId)))
                .andRespond(withSuccess().body(this.segment));

        // When
        Segment result = deviceServiceClient.getSegment(segmentId);

        // Then
        mockServer.verify();
        assertThat(result).isEqualTo(expected);
    }

    @Test
    public void getSegment_whenFails_shouldThrowException() throws Exception {
        // Given
        final Endpoint endpoint = DeviceServiceClient.GET_SEGMENT_ENDPOINT;
        final String segmentId = UUID.randomUUID().toString();
        mockServer.expect(method(endpoint.getMethod()))
                .andExpect(requestTo(endpoint.withBase(baseUrl).getURI(segmentId)))
                .andRespond(withBadRequest());

        // ThenWhen
        assertThatExceptionOfType(DeviceServiceClientException.class)
                .isThrownBy(() -> deviceServiceClient.getSegment(segmentId));
        mockServer.verify();
    }

    @Test
    public void getSegments_whenSucceeds_shouldReturnSegments() throws Exception {
        // Given
        final Endpoint endpoint = DeviceServiceClient.GET_SEGMENTS_ENDPOINT;
        final String userId = UUID.randomUUID().toString();
        final Pageable pageable = new PageRequest(0, 10);
        final PagedResources<Segment> resources = mapper.readValue(this.segments.getInputStream(), new TypeReference<PagedResources<Segment>>() {
        });
        mockServer.expect(method(endpoint.getMethod()))
                .andExpect(requestTo(endpoint.withBase(baseUrl).pageable(pageable).getURI(userId)))
                .andRespond(withSuccess().body(this.segments));

        // When
        PagedResources<Segment> result = deviceServiceClient.getSegments(userId, pageable);

        // Then
        mockServer.verify();
        assertThat(result).isNotEmpty();
        assertThat(result).isEqualTo(resources);
    }

    @Test
    public void getSegments_whenFails_shouldThrowException() throws Exception {
        // Given
        final Endpoint endpoint = DeviceServiceClient.GET_SEGMENTS_ENDPOINT;
        final String userId = UUID.randomUUID().toString();
        final Pageable pageable = new PageRequest(0, 10);
        mockServer.expect(method(endpoint.getMethod()))
                .andExpect(requestTo(endpoint.withBase(baseUrl).pageable(pageable).getURI(userId)))
                .andRespond(withBadRequest());

        // ThenWhen
        assertThatExceptionOfType(DeviceServiceClientException.class)
                .isThrownBy(() -> deviceServiceClient.getSegments(userId, pageable));
        mockServer.verify();
    }

    @Test
    public void getSegmentsByStatus_whenSucceeds_shouldReturnSegments() throws Exception {
        // Given
        final Endpoint endpoint = DeviceServiceClient.GET_SEGMENT_BY_STATUS;
        final String userId = UUID.randomUUID().toString();
        final SegmentStatus status = SegmentStatus.ACTIVE;
        mockServer.expect(method(endpoint.getMethod()))
                .andExpect(requestTo(endpoint.withBase(baseUrl).getURI(userId, status.getName())))
                .andRespond(withSuccess().body(segmentList));

        // When
        final List<Segment> result = deviceServiceClient.getSegmentsByStatus(userId, status);

        // Then
        mockServer.verify();
        assertThat(result).isNotEmpty();
    }

    @Test
    public void getSegmentsByStatus_whenFails_shouldThrowException() throws UnsupportedEncodingException {
        // Given
        final Endpoint endpoint = DeviceServiceClient.GET_SEGMENT_BY_STATUS;
        final String userId = UUID.randomUUID().toString();
        final SegmentStatus status = SegmentStatus.INACTIVE;
        mockServer.expect(method(endpoint.getMethod()))
                .andExpect(requestTo(endpoint.withBase(baseUrl).getURI(userId, status.getName())))
                .andRespond(withBadRequest());

        // Then When
        assertThatExceptionOfType(DeviceServiceClientException.class)
                .isThrownBy(() -> deviceServiceClient.getSegmentsByStatus(userId, status));
        mockServer.verify();
    }

    @Test
    public void updateSegmentsOrder_whenSucceeds_shouldReturnUpdatedList() throws Exception {
        // Given
        final Endpoint endpoint = DeviceServiceClient.UPDATE_SEGMENT_ORDER_ENDPOINT;
        final String userId = UUID.randomUUID().toString();
        final List<String> order = Collections.singletonList(UUID.randomUUID().toString());
        final List<String> expected = mapper.readValue(segmentOrder.getInputStream(), new TypeReference<List<String>>() {
        });
        mockServer.expect(method(endpoint.getMethod()))
                .andExpect(requestTo(endpoint.withBase(baseUrl).getURI(userId)))
                .andExpect(content().string(mapper.writeValueAsString(order)))
                .andRespond(withSuccess().body(segmentOrder));

        // When
        final List<String> result = deviceServiceClient.updateSegmentsOrder(userId, order);

        // Then
        mockServer.verify();
        assertThat(result).containsExactlyElementsOf(expected);
    }

    @Test
    public void updateSegmentsOrder_whenFails_shouldThrowException() throws Exception {
        // Given
        final Endpoint endpoint = DeviceServiceClient.UPDATE_SEGMENT_ORDER_ENDPOINT;
        final String userId = UUID.randomUUID().toString();
        final List<String> order = Collections.singletonList(UUID.randomUUID().toString());
        mockServer.expect(method(endpoint.getMethod()))
                .andExpect(requestTo(endpoint.withBase(baseUrl).getURI(userId)))
                .andExpect(content().string(mapper.writeValueAsString(order)))
                .andRespond(withBadRequest());

        // Then When
        assertThatExceptionOfType(DeviceServiceClientException.class)
                .isThrownBy(() -> deviceServiceClient.updateSegmentsOrder(userId, order));
        mockServer.verify();
    }

    @Test
    public void getDevicesInSegment_whenSucceeds_shouldReturnDevices() throws Exception {
        // Given
        final Endpoint endpoint = DeviceServiceClient.GET_SEGMENT_DEVICES_ENDPOINT;
        final String userId = UUID.randomUUID().toString();
        final String segmentId = UUID.randomUUID().toString();
        final Pageable pageable = new PageRequest(0, 10);

        mockServer.expect(method(endpoint.getMethod()))
                .andExpect(requestTo(endpoint.withBase(baseUrl).pageable(pageable).getURI(segmentId, userId)))
                .andRespond(withSuccess().body(devices));

        // When
        PagedResources<Device> result = deviceServiceClient.getDevicesBySegment(userId, segmentId, pageable);

        // Then
        mockServer.verify();
        assertThat(result).isNotEmpty();
    }

    @Test
    public void getDevicesInSegment_whenFails_shouldThrowException() throws Exception {
        // Given
        final Endpoint endpoint = DeviceServiceClient.GET_SEGMENT_DEVICES_ENDPOINT;
        final String userId = UUID.randomUUID().toString();
        final String segmentId = UUID.randomUUID().toString();
        final Pageable pageable = new PageRequest(0, 10);

        mockServer.expect(method(endpoint.getMethod()))
                .andExpect(requestTo(endpoint.withBase(baseUrl).pageable(pageable).getURI(segmentId, userId)))
                .andRespond(withBadRequest());

        // ThenWhen
        assertThatExceptionOfType(DeviceServiceClientException.class)
                .isThrownBy(() -> deviceServiceClient.getDevicesBySegment(userId, segmentId, pageable));
        mockServer.verify();
    }

    @Test
    public void getDevicesBySegmentAndVersion_whenSucceeds_shouldReturnDevices() throws Exception {
        // Given
        final Endpoint endpoint = DeviceServiceClient.GET_SEGMENT_DEVICES_FOR_VERSION_ENDPOINT;
        final String userId = UUID.randomUUID().toString();
        final String segmentId = UUID.randomUUID().toString();
        final String versionId = UUID.randomUUID().toString();
        final Pageable pageable = new PageRequest(0, 10);
        final PagedResources<Device> expected = mapper.readValue(devices.getInputStream(), new TypeReference<PagedResources<Device>>() {
        });

        mockServer.expect(method(endpoint.getMethod()))
                .andExpect(requestTo(endpoint.withBase(baseUrl).pageable(pageable).getURI(segmentId, userId, versionId)))
                .andRespond(withSuccess().body(devices));

        // When
        PagedResources<Device> result = deviceServiceClient.getDevicesBySegmentAndVersion(userId, segmentId, versionId, pageable);

        // Then
        mockServer.verify();
        assertThat(result).isEqualTo(expected);
    }

    @Test
    public void getDevicesBySegmentAndVersion_whenFails_shouldThrowException() throws Exception {
        // Given
        final Endpoint endpoint = DeviceServiceClient.GET_SEGMENT_DEVICES_FOR_VERSION_ENDPOINT;
        final String userId = UUID.randomUUID().toString();
        final String segmentId = UUID.randomUUID().toString();
        final String versionId = UUID.randomUUID().toString();
        final Pageable pageable = new PageRequest(0, 10);

        mockServer.expect(method(endpoint.getMethod()))
                .andExpect(requestTo(endpoint.withBase(baseUrl).pageable(pageable).getURI(segmentId, userId, versionId)))
                .andRespond(withBadRequest());

        // ThenWhen
        assertThatExceptionOfType(DeviceServiceClientException.class)
                .isThrownBy(() -> deviceServiceClient.getDevicesBySegmentAndVersion(userId, segmentId ,versionId, pageable));
        mockServer.verify();
    }

    @Test
    public void getDeviceByUserAndUnitId_whenSucceeds_shouldReturnDevice() throws Exception {
        // Given
        final Endpoint endpoint = DeviceServiceClient.GET_DEVICE_ENDPOINT;
        final String userId = UUID.randomUUID().toString();
        final String unitId = UUID.randomUUID().toString();
        final Device expected = mapper.readValue(device.getInputStream(), Device.class);
        mockServer.expect(method(endpoint.getMethod()))
                .andExpect(requestTo(endpoint.withBase(baseUrl).getURI(unitId, userId)))
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
                .andExpect(requestTo(endpoint.withBase(baseUrl).getURI(unitId, userId)))
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
        mockServer.expect(method(endpoint.getMethod()))
                .andExpect(requestTo(endpoint.withBase(baseUrl).pageable(pageable).getURI(userId, query.toJsonString())))
                .andRespond(withSuccess().body(devices));

        // When
        final PagedResources<Device> devices = deviceServiceClient.getDevices(userId, pageable, query);

        // Then
        mockServer.verify();
        assertThat(devices).isNotEmpty();
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
                .isThrownBy( () -> deviceServiceClient.deleteFilter(userId, filterName));
        mockServer.verify();
    }
}
