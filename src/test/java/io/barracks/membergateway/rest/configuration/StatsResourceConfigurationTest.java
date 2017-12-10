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

package io.barracks.membergateway.rest.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.barracks.commons.util.Endpoint;
import io.barracks.membergateway.model.DataSet;
import io.barracks.membergateway.rest.BarracksResourceTest;
import io.barracks.membergateway.rest.StatsResource;
import io.barracks.membergateway.utils.RandomDataSet;
import io.barracks.membergateway.utils.RandomPrincipal;
import net.minidev.json.JSONArray;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpMethod;
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;

import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.request.RequestDocumentation.requestParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@BarracksResourceTest(controllers = StatsResource.class, outputDir = "build/generated-snippets/stats")
public class StatsResourceConfigurationTest {

    private static final Endpoint DEVICES_LAST_SEEN_ENDPOINT = Endpoint.from(HttpMethod.GET, "/stats/devices/lastSeen", "start={start}&end={end}");
    private static final Endpoint DEVICES_SEEN_ENDPOINT = Endpoint.from(HttpMethod.GET, "/stats/devices/seen", "start={start}&end={end}");
    private static final String baseUrl = "https://not.barracks.io";
    @MockBean
    private StatsResource statsResource;
    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper json;

    private RandomPrincipal principal = new RandomPrincipal();

    @Test
    public void documentGetLastSeenDevices() throws Exception {
        // Given
        final Endpoint endpoint = DEVICES_LAST_SEEN_ENDPOINT;
        json.enable(SerializationFeature.INDENT_OUTPUT);
        final DataSet expected = DataSet.builder().total(BigDecimal.valueOf(42L)).build();
        final String start = "1986-02-27T10:03:00.000Z";
        final String end = "1986-02-27T10:03:00.999Z";
        final OffsetDateTime startDate = OffsetDateTime.parse(start);
        final OffsetDateTime endDate = OffsetDateTime.parse(end);
        doReturn(expected).when(statsResource).getLastSeenDevices(principal, startDate, endDate);

        // When
        final ResultActions result = mvc.perform(
                RestDocumentationRequestBuilders.request(endpoint.getMethod(), endpoint.getPath())
                        .param("start", startDate.toString())
                        .param("end", endDate.toString())
                        .principal(principal)
        );

        // Then
        verify(statsResource).getLastSeenDevices(principal, startDate, endDate);
        result.andExpect(DataSetMatcher.from(expected));
        result.andExpect(status().isOk())
                .andDo(document(
                        "last-seen",
                        requestParameters(
                                parameterWithName("start").description("The date from when we want to have the devices").optional(),
                                parameterWithName("end").description("The end date for the search").optional()
                        ),
                        responseFields(
                                fieldWithPath("total").description("The number of devices last seen during the period")
                        )
                        )
                );
    }

    @Test
    public void getLastSeenDevices_shouldCallResource_andReturnResult() throws Exception {
        // Given
        final Endpoint endpoint = DEVICES_LAST_SEEN_ENDPOINT;
        final DataSet expected = RandomDataSet.create();
        final String start = "1986-02-27T10:03:00.000Z";
        final String end = "1986-02-27T10:03:00.999Z";
        final OffsetDateTime startDate = OffsetDateTime.parse(start);
        final OffsetDateTime endDate = OffsetDateTime.parse(end);
        doReturn(expected).when(statsResource).getLastSeenDevices(principal, startDate, endDate);

        // When
        final ResultActions result = mvc.perform(
                RestDocumentationRequestBuilders.request(endpoint.getMethod(), endpoint.getPath())
                        .param("start", startDate.toString())
                        .param("end", endDate.toString())
                        .principal(principal)
        );

        // Then
        verify(statsResource).getLastSeenDevices(principal, startDate, endDate);
        result.andExpect(DataSetMatcher.from(expected));
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.values[0].value").value(expected.getValues().get(0).getValue()))
                .andExpect(jsonPath("$.values[0].name").value(expected.getValues().get(0).getName()))
                .andExpect(jsonPath("$.total").value(expected.getTotal()));
    }

