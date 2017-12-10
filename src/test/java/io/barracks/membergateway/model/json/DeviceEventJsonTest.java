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
import io.barracks.membergateway.model.DeviceEvent;
import io.barracks.membergateway.model.DevicePackage;
import io.barracks.membergateway.model.DeviceRequest;
import io.barracks.membergateway.model.DeviceResponse;
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
public class DeviceEventJsonTest {

    private static final String PACKAGE_1_REF = "71d56f26-0106-4763-894f-60e3e9a592e7";
    private static final String PACKAGE_1_VERSION = "13c38c67-17b2-4b57-83a1-88ac849a660b";
    private static final String PACKAGE_2_REF = "da57ab88-b378-4f15-b54d-8ae1af09080d";
    private static final String PACKAGE_2_VERSION = "a4954952-5dfa-43f5-9e1d-de8efdc92e75";
    private static final String CUSTOM_DATA_1_KEY = "28ee7ff9-c8ad-473d-8a9d-0233008105d9";
    private static final String CUSTOM_DATA_2_KEY = "80f7acea-f9c7-43a4-be88-122bea04614c";
    private static final String CUSTOM_DATA_1_VALUE = "31ac8365-0ad5-459a-ad8d-86856e8c1794";
    private static final String CUSTOM_DATA_2_VALUE = "8932ea04-a6b0-4da9-8bcd-f03252075729";
    private static final String AVAILABLE_PACKAGE_REF = "0038e4eb-20e2-4d6f-b1b2-38f02c53d261";
    private static final String AVAILABLE_PACKAGE_VERSION = "172e7a63-1ed1-4c75-bcf5-41bbf8a51676";
    private static final String UNAVAILABLE_PACKAGE_REF = "03834446-2d0c-49d5-8c61-19f9fac39ec9";
    private static final String UNAVAILABLE_PACKAGE_VERSION = "14a9638d-2376-42e1-893c-7ddf73ec039d";
    private static final String CHANGED_PACKAGE_REF = "f18de6db-17e8-492a-85ed-4162f068d500";
    private static final String CHANGED_PACKAGE_VERSION = "9d2ca75f-2e86-43f7-bb0e-b7ba0624c151";
    private static final String UNCHANGED_PACKAGE_REF = "a0427678-89c9-4bab-8153-7156e743e680";
    private static final String UNCHANGED_PACKAGE_VERSION = "50a5839b-50a5-4cb8-ae38-e8d9f9a5dfc3";
    private static final String RECEPTION_DATE = "2017-04-06T19:39:57.833Z";

    @Autowired
    private JacksonTester<DeviceEvent> json;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("classpath:io/barracks/membergateway/deviceEvent.json")
    private Resource device;

    @Test
    public void deserialize_shouldIgnore() throws Exception {
        // Given
        final DeviceEvent expected = buildDevice();

        // When
        final DeviceEvent result = json.readObject(device.getInputStream());

        // Then
        assertThat(result).isEqualTo(expected);
    }

    @Test
    public void serialize_shouldIncludeAllAttributes() throws Exception {
        // Given
        final DeviceEvent source = buildDevice();

        // When
        final JsonContent<DeviceEvent> result = json.write(source);

        // Then
        assertThat(result).extractingJsonPathValue("request.customClientData." + CUSTOM_DATA_1_KEY).isEqualTo(CUSTOM_DATA_1_VALUE);
        assertThat(result).extractingJsonPathValue("request.customClientData." + CUSTOM_DATA_2_KEY).isEqualTo(CUSTOM_DATA_2_VALUE);
        assertThat(result).extractingJsonPathValue("request.packages[0].reference").isEqualTo(PACKAGE_1_REF);
        assertThat(result).extractingJsonPathValue("request.packages[0].version").isEqualTo(PACKAGE_1_VERSION);
        assertThat(result).extractingJsonPathValue("request.packages[1].reference").isEqualTo(PACKAGE_2_REF);
        assertThat(result).extractingJsonPathValue("request.packages[1].version").isEqualTo(PACKAGE_2_VERSION);
        assertThat(result).extractingJsonPathValue("response.available[0].reference").isEqualTo(AVAILABLE_PACKAGE_REF);
        assertThat(result).extractingJsonPathValue("response.available[0].version").isEqualTo(AVAILABLE_PACKAGE_VERSION);
        assertThat(result).extractingJsonPathValue("response.unavailable[0].reference").isEqualTo(UNAVAILABLE_PACKAGE_REF);
        assertThat(result).extractingJsonPathValue("response.unavailable[0].version").isEqualTo(UNAVAILABLE_PACKAGE_VERSION);
        assertThat(result).extractingJsonPathValue("response.changed[0].reference").isEqualTo(CHANGED_PACKAGE_REF);
        assertThat(result).extractingJsonPathValue("response.changed[0].version").isEqualTo(CHANGED_PACKAGE_VERSION);
        assertThat(result).extractingJsonPathValue("response.unchanged[0].reference").isEqualTo(UNCHANGED_PACKAGE_REF);
        assertThat(result).extractingJsonPathValue("response.unchanged[0].version").isEqualTo(UNCHANGED_PACKAGE_VERSION);
        assertThat(result).extractingJsonPathValue("receptionDate").isEqualTo(source.getReceptionDate().get().toString());
    }

    private DeviceEvent buildDevice() {
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
        return DeviceEvent.builder()
                .request(request)
                .response(response)
                .receptionDate(OffsetDateTime.parse(RECEPTION_DATE))
                .build();
    }
}
