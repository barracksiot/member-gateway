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

package io.barracks.membergateway.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.barracks.commons.util.Endpoint;
import io.barracks.membergateway.client.exception.PackageServiceClientException;
import io.barracks.membergateway.model.PackageInfo;
import io.barracks.membergateway.utils.PackageInfoUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.client.MockRestServiceServer;

import java.util.UUID;

import static io.barracks.membergateway.client.PackageServiceClient.GET_PACKAGE_ENDPOINT;
import static io.barracks.membergateway.client.PackageServiceClient.UPLOAD_PACKAGE_ENDPOINT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@RunWith(SpringRunner.class)
@RestClientTest(PackageServiceClient.class)
public class PackageServiceClientTest {
    @Autowired
    private ObjectMapper mapper;
    @Value("${io.barracks.packageservice.base_url}")
    private String baseUrl;
    @Autowired
    private MockRestServiceServer mockServer;
    @Autowired
    private PackageServiceClient packageServiceClient;

    @Value("classpath:io/barracks/membergateway/client/package.json")
    private Resource packageRes;
    @Value("classpath:package.exe")
    private Resource packageFile;

    @Test
    public void uploadPackage_whenCreationSucceed_shouldReturnAPackageInfo() throws Exception {
        // Given
        final PackageInfo packageInfo = PackageInfoUtils.getPackageInfo();
        final MockMultipartFile multipartFile = new MockMultipartFile("file", packageInfo.getFileName(), "application/x-msdownload", packageFile.getInputStream());
        mockServer.expect(method(UPLOAD_PACKAGE_ENDPOINT.getMethod()))
                .andExpect(requestTo(UPLOAD_PACKAGE_ENDPOINT.withBase(baseUrl).getURI()))
                .andExpect(
                        req -> {
                            MockClientHttpRequest request = ((MockClientHttpRequest) req);
                            String start = "--" + request.getHeaders().getContentType().getParameter("boundary") + "\r\n";
                            String end = "\r\n--" + request.getHeaders().getContentType().getParameter("boundary") + "--\r\n";
                            String body = request.getBodyAsString().substring(start.length());
                            body = body.substring(0, body.length() - end.length());
                            for (String part : body.split(start)) {
                                System.out.println(part); // TODO find a way to test the parts
                            }
                        }
                ).andRespond(withSuccess(packageRes, MediaType.APPLICATION_JSON));

        // When
        final PackageInfo result = packageServiceClient.uploadPackage(
                multipartFile.getOriginalFilename(),
                multipartFile.getContentType(),
                multipartFile.getInputStream(),
                packageFile.contentLength(),
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString()
        );

        // Then
        mockServer.verify();
        assertThat(result).isEqualTo(mapper.readValue(packageRes.getInputStream(), PackageInfo.class));
    }

    @Test
    public void getPackageInfo_whenNotExists_shouldThrowException() {
        // Given
        final String packageId = UUID.randomUUID().toString();
        mockServer.expect(method(GET_PACKAGE_ENDPOINT.getMethod()))
                .andExpect(requestTo(GET_PACKAGE_ENDPOINT.withBase(baseUrl).getURI(packageId)))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));
        // When Then
        assertThatExceptionOfType(PackageServiceClientException.class).isThrownBy(() -> packageServiceClient.getPackageInfo(packageId));
        mockServer.verify();
    }

    @Test
    public void getPackageInfo_whenExists_shouldReturnInfo() throws Exception {
        // Given
        final Endpoint endpoint = GET_PACKAGE_ENDPOINT;
        final String packageId = UUID.randomUUID().toString();

        mockServer.expect(method(endpoint.getMethod()))
                .andExpect(requestTo(endpoint.withBase(baseUrl).getURI(packageId)))
                .andRespond(withSuccess().body(packageRes));

        // When
        final PackageInfo result = packageServiceClient.getPackageInfo(packageId);

        // Then
        assertThat(result).isEqualTo(mapper.readValue(packageRes.getInputStream(), PackageInfo.class));
    }
}