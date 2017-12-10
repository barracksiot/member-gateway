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

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import io.barracks.membergateway.client.DeviceServiceClient;
import io.barracks.membergateway.exception.InvalidOwnerException;
import io.barracks.membergateway.manager.entity.SegmentStatus;
import io.barracks.membergateway.model.Device;
import io.barracks.membergateway.model.Segment;
import io.barracks.membergateway.rest.entity.SegmentsOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.PagedResources;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SegmentManager {

    public static final String OTHER_SEGMENT_KEYWORD = "other";

    private final DeviceServiceClient deviceServiceClient;

    @Autowired
    public SegmentManager(DeviceServiceClient deviceServiceClient) {
        this.deviceServiceClient = deviceServiceClient;
    }

    public Segment createSegment(String userId, Segment segment) {
        Segment toCreate = Segment.builder()
                .userId(userId)
                .name(segment.getName())
                .query(segment.getQuery())
                .build();
        Segment result = deviceServiceClient.createSegment(toCreate);
        return enhanceSegment(result);
    }

    public Segment updateSegment(String userId, String segmentId, Segment segment) {
        getSegmentAndCheckOwnership(userId, segmentId);
        Segment update = Segment.builder()
                .userId(userId)
                .name(segment.getName())
                .query(segment.getQuery())
                .build();
        final Segment result = deviceServiceClient.updateSegment(segmentId, update);
        return enhanceSegment(result);
    }

    public Segment getSegmentForUser(String userId, String segmentId) {
        if (OTHER_SEGMENT_KEYWORD.equals(segmentId)) {
            return getOtherSegment(userId);
        } else {
            final Segment segment = getSegmentAndCheckOwnership(userId, segmentId);
            return enhanceSegment(segment);
        }
    }

    public Page<Segment> getSegmentsForUser(String userId, Pageable pageable) {
        PagedResources<Segment> pagedResources = deviceServiceClient.getSegments(userId, pageable);
        List<Segment> enhancedSegments = enhanceSegments(pagedResources.getContent());
        return new PageImpl<>(enhancedSegments, pageable, pagedResources.getMetadata().getTotalElements());
    }

    public Page<Device> getDevicesBySegment(String userId, String segmentId, Pageable pageable) {
        PagedResources<Device> devicePagedResources;
        if (!OTHER_SEGMENT_KEYWORD.equals(segmentId)) {
            getSegmentAndCheckOwnership(userId, segmentId);
        }
        devicePagedResources = deviceServiceClient.getDevicesBySegment(userId, segmentId, pageable);
        return new PageImpl<>(
                Lists.newArrayList(devicePagedResources.getContent()),
                pageable,
                devicePagedResources.getMetadata().getTotalElements()
        );
    }

    public SegmentsOrder getOrderedSegments(String userId) {
        return SegmentsOrder.builder()
                .active(
                        enhanceSegments(deviceServiceClient.getSegmentsByStatus(userId, SegmentStatus.ACTIVE))
                )
                .inactive(
                        enhanceSegments(deviceServiceClient.getSegmentsByStatus(userId, SegmentStatus.INACTIVE))
                )
                .other(
                        getOtherSegment(userId)
                )
                .build();
    }

    public List<String> updateSegmentsOrder(String userId, List<String> order) {
        for (String segmentId : order) {
            getSegmentAndCheckOwnership(userId, segmentId);
        }
        return deviceServiceClient.updateSegmentsOrder(userId, order);
    }

    Segment getOtherSegment(String userId) {
        return Segment.builder()
                .id(OTHER_SEGMENT_KEYWORD)
                .userId(userId)
                .name(StringUtils.capitalize(OTHER_SEGMENT_KEYWORD))
                .active(true)
                .deviceCount(getDeviceCount(userId, OTHER_SEGMENT_KEYWORD))
                .build();
    }

    Segment enhanceSegment(Segment segment) {
        return enhanceSegments(Collections.singletonList(segment)).get(0);
    }

    List<Segment> enhanceSegments(Collection<Segment> segments) {
        if (segments.size() == 0) {
            return Collections.emptyList();
        }
        List<Segment> activeSegments = deviceServiceClient.getSegmentsByStatus(
                Iterables.get(segments, 0).getUserId(),
                SegmentStatus.ACTIVE
        );
        return segments.stream()
                .map(segment ->
                        segment.toBuilder()
                                .active(activeSegments.stream().anyMatch(activeSegment ->
                                        activeSegment.getId().equals(segment.getId())
                                ))
                                .deviceCount(
                                        getDeviceCount(segment.getUserId(), segment.getId())
                                )
                                .build()
                )
                .collect(Collectors.toList());
    }

    Segment getSegmentAndCheckOwnership(String userId, String segmentId) {
        Segment segment = deviceServiceClient.getSegment(segmentId);
        if (!segment.getUserId().equals(userId)) {
            throw new InvalidOwnerException("Segment owner differs from user");
        }
        return segment;
    }

    long getDeviceCount(String userId, String segmentId) {
        return deviceServiceClient.getDevicesBySegment(userId, segmentId, new PageRequest(0, 1))
                .getMetadata()
                .getTotalElements();
    }

}
