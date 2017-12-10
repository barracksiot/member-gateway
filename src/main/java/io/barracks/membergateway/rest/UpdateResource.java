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

import io.barracks.membergateway.manager.UpdateManager;
import io.barracks.membergateway.model.DetailedUpdate;
import io.barracks.membergateway.model.Update;
import io.barracks.membergateway.model.UpdateStatus;
import io.barracks.membergateway.model.UpdateStatusCompatibility;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.security.Principal;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/updates")
public class UpdateResource {

    @Autowired
    private UpdateManager updateManager;

    @Autowired
    private PagedResourcesAssembler<DetailedUpdate> assembler;

    @ResponseStatus(HttpStatus.OK)
    @RequestMapping(method = RequestMethod.GET)
    public PagedResources<Resource<DetailedUpdate>> getAllUpdates(
            Pageable pageable,
            Principal principal,
            @RequestParam(name = "status", required = false, defaultValue = "") String[] statuses
    ) {
        final List<UpdateStatus> updateStatuses = Arrays.asList(statuses).parallelStream().map(s -> UpdateStatus.fromName(s)).collect(Collectors.toList());
        final Page<DetailedUpdate> page = updateManager.getUpdatesByStatusesAndSegments(pageable, principal.getName(), updateStatuses, Collections.emptyList());
        return assembler.toResource(page);
    }

    @RequestMapping(method = RequestMethod.POST)
    public ResponseEntity<?> createUpdate(@Valid @RequestBody Update update, Principal principal) {
        final Update updateWithUser = update.toBuilder().userId(principal.getName()).build();
        final DetailedUpdate createdUpdate = updateManager.createUpdate(updateWithUser);
        return new ResponseEntity<>(createdUpdate, HttpStatus.CREATED);
    }

    @RequestMapping(method = RequestMethod.PUT, path = "/{uuid}")
    public ResponseEntity<?> editUpdate(@Valid @RequestBody Update update, @PathVariable("uuid") String uuid, Principal principal) {
        final Update updateWithUuidAndUserId = update.toBuilder().userId(principal.getName()).uuid(uuid).build();
        final DetailedUpdate updatedUpdate = updateManager.editUpdate(updateWithUuidAndUserId);
        return new ResponseEntity<>(updatedUpdate, HttpStatus.OK);
    }

    @ResponseStatus(HttpStatus.OK)
    @RequestMapping(method = RequestMethod.GET, path = "/{uuid}")
    public ResponseEntity<DetailedUpdate> getUpdateByUuidAndUserId(@PathVariable("uuid") String uuid, Principal principal) {
        final DetailedUpdate result = updateManager.getUpdateByUuidAndUserId(uuid, principal.getName());
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    @RequestMapping(method = RequestMethod.PUT, path = "/{uuid}/status/{status}")
    public ResponseEntity<?> changeUpdateStatus(
            @PathVariable("uuid") String uuid,
            @PathVariable("status") String status,
            Principal principal
    ) {
        final UpdateStatus updateStatus = UpdateStatus.fromName(status);
        updateManager.changeUpdateStatus(uuid, updateStatus, principal.getName());
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @RequestMapping(method = RequestMethod.PUT, path = "/{uuid}/status/scheduled")
    public ResponseEntity<?> scheduleUpdate(
            @PathVariable("uuid") String uuid,
            @RequestParam(name = "time") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime scheduledTime,
            Principal principal
    ) {
        updateManager.scheduleUpdatePublication(uuid, scheduledTime, principal.getName());
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @RequestMapping(path = "/status", method = RequestMethod.GET)
    public List<UpdateStatusCompatibility> getAllStatusesCompatibilities() {
        return updateManager.getAllStatusesCompatibilities();
    }

    @RequestMapping(path = "/status/{statusName}", method = RequestMethod.GET)
    public UpdateStatusCompatibility getStatusCompatibilities(@PathVariable String statusName) {
        return updateManager.getStatusCompatibilities(UpdateStatus.fromName(statusName));
    }
}
