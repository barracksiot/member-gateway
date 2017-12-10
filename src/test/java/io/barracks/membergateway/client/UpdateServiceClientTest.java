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
import com.fasterxml.jackson.databind.ObjectMapper;
import io.barracks.commons.util.Endpoint;
import io.barracks.membergateway.client.exception.UpdateServiceClientException;
import io.barracks.membergateway.model.Update;
import io.barracks.membergateway.model.UpdateStatus;
import io.barracks.membergateway.model.UpdateStatusCompatibility;
import io.barracks.membergateway.utils.UpdateUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.PagedResources;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.HttpClientErrorException;

import java.util.*;
import java.util.stream.Collectors;

import static io.barracks.membergateway.client.UpdateServiceClient.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

@RunWith(SpringRunner.class)
@RestClientTest(UpdateServiceClient.class)
public class UpdateServiceClientTest {
    @Autowired
    private ObjectMapper mapper;
    @Autowired
    private MockRestServiceServer mockServer;
    @Autowired
    private UpdateServiceClient updateServiceClient;

    @Value("${io.barracks.updateservice.base_url}")
    private String baseUrl;
    @Value("classpath:io/barracks/membergateway/client/update.json")
    private Resource update;
    @Value("classpath:io/barracks/membergateway/client/updates.json")
    private Resource updates;
    @Value("classpath:io/barracks/membergateway/client/status.json")
    private Resource status;
    @Value("classpath:io/barracks/membergateway/client/statuses.json")
    private Resource statuses;

    @Test
    public void createUpdate_whenCreationSucceed_shouldReturnTheCreatedUpdate() throws Exception {
        // Given
        final Endpoint endpoint = CREATE_UPDATE_ENDPOINT;
        final Update toCreate = UpdateUtils.getUpdate();
        final Update expectedUpdate = mapper.readValue(update.getInputStream(), Update.class);

        mockServer.expect(method(endpoint.getMethod()))
                .andExpect(requestTo(endpoint.withBase(baseUrl).getURI()))
                .andExpect(content().string(mapper.writeValueAsString(toCreate)))
                .andRespond(withStatus(HttpStatus.CREATED).body(update));

        // When
        final Update result = updateServiceClient.createUpdate(toCreate);

        // Then
        mockServer.verify();
        assertThat(result).isEqualTo(expectedUpdate);
    }

    @Test
    public void createUpdate_whenCreationFail_shouldThrowAnException() {
        // Given
        final Endpoint endpoint = CREATE_UPDATE_ENDPOINT;
        final Update update = UpdateUtils.getUpdate();
        mockServer.expect(method(endpoint.getMethod()))
                .andExpect(requestTo(endpoint.withBase(baseUrl).getURI()))
                .andRespond(withBadRequest());

        // Then When
        assertThatExceptionOfType(UpdateServiceClientException.class)
                .isThrownBy(() -> updateServiceClient.createUpdate(update))
                .withCauseInstanceOf(HttpClientErrorException.class)
                .matches((e) -> e.getCause().getStatusCode().equals(HttpStatus.BAD_REQUEST));
        mockServer.verify();
    }

    @Test
    public void getUpdatesByStatusesAndSegments_whenServiceFails_shouldThrowClientException() {
        // Given
        final Endpoint endpoint = LIST_UPDATES_ENDPOINT;
        final String userId = UUID.randomUUID().toString();
        final Pageable pageable = new PageRequest(4, 15);
        mockServer.expect(method(endpoint.getMethod()))
                .andExpect(requestTo(endpoint.withBase(baseUrl).pageable(pageable).getURI(userId, null, null)))
                .andRespond(withBadRequest());

        // Then When
        assertThatExceptionOfType(UpdateServiceClientException.class)
                .isThrownBy(() -> updateServiceClient.getUpdatesByStatusesAndSegments(pageable, userId, Collections.emptyList(), Collections.emptyList()));
        mockServer.verify();
    }

    @Test
    public void getUpdatesByStatusesAndSegments_whenSuccessful_shouldReturnAListOfUpdates() throws Exception {
        // Given
        final Endpoint endpoint = LIST_UPDATES_ENDPOINT;
        final String userId = UUID.randomUUID().toString();
        final List<String> segments = Arrays.asList(UUID.randomUUID().toString(), UUID.randomUUID().toString());
        final List<UpdateStatus> statuses = Arrays.asList(UpdateStatus.DRAFT, UpdateStatus.PUBLISHED);
        final Pageable pageable = new PageRequest(0, 10);
        mockServer.expect(method(endpoint.getMethod()))
                .andExpect(requestTo(endpoint.withBase(baseUrl).pageable(pageable).getURI(
                        userId,
                        String.join(",", statuses.stream().map(UpdateStatus::getName).collect(Collectors.toList())),
                        String.join(",", segments)))
                )
                .andRespond(withSuccess().body(updates));

        // When
        final PagedResources<Update> result = updateServiceClient.getUpdatesByStatusesAndSegments(pageable, userId, statuses, segments);

        // Then
        mockServer.verify();
        assertThat(result).isNotEmpty();
    }

