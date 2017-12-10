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

package io.barracks.membergateway.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.barracks.membergateway.utils.DeviceUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.core.io.Resource;
import org.springframework.test.context.junit4.SpringRunner;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@JsonTest
public class DeviceTest {
    @Autowired
    private ObjectMapper objectMapper;
    @Value("classpath:io/barracks/membergateway/model/device.json")
    private Resource deviceResource;

    @Test
    public void serializeUnit_shouldCorrectlyRenderFields() throws Exception {
        // Given
        final DateFormat formatter = new SimpleDateFormat(Device.DATE_FORMAT);
        formatter.setTimeZone(TimeZone.getTimeZone("Z"));
        final Device device = DeviceUtils.getDevice();
        final String expectedDate = formatter.format(device.getFirstSeen().get());

        // When
        JsonNode node = objectMapper.valueToTree(device);

        // Then
        assertThat(node.get("unitId").asText()).isEqualTo(device.getUnitId());
        assertThat(node.get("firstSeen").asText()).isEqualTo(expectedDate);
        assertThat(node.get("lastEvent")).isInstanceOf(ObjectNode.class);
        assertThat(node.get("configuration")).isInstanceOf(ObjectNode.class);
    }

    @Test
    public void deserializeUnit_shouldCorrectlyIncludeFields() throws Exception {
        // Given
        JsonNode json = objectMapper.readTree(deviceResource.getInputStream());
        final DateFormat formatter = new SimpleDateFormat(Device.DATE_FORMAT);
        formatter.setTimeZone(TimeZone.getTimeZone("Z"));

        // When
        Device device = objectMapper.readValue(deviceResource.getInputStream(), Device.class);

        // Then
        assertThat(device.getFirstSeen()).hasValue(formatter.parse(json.get("firstSeen").textValue()));
        assertThat(device.getUnitId()).isEqualTo(json.get("unitId").textValue());
    }
}
