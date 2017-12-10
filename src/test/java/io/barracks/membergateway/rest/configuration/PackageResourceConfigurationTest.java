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
import io.barracks.membergateway.model.Package;
import io.barracks.membergateway.rest.BarracksResourceTest;
import io.barracks.membergateway.rest.PackageResource;
import io.barracks.membergateway.utils.PackageUtils;
import io.barracks.membergateway.utils.RandomPrincipal;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.Arrays;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.*;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@RunWith(SpringRunner.class)
@BarracksResourceTest(controllers = PackageResource.class, outputDir = "build/generated-snippets/packages")
public class PackageResourceConfigurationTest {

    private static final Endpoint CREATE_PACKAGE_ENDPOINT = Endpoint.from(HttpMethod.POST, "/packages");
    private static final Endpoint GET_PACKAGES_ENDPOINT = Endpoint.from(HttpMethod.GET, "/packages");
    private static final Endpoint GET_PACKAGE_ENDPOINT = Endpoint.from(HttpMethod.GET, "/packages/{reference}");
    private static final String baseUrl = "https://not.barracks.io";

    @MockBean
    private PackageResource resource;
    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper mapper;

    private RandomPrincipal principal;

    private PagedResourcesAssembler<Package> assembler = PagedResourcesUtils.getPagedResourcesAssembler();

    @Before
    public void setUp() throws Exception {
        reset(resource);
        this.principal = new RandomPrincipal();
    }

    @Test
    public void documentCreatePackage() throws Exception {
        // Given
        final Endpoint endpoint = CREATE_PACKAGE_ENDPOINT;
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        final Package aPackage = Package.builder()
                .reference("io.barracks.package.documentation")
                .name("My documentation package")
                .description("A great package providing documentation to my system.")
                .build();
        doReturn(aPackage).when(resource).createPackage(aPackage, principal);

        // When
        final ResultActions result = mvc.perform(
                RestDocumentationRequestBuilders.request(endpoint.getMethod(), endpoint.withBase(baseUrl).getURI())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(aPackage))
                        .principal(principal)
        );

        // Then
        result.andExpect(status().isCreated())
                .andDo(document(
                        "create",
                        requestFields(
                                fieldWithPath("reference").description("The package's unique reference"),
                                fieldWithPath("name").description("The package's name"),
                                fieldWithPath("description").description("The package's description").optional()
                        ))
                );
    }

    @Test
    public void postPackage_shouldCallCreatePackageAndReturn201() throws Exception {
        // Given
        final Endpoint endpoint = CREATE_PACKAGE_ENDPOINT;
        final Package aPackage = PackageUtils.getPackage();
        final Package expected = PackageUtils.getPackage();
        doReturn(expected).when(resource).createPackage(aPackage, principal);

        // When
        final ResultActions result = mvc.perform(
                request(endpoint.getMethod(), endpoint.withBase(baseUrl).getURI())
                        .principal(principal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(aPackage))
        );

        // Then
        verify(resource).createPackage(aPackage, principal);
        result.andExpect(status().isCreated())
                .andExpect(content().string(mapper.writeValueAsString(expected)));
    }

    @Test
    public void documentGetPackages() throws Exception {
        final Endpoint endpoint = GET_PACKAGES_ENDPOINT;
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        final String userId = UUID.randomUUID().toString();
        final Package package1 = Package.builder()
                .name("A great package")
                .description("This package will be used to build great stuff !")
                .reference("io.barracks.package1")
                .build();

        final Package package2 = Package.builder()
                .name("An other great package")
                .description("This package will also be used to build great stuff !")
                .reference("io.barracks.package2")
                .build();

        final Page<Package> page = new PageImpl<>(Arrays.asList(package1, package2));
        final PagedResources<Resource<Package>> packages = PagedResourcesUtils.<Package>getPagedResourcesAssembler(baseUrl).toResource(page);
        doReturn(packages).when(resource).getPackages(any(Pageable.class), eq(principal));

        // When
        final ResultActions result = mvc.perform(
                RestDocumentationRequestBuilders.request(endpoint.getMethod(), endpoint.getPath(), userId)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .principal(principal)
        );

        // Then
        verify(resource).getPackages(any(Pageable.class), eq(principal));
        result.andExpect(status().isOk())
                .andDo(
                        document(
                                "list",
                                responseFields(
                                        fieldWithPath("_embedded.packages").description("The list of packages"),
                                        fieldWithPath("_links").ignored(),
                                        fieldWithPath("page").ignored()
                                )
                        )
                );
    }

    @Test
    public void getPackages_shouldCallResource_andReturnResults() throws Exception {
        // Given
        final Endpoint endpoint = GET_PACKAGES_ENDPOINT;
        final Pageable pageable = new PageRequest(0, 10);
        final Package package1 = PackageUtils.getPackage();
        final Package package2 = PackageUtils.getPackage();
        final Page<Package> page = new PageImpl<>(Arrays.asList(package1, package2));
        final PagedResources expected = assembler.toResource(page);

        doReturn(expected).when(resource).getPackages(pageable, principal);

        // When
        final ResultActions result = mvc.perform(
                request(endpoint.getMethod(), endpoint.withBase(baseUrl).pageable(pageable).getURI())
                        .accept(MediaType.APPLICATION_JSON_UTF8)
                        .principal(principal)
        );

        // Then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.packages", hasSize(page.getNumberOfElements())))
                .andExpect(jsonPath("$._embedded.packages[0].name").value(package1.getName()))
                .andExpect(jsonPath("$._embedded.packages[1].name").value(package2.getName()));
    }

    @Test
    public void documentGetPackage() throws Exception {
        final Endpoint endpoint = GET_PACKAGE_ENDPOINT;
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        final Package aPackage = Package.builder()
                .name("A great packages")
                .description("This packages will be used to build great stuff !")
                .reference("io.barracks.packages1")
                .build();

        doReturn(aPackage).when(resource).getPackage(aPackage.getReference(), principal);

        // When
        final ResultActions result = mvc.perform(
                RestDocumentationRequestBuilders.request(endpoint.getMethod(), endpoint.getPath(), aPackage.getReference())
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .principal(principal)
        );

        // Then
        verify(resource).getPackage(aPackage.getReference(), principal);
        result.andExpect(status().isOk())
                .andDo(
                        document(
                                "get",
                                pathParameters(
                                        parameterWithName("reference").description("The packages's unique reference")
                                ),
                                responseFields(
                                        fieldWithPath("name").description("The packages's name."),
                                        fieldWithPath("reference").description("The packages's unique reference"),
                                        fieldWithPath("description").description("The packages's description")
                                )
                        )
                );
    }

    @Test
    public void getPackage_shouldCallResource_andReturnResult() throws Exception {
        // Given
        final Endpoint endpoint = GET_PACKAGE_ENDPOINT;
        final Package expected = PackageUtils.getPackage();

        doReturn(expected).when(resource).getPackage(expected.getReference(), principal);

        // When
        final ResultActions result = mvc.perform(
                request(endpoint.getMethod(), endpoint.withBase(baseUrl).getURI(expected.getReference()))
                        .accept(MediaType.APPLICATION_JSON_UTF8)
                        .principal(principal)
        );

        // Then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(expected.getName()))
                .andExpect(jsonPath("$.reference").value(expected.getReference()));
    }
}
