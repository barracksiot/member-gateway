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

import io.barracks.membergateway.manager.DeviceConfigurationManager;
import io.barracks.membergateway.manager.DeviceEventManager;
import io.barracks.membergateway.model.DeviceConfiguration;
import io.barracks.membergateway.model.DeviceEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/devices/configuration")
public class DeviceConfigurationResource {

    private DeviceConfigurationManager deviceConfigurationManager;

    @Autowired
    public DeviceConfigurationResource(DeviceConfigurationManager deviceConfigurationManager) {
        this.deviceConfigurationManager = deviceConfigurationManager;
    }

    @RequestMapping(method = RequestMethod.GET, path = "/{unitId}")
    public ResponseEntity<?> getDeviceConfiguration(Principal principal, @PathVariable("unitId") String unitId) {
        final DeviceConfiguration configuration = deviceConfigurationManager.getConfiguration(principal.getName(), unitId);
        return new ResponseEntity<>(configuration, HttpStatus.OK);
    }

    @RequestMapping(method = RequestMethod.POST, path = "/{unitId}")
    public ResponseEntity<?> setDeviceConfiguration(Principal principal, @PathVariable("unitId") String unitId, @RequestBody DeviceConfiguration configuration) {
        final DeviceConfiguration result = deviceConfigurationManager.setConfiguration(principal.getName(), unitId, configuration);
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

}
