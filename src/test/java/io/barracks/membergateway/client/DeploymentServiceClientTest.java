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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.barracks.commons.util.Endpoint;
import io.barracks.membergateway.client.exception.DeploymentPlanServiceClientException;
import io.barracks.membergateway.utils.DeploymentPlanUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.ResponseActions;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static io.barracks.membergateway.client.DeploymentServiceClient.CREATE_DEPLOYMENT_PLAN_ENDPOINT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

@RunWith(SpringRunner.class)
@RestClientTest(DeploymentServiceClient.class)
public class DeploymentServiceClientTest {
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private MockRestServiceServer mockServer;
    @Autowired
    private DeploymentServiceClient deploymentServiceClient;

    @Value("${io.barracks.deploymentservice.base_url}")
    private String baseUrl;
    @Value("classpath:io/barracks/membergateway/rest/configuration/deployment-plan.json")
    private Resource deploymentPlan;
    @Value("classpath:io/barracks/membergateway/client/deploymentPlans.json")
    private Resource deploymentPlans;
    @Value("classpath:io/barracks/membergateway/client/deployedVersions.json")
    private Resource deployedVersions;

    @Test
    public void createDeploymentPlan_whenServiceSucceeds_shouldReturnValue() throws Exception {
        // Given
        final String userId = UUID.randomUUID().toString();
        final JsonNode plan = DeploymentPlanUtils.getDeploymentPlan();
        final JsonNode expected = objectMapper.readTree(deploymentPlan.getInputStream());
        getDefaultServer().andExpect(requestTo(CREATE_DEPLOYMENT_PLAN_ENDPOINT.withBase(baseUrl).getURI(userId)))
                .andExpect(content().string(plan.toString()))
                .andRespond(withStatus(HttpStatus.CREATED).body(deploymentPlan));

        // When
        final JsonNode result = deploymentServiceClient.createDeploymentPlan(userId, plan);

        // Then
        mockServer.verify();
        assertThat(result).isEqualTo(expected);
    }

