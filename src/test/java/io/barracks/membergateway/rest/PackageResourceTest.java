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
import io.barracks.membergateway.manager.PackageManager;
import io.barracks.membergateway.model.Package;
import io.barracks.membergateway.utils.PackageUtils;
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
public class PackageResourceTest {

    @Mock
    private PackageManager packageManager;

    private PagedResourcesAssembler<Package> resourcesAssembler = PagedResourcesUtils.getPagedResourcesAssembler();

    private PackageResource resource;

    private Principal principal = new RandomPrincipal();

    @Before
    public void setUp() {
        resource = new PackageResource(packageManager, resourcesAssembler);
    }

    @Test
    public void createPackage_shouldCallManager_andReturnResult() {
        // Given
        final Package aPackage = PackageUtils.getPackage();
        final Package expected = PackageUtils.getPackage();
        doReturn(expected).when(packageManager).createPackage(principal.getName(), aPackage);

        // When
        final Package result = resource.createPackage(aPackage, principal);

        // Then
        verify(packageManager).createPackage(principal.getName(), aPackage);
        assertThat(result).isEqualTo(expected);
    }

    @Test
    public void getPackages_shouldCallManager_andReturnPackages() {
        // Given
        final String userId = principal.getName();
        final Pageable pageable = new PageRequest(0, 10);
        final Package package1 = PackageUtils.getPackage();
        final Package package2 = PackageUtils.getPackage();
        final Page<Package> page = new PageImpl<>(Lists.newArrayList(package1, package2));
        final PagedResources<Resource<Package>> expected = resourcesAssembler.toResource(page);
        when(packageManager.getPackages(userId, pageable)).thenReturn(page);

        // When
        final PagedResources<Resource<Package>> result = resource.getPackages(pageable, principal);

        // Then
        verify(packageManager).getPackages(userId, pageable);
        assertThat(result).isEqualTo(expected);
    }

    @Test
    public void getPackage_shouldCallManager_andReturnPackage() {
        // Given
        final String userId = principal.getName();
        final Package expected = PackageUtils.getPackage();

        when(packageManager.getPackage(userId, expected.getReference())).thenReturn(expected);

        // When
        final Package result = resource.getPackage(expected.getReference(), principal);

        // Then
        verify(packageManager).getPackage(userId, expected.getReference());
        assertThat(result).isEqualTo(expected);
    }
}
