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
import io.barracks.membergateway.model.Version;
import io.barracks.membergateway.utils.VersionUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.boot.test.json.JsonContent;
import org.springframework.core.io.Resource;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@JsonTest
public class VersionJsonTest {

    @Autowired
    private JacksonTester<Version> json;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("classpath:io/barracks/membergateway/version-with-status.json")
    private Resource version;

    @Test
    public void deserialize_shouldReturnAllFieldsButStatus() throws Exception {
        // Given
        final Version expected = Version.builder()
                .id("versionId")
                .name("versionName")
                .description("versionDescription")
                .filename("versionFileName")
                .length(Long.parseLong("2957486"))
                .md5("Md915874")
                .addMetadata("truc", "machin")
                .build();
        assertThat(expected).hasNoNullFieldsOrPropertiesExcept("status");

        // When
        final Version result = json.readObject(version.getInputStream());

        // Then
        assertThat(result).isEqualTo(expected);
    }

    @Test
    public void serialize_shouldIncludeAllAttributes() throws Exception {
        // Given
        final Version source = VersionUtils.getVersion();

        // When
        final JsonContent<Version> result = json.write(source);

        // Then
        assertThat(result).extractingJsonPathValue("id").isEqualTo(source.getId());
        assertThat(result).extractingJsonPathValue("name").isEqualTo(source.getName());
        assertThat(result).extractingJsonPathValue("description").isEqualTo(source.getDescription());
        assertThat(result).extractingJsonPathValue("filename").isEqualTo(source.getFilename());
        assertThat(result).extractingJsonPathValue("length").isIn(source.getLength(), (int) source.getLength());
        assertThat(result).extractingJsonPathValue("md5").isEqualTo(source.getMd5());
        assertThat(result).extractingJsonPathValue("metadata").isEqualTo(source.getMetadata());
        assertThat(result).extractingJsonPathValue("status").isEqualTo(source.getStatus().getName());
    }
}
