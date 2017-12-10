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
import io.barracks.membergateway.client.exception.ComponentServiceClientException;
import io.barracks.membergateway.model.Package;
import io.barracks.membergateway.model.Version;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.PagedResources;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

@Service
public class ComponentServiceClient extends HateoasRestClient {
    static final Endpoint CREATE_PACKAGE_ENDPOINT = Endpoint.from(HttpMethod.POST, "/owners/{userId}/packages");
    static final Endpoint CREATE_VERSION_ENDPOINT = Endpoint.from(HttpMethod.POST, "/owners/{userId}/packages/{packageRef}/versions");
    static final Endpoint GET_COMPONENTS_ENDPOINT = Endpoint.from(HttpMethod.GET, "/owners/{userId}/packages");
    static final Endpoint GET_VERSIONS_ENDPOINT = Endpoint.from(HttpMethod.GET, "/owners/{userId}/packages/{packageRef}/versions");
    static final Endpoint GET_COMPONENT_ENDPOINT = Endpoint.from(HttpMethod.GET, "/owners/{userId}/packages/{packageRef}");
    static final Endpoint GET_VERSION_ENDPOINT = Endpoint.from(HttpMethod.GET, "/owners/{userId}/packages/{packageRef}/versions/{version}");

    private final String baseUrl;
    private final RestTemplate restTemplate;

    @Autowired
    public ComponentServiceClient(
            ObjectMapper mapper,
            RestTemplateBuilder restTemplateBuilder,
            @Value("${io.barracks.componentservice.base_url}") String baseUrl
    ) {
        this.restTemplate = prepareRestTemplateBuilder(mapper, restTemplateBuilder).build();
        this.baseUrl = baseUrl;
    }

    @Override
    protected RestTemplateBuilder prepareRestTemplateBuilder(ObjectMapper mapper, RestTemplateBuilder builder) {
        RestTemplateBuilder restTemplateBuilder = super.prepareRestTemplateBuilder(mapper, builder);
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setBufferRequestBody(false);
        return restTemplateBuilder.requestFactory(requestFactory);
    }

    public Package createPackage(String userId, Package aPackage) {
        try {
            return restTemplate.exchange(
                    CREATE_PACKAGE_ENDPOINT.withBase(baseUrl).body(aPackage).getRequestEntity(userId),
                    Package.class
            ).getBody();
        } catch (HttpStatusCodeException e) {
            throw new ComponentServiceClientException(e);
        }
    }

    public Version createVersion(
            String userId,
            String packageRef,
            Version version,
            String filename,
            long length,
            InputStream inputStream
    ) {
        final MultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();
        parts.add("file", new InputStreamResource(inputStream) {
            @Override
            public long contentLength() throws IOException {
                return length;
            }

            @Override
            public String getFilename() {
                return filename;
            }
        });
        parts.add("version", version);
        try {
            return restTemplate.exchange(
                    CREATE_VERSION_ENDPOINT.withBase(baseUrl).body(parts).getRequestEntity(userId, packageRef),
                    Version.class
            ).getBody();
        } catch (HttpStatusCodeException e) {
            throw new ComponentServiceClientException(e);
        }
    }

    public PageImpl<Package> getPackages(String userId, Pageable pageable) {
        try {
            final ResponseEntity<PagedResources<Package>> responseEntity = restTemplate.exchange(
                    GET_COMPONENTS_ENDPOINT.withBase(baseUrl).pageable(pageable).getRequestEntity(userId),
                    new ParameterizedTypeReference<PagedResources<Package>>() {
                    }
            );
            return new PageImpl<>(new ArrayList<>(responseEntity.getBody().getContent()), pageable, responseEntity.getBody().getMetadata().getTotalElements());
        } catch (HttpStatusCodeException e) {
            throw new ComponentServiceClientException(e);
        }
    }

    public PageImpl<Version> getVersions(String userId, String packageRef, Pageable pageable) {
        try {
            final ResponseEntity<PagedResources<Version>> responseEntity = restTemplate.exchange(
                    GET_VERSIONS_ENDPOINT.withBase(baseUrl).pageable(pageable).getRequestEntity(userId, packageRef),
                    new ParameterizedTypeReference<PagedResources<Version>>() {
                    }
            );
            return new PageImpl<>(new ArrayList<>(responseEntity.getBody().getContent()), pageable, responseEntity.getBody().getMetadata().getTotalElements());
        } catch (HttpStatusCodeException e) {
            throw new ComponentServiceClientException(e);
        }

    }

    public Package getPackage(String name, String packageRef) {
        try {
            return restTemplate.exchange(
                    GET_COMPONENT_ENDPOINT.withBase(baseUrl).getRequestEntity(name, packageRef),
                    new ParameterizedTypeReference<Package>() {
                    }
            ).getBody();
        } catch (HttpStatusCodeException e) {
            throw new ComponentServiceClientException(e);
        }
    }

    public Version getVersion(String userId, String packageRef, String version) {
        try {
            return restTemplate.exchange(
                    GET_VERSION_ENDPOINT.withBase(baseUrl).getRequestEntity(userId, packageRef, version),
                    new ParameterizedTypeReference<Version>() {
                    }
            ).getBody();
        } catch (HttpStatusCodeException e) {
            throw new ComponentServiceClientException(e);
        }
    }
}
