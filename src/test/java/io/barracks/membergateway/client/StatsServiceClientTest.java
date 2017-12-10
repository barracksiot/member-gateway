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

package io.barracks.membergateway.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.barracks.commons.exceptions.BarracksServiceClientException;
import io.barracks.commons.util.Endpoint;
import io.barracks.membergateway.model.DataSet;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.core.io.Resource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.client.MockRestServiceServer;

import java.time.OffsetDateTime;
import java.util.UUID;

import static io.barracks.membergateway.client.StatsServiceClient.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@RunWith(SpringRunner.class)
@RestClientTest(StatsServiceClient.class)
public class StatsServiceClientTest {
    @Value("${io.barracks.deviceservice.base_url}")
    private String baseUrl;
    @Autowired
    private MockRestServiceServer mockServer;
    @Autowired
    private StatsServiceClient statsServiceClient;
    @Autowired
    private ObjectMapper mapper;
    @Value("classpath:io/barracks/membergateway/client/dataSet.json")
    private Resource dataSet;

    @Test
    public void getDevicePerVersionId_shouldReturnDataSet() throws Exception {
        // Given
        final Endpoint endpoint = DEVICES_PER_VERSION_ID_ENDPOINT;
        final String userId = UUID.randomUUID().toString();
        final DataSet expected = mapper.readValue(dataSet.getInputStream(), DataSet.class);
        mockServer.expect(method(endpoint.getMethod()))
                .andExpect(requestTo(endpoint.withBase(baseUrl).getURI(userId)))
                .andRespond(withSuccess().body(dataSet));

        // When
        final DataSet result = statsServiceClient.getDevicesPerVersionId(userId);

        // Then
        mockServer.verify();
        assertThat(result).isEqualTo(expected);
    }

    @Test
    public void getDevicePerVersionId_whenFails_shouldThrowException() {
        // Given
        final Endpoint endpoint = DEVICES_PER_VERSION_ID_ENDPOINT;
        final String userId = UUID.randomUUID().toString();
        mockServer.expect(method(endpoint.getMethod()))
                .andExpect(requestTo(endpoint.withBase(baseUrl).getURI(userId)))
                .andRespond(withBadRequest());

        // Then When
        assertThatExceptionOfType(BarracksServiceClientException.class).isThrownBy(() -> statsServiceClient.getDevicesPerVersionId(userId));
        mockServer.verify();
    }

    @Test
    public void getLastSeenDevices_shouldReturnDataSet() throws Exception {
        // Given
        final Endpoint endpoint = DEVICES_LAST_SEEN_ENDPOINT;
        final String userId = UUID.randomUUID().toString();
        final DataSet expected = mapper.readValue(dataSet.getInputStream(), DataSet.class);
        final String start = "1986-02-27T10:03:29.042Z";
        final String end = "2086-02-27T10:03:29.042Z";
        final OffsetDateTime startDate = OffsetDateTime.parse(start);
        final OffsetDateTime endDate = OffsetDateTime.parse(end);
        mockServer.expect(method(endpoint.getMethod()))
                .andExpect(requestTo(endpoint.withBase(baseUrl).getURI(userId, start, end)))
                .andRespond(withSuccess().body(dataSet));

        // When
        final DataSet result = statsServiceClient.getLastSeenDevices(userId, startDate, endDate);

        // Then
        mockServer.verify();
        assertThat(result).isEqualTo(expected);
    }

    @Test
    public void getLastSeenDevices_whenFails_shouldThrowException() throws Exception {
        // Given
        final Endpoint endpoint = DEVICES_LAST_SEEN_ENDPOINT;
        final String userId = UUID.randomUUID().toString();
        final String start = "1986-02-27T10:03:29.042Z";
        final String end = "2086-02-27T10:03:29.042Z";
        final OffsetDateTime startDate = OffsetDateTime.parse(start);
        final OffsetDateTime endDate = OffsetDateTime.parse(end);
        mockServer.expect(method(endpoint.getMethod()))
                .andExpect(requestTo(endpoint.withBase(baseUrl).getURI(userId, start, end)))
                .andRespond(withBadRequest());

        // Then When
        assertThatExceptionOfType(BarracksServiceClientException.class).isThrownBy(() -> statsServiceClient.getLastSeenDevices(userId, startDate, endDate));
        mockServer.verify();
    }

    @Test
    public void getSeenDevices_shouldReturnDataSet() throws Exception {
        // Given
        final Endpoint endpoint = DEVICES_SEEN_ENDPOINT;
        final String userId = UUID.randomUUID().toString();
        final DataSet expected = mapper.readValue(dataSet.getInputStream(), DataSet.class);
        final String start = "1986-02-27T10:03:29.042Z";
        final String end = "2086-02-27T10:03:29.042Z";
        final OffsetDateTime startDate = OffsetDateTime.parse(start);
        final OffsetDateTime endDate = OffsetDateTime.parse(end);
        mockServer.expect(method(endpoint.getMethod()))
                .andExpect(requestTo(endpoint.withBase(baseUrl).getURI(userId, start, end)))
                .andRespond(withSuccess().body(dataSet));

        // When
        final DataSet result = statsServiceClient.getSeenDevices(userId, startDate, endDate);

        // Then
        mockServer.verify();
        assertThat(result).isEqualTo(expected);
    }

    @Test
    public void getSeenDevices_whenFails_shouldThrowException() throws Exception {
        // Given
        final Endpoint endpoint = DEVICES_SEEN_ENDPOINT;
        final String userId = UUID.randomUUID().toString();
        final String start = "1986-02-27T10:03:29.042Z";
        final String end = "2086-02-27T10:03:29.042Z";
        final OffsetDateTime startDate = OffsetDateTime.parse(start);
        final OffsetDateTime endDate = OffsetDateTime.parse(end);
        mockServer.expect(method(endpoint.getMethod()))
                .andExpect(requestTo(endpoint.withBase(baseUrl).getURI(userId, start, end)))
                .andRespond(withBadRequest());

        // Then When
        assertThatExceptionOfType(BarracksServiceClientException.class).isThrownBy(() -> statsServiceClient.getSeenDevices(userId, startDate, endDate));
        mockServer.verify();
    }

}
