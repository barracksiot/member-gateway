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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;
import io.barracks.commons.test.PagedResourcesUtils;
import io.barracks.membergateway.manager.DeploymentPlanManager;
import io.barracks.membergateway.utils.DeploymentPlanUtils;
import io.barracks.membergateway.utils.RandomPrincipal;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;

import java.security.Principal;
import java.util.Collections;
import java.util.UUID;

import static io.barracks.membergateway.utils.DeploymentPlanUtils.getDeploymentPlan;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class DeploymentPlanResourceTest {

    private final Principal principal = new RandomPrincipal();
    @Mock
    private DeploymentPlanManager deploymentPlanManager;

    private PagedResourcesAssembler<JsonNode> assembler = PagedResourcesUtils.getPagedResourcesAssembler();
    private DeploymentPlanResource deploymentPlanResource;

    @Before
    public void setup() {
        deploymentPlanResource = new DeploymentPlanResource(deploymentPlanManager, assembler);
    }

    @Test
    public void createDeploymentPlan_shouldCallManager_andReturnResult() {
        // Given
        final String reference = UUID.randomUUID().toString();
        final ObjectNode plan = getDeploymentPlan();
        final ObjectNode expected = getDeploymentPlan();
        doReturn(expected).when(deploymentPlanManager).createDeploymentPlan(principal.getName(), reference, plan);

        // When
        final JsonNode result = deploymentPlanResource.createDeploymentPlan(reference, plan, principal);

        // Then
        verify(deploymentPlanManager).createDeploymentPlan(principal.getName(), reference, plan);
        assertThat(result).isEqualTo(expected);
    }

    @Test
    public void getActiveDeploymentPlan_shouldCallManager_andReturnDeploymentPLan() {
        // Given
        final String userId = principal.getName();
        final String packageRef = UUID.randomUUID().toString();
        final ObjectNode expected = getDeploymentPlan();

        when(deploymentPlanManager.getActiveDeploymentPlan(userId, packageRef)).thenReturn(expected);

        // When
        final ObjectNode plan = (ObjectNode) deploymentPlanResource.getActiveDeploymentPlan(packageRef, principal);

        // Then
        verify(deploymentPlanManager).getActiveDeploymentPlan(userId, packageRef);
        assertThat(plan).isEqualTo(expected);
    }


    @Test
    public void getDeploymentPlansByFilterName_whenAllIsFine_shouldCallManagerAndReturnResult() {
        // Given
        final String filterName = UUID.randomUUID().toString();
        final Pageable pageable = new PageRequest(0, 10);
        final ObjectNode plan1 = getDeploymentPlan();
        final ObjectNode plan2 = getDeploymentPlan();
        final Page<JsonNode> page = new PageImpl<>(Lists.newArrayList(plan1, plan2));
        final PagedResources<Resource<JsonNode>> expected = assembler.toResource(page);
        when(deploymentPlanManager.getDeploymentPlansByFilterName(principal.getName(), filterName, pageable)).thenReturn(page);

        // When
        final PagedResources<Resource<JsonNode>> result = deploymentPlanResource.getDeploymentPlansByFilterName(filterName, principal, pageable);

        // Then
        verify(deploymentPlanManager).getDeploymentPlansByFilterName(principal.getName(), filterName, pageable);
        assertThat(result).isEqualTo(expected);
    }


    @Test
    public void getDeploymentPlansByFilterName_whenAllIsFineAndNoPlan_shouldCallManagerAndReturnEmptyPage() {
        // Given
        final String filterName = UUID.randomUUID().toString();
        final Pageable pageable = new PageRequest(0, 10);
        final Page<JsonNode> page = new PageImpl<>(Collections.emptyList());
        final PagedResources<Resource<JsonNode>> expected = assembler.toResource(page);
        when(deploymentPlanManager.getDeploymentPlansByFilterName(principal.getName(), filterName, pageable)).thenReturn(page);

        // When
        final PagedResources<Resource<JsonNode>> result = deploymentPlanResource.getDeploymentPlansByFilterName(filterName, principal, pageable);

        // Then
        verify(deploymentPlanManager).getDeploymentPlansByFilterName(principal.getName(), filterName, pageable);
        assertThat(result).isEqualTo(expected);
    }
}