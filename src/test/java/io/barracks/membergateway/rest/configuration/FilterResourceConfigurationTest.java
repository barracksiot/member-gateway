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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.barracks.commons.test.PagedResourcesUtils;
import io.barracks.commons.util.Endpoint;
import io.barracks.membergateway.exception.FilterInUseException;
import io.barracks.membergateway.model.Filter;
import io.barracks.membergateway.rest.BarracksResourceTest;
import io.barracks.membergateway.rest.FilterResource;
import io.barracks.membergateway.utils.FilterUtils;
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

import java.util.Arrays;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.*;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@BarracksResourceTest(controllers = FilterResource.class, outputDir = "build/generated-snippets/filters")
public class FilterResourceConfigurationTest {

    private static final Endpoint CREATE_FILTER_ENDPOINT = Endpoint.from(HttpMethod.POST, "/filters");
    private static final Endpoint UPDATE_FILTER_ENDPOINT = Endpoint.from(HttpMethod.POST, "/filters/{name}");
    private static final Endpoint GET_FILTERS_ENDPOINT = Endpoint.from(HttpMethod.GET, "/filters");
    private static final Endpoint GET_FILTER_ENDPOINT = Endpoint.from(HttpMethod.GET, "/filters/{name}");
    private static final Endpoint DELETE_FILTER_ENDPOINT = Endpoint.from(HttpMethod.DELETE, "/filters/{name}");
    private static final String baseUrl = "https://not.barracks.io";

    @Value("classpath:io/barracks/membergateway/rest/configuration/filter.json")
    private Resource filterRequestResource;
    @MockBean
    private FilterResource filterResource;
    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper objectMapper;
    private RandomPrincipal principal;

    @Before
    public void setUp() throws Exception {
        this.principal = new RandomPrincipal();
    }

    @Test
    public void documentCreateFilter() throws Exception {
        //  Given
        final Endpoint endpoint = CREATE_FILTER_ENDPOINT;
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        final Filter filter = Filter.builder()
                .name("My Beta Users")
                .query(objectMapper.readTree("{ \"eq\" : { \"customClientData.isBeta\" : true } }"))
                .build();
        final Filter response = filter.toBuilder()
                .build();
        doReturn(response).when(filterResource).createFilter(filter, principal);
        // When
        final ResultActions result = mvc.perform(
                RestDocumentationRequestBuilders.request(endpoint.getMethod(), endpoint.withBase(baseUrl).getURI())
                        .principal(principal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(filter))
        );

        // Then
        verify(filterResource).createFilter(filter, principal);
        result.andExpect(status().isCreated())
                .andDo(document(
                        "create",
                        requestFields(
                                fieldWithPath("name").description("The filter's unique name"),
                                fieldWithPath("query").description("The filter's query"),
                                fieldWithPath("deploymentCount").ignored(),
                                fieldWithPath("deviceCount").ignored()
                        )
                ));

    }

    @Test
    public void documentUpdateFilter() throws Exception {
        //  Given
        final Endpoint endpoint = UPDATE_FILTER_ENDPOINT;
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        final Filter filter = Filter.builder()
                .name("My Beta Users")
                .query(objectMapper.readTree("{ \"eq\" : { \"customClientData.isBeta\" : true } }"))
                .build();
        final Filter response = filter.toBuilder()
                .build();
        doReturn(response).when(filterResource).updateFilter(filter.getName(), filter.getQuery(), principal);
        // When
        final ResultActions result = mvc.perform(
                RestDocumentationRequestBuilders.request(endpoint.getMethod(), endpoint.getPath(), filter.getName())
                        .principal(principal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(filter.getQuery()))
        );

        // Then
        verify(filterResource).updateFilter(filter.getName(), filter.getQuery(), principal);
        result.andExpect(status().isOk())
                .andDo(document(
                        "update",
                        requestFields(
                                fieldWithPath("eq").description("The filter's query")
                        )
                ));

    }