    @Test
    public void createDeploymentPlan_whenServiceFails_shouldThrowException() throws Exception {
        // Given
        final String userId = UUID.randomUUID().toString();
        final JsonNode plan = DeploymentPlanUtils.getDeploymentPlan();
        getDefaultServer().andExpect(requestTo(CREATE_DEPLOYMENT_PLAN_ENDPOINT.withBase(baseUrl).getURI(userId)))
                .andExpect(content().string(plan.toString()))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST));

        // Then When
        assertThatExceptionOfType(DeploymentPlanServiceClientException.class)
                .isThrownBy(() -> deploymentServiceClient.createDeploymentPlan(userId, plan));
        mockServer.verify();
    }

    private ResponseActions getDefaultServer() {
        return mockServer.expect(header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(request -> request.getHeaders().getAccept().stream().anyMatch(MediaType.APPLICATION_JSON::isCompatibleWith));
    }

    @Test
    public void getDeploymentPlanByFilterName_whenDeploymentPlans_shouldReturnResult() throws Exception {
        // Given
        final Endpoint endpoint = DeploymentServiceClient.GET_DEPLOYMENT_PLAN_BY_FILTER_ENDPOINT;
        final String userId = UUID.randomUUID().toString();
        final String filterName = UUID.randomUUID().toString();
        final List<JsonNode> expectedList = getJsonDeploymentPlans();
        final Pageable pageable = new PageRequest(0, 10);

        mockServer.expect(requestTo(endpoint.withBase(baseUrl).pageable(pageable).getURI(userId, filterName)))
                .andRespond(withSuccess().body(deploymentPlans));

        // When
        final Page<JsonNode> result = deploymentServiceClient.getDeploymentPlansByFilterName(userId, filterName, pageable);

        // Then
        mockServer.verify();
        assertThat(result).isNotEmpty()
                .hasSize(expectedList.size())
                .containsAll(expectedList);
    }


    @Test
    public void getDeploymentPlanByFilterName_whenServiceFails_shouldThrowException() throws Exception {
        // Given
        final Endpoint endpoint = DeploymentServiceClient.GET_DEPLOYMENT_PLAN_BY_FILTER_ENDPOINT;
        final String userId = UUID.randomUUID().toString();
        final String filterName = UUID.randomUUID().toString();
        final Pageable pageable = new PageRequest(0, 10);

        mockServer.expect(requestTo(endpoint.withBase(baseUrl).pageable(pageable).getURI(userId, filterName)))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST));

        // Then When
        assertThatExceptionOfType(DeploymentPlanServiceClientException.class).isThrownBy( () ->
                deploymentServiceClient.getDeploymentPlansByFilterName(userId, filterName, pageable)
        );

        // Then
        mockServer.verify();
    }

    private List<JsonNode> getJsonDeploymentPlans() throws Exception {
        final JsonNode plansJson = objectMapper.readTree(deploymentPlans.getInputStream());
        final List<JsonNode> plans = new ArrayList<>();

        for (int i = 0; i < plansJson.get("_embedded").get("plans").size(); i++) {
            plans.add(i, objectMapper.readValue(plansJson.get("_embedded").get("plans").get(i).toString(), JsonNode.class));
        }

        return plans;
    }

    @Test
    public void getActiveDeploymentPlan_whenSucceeded_shouldReturnDeploymentPlans() throws Exception {
        // Given
        final Endpoint endpoint = DeploymentServiceClient.GET_ACTIVE_DEPLOYMENT_PLAN_BY_PACKAGE_REF_ENDPOINT;
        final String userId = UUID.randomUUID().toString();
        final String packageRef = UUID.randomUUID().toString();
        final ObjectNode expected = (ObjectNode) objectMapper.readTree(deploymentPlan.getInputStream());

        mockServer.expect(method(endpoint.getMethod()))
                .andExpect(requestTo(endpoint.withBase(baseUrl).getURI(userId, packageRef)))
                .andRespond(withSuccess().body(deploymentPlan));

        // When
        final JsonNode result = deploymentServiceClient.getActiveDeploymentPlan(userId, packageRef);

        // Then
        mockServer.verify();
        assertThat(result).isEqualTo(expected);
    }

    @Test
    public void getActiveDeploymentPlan_whenFails_shouldThrowException() throws Exception {
        // Given
        final Endpoint endpoint = DeploymentServiceClient.GET_ACTIVE_DEPLOYMENT_PLAN_BY_PACKAGE_REF_ENDPOINT;
        final String userId = UUID.randomUUID().toString();
        final String packageRef = UUID.randomUUID().toString();

        mockServer.expect(method(endpoint.getMethod()))
                .andExpect(requestTo(endpoint.withBase(baseUrl).getURI(userId, packageRef)))
                .andRespond(withBadRequest());

        // ThenWhen
        assertThatExceptionOfType(DeploymentPlanServiceClientException.class)
                .isThrownBy(() -> deploymentServiceClient.getActiveDeploymentPlan(userId, packageRef));
        mockServer.verify();
    }

    @Test
    public void getDeployedVersions_whenServiceFails_shouldThrowException() throws Exception {
        // Given
        final Endpoint endpoint = DeploymentServiceClient.GET_DEPLOYED_VERSIONS;
        final String userId = UUID.randomUUID().toString();
        final String packageRef = UUID.randomUUID().toString();

        mockServer.expect(method(endpoint.getMethod()))
                .andExpect(requestTo(endpoint.withBase(baseUrl).getURI(userId, packageRef, true)))
                .andRespond(withBadRequest());

        // ThenWhen
        assertThatExceptionOfType(DeploymentPlanServiceClientException.class)
                .isThrownBy(() -> deploymentServiceClient.getDeployedVersions(userId, packageRef, true));
        mockServer.verify();
    }

    @Test
    public void getDeployedVersions_whenSucceeded_shouldReturnDeployedVersions() throws Exception {
        // Given
        final Endpoint endpoint = DeploymentServiceClient.GET_DEPLOYED_VERSIONS;
        final String userId = UUID.randomUUID().toString();
        final String packageRef = UUID.randomUUID().toString();
        final List<String> expected = objectMapper.readValue(deployedVersions.getInputStream(), new TypeReference<List<String>>() {
        });

        mockServer.expect(method(endpoint.getMethod()))
                .andExpect(requestTo(endpoint.withBase(baseUrl).getURI(userId, packageRef, true)))
                .andRespond(withSuccess().body(deployedVersions));

        // When
        final List<String> result = deploymentServiceClient.getDeployedVersions(userId, packageRef, true);

        // Then
        mockServer.verify();
        assertThat(result).isEqualTo(expected);
    }
}
