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

import io.barracks.membergateway.client.PackageServiceClient;
import io.barracks.membergateway.client.UpdateServiceClient;
import io.barracks.membergateway.exception.InvalidOwnerException;
import io.barracks.membergateway.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.PagedResources;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class UpdateManager {
    private final UpdateServiceClient updateServiceClient;
    private final PackageServiceClient packageServiceClient;
    private final SegmentManager segmentManager;

    @Autowired
    public UpdateManager(UpdateServiceClient updateServiceClient, PackageServiceClient packageServiceClient, SegmentManager segmentManager) {
        this.updateServiceClient = updateServiceClient;
        this.packageServiceClient = packageServiceClient;
        this.segmentManager = segmentManager;
    }

    public Page<DetailedUpdate> getUpdatesByStatusesAndSegments(Pageable pageable, String userId, List<UpdateStatus> statuses, List<String> segmentIds) {
        final PagedResources<Update> pagedResources = updateServiceClient.getUpdatesByStatusesAndSegments(pageable, userId, statuses, segmentIds);
        final List<DetailedUpdate> detailedUpdates = pagedResources.getContent().parallelStream().map(
                update -> new DetailedUpdate(
                        update,
                        packageServiceClient.getPackageInfo(update.getPackageId()),
                        getSegmentForUpdate(update)
                )
        ).collect(Collectors.toList());
        return new PageImpl<>(new ArrayList<>(detailedUpdates), pageable, pagedResources.getMetadata().getTotalElements());
    }

    public DetailedUpdate getUpdateByUuidAndUserId(String uuid, String userId) {
        final Update update = updateServiceClient.getUpdateByUuidAndUserId(uuid, userId);
        return new DetailedUpdate(
                update,
                packageServiceClient.getPackageInfo(update.getPackageId()),
                getSegmentForUpdate(update)
        );
    }

    public DetailedUpdate createUpdate(Update update) {
        update = normalizeUpdate(update);
        final PackageInfo packageInfo = checkPackageInfoOwnershipOnUpdate(update);
        final Segment segment = getSegmentForUpdate(update);
        return new DetailedUpdate(updateServiceClient.createUpdate(update), packageInfo, segment);
    }

    public DetailedUpdate editUpdate(Update update) {
        update = normalizeUpdate(update);
        checkUpdateOwnership(update.getUuid(), update.getUserId());
        final PackageInfo packageInfo = checkPackageInfoOwnershipOnUpdate(update);
        final Segment segment = getSegmentForUpdate(update);
        final Update updatedUpdate = updateServiceClient.editUpdate(update);
        return new DetailedUpdate(updatedUpdate, packageInfo, segment);
    }

    public void changeUpdateStatus(String uuid, UpdateStatus updateStatus, String userId) {
        this.changeUpdateStatus(uuid, updateStatus, Optional.empty(), userId);
    }

    public void scheduleUpdatePublication(String uuid, OffsetDateTime scheduledTime, String userId) {
        this.changeUpdateStatus(uuid, UpdateStatus.SCHEDULED, Optional.of(scheduledTime), userId);
    }

    public List<UpdateStatusCompatibility> getAllStatusesCompatibilities() {
        return this.updateServiceClient.getAllStatusesCompatibilities();
    }

    public UpdateStatusCompatibility getStatusCompatibilities(UpdateStatus status) {
        return this.updateServiceClient.getStatusCompatibilities(status);
    }

    void changeUpdateStatus(String uuid, UpdateStatus status, Optional<OffsetDateTime> scheduledTime, String userId) {
        final Update update = checkUpdateOwnership(uuid, userId);
        final Update.UpdateBuilder updateWithNewStatusBuilder = update.toBuilder().status(status);
        scheduledTime.ifPresent(offsetDateTime -> updateWithNewStatusBuilder.scheduledDate(Date.from(offsetDateTime.toInstant())));
        updateServiceClient.editUpdate(updateWithNewStatusBuilder.build());
    }

    Update normalizeUpdate(Update original) {
        if (SegmentManager.OTHER_SEGMENT_KEYWORD.equals(original.getSegmentId())) {
            return original.toBuilder().segmentId(null).build();
        }
        return original;
    }

    Segment getSegmentForUpdate(Update update) {
        if (update.hasSegment()) {
            return segmentManager.getSegmentForUser(update.getUserId(), update.getSegmentId());
        } else {
            return segmentManager.getOtherSegment(update.getUserId());
        }
    }

    PackageInfo checkPackageInfoOwnershipOnUpdate(Update update) {
        final PackageInfo packageInfo = packageServiceClient.getPackageInfo(update.getPackageId());
        if (!packageInfo.getUserId().equals(update.getUserId())) {
            throw new InvalidOwnerException("Package owner differs from the update owner");
        }
        return packageInfo;
    }

    Update checkUpdateOwnership(String uuid, String userId) {
        return updateServiceClient.getUpdateByUuidAndUserId(uuid, userId);
    }
}
