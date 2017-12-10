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

import io.barracks.membergateway.manager.PackageManager;
import io.barracks.membergateway.model.Package;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/packages")
public class PackageResource {

    private final PackageManager packageManager;
    private final PagedResourcesAssembler<Package> assembler;

    @Autowired
    public PackageResource(PackageManager packageManager, PagedResourcesAssembler<Package> assembler) {
        this.packageManager = packageManager;
        this.assembler = assembler;
    }

    @ResponseBody
    @RequestMapping(method = RequestMethod.POST)
    @ResponseStatus(HttpStatus.CREATED)
    public Package createPackage(@RequestBody Package aPackage, Principal principal) {
        return packageManager.createPackage(principal.getName(), aPackage);
    }

    @RequestMapping(method = RequestMethod.GET)
    public PagedResources<Resource<Package>> getPackages(Pageable pageable, Principal principal) {
        final Page<Package> packages = packageManager.getPackages(principal.getName(), pageable);
        return assembler.toResource(packages);
    }

    @RequestMapping(method = RequestMethod.GET, path = "/{reference}")
    public Package getPackage(@PathVariable("reference") String reference, Principal principal) {
        return packageManager.getPackage(principal.getName(), reference);
    }
}
