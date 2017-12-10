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

import io.barracks.membergateway.client.DeviceServiceClient;
import io.barracks.membergateway.model.BarracksQuery;
import io.barracks.membergateway.model.Device;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.PagedResources;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

@Service
public class DeviceManager {

    private final DeviceServiceClient deviceServiceClient;

    @Autowired
    public DeviceManager(DeviceServiceClient deviceServiceClient) {
        this.deviceServiceClient = deviceServiceClient;
    }

    public Page<Device> getDevices(String userId, Pageable pageable, BarracksQuery query) {
        final PagedResources<Device> devices = deviceServiceClient.getDevices(userId, pageable, query);
        return new PageImpl<>(new ArrayList<>(devices.getContent()), pageable, devices.getMetadata().getTotalElements());
    }

    public Device getDeviceByUserIdAndUnitId(String userId, String unitId) {
        return deviceServiceClient.getDeviceByUserIdAndUnitId(userId, unitId);
    }

}
