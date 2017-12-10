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
import io.barracks.membergateway.exception.InvalidOwnerException;
import io.barracks.membergateway.manager.entity.SegmentStatus;
import io.barracks.membergateway.model.Device;
import io.barracks.membergateway.model.DeviceConfiguration;
import io.barracks.membergateway.model.DeviceEvent;
import io.barracks.membergateway.model.Segment;
import io.barracks.membergateway.rest.entity.SegmentsOrder;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.PagedResources;

import java.security.SecureRandom;
import java.util.*;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class SegmentManagerTest {
    @Mock
    private DeviceServiceClient deviceServiceClient;
    private SegmentManager segmentManager;

    @Before
    public void setup() {
        this.segmentManager = spy(new SegmentManager(deviceServiceClient));
    }

    @Test
    public void createSegment_shouldCallClientAndEnhanceSegment() {
        // Given
        final String userId = UUID.randomUUID().toString();
        final Segment segment = Segment.builder().name("name").build();
        final Segment toCreate = Segment.builder().userId(userId).name(segment.getName()).build();
        final Segment expected = Segment.builder().id(UUID.randomUUID().toString()).name("name").userId(userId).active(false).deviceCount(42).build();
        doReturn(expected).when(deviceServiceClient).createSegment(toCreate);
        doReturn(expected).when(segmentManager).enhanceSegment(expected);

        // When
        Segment result = segmentManager.createSegment(userId, segment);

        // Then
        verify(deviceServiceClient).createSegment(toCreate);
        verify(segmentManager).enhanceSegment(expected);
        assertThat(result).isEqualTo(expected);
    }

    @Test
    public void updateSegment_shouldCallClientAndEnhanceSegment() {
        // Given
        final String userId = UUID.randomUUID().toString();
        final String segmentId = UUID.randomUUID().toString();
        final Segment segment = Segment.builder().name("toUpdate").build();
        final Segment toUpdate = Segment.builder().userId(userId).name(segment.getName()).build();
        final Segment expected = Segment.builder().id(segmentId).name("updated").active(false).deviceCount(42).build();
        doReturn(null).when(segmentManager).getSegmentAndCheckOwnership(userId, segmentId);
        doReturn(expected).when(deviceServiceClient).updateSegment(segmentId, toUpdate);
        doReturn(expected).when(segmentManager).enhanceSegment(expected);

        // When
        Segment result = segmentManager.updateSegment(userId, segmentId, segment);

        // Then
        verify(segmentManager).getSegmentAndCheckOwnership(userId, segmentId);
        verify(deviceServiceClient).updateSegment(segmentId, toUpdate);
        verify(segmentManager).enhanceSegment(expected);
        assertThat(result).isEqualTo(expected);
    }

    @Test
    public void getSegmentForUser_shouldCheckOwnershipAndReturnEnhancedSegments() {
        // Given
        final String userId = UUID.randomUUID().toString();
        final String segmentId = UUID.randomUUID().toString();
        final Segment segment = Segment.builder().id(segmentId).name("name").userId(userId).build();
        final Segment enhanced = segment.toBuilder().active(true).deviceCount(42).build();
        doReturn(segment).when(segmentManager).getSegmentAndCheckOwnership(userId, segmentId);
        doReturn(enhanced).when(segmentManager).enhanceSegment(segment);

        // When
        final Segment result = segmentManager.getSegmentForUser(userId, segmentId);

        // Then
        verify(segmentManager).getSegmentAndCheckOwnership(userId, segmentId);
        verify(segmentManager).enhanceSegment(segment);
        assertThat(result).isEqualTo(enhanced);
    }

    @Test
    public void getSegmentForUser_whenOtherSegment_shouldReturnOtherSegment() {
        // Given
        final String userId = UUID.randomUUID().toString();
        final String segmentId = "other";
        final Segment expected = Segment.builder().id(UUID.randomUUID().toString()).build();
        doReturn(expected).when(segmentManager).getOtherSegment(userId);

        // When
        final Segment result = segmentManager.getSegmentForUser(userId, segmentId);

        // Then
        verify(segmentManager).getOtherSegment(userId);
        assertThat(result).isEqualTo(expected);
    }

    @Test
    public void getSegmentsForUser_shouldCallClientAndReturnEnhancedSegments() {
        // Given
        final String userId = UUID.randomUUID().toString();
        final Pageable pageable = new PageRequest(0, 10);
        final Segment segment = Segment.builder().id(UUID.randomUUID().toString()).name("name").userId(userId).build();
        final Segment enhanced = segment.toBuilder().active(true).deviceCount(42).build();
        final PagedResources<Segment> segmentPagedResources = new PagedResources<>(Collections.singleton(segment), new PagedResources.PageMetadata(1, 0, 1));
        doReturn(segmentPagedResources).when(deviceServiceClient).getSegments(userId, pageable);
        doReturn(Collections.singletonList(enhanced))
                .when(segmentManager).enhanceSegments((Collection<Segment>) argThat(hasItem(segment)));

        // When
        final Page<Segment> result = segmentManager.getSegmentsForUser(userId, pageable);

        // Then
        verify(deviceServiceClient).getSegments(userId, pageable);
        verify(segmentManager).enhanceSegments((Collection<Segment>) argThat(hasItem(segment)));
        assertThat(result).hasSize(1).contains(enhanced);
    }

    @Test
    public void getDevicesBySegment_whitSegmentId_shouldCallClientAndReturnEvents() {
        // Given
        final String userId = UUID.randomUUID().toString();
        final String segmentId = UUID.randomUUID().toString();
        final Pageable pageable = new PageRequest(0, 10);
        final DeviceEvent event = DeviceEvent.builder().unitId(UUID.randomUUID().toString()).versionId(UUID.randomUUID().toString()).build();
        final DeviceConfiguration configuration = DeviceConfiguration.builder().build();
        final Device device = Device.builder().lastEvent(event).configuration(configuration).unitId(UUID.randomUUID().toString()).build();
        doReturn(null).when(segmentManager).getSegmentAndCheckOwnership(userId, segmentId);
        doReturn(new PagedResources<>(Collections.singletonList(device), new PagedResources.PageMetadata(1, 0, 1)))
                .when(deviceServiceClient).getDevicesBySegment(userId, segmentId, pageable);

        // When
        Page<Device> result = segmentManager.getDevicesBySegment(userId, segmentId, pageable);

        // Then
        verify(segmentManager).getSegmentAndCheckOwnership(userId, segmentId);
        verify(deviceServiceClient).getDevicesBySegment(userId, segmentId, pageable);
        assertThat(result).hasSize(1).contains(device);
    }

    @Test
    public void getDeviceBySegment_forOtherSegment_shouldCallClientAndReturnEvents() {
        // Given
        final String userId = UUID.randomUUID().toString();
        final String segmentId = "other";
        final Pageable pageable = new PageRequest(0, 10);
        final DeviceEvent event = DeviceEvent.builder().unitId(UUID.randomUUID().toString()).versionId(UUID.randomUUID().toString()).build();
        final DeviceConfiguration configuration = DeviceConfiguration.builder().build();
        final Device device = Device.builder().lastEvent(event).configuration(configuration).unitId(UUID.randomUUID().toString()).build();
        doReturn(new PagedResources<>(Collections.singletonList(device), new PagedResources.PageMetadata(1, 0, 1)))
                .when(deviceServiceClient).getDevicesBySegment(userId, segmentId, pageable);

        // When
        Page<Device> result = segmentManager.getDevicesBySegment(userId, segmentId, pageable);

        // Then
        verify(deviceServiceClient).getDevicesBySegment(userId, segmentId, pageable);
        assertThat(result).hasSize(1).contains(device);
    }

    @Test
    public void getOrderedSegments_shouldCallClient_andReturnSegmentList() {
        // Given
        final String otherId = "other";
        final String userId = UUID.randomUUID().toString();
        final List<Segment> active = Arrays.asList(
                Segment.builder().id(UUID.randomUUID().toString()).userId(userId).build(),
                Segment.builder().id(UUID.randomUUID().toString()).userId(userId).build()
        );
        final List<Segment> inactive = Arrays.asList(
                Segment.builder().id(UUID.randomUUID().toString()).userId(userId).build(),
                Segment.builder().id(UUID.randomUUID().toString()).userId(userId).build()
        );
        final Segment other = Segment.builder()
                .id(otherId)
                .name("other")
                .userId(userId)
                .deviceCount(42)
                .active(true)
                .build();
        final SegmentsOrder expected = SegmentsOrder.builder()
                .active(active)
                .inactive(inactive)
                .other(other)
                .build();
        doReturn(active).when(deviceServiceClient).getSegmentsByStatus(userId, SegmentStatus.ACTIVE);
        doReturn(inactive).when(deviceServiceClient).getSegmentsByStatus(userId, SegmentStatus.INACTIVE);
        Stream.of(active, inactive).forEach(segments -> doReturn(segments).when(segmentManager).enhanceSegments(segments));
        doReturn(other).when(segmentManager).getOtherSegment(userId);


        // When
        final SegmentsOrder result = segmentManager.getOrderedSegments(userId);

        // Then
        verify(deviceServiceClient).getSegmentsByStatus(userId, SegmentStatus.ACTIVE);
        verify(deviceServiceClient).getSegmentsByStatus(userId, SegmentStatus.INACTIVE);
        Stream.of(active, inactive).forEach(segments -> verify(segmentManager).enhanceSegments(segments));
        verify(segmentManager).getOtherSegment(userId);
        assertThat(result).isEqualTo(expected);
    }

    @Test
    public void updateSegmentsOrder_shouldCallClient_andReturnIdxList() {
        // Given
        final String userId = UUID.randomUUID().toString();
        final List<String> order = Arrays.asList(
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString()
        );
        order.forEach(segmentId ->
                doReturn(null).when(segmentManager).getSegmentAndCheckOwnership(userId, segmentId)
        );
        final List<String> expected = Arrays.asList(
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString()
        );
        doReturn(expected).when(deviceServiceClient).updateSegmentsOrder(userId, order);

        // When
        final List<String> result = segmentManager.updateSegmentsOrder(userId, order);

        // Then
        order.forEach(segmentId ->
                verify(segmentManager).getSegmentAndCheckOwnership(userId, segmentId)
        );
        verify(deviceServiceClient).updateSegmentsOrder(userId, order);
        assertThat(result).containsExactlyElementsOf(expected);
    }

    @Test
    public void getOtherSegment_shouldReturnSegmentWithDeviceCountAndFakeCharacteristics() {
        // Given
        final String segmentId = "other";
        final String userId = UUID.randomUUID().toString();
        final long count = new SecureRandom().nextLong();
        final Segment expected = Segment.builder()
                .id("other")
                .active(true)
                .deviceCount(count)
                .name("Other")
                .userId(userId)
                .build();
        doReturn(count).when(segmentManager).getDeviceCount(userId, segmentId);

        // When
        final Segment result = segmentManager.getOtherSegment(userId);

        // Then
        verify(segmentManager).getDeviceCount(userId, segmentId);
        assertThat(result).isEqualTo(expected);
    }

    @Test
    public void enhanceSegment_shouldCallEnhanceWithOneSegment_andReturnResult() {
        // Given
        final Segment segment = Segment.builder().id(UUID.randomUUID().toString()).build();
        final Segment enhanced = Segment.builder().id(UUID.randomUUID().toString()).build();
        doReturn(Collections.singletonList(enhanced)).when(segmentManager).enhanceSegments(Collections.singletonList(segment));

        // When
        final Segment result = segmentManager.enhanceSegment(segment);

        // Then
        verify(segmentManager).enhanceSegments(Collections.singletonList(segment));
        assertThat(result).isEqualTo(enhanced);
    }

    @Test
    public void enhanceSegments_whenEmptySegmentList_shouldReturnEmptyList() {
        // Given
        final List<Segment> noSegments = Collections.emptyList();

        // When
        final List<Segment> result = segmentManager.enhanceSegments(noSegments);

        // Then
        verify(segmentManager).enhanceSegments(noSegments);
        verifyNoMoreInteractions(segmentManager);
        assertThat(result).isEmpty();
    }

    @Test
    public void enhanceSegments_whenSegmentList_shouldCheckForStatusAndGetDeviceCount() {
        // Given
        final String userId = UUID.randomUUID().toString();
        final Segment active = Segment.builder()
                .id(UUID.randomUUID().toString())
                .userId(userId)
                .build();
        final Segment inactive = Segment.builder()
                .id(UUID.randomUUID().toString())
                .userId(userId)
                .build();
        final List<Segment> segments = Arrays.asList(inactive, active);
        final List<Segment> activeSegments = Collections.singletonList(active);
        final List<Segment> expected = Arrays.asList(
                inactive.toBuilder().active(false).deviceCount(24L).build(),
                active.toBuilder().active(true).deviceCount(42L).build()
        );
        doReturn(activeSegments).when(deviceServiceClient).getSegmentsByStatus(userId, SegmentStatus.ACTIVE);
        doReturn(42L).when(segmentManager).getDeviceCount(userId, active.getId());
        doReturn(24L).when(segmentManager).getDeviceCount(userId, inactive.getId());

        // When
        final List<Segment> result = segmentManager.enhanceSegments(segments);

        // Then
        verify(deviceServiceClient).getSegmentsByStatus(userId, SegmentStatus.ACTIVE);
        verify(segmentManager).getDeviceCount(userId, active.getId());
        verify(segmentManager).getDeviceCount(userId, inactive.getId());
        assertThat(result).containsExactlyElementsOf(expected);
    }

    @Test
    public void getSegmentAndCheckOwnership_whenUserIsNotOwner_shouldThrowException() {
        // Given
        final String userId = UUID.randomUUID().toString();
        final String segmentId = UUID.randomUUID().toString();
        final Segment original = Segment.builder().userId(UUID.randomUUID().toString()).build();
        doReturn(original).when(deviceServiceClient).getSegment(segmentId);

        // Then When
        assertThatExceptionOfType(InvalidOwnerException.class)
                .isThrownBy(() -> segmentManager.getSegmentAndCheckOwnership(userId, segmentId));
        verify(deviceServiceClient).getSegment(segmentId);
    }

    @Test
    public void getSegmentAndCheckOwnership_whenUserIsOwner_shouldCallClientAndReturnSegment() {
        // Given
        final String userId = UUID.randomUUID().toString();
        final String segmentId = UUID.randomUUID().toString();
        final Segment expected = Segment.builder().userId(userId).name("aSegment").build();
        doReturn(expected).when(deviceServiceClient).getSegment(segmentId);

        // When
        Segment result = segmentManager.getSegmentAndCheckOwnership(userId, segmentId);

        // Then
        verify(deviceServiceClient).getSegment(segmentId);
        assertThat(result).isEqualTo(expected);
    }

    @Test
    public void getDeviceCount_shouldCallServiceClientAndReturnTotalNumberOfDevices() {
        // Given
        final String userId = UUID.randomUUID().toString();
        final String segmentId = UUID.randomUUID().toString();
        final PageRequest request = new PageRequest(0, 1);
        final PagedResources<DeviceEvent> clientResult = new PagedResources<>(
                Collections.emptyList(),
                new PagedResources.PageMetadata(1, 0, 42L)
        );
        doReturn(clientResult).when(deviceServiceClient).getDevicesBySegment(userId, segmentId, request);

        // When
        final long result = segmentManager.getDeviceCount(userId, segmentId);

        // Then
        verify(deviceServiceClient).getDevicesBySegment(userId, segmentId, request);
        assertThat(result).isEqualTo(42L);
    }

    @Test
    public void getDeviceCountForOther_shouldCallServiceClientAndReturnTotalNumberOfDevices() {
        // Given
        final String userId = UUID.randomUUID().toString();
        final String segmentId = "other";
        final PageRequest request = new PageRequest(0, 1);
        final PagedResources<DeviceEvent> clientResult = new PagedResources<>(
                Collections.emptyList(),
                new PagedResources.PageMetadata(1, 0, 42L)
        );
        doReturn(clientResult).when(deviceServiceClient).getDevicesBySegment(userId, segmentId, request);

        // When
        final long result = segmentManager.getDeviceCount(userId, segmentId);

        // Then
        verify(deviceServiceClient).getDevicesBySegment(userId, segmentId, request);
        assertThat(result).isEqualTo(42L);
    }
}