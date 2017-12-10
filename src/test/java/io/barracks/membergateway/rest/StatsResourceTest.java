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

import io.barracks.membergateway.manager.StatsManager;
import io.barracks.membergateway.model.DataSet;
import io.barracks.membergateway.utils.RandomDataSet;
import io.barracks.membergateway.utils.RandomPrincipal;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import java.security.Principal;
import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

@RunWith(SpringRunner.class)
@BarracksResourceTest(controllers = StatsResource.class)
public class StatsResourceTest {
    @Autowired
    private MockMvc mvc;
    @MockBean
    private StatsManager statsManager;
    @Autowired
    private StatsResource statsResource;

    private Principal principal = new RandomPrincipal();

    @Test
    public void getLastSeenDevices_shouldReturnJsonDataSet() throws Exception {
        // Given
        final DataSet expected = RandomDataSet.create();
        final String start = "1986-02-27T10:03:00.000Z";
        final String end = "1986-02-27T10:03:00.999Z";
        final OffsetDateTime startDate = OffsetDateTime.parse(start);
        final OffsetDateTime endDate = OffsetDateTime.parse(end);
        doReturn(expected).when(statsManager).getLastSeenDevices(principal.getName(), startDate, endDate);

        // When
        final DataSet result = statsResource.getLastSeenDevices(principal, startDate, endDate);

        // Then
        verify(statsManager).getLastSeenDevices(principal.getName(), startDate, endDate);
        assertThat(result).isEqualTo(expected);
    }

    @Test
    public void getLastSeenDevices_whenNoDatesAreProvided_shouldUseDefault() throws Exception {
        // Given
        final DataSet expected = RandomDataSet.create();
        doReturn(expected).when(statsManager).getLastSeenDevices(principal.getName(), OffsetDateTime.MIN, OffsetDateTime.MAX);

        // When
        final DataSet result = statsResource.getLastSeenDevices(principal, null, null);

        // Then
        verify(statsManager).getLastSeenDevices(principal.getName(), OffsetDateTime.MIN, OffsetDateTime.MAX);
        assertThat(result).isEqualTo(expected);
    }


    @Test
    public void getSeenDevices_shouldReturnJsonDataSet() throws Exception {
        // Given
        final DataSet expected = RandomDataSet.create();
        final String start = "1986-02-27T10:03:00.000Z";
        final String end = "1986-02-27T10:03:00.999Z";
        final OffsetDateTime startDate = OffsetDateTime.parse(start);
        final OffsetDateTime endDate = OffsetDateTime.parse(end);
        doReturn(expected).when(statsManager).getSeenDevices(principal.getName(), startDate, endDate);

        // When
        final DataSet result = statsResource.getSeenDevices(principal, startDate, endDate);

        // Then
        verify(statsManager).getSeenDevices(principal.getName(), startDate, endDate);
        assertThat(result).isEqualTo(expected);

    }

    @Test
    public void getSeenDevices_whenNoDatesAreProvided_shouldUseDefault() throws Exception {
        // Given
        final DataSet expected = RandomDataSet.create();
        doReturn(expected).when(statsManager).getSeenDevices(principal.getName(), OffsetDateTime.MIN, OffsetDateTime.MAX);

        // When
        final DataSet result = statsResource.getSeenDevices(principal, null, null);

        // Then
        verify(statsManager).getSeenDevices(principal.getName(), OffsetDateTime.MIN, OffsetDateTime.MAX);
        assertThat(result).isEqualTo(expected);
    }

}
