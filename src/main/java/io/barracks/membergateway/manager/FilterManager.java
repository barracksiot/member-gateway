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
import io.barracks.membergateway.model.Filter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.PagedResources;
import org.springframework.stereotype.Service;

import java.util.ArrayList;


@Service
public class FilterManager {
    private final DeviceServiceClient deviceServiceClient;

    public FilterManager(DeviceServiceClient deviceServiceClient) {
        this.deviceServiceClient = deviceServiceClient;
    }

    public Filter createFilter(String userId, Filter filter) {
        return deviceServiceClient.createFilter(userId, filter);
    }

    public Page<Filter> getFilters(String userId, Pageable pageable) {
        final PagedResources<Filter> filters = deviceServiceClient.getFilters(userId, pageable);
        return new PageImpl<>(new ArrayList<>(filters.getContent()), pageable, filters.getMetadata().getTotalElements());
    }

    public void deleteFilter(String name, String userId) {
        deviceServiceClient.deleteFilter(userId, name);
    }
}
