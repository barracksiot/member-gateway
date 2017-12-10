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

package io.barracks.membergateway.rest;

import io.barracks.membergateway.manager.StatsManager;
import io.barracks.membergateway.model.DataSet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.time.OffsetDateTime;

@RestController
@RequestMapping("/stats")
public class StatsResource {
    static final OffsetDateTime DEFAULT_START = OffsetDateTime.MIN;
    static final OffsetDateTime DEFAULT_END = OffsetDateTime.MAX;
    private final StatsManager statsManager;

    @Autowired
    public StatsResource(StatsManager statsManager) {
        this.statsManager = statsManager;
    }

    @RequestMapping("/devices/perVersionId")
    public DataSet getDeviceCountPerVersionId(Principal principal) {
        return statsManager.getDevicesPerVersionId(principal.getName());
    }

    @RequestMapping("/devices/lastSeen")
    public DataSet getLastSeenDevices(
            Principal principal,
            @RequestParam(required = false, name = "start")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    OffsetDateTime start,
            @RequestParam(required = false, name = "end")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    OffsetDateTime end
    ) {
        if (start == null) {
            start = DEFAULT_START;
        }
        if (end == null) {
            end = DEFAULT_END;
        }
        return statsManager.getLastSeenDevices(principal.getName(), start, end);
    }

    @RequestMapping("/devices/seen")
    public DataSet getSeenDevices(
            Principal principal,
            @RequestParam(required = false, name = "start")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    OffsetDateTime start,
            @RequestParam(required = false, name = "end")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    OffsetDateTime end
    ) {
        if (start == null) {
            start = DEFAULT_START;
        }
        if (end == null) {
            end = DEFAULT_END;
        }
        return statsManager.getSeenDevices(principal.getName(), start, end);
    }

    @RequestMapping("/devices/perSegmentId")
    public DataSet getDevicesPerSegmentId(
            Principal principal,
            @RequestParam(name = "updated", defaultValue = "false", required = false)
                    boolean updated
    ) {
        if (updated) {
            return statsManager.getUpdatedDevicesPerSegmentId(principal.getName());
        } else {
            return statsManager.getDevicesPerSegmentId(principal.getName());
        }
    }

}
