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

import io.barracks.membergateway.client.exception.PackageManagerException;
import io.barracks.membergateway.client.exception.PackageServiceClientException;
import io.barracks.membergateway.manager.PackageManager;
import io.barracks.membergateway.model.PackageInfo;
import io.barracks.membergateway.utils.RandomPrincipal;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.client.HttpServerErrorException;

import java.io.InputStream;
import java.net.URI;
import java.security.Principal;
import java.util.UUID;

import static org.hamcrest.Matchers.anyOf;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@BarracksResourceTest(controllers = PackageResource.class)
public class PackageResourceTest {
    @Autowired
    private MockMvc mvc;
    @MockBean
    private PackageManager packageManager;
    @Autowired
    private PackageResource packageResource;
    @Value("classpath:/package.exe")
    private Resource packageFile;

    private Principal principal = new RandomPrincipal();

    @Test
    public void uploadPackage_whenRequestContentIsValid_shouldReturn201Code() throws Exception {
        // Given
        final PackageInfo packageInfo = new PackageInfo("OBJECTID", principal.getName(), "Example.exe", "MD5", packageFile.contentLength(), "Version");
        final MockMultipartFile multipartFile = new MockMultipartFile("file", packageInfo.getFileName(), "application/x-msdownload", packageFile.getInputStream());
        when(packageManager.upload(eq(packageInfo.getFileName()), eq(multipartFile.getContentType()), isA(InputStream.class), eq(packageFile.contentLength()), eq(packageInfo.getVersionId()), eq(principal.getName()))).thenReturn(packageInfo);

        // When
        final ResultActions result = mvc.perform(
                MockMvcRequestBuilders.fileUpload("/packages").file(multipartFile).param("versionId", packageInfo.getVersionId()).principal(principal)
        );

        // Then
        verify(packageManager).upload(eq(packageInfo.getFileName()), eq(multipartFile.getContentType()), isA(InputStream.class), eq(packageFile.contentLength()), eq(packageInfo.getVersionId()), eq(principal.getName()));
        result.andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(packageInfo.getId()))
                .andExpect(jsonPath("$.userId").value(packageInfo.getUserId()))
                .andExpect(jsonPath("$.fileName").value(packageInfo.getFileName()))
                .andExpect(jsonPath("$.md5").value(packageInfo.getMd5()))
                .andExpect(jsonPath("$.size").value(anyOf(
                        Matchers.equalTo(packageInfo.getSize()),
                        Matchers.equalTo((int) packageInfo.getSize())
                )));
    }

    @Test
    public void getPackageInfoByUuidAndUserId_whenInvalidParameter_shouldReturnBadRequest() throws Exception {
        // When
        final ResultActions result = mvc.perform(
                MockMvcRequestBuilders.get(URI.create("/packages/package"))
                        .param("badParam", "badValue")
                        .principal(principal)
        );

        // Then
        result.andExpect(status().isBadRequest());
    }

    @Test
    public void getPackageInfoByUuidAndUserId_whenPackageNotFound_shouldThrowAnExceptionToo() throws Exception {
        // Given
        final String packageId = UUID.randomUUID().toString();
        when(packageManager.getPackageInfoByUuidAndUserId(packageId, principal.getName())).thenThrow(new PackageServiceClientException(new HttpServerErrorException(HttpStatus.NOT_FOUND)));

        // When
        final ResultActions result = mvc.perform(
                MockMvcRequestBuilders.get(URI.create("/packages/package"))
                        .param("uuid", packageId)
                        .principal(principal)
        );

        // Then
        verify(packageManager).getPackageInfoByUuidAndUserId(packageId, principal.getName());
        result.andExpect(status().isNotFound());
    }

    @Test
    public void getPackageInfoByUuidAndUserId_whenClientReturnAPackageThatDoesNotBelongToUser_shouldThrowAnException() throws Exception {
        // Given
        final String packageId = UUID.randomUUID().toString();
        when(packageManager.getPackageInfoByUuidAndUserId(packageId, principal.getName())).thenThrow(new PackageManagerException(new HttpServerErrorException(HttpStatus.NOT_FOUND)));

        // When
        final ResultActions result = mvc.perform(
                MockMvcRequestBuilders.get(URI.create("/packages/package"))
                        .param("uuid", packageId)
                        .principal(principal)
        );

        // Then
        verify(packageManager).getPackageInfoByUuidAndUserId(packageId, principal.getName());
        result.andExpect(status().isNotFound());
    }

    @Test
    public void getPackageInfoByUuidAndUserId_whenClientReturnAPackageThatBelongToUser_shouldReturnItToo() throws Exception {
        // Given
        final String packageId = UUID.randomUUID().toString();
        final PackageInfo packageInfo = new PackageInfo(packageId, "NotTheGoodUser", "Example.exe", "MD5", 12344444444L, "Version");
        when(packageManager.getPackageInfoByUuidAndUserId(packageId, principal.getName())).thenReturn(packageInfo);

        // When
        final ResultActions result = mvc.perform(
                MockMvcRequestBuilders.get(URI.create("/packages/package"))
                        .param("uuid", packageId)
                        .principal(principal)
        );

        // Then
        verify(packageManager).getPackageInfoByUuidAndUserId(packageId, principal.getName());
        Assert.assertNotNull(result);
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(packageInfo.getId()))
                .andExpect(jsonPath("$.userId").value(packageInfo.getUserId()))
                .andExpect(jsonPath("$.fileName").value(packageInfo.getFileName()))
                .andExpect(jsonPath("$.md5").value(packageInfo.getMd5()))
                .andExpect(jsonPath("$.size").value(anyOf(
                        Matchers.equalTo(packageInfo.getSize()),
                        Matchers.equalTo((int) packageInfo.getSize())
                )));
    }

}