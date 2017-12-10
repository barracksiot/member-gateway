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

package io.barracks.membergateway.utils;

import io.barracks.membergateway.model.AdditionalProperties;
import io.barracks.membergateway.model.DetailedUpdate;
import io.barracks.membergateway.model.Update;
import io.barracks.membergateway.model.UpdateStatus;

import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class UpdateUtils {

    public static Update.UpdateBuilder getPredefinedCreateUpdateRequestBuilder() {
        return Update.builder()
                .name("Name")
                .description("description")
                .packageId(UUID.randomUUID().toString())
                .segmentId(UUID.randomUUID().toString())
                .additionalProperties(new AdditionalProperties());
    }

    public static Update.UpdateBuilder getPredefinedCreatedUpdateBuilder(String userId) {
        return getPredefinedCreateUpdateRequestBuilder()
                .uuid(UUID.randomUUID().toString())
                .userId(userId)
                .creationDate(new Date())
                .status(UpdateStatus.DRAFT)
                .revisionId(1);
    }

    public static Update getUpdate() {
        final Update update = Update.builder()
                .userId(UUID.randomUUID().toString())
                .name(UUID.randomUUID().toString())
                .description(UUID.randomUUID().toString())
                .packageId(UUID.randomUUID().toString())
                .segmentId(UUID.randomUUID().toString())
                .creationDate(new Date())
                .scheduledDate(new Date())
                .revisionId(42)
                .status(UpdateStatus.DRAFT)
                .uuid(UUID.randomUUID().toString())
                .additionalProperties(new AdditionalProperties())
                .build();
        assertThat(update).hasNoNullFieldsOrProperties();
        return update;
    }

    public static DetailedUpdate getDetailedUpdate() {
        final DetailedUpdate update = DetailedUpdate.builder()
                .userId(UUID.randomUUID().toString())
                .uuid(UUID.randomUUID().toString())
                .name(UUID.randomUUID().toString())
                .description(UUID.randomUUID().toString())
                .revisionId(42)
                .status(UpdateStatus.DRAFT)
                .additionalProperties(new AdditionalProperties())
                .packageInfo(PackageInfoUtils.getPackageInfo())
                .creationDate(new Date())
                .scheduledDate(new Date())
                .segment(SegmentUtils.getSegment())
                .build();
        assertThat(update).hasNoNullFieldsOrProperties();
        return update;
    }

}
