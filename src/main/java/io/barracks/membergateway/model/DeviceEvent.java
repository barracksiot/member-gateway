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
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.springframework.hateoas.core.Relation;

import javax.validation.constraints.NotNull;
import java.util.*;

@Builder(toBuilder = true)
@Getter
@ToString
@EqualsAndHashCode
@Relation(collectionRelation = "events")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_ABSENT)
public class DeviceEvent {
    @NotNull
    private final String unitId;
    @NotNull
    private final String versionId;
    private final String segmentId;
    private final Date receptionDate;
    private final String deviceIP;
    @NotNull
    private final DeviceProperties additionalProperties;


    @JsonCreator
    public static DeviceEvent fromJson(@JsonProperty("additionalProperties") Map<String, Object> properties) {
        return DeviceEvent.builder()
                .additionalProperties(new DeviceProperties(Optional.ofNullable(properties)
                        .map(props -> (Map<String, Object>) new HashMap<>(props))
                        .orElse(Collections.emptyMap())
                ))
                .build();
    }

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSX")
    public Optional<Date> getReceptionDate() {
        return Optional.ofNullable(receptionDate)
                .map(date -> new Date(date.getTime()));
    }

    public Optional<DeviceProperties> getAdditionalProperties() {
        return Optional.ofNullable(additionalProperties)
                .map(props -> new DeviceProperties(props.getProperties()));
    }
}
