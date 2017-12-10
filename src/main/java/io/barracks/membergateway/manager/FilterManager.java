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

package io.barracks.membergateway.manager;

import com.fasterxml.jackson.databind.JsonNode;
import io.barracks.membergateway.client.DeploymentServiceClient;
import io.barracks.membergateway.client.DeviceServiceClient;
import io.barracks.membergateway.exception.FilterInUseException;
import io.barracks.membergateway.model.BarracksQuery;
import io.barracks.membergateway.model.Device;
import io.barracks.membergateway.model.Filter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.PagedResources;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

@Service
public class FilterManager {
    private final DeviceServiceClient deviceServiceClient;
    private final DeploymentServiceClient deploymentServiceClient;

    @Autowired
    FilterManager(DeviceServiceClient deviceServiceClient, DeploymentServiceClient deploymentServiceClient) {
        this.deviceServiceClient = deviceServiceClient;
        this.deploymentServiceClient = deploymentServiceClient;
    }

    public Filter createFilter(String userId, Filter filter) {
        Filter createdFilter = deviceServiceClient.createFilter(userId, filter);
        return addDeploymentPlansAndDeviceCount(userId, createdFilter);
    }

    public Page<Filter> getFilters(String userId, Pageable pageable) {
        final PagedResources<Filter> filters = deviceServiceClient.getFilters(userId, pageable);
        return new PageImpl<>(new ArrayList<>(filters.getContent()), pageable, filters.getMetadata().getTotalElements())
                .map(source -> addDeploymentPlansAndDeviceCount(userId, source));
    }

    public Filter getFilterByUserIdAndName(String userId, String name) {
        Filter filter = deviceServiceClient.getFilterByUserIdAndName(userId, name);
        return addDeploymentPlansAndDeviceCount(userId, filter);
    }

    private Filter addDeploymentPlansAndDeviceCount(String userId, Filter filter) {
        return filter.toBuilder()
                .deploymentCount(getAssociatedDeploymentCount(userId, filter))
                .deviceCount(getAssociatedDevicesCount(userId, filter))
                .build();
    }

    long getAssociatedDeploymentCount(String userId, Filter filter) {
        Pageable pageable = new PageRequest(0, 1);
        return deploymentServiceClient.getDeploymentPlansByFilterName(userId, filter.getName(), pageable).getTotalElements();
    }

    long getAssociatedDevicesCount(String userId, Filter filter) {
        final Pageable pageable = new PageRequest(0, 5);
        final BarracksQuery barracksQuery = new BarracksQuery(filter.getQuery());
        final PagedResources<Device> associatedDevices = deviceServiceClient.getDevices(userId, pageable, barracksQuery);
        return associatedDevices.getMetadata().getTotalElements();
    }

    public void deleteFilter(String name, String userId) {
        Pageable pageable = new PageRequest(0, 5);
        if (deploymentServiceClient.getDeploymentPlansByFilterName(userId, name, pageable).getContent().isEmpty()) {
            deviceServiceClient.deleteFilter(userId, name);
        } else {
            throw new FilterInUseException("This filter cannot be deleted because it's used by a deployment plan.");
        }
    }

    public Filter updateFilter(String userId, String name, JsonNode query) {
        Filter updatedFilter = deviceServiceClient.updateFilter(userId, name, query);
        return addDeploymentPlansAndDeviceCount(userId, updatedFilter);
    }
}
