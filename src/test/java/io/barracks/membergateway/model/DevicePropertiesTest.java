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
import com.google.common.collect.ImmutableMap;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@JsonTest
public class DevicePropertiesTest {
    @Autowired
    private ObjectMapper mapper;

    @Test
    public void serializeProperties_shouldNotSerializeInternalData() {
        // Given
        Map<String, Object> original = ImmutableMap.of("aProp", "aValue");
        DeviceProperties properties = DeviceProperties.builder().properties(original).build();

        // When
        JsonNode node = mapper.valueToTree(properties);

        // Then
        assertThat(node.get("properties")).isNull();
        assertThat(node.get("aProp").textValue()).isEqualTo(original.get("aProp"));
    }

    @Test
    public void serializeProperties_shouldNotIgnorePropertiesIfInMap() {
        // Given
        Map<String, Object> original = ImmutableMap.of("properties", "value");
        DeviceProperties properties = DeviceProperties.builder().properties(original).build();

        // When
        JsonNode node = mapper.valueToTree(properties);

        // Then
        assertThat(node.get("properties").textValue()).isEqualTo(original.get("properties"));
    }
}