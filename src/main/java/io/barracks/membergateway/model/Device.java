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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.Date;
import java.util.Optional;

@Builder
@Getter
@ToString
@EqualsAndHashCode
public class Device {
    public static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSX";
    private final String unitId;
    private final Date firstSeen;
    private final DeviceEvent lastEvent;
    private final DeviceConfiguration configuration;

    @JsonCreator
    public static Device fromJson(
            @JsonProperty("firstSeen") Date firstSeen
    ) {
        return builder()
                .firstSeen(
                        Optional.ofNullable(firstSeen)
                                .map(date -> new Date(date.getTime()))
                                .orElse(null)
                )
                .build();
    }

    @JsonFormat(pattern = DATE_FORMAT)
    public Optional<Date> getFirstSeen() {
        return Optional.ofNullable(firstSeen).map(date -> new Date(date.getTime()));
    }

    public Optional<DeviceEvent> getLastEvent() {
        return Optional.ofNullable(lastEvent).map(event -> event.toBuilder().build());
    }

    public Optional<DeviceConfiguration> getConfiguration() {
        return Optional.ofNullable(configuration).map(conf -> conf.toBuilder().build());
    }
}
