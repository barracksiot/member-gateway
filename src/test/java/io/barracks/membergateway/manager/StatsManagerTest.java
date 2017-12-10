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

import io.barracks.membergateway.client.StatsServiceClient;
import io.barracks.membergateway.model.DataSet;
import io.barracks.membergateway.utils.RandomDataSet;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class StatsManagerTest {
    @Mock
    private StatsServiceClient statsServiceClient;

    private StatsManager statsManager;

    @Before
    public void setUp() {
        this.statsManager = spy(new StatsManager(statsServiceClient));
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

    private OffsetDateTime randomDate() {
        final SecureRandom secureRandom = new SecureRandom();
        return OffsetDateTime.from(Instant.ofEpochMilli(secureRandom.nextLong()).atZone(ZoneId.of("Z")));
    }
}
