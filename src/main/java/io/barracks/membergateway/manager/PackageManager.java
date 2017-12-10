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

import io.barracks.membergateway.client.PackageServiceClient;
import io.barracks.membergateway.client.exception.PackageManagerException;
import io.barracks.membergateway.client.exception.PackageServiceClientException;
import io.barracks.membergateway.model.PackageInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.HttpStatusCodeException;

import java.io.File;
import java.io.InputStream;
import java.util.Optional;

@Service
public class PackageManager {

    private final PackageServiceClient packageServiceClient;

    @Autowired
    public PackageManager(PackageServiceClient packageServiceClient) {
        this.packageServiceClient = packageServiceClient;
    }

    public PackageInfo upload(String fileName, String contentType, InputStream inputStream, long size, String versionId, String userId) {
        return packageServiceClient.uploadPackage(fileName, contentType, inputStream, size, versionId, userId);
    }

    public PackageInfo getPackageInfoByUuidAndUserId(String uuid, String userId) {
        final PackageInfo packageInfo = packageServiceClient.getPackageInfo(uuid);
        if (userId.equals(packageInfo.getUserId())) {
            return packageInfo;
        } else {
            throw new PackageManagerException(new HttpServerErrorException(HttpStatus.NOT_FOUND));
        }
    }

}
