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
import io.barracks.commons.util.Endpoint;
import io.barracks.membergateway.client.exception.DeviceServiceClientException;
import io.barracks.membergateway.model.BarracksQuery;
import io.barracks.membergateway.model.Device;
import io.barracks.membergateway.model.DeviceEvent;
import io.barracks.membergateway.model.Filter;
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

@Component
public class DeviceServiceClient extends HateoasRestClient {

    /* Devices endpoints */
    static final Endpoint GET_DEVICES_ENDPOINT = Endpoint.from(HttpMethod.GET, "/owners/{userId}/devices", "query={query}");
    static final Endpoint GET_DEVICE_ENDPOINT = Endpoint.from(HttpMethod.GET, "/owners/{userId}/devices/{unitId}");

    /* Device events endpoints */
    static final Endpoint GET_DEVICE_EVENTS_ENDPOINT = Endpoint.from(HttpMethod.GET, "/owners/{userId}/devices/{unitId}/events");

    /* Filters endpoints */
    static final Endpoint CREATE_FILTER_ENDPOINT = Endpoint.from(HttpMethod.POST, "/owners/{userId}/filters");
    static final Endpoint GET_FILTERS_ENDPOINT = Endpoint.from(HttpMethod.GET, "/owners/{userId}/filters");
    static final Endpoint DELETE_FILTER_ENDPOINT = Endpoint.from(HttpMethod.DELETE, "/owners/{userId}/filters/{name}");
    static final Endpoint GET_FILTER_ENDPOINT = Endpoint.from(HttpMethod.GET, "/owners/{userId}/filters/{name}");
    static final Endpoint UPDATE_FILTER_ENDPOINT = Endpoint.from(HttpMethod.POST, "/owners/{userId}/filters/{name}");

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

    public Device getDeviceByUserIdAndUnitId(String userId, String unitId) {
        try {
            final ResponseEntity<Device> responseEntity = restTemplate.exchange(
                    GET_DEVICE_ENDPOINT.withBase(baseUrl).getRequestEntity(userId, unitId),
                    Device.class
            );
            return responseEntity.getBody();
        } catch (HttpStatusCodeException e) {
            throw new DeviceServiceClientException(e);
        }
    }

    public PagedResources<DeviceEvent> getDeviceEvents(Pageable pageable, String userId, String unitId) {
        try {
            final ResponseEntity<PagedResources<DeviceEvent>> responseEntity = restTemplate.exchange(
                    GET_DEVICE_EVENTS_ENDPOINT.withBase(baseUrl).pageable(pageable).getRequestEntity(userId, unitId),
                    new ParameterizedTypeReference<PagedResources<DeviceEvent>>() {
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
                    CREATE_FILTER_ENDPOINT.withBase(baseUrl).body(filter).getRequestEntity(userId),
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

    public Filter getFilterByUserIdAndName(String userId, String name) {
        try {
            final ResponseEntity<Filter> responseEntity = restTemplate.exchange(
                    GET_FILTER_ENDPOINT.withBase(baseUrl).getRequestEntity(userId, name),
                    Filter.class
            );
            return responseEntity.getBody();
        }
        catch (HttpStatusCodeException e) {
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

    public Filter updateFilter(String userId, String name, JsonNode query) {
        try {
            final ResponseEntity<Filter> responseEntity = restTemplate.exchange(
                    UPDATE_FILTER_ENDPOINT.withBase(baseUrl).body(query).getRequestEntity(userId, name),
                    Filter.class
            );
            return responseEntity.getBody();
        } catch (HttpStatusCodeException e) {
            throw new DeviceServiceClientException(e);
        }
    }
}
