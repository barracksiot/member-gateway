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

package io.barracks.membergateway.rest.configuration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;
import io.barracks.commons.test.PagedResourcesUtils;
import io.barracks.commons.util.Endpoint;
import io.barracks.membergateway.rest.BarracksResourceTest;
import io.barracks.membergateway.rest.DeploymentPlanResource;
import io.barracks.membergateway.utils.RandomPrincipal;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.PagedResources;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.util.FileCopyUtils;

import java.util.UUID;

import static io.barracks.membergateway.utils.DeploymentPlanUtils.getDeploymentPlan;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.mockito.Mockito.*;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@RunWith(SpringRunner.class)
@BarracksResourceTest(controllers = DeploymentPlanResourceConfigurationTest.class, outputDir = "build/generated-snippets/deployment-plans")
public class DeploymentPlanResourceConfigurationTest {

    private static final String baseUrl = "https://not.barracks.io";
    private static final Endpoint CREATE_DEPLOYMENT_PLAN = Endpoint.from(HttpMethod.POST, "/packages/{packageRef}/deployment-plan");
    private static final Endpoint GET_ACTIVE_DEPLOYMENT_PLAN = Endpoint.from(HttpMethod.GET, "/packages/{packageRef}/deployment-plan");
    private static final Endpoint GET_DEPLOYMENT_PLANS_BY_FILTER_ENDPOINT = Endpoint.from(HttpMethod.GET, "/filters/{filter}/deployment-plan");

    @MockBean
    private DeploymentPlanResource deploymentPlanResource;
    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Value("classpath:io/barracks/membergateway/rest/configuration/deployment-plan.json")
    private Resource deploymentPlan;
    private RandomPrincipal principal;

    @Before
    public void setup() throws Exception {
        reset(deploymentPlanResource);
        this.principal = new RandomPrincipal();
    }

    @Test
    public void documentCreateDeploymentPlan() throws Exception {
        // Given
        final Endpoint endpoint = CREATE_DEPLOYMENT_PLAN;
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        final String packageRef = "io.barracks.package.documentation";
        final ObjectNode expected = (ObjectNode) objectMapper.readTree(deploymentPlan.getInputStream());
        doReturn(expected).when(deploymentPlanResource).createDeploymentPlan(packageRef, expected, principal);

        // When
        final ResultActions result = mvc.perform(
                RestDocumentationRequestBuilders.request(endpoint.getMethod(), endpoint.getPath(), packageRef)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .principal(principal)
                        .content(FileCopyUtils.copyToByteArray(deploymentPlan.getInputStream()))
        );

        // Then
        result.andExpect(status().isCreated())
                .andDo(document(
                        "create",
                        pathParameters(parameterWithName("packageRef").description("The package's reference")),
                        requestFields(
                                fieldWithPath("allow.filters").description("The filters used to define which devices are allowed to access the package"),
                                fieldWithPath("rules").description("The ordered list of rules for selecting which version will be offered to which device"),
                                fieldWithPath("rules[].version").description("The version linked to this specific rule"),
                                fieldWithPath("rules[].allow.filters").description("The list of filters allowing a device to receive this version")
                        )
                ));
    }

    @Test
    public void postDeploymentPlan_shouldCallResource_andReturnCreated() throws Exception {
        // Given
        final Endpoint endpoint = CREATE_DEPLOYMENT_PLAN;
        final String packageRef = UUID.randomUUID().toString();
        final ObjectNode expected = (ObjectNode) objectMapper.readTree(deploymentPlan.getInputStream());
        doReturn(expected).when(deploymentPlanResource).createDeploymentPlan(packageRef, expected, principal);

        // When
        final ResultActions result = mvc.perform(
                request(endpoint.getMethod(), endpoint.withBase(baseUrl).getURI(packageRef))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .principal(principal)
                        .content(FileCopyUtils.copyToByteArray(deploymentPlan.getInputStream()))
        );

        // Then
        result.andExpect(status().isCreated())
                .andExpect(content().json(expected.toString()));
    }

