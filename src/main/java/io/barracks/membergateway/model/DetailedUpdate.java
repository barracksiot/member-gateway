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

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;
import org.springframework.hateoas.core.Relation;

import javax.annotation.Nullable;
import java.util.Date;
import java.util.Optional;

@Builder
@Getter
@EqualsAndHashCode
@ToString
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Relation(collectionRelation = "updates")
public class DetailedUpdate {
    public static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSX";
    private final String uuid;

    private final String userId;
    private final Integer revisionId;
    private final String name;
    private final String description;
    private final PackageInfo packageInfo;
    private final AdditionalProperties additionalProperties;
    private final Date creationDate;
    private final UpdateStatus status;
    private final Segment segment;
    private final Date scheduledDate;

    public DetailedUpdate(Update update, PackageInfo packageInfo, @Nullable Segment segment) {
        this.uuid = update.getUuid();
        this.userId = update.getUserId();
        this.revisionId = update.getRevisionId();
        this.name = update.getName();
        this.description = update.getDescription();
        this.additionalProperties = update.getAdditionalProperties().orElse(null);
        this.creationDate = update.getCreationDate().orElse(null);
        this.status = update.getStatus();
        this.packageInfo = packageInfo;
        this.segment = segment;
        this.scheduledDate = update.getScheduledDate().orElse(null);
    }

    @JsonFormat(pattern = DATE_FORMAT)
    public Optional<Date> getCreationDate() {
        return Optional.ofNullable(creationDate)
                .map(date -> new Date(date.getTime()));
    }

    @JsonFormat(pattern = DATE_FORMAT)
    public Optional<Date> getScheduledDate() {
        return Optional.ofNullable(scheduledDate)
                .map(date -> new Date(date.getTime()));
    }

    public Optional<AdditionalProperties> getAdditionalProperties() {
        return Optional.ofNullable(additionalProperties);
    }
}
