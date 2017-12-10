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

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.barracks.membergateway.exception.UnknownUpdateStatusException;
import io.barracks.membergateway.model.utils.UpdateStatusDeserializer;
import io.barracks.membergateway.model.utils.UpdateStatusSerializer;

import java.util.HashMap;
import java.util.Map;

@JsonSerialize(using = UpdateStatusSerializer.class)
@JsonDeserialize(using = UpdateStatusDeserializer.class)
public enum UpdateStatus {
    DRAFT("draft"),
    PUBLISHED("published"),
    ARCHIVED("archived"),
    SCHEDULED("scheduled");

    private static final Map<String, UpdateStatus> valueMap;

    static {
        valueMap = new HashMap<>();
        for (UpdateStatus status : UpdateStatus.values()) {
            valueMap.put(status.getName(), status);
        }
    }

    private String name;

    UpdateStatus(String name) {
        this.name = name;
    }

    public static UpdateStatus fromName(String statusName) {
        UpdateStatus statusFound = valueMap.get(statusName);
        if (statusFound == null) {
            throw new UnknownUpdateStatusException(statusName);
        }
        return statusFound;
    }

    public String getName() {
        return this.name;
    }
}