    @Test
    public void postFilter_withValidFilter_shouldCreateFilterAndReturnValue() throws Exception {
        // Given
        final Endpoint endpoint = CREATE_FILTER_ENDPOINT;
        final Filter request = objectMapper.readValue(filterRequestResource.getInputStream(), Filter.class);
        final Filter response = FilterUtils.getFilter();
        doReturn(response).when(filterResource).createFilter(request, principal);

        // When
        final ResultActions result = mvc.perform(
                request(endpoint.getMethod(), endpoint.withBase(baseUrl).getURI())
                        .principal(principal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(FileCopyUtils.copyToByteArray(filterRequestResource.getInputStream()))
        );

        // Then
        verify(filterResource).createFilter(request, principal);
        result.andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value(response.getName()));
    }

    @Test
    public void postFilter_withInvalidFilter_shouldReturnBadRequest() throws Exception {
        // Given
        final Endpoint endpoint = CREATE_FILTER_ENDPOINT;
        final String badFilter = "{}";

        // When
        final ResultActions result = mvc.perform(
                request(endpoint.getMethod(), endpoint.withBase(baseUrl).getURI())
                        .principal(principal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(badFilter)
        );

        // Then
        result.andExpect(status().isBadRequest());
    }

    @Test
    public void documentGetFilters() throws Exception {
        //  Given
        final Endpoint endpoint = GET_FILTERS_ENDPOINT;
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        final Pageable pageable = new PageRequest(0, 10);
        final Filter filter1 = Filter.builder()
                .name("My Alpha Users")
                .query(objectMapper.readTree("{ \"eq\" : { \"customClientData.isAlpha\" : true } }"))
                .build();
        final Filter filter2 = Filter.builder()
                .name("My Beta Users")
                .query(objectMapper.readTree("{ \"eq\" : { \"customClientData.isBeta\" : true } }"))
                .build();

        final Page<Filter> page = new PageImpl<>(Arrays.asList(filter1, filter2));
        final PagedResources expected = PagedResourcesUtils.<Filter>getPagedResourcesAssembler().toResource(page);
        doReturn(expected).when(filterResource).getFilters(pageable, principal);
        // When
        final ResultActions result = mvc.perform(
                RestDocumentationRequestBuilders.request(endpoint.getMethod(), endpoint.withBase(baseUrl).pageable(pageable).getURI())
                        .principal(principal)
        );

        // Then
        verify(filterResource).getFilters(pageable, principal);
        result.andExpect(status().isOk())
                .andDo(document(
                        "list",
                        responseFields(
                                fieldWithPath("_embedded.filters").description("The list of filters"),
                                fieldWithPath("_links").ignored(),
                                fieldWithPath("page").ignored()
                        )
                ));

    }

    @Test
    public void getFilters_shouldCallResource_andReturnResults() throws Exception {
        //Given
        final Endpoint endpoint = GET_FILTERS_ENDPOINT;
        final Pageable pageable = new PageRequest(0, 10);
        final Filter filter1 = FilterUtils.getFilter();
        final Filter filter2 = FilterUtils.getFilter();
        final Page<Filter> page = new PageImpl<>(Arrays.asList(filter1, filter2));
        final PagedResources expected = PagedResourcesUtils.<Filter>getPagedResourcesAssembler().toResource(page);
        doReturn(expected).when(filterResource).getFilters(pageable, principal);

        // When
        final ResultActions result = mvc.perform(
                request(endpoint.getMethod(), endpoint.withBase(baseUrl).pageable(pageable).getURI())
                        .accept(MediaType.APPLICATION_JSON_UTF8)
                        .principal(principal)
        );

        //Then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.filters", hasSize(page.getNumberOfElements())))
                .andExpect(jsonPath("$._embedded.filters[0].name").value(filter1.getName()))
                .andExpect(jsonPath("$._embedded.filters[1].name").value(filter2.getName()));
    }

    @Test
    public void documentGetFilter() throws Exception {
        //  Given
        final Endpoint endpoint = GET_FILTER_ENDPOINT;
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        final String name = "My filter name";
        final Filter expected = Filter.builder()
                .name(name)
                .query(objectMapper.readTree("{ \"eq\" : { \"customClientData.isAlpha\" : true } }"))
                .deploymentCount(13)
                .deviceCount(2)
                .build();
        doReturn(expected).when(filterResource).getFilter(name, principal);
        // When
        final ResultActions result = mvc.perform(
                RestDocumentationRequestBuilders.request(endpoint.getMethod(), endpoint.getPath(), name)
                        .principal(principal)
        );

        // Then
        verify(filterResource).getFilter(name, principal);
        result.andExpect(status().isOk())
                .andDo(document(
                        "get",
                        pathParameters(
                                parameterWithName("name").description("The filter's unique name")
                        ),
                        responseFields(
                                fieldWithPath("name").description("The name of the filter"),
                                fieldWithPath("query").description("The query of the filter"),
                                fieldWithPath("deploymentCount").description("The number of deployment plans containing the filter"),
                                fieldWithPath("deviceCount").description("The number of devices associated to this filter")
                        )
                ));

    }

    @Test
    public void getFilter_shouldCallResource_andReturnResult() throws Exception {
        //Given
        final Endpoint endpoint = GET_FILTER_ENDPOINT;
        final String filterName = "FilterName";
        final Filter expected = FilterUtils.getFilter();
        doReturn(expected).when(filterResource).getFilter(filterName, principal);

        // When
        final ResultActions result = mvc.perform(
                request(endpoint.getMethod(), endpoint.withBase(baseUrl).getURI(filterName))
                        .principal(principal)
        );

        //Then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(expected.getName()))
                .andExpect(jsonPath("$.deploymentCount").value(expected.getDeploymentCount()))
                .andExpect(jsonPath("$.deviceCount").value(expected.getDeviceCount()));
    }

    @Test
    public void documentDeleteFilter() throws Exception {
        //  Given
        final Endpoint endpoint = DELETE_FILTER_ENDPOINT;
        final String filterName = "aFilterName";

        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);

        // When
        final ResultActions result = mvc.perform(
                RestDocumentationRequestBuilders
                        .request(endpoint.getMethod(), endpoint.getPath(), filterName)
                        .principal(principal)
        );

        // Then
        verify(filterResource).deleteFilter(filterName, principal);
        result.andExpect(status().isNoContent())
                .andDo(
                        document(
                                "delete",
                                pathParameters(
                                        parameterWithName("name").description("The filter's unique name")
                                )
                        ));
    }