    @Test
    public void getUpdateByUuidAndUserId_whenServiceReturn404_shouldReturnNullObject() {
        // Given
        final Endpoint endpoint = GET_UPDATE_ENDPOINT;
        final String uuid = UUID.randomUUID().toString();
        final String userId = UUID.randomUUID().toString();
        mockServer.expect(method(endpoint.getMethod()))
                .andExpect(requestTo(endpoint.withBase(baseUrl).getURI(uuid, userId)))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        // When
        assertThatExceptionOfType(UpdateServiceClientException.class)
                .isThrownBy(() -> updateServiceClient.getUpdateByUuidAndUserId(uuid, userId));
        mockServer.verify();
    }

    @Test
    public void getUpdateByUuidAndUserId_whenServiceThrowsAnException_ShouldThrowAnExceptionToo() {
        // Given
        final Endpoint endpoint = GET_UPDATE_ENDPOINT;
        final String uuid = UUID.randomUUID().toString();
        final String userId = UUID.randomUUID().toString();
        mockServer.expect(method(endpoint.getMethod()))
                .andExpect(requestTo(endpoint.withBase(baseUrl).getURI(uuid, userId)))
                .andRespond(withServerError());

        // When
        assertThatExceptionOfType(UpdateServiceClientException.class)
                .isThrownBy(() -> updateServiceClient.getUpdateByUuidAndUserId(uuid, userId));
        mockServer.verify();
    }

    @Test
    public void getUpdateByUuidAndUserId_whenServiceReturnAnUpdate_shouldReturnItToo() throws Exception {
        // Given
        final Endpoint endpoint = GET_UPDATE_ENDPOINT;
        final String uuid = UUID.randomUUID().toString();
        final String userId = UUID.randomUUID().toString();
        mockServer.expect(method(endpoint.getMethod()))
                .andExpect(requestTo(endpoint.withBase(baseUrl).getURI(uuid, userId)))
                .andRespond(withSuccess().body(update));

        // When
        final Update updateInfo = updateServiceClient.getUpdateByUuidAndUserId(uuid, userId);

        // Then
        mockServer.verify();
        assertThat(updateInfo).isEqualTo(mapper.readValue(update.getInputStream(), Update.class));
    }

    @Test
    public void editUpdate_whenEditSucceedAndUpdateContainsProperties_shouldReturnAnUpdatedUpdate() throws Exception {
        // Given
        final Endpoint endpoint = EDIT_UPDATE_ENDPOINT;
        final Update toEdit = UpdateUtils.getUpdate();
        mockServer.expect(method(endpoint.getMethod()))
                .andExpect(requestTo(endpoint.withBase(baseUrl).getURI(toEdit.getUuid(), toEdit.getUserId())))
                .andExpect(content().string(mapper.writeValueAsString(toEdit)))
                .andRespond(withSuccess().body(update));

        // When
        final Update result = updateServiceClient.editUpdate(toEdit);

        // Then
        mockServer.verify();
        assertThat(result).isEqualTo(mapper.readValue(update.getInputStream(), Update.class));
    }

    @Test
    public void editUpdate_whenEditFails_shouldThrowAnException() throws Exception {
        // Given
        final Endpoint endpoint = EDIT_UPDATE_ENDPOINT;
        final Update toEdit = UpdateUtils.getUpdate();

        mockServer.expect(method(endpoint.getMethod()))
                .andExpect(requestTo(endpoint.withBase(baseUrl).getURI(toEdit.getUuid(), toEdit.getUserId())))
                .andRespond(withBadRequest());
        // When / Then
        assertThatExceptionOfType(UpdateServiceClientException.class)
                .isThrownBy(() -> updateServiceClient.editUpdate(toEdit))
                .withCauseInstanceOf(HttpClientErrorException.class)
                .matches((e) -> e.getCause().getStatusCode().equals(HttpStatus.BAD_REQUEST));
        mockServer.verify();
    }

    @Test
    public void getLatestUpdateBySegmentId_whenSuccessful_shouldReturnUpdate() throws Exception {
        // Given
        final Endpoint endpoint = GET_LATEST_UPDATE_FOR_SEGMENT_ENDPOINT;
        final String userId = UUID.randomUUID().toString();
        final String segmentId = UUID.randomUUID().toString();
        final Update expected = mapper.readValue(update.getInputStream(), Update.class);
        mockServer.expect(requestTo(endpoint.withBase(baseUrl).getURI(userId, segmentId)))
                .andExpect(method(endpoint.getMethod()))
                .andRespond(withSuccess().body(update));

        // When
        final Optional<Update> result = updateServiceClient.getLatestUpdateForSegment(userId, segmentId);

        // Then
        mockServer.verify();
        assertThat(result).hasValue(expected);
    }

