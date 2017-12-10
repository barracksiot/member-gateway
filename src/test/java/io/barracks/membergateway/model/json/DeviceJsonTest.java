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

package io.barracks.membergateway.model.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.barracks.membergateway.model.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.boot.test.json.JsonContent;
import org.springframework.core.io.Resource;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@JsonTest
public class DeviceJsonTest {

    private static final String PACKAGE_1_REF = "07a982ce-f857-4005-88ab-24eae151379c";
    private static final String PACKAGE_1_VERSION = "cf39e929-87a9-4fae-b0ec-09223a9c80dd";
    private static final String PACKAGE_2_REF = "aa8e2d81-8938-4fa3-9d8c-8082e1b48875";
    private static final String PACKAGE_2_VERSION = "1194da10-95dc-46f4-b383-2e38baccc589";
    private static final String CUSTOM_DATA_1_KEY = "8452c182-f40a-4dc8-acf4-3f10512f39ea";
    private static final String CUSTOM_DATA_2_KEY = "6cfa6ed5-b23f-497c-aa9e-b9dce3c4fa42";
    private static final String CUSTOM_DATA_1_VALUE = "660c97b9-ef6a-46fb-8a27-c81f5fa212f3";
    private static final String CUSTOM_DATA_2_VALUE = "bb2675ea-89bd-4a0f-b4d8-217ffe2815c3";
    private static final String AVAILABLE_PACKAGE_REF = "41739a85-c38d-483a-9c33-f391497d5cbb";
    private static final String AVAILABLE_PACKAGE_VERSION = "64155ff9-4171-43a7-b723-830d661e973c";
    private static final String UNAVAILABLE_PACKAGE_REF = "a856045f-bcb4-42bb-8efa-2021a710e259";
    private static final String UNAVAILABLE_PACKAGE_VERSION = "347bb6b9-706d-4613-8c31-a0ca9f16db0e";
    private static final String CHANGED_PACKAGE_REF = "8cc1a0c5-c40a-41bb-8d10-76dc95caf752";
    private static final String CHANGED_PACKAGE_VERSION = "9b985ce3-4073-4d92-bb72-e7a4934a74cd";
    private static final String UNCHANGED_PACKAGE_REF = "674ae377-74d9-4b66-ba4c-81489a237153";
    private static final String UNCHANGED_PACKAGE_VERSION = "393b95b4-e930-40f6-a3e5-d5f97b0db990";
    private static final String DEVICE_UNIT_ID = "837013ae-ef90-48ce-8813-82a361204a66";
    private static final String RECEPTION_DATE = "2017-04-06T19:39:58.081Z";
    private static final String FIRST_SEEN = "2017-04-06T19:39:58.081Z";

    @Autowired
    private JacksonTester<Device> json;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("classpath:io/barracks/membergateway/device.json")
    private Resource device;

    @Test
    public void deserialize_shouldReturnCompleteObject() throws Exception {
        // Given
        final Device expected = buildDevice();

        // When
        final Device result = json.readObject(device.getInputStream());

        // Then
        assertThat(result).isEqualTo(expected);
    }

    @Test
    public void serialize_shouldIncludeAllAttributes() throws Exception {
        // Given
        final Device source = buildDevice();

        // When
        final JsonContent<Device> result = json.write(source);

        // Then
        assertThat(result).extractingJsonPathValue("unitId").isEqualTo(source.getUnitId());
        assertThat(result).extractingJsonPathValue("lastEvent.request.customClientData." + CUSTOM_DATA_1_KEY).isEqualTo(CUSTOM_DATA_1_VALUE);
        assertThat(result).extractingJsonPathValue("lastEvent.request.customClientData." + CUSTOM_DATA_2_KEY).isEqualTo(CUSTOM_DATA_2_VALUE);
        assertThat(result).extractingJsonPathValue("lastEvent.request.packages[0].reference").isEqualTo(PACKAGE_1_REF);
        assertThat(result).extractingJsonPathValue("lastEvent.request.packages[0].version").isEqualTo(PACKAGE_1_VERSION);
        assertThat(result).extractingJsonPathValue("lastEvent.request.packages[1].reference").isEqualTo(PACKAGE_2_REF);
        assertThat(result).extractingJsonPathValue("lastEvent.request.packages[1].version").isEqualTo(PACKAGE_2_VERSION);
        assertThat(result).extractingJsonPathValue("lastEvent.response.available[0].reference").isEqualTo(AVAILABLE_PACKAGE_REF);
        assertThat(result).extractingJsonPathValue("lastEvent.response.available[0].version").isEqualTo(AVAILABLE_PACKAGE_VERSION);
        assertThat(result).extractingJsonPathValue("lastEvent.response.unavailable[0].reference").isEqualTo(UNAVAILABLE_PACKAGE_REF);
        assertThat(result).extractingJsonPathValue("lastEvent.response.unavailable[0].version").isEqualTo(UNAVAILABLE_PACKAGE_VERSION);
        assertThat(result).extractingJsonPathValue("lastEvent.response.changed[0].reference").isEqualTo(CHANGED_PACKAGE_REF);
        assertThat(result).extractingJsonPathValue("lastEvent.response.changed[0].version").isEqualTo(CHANGED_PACKAGE_VERSION);
        assertThat(result).extractingJsonPathValue("lastEvent.response.unchanged[0].reference").isEqualTo(UNCHANGED_PACKAGE_REF);
        assertThat(result).extractingJsonPathValue("lastEvent.response.unchanged[0].version").isEqualTo(UNCHANGED_PACKAGE_VERSION);
        assertThat(result).extractingJsonPathValue("lastEvent.receptionDate").isEqualTo(source.getLastEvent().get().getReceptionDate().get().toString());
        assertThat(result).extractingJsonPathValue("firstSeen").isEqualTo(source.getFirstSeen().get().toString());
    }

    private Device buildDevice() {
        final List<DevicePackage> packages = Arrays.asList(
                DevicePackage.builder()
                        .reference(PACKAGE_1_REF)
                        .version(PACKAGE_1_VERSION)
                        .build(),
                DevicePackage.builder()
                        .reference(PACKAGE_2_REF)
                        .version(PACKAGE_2_VERSION)
                        .build()
        );
        final DeviceRequest request = DeviceRequest.builder()
                .addCustomClientData(CUSTOM_DATA_1_KEY, CUSTOM_DATA_1_VALUE)
                .addCustomClientData(CUSTOM_DATA_2_KEY, CUSTOM_DATA_2_VALUE)
                .packages(packages)
                .build();
        final DeviceResponse response = DeviceResponse.builder()
                .addAvailable(DevicePackage.builder().reference(AVAILABLE_PACKAGE_REF).version(AVAILABLE_PACKAGE_VERSION).build())
                .addUnavailable(DevicePackage.builder().reference(UNAVAILABLE_PACKAGE_REF).version(UNAVAILABLE_PACKAGE_VERSION).build())
                .addChanged(DevicePackage.builder().reference(CHANGED_PACKAGE_REF).version(CHANGED_PACKAGE_VERSION).build())
                .addUnchanged(DevicePackage.builder().reference(UNCHANGED_PACKAGE_REF).version(UNCHANGED_PACKAGE_VERSION).build())
                .build();
        return Device.builder()
                .unitId(DEVICE_UNIT_ID)
                .lastEvent(DeviceEvent.builder()
                        .request(request)
                        .response(response)
                        .receptionDate(OffsetDateTime.parse(RECEPTION_DATE))
                        .build())
                .firstSeen(OffsetDateTime.parse(FIRST_SEEN))
                .build();
    }
}
