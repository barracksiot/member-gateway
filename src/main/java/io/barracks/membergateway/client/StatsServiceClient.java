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
import io.barracks.membergateway.client.exception.StatsServiceClientException;
import io.barracks.membergateway.model.DataSet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.time.OffsetDateTime;

@Component
public class StatsServiceClient extends HateoasRestClient {
    static final Endpoint DEVICES_PER_VERSION_ID_ENDPOINT = Endpoint.from(HttpMethod.GET, "/stats/{userId}/devices/perVersionId");
    static final Endpoint DEVICES_LAST_SEEN_ENDPOINT = Endpoint.from(HttpMethod.GET, "/stats/{userId}/devices/lastSeen", "start={start}&end={end}");
    static final Endpoint DEVICES_SEEN_ENDPOINT = Endpoint.from(HttpMethod.GET, "/stats/{userId}/devices/seen", "start={start}&end={end}");
    private final String baseUrl;
    private final RestTemplate restTemplate;

    @Autowired
    public StatsServiceClient(
            ObjectMapper mapper,
            RestTemplateBuilder restTemplateBuilder,
            @Value("${io.barracks.deviceservice.base_url}") String baseUrl
    ) {
        this.restTemplate = prepareRestTemplateBuilder(mapper, restTemplateBuilder).build();
        this.baseUrl = baseUrl;
    }

    public DataSet getDevicesPerVersionId(String userId) {
        try {
            return restTemplate.exchange(
                    DEVICES_PER_VERSION_ID_ENDPOINT.withBase(baseUrl).getRequestEntity(userId),
                    DataSet.class
            ).getBody();
        } catch (HttpStatusCodeException e) {
            throw new StatsServiceClientException(e);
        }
    }

    public DataSet getLastSeenDevices(String userId, OffsetDateTime start, OffsetDateTime end) {
        try {
            return restTemplate.exchange(
                    DEVICES_LAST_SEEN_ENDPOINT.withBase(baseUrl).getRequestEntity(userId, start, end),
                    DataSet.class
            ).getBody();
        } catch (HttpStatusCodeException e) {
            throw new StatsServiceClientException(e);
        }
    }

    public DataSet getSeenDevices(String userId, OffsetDateTime start, OffsetDateTime end) {
        try {
            return restTemplate.exchange(
                    DEVICES_SEEN_ENDPOINT.withBase(baseUrl).getRequestEntity(userId, start, end),
                    DataSet.class
            ).getBody();
        } catch (HttpStatusCodeException e) {
            throw new StatsServiceClientException(e);
        }
    }
}
