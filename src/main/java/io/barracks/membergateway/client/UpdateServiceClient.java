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
import io.barracks.membergateway.client.exception.UpdateServiceClientException;
import io.barracks.membergateway.model.Update;
import io.barracks.membergateway.model.UpdateStatus;
import io.barracks.membergateway.model.UpdateStatusCompatibility;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.PagedResources;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class UpdateServiceClient extends HateoasRestClient {
    static final Endpoint CREATE_UPDATE_ENDPOINT = Endpoint.from(HttpMethod.POST, "/updates");
    static final Endpoint LIST_UPDATES_ENDPOINT = Endpoint.from(HttpMethod.GET, "/updates", "userId={userId}&status={statuses}&segmentId={segmentIds}");
    static final Endpoint GET_UPDATE_ENDPOINT = Endpoint.from(HttpMethod.GET, "/updates/{uuid}", "userId={userId}");
    static final Endpoint EDIT_UPDATE_ENDPOINT = Endpoint.from(HttpMethod.PUT, "/updates/{uuid}", "userId={userId}");
    static final Endpoint GET_STATUS_ENDPOINT = Endpoint.from(HttpMethod.GET, "/status/{statusName}");
    static final Endpoint GET_STATUSES_ENDPOINT = Endpoint.from(HttpMethod.GET, "/status");
    static final Endpoint GET_LATEST_UPDATE_FOR_SEGMENT_ENDPOINT = Endpoint.from(HttpMethod.GET, "/updates/latest", "userId={userId}&segmentId={segmentId}");

    private final String baseUrl;

    private final RestTemplate restTemplate;

    @Autowired
    public UpdateServiceClient(
            ObjectMapper mapper,
            RestTemplateBuilder restTemplateBuilder,
            @Value("${io.barracks.updateservice.base_url}") String baseUrl
    ) {
        this.restTemplate = prepareRestTemplateBuilder(mapper, restTemplateBuilder).build();
        this.baseUrl = baseUrl;
    }

    public Update getUpdateByUuidAndUserId(String uuid, String userId) {
        try {
            return restTemplate.exchange(
                    GET_UPDATE_ENDPOINT.withBase(baseUrl).getRequestEntity(uuid, userId),
                    Update.class
            ).getBody();
        } catch (HttpStatusCodeException e) {
            throw new UpdateServiceClientException(e);
        }
    }

    public PagedResources<Update> getUpdatesByStatusesAndSegments(Pageable pageable, String userId, List<UpdateStatus> statuses, List<String> segmentIds) {
        try {
            final String statusesParam = String.join(",", statuses.stream().map(UpdateStatus::getName).collect(Collectors.toList()));
            final String segmentsParam = String.join(",", segmentIds);
            return restTemplate.exchange(
                    LIST_UPDATES_ENDPOINT.withBase(baseUrl)
                            .pageable(pageable)
                            .getRequestEntity(
                                    userId,
                                    statusesParam,
                                    segmentsParam
                            ),
                    new ParameterizedTypeReference<PagedResources<Update>>() {
                    }
            ).getBody();
        } catch (HttpStatusCodeException e) {
            throw new UpdateServiceClientException(e);
        }
    }

    public Optional<Update> getLatestUpdateForSegment(String userId, String segmentId) {
        try {
            final ResponseEntity<Update> response = restTemplate.exchange(
                    GET_LATEST_UPDATE_FOR_SEGMENT_ENDPOINT.withBase(baseUrl).getRequestEntity(userId, segmentId),
                    Update.class
            );
            if (response.getStatusCode() == HttpStatus.NO_CONTENT) {
                return Optional.empty();
            } else {
                return Optional.of(response.getBody());
            }
        } catch (HttpStatusCodeException e) {
            throw new UpdateServiceClientException(e);
        }
    }

    public Update createUpdate(Update update) {
        try {
            return restTemplate.exchange(
                    CREATE_UPDATE_ENDPOINT.withBase(baseUrl).body(update).getRequestEntity(),
                    Update.class
            ).getBody();
        } catch (HttpStatusCodeException e) {
            throw new UpdateServiceClientException(e);
        }
    }

    public Update editUpdate(Update update) {
        try {
            return restTemplate.exchange(
                    EDIT_UPDATE_ENDPOINT.withBase(baseUrl).body(update).getRequestEntity(update.getUuid(), update.getUserId()),
                    Update.class
            ).getBody();
        } catch (HttpStatusCodeException e) {
            throw new UpdateServiceClientException(e);
        }
    }

    public List<UpdateStatusCompatibility> getAllStatusesCompatibilities() {
        try {
            return restTemplate.exchange(
                    GET_STATUSES_ENDPOINT.withBase(baseUrl).getRequestEntity(),
                    new ParameterizedTypeReference<List<UpdateStatusCompatibility>>() {
                    }
            ).getBody();
        } catch (HttpStatusCodeException e) {
            throw new UpdateServiceClientException(e);
        }
    }

    public UpdateStatusCompatibility getStatusCompatibilities(UpdateStatus status) {
        try {
            return restTemplate.exchange(
                    GET_STATUS_ENDPOINT.withBase(baseUrl).getRequestEntity(status.getName()),
                    UpdateStatusCompatibility.class
            ).getBody();
        } catch (HttpStatusCodeException e) {
            throw new UpdateServiceClientException(e);
        }
    }
}
