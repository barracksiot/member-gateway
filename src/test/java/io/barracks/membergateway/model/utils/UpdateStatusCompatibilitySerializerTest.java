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

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
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
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(SpringRunner.class)
@JsonTest
public class UpdateStatusCompatibilitySerializerTest {
    @Autowired
    private ObjectMapper mapper;

    private StringWriter stringWriter;
    private JsonGenerator generator;
    private UpdateStatusCompatibilitySerializer serializer;
    private StringBuilder builder;

    @Before
    public void setUp() throws IOException {
        this.stringWriter = new StringWriter();
        this.generator = new JsonFactory().createGenerator(stringWriter);
        this.serializer = new UpdateStatusCompatibilitySerializer();
        this.builder = new StringBuilder();
    }

    @Test
    public void testSerializeStatusCompatibleWithOneStatus() throws IOException {
        final UpdateStatus status = UpdateStatus.ARCHIVED;
        final List<UpdateStatus> compatibilities = Collections.singletonList(UpdateStatus.SCHEDULED);
        serializer.serialize(new UpdateStatusCompatibility(status, compatibilities), generator, null);

        generator.flush();

        String serializedStatus = stringWriter.toString();
        builder.append(serializedStatus);
        String jsonString = builder.toString();

        assertNotNull(serializedStatus);
        assertEquals("{\"name\":\"archived\",\"compatibleStatus\":[\"scheduled\"]}", jsonString);
    }

    @Test
    public void testSerializeStatusCompatibleWithTwoStatuses() throws IOException {
        final UpdateStatus status = UpdateStatus.DRAFT;
        final List<UpdateStatus> compatibilities = Arrays.asList(UpdateStatus.DRAFT, UpdateStatus.PUBLISHED);
        serializer.serialize(new UpdateStatusCompatibility(status, compatibilities), generator, null);

        generator.flush();
        String serializedStatus = stringWriter.toString();
        builder.append(serializedStatus);
        String jsonString = builder.toString();

        assertNotNull(serializedStatus);
        assertEquals("{\"name\":\"draft\",\"compatibleStatus\":[\"draft\",\"published\"]}", jsonString);
    }

    @Test
    public void testSerializeStatusCompatibleWithThreeStatuses() throws IOException {
        final UpdateStatus status = UpdateStatus.SCHEDULED;
        final List<UpdateStatus> compatibilities = Arrays.asList(UpdateStatus.ARCHIVED, UpdateStatus.PUBLISHED, UpdateStatus.DRAFT);
        serializer.serialize(new UpdateStatusCompatibility(status, compatibilities), generator, null);

        generator.flush();
        String serializedStatus = stringWriter.toString();
        builder.append(serializedStatus);
        String jsonString = builder.toString();

        assertNotNull(serializedStatus);
        assertEquals("{\"name\":\"scheduled\",\"compatibleStatus\":[\"archived\",\"published\",\"draft\"]}", jsonString);
    }

    @Test
    public void testSerializeStatusCompatibleWithFourStatuses() throws IOException {
        final UpdateStatus status = UpdateStatus.PUBLISHED;
        final List<UpdateStatus> compatibilities = Arrays.asList(UpdateStatus.ARCHIVED, UpdateStatus.SCHEDULED, UpdateStatus.PUBLISHED, UpdateStatus.DRAFT);
        serializer.serialize(new UpdateStatusCompatibility(status, compatibilities), generator, null);

        generator.flush();
        String serializedStatus = stringWriter.toString();
        builder.append(serializedStatus);
        String jsonString = builder.toString();

        assertNotNull(serializedStatus);
        assertEquals("{\"name\":\"published\",\"compatibleStatus\":[\"archived\",\"scheduled\",\"published\",\"draft\"]}", jsonString);
    }
}
