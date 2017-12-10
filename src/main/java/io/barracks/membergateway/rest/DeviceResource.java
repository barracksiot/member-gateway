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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.barracks.membergateway.exception.BarracksQueryFormatException;
import io.barracks.membergateway.manager.DeviceManager;
import io.barracks.membergateway.model.BarracksQuery;
import io.barracks.membergateway.model.Device;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.PagedResources;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.security.Principal;

@RestController
@RequestMapping("/devices")
public class DeviceResource {
    private final DeviceManager deviceManager;
    private final PagedResourcesAssembler<Device> devicePagedResourcesAssembler;
    private final ObjectMapper mapper;

    @Autowired
    public DeviceResource(ObjectMapper mapper, DeviceManager deviceManager, PagedResourcesAssembler<Device> devicePagedResourcesAssembler) {
        this.mapper = mapper;
        this.deviceManager = deviceManager;
        this.devicePagedResourcesAssembler = devicePagedResourcesAssembler;
    }

    @RequestMapping(method = RequestMethod.GET)
    public PagedResources<?> getDevices(@RequestParam(required = false, defaultValue = "") String query, Pageable pageable, Principal principal) {
        final Page<Device> devices;
        try {
            final JsonNode jsonQuery = StringUtils.isEmpty(query) ? null : mapper.readTree(query);
            devices = deviceManager.getDevices(principal.getName(), pageable, new BarracksQuery(jsonQuery));
            return devicePagedResourcesAssembler.toResource(devices);
        } catch (IOException e) {
            throw new BarracksQueryFormatException(query, e);
        }
    }

    @RequestMapping(method = RequestMethod.GET, path = "/{unitId}")
    public Device getDevice(Principal principal, @PathVariable("unitId") String unitId) {
        return deviceManager.getDeviceByUserIdAndUnitId(principal.getName(), unitId);
    }

}
