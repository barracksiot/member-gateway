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

import com.fasterxml.jackson.databind.ObjectMapper;
import io.barracks.commons.util.Endpoint;
import io.barracks.membergateway.client.exception.DeviceServiceClientException;
import io.barracks.membergateway.manager.entity.SegmentStatus;
import io.barracks.membergateway.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.PagedResources;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Component
public class DeviceServiceClient extends HateoasRestClient {

    /* Devices endpoints */
    static final Endpoint GET_DEVICES_ENDPOINT = Endpoint.from(HttpMethod.GET, "/devices", "userId={userId}&query={query}");
    static final Endpoint GET_DEVICE_ENDPOINT = Endpoint.from(HttpMethod.GET, "/devices/{unitId}", "userId={userId}");

    /* Device events endpoints */
    static final Endpoint GET_DEVICE_EVENTS_ENDPOINT = Endpoint.from(HttpMethod.GET, "/devices/{unitId}/events", "userId={userId}");

    /* Device configurations endpoints */
    static final Endpoint GET_DEVICE_CONFIGURATION_ENDPOINT = Endpoint.from(HttpMethod.GET, "/devices/{unitId}/configuration", "userId={userId}");
    static final Endpoint CREATE_DEVICE_CONFIGURATION_ENDPOINT = Endpoint.from(HttpMethod.POST, "/devices/{unitId}/configuration", "userId={userId}");

    /* Segments endpoints */
    static final Endpoint CREATE_SEGMENT_ENDPOINT = Endpoint.from(HttpMethod.POST, "/segments");
    static final Endpoint GET_SEGMENTS_ENDPOINT = Endpoint.from(HttpMethod.GET, "/segments", "userId={userId}");
    static final Endpoint GET_SEGMENT_ENDPOINT = Endpoint.from(HttpMethod.GET, "/segments/{segmentId}");
    static final Endpoint GET_SEGMENT_BY_STATUS = Endpoint.from(HttpMethod.GET, "/segments", "userId={userId}&status={status}");
    static final Endpoint UPDATE_SEGMENT_ENDPOINT = Endpoint.from(HttpMethod.PUT, "/segments/{segmentId}");
    static final Endpoint GET_SEGMENT_DEVICES_ENDPOINT = Endpoint.from(HttpMethod.GET, "/segments/{segmentId}/devices", "userId={userId}");
    static final Endpoint GET_SEGMENT_DEVICES_FOR_VERSION_ENDPOINT = Endpoint.from(HttpMethod.GET, "/segments/{segmentId}/devices", "userId={userId}&versionId={versionId}");
    static final Endpoint UPDATE_SEGMENT_ORDER_ENDPOINT = Endpoint.from(HttpMethod.PUT, "/segments/order", "userId={userId}");

    /* Filters endpoints */
    static final Endpoint CREATE_FILTER_ENDPOINT = Endpoint.from(HttpMethod.POST, "/owners/{userId}/filters");
    static final Endpoint GET_FILTERS_ENDPOINT = Endpoint.from(HttpMethod.GET, "/owners/{userId}/filters");
    static final Endpoint DELETE_FILTER_ENDPOINT = Endpoint.from(HttpMethod.DELETE, "/owners/{userId}/filters/{name}");

    private final String baseUrl;

    private final RestTemplate restTemplate;

    @Autowired
    public DeviceServiceClient(
            ObjectMapper mapper,
            RestTemplateBuilder restTemplateBuilder,
            @Value("${io.barracks.deviceservice.base_url}") String baseUrl
    ) {
        this.restTemplate = prepareRestTemplateBuilder(mapper, restTemplateBuilder).build();
        this.baseUrl = baseUrl;
    }

