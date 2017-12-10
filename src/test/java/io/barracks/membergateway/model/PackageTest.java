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
import io.barracks.membergateway.utils.PackageUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.core.io.Resource;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@JsonTest
public class PackageTest {
    @Value("classpath:io/barracks/membergateway/model/package.json")
    private Resource pkgResource;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void serializeDeserialize_shouldReturnIdenticalObjects() throws Exception {
        // Given
        final Package aPackage = PackageUtils.getPackage();

        // When
        final Package result = objectMapper.readValue(objectMapper.writeValueAsString(aPackage), Package.class);

        // Then
        assertThat(result).isEqualTo(aPackage);
    }

    @Test
    public void deserialize_shouldMapFields() throws Exception {
        // Given
        JsonNode object = objectMapper.readTree(pkgResource.getInputStream());

        // When
        final Package aPackage = objectMapper.readValue(pkgResource.getInputStream(), Package.class);

        // Then
        assertThat(aPackage).hasNoNullFieldsOrProperties();
        assertThat(aPackage.getName()).isEqualTo(object.get("name").textValue());
        assertThat(aPackage.getReference()).isEqualTo(object.get("reference").textValue());
        assertThat(aPackage.getDescription()).isEqualTo(object.get("description").textValue());
    }
}
