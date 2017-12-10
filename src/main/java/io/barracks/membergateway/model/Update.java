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

import com.fasterxml.jackson.annotation.*;
import lombok.*;
import org.hibernate.validator.constraints.NotBlank;

import javax.validation.constraints.NotNull;
import java.util.*;

@Builder(toBuilder = true)
@EqualsAndHashCode
@ToString
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class Update {

    public static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSX";

    private final String userId;

    private final Integer revisionId;

    @JsonFormat(pattern = DATE_FORMAT)
    private final Date scheduledDate;

    @NotBlank
    private final String name;

    private final String description;

    @NotBlank
    private final String packageId;

    private final String segmentId;

    @NotNull
    private final AdditionalProperties additionalProperties;

    @JsonFormat(pattern = DATE_FORMAT)
    private final Date creationDate;

    private final String uuid;

    private UpdateStatus status;

    @JsonCreator
    public static Update fromJson(
            @JsonProperty("creationDate") Date creationDate,
            @JsonProperty("scheduledDate") Date scheduledDate,
            @JsonProperty("additionalProperties") Map<String, Object> properties
    ) {
        return Update.builder()
                .creationDate(
                        Optional.ofNullable(creationDate)
                                .map(date -> new Date(date.getTime()))
                                .orElse(null)
                )
                .additionalProperties(new AdditionalProperties(
                        Optional.ofNullable(properties)
                                .map(props -> (Map<String, Object>) new HashMap<>(props))
                                .orElse(Collections.emptyMap())
                ))
                .scheduledDate(
                        Optional.ofNullable(scheduledDate)
                                .map(date -> new Date(date.getTime()))
                                .orElse(null)
                )
                .build();
    }

    public Optional<Date> getCreationDate() {
        return Optional.ofNullable(creationDate)
                .map(date -> new Date(date.getTime()));
    }

    public Optional<Date> getScheduledDate() {
        return Optional.ofNullable(scheduledDate)
                .map(date -> new Date(date.getTime()));
    }

    public Optional<AdditionalProperties> getAdditionalProperties() {
        return Optional.ofNullable(additionalProperties);
    }

    @JsonIgnore
    public boolean hasSegment() {
        return segmentId != null;
    }

}