    @Test
    public void documentGetActiveDeploymentPlan() throws Exception {
        final Endpoint endpoint = GET_ACTIVE_DEPLOYMENT_PLAN;
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        final String packageRef = UUID.randomUUID().toString();
        final ObjectNode plan = (ObjectNode) objectMapper.readTree(deploymentPlan.getInputStream());

        doReturn(plan).when(deploymentPlanResource).getActiveDeploymentPlan(packageRef, principal);

        // When
        final ResultActions result = mvc.perform(
                RestDocumentationRequestBuilders.request(endpoint.getMethod(), endpoint.getPath(), packageRef)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .principal(principal)
        );

        // Then
        verify(deploymentPlanResource).getActiveDeploymentPlan(packageRef, principal);
        result.andExpect(status().isOk())
                .andDo(
                        document(
                                "get",
                                pathParameters(
                                        parameterWithName("packageRef").description("The package's unique reference")
                                ),
                                responseFields(
                                        fieldWithPath("allow.filters").description("The filters used to define which devices are allowed to access the package"),
                                        fieldWithPath("rules").description("The ordered list of rules for selecting which version will be offered to which device"),
                                        fieldWithPath("rules[].version").description("The version linked to this specific rule"),
                                        fieldWithPath("rules[].allow.filters").description("The list of filters allowing a device to receive this version")
                                )
                        )
                );
    }

    @Test
    public void getActiveDeploymentPlan_shouldCallResource_andReturnResult() throws Exception {
        // Given
        final Endpoint endpoint = GET_ACTIVE_DEPLOYMENT_PLAN;
        final String packageRef = UUID.randomUUID().toString();
        final ObjectNode expected = (ObjectNode) objectMapper.readTree(deploymentPlan.getInputStream());

        doReturn(expected).when(deploymentPlanResource).getActiveDeploymentPlan(packageRef, principal);

        // When
        final ResultActions result = mvc.perform(
                request(endpoint.getMethod(), endpoint.withBase(baseUrl).getURI(packageRef))
                        .accept(MediaType.APPLICATION_JSON_UTF8)
                        .principal(principal)
        );

        // Then
        result.andExpect(status().isOk())
                .andExpect(content().json(expected.toString()));
    }


    @Test
    public void documentGetDeploymentPlanByFilterName() throws Exception {
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        final Endpoint endpoint = GET_DEPLOYMENT_PLANS_BY_FILTER_ENDPOINT;
        final String filterName = "filterName";
        final ObjectNode deploymentPlan1 = getDeploymentPlan();
        final ObjectNode deploymentPlan2 = getDeploymentPlan();
        final Page<ObjectNode> page = new PageImpl<>(Lists.newArrayList(deploymentPlan1, deploymentPlan2));
        final PagedResources<org.springframework.hateoas.Resource<ObjectNode>> expected = PagedResourcesUtils.<ObjectNode>getPagedResourcesAssembler().toResource(page);

        doReturn(expected).when(deploymentPlanResource).getDeploymentPlansByFilterName(eq(filterName), eq(principal), any(Pageable.class));

        // When
        final ResultActions result = mvc.perform(
                RestDocumentationRequestBuilders.request(endpoint.getMethod(), endpoint.getPath(), filterName)
                        .accept(MediaType.APPLICATION_JSON_UTF8)
                        .principal(principal)
        );

        // Then
        result.andExpect(status().isOk())
                .andDo(document(
                        "get-by-filter-name",
                        pathParameters(
                                parameterWithName("filter").description("The unique identifier of the filter")
                        ),
                        responseFields(
                                fieldWithPath("_embedded.objectNodes").description("The list of deployment Plans"),
                                fieldWithPath("_links").ignored(),
                                fieldWithPath("page").ignored()
                        )
                ));

        verify(deploymentPlanResource).getDeploymentPlansByFilterName(eq(filterName), eq(principal), any(Pageable.class));
    }

    @Test
    public void getDeploymentPlansByFilterName_whenAllIsFine_shouldCallResourceAndReturnPlansList() throws Exception {
        //Given
        final Endpoint endpoint = GET_DEPLOYMENT_PLANS_BY_FILTER_ENDPOINT;
        final String filter = UUID.randomUUID().toString();
        final Pageable pageable = new PageRequest(0, 10);
        final ObjectNode deploymentPlan1 = getDeploymentPlan();
        final ObjectNode deploymentPlan2 = getDeploymentPlan();
        final Page<JsonNode> page = new PageImpl<>(Lists.newArrayList(deploymentPlan1, deploymentPlan2));
        final PagedResources<org.springframework.hateoas.Resource<JsonNode>> expected = PagedResourcesUtils.<JsonNode>getPagedResourcesAssembler().toResource(page);

        when(deploymentPlanResource.getDeploymentPlansByFilterName(filter, principal, pageable)).thenReturn(expected);

        // When
        final ResultActions result = mvc.perform(
                request(
                        endpoint.getMethod(),
                        endpoint.withBase(baseUrl).pageable(pageable).getURI(filter))
                        .accept(MediaType.APPLICATION_JSON_UTF8)
                        .principal(principal));

        //Then
        verify(deploymentPlanResource).getDeploymentPlansByFilterName(filter, principal, pageable);

        result.andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.objectNodes", hasSize(page.getNumberOfElements())));
    }
}