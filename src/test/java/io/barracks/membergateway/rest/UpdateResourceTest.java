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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;
import io.barracks.membergateway.client.exception.UpdateServiceClientException;
import io.barracks.membergateway.exception.InvalidOwnerException;
import io.barracks.membergateway.manager.UpdateManager;
import io.barracks.membergateway.model.*;
import io.barracks.membergateway.utils.RandomPrincipal;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.client.HttpServerErrorException;

import java.net.URI;
import java.nio.charset.Charset;
import java.security.Principal;
import java.text.SimpleDateFormat;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import static io.barracks.membergateway.utils.UpdateUtils.getPredefinedCreateUpdateRequestBuilder;
import static io.barracks.membergateway.utils.UpdateUtils.getPredefinedCreatedUpdateBuilder;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@RunWith(SpringRunner.class)
@BarracksResourceTest(controllers = UpdateResource.class)
public class UpdateResourceTest {
    @Autowired
    private ObjectMapper mapper;
    @Autowired
    private MockMvc mvc;
    @MockBean
    private UpdateManager updateManager;
    private Principal principal = new RandomPrincipal();

    @Test
    public void createUpdate_whenRequestIsMissingPackageId_shouldReturn400Code() throws Exception {
        // Given
        final Update requestBody = getPredefinedCreateUpdateRequestBuilder()
                .packageId(null)
                .build();
        final String jsonRequestBody = mapper.writeValueAsString(requestBody);

        // When
        final ResultActions result = mvc.perform(
                MockMvcRequestBuilders.post("/updates/")
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .content(jsonRequestBody)
                        .principal(principal)
        );

        // Then
        result.andExpect(status().isBadRequest());
    }

    @Test
    public void createUpdate_whenRequestPackageIdIsBlank_shouldReturn400Code() throws Exception {
        // Given
        final Update requestBody = getPredefinedCreateUpdateRequestBuilder()
                .packageId("")
                .build();
        final String jsonRequestBody = mapper.writeValueAsString(requestBody);

        // When
        final ResultActions result = mvc.perform(
                MockMvcRequestBuilders.post("/updates/")
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .content(jsonRequestBody)
                        .principal(principal)
        );

        // Then
        result.andExpect(status().isBadRequest());
    }

    @Test
    public void createUpdate_whenRequestIsMissingSegmentId_shouldReturnCreated() throws Exception {
        // Given
        final Update requestBody = getPredefinedCreateUpdateRequestBuilder()
                .segmentId(null)
                .build();
        final String jsonRequestBody = mapper.writeValueAsString(requestBody);

        // When
        final ResultActions result = mvc.perform(
                MockMvcRequestBuilders.post("/updates/")
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .content(jsonRequestBody)
                        .principal(principal)
        );

        // Then
        result.andExpect(status().isCreated());
    }

