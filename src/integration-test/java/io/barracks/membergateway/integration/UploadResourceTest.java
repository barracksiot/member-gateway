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

package io.barracks.membergateway.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.barracks.membergateway.client.AuthorizationServiceClient;
import io.barracks.membergateway.client.PackageServiceClient;
import io.barracks.membergateway.model.PackageInfo;
import io.barracks.membergateway.model.User;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.io.InputStream;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class UploadResourceTest {
    @Value("${io.barracks.packageservice.base_url}")
    private String packageServiceUrl;
    @Value("${local.server.port}")
    private int port;
    @Value("classpath:io/barracks/membergateway/integration/packageServiceSuccess.json")
    private Resource packageSuccessResource;
    @Value("classpath:package.exe")
    private Resource packageUploadResource;
    @MockBean
    private AuthorizationServiceClient authorizationServiceClient;
    @MockBean
    private PackageServiceClient packageServiceClient;

    @Autowired
    private TestRestTemplate testTemplate;
    @Autowired
    private ObjectMapper objectMapper;

    private String serverUrl;
    private String userId;

    @Before
    public void setUp() {
        serverUrl = "http://localhost:" + port;
        userId = UUID.randomUUID().toString();
        doReturn(User.builder().id(userId).build())
                .when(authorizationServiceClient).requestUserFromToken(any());
    }

    @Test
    public void uploadFile_verifyMemoryUsage() throws Exception {
        // Given
        final MultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();
        final String versionId = UUID.randomUUID().toString();
        parts.add("versionId", versionId);
        parts.add("file", packageUploadResource);
        doReturn(objectMapper.readValue(packageSuccessResource.getInputStream(), PackageInfo.class))
                .when(packageServiceClient)
                .uploadPackage(
                        eq(packageUploadResource.getFilename()),
                        eq(MediaType.APPLICATION_OCTET_STREAM_VALUE),
                        isA(InputStream.class),
                        eq(packageUploadResource.contentLength()),
                        eq(versionId),
                        eq(userId)
                );

        // When
        ResponseEntity responseEntity = testTemplate.postForEntity(
                serverUrl + "/packages", parts, String.class
        );

        // Then
        verify(packageServiceClient).uploadPackage(
                eq(packageUploadResource.getFilename()),
                eq(MediaType.APPLICATION_OCTET_STREAM_VALUE),
                isA(InputStream.class),
                eq(packageUploadResource.contentLength()),
                eq(versionId),
                eq(userId)
        );
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(responseEntity.getBody()).isNotNull();
    }
}