    public PagedResources<DeviceEvent> getDeviceEvents(Pageable pageable, String userId, String unitId) {
        try {
            final ResponseEntity<PagedResources<DeviceEvent>> responseEntity = restTemplate.exchange(
                    GET_DEVICE_EVENTS_ENDPOINT.withBase(baseUrl).pageable(pageable).getRequestEntity(unitId, userId),
                    new ParameterizedTypeReference<PagedResources<DeviceEvent>>() {
                    }
            );
            return responseEntity.getBody();
        } catch (HttpStatusCodeException e) {
            throw new DeviceServiceClientException(e);
        }
    }

    public DeviceConfiguration getDeviceConfiguration(String userId, String unitId) {
        try {
            final ResponseEntity<DeviceConfiguration> responseEntity = restTemplate.exchange(
                    GET_DEVICE_CONFIGURATION_ENDPOINT.withBase(baseUrl).getRequestEntity(unitId, userId),
                    DeviceConfiguration.class
            );
            return responseEntity.getBody();
        } catch (HttpStatusCodeException e) {
            throw new DeviceServiceClientException(e);
        }
    }

    public DeviceConfiguration setDeviceConfiguration(String userId, String unitId, DeviceConfiguration configuration) {
        try {
            final ResponseEntity<DeviceConfiguration> responseEntity = restTemplate.exchange(
                    CREATE_DEVICE_CONFIGURATION_ENDPOINT.withBase(baseUrl).body(configuration).getRequestEntity(unitId, userId), //new HttpEntity<>(configuration, jsonHeaders),
                    DeviceConfiguration.class
            );
            return responseEntity.getBody();
        } catch (HttpStatusCodeException e) {
            throw new DeviceServiceClientException(e);
        }
    }

    public Segment createSegment(Segment segment) {
        try {
            final ResponseEntity<Segment> responseEntity = restTemplate.exchange(
                    CREATE_SEGMENT_ENDPOINT.withBase(baseUrl).body(segment).getRequestEntity(), //jsonHeaders
                    Segment.class
            );
            return responseEntity.getBody();
        } catch (HttpStatusCodeException e) {
            throw new DeviceServiceClientException(e);
        }
    }

    public Segment getSegment(String segmentId) {
        try {
            final ResponseEntity<Segment> responseEntity = restTemplate.exchange(
                    GET_SEGMENT_ENDPOINT.withBase(baseUrl).getRequestEntity(segmentId), // jsonHeaders
                    Segment.class
            );
            return responseEntity.getBody();
        } catch (HttpStatusCodeException e) {
            throw new DeviceServiceClientException(e);
        }
    }

    public Segment updateSegment(String segmentId, Segment segment) {
        try {
            final ResponseEntity<Segment> responseEntity = restTemplate.exchange(
                    UPDATE_SEGMENT_ENDPOINT.withBase(baseUrl).body(segment).getRequestEntity(segmentId), // jsonHeaders
                    Segment.class
            );
            return responseEntity.getBody();
        } catch (HttpStatusCodeException e) {
            throw new DeviceServiceClientException(e);
        }
    }

    public PagedResources<Segment> getSegments(String userId, Pageable pageable) {
        try {
            final ResponseEntity<PagedResources<Segment>> responseEntity = restTemplate.exchange(
                    GET_SEGMENTS_ENDPOINT.withBase(baseUrl).pageable(pageable).getRequestEntity(userId),
                    new ParameterizedTypeReference<PagedResources<Segment>>() {
                    }
            );
            return responseEntity.getBody();
        } catch (HttpStatusCodeException e) {
            throw new DeviceServiceClientException(e);
        }
    }

    public List<Segment> getSegmentsByStatus(String userId, SegmentStatus status) {
        try {
            final ResponseEntity<List<Segment>> responseEntity = restTemplate.exchange(
                    GET_SEGMENT_BY_STATUS.withBase(baseUrl).getRequestEntity(userId, status.getName()), // jsonHeaders
                    new ParameterizedTypeReference<List<Segment>>() {
                    }
            );
            return responseEntity.getBody();
        } catch (HttpStatusCodeException e) {
            throw new DeviceServiceClientException(e);
        }
    }

