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

import io.barracks.membergateway.client.ComponentServiceClient;
import io.barracks.membergateway.client.DeploymentServiceClient;
import io.barracks.membergateway.model.Version;
import io.barracks.membergateway.model.VersionStatus;
import io.barracks.membergateway.utils.VersionUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.internal.verification.VerificationModeFactory.times;

@RunWith(MockitoJUnitRunner.class)
public class VersionManagerTest {
    @Mock
    private ComponentServiceClient componentServiceClient;

    @Mock
    private DeploymentServiceClient deploymentServiceClient;

    @InjectMocks
    @Spy
    private VersionManager versionManager;

    @Test
    public void createVersion_shouldCallClientAndSetVersionStatus_andReturnResult() {
        // Given
        final String userId = UUID.randomUUID().toString();
        final String packageRef = UUID.randomUUID().toString();
        final String filename = UUID.randomUUID().toString();
        final long length = 4242L;
        final Version version = VersionUtils.getVersion();
        final InputStream inputStream = new ByteArrayInputStream(new byte[]{'a', 'b', 'c'});
        final Version created = VersionUtils.getVersion();
        final VersionStatus versionStatus = VersionStatus.NEVER_USED;
        assertThat(versionStatus).isNotEqualTo(created.getStatus());
        final Version expected = created.toBuilder().status(versionStatus).build();

        doReturn(versionStatus).when(versionManager).getVersionStatus(userId, packageRef, version);
        doReturn(created).when(componentServiceClient).createVersion(userId, packageRef, version, filename, length, inputStream);

        // When
        final Version result = versionManager.createVersion(userId, packageRef, version, filename, length, inputStream);

        // Then
        verify(componentServiceClient).createVersion(userId, packageRef, version, filename, length, inputStream);
        verify(versionManager).getVersionStatus(userId, packageRef, version);
        assertThat(result).isEqualTo(expected);
    }

    @Test
    public void getVersions_shouldCallClientAndSetStatus_andReturnResult() {
        //Given
        final String userId = UUID.randomUUID().toString();
        final String packageRef = UUID.randomUUID().toString();
        final Version version1 = VersionUtils.getVersion();
        final Version version2 = VersionUtils.getVersion();
        final List<Version> list = Arrays.asList(version1, version2);
        final Pageable pageable = new PageRequest(0, 10);
        final PageImpl<Version> page = new PageImpl<>(list, pageable, list.size());
        final VersionStatus versionStatus = VersionStatus.NEVER_USED;

        assertThat(versionStatus).isNotEqualTo(page.getContent().get(0));
        assertThat(versionStatus).isNotEqualTo(page.getContent().get(1));

        final Version expectedVersion1 = version1.toBuilder().status(versionStatus).build();
        final Version expectedVersion2 = version2.toBuilder().status(versionStatus).build();
        final PageImpl<Version> expected = new PageImpl<>(Arrays.asList(expectedVersion1, expectedVersion2), pageable, list.size());

        doReturn(page).when(componentServiceClient).getVersions(userId, packageRef, pageable);
        doReturn(VersionStatus.NEVER_USED).when(versionManager).getVersionStatus(eq(userId), eq(packageRef), any(Version.class));

        //When
        final Page<Version> result = versionManager.getVersions(userId, packageRef, pageable);

        //Then
        verify(componentServiceClient).getVersions(userId, packageRef, pageable);
        verify(versionManager, times(2)).getVersionStatus(eq(userId), eq(packageRef), any(Version.class));
        assertThat(result).isEqualTo(expected);
    }

    @Test
    public void getVersionStatus_whenVersionIsInUse_shouldReturnInUse() {
        // Given
        final String userId = UUID.randomUUID().toString();
        final String packageRef = UUID.randomUUID().toString();
        final Version version = VersionUtils.getVersion();
        final List<String> versions = Arrays.asList(version.getId(), "anotherVersion", "oneMoreVersion");

        doReturn(versions).when(deploymentServiceClient).getDeployedVersions(userId, packageRef, true);

        // When
        final VersionStatus result = versionManager.getVersionStatus(userId, packageRef, version);

        // Then
        verify(deploymentServiceClient).getDeployedVersions(userId, packageRef, true);
        verifyNoMoreInteractions(deploymentServiceClient);
        assertThat(result).isEqualTo(VersionStatus.IS_IN_USE);
    }

    @Test
    public void getVersionStatus_whenVersionWasUsed_shouldReturnWasUsed() {
        // Given
        final String userId = UUID.randomUUID().toString();
        final String packageRef = UUID.randomUUID().toString();
        final Version version = VersionUtils.getVersion();
        final List<String> versions = Arrays.asList(version.getId(), "anotherVersion", "oneMoreVersion");
        final List<String> emptyList = Collections.emptyList();

        doReturn(emptyList).when(deploymentServiceClient).getDeployedVersions(userId, packageRef, true);
        doReturn(versions).when(deploymentServiceClient).getDeployedVersions(userId, packageRef, false);

        // When
        final VersionStatus result = versionManager.getVersionStatus(userId, packageRef, version);

        // Then
        verify(deploymentServiceClient).getDeployedVersions(userId, packageRef, true);
        verify(deploymentServiceClient).getDeployedVersions(userId, packageRef, false);
        verifyNoMoreInteractions(deploymentServiceClient);
        assertThat(result).isEqualTo(VersionStatus.WAS_USED);
    }

    @Test
    public void getVersionStatus_whenVersionWasNeverUsed_shouldReturnNeverUsed() {
        // Given
        final String userId = UUID.randomUUID().toString();
        final String packageRef = UUID.randomUUID().toString();
        final Version version = VersionUtils.getVersion();
        final List<String> emptyList = Collections.emptyList();

        doReturn(emptyList).when(deploymentServiceClient).getDeployedVersions(userId, packageRef, true);
        doReturn(emptyList).when(deploymentServiceClient).getDeployedVersions(userId, packageRef, false);

        // When
        final VersionStatus result = versionManager.getVersionStatus(userId, packageRef, version);

        // Then
        verify(deploymentServiceClient).getDeployedVersions(userId, packageRef, true);
        verify(deploymentServiceClient).getDeployedVersions(userId, packageRef, false);
        verifyNoMoreInteractions(deploymentServiceClient);
        assertThat(result).isEqualTo(VersionStatus.NEVER_USED);
    }

    @Test
    public void getVersion_shouldCallClientAndSetStatus_andReturnVersion() {
        // Given
        final String userId = UUID.randomUUID().toString();
        final String packageRef = UUID.randomUUID().toString();
        final Version version = VersionUtils.getVersion();
        final VersionStatus versionStatus = VersionStatus.NEVER_USED;
        assertThat(versionStatus).isNotEqualTo(version.getStatus());
        final Version expected = version.toBuilder().status(versionStatus).build();

        doReturn(expected).when(componentServiceClient).getVersion(userId, packageRef, version.getId());

        // When
        final Version result = versionManager.getVersion(userId, packageRef, version.getId());

        // Then
        verify(componentServiceClient).getVersion(userId, packageRef, version.getId());
        assertThat(result).isEqualTo(expected);

    }
}
