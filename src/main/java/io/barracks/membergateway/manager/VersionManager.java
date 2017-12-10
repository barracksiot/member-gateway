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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.io.InputStream;

@Service
public class VersionManager {
    private final ComponentServiceClient componentServiceClient;
    private final DeploymentServiceClient deploymentServiceClient;

    @Autowired
    public VersionManager(ComponentServiceClient componentServiceClient, DeploymentServiceClient deploymentServiceClient) {
        this.componentServiceClient = componentServiceClient;
        this.deploymentServiceClient = deploymentServiceClient;
    }

    public Version createVersion(
            String userId,
            String packageRef,
            Version version,
            String filename,
            long length,
            InputStream inputStream) {
        final Version createdVersion = componentServiceClient.createVersion(userId, packageRef, version, filename, length, inputStream);
        return createdVersion.toBuilder().status(getVersionStatus(userId, packageRef, version)).build();
    }

    public Page<Version> getVersions(String userId, String packageRef, Pageable pageable) {
        final Page<Version> versions = componentServiceClient.getVersions(userId, packageRef, pageable);
        return versions.map(version ->
                version.toBuilder().status(getVersionStatus(userId, packageRef, version)).build()
        );
    }

    public Version getVersion(String userId, String packageRef, String versionId) {
        final Version version = componentServiceClient.getVersion(userId, packageRef, versionId);
        return version.toBuilder().status(getVersionStatus(userId, packageRef, version)).build();
    }

    VersionStatus getVersionStatus(String userId, String packageRef, Version version) {
        if (deploymentServiceClient.getDeployedVersions(userId, packageRef, true).contains(version.getId())) {
            return VersionStatus.IS_IN_USE;
        } else if (deploymentServiceClient.getDeployedVersions(userId, packageRef, false).contains(version.getId())) {
            return VersionStatus.WAS_USED;
        } else {
            return VersionStatus.NEVER_USED;
        }
    }

}
