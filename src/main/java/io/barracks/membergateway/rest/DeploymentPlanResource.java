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
import io.barracks.membergateway.manager.DeploymentPlanManager;
import org.hibernate.validator.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
public class DeploymentPlanResource {
    private final DeploymentPlanManager deploymentPlanManager;
    private final PagedResourcesAssembler<JsonNode> assembler;

    @Autowired
    public DeploymentPlanResource(DeploymentPlanManager deploymentPlanManager, PagedResourcesAssembler<JsonNode> assembler) {
        this.deploymentPlanManager = deploymentPlanManager;
        this.assembler = assembler;
    }

    @RequestMapping(method = RequestMethod.POST, path = "/packages/{reference}/deployment-plan")
    @ResponseStatus(value = HttpStatus.CREATED)
    public JsonNode createDeploymentPlan(@PathVariable("reference") String packageRef, @RequestBody ObjectNode plan, Principal principal) {
        return deploymentPlanManager.createDeploymentPlan(principal.getName(), packageRef, plan);
    }

    @ResponseBody
    @RequestMapping(method = RequestMethod.GET, path = "/packages/{reference}/deployment-plan")
    public JsonNode getActiveDeploymentPlan(
            @NotBlank @PathVariable("reference") String packageRef,
            Principal principal
    ) {
        return deploymentPlanManager.getActiveDeploymentPlan(principal.getName(), packageRef);
    }

    @ResponseBody
    @RequestMapping(method = RequestMethod.GET, path = "/filters/{filter}/deployment-plan")
    public PagedResources<Resource<JsonNode>> getDeploymentPlansByFilterName(
            @NotBlank @PathVariable("filter") String filterName,
            Principal principal,
            Pageable pageable
    ) {
        return assembler.toResource(
                deploymentPlanManager.getDeploymentPlansByFilterName(principal.getName(), filterName, pageable));
    }
}