    @Test
    public void getLatestUpdateBySegmentId_whenNoUpdate_shouldReturnEmpty() throws Exception {
        // Given
        final Endpoint endpoint = GET_LATEST_UPDATE_FOR_SEGMENT_ENDPOINT;
        final String userId = UUID.randomUUID().toString();
        final String segmentId = UUID.randomUUID().toString();
        mockServer.expect(requestTo(endpoint.withBase(baseUrl).getURI(userId, segmentId)))
                .andExpect(method(endpoint.getMethod()))
                .andRespond(withNoContent());

        // When
        final Optional<Update> result = updateServiceClient.getLatestUpdateForSegment(userId, segmentId);

        // Then
        mockServer.verify();
        assertThat(result).isEmpty();
    }

    @Test
    public void getLatestUpdateBySegmentId_whenError_shouldThrowException() throws Exception {
        // Given
        final Endpoint endpoint = GET_LATEST_UPDATE_FOR_SEGMENT_ENDPOINT;
        final String userId = UUID.randomUUID().toString();
        final String segmentId = UUID.randomUUID().toString();
        mockServer.expect(requestTo(endpoint.withBase(baseUrl).getURI(userId, segmentId)))
                .andExpect(method(endpoint.getMethod()))
                .andRespond(withServerError());

        // Then / When
        assertThatExceptionOfType(UpdateServiceClientException.class)
                .isThrownBy(() -> updateServiceClient.getLatestUpdateForSegment(userId, segmentId));
        mockServer.verify();
    }

    @Test
    public void getAllStatusesCompatibilities_whenUpdateServiceReturnBadRequest_shouldThrowUpdateServiceClientException() {
        // Given
        final Endpoint endpoint = GET_STATUSES_ENDPOINT;
        mockServer.expect(method(endpoint.getMethod()))
                .andExpect(requestTo(endpoint.withBase(baseUrl).getURI()))
                .andRespond(withBadRequest());

        // When / Then
        assertThatExceptionOfType(UpdateServiceClientException.class)
                .isThrownBy(() -> updateServiceClient.getAllStatusesCompatibilities())
                .withCauseInstanceOf(HttpClientErrorException.class)
                .matches((e) -> e.getCause().getStatusCode().equals(HttpStatus.BAD_REQUEST));

        mockServer.verify();
    }

    @Test
    public void getAllStatusesCompatibilities_whenUpdateServiceReturnListOfCompatibilities_shouldReturnThatList() throws Exception {
        // Given
        final Endpoint endpoint = GET_STATUSES_ENDPOINT;
        mockServer.expect(method(endpoint.getMethod()))
                .andExpect(requestTo(endpoint.withBase(baseUrl).getURI()))
                .andRespond(withSuccess().body(statuses));

        // When
        final List<UpdateStatusCompatibility> result = updateServiceClient.getAllStatusesCompatibilities();

        // Then
        mockServer.verify();
        assertThat(result).isNotEmpty().isEqualTo(mapper.readValue(statuses.getInputStream(), new TypeReference<List<UpdateStatusCompatibility>>() {
        }));
    }

    @Test
    public void getStatusCompatibilities_whenUpdateServiceReturnBadRequest_shouldThrowUpdateServiceClientException() {
        // Given
        final Endpoint endpoint = GET_STATUS_ENDPOINT;
        final UpdateStatus status = UpdateStatus.ARCHIVED;
        mockServer.expect(method(endpoint.getMethod()))
                .andExpect(requestTo(endpoint.withBase(baseUrl).getURI(status.getName())))
                .andRespond(withBadRequest());

        // When / Then
        assertThatExceptionOfType(UpdateServiceClientException.class)
                .isThrownBy(() -> updateServiceClient.getStatusCompatibilities(status))
                .withCauseInstanceOf(HttpClientErrorException.class)
                .matches((e) -> e.getCause().getStatusCode().equals(HttpStatus.BAD_REQUEST));

        mockServer.verify();
    }

    @Test
    public void getStatusCompatibilities_whenUpdateServiceReturnCompatibilityObject_shouldReturnThatObject() throws Exception {
        // Given
        final Endpoint endpoint = GET_STATUS_ENDPOINT;
        final UpdateStatus draft = UpdateStatus.DRAFT;
        mockServer.expect(method(endpoint.getMethod()))
                .andExpect(requestTo(endpoint.withBase(baseUrl).getURI(draft.getName())))
                .andRespond(withSuccess().body(status));

        // When
        final UpdateStatusCompatibility result = updateServiceClient.getStatusCompatibilities(draft);

        // Then
        mockServer.verify();
        assertThat(result).isEqualToComparingFieldByFieldRecursively(mapper.readValue(status.getInputStream(), UpdateStatusCompatibility.class));
    }
}