    public List<String> updateSegmentsOrder(String userId, List<String> order) {
        try {
            final ResponseEntity<List<String>> responseEntity = restTemplate.exchange(
                    UPDATE_SEGMENT_ORDER_ENDPOINT.withBase(baseUrl).body(order).getRequestEntity(userId), // jsonHeaders
                    new ParameterizedTypeReference<List<String>>() {
                    }
            );
            return responseEntity.getBody();
        } catch (HttpStatusCodeException e) {
            throw new DeviceServiceClientException(e);
        }
    }

    public Device getDeviceByUserIdAndUnitId(String userId, String unitId) {
        try {
            final ResponseEntity<Device> responseEntity = restTemplate.exchange(
                    GET_DEVICE_ENDPOINT.withBase(baseUrl).getRequestEntity(unitId, userId), // jsonHeaders
                    Device.class
            );
            return responseEntity.getBody();
        } catch (HttpStatusCodeException e) {
            throw new DeviceServiceClientException(e);
        }
    }

    public PagedResources<Device> getDevicesBySegment(String userId, String segmentId, Pageable pageable) {
        try {
            final ResponseEntity<PagedResources<Device>> responseEntity = restTemplate.exchange(
                    GET_SEGMENT_DEVICES_ENDPOINT.withBase(baseUrl).pageable(pageable).getRequestEntity(segmentId, userId),
                    new ParameterizedTypeReference<PagedResources<Device>>() {
                    }
            );
            return responseEntity.getBody();
        } catch (HttpStatusCodeException e) {
            throw new DeviceServiceClientException(e);
        }
    }

    public PagedResources<Device> getDevicesBySegmentAndVersion(String userId, String segmentId, String versionId, Pageable pageable) {
        try {
            return restTemplate.exchange(
                    GET_SEGMENT_DEVICES_FOR_VERSION_ENDPOINT.withBase(baseUrl).pageable(pageable).getRequestEntity(segmentId, userId, versionId),
                    new ParameterizedTypeReference<PagedResources<Device>>() {
                    }
            ).getBody();
        } catch (HttpStatusCodeException e) {
            throw new DeviceServiceClientException(e);
        }
    }

    public PagedResources<Device> getDevices(String userId, Pageable pageable, BarracksQuery query) {
        try {
            final ResponseEntity<PagedResources<Device>> responseEntity = restTemplate.exchange(
                    GET_DEVICES_ENDPOINT.withBase(baseUrl).pageable(pageable).getRequestEntity(userId, query.toJsonString()),
                    new ParameterizedTypeReference<PagedResources<Device>>() {
                    }
            );
            return responseEntity.getBody();
        } catch (HttpStatusCodeException e) {
            throw new DeviceServiceClientException(e);
        }
    }

    public Filter createFilter(String userId, Filter filter) {
        try {
            final ResponseEntity<Filter> responseEntity = restTemplate.exchange(
                    CREATE_FILTER_ENDPOINT.withBase(baseUrl).body(filter).getRequestEntity(userId), // jsonHeaders
                    Filter.class
            );
            return responseEntity.getBody();
        } catch (HttpStatusCodeException e) {
            throw new DeviceServiceClientException(e);
        }
    }

    public PagedResources<Filter> getFilters(String userId, Pageable pageable) {
        try {
            final ResponseEntity<PagedResources<Filter>> responseEntity = restTemplate.exchange(
                    GET_FILTERS_ENDPOINT.withBase(baseUrl).pageable(pageable).getRequestEntity(userId),
                    new ParameterizedTypeReference<PagedResources<Filter>>() {
                    }
            );
            return responseEntity.getBody();
        } catch (HttpStatusCodeException e) {
            throw new DeviceServiceClientException(e);
        }
    }

    public void deleteFilter(String userId, String name) {
        try {
            restTemplate.exchange(
                    DELETE_FILTER_ENDPOINT.withBase(baseUrl).getRequestEntity(userId, name),
                    Void.class
            );
        } catch (HttpStatusCodeException e) {
            throw new DeviceServiceClientException(e);
        }
    }
}
