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
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.barracks.commons.util.Endpoint;
import io.barracks.membergateway.client.exception.PackageServiceClientException;
import io.barracks.membergateway.model.PackageInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;

@Component
public class PackageServiceClient extends HateoasRestClient {

    static final Endpoint UPLOAD_PACKAGE_ENDPOINT = Endpoint.from(HttpMethod.POST, "/packages");
    static final Endpoint GET_PACKAGE_ENDPOINT = Endpoint.from(HttpMethod.GET, "/packages/{packageId}");

    private final String baseUrl;

    private final RestTemplate restTemplate;

    @Autowired
    public PackageServiceClient(
            ObjectMapper objectMapper,
            RestTemplateBuilder restTemplateBuilder,
            @Value("${io.barracks.packageservice.base_url}") String baseUrl
    ) {
        this.restTemplate = prepareRestTemplateBuilder(objectMapper, restTemplateBuilder).build();
        this.baseUrl = baseUrl;
    }

    @Override
    protected RestTemplateBuilder prepareRestTemplateBuilder(ObjectMapper mapper, RestTemplateBuilder builder) {
        RestTemplateBuilder restTemplateBuilder = super.prepareRestTemplateBuilder(mapper, builder);
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setBufferRequestBody(false);
        return restTemplateBuilder.requestFactory(requestFactory);
    }

    public PackageInfo uploadPackage(String fileName, String contentType, InputStream inputStream, long size, String versionId, String userId) {
        try {
            final MultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();
            parts.add("userId", userId);
            parts.add("versionId", versionId);
            parts.add("file", new InputStreamResource(inputStream) {
                @Override
                public String getFilename() {
                    return fileName;
                }

                @SuppressFBWarnings
                public String getContentType() {
                    return contentType;
                }

                @Override
                public long contentLength() throws IOException {
                    return size;
                }
            });
            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON_UTF8));
            return restTemplate.exchange(UPLOAD_PACKAGE_ENDPOINT.withBase(baseUrl).body(parts).headers(headers).getRequestEntity(), PackageInfo.class).getBody();
        } catch (HttpStatusCodeException e) {
            throw new PackageServiceClientException(e);
        }
    }

    public PackageInfo getPackageInfo(String packageId) {
        try {
            return this.restTemplate.exchange(
                    GET_PACKAGE_ENDPOINT.withBase(baseUrl).getRequestEntity(packageId),
                    PackageInfo.class
            ).getBody();
        } catch (HttpStatusCodeException e) {
            throw new PackageServiceClientException(e);
        }
    }

}
