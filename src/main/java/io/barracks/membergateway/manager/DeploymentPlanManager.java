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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class DeploymentPlanManager {
    private final DeploymentServiceClient deploymentServiceClient;

    public DeploymentPlanManager(DeploymentServiceClient deploymentServiceClient) {
        this.deploymentServiceClient = deploymentServiceClient;
    }

    public JsonNode createDeploymentPlan(String userId, String packageRef, ObjectNode plan) {
        plan.set("packageRef", TextNode.valueOf(packageRef));
        return deploymentServiceClient.createDeploymentPlan(userId, plan);
    }

    public JsonNode getActiveDeploymentPlan(String userId, String packageRef) {
        return deploymentServiceClient.getActiveDeploymentPlan(userId, packageRef);
    }

    public Page<JsonNode> getDeploymentPlansByFilterName(String userId, String filterName, Pageable pageable) {
        return deploymentServiceClient.getDeploymentPlansByFilterName(userId, filterName, pageable);
    }
}
