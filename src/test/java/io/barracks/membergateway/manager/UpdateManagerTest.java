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

package io.barracks.membergateway.manager;

import io.barracks.commons.test.PagedResourcesUtils;
import io.barracks.membergateway.client.PackageServiceClient;
import io.barracks.membergateway.client.UpdateServiceClient;
import io.barracks.membergateway.exception.InvalidOwnerException;
import io.barracks.membergateway.model.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.*;

import static io.barracks.membergateway.utils.PackageUtils.getPredefinedCreatedPackageInfoBuilder;
import static io.barracks.membergateway.utils.SegmentUtils.getPredefinedSegmentBuilder;
import static io.barracks.membergateway.utils.UpdateUtils.getPredefinedCreateUpdateRequestBuilder;
import static io.barracks.membergateway.utils.UpdateUtils.getPredefinedCreatedUpdateBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class UpdateManagerTest {

    @Mock
    private UpdateServiceClient updateServiceClient;

    @Mock
    private PackageServiceClient packageServiceClient;

    @Mock
    private SegmentManager segmentManager;

    private UpdateManager updateManager;


    @Before
    public void setUp() {
        updateManager = spy(new UpdateManager(updateServiceClient, packageServiceClient, segmentManager));
    }

    @Test
    public void getUpdatesByStatusesAndSegments_whenUpdateServiceClientReturnsUpdates_shouldReturnThemWithPackageAndSegmentInformation() {
        // Given
        final Pageable pageable = new PageRequest(0, 20);
        final String userId = UUID.randomUUID().toString();
        final List<UpdateStatus> statuses = Collections.emptyList();

        final Segment segment = getPredefinedSegmentBuilder(userId).build();
        final PackageInfo publishedUpdatePackage = getPredefinedCreatedPackageInfoBuilder(userId).build();
        final Update publishedUpdate = getPredefinedCreatedUpdateBuilder(userId)
                .status(UpdateStatus.PUBLISHED)
                .segmentId(segment.getId())
                .packageId(publishedUpdatePackage.getId())
                .build();

        final Segment scheduledUpdateSegment = getPredefinedSegmentBuilder(userId).build();
        final PackageInfo scheduledUpdatePackage = getPredefinedCreatedPackageInfoBuilder(userId).build();
        final Update scheduledUpdate = getPredefinedCreatedUpdateBuilder(userId)
                .status(UpdateStatus.SCHEDULED)
                .scheduledDate(new Date())
                .packageId(scheduledUpdatePackage.getId())
                .segmentId(scheduledUpdateSegment.getId())
                .build();

        when(updateServiceClient.getUpdatesByStatusesAndSegments(pageable, userId, statuses, Collections.emptyList()))
                .thenReturn(PagedResourcesUtils.buildPagedResources(pageable, Arrays.asList(publishedUpdate, scheduledUpdate)));

        when(packageServiceClient.getPackageInfo(publishedUpdate.getPackageId()))
                .thenReturn(publishedUpdatePackage);
        when(packageServiceClient.getPackageInfo(scheduledUpdate.getPackageId()))
                .thenReturn(scheduledUpdatePackage);

        doReturn(segment).when(updateManager).getSegmentForUpdate(publishedUpdate);
        doReturn(scheduledUpdateSegment).when(updateManager).getSegmentForUpdate(scheduledUpdate);

        // When
        final Page<DetailedUpdate> result = updateManager.getUpdatesByStatusesAndSegments(pageable, userId, statuses, Collections.emptyList());

        // Then
        verify(updateServiceClient).getUpdatesByStatusesAndSegments(pageable, userId, Collections.emptyList(), Collections.emptyList());
        verify(packageServiceClient).getPackageInfo(publishedUpdatePackage.getId());
        verify(packageServiceClient).getPackageInfo(scheduledUpdatePackage.getId());
        verify(updateManager).getSegmentForUpdate(publishedUpdate);
        verify(updateManager).getSegmentForUpdate(scheduledUpdate);

        assertThat(result).contains(
                new DetailedUpdate(publishedUpdate, publishedUpdatePackage, segment),
                new DetailedUpdate(scheduledUpdate, scheduledUpdatePackage, scheduledUpdateSegment)
        );
    }

    @Test
    public void getUpdateByUuidAndUserId_whenUpdateHasSegment_shouldReturnUpdateWithSegment() {
        // Given
        final String userId = UUID.randomUUID().toString();
        final Segment updateSegment = getPredefinedSegmentBuilder(userId).build();
        final PackageInfo updatePackage = getPredefinedCreatedPackageInfoBuilder(userId).build();
        final Update update = getPredefinedCreatedUpdateBuilder(userId)
                .status(UpdateStatus.PUBLISHED)
                .segmentId(updateSegment.getId())
                .packageId(updatePackage.getId())
                .build();

        doReturn(update).when(updateServiceClient).getUpdateByUuidAndUserId(update.getUuid(), userId);
        doReturn(updatePackage).when(packageServiceClient).getPackageInfo(updatePackage.getId());
        doReturn(updateSegment).when(updateManager).getSegmentForUpdate(update);

        // When
        final DetailedUpdate result = updateManager.getUpdateByUuidAndUserId(update.getUuid(), userId);

        // Then
        verify(updateServiceClient).getUpdateByUuidAndUserId(update.getUuid(), userId);
        verify(packageServiceClient).getPackageInfo(updatePackage.getId());
        verify(updateManager).getSegmentForUpdate(update);
        assertThat(result).isEqualTo(new DetailedUpdate(update, updatePackage, updateSegment));
    }

    @Test
    public void createUpdate_shouldNormalizeCheckPackageAndSegmentOwnership_shouldReturnResponseFromClient() {
        // Given
        final String userId = UUID.randomUUID().toString();
        final Segment segment = getPredefinedSegmentBuilder(userId).build();
        final PackageInfo updatePackage = getPredefinedCreatedPackageInfoBuilder(userId).build();
        final Update update = getPredefinedCreateUpdateRequestBuilder()
                .segmentId(segment.getId())
                .packageId(updatePackage.getId())
                .userId(userId)
                .build();
        final Update updateAfterCreation = update.toBuilder()
                .revisionId(1)
                .creationDate(new Date())
                .uuid(UUID.randomUUID().toString())
                .status(UpdateStatus.DRAFT)
                .build();

        doReturn(update).when(updateManager).normalizeUpdate(update);
        doReturn(updatePackage).when(updateManager).checkPackageInfoOwnershipOnUpdate(update);
        doReturn(segment).when(updateManager).getSegmentForUpdate(update);
        doReturn(updateAfterCreation).when(updateServiceClient).createUpdate(update);

        // When
        final DetailedUpdate result = updateManager.createUpdate(update);

        // Then
        verify(updateManager).normalizeUpdate(update);
        verify(updateManager).checkPackageInfoOwnershipOnUpdate(update);
        verify(updateManager).getSegmentForUpdate(update);
        verify(updateServiceClient).createUpdate(update);
        assertThat(result).isEqualTo(
                new DetailedUpdate(
                        updateAfterCreation,
                        updatePackage,
                        segment
                )
        );
    }

    @Test
    public void editUpdate_shouldNormalizeCheckUpdateAndPackageAndSegmentOwnership_shouldReturnResponseFromClient() {
        // Given
        final String userId = UUID.randomUUID().toString();
        final Segment segment = getPredefinedSegmentBuilder("anotherId").build();
        final PackageInfo updatePackage = getPredefinedCreatedPackageInfoBuilder(userId).build();
        final Update update = getPredefinedCreatedUpdateBuilder(userId)
                .segmentId(segment.getId())
                .packageId(updatePackage.getId())
                .build();

        doReturn(update).when(updateManager).normalizeUpdate(update);
        doReturn(null).when(updateManager).checkUpdateOwnership(update.getUuid(), userId);
        doReturn(updatePackage).when(updateManager).checkPackageInfoOwnershipOnUpdate(update);
        doReturn(segment).when(updateManager).getSegmentForUpdate(update);
        doReturn(update).when(updateServiceClient).editUpdate(update);

        // When
        final DetailedUpdate result = updateManager.editUpdate(update);

        // Then;
        verify(updateManager).normalizeUpdate(update);
        verify(updateManager).checkUpdateOwnership(update.getUuid(), userId);
        verify(updateManager).checkPackageInfoOwnershipOnUpdate(update);
        verify(updateManager).getSegmentForUpdate(update);
        verify(updateServiceClient).editUpdate(update);
        assertThat(result).isEqualTo(
                new DetailedUpdate(
                        update,
                        updatePackage,
                        segment
                )
        );
    }

    @Test
    public void changeUpdateStatus_withScheduledTime_shouldUpdateExistingUpdateWithTimeAndStatus() {
        // Given
        final String userId = UUID.randomUUID().toString();
        final UpdateStatus newStatus = UpdateStatus.ARCHIVED;
        final Update update = getPredefinedCreatedUpdateBuilder(userId).build();
        final OffsetDateTime time = OffsetDateTime.ofInstant(Instant.ofEpochMilli(1234567890L), ZoneId.of("UTC"));
        final Update expected = update.toBuilder().status(newStatus).scheduledDate(Date.from(time.toInstant())).build();
        doReturn(update).when(updateManager).checkUpdateOwnership(update.getUuid(), userId);

        // When
        updateManager.changeUpdateStatus(update.getUuid(), newStatus, Optional.of(time), userId);

        // Then
        verify(updateManager).checkUpdateOwnership(update.getUuid(), userId);
        verify(updateServiceClient).editUpdate(expected);
    }

    @Test
    public void changeUpdateStatus_withNoScheduledTime_shouldUpdateExistingUpdateWithNewStatus() {
        // Given
        final String userId = UUID.randomUUID().toString();
        final UpdateStatus newStatus = UpdateStatus.ARCHIVED;
        final OffsetDateTime time = OffsetDateTime.ofInstant(Instant.ofEpochMilli(1234567890L), ZoneId.of("UTC"));
        final Update update = getPredefinedCreatedUpdateBuilder(userId).scheduledDate(Date.from(time.toInstant())).build();
        final Update expected = update.toBuilder().status(newStatus).build();
        doReturn(update).when(updateManager).checkUpdateOwnership(update.getUuid(), userId);

        // When
        updateManager.changeUpdateStatus(update.getUuid(), newStatus, Optional.empty(), userId);

        // Then
        verify(updateManager).checkUpdateOwnership(update.getUuid(), userId);
        verify(updateServiceClient).editUpdate(expected);
    }

    @Test
    public void scheduleUpdatePublication_shouldUpdateUpdateAsScheduledWithTime() {
        // Given
        final String userId = UUID.randomUUID().toString();
        final String uuid = UUID.randomUUID().toString();
        final OffsetDateTime time = OffsetDateTime.ofInstant(Instant.ofEpochMilli(1234567890L), ZoneId.of("UTC"));
        doNothing().when(updateManager).changeUpdateStatus(uuid, UpdateStatus.SCHEDULED, Optional.of(time), userId);

        // When
        updateManager.scheduleUpdatePublication(uuid, time, userId);

        // Then
        verify(updateManager).changeUpdateStatus(uuid, UpdateStatus.SCHEDULED, Optional.of(time), userId);
    }

    @Test
    public void changeUpdateStatus_shouldUpdateStatusWithNoTime() {
        // Given
        final String userId = UUID.randomUUID().toString();
        final String uuid = UUID.randomUUID().toString();
        final UpdateStatus status = UpdateStatus.PUBLISHED;
        doNothing().when(updateManager).changeUpdateStatus(uuid, status, Optional.empty(), userId);

        // When
        updateManager.changeUpdateStatus(uuid, status, userId);

        // Then
        verify(updateManager).changeUpdateStatus(uuid, status, Optional.empty(), userId);
    }

    @Test
    public void getAllStatusesCompatibilities_whenUpdateClientReturnCompatibilityList_shouldReturnThatList() {
        // Given
        final UpdateStatusCompatibility draftCompatibility = new UpdateStatusCompatibility(
                UpdateStatus.DRAFT,
                Collections.singletonList(UpdateStatus.DRAFT)
        );
        final UpdateStatusCompatibility publishedCompatibility = new UpdateStatusCompatibility(
                UpdateStatus.PUBLISHED,
                Arrays.asList(UpdateStatus.DRAFT, UpdateStatus.PUBLISHED)
        );
        final List<UpdateStatusCompatibility> compatibilities = Arrays.asList(draftCompatibility, publishedCompatibility);

        when(updateServiceClient.getAllStatusesCompatibilities()).thenReturn(compatibilities);

        // When
        final List<UpdateStatusCompatibility> managerResponse = updateManager.getAllStatusesCompatibilities();

        // Then
        assertThat(managerResponse).containsExactlyElementsOf(compatibilities);
        verify(updateServiceClient).getAllStatusesCompatibilities();
    }

    @Test
    public void getStatusCompatibilities_whenUpdateClientReturnCompatibilityObject_shouldReturnThatObject() {
        // Given
        final UpdateStatus status = UpdateStatus.PUBLISHED;
        final UpdateStatusCompatibility publishedCompatibility = new UpdateStatusCompatibility(
                status,
                Arrays.asList(UpdateStatus.DRAFT, UpdateStatus.PUBLISHED, UpdateStatus.SCHEDULED, UpdateStatus.ARCHIVED)
        );

        when(updateServiceClient.getStatusCompatibilities(status)).thenReturn(publishedCompatibility);

        // When
        final UpdateStatusCompatibility managerResponse = updateManager.getStatusCompatibilities(status);

        // Then
        assertThat(managerResponse).isEqualTo(publishedCompatibility);
        verify(updateServiceClient).getStatusCompatibilities(status);
    }

    @Test
    public void getSegmentForUpdate_whenUpdateHasSegment_shouldCallManagerAndReturnSegment() {
        // Given
        final String userId = UUID.randomUUID().toString();
        final String segmentId = UUID.randomUUID().toString();
        final Update update = getPredefinedCreatedUpdateBuilder(userId).segmentId(segmentId).build();
        final Segment segment = Segment.builder().id(UUID.randomUUID().toString()).build();
        doReturn(segment).when(segmentManager).getSegmentForUser(userId, segmentId);

        // When
        final Segment result = updateManager.getSegmentForUpdate(update);

        // Then
        verify(segmentManager).getSegmentForUser(userId, segmentId);
        assertThat(result).isEqualTo(segment);
    }

    @Test
    public void getSegmentForUpdate_whenUpdateHasNoSegment_shouldReturnOtherSegment() {
        // Given
        final String userId = UUID.randomUUID().toString();
        final Update update = getPredefinedCreatedUpdateBuilder(userId).segmentId(null).build();
        final Segment segment = Segment.builder().id(UUID.randomUUID().toString()).build();
        doReturn(segment).when(segmentManager).getOtherSegment(userId);

        // When
        final Segment result = updateManager.getSegmentForUpdate(update);

        // Then
        verify(segmentManager).getOtherSegment(userId);
        assertThat(result).isEqualTo(segment);
    }

    @Test
    public void normalizeUpdate_whenUpdateHasSegmentId_shouldReturnOriginal() {
        // Given
        final Update update = Update.builder().uuid(UUID.randomUUID().toString())
                .segmentId(UUID.randomUUID().toString())
                .build();

        // When
        final Update result = updateManager.normalizeUpdate(update);

        // Then
        assertThat(result).isEqualTo(update);
    }

    @Test
    public void normalizeUpdate_whenUpdateHasOtherAsId_shouldRemoveId() {
        // Given
        final Update update = Update.builder().uuid(UUID.randomUUID().toString())
                .segmentId("other")
                .build();
        final Update expected = update.toBuilder().segmentId(null).build();

        // When
        final Update result = updateManager.normalizeUpdate(update);

        // Then
        assertThat(result).isEqualTo(expected);
    }

    @Test
    public void checkPackageInfoOwnershipOnUpdate_whenUserOwnsPackage_shouldReturnPackageInfo() {
        // Given
        final String userId = UUID.randomUUID().toString();
        final String packageId = UUID.randomUUID().toString();
        final Update update = Update.builder().userId(userId).packageId(packageId).build();
        final PackageInfo expected = PackageInfo.builder().id(UUID.randomUUID().toString()).userId(userId).build();
        doReturn(expected).when(packageServiceClient).getPackageInfo(packageId);

        // When
        final PackageInfo result = updateManager.checkPackageInfoOwnershipOnUpdate(update);

        // Then
        verify(packageServiceClient).getPackageInfo(packageId);
        assertThat(result).isEqualTo(expected);
    }

    @Test
    public void checkPackageInfoOwnershipOnUpdate_whenUserDoesNotOwnsPackage_shouldThrowException() {
        // Given
        final String userId = UUID.randomUUID().toString();
        final String packageId = UUID.randomUUID().toString();
        final Update update = Update.builder().userId(userId).packageId(packageId).build();
        final PackageInfo expected = PackageInfo.builder().id(UUID.randomUUID().toString()).userId(UUID.randomUUID().toString()).build();
        doReturn(expected).when(packageServiceClient).getPackageInfo(packageId);

        // Then When
        assertThatExceptionOfType(InvalidOwnerException.class).isThrownBy(() ->
                updateManager.checkPackageInfoOwnershipOnUpdate(update)
        );
        verify(packageServiceClient).getPackageInfo(packageId);
    }

    @Test
    public void checkUpdateOwnership_shouldCallClientWithUserIdAndUuid_andReturnResult() {
        // Given
        final String userId = UUID.randomUUID().toString();
        final String uuid = UUID.randomUUID().toString();
        final Update expected = Update.builder().uuid(UUID.randomUUID().toString()).build();
        doReturn(expected).when(updateServiceClient).getUpdateByUuidAndUserId(uuid, userId);

        // When
        final Update result = updateManager.checkUpdateOwnership(uuid, userId);

        // Then
        verify(updateServiceClient).getUpdateByUuidAndUserId(uuid, userId);
        assertThat(result).isEqualTo(expected);
    }
}
