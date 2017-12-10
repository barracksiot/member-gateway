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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.barracks.commons.util.Endpoint;
import io.barracks.membergateway.client.exception.DeploymentPlanServiceClientException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.PagedResources;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@Service
public class DeploymentServiceClient extends HateoasRestClient {
    static final Endpoint CREATE_DEPLOYMENT_PLAN_ENDPOINT = Endpoint.from(HttpMethod.POST, "/owners/{userId}/plans");
    static final Endpoint GET_DEPLOYMENT_PLAN_BY_FILTER_ENDPOINT = Endpoint.from(HttpMethod.GET, "/owners/{userId}/plans", "filter={filter}");
    static final Endpoint GET_ACTIVE_DEPLOYMENT_PLAN_BY_PACKAGE_REF_ENDPOINT = Endpoint.from(HttpMethod.GET, "/owners/{userId}/plans/{packageRef}");
    static final Endpoint GET_DEPLOYED_VERSIONS = Endpoint.from(HttpMethod.GET, "/owners/{userId}/plans/{packageRef}/versions", "onlyActive={onlyActive}");

    private final String baseUrl;
    private final RestTemplate restTemplate;

    public DeploymentServiceClient(
            ObjectMapper mapper,
            RestTemplateBuilder restTemplateBuilder,
            @Value("${io.barracks.deploymentservice.base_url}") String baseUrl
    ) {
        this.restTemplate = prepareRestTemplateBuilder(mapper, restTemplateBuilder).build();
        this.baseUrl = baseUrl;
    }

    public JsonNode createDeploymentPlan(String userId, JsonNode plan) {
        try {
            return restTemplate.exchange(
                    CREATE_DEPLOYMENT_PLAN_ENDPOINT.withBase(baseUrl).body(plan).getRequestEntity(userId),
                    JsonNode.class
            ).getBody();
        } catch (HttpStatusCodeException e) {
            throw new DeploymentPlanServiceClientException(e);
        }
    }

    public Page<JsonNode> getDeploymentPlansByFilterName(String userId, String filterName, Pageable pageable) {
        try {
            final ResponseEntity<PagedResources<JsonNode>> responseEntity = restTemplate.exchange(
                    GET_DEPLOYMENT_PLAN_BY_FILTER_ENDPOINT.withBase(baseUrl).pageable(pageable).getRequestEntity(userId, filterName),
                    new ParameterizedTypeReference<PagedResources<JsonNode>>() {
                    }
            );
            return new PageImpl<>(new ArrayList<>(responseEntity.getBody().getContent()), pageable, responseEntity.getBody().getMetadata().getTotalElements());
        } catch (HttpStatusCodeException e) {
            throw new DeploymentPlanServiceClientException(e);
        }
    }

    public JsonNode getActiveDeploymentPlan(String userId, String packageRef) {
        try {
            return restTemplate.exchange(
                    GET_ACTIVE_DEPLOYMENT_PLAN_BY_PACKAGE_REF_ENDPOINT.withBase(baseUrl).getRequestEntity(userId, packageRef),
                    new ParameterizedTypeReference<JsonNode>() {
                    }
            ).getBody();
        } catch (HttpStatusCodeException e) {
            throw new DeploymentPlanServiceClientException(e);
        }
    }

    public List<String> getDeployedVersions(String userId, String packageRef, boolean onlyActive) {
        try {
            return restTemplate.exchange(
                    GET_DEPLOYED_VERSIONS.withBase(baseUrl).getRequestEntity(userId, packageRef, onlyActive),
                    new ParameterizedTypeReference<List<String>>() {}
            ).getBody();
        } catch(HttpStatusCodeException e) {
            throw new DeploymentPlanServiceClientException(e);
        }
    }
}