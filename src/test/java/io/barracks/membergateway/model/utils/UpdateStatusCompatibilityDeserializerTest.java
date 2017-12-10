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

package io.barracks.membergateway.model.utils;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.barracks.membergateway.model.UpdateStatus;
import io.barracks.membergateway.model.UpdateStatusCompatibility;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Java6Assertions.assertThat;

@RunWith(SpringRunner.class)
@JsonTest
public class UpdateStatusCompatibilityDeserializerTest {
    @Autowired
    private ObjectMapper mapper;

    private DeserializationContext ctx;
    private UpdateStatusCompatibilityDeserializer deserializer;

    @Before
    public void setUp() {
        ctx = mapper.getDeserializationContext();
        deserializer = new UpdateStatusCompatibilityDeserializer();
    }

    @Test
    public void testDeserializeStatusCompatibleWithOneStatus() throws IOException {
        final UpdateStatus status = UpdateStatus.ARCHIVED;
        final List<UpdateStatus> compatibilities = Collections.singletonList(UpdateStatus.SCHEDULED);
        final String serializedStatusCompatibility = "{\n" +
                "  \"name\": \"" + status.getName() + "\",\n" +
                "  \"compatibleStatus\": [\n" +
                "    \"" + compatibilities.get(0).getName() + "\"\n" +
                "  ]\n" +
                "}";
        final JsonParser parser = mapper.getFactory().createParser(serializedStatusCompatibility);

        UpdateStatusCompatibility compatibility = deserializer.deserialize(parser, ctx);

        assertThat(compatibility).isNotNull();
        assertThat(compatibility.getStatus()).isEqualTo(status);
        assertThat(compatibility.getCompatibilities()).containsExactlyElementsOf(compatibilities);
    }

    @Test
    public void testDeserializeStatusCompatibleWithTwoStatuses() throws IOException {
        final UpdateStatus status = UpdateStatus.DRAFT;
        final List<UpdateStatus> compatibilities = Arrays.asList(UpdateStatus.DRAFT, UpdateStatus.PUBLISHED);
        final String serializedStatusCompatibility = "{\n" +
                "  \"name\": \"" + status.getName() + "\",\n" +
                "  \"compatibleStatus\": [\n" +
                "    \"" + compatibilities.get(0).getName() + "\",\n" +
                "    \"" + compatibilities.get(1).getName() + "\"\n" +
                "  ]\n" +
                "}";
        final JsonParser parser = mapper.getFactory().createParser(serializedStatusCompatibility);

        UpdateStatusCompatibility compatibility = deserializer.deserialize(parser, ctx);

        assertThat(compatibility).isNotNull();
        assertThat(compatibility.getStatus()).isEqualTo(status);
        assertThat(compatibility.getCompatibilities()).containsExactlyElementsOf(compatibilities);
    }

    @Test
    public void testDeserializeStatusCompatibleWithThreeStatuses() throws IOException {
        final UpdateStatus status = UpdateStatus.SCHEDULED;
        final List<UpdateStatus> compatibilities = Arrays.asList(UpdateStatus.ARCHIVED, UpdateStatus.PUBLISHED, UpdateStatus.DRAFT);
        final String serializedStatusCompatibility = "{\n" +
                "  \"name\": \"" + status.getName() + "\",\n" +
                "  \"compatibleStatus\": [\n" +
                "    \"" + compatibilities.get(0).getName() + "\",\n" +
                "    \"" + compatibilities.get(1).getName() + "\",\n" +
                "    \"" + compatibilities.get(2).getName() + "\"\n" +
                "  ]\n" +
                "}";
        final JsonParser parser = mapper.getFactory().createParser(serializedStatusCompatibility);

        UpdateStatusCompatibility compatibility = deserializer.deserialize(parser, ctx);

        assertThat(compatibility).isNotNull();
        assertThat(compatibility.getStatus()).isEqualTo(status);
        assertThat(compatibility.getCompatibilities()).containsExactlyElementsOf(compatibilities);
    }

    @Test
    public void testDeserializeStatusCompatibleWithFourStatuses() throws IOException {
        final UpdateStatus status = UpdateStatus.PUBLISHED;
        final List<UpdateStatus> compatibilities = Arrays.asList(UpdateStatus.ARCHIVED, UpdateStatus.SCHEDULED, UpdateStatus.PUBLISHED, UpdateStatus.DRAFT);
        final String serializedStatusCompatibility = "{\n" +
                "  \"name\": \"" + status.getName() + "\",\n" +
                "  \"compatibleStatus\": [\n" +
                "    \"" + compatibilities.get(0).getName() + "\",\n" +
                "    \"" + compatibilities.get(1).getName() + "\",\n" +
                "    \"" + compatibilities.get(2).getName() + "\",\n" +
                "    \"" + compatibilities.get(3).getName() + "\"\n" +
                "  ]\n" +
                "}";
        final JsonParser parser = mapper.getFactory().createParser(serializedStatusCompatibility);

        UpdateStatusCompatibility compatibility = deserializer.deserialize(parser, ctx);

        assertThat(compatibility).isNotNull();
        assertThat(compatibility.getStatus()).isEqualTo(status);
        assertThat(compatibility.getCompatibilities()).containsExactlyElementsOf(compatibilities);
    }
}