    @Test
    public void documentGetSeenDevices() throws Exception {
        // Given
        final Endpoint endpoint = DEVICES_SEEN_ENDPOINT;
        json.enable(SerializationFeature.INDENT_OUTPUT);
        final DataSet expected = DataSet.builder().total(BigDecimal.valueOf(42L)).build();
        final String start = "1986-02-27T10:03:00.000Z";
        final String end = "1986-02-27T10:03:00.999Z";
        final OffsetDateTime startDate = OffsetDateTime.parse(start);
        final OffsetDateTime endDate = OffsetDateTime.parse(end);
        doReturn(expected).when(statsResource).getSeenDevices(principal, startDate, endDate);

        // When
        final ResultActions result = mvc.perform(
                RestDocumentationRequestBuilders.request(endpoint.getMethod(), endpoint.getPath())
                        .param("start", startDate.toString())
                        .param("end", endDate.toString())
                        .principal(principal)
        );

        // Then
        verify(statsResource).getSeenDevices(principal, startDate, endDate);
        result.andExpect(DataSetMatcher.from(expected));
        result.andExpect(status().isOk())
                .andDo(document(
                        "seen",
                        requestParameters(
                                parameterWithName("start").description("The date from when we want to have the devices").optional(),
                                parameterWithName("end").description("The end date for the search").optional()
                        ),
                        responseFields(
                                fieldWithPath("total").description("The number of seen devices during the period")
                        )
                        )
                );
    }

    @Test
    public void getSeenDevices_shouldCallResource_andReturnResult() throws Exception {
        // Given
        final Endpoint endpoint = DEVICES_SEEN_ENDPOINT;
        final DataSet expected = RandomDataSet.create();
        final String start = "1986-02-27T10:03:00.000Z";
        final String end = "1986-02-27T10:03:00.999Z";
        final OffsetDateTime startDate = OffsetDateTime.parse(start);
        final OffsetDateTime endDate = OffsetDateTime.parse(end);
        doReturn(expected).when(statsResource).getSeenDevices(principal, startDate, endDate);

        // When
        final ResultActions result = mvc.perform(
                RestDocumentationRequestBuilders.request(endpoint.getMethod(), endpoint.getPath())
                        .param("start", startDate.toString())
                        .param("end", endDate.toString())
                        .principal(principal)
        );

        // Then
        verify(statsResource).getSeenDevices(principal, startDate, endDate);
        result.andExpect(DataSetMatcher.from(expected));
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.values[0].value").value(expected.getValues().get(0).getValue()))
                .andExpect(jsonPath("$.values[0].name").value(expected.getValues().get(0).getName()))
                .andExpect(jsonPath("$.total").value(expected.getTotal()));
    }

    private static class DataSetMatcher implements ResultMatcher {
        final ArrayList<ResultMatcher> matchers;

        private DataSetMatcher(DataSet expected) {
            this.matchers = new ArrayList<>(expected.getValues().size() + 1);
            matchers.add(jsonPath("$.total")
                    .value(isOneOf(expected.getTotal().longValue(), expected.getTotal().intValue(), expected.getTotal().floatValue(), expected.getTotal().doubleValue()))
            );
            for (DataSet.Metric stat : expected.getValues()) {
                final JSONArray longValue = new JSONArray();
                longValue.add(stat.getValue().longValue());
                final JSONArray doubleValue = new JSONArray();
                doubleValue.add(stat.getValue().doubleValue());
                final JSONArray bigValue = new JSONArray();
                bigValue.add(stat.getValue());
                matchers.add(
                        jsonPath("$.values[?(@.name == '%s')].value", stat.getName())
                                .value(anyOf(equalTo(longValue), equalTo(doubleValue), equalTo(bigValue)))
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
