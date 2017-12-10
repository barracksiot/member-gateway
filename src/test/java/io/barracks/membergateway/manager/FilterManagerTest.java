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

import io.barracks.commons.test.PagedResourcesUtils;
import io.barracks.membergateway.client.DeviceServiceClient;
import io.barracks.membergateway.model.Filter;
import io.barracks.membergateway.utils.FilterUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.PagedResources;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class FilterManagerTest {
    @Mock
    private DeviceServiceClient deviceServiceClient;
    @Mock

    private FilterManager filterManager;

    @Before
    public void setup() {
        filterManager = new FilterManager(deviceServiceClient);
    }

    @Test
    public void createFilter_shouldCallRepository_andReturnResult() {
        // Given
        final String userId = UUID.randomUUID().toString();
        final Filter toCreate = FilterUtils.getFilter();
        final Filter expected = FilterUtils.getFilter();
        doReturn(expected).when(deviceServiceClient).createFilter(userId, toCreate);

        // When
        final Filter result = filterManager.createFilter(userId, toCreate);

        // Then
        verify(deviceServiceClient).createFilter(userId, toCreate);
        assertThat(result).isEqualTo(expected);
    }

    @Test
    public void getFilters_shouldCallClient_andReturnResult() {
        // Given
        final String userId = UUID.randomUUID().toString();
        final List<Filter> list = Collections.singletonList(FilterUtils.getFilter());
        final Pageable pageable = new PageRequest(0, 10);
        final Page<Filter> expected = new PageImpl<>(list, pageable, 1);
        final PagedResources<Filter> response = PagedResourcesUtils.buildPagedResources(pageable, list);

        when(deviceServiceClient.getFilters(userId, pageable)).thenReturn(response);

        // When
        final Page<Filter> result = filterManager.getFilters(userId, pageable);

        // Then
        verify(deviceServiceClient).getFilters(userId, pageable);
        assertThat(result).isEqualTo(expected);
    }

    @Test
    public void deleteFilter_whenFilterIsNotUsed_shouldCallClient() throws Exception {
        // Given
        final String userId = UUID.randomUUID().toString();
        final String filterName = UUID.randomUUID().toString();

        // When
        filterManager.deleteFilter(filterName, userId);

        // Then
        verify(deviceServiceClient).deleteFilter(userId, filterName);
    }
}
