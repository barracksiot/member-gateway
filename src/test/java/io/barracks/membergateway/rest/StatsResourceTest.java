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
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.security.Principal;
import java.time.OffsetDateTime;
import java.util.ArrayList;

import static io.barracks.membergateway.utils.NumberCloseTo.closeTo;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
    public void getDeviceCountPerVersionId_shouldReturnJsonDataSet() throws Exception {
        // Given
        final DataSet expected = RandomDataSet.create();
        doReturn(expected).when(statsManager).getDevicesPerVersionId(principal.getName());

        // When
        final ResultActions result = mvc.perform(
                MockMvcRequestBuilders.get("/stats/devices/perVersionId")
                        .principal(principal)
                        .accept(MediaType.APPLICATION_JSON)
        );

        // Then
        verify(statsManager).getDevicesPerVersionId(principal.getName());
        result.andExpect(DataSetMatcher.from(expected));
    }

    @Test
    public void getDeviceCountPerVersionId_whenClientThrowsException_shouldReturnErrorCode() throws Exception {
        // Given
        doThrow(AnyBarracksClientException.from(HttpStatus.BAD_REQUEST)).when(statsManager).getDevicesPerVersionId(principal.getName());

        // When
        final ResultActions result = mvc.perform(
                MockMvcRequestBuilders.get("/stats/devices/perVersionId")
                        .principal(principal)
                        .accept(MediaType.APPLICATION_JSON)
        );

        // Then
        verify(statsManager).getDevicesPerVersionId(principal.getName());
        result.andExpect(status().isBadRequest());
    }

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
        final ResultActions result = mvc.perform(
                MockMvcRequestBuilders
                        .get(
                                "/stats/devices/lastSeen?start={start}&end={end}",
                                start, end
                        )
                        .principal(principal)
                        .accept(MediaType.APPLICATION_JSON)
        );

        // Then
        verify(statsManager).getLastSeenDevices(principal.getName(), startDate, endDate);
        result.andExpect(DataSetMatcher.from(expected));
    }

    @Test
    public void getLastSeenDevices_whenNoDatesAreProvided_shouldUseDefault() throws Exception {
        // Given
        final DataSet expected = RandomDataSet.create();
        doReturn(expected).when(statsManager).getLastSeenDevices(principal.getName(), OffsetDateTime.MIN, OffsetDateTime.MAX);

        // When
        final ResultActions result = mvc.perform(
                MockMvcRequestBuilders
                        .get(
                                "/stats/devices/lastSeen"
                        )
                        .principal(principal)
                        .accept(MediaType.APPLICATION_JSON)
        );

        // Then
        verify(statsManager).getLastSeenDevices(principal.getName(), OffsetDateTime.MIN, OffsetDateTime.MAX);
        result.andExpect(DataSetMatcher.from(expected));
    }


    @Test
    public void getLastSeenDevices_whenClientThrowsException_shouldReturnErrorCode() throws Exception {
        // Given
        final String start = "1986-02-27T10:03:00.000Z";
        final String end = "1986-02-27T10:03:00.999Z";
        final OffsetDateTime startDate = OffsetDateTime.parse(start);
        final OffsetDateTime endDate = OffsetDateTime.parse(end);
        doThrow(AnyBarracksClientException.from(HttpStatus.BAD_REQUEST)).when(statsManager).getLastSeenDevices(principal.getName(), startDate, endDate);

        // When
        final ResultActions result = mvc.perform(
                MockMvcRequestBuilders
                        .get(
                                "/stats/devices/lastSeen?start={start}&end={end}",
                                start, end
                        )
                        .principal(principal)
                        .accept(MediaType.APPLICATION_JSON)
        );

        // Then
        verify(statsManager).getLastSeenDevices(principal.getName(), startDate, endDate);
        result.andExpect(status().isBadRequest());
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
        final ResultActions result = mvc.perform(
                MockMvcRequestBuilders
                        .get(
                                "/stats/devices/seen?start={start}&end={end}",
                                start, end
                        )
                        .principal(principal)
                        .accept(MediaType.APPLICATION_JSON)
        );

        // Then
        verify(statsManager).getSeenDevices(principal.getName(), startDate, endDate);
        result.andExpect(DataSetMatcher.from(expected));
    }

    @Test
    public void getSeenDevices_whenNoDatesAreProvided_shouldUseDefault() throws Exception {
        // Given
        final DataSet expected = RandomDataSet.create();
        doReturn(expected).when(statsManager).getSeenDevices(principal.getName(), OffsetDateTime.MIN, OffsetDateTime.MAX);

        // When
        final ResultActions result = mvc.perform(
                MockMvcRequestBuilders
                        .get(
                                "/stats/devices/seen"
                        )
                        .principal(principal)
                        .accept(MediaType.APPLICATION_JSON)
        );

        // Then
        verify(statsManager).getSeenDevices(principal.getName(), OffsetDateTime.MIN, OffsetDateTime.MAX);
        result.andExpect(DataSetMatcher.from(expected));
    }

    @Test
    public void getSeenDevices_whenClientThrowsException_shouldReturnErrorCode() throws Exception {
        // Given
        final String start = "1986-02-27T10:03:00.000Z";
        final String end = "1986-02-27T10:03:00.999Z";
        final OffsetDateTime startDate = OffsetDateTime.parse(start);
        final OffsetDateTime endDate = OffsetDateTime.parse(end);
        doThrow(AnyBarracksClientException.from(HttpStatus.BAD_REQUEST)).when(statsManager).getSeenDevices(principal.getName(), startDate, endDate);

        // When
        final ResultActions result = mvc.perform(
                MockMvcRequestBuilders
                        .get("/stats/devices/seen?start={start}&end={end}", start, end)
                        .principal(principal)
                        .accept(MediaType.APPLICATION_JSON)
        );

        // Then
        verify(statsManager).getSeenDevices(principal.getName(), startDate, endDate);
        result.andExpect(status().isBadRequest());
    }

    @Test
    public void getDevicesPerSegmentId_shouldCallManager_andReturnJsonDataSet() throws Exception {
        // Given
        final DataSet expected = RandomDataSet.create();
        doReturn(expected).when(statsManager).getDevicesPerSegmentId(principal.getName());

        // When
        final ResultActions result = mvc.perform(
                MockMvcRequestBuilders
                        .get("/stats/devices/perSegmentId")
                        .principal(principal)
                        .accept(MediaType.APPLICATION_JSON)
        );

        // Then
        verify(statsManager).getDevicesPerSegmentId(principal.getName());
        result.andExpect(DataSetMatcher.from(expected));
    }

    @Test
    public void getUpdatedDevicesPerSegmentId_shouldCallManager_andReturnJsonDataSet() throws Exception {
        // Given
        final DataSet expected = RandomDataSet.create();
        doReturn(expected).when(statsManager).getUpdatedDevicesPerSegmentId(principal.getName());

        // When
        final ResultActions result = mvc.perform(
                MockMvcRequestBuilders
                        .get("/stats/devices/perSegmentId?updated=true")
                        .principal(principal)
                        .accept(MediaType.APPLICATION_JSON)
        );

        // Then
        verify(statsManager).getUpdatedDevicesPerSegmentId(principal.getName());
        result.andExpect(DataSetMatcher.from(expected));
    }

    private static class DataSetMatcher implements ResultMatcher {
        final ArrayList<ResultMatcher> matchers;

        private DataSetMatcher(DataSet expected) {
            this.matchers = new ArrayList<>(expected.getValues().size() + 1);
            matchers.add(jsonPath("total")
                    .value(closeTo(expected.getTotal()))
            );
            for (DataSet.Metric stat : expected.getValues()) {
                matchers.add(
                        jsonPath("values[?(@.name == '%s')].value", stat.getName()).value(hasItem(closeTo(stat.getValue())))
                );
            }
        }

        static ResultMatcher from(DataSet expected) {
            return new DataSetMatcher(expected);
        }

        @Override
        public void match(MvcResult result) throws Exception {
            for (ResultMatcher matcher : matchers) {
                matcher.match(result);
            }
        }
    }


}
