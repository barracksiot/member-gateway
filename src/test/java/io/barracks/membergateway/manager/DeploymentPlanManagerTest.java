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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import io.barracks.membergateway.client.DeploymentServiceClient;
import io.barracks.membergateway.utils.DeploymentPlanUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class DeploymentPlanManagerTest {
    @Mock
    private DeploymentServiceClient deploymentServiceClient;
    @InjectMocks
    private DeploymentPlanManager deploymentPlanManager;

    @Test
    public void createDeploymentPlan_shouldCallClientWithCompletedJson_andReturnResult() {
        // Given
        final String userId = UUID.randomUUID().toString();
        final String packageRef = UUID.randomUUID().toString();
        final ObjectNode plan = DeploymentPlanUtils.getDeploymentPlan();
        final ObjectNode toCreate = plan.deepCopy();
        toCreate.set("packageRef", TextNode.valueOf(packageRef));
        final ObjectNode expected = DeploymentPlanUtils.getDeploymentPlan();
        doReturn(expected).when(deploymentServiceClient).createDeploymentPlan(userId, toCreate);

        // When
        final JsonNode result = deploymentPlanManager.createDeploymentPlan(userId, packageRef, plan);

        // Then
        verify(deploymentServiceClient).createDeploymentPlan(userId, toCreate);
        assertThat(result).isEqualTo(expected);
    }

    @Test
    public void getActiveDeploymentPlan_shouldCallClient_andReturnResult() {
        // Given
        final String userId = UUID.randomUUID().toString();
        final String packageRef = UUID.randomUUID().toString();
        final ObjectNode expected = DeploymentPlanUtils.getDeploymentPlan();

        when(deploymentServiceClient.getActiveDeploymentPlan(userId, packageRef)).thenReturn(expected);

        // When
        final ObjectNode result = (ObjectNode) deploymentPlanManager.getActiveDeploymentPlan(userId, packageRef);

        // Then
        verify(deploymentServiceClient).getActiveDeploymentPlan(userId, packageRef);
        assertThat(result).isEqualTo(expected);
    }

    @Test
    public void getDeploymentPlansByFilterName_shouldCallClient_andReturnResult() {
        // Given
        final String userId = UUID.randomUUID().toString();
        final String filterName = UUID.randomUUID().toString();
        final Pageable pageable = new PageRequest(0, 10);
        final List<JsonNode> plans = Arrays.asList(
                DeploymentPlanUtils.getDeploymentPlan(),
                DeploymentPlanUtils.getDeploymentPlan()
        );

        final Page<JsonNode> expected = new PageImpl<JsonNode>(plans, pageable, plans.size());

        doReturn(expected).when(deploymentServiceClient).getDeploymentPlansByFilterName(userId, filterName, pageable);

        // When
        final Page<JsonNode> result = deploymentPlanManager.getDeploymentPlansByFilterName(userId, filterName, pageable);

        // Then
        verify(deploymentServiceClient).getDeploymentPlansByFilterName(userId, filterName, pageable);

        assertThat(result).containsOnlyElementsOf(plans);
    }
}