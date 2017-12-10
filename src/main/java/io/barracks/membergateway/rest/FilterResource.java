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
import io.barracks.membergateway.manager.FilterManager;
import io.barracks.membergateway.model.Filter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.security.Principal;

@RestController
@RequestMapping("/filters")
public class FilterResource {
    private final FilterManager filterManager;
    private final PagedResourcesAssembler<Filter> assembler;

    @Autowired
    public FilterResource(FilterManager filterManager, PagedResourcesAssembler<Filter> assembler) {
        this.filterManager = filterManager;
        this.assembler = assembler;
    }

    @ResponseBody
    @RequestMapping(method = RequestMethod.POST)
    @ResponseStatus(HttpStatus.CREATED)
    public Filter createFilter(@RequestBody @Valid Filter filter, Principal authentication) {
        return filterManager.createFilter(authentication.getName(), filter);
    }

    @RequestMapping(method = RequestMethod.GET, path = "/{name}")
    public Filter getFilter(@PathVariable("name") String name, Principal principal) {
        return filterManager.getFilterByUserIdAndName(principal.getName(), name);
    }

    @ResponseBody
    @RequestMapping(method = RequestMethod.POST, path = "/{name}")
    public Filter updateFilter(@PathVariable("name") String name, @RequestBody @Valid JsonNode query, Principal principal) {
        return filterManager.updateFilter(principal.getName(), name, query);
    }

    @RequestMapping(method = RequestMethod.GET)
    public PagedResources<Resource<Filter>> getFilters(Pageable pageable, Principal principal) {
        final Page<Filter> filters = filterManager.getFilters(principal.getName(), pageable);
        return assembler.toResource(filters);
    }

    @RequestMapping(method = RequestMethod.DELETE, path = "/{name}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteFilter(@PathVariable("name") String name, Principal principal) {
        filterManager.deleteFilter(name, principal.getName());
    }
}