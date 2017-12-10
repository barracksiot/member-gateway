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

import io.barracks.membergateway.manager.VersionManager;
import io.barracks.membergateway.model.Version;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.Principal;

@RestController
@RequestMapping("/packages/{reference}/versions")
public class VersionResource {
    private final VersionManager versionManager;
    private final PagedResourcesAssembler<Version> assembler;

    @Autowired
    public VersionResource(VersionManager versionManager, PagedResourcesAssembler<Version> assembler) {
        this.versionManager = versionManager;
        this.assembler = assembler;
    }

    @RequestMapping(method = RequestMethod.POST)
    @ResponseBody
    @ResponseStatus(value = HttpStatus.CREATED)
    public Version createVersion(
            @RequestParam("file") MultipartFile file,
            @RequestPart("version") Version versionEntity,
            @PathVariable("reference") String packageRef,
            Principal principal) {
        try {
            return versionManager.createVersion(principal.getName(), packageRef, versionEntity, file.getOriginalFilename(), file.getSize(), file.getInputStream());
        } catch (IOException e) {
            throw new MultipartException("Failed to access file.", e);
        }
    }

    @RequestMapping(method = RequestMethod.GET)
    public PagedResources<Resource<Version>> getVersions(
            @PathVariable("reference") String packageRef,
            Principal principal,
            Pageable pageable) {
        final Page<Version> page = versionManager.getVersions(principal.getName(), packageRef, pageable);
        return assembler.toResource(page);
    }

    @RequestMapping(method = RequestMethod.GET, path = "/{version}")
    public Version getVersion(
            @PathVariable("reference") String packageRef,
            @PathVariable("version") String version,
            Principal principal) {
        return versionManager.getVersion(principal.getName(), packageRef, version);
    }
}
