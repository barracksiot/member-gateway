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

import com.google.common.collect.Lists;
import io.barracks.commons.test.PagedResourcesUtils;
import io.barracks.membergateway.manager.FilterManager;
import io.barracks.membergateway.model.Filter;
import io.barracks.membergateway.utils.FilterUtils;
import io.barracks.membergateway.utils.RandomPrincipal;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;

import java.security.Principal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class FilterResourceTest {
    @Mock
    private FilterManager filterManager;

    private PagedResourcesAssembler<Filter> filterPagedResourcesAssembler = PagedResourcesUtils.getPagedResourcesAssembler();
    private FilterResource filterResource;
    private Principal principal = new RandomPrincipal();

    @Before
    public void setup() {
        filterResource = new FilterResource(filterManager, filterPagedResourcesAssembler);
    }

    @Test
    public void createFilter_shouldCallManager_andReturnResult() {
        // Given
        final String userId = principal.getName();
        final Filter filter = FilterUtils.getFilter();
        final Filter expected = FilterUtils.getFilter();
        doReturn(expected).when(filterManager).createFilter(userId, filter);

        // When
        final Filter result = filterResource.createFilter(filter, principal);

        // Then
        assertThat(result).isEqualTo(expected);
    }

    @Test
    public void getFilters_shouldCallManagerAndReturnFilters() throws Exception {
        // Given
        final String userId = principal.getName();
        final Pageable pageable = new PageRequest(0, 10);
        final Filter filter1 = FilterUtils.getFilter();
        final Filter filter2 = FilterUtils.getFilter();
        final Page<Filter> page = new PageImpl<>(Lists.newArrayList(filter1, filter2));
        final PagedResources<Resource<Filter>> expected = filterPagedResourcesAssembler.toResource(page);
        when(filterManager.getFilters(userId, pageable)).thenReturn(page);
        // When
        final PagedResources<Resource<Filter>> result = filterResource.getFilters(pageable, principal);

        // Then
        verify(filterManager).getFilters(userId, pageable);
        assertThat(result).isEqualTo(expected);
    }

    @Test
    public void deleteFilter_whenAllIsFine_shouldCallManager() throws Exception {
        // Given
        final String filterName = "aFilterName";

        // When
        filterResource.deleteFilter(filterName, principal);

        //Then
        verify(filterManager).deleteFilter(filterName, principal.getName());
    }
}