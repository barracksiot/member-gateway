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

import io.barracks.membergateway.client.DeviceServiceClient;
import io.barracks.membergateway.client.StatsServiceClient;
import io.barracks.membergateway.client.UpdateServiceClient;
import io.barracks.membergateway.model.DataSet;
import io.barracks.membergateway.model.Segment;
import io.barracks.membergateway.rest.entity.SegmentsOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.stream.Collectors;

import static io.barracks.membergateway.manager.SegmentManager.OTHER_SEGMENT_KEYWORD;

@Service
public class StatsManager {
    private final StatsServiceClient statsServiceClient;
    private final UpdateServiceClient updateServiceClient;
    private final SegmentManager segmentManager;
    private final UpdateManager updateManager;
    private final DeviceServiceClient deviceServiceClient;

    @Autowired
    public StatsManager(
            StatsServiceClient statsServiceClient,
            SegmentManager segmentManager,
            UpdateServiceClient updateServiceClient,
            UpdateManager updateManager,
            DeviceServiceClient deviceServiceClient
    ) {
        this.statsServiceClient = statsServiceClient;
        this.updateServiceClient = updateServiceClient;
        this.segmentManager = segmentManager;
        this.updateManager = updateManager;
        this.deviceServiceClient = deviceServiceClient;
    }

    public DataSet getDevicesPerVersionId(String userId) {
        return statsServiceClient.getDevicesPerVersionId(userId);
    }

    public DataSet getLastSeenDevices(String userId, OffsetDateTime start, OffsetDateTime end) {
        return statsServiceClient.getLastSeenDevices(userId, start, end);
    }

    public DataSet getSeenDevices(String userId, OffsetDateTime start, OffsetDateTime end) {
        return statsServiceClient.getSeenDevices(userId, start, end);
    }

    public DataSet getDevicesPerSegmentId(String userId) {
        final SegmentsOrder segmentOrder = segmentManager.getOrderedSegments(userId);
        final Map<String, BigDecimal> stats = segmentOrder.getActive().stream().collect(Collectors.toMap(
                Segment::getName,
                segment -> BigDecimal.valueOf(segment.getDeviceCount())
        ));
        stats.put(segmentOrder.getOther().getName(), BigDecimal.valueOf(segmentOrder.getOther().getDeviceCount()));
        final BigDecimal total = stats.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return DataSet.builder().total(total).values(stats).build();
    }

    public DataSet getUpdatedDevicesPerSegmentId(String userId) {
        final SegmentsOrder segmentOrder = segmentManager.getOrderedSegments(userId);
        final Map<String, BigDecimal> stats = segmentOrder.getActive().stream()
                .collect(Collectors.toMap(
                        Segment::getName,
                        segment -> getUpdatedDevicesCountForSegment(userId, segment.getId(), segment.getDeviceCount())
                ));
        stats.put(segmentOrder.getOther().getName(), getUpdatedDevicesCountForSegment(userId, OTHER_SEGMENT_KEYWORD, segmentOrder.getOther().getDeviceCount()));
        final BigDecimal total = stats.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return DataSet.builder().values(stats).total(total).build();
    }

    BigDecimal getUpdatedDevicesCountForSegment(String userId, String segmentId, long defaultValue) {
        final Pageable pageable = new PageRequest(0, 1);
        return BigDecimal.valueOf(
                updateServiceClient.getLatestUpdateForSegment(userId, segmentId.equals(OTHER_SEGMENT_KEYWORD) ? null : segmentId)
                        .map(update -> updateManager.getUpdateByUuidAndUserId(update.getUuid(), userId).getPackageInfo().getVersionId())
                        .map(versionId -> deviceServiceClient.getDevicesBySegmentAndVersion(userId, segmentId, versionId, pageable).getMetadata().getTotalElements())
                        .orElse(defaultValue)
        );
    }
}
