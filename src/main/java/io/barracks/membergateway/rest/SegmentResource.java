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

import io.barracks.membergateway.manager.SegmentManager;
import io.barracks.membergateway.manager.UpdateManager;
import io.barracks.membergateway.model.DetailedUpdate;
import io.barracks.membergateway.model.Device;
import io.barracks.membergateway.model.Segment;
import io.barracks.membergateway.rest.entity.SegmentsOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/segments")
public class SegmentResource {
    private final SegmentManager segmentManager;
    private final UpdateManager updateManager;
    private final PagedResourcesAssembler<Segment> segmentPagedResourcesAssembler;
    private final PagedResourcesAssembler<Device> devicePagedResourcesAssembler;
    private final PagedResourcesAssembler<DetailedUpdate> detailedUpdatePagedResourcesAssembler;

    @Autowired
    public SegmentResource(
            SegmentManager segmentManager,
            UpdateManager updateManager,
            PagedResourcesAssembler<Segment> segmentPagedResourcesAssembler,
            PagedResourcesAssembler<DetailedUpdate> detailedUpdatePagedResourcesAssembler,
            PagedResourcesAssembler<Device> devicePagedResourcesAssembler) {
        this.segmentManager = segmentManager;
        this.updateManager = updateManager;
        this.segmentPagedResourcesAssembler = segmentPagedResourcesAssembler;
        this.detailedUpdatePagedResourcesAssembler = detailedUpdatePagedResourcesAssembler;
        this.devicePagedResourcesAssembler = devicePagedResourcesAssembler;
    }

    @RequestMapping(method = RequestMethod.POST)
    public Segment createSegment(@RequestBody Segment segment, Principal authentication) {
        return segmentManager.createSegment(authentication.getName(), segment);
    }

    @RequestMapping(method = RequestMethod.PUT, path = "/{id}")
    public Segment updateSegment(@PathVariable("id") String segmentId, @RequestBody Segment segment, Principal authentication) {
        return segmentManager.updateSegment(authentication.getName(), segmentId, segment);
    }

    @RequestMapping(method = RequestMethod.GET, path = "/{id}")
    public Segment getSegment(@PathVariable("id") String segmentId, Principal authentication) {
        return segmentManager.getSegmentForUser(authentication.getName(), segmentId);
    }

    @RequestMapping
    public PagedResources<Resource<Segment>> getSegments(Pageable pageable, Principal authentication) {
        return segmentPagedResourcesAssembler.toResource(segmentManager.getSegmentsForUser(authentication.getName(), pageable));
    }

    @RequestMapping(method = RequestMethod.GET, path = "/{id}/devices")
    public PagedResources<Resource<Device>> getSegmentDevices(@PathVariable("id") String segmentId, Pageable pageable, Principal authentication) {
        return devicePagedResourcesAssembler.toResource(segmentManager.getDevicesBySegment(authentication.getName(), segmentId, pageable));
    }

    @RequestMapping(method = RequestMethod.GET, path = "/{id}/updates")
    public PagedResources<Resource<DetailedUpdate>> getSegmentUpdates(@PathVariable("id") String segmentId, Pageable pageable, Principal authentication) {
        return detailedUpdatePagedResourcesAssembler.toResource(
                updateManager.getUpdatesByStatusesAndSegments(pageable, authentication.getName(), Collections.emptyList(), Collections.singletonList(segmentId))
        );
    }

    @RequestMapping(method = RequestMethod.GET, path = "/order")
    public SegmentsOrder getOrderedSegments(Principal authentication) {
        return segmentManager.getOrderedSegments(authentication.getName());
    }

    @RequestMapping(method = RequestMethod.POST, path = "/order")
    public List<String> updateSegmentsOrder(@RequestBody List<String> order, Principal authentication) {
        return segmentManager.updateSegmentsOrder(authentication.getName(), order);
    }
}
