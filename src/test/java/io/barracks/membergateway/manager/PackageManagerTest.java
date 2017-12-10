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
import io.barracks.membergateway.utils.RandomPrincipal;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.mock.web.MockMultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.security.Principal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class PackageManagerTest {

    @Mock
    private PackageServiceClient packageServiceClient;

    @InjectMocks
    private PackageManager packageManager;

    private Principal principal;

    @Before
    public void setUp() {
        principal = new RandomPrincipal();
    }

    @Test
    public void upload_whenUpdateServiceClientReturnPackageInfo_shouldReturnItToo() throws IOException {
        // Given
        final File file = getPackageFile();
        final PackageInfo packageInfo = new PackageInfo("ObjectID", principal.getName(), "Example.exe", "MD5", 123, "Version");
        final FileInputStream inputStream = new FileInputStream(file);
        final MockMultipartFile multipartFile = new MockMultipartFile("file", packageInfo.getFileName(), "application/x-msdownload", inputStream);
        final String versionId = UUID.randomUUID().toString();
        when(packageServiceClient.uploadPackage(packageInfo.getFileName(), multipartFile.getContentType(), inputStream, file.length(), versionId, principal.getName())).thenReturn(packageInfo);

        // When
        final PackageInfo result = packageManager.upload(packageInfo.getFileName(), multipartFile.getContentType(), inputStream, file.length(), versionId, principal.getName());

        // Then
        assertThat(result).isEqualTo(packageInfo);
        verify(packageServiceClient).uploadPackage(packageInfo.getFileName(), multipartFile.getContentType(), inputStream, file.length(), versionId, principal.getName());
    }

    @Test
    public void getPackageInfoByUuidAndUserId_whenClientThrowAnException_shouldThrowAnExceptionToo() {
        // Given
        final String packageId = UUID.randomUUID().toString();
        when(packageServiceClient.getPackageInfo(packageId)).thenThrow(PackageServiceClientException.class);

        // When & Then
        assertThatExceptionOfType(PackageServiceClientException.class)
                .isThrownBy(() -> packageManager.getPackageInfoByUuidAndUserId(packageId, principal.getName()));
        verify(packageServiceClient).getPackageInfo(packageId);
    }

    @Test
    public void getPackageInfoByUuidAndUserId_whenClientReturnAPackageThatDoesNotBelongToUser_shouldThrowAnException() {
        // Given
        final String packageId = UUID.randomUUID().toString();
        final PackageInfo packageInfo = new PackageInfo(packageId, "NotTheGoodUser", "Example.exe", "MD5", 123, "Version");
        when(packageServiceClient.getPackageInfo(packageId)).thenReturn(packageInfo);

        // When & Then
        assertThatExceptionOfType(PackageManagerException.class)
                .isThrownBy(() -> packageManager.getPackageInfoByUuidAndUserId(packageId, principal.getName()));
        verify(packageServiceClient).getPackageInfo(packageId);
    }

    @Test
    public void getPackageInfoByUuidAndUserId_whenClientReturnAPackageThatBelongToUser_shouldReturnItToo() {
        // Given
        final String packageId = UUID.randomUUID().toString();
        final PackageInfo packageInfo = new PackageInfo(packageId, principal.getName(), "Example.exe", "MD5", 123, "Version");
        when(packageServiceClient.getPackageInfo(packageId)).thenReturn(packageInfo);

        // When
        final PackageInfo result = packageManager.getPackageInfoByUuidAndUserId(packageId, principal.getName());

        // Then
        verify(packageServiceClient).getPackageInfo(packageId);
        Assert.assertNotNull(result);
        Assert.assertEquals(packageInfo, result);
    }

    public File getPackageFile() {
        final URL resource = this.getClass().getResource("/package.exe");
        return new File(resource.getFile());
    }
}