    @Test
    public void createUpdate_whenRequestContentIsValidWithoutProperties_shouldReturn201Code() throws Exception {
        // Given
        final String segmentId = UUID.randomUUID().toString();
        final Segment segment = Segment.builder().id(segmentId).userId(principal.getName()).name("prod").query(NullNode.getInstance()).build();
        final Update requestBody = getPredefinedCreateUpdateRequestBuilder().build();
        final Update updateWithUserId = requestBody.toBuilder().userId(principal.getName()).build();
        final Update serviceUpdate = getPredefinedCreatedUpdateBuilder(principal.getName())
                .userId(principal.getName())
                .build();
        final PackageInfo packageInfo = new PackageInfo(UUID.randomUUID().toString(), principal.getName(), "file.dat", "md5hdiknfhf4", 2345L, "version");
        final DetailedUpdate serviceResponse = new DetailedUpdate(serviceUpdate, packageInfo, segment);
        final String jsonRequestBody = mapper.writeValueAsString(requestBody);

        when(updateManager.createUpdate(updateWithUserId)).thenReturn(serviceResponse);

        // When
        final ResultActions result = mvc.perform(
                MockMvcRequestBuilders.post("/updates")
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .content(jsonRequestBody)
                        .principal(principal)
        );

        // Then
        verify(updateManager).createUpdate(updateWithUserId);
        result.andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8));
        this.assertValidJSONUpdate(serviceResponse, result);
    }

    @Test
    public void createUpdate_whenRequestContentIsValidWithVariousProperties_shouldReturn201Code() throws Exception {
        // Given
        final String segmentId = UUID.randomUUID().toString();
        final String additionalPropertiesString = "coucou";
        final int additionalPropertiesInt = 123;
        final Segment segment = Segment.builder().id(segmentId).userId(principal.getName()).name("prod").query(NullNode.getInstance()).build();
        final Map<String, Object> properties = new HashMap<String, Object>() {
            {
                put("aString", additionalPropertiesString);
                put("anInteger", additionalPropertiesInt);
                put("anArray", Arrays.asList("This", "is", "an", "array"));
            }
        };

        final Update requestBody = getPredefinedCreateUpdateRequestBuilder()
                .additionalProperties(new AdditionalProperties(properties))
                .build();

        final Update updateWithUserId = requestBody.toBuilder().userId(principal.getName()).build();

        final Update serviceUpdate = getPredefinedCreatedUpdateBuilder(principal.getName())
                .userId(principal.getName())
                .additionalProperties(new AdditionalProperties(properties))
                .status(UpdateStatus.DRAFT)
                .build();

        final PackageInfo packageInfo = new PackageInfo(UUID.randomUUID().toString(), principal.getName(), "file.dat", "md5hdiknfhf4", 2345L, "version");
        final DetailedUpdate serviceResponse = new DetailedUpdate(serviceUpdate, packageInfo, segment);
        final String jsonRequestBody = mapper.writeValueAsString(requestBody);
        when(updateManager.createUpdate(updateWithUserId)).thenReturn(serviceResponse);

        // When
        final ResultActions result = mvc.perform(
                MockMvcRequestBuilders.post("/updates")
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .content(jsonRequestBody)
                        .principal(principal)
        );

        // Then
        verify(updateManager).createUpdate(updateWithUserId);
        result.andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8));
        this.assertValidJSONUpdate(serviceResponse, result);
    }

    @Test
    public void createUpdate_whenManagerThrowServiceClientException_shouldReturnTheSameStatus() throws Exception {
        // Given
        final Update requestBody = getPredefinedCreateUpdateRequestBuilder().build();
        final Update updateWithUserId = requestBody.toBuilder().userId(principal.getName()).build();
        final String jsonRequestBody = mapper.writeValueAsString(requestBody);
        final String errorMessage = "coucou";
        final HttpServerErrorException httpException = new HttpServerErrorException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
                errorMessage.getBytes(Charset.defaultCharset()), Charset.defaultCharset());
        when(updateManager.createUpdate(updateWithUserId)).thenThrow(new UpdateServiceClientException(httpException));

        // When
        final ResultActions result = mvc.perform(
                MockMvcRequestBuilders.post("/updates")
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .content(jsonRequestBody)
                        .principal(principal)
        );

        // Then
        verify(updateManager).createUpdate(updateWithUserId);
        result.andExpect(status().isInternalServerError())
                .andExpect(content().json("{\"title\":\"" + HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase() + "\",\"status\":" + HttpStatus.INTERNAL_SERVER_ERROR.value() + ",\"detail\":\"" + errorMessage + "\"}"));
    }

    @Test
    public void createUpdate_whenTheNameIsEmpty_shouldReturn400Code() throws Exception {
        // Given
        final Update requestBody = getPredefinedCreateUpdateRequestBuilder()
                .name("")
                .build();
        final String jsonRequestBody = mapper.writeValueAsString(requestBody);

        // When
        final ResultActions result = mvc.perform(
                MockMvcRequestBuilders.post("/updates/")
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .content(jsonRequestBody)
                        .principal(principal)
        );

        // Then
        result.andExpect(status().isBadRequest());
    }

    @Test
    public void editUpdate_whenManagerThrowInvalidOwnerException_shouldThrowExceptionToo() throws Exception {
        // Given
        final String uuid = UUID.randomUUID().toString();
        final Update requestBody = getPredefinedCreateUpdateRequestBuilder().build();
        final Update updateTransformedByResource = requestBody.toBuilder()
                .userId(principal.getName())
                .uuid(uuid)
                .build();
        final String jsonRequestBody = mapper.writeValueAsString(requestBody);
        final InvalidOwnerException exception = new InvalidOwnerException("Error message");
        when(updateManager.editUpdate(updateTransformedByResource)).thenThrow(exception);

        // When
        final ResultActions result = mvc.perform(
                MockMvcRequestBuilders.put("/updates/" + uuid)
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .content(jsonRequestBody)
                        .principal(principal)
        );

        // Then
        verify(updateManager).editUpdate(updateTransformedByResource);
        result.andExpect(status().isForbidden());
    }

    @Test
    public void editUpdate_whenManagerReturnsAnUpdate_shouldReturnItToo() throws Exception {
        // Given
        final String uuid = UUID.randomUUID().toString();
        final String segmentId = UUID.randomUUID().toString();
        final Segment segment = Segment.builder().id(segmentId).userId(principal.getName()).name("prod").query(NullNode.getInstance()).build();
        final Update requestBody = getPredefinedCreateUpdateRequestBuilder().build();
        final Update updateTransformedByResource = requestBody.toBuilder()
                .userId(principal.getName())
                .uuid(uuid)
                .build();
        final Update managerUpdate = getPredefinedCreatedUpdateBuilder(principal.getName())
                .userId(principal.getName())
                .revisionId(2)
                .status(UpdateStatus.DRAFT)
                .build();
        final String jsonRequestBody = mapper.writeValueAsString(requestBody);

        final PackageInfo packageInfo = new PackageInfo(UUID.randomUUID().toString(), principal.getName(), "file.dat", "md5hdiknfhf4", 2345L, "version");
        final DetailedUpdate managerResponse = new DetailedUpdate(managerUpdate, packageInfo, segment);
        when(updateManager.editUpdate(updateTransformedByResource)).thenReturn(managerResponse);

        // When
        final ResultActions result = mvc.perform(
                MockMvcRequestBuilders.put("/updates/" + uuid)
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .content(jsonRequestBody)
                        .principal(principal)
        );

        // Then
        verify(updateManager).editUpdate(updateTransformedByResource);
        result.andExpect(status().isOk());
        this.assertValidJSONUpdate(managerResponse, result);
    }

    @Test
    public void changeUpdateStatus_whenManagerThrowUnknownUpdateStatusException_shouldThrowItToo() throws Exception {
        // Given
        final String uuid = UUID.randomUUID().toString();
        final String status = "coucou";

        // When
        final ResultActions result = mvc.perform(
                MockMvcRequestBuilders.put("/updates/" + uuid + "/status/" + status)
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .principal(principal)
        );

        // Then
        result.andExpect(status().isBadRequest());
    }

    @Test
    public void changeUpdateStatus_whenManagerReturnAnUpdate_shouldReturnItToo() throws Exception {
        // Given
        final String uuid = UUID.randomUUID().toString();
        final UpdateStatus status = UpdateStatus.PUBLISHED;

        // When
        final ResultActions result = mvc.perform(
                MockMvcRequestBuilders.put("/updates/{uuid}/status/{status}", uuid, status.getName())
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .principal(principal)
        );

        // Then
        verify(updateManager).changeUpdateStatus(uuid, status, principal.getName());
        result.andExpect(status().isOk());
    }

    @Test
    public void changeUpdateStatus_whenStatusIsScheduled_shouldCallManagerScheduleMethod() throws Exception {
        // Given
        final String uuid = UUID.randomUUID().toString();
        final UpdateStatus status = UpdateStatus.SCHEDULED;
        final OffsetDateTime scheduledTime = OffsetDateTime.now();

        // When
        final ResultActions result = mvc.perform(
                MockMvcRequestBuilders.put("/updates/{uuid}/status/{status}?time={time}", uuid, status.getName(), scheduledTime.format(DateTimeFormatter.ISO_DATE_TIME))
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .principal(principal)
        );

        // Then
        verify(updateManager).scheduleUpdatePublication(uuid, scheduledTime, principal.getName());
        result.andExpect(status().isOk());
    }

    @Test
    public void changeUpdateStatus_whenStatusIsScheduledAndTimeIsMissing_shouldReturnStatusCode400() throws Exception {
        // Given
        final String uuid = UUID.randomUUID().toString();
        final UpdateStatus status = UpdateStatus.SCHEDULED;

        // When
        final ResultActions result = mvc.perform(
                MockMvcRequestBuilders.put("/updates/{uuid}/status/{status}", uuid, status.getName())
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .principal(principal)
        );

        // Then
        verifyZeroInteractions(updateManager);
        result.andExpect(status().isBadRequest());
    }

    @Test
    public void getAllStatusesCompatibilities_whenUpdateManagerThrowUpdateServiceClientException_shouldReturnInternalServerError() throws Exception {
        // Given
        final String requestUrl = "/updates/status";
        when(updateManager.getAllStatusesCompatibilities())
                .thenThrow(new UpdateServiceClientException(
                        new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR)));

        // When
        final ResultActions result = mvc.perform(
                MockMvcRequestBuilders.get(requestUrl)
                        .principal(principal)
        );

        // Then
        verify(updateManager).getAllStatusesCompatibilities();
        result.andExpect(status().isInternalServerError());
    }

    @Test
    public void getAllStatusesCompatibilities_shouldReturnAllStatusesCompatibilities() throws Exception {
        // Given
        final String requestUrl = "/updates/status";
        final UpdateStatusCompatibility draftCompatibility = new UpdateStatusCompatibility(
                UpdateStatus.DRAFT,
                Collections.singletonList(UpdateStatus.DRAFT)
        );
        final UpdateStatusCompatibility publishedCompatibility = new UpdateStatusCompatibility(
                UpdateStatus.PUBLISHED,
                Arrays.asList(UpdateStatus.DRAFT, UpdateStatus.PUBLISHED)
        );
        final UpdateStatusCompatibility scheduledCompatibility = new UpdateStatusCompatibility(
                UpdateStatus.SCHEDULED,
                Arrays.asList(UpdateStatus.DRAFT, UpdateStatus.PUBLISHED, UpdateStatus.SCHEDULED)
        );
        final UpdateStatusCompatibility archivedCompatibility = new UpdateStatusCompatibility(
                UpdateStatus.ARCHIVED,
                Arrays.asList(UpdateStatus.DRAFT, UpdateStatus.PUBLISHED, UpdateStatus.SCHEDULED, UpdateStatus.ARCHIVED)
        );
        final List<UpdateStatusCompatibility> allCompatibilities = Arrays.asList(
                draftCompatibility,
                publishedCompatibility,
                scheduledCompatibility,
                archivedCompatibility
        );
        when(updateManager.getAllStatusesCompatibilities()).thenReturn(allCompatibilities);

        // When
        final ResultActions result = mvc.perform(
                MockMvcRequestBuilders.get(requestUrl)
                        .principal(principal)
        );

        // Then
        verify(updateManager).getAllStatusesCompatibilities();
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(4)))

                .andExpect(jsonPath("$[0].name").value(is(UpdateStatus.DRAFT.getName())))
                .andExpect(jsonPath("$[0].compatibleStatus", hasSize(1)))
                .andExpect(jsonPath("$[0].compatibleStatus[0]").value(is(UpdateStatus.DRAFT.getName())))

                .andExpect(jsonPath("$[1].name").value(is(UpdateStatus.PUBLISHED.getName())))
                .andExpect(jsonPath("$[1].compatibleStatus", hasSize(2)))
                .andExpect(jsonPath("$[1].compatibleStatus[0]").value(is(UpdateStatus.DRAFT.getName())))
                .andExpect(jsonPath("$[1].compatibleStatus[1]").value(is(UpdateStatus.PUBLISHED.getName())))

                .andExpect(jsonPath("$[2].name").value(is(UpdateStatus.SCHEDULED.getName())))
                .andExpect(jsonPath("$[2].compatibleStatus", hasSize(3)))
                .andExpect(jsonPath("$[2].compatibleStatus[0]").value(is(UpdateStatus.DRAFT.getName())))
                .andExpect(jsonPath("$[2].compatibleStatus[1]").value(is(UpdateStatus.PUBLISHED.getName())))
                .andExpect(jsonPath("$[2].compatibleStatus[2]").value(is(UpdateStatus.SCHEDULED.getName())))

                .andExpect(jsonPath("$[3].name").value(is(UpdateStatus.ARCHIVED.getName())))
                .andExpect(jsonPath("$[3].compatibleStatus", hasSize(4)))
                .andExpect(jsonPath("$[3].compatibleStatus[0]").value(is(UpdateStatus.DRAFT.getName())))
                .andExpect(jsonPath("$[3].compatibleStatus[1]").value(is(UpdateStatus.PUBLISHED.getName())))
                .andExpect(jsonPath("$[3].compatibleStatus[2]").value(is(UpdateStatus.SCHEDULED.getName())))
                .andExpect(jsonPath("$[3].compatibleStatus[3]").value(is(UpdateStatus.ARCHIVED.getName())));
    }

    @Test
    public void getStatusCompatibilities_whenUpdateManagerThrowUpdateServiceClientException_shouldReturnInternalServerError() throws Exception {
        // Given
        final UpdateStatus status = UpdateStatus.DRAFT;
        final String requestUrl = "/updates/status/" + status.getName();
        when(updateManager.getStatusCompatibilities(status))
                .thenThrow(new UpdateServiceClientException(
                        new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR)));

        // When
        final ResultActions result = mvc.perform(
                MockMvcRequestBuilders.get(requestUrl)
                        .principal(principal)
        );

        // Then
        verify(updateManager).getStatusCompatibilities(status);
        result.andExpect(status().isInternalServerError());
    }

    @Test
    public void getStatusCompatibilities_whenDraftStatusGiven_shouldReturnDraftWithCompatibilities() throws Exception {
        final UpdateStatus status = UpdateStatus.DRAFT;
        final UpdateStatusCompatibility draftCompatibility = new UpdateStatusCompatibility(status,
                Collections.singletonList(UpdateStatus.DRAFT)
        );
        getStatusCompatibilitiesTestHelper(status, draftCompatibility);
    }

    @Test
    public void getStatusCompatibilities_whenArchivedStatusGiven_shouldReturnArchivedWithCompatibilities() throws Exception {
        final UpdateStatus status = UpdateStatus.ARCHIVED;
        final UpdateStatusCompatibility archivedCompatibility = new UpdateStatusCompatibility(status,
                Arrays.asList(UpdateStatus.DRAFT, UpdateStatus.PUBLISHED, UpdateStatus.SCHEDULED, UpdateStatus.ARCHIVED)
        );
        getStatusCompatibilitiesTestHelper(status, archivedCompatibility);
    }

    @Test
    public void getStatusCompatibilities_whenPublishedStatusGiven_shouldReturnPublishedWithCompatibilities() throws Exception {
        final UpdateStatus status = UpdateStatus.PUBLISHED;
        final UpdateStatusCompatibility publishedCompatibility = new UpdateStatusCompatibility(status,
                Arrays.asList(UpdateStatus.DRAFT, UpdateStatus.PUBLISHED)
        );
        getStatusCompatibilitiesTestHelper(status, publishedCompatibility);
    }

    @Test
    public void getStatusCompatibilities_whenScheduledStatusGiven_shouldReturnScheduledWithCompatibilities() throws Exception {
        final UpdateStatus status = UpdateStatus.SCHEDULED;
        final UpdateStatusCompatibility scheduledCompatibility = new UpdateStatusCompatibility(status,
                Arrays.asList(UpdateStatus.DRAFT, UpdateStatus.PUBLISHED, UpdateStatus.SCHEDULED)
        );
        getStatusCompatibilitiesTestHelper(status, scheduledCompatibility);
    }

    private void getStatusCompatibilitiesTestHelper(UpdateStatus status, UpdateStatusCompatibility compatibilites) throws Exception {
        // Given
        final String requestUrl = "/updates/status/" + status.getName();
        final List<UpdateStatus> compatibleStatuses = compatibilites.getCompatibilities();
        when(updateManager.getStatusCompatibilities(status)).thenReturn(compatibilites);

        // When
        final ResultActions result = mvc.perform(
                MockMvcRequestBuilders.get(requestUrl)
                        .principal(principal)
        );

        // Then
        verify(updateManager).getStatusCompatibilities(status);
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(is(status.getName())))
                .andExpect(jsonPath("$.compatibleStatus", hasSize(compatibleStatuses.size())));
        for (int index = 0; index < compatibleStatuses.size(); ++index) {
            result.andExpect(jsonPath("$.compatibleStatus[" + index + "]").value(is(compatibleStatuses.get(index).getName())));
        }
    }

    @Test
    public void getStatus_whenUnknownStatusGiven_shouldReturn400Code() throws Exception {
        // Given
        final String requestUrl = "/updates/status/fwedszx";

        // When
        final ResultActions result = mvc.perform(
                MockMvcRequestBuilders.get(requestUrl)
                        .principal(principal)
        );

        // Then
        result.andExpect(status().isBadRequest());
    }

    @Test
    public void getAllUpdates_whenRequestIsValid_shouldReturn200CodeAndAListOfUpdates() throws Exception {
        // Given
        final Pageable pageable = new PageRequest(0, 20);
        final int totalElements = 2;
        final String packageId1 = UUID.randomUUID().toString();
        final String packageId2 = UUID.randomUUID().toString();
        final String segmentId1 = UUID.randomUUID().toString();
        final String segmentId2 = UUID.randomUUID().toString();
        final List<DetailedUpdate> listResponse = new ArrayList<>();
        final Update update1 = getPredefinedCreatedUpdateBuilder(principal.getName()).status(UpdateStatus.PUBLISHED).build();
        final Update update2 = getPredefinedCreatedUpdateBuilder(principal.getName()).build();
        final PackageInfo package1 = new PackageInfo(packageId1, principal.getName(), "file.dat", "md5hdiknfhf4", 2345L, "version");
        final PackageInfo package2 = new PackageInfo(packageId2, principal.getName(), "file2.dat", "md5hdikn222fhf4", 234225L, "version222");
        final Segment segment1 = Segment.builder().id(segmentId1).userId(principal.getName()).name("prod1").query(NullNode.getInstance()).build();
        final Segment segment2 = Segment.builder().id(segmentId2).userId(principal.getName()).name("prod2").query(NullNode.getInstance()).build();
        final DetailedUpdate expectedUpdate1 = new DetailedUpdate(update1, package1, segment1);
        final DetailedUpdate expectedUpdate2 = new DetailedUpdate(update2, package2, segment2);
        listResponse.add(expectedUpdate1);
        listResponse.add(expectedUpdate2);
        final Page<DetailedUpdate> managerResponse = this.getMockedPage(listResponse, pageable.getPageSize(), pageable.getPageNumber(), totalElements);
        when(updateManager.getUpdatesByStatusesAndSegments(pageable, principal.getName(), Collections.emptyList(), Collections.emptyList())).thenReturn(managerResponse);

        // When
        final ResultActions result = mvc.perform(
                MockMvcRequestBuilders.get("/updates").accept(MediaType.APPLICATION_JSON_UTF8).principal(principal)
        );

        // Then
        verify(updateManager).getUpdatesByStatusesAndSegments(pageable, principal.getName(), Collections.emptyList(), Collections.emptyList());
        result.andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("$._embedded.updates", hasSize(listResponse.size())));

        this.assertValidJSONUpdate(expectedUpdate1, result, "$._embedded.updates[0]");
        this.assertValidJSONUpdate(expectedUpdate2, result, "$._embedded.updates[1]");
    }

    @Test
    public void getAllUpdates_whenRequestWithParametersIsValid_shouldReturn200CodeAndAListOfDeviceEvent() throws Exception {
        // Given
        final Pageable pageable = new PageRequest(0, 20);
        final int totalElements = 2;
        final List<DetailedUpdate> listResponse = new ArrayList<>();
        final String packageId1 = UUID.randomUUID().toString();
        final String packageId2 = UUID.randomUUID().toString();
        final String segmentId1 = UUID.randomUUID().toString();
        final String segmentId2 = UUID.randomUUID().toString();
        final Update update1 = getPredefinedCreatedUpdateBuilder(principal.getName())
                .status(UpdateStatus.PUBLISHED)
                .packageId(packageId1)
                .segmentId(segmentId1)
                .build();
        final Update update2 = getPredefinedCreatedUpdateBuilder(principal.getName())
                .packageId(packageId2)
                .segmentId(segmentId2)
                .build();
        final PackageInfo package1 = new PackageInfo(packageId1, principal.getName(), "file.dat", "md5hdiknfhf4", 2345L, "version");
        final PackageInfo package2 = new PackageInfo(packageId2, principal.getName(), "file2.dat", "md5hdik22nfhf4", 234225L, "version22");
        final Segment segment1 = Segment.builder().id(segmentId1).userId(principal.getName()).name("prod1").query(NullNode.getInstance()).build();
        final Segment segment2 = Segment.builder().id(segmentId2).userId(principal.getName()).name("prod2").query(NullNode.getInstance()).build();
        final DetailedUpdate expectedUpdate1 = new DetailedUpdate(update1, package1, segment1);
        final DetailedUpdate expectedUpdate2 = new DetailedUpdate(update2, package2, segment2);

        listResponse.add(expectedUpdate1);
        listResponse.add(expectedUpdate2);

        final Page<DetailedUpdate> managerResponse = this.getMockedPage(listResponse, pageable.getPageSize(), pageable.getPageNumber(), totalElements);
        when(updateManager.getUpdatesByStatusesAndSegments(pageable, principal.getName(), Collections.emptyList(), Collections.emptyList())).thenReturn(managerResponse);

        // When
        final ResultActions result = mvc.perform(
                MockMvcRequestBuilders.get("/updates?page=" + pageable.getPageNumber() + "&size=" + pageable.getPageSize()).accept(MediaType.APPLICATION_JSON_UTF8).principal(principal)
        );

        // Then
        verify(updateManager).getUpdatesByStatusesAndSegments(pageable, principal.getName(), Collections.emptyList(), Collections.emptyList());
        result.andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("$._embedded.updates", hasSize(listResponse.size())));

        this.assertValidJSONUpdate(expectedUpdate1, result, "$._embedded.updates[0]");
        this.assertValidJSONUpdate(expectedUpdate2, result, "$._embedded.updates[1]");
    }

    @Test
    public void getAllUpdates_withNoStatus_shouldCallManagerWithEmptyList() throws Exception {
        // Given
        final Pageable pageable = new PageRequest(0, 20);

        // When
        mvc.perform(
                MockMvcRequestBuilders.get("/updates?page={pageNumber}&size={pageSize}", pageable.getPageNumber(), pageable.getPageSize())
                        .accept(MediaType.APPLICATION_JSON_UTF8).principal(principal)
        );

        // Then
        verify(updateManager).getUpdatesByStatusesAndSegments(pageable, principal.getName(), Collections.emptyList(), Collections.emptyList());
    }

    @Test
    public void getAllUpdates_withEmptyStatus_shouldCallManagerWithEmptyList() throws Exception {
        // Given
        final Pageable pageable = new PageRequest(0, 20);

        // When
        mvc.perform(
                MockMvcRequestBuilders.get("/updates?page={pageNumber}&size={pageSize}&status=", pageable.getPageNumber(), pageable.getPageSize())
                        .accept(MediaType.APPLICATION_JSON_UTF8).principal(principal)
        );

        // Then
        verify(updateManager).getUpdatesByStatusesAndSegments(pageable, principal.getName(), Collections.emptyList(), Collections.emptyList());
    }

    @Test
    public void getAllUpdates_withMultipleStatuses_shouldCallManagerWithList() throws Exception {
        // Given
        final Pageable pageable = new PageRequest(0, 20);
        final List<UpdateStatus> statuses = Arrays.asList(UpdateStatus.DRAFT, UpdateStatus.PUBLISHED);
        final String joinedStatuses = String.join(",", statuses.stream().map(UpdateStatus::getName).collect(Collectors.toList()));

        // When
        mvc.perform(
                MockMvcRequestBuilders.get("/updates?page={pageNumber}&size={pageSize}&status={statuses}", pageable.getPageNumber(), pageable.getPageSize(), joinedStatuses)
                        .accept(MediaType.APPLICATION_JSON_UTF8).principal(principal)
        );

        // Then
        verify(updateManager).getUpdatesByStatusesAndSegments(pageable, principal.getName(), statuses, Collections.emptyList());
    }

    @Test
    public void getUpdateByUuidAndUserId_whenUpdateManagerReturnEmptyResult_shouldReturn404() throws Exception {
        // Given
        final String uuid = UUID.randomUUID().toString();
        when(updateManager.getUpdateByUuidAndUserId(uuid, principal.getName())).thenThrow(new UpdateServiceClientException(new HttpServerErrorException(HttpStatus.NOT_FOUND)));

        // When
        final ResultActions result = mvc.perform(
                MockMvcRequestBuilders.get(URI.create("/updates/" + uuid)).principal(principal)
        );

        // Then
        verify(updateManager).getUpdateByUuidAndUserId(uuid, principal.getName());
        result.andExpect(status().isNotFound());
    }

    @Test
    public void getUpdateByUuidAndUserId_whenUpdateManagerReturnAnUpdate_shouldReturnItWithStatus200() throws Exception {
        // Given
        final String uuid = UUID.randomUUID().toString();
        final String packageId = UUID.randomUUID().toString();
        final String segmentId = UUID.randomUUID().toString();
        final Update resultUpdate = getPredefinedCreatedUpdateBuilder(principal.getName())
                .status(UpdateStatus.ARCHIVED)
                .segmentId(segmentId)
                .packageId(packageId)
                .uuid(uuid)
                .build();
        final PackageInfo packageInfo = new PackageInfo(packageId, principal.getName(), "file.txt", "md5", 123456789L, "version");
        final Segment segment = Segment.builder().id(segmentId).userId(principal.getName()).name("prod").query(NullNode.getInstance()).build();
        final DetailedUpdate managerResponse = new DetailedUpdate(resultUpdate, packageInfo, segment);
        when(updateManager.getUpdateByUuidAndUserId(uuid, principal.getName())).thenReturn(managerResponse);

        // When
        final ResultActions result = mvc.perform(
                MockMvcRequestBuilders.get(URI.create("/updates/" + uuid)).principal(principal)
        );

        // Then
        verify(updateManager).getUpdateByUuidAndUserId(uuid, principal.getName());
        result.andExpect(status().isOk());
        this.assertValidJSONUpdate(managerResponse, result);
    }

    private void assertValidJSONUpdate(DetailedUpdate expected, ResultActions actual) throws Exception {
        this.assertValidJSONUpdate(expected, actual, "$.");
    }

    private void assertValidJSONUpdate(DetailedUpdate expected, ResultActions actual, String pathPrefix) throws Exception {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

        actual
                .andExpect(jsonPath(pathPrefix + "uuid").value(expected.getUuid()))
                .andExpect(jsonPath(pathPrefix + "userId").value(expected.getUserId()))
                .andExpect(jsonPath(pathPrefix + "name").value(expected.getName()))
                .andExpect(jsonPath(pathPrefix + "description").value(expected.getDescription()))
                .andExpect(jsonPath(pathPrefix + "revisionId").value(expected.getRevisionId()))
                .andExpect(jsonPath(pathPrefix + "status").value(expected.getStatus().getName()))
                .andExpect(jsonPath(pathPrefix + "creationDate").value(expected.getCreationDate().map(dateFormat::format).orElse(null)))
                .andExpect(jsonPath(pathPrefix + "scheduledDate").value(expected.getScheduledDate().map(dateFormat::format).orElse(null)));

        if (!expected.getAdditionalProperties().isPresent()) {
            actual.andExpect(jsonPath(pathPrefix + "additionalProperties").isEmpty());
        } else {
            Map<String, Object> properties = expected.getAdditionalProperties().get().toMap();
            for (final Map.Entry<String, Object> entry : properties.entrySet()) {
                if (List.class.isAssignableFrom(entry.getValue().getClass())) {
                    actual.andExpect(jsonPath("$.additionalProperties." + entry.getKey()).isArray());
                } else {
                    actual.andExpect(jsonPath("$.additionalProperties." + entry.getKey()).value(entry.getValue()));
                }
            }
        }

        this.assertValidJSONSegment(expected.getSegment(), actual, pathPrefix);
        this.assertValidJSONPackage(expected.getPackageInfo(), actual, pathPrefix);
    }

    private void assertValidJSONSegment(Segment expected, ResultActions actual, String pathPrefix) throws Exception {
        actual
                .andExpect(jsonPath(pathPrefix + "segment.id").value(expected.getId()))
                .andExpect(jsonPath(pathPrefix + "segment.name").value(expected.getName()))
                .andExpect(jsonPath(pathPrefix + "segment.userId").value(expected.getUserId()))
                //.andExpect(jsonPath(pathPrefix + "segment.query").value(anyOf(NullNode.getInstance(), ))) TODO Handle the case of the query later
                .andExpect(jsonPath(pathPrefix + "segment.deviceCount").value(anyOf(equalTo(expected.getDeviceCount()), equalTo((int) expected.getDeviceCount()))));
    }

    private void assertValidJSONPackage(PackageInfo expected, ResultActions actual, String pathPrefix) throws Exception {
        actual
                .andExpect(jsonPath(pathPrefix + "packageInfo.id").value(expected.getId()))
                .andExpect(jsonPath(pathPrefix + "packageInfo.versionId").value(expected.getVersionId()))
                .andExpect(jsonPath(pathPrefix + "packageInfo.userId").value(expected.getUserId()))
                .andExpect(jsonPath(pathPrefix + "packageInfo.fileName").value(expected.getFileName()))
                .andExpect(jsonPath(pathPrefix + "packageInfo.md5").value(expected.getMd5()))
                .andExpect(jsonPath(pathPrefix + "packageInfo.size").value(anyOf(
                        equalTo(expected.getSize()),
                        equalTo((int) expected.getSize())
                )));
    }

    private <T> Page<T> getMockedPage(List<T> listResponse, int size, int page, int totalElements) {
        Pageable pageable = new PageRequest(page, size);
        return new PageImpl<>(listResponse, pageable, totalElements);
    }

}