    @Test
    public void deleteFilter_whenAllIsFine_shouldCallResourceAndReturnNoContent() throws Exception {
        //  Given
        final Endpoint endpoint = DELETE_FILTER_ENDPOINT;
        final String filterName = UUID.randomUUID().toString();

        // When
        final ResultActions result = mvc.perform(
                request(endpoint.getMethod(), endpoint.withBase(baseUrl).getURI(filterName))
                        .accept(MediaType.APPLICATION_JSON_UTF8)
                        .principal(principal)
        );

        // Then
        verify(filterResource).deleteFilter(filterName, principal);
        result.andExpect(status().isNoContent());
    }

    @Test
    public void deleteFilter_whenFilterIsUsed_shouldCallResourceAndReturnConflict() throws Exception {
        //  Given
        final Endpoint endpoint = DELETE_FILTER_ENDPOINT;
        final String filterName = UUID.randomUUID().toString();
        doThrow(FilterInUseException.class).when(filterResource).deleteFilter(filterName, principal);


        // When
        final ResultActions result = mvc.perform(
                request(endpoint.getMethod(), endpoint.withBase(baseUrl).getURI(filterName))
                        .accept(MediaType.APPLICATION_JSON_UTF8)
                        .principal(principal)
        );

        // Then
        verify(filterResource).deleteFilter(filterName, principal);
        result.andExpect(status().isConflict());
    }
}
