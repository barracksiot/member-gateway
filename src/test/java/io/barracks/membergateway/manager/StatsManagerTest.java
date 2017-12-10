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
import io.barracks.membergateway.model.DetailedUpdate;
import io.barracks.membergateway.model.Device;
import io.barracks.membergateway.model.Update;
import io.barracks.membergateway.rest.entity.SegmentsOrder;
import io.barracks.membergateway.utils.DeviceUtils;
import io.barracks.membergateway.utils.RandomDataSet;
import io.barracks.membergateway.utils.SegmentUtils;
import io.barracks.membergateway.utils.UpdateUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.PagedResources;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class StatsManagerTest {
    @Mock
    private StatsServiceClient statsServiceClient;
    @Mock
    private UpdateServiceClient updateServiceClient;
    @Mock
    private SegmentManager segmentManager;
    @Mock
    private UpdateManager updateManager;
    @Mock
    private DeviceServiceClient deviceServiceClient;

    private StatsManager statsManager;

    @Before
    public void setUp() {
        this.statsManager = spy(new StatsManager(statsServiceClient, segmentManager, updateServiceClient, updateManager, deviceServiceClient));
    }

    @Test
    public void getDevicesPerVersionId_shouldForwardCallToClient() {
        // Given
        final String userId = UUID.randomUUID().toString();
        final DataSet expected = RandomDataSet.create();
        doReturn(expected).when(statsServiceClient).getDevicesPerVersionId(userId);

        // When
        final DataSet result = statsManager.getDevicesPerVersionId(userId);

        // Then
        verify(statsServiceClient).getDevicesPerVersionId(userId);
        assertThat(result).isEqualTo(expected);
    }

    @Test
    public void getLastSeenDevices_shouldForwardCallToClient() {
        // Given
        final String userId = UUID.randomUUID().toString();
        final DataSet expected = RandomDataSet.create();
        final OffsetDateTime start = randomDate();
        final OffsetDateTime end = randomDate();
        doReturn(expected).when(statsServiceClient).getLastSeenDevices(userId, start, end);

        // When
        final DataSet result = statsManager.getLastSeenDevices(userId, start, end);

        // Then
        verify(statsServiceClient).getLastSeenDevices(userId, start, end);
        assertThat(result).isEqualTo(expected);
    }

    @Test
    public void getSeenDevices_shouldForwardCallToClient() {
        // Given
        final String userId = UUID.randomUUID().toString();
        final DataSet expected = RandomDataSet.create();
        final OffsetDateTime start = randomDate();
        final OffsetDateTime end = randomDate();
        doReturn(expected).when(statsServiceClient).getSeenDevices(userId, start, end);

        // When
        final DataSet result = statsManager.getSeenDevices(userId, start, end);

        // Then
        verify(statsServiceClient).getSeenDevices(userId, start, end);
        assertThat(result).isEqualTo(expected);
    }

    @Test
    public void getDevicesPerSegmentId_shouldMapSegmentOrder_andReturnResult() {
        // Given
        final String userId = UUID.randomUUID().toString();
        final SegmentsOrder order = SegmentsOrder.builder()
                .addActive(SegmentUtils.getSegment().toBuilder().deviceCount(20).build())
                .addActive(SegmentUtils.getSegment().toBuilder().deviceCount(1).build())
                .other(SegmentUtils.getSegment().toBuilder().id("other").name("Other").deviceCount(21).build())
                .addInactive(SegmentUtils.getSegment().toBuilder().deviceCount(9000).build())
                .build();
        doReturn(order).when(segmentManager).getOrderedSegments(userId);
        final DataSet expected = DataSet.builder()
                .value(order.getActive().get(0).getName(), BigDecimal.valueOf(order.getActive().get(0).getDeviceCount()))
                .value(order.getActive().get(1).getName(), BigDecimal.valueOf(order.getActive().get(1).getDeviceCount()))
                .value("Other", BigDecimal.valueOf(order.getOther().getDeviceCount()))
                .total(BigDecimal.valueOf(42))
                .build();

        // When
        final DataSet result = statsManager.getDevicesPerSegmentId(userId);

        // Then
        verify(segmentManager).getOrderedSegments(userId);
        assertThat(result.getValues()).containsAll(expected.getValues());
        assertThat(result.getTotal()).isEqualTo(expected.getTotal());
    }

    @Test
    public void getUpdatedDevicesPerSegmentId_shouldGetUpdatedDevicesCountForActiveAndOther_andReturnResults() {
        // Given
        final String userId = UUID.randomUUID().toString();
        final SegmentsOrder order = SegmentsOrder.builder()
                .addActive(SegmentUtils.getSegment().toBuilder().deviceCount(10).build())
                .addActive(SegmentUtils.getSegment().toBuilder().deviceCount(20).build())
                .other(SegmentUtils.getSegment().toBuilder().id("other").name("Other").deviceCount(30).build())
                .addInactive(SegmentUtils.getSegment().toBuilder().deviceCount(9000).build())
                .build();
        doReturn(order).when(segmentManager).getOrderedSegments(userId);
        doReturn(BigDecimal.valueOf(20)).when(statsManager).getUpdatedDevicesCountForSegment(userId, order.getActive().get(0).getId(), order.getActive().get(0).getDeviceCount());
        doReturn(BigDecimal.valueOf(1)).when(statsManager).getUpdatedDevicesCountForSegment(userId, order.getActive().get(1).getId(), order.getActive().get(1).getDeviceCount());
        doReturn(BigDecimal.valueOf(21)).when(statsManager).getUpdatedDevicesCountForSegment(userId, order.getOther().getId(), order.getOther().getDeviceCount());
        final DataSet expected = DataSet.builder()
                .value(order.getActive().get(0).getName(), BigDecimal.valueOf(20))
                .value(order.getActive().get(1).getName(), BigDecimal.valueOf(1))
                .value("Other", BigDecimal.valueOf(21))
                .total(BigDecimal.valueOf(42))
                .build();

        // When
        final DataSet result = statsManager.getUpdatedDevicesPerSegmentId(userId);

        // Then
        verify(segmentManager).getOrderedSegments(userId);
        verify(statsManager).getUpdatedDevicesCountForSegment(userId, order.getActive().get(0).getId(), order.getActive().get(0).getDeviceCount());
        verify(statsManager).getUpdatedDevicesCountForSegment(userId, order.getActive().get(1).getId(), order.getActive().get(1).getDeviceCount());
        verify(statsManager).getUpdatedDevicesCountForSegment(userId, order.getOther().getId(), order.getOther().getDeviceCount());
        assertThat(result.getValues()).containsAll(expected.getValues());
        assertThat(result.getTotal()).isEqualTo(expected.getTotal());
    }

    @Test
    public void getUpdatedDevicesCountForSegment_whenLatestUpdateExists_shouldCallClientAndReturnResult() {
        // Given
        final String userId = UUID.randomUUID().toString();
        final String segmentId = UUID.randomUUID().toString();
        final Pageable pageable = new PageRequest(0, 1);
        final Update update = UpdateUtils.getUpdate();
        final DetailedUpdate detailedUpdate = UpdateUtils.getDetailedUpdate();
        final PagedResources<Device> devices = new PagedResources<>(Collections.singleton(DeviceUtils.getDevice()), new PagedResources.PageMetadata(1, 0, 42));
        doReturn(Optional.of(update)).when(updateServiceClient).getLatestUpdateForSegment(userId, segmentId);
        doReturn(detailedUpdate).when(updateManager).getUpdateByUuidAndUserId(update.getUuid(), userId);
        doReturn(devices).when(deviceServiceClient).getDevicesBySegmentAndVersion(userId, segmentId, detailedUpdate.getPackageInfo().getVersionId(), pageable);

        // When
        final BigDecimal result = statsManager.getUpdatedDevicesCountForSegment(userId, segmentId, 24);

        // Then
        verify(updateServiceClient).getLatestUpdateForSegment(userId, segmentId);
        verify(updateManager).getUpdateByUuidAndUserId(update.getUuid(), userId);
        verify(deviceServiceClient).getDevicesBySegmentAndVersion(userId, segmentId, detailedUpdate.getPackageInfo().getVersionId(), pageable);
        assertThat(result).isEqualTo(BigDecimal.valueOf(42));
    }

    @Test
    public void getUpdatedDevicesCountForSegment_whenNoLatestUpdate_shouldReturnDefaultValue() {
        // Given
        final String userId = UUID.randomUUID().toString();
        final String segmentId = UUID.randomUUID().toString();
        doReturn(Optional.empty()).when(updateServiceClient).getLatestUpdateForSegment(userId, segmentId);

        // When
        final BigDecimal result = statsManager.getUpdatedDevicesCountForSegment(userId, segmentId, 24);

        // Then
        verify(updateServiceClient).getLatestUpdateForSegment(userId, segmentId);
        assertThat(result).isEqualTo(BigDecimal.valueOf(24));
    }

    private OffsetDateTime randomDate() {
        final SecureRandom secureRandom = new SecureRandom();
        return OffsetDateTime.from(Instant.ofEpochMilli(secureRandom.nextLong()).atZone(ZoneId.of("Z")));
    }
}