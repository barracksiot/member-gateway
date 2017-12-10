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

import io.barracks.commons.test.PagedResourcesUtils;
import io.barracks.membergateway.manager.VersionManager;
import io.barracks.membergateway.model.Version;
import io.barracks.membergateway.utils.RandomPrincipal;
import io.barracks.membergateway.utils.VersionUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

import java.io.InputStream;
import java.security.Principal;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class VersionResourceTest {

    @Mock
    private VersionManager versionManager;

    private PagedResourcesAssembler<Version> versionPagedResourcesAssembler = PagedResourcesUtils.getPagedResourcesAssembler();

    private VersionResource versionResource;

    private Principal principal = new RandomPrincipal();

    @Before
    public void setUp() {
        versionResource = new VersionResource(versionManager, versionPagedResourcesAssembler);
    }

    @Test
    public void createVersion_shouldCallManagerAndReturnResult() throws Exception {
        // Given
        final String packageRef = UUID.randomUUID().toString();
        final Version version = VersionUtils.getVersion();
        final MockMultipartFile file = new MockMultipartFile("file", UUID.randomUUID().toString(), MediaType.APPLICATION_OCTET_STREAM_VALUE, new byte[]{'a', 'b', 'c'});
        final Version expected = VersionUtils.getVersion();
        doReturn(expected).when(versionManager).createVersion(
                eq(principal.getName()),
                eq(packageRef),
                eq(version),
                eq(file.getOriginalFilename()),
                eq(file.getSize()),
                isA(InputStream.class)
        );

        // When
        final Version result = versionResource.createVersion(file, version, packageRef, principal);

        // Then
        verify(versionManager).createVersion(
                eq(principal.getName()),
                eq(packageRef),
                eq(version),
                eq(file.getOriginalFilename()),
                eq(file.getSize()),
                isA(InputStream.class)
        );
        assertThat(expected).hasNoNullFieldsOrPropertiesExcept("userId");
        assertThat(result).isEqualTo(expected);
    }

    @Test
    public void getVersions_shouldCallManager_andReturnResult() {
        //Given
        final String userId = principal.getName();
        final String packageRef = UUID.randomUUID().toString();
        final List<Version> list = Collections.singletonList(VersionUtils.getVersion());
        final Pageable pageable = new PageRequest(0, 10);
        final PageImpl<Version> page = new PageImpl<>(list, pageable, list.size());
        final PagedResources<Resource<Version>> expected = versionPagedResourcesAssembler.toResource(page);

        doReturn(page).when(versionManager).getVersions(userId, packageRef, pageable);

        //When
        final PagedResources<Resource<Version>> result = versionResource.getVersions(packageRef, principal, pageable);

        //Then
        verify(versionManager).getVersions(userId, packageRef, pageable);
        assertThat(result).isEqualTo(expected);
    }

    @Test
    public void getVersion_shouldCallManager_andReturnVersion() {
        // Given
        final String userId = principal.getName();
        final String packageRef = UUID.randomUUID().toString();
        final Version expected = VersionUtils.getVersion();

        doReturn(expected).when(versionManager).getVersion(userId, packageRef, expected.getId());

        // When
        final Version result = versionResource.getVersion(packageRef, expected.getId(), principal);

        // Then
        verify(versionManager).getVersion(userId, packageRef, expected.getId());
        assertThat(result).isEqualTo(expected);
    }
}
