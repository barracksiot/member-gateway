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
import io.barracks.membergateway.model.Version;
import io.barracks.membergateway.model.VersionStatus;
import io.barracks.membergateway.rest.BarracksResourceTest;
import io.barracks.membergateway.rest.VersionResource;
import io.barracks.membergateway.utils.RandomPrincipal;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;

import static io.barracks.membergateway.utils.VersionUtils.getVersion;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.fileUpload;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@RunWith(SpringRunner.class)
@BarracksResourceTest(controllers = VersionResource.class, outputDir = "build/generated-snippets/versions")
public class VersionResourceConfigurationTest {

    private static final String baseUrl = "https://not.barracks.io";
    private static final Endpoint CREATE_VERSION_ENDPOINT = Endpoint.from(HttpMethod.POST, "/packages/{packageRef}/versions");
    private static final Endpoint GET_VERSION_ENDPOINT = Endpoint.from(HttpMethod.GET, "/packages/{packageRef}/versions/{versionId}");
    private static final Endpoint GET_VERSIONS_ENDPOINT = Endpoint.from(HttpMethod.GET, "/packages/{packageRef}/versions");

    @MockBean
    private VersionResource resource;
    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper json;

    private PagedResourcesAssembler<Version> assembler = PagedResourcesUtils.getPagedResourcesAssembler();
    private RandomPrincipal principal = new RandomPrincipal();

    @Test
    public void documentCreateVersion() throws Exception {
        // Given
        json.enable(SerializationFeature.INDENT_OUTPUT);
        final String packageRef = "io.barracks.package.documentation";
        final Version version = Version.builder()
                .id("0.0.1")
                .name("My first version")
                .description("This version is the first one for this package.")
                .metadata(Collections.singletonMap("aKey", "aValue"))
                .build();
        final MockMultipartFile mockMultipartFile = new MockMultipartFile("file", "package.bin", MediaType.APPLICATION_OCTET_STREAM_VALUE, new byte[]{'a', 'b', 'c'});
        final MockMultipartFile mockMultipartVersion = new MockMultipartFile("version", null, MediaType.APPLICATION_JSON_UTF8_VALUE, json.writeValueAsBytes(version));
        final Version expected = Version.builder()
                .id(version.getId())
                .filename(mockMultipartFile.getOriginalFilename())
                .length(3)
                .md5("900150983cd24fb0d6963f7d28e17f72")
                .name(version.getName())
                .description(version.getDescription())
                .metadata(version.getMetadata())
                .status(VersionStatus.NEVER_USED)
                .build();
        doReturn(expected).when(resource).createVersion(
                mockMultipartFile,
                version,
                packageRef,
                principal
        );
        // When
        final ResultActions result = mvc.perform(
                RestDocumentationRequestBuilders
                        .fileUpload(CREATE_VERSION_ENDPOINT.getPath(), packageRef)
                        .file(mockMultipartFile)
                        .file(mockMultipartVersion)
                        .principal(principal)
        );

        // Then
        assertThat(expected).hasNoNullFieldsOrProperties();
        verify(resource).createVersion(
                mockMultipartFile,
                version,
                packageRef,
                principal
        );
        result.andExpect(status().isCreated())
                .andExpect(content().json(json.writeValueAsString(expected)))
                .andDo(document(
                        "create",
                        requestParts(
                                partWithName("file").description("The file to upload with the version"),
                                partWithName("version").description("The version information")
                        ),
                        pathParameters(
                                parameterWithName("packageRef").description("Reference to the version's package")
                        ),
                        responseFields(
                                fieldWithPath("id").description("The version's unique identifier"),
                                fieldWithPath("name").description("The version's name"),
                                fieldWithPath("description").description("The version's description"),
                                fieldWithPath("filename").description("The version's file name"),
                                fieldWithPath("md5").description("The version's file md5 hash"),
                                fieldWithPath("length").description("The version's file length"),
                                fieldWithPath("metadata").description("The version's metadata"),
                                fieldWithPath("status").description("The version's status")
                        )
                        )
                );
    }

    @Test
    public void postVersion_shouldCallCreateVersionAndReturn201() throws Exception {
        // Given
        final String packageRef = UUID.randomUUID().toString();
        final Version version = getVersion().toBuilder().status(null).build();
        final MockMultipartFile mockMultipartFile = new MockMultipartFile("file", UUID.randomUUID().toString(), MediaType.APPLICATION_OCTET_STREAM_VALUE, new byte[]{'a', 'b', 'c'});
        final MockMultipartFile mockMultipartVersion = new MockMultipartFile("version", UUID.randomUUID().toString(), MediaType.APPLICATION_JSON_UTF8_VALUE, json.writeValueAsBytes(version));
        final Version expected = getVersion();

        doReturn(expected).when(resource).createVersion(
                mockMultipartFile,
                version,
                packageRef,
                principal
        );

        // When
        final ResultActions result = mvc.perform(
                fileUpload(CREATE_VERSION_ENDPOINT.withBase(baseUrl).getURI(packageRef))
                        .file(mockMultipartFile)
                        .file(mockMultipartVersion)
                        .principal(principal)
        );

        // Then
        verify(resource).createVersion(
                mockMultipartFile,
                version,
                packageRef,
                principal
        );
        result.andExpect(status().isCreated())
                .andExpect(content().json(json.writeValueAsString(expected)));
    }

    @Test
    public void documentGetVersions() throws Exception {
        final Endpoint endpoint = GET_VERSIONS_ENDPOINT;
        json.enable(SerializationFeature.INDENT_OUTPUT);
        final Pageable pageable = new PageRequest(0, 10);
        final String packageRef = UUID.randomUUID().toString();
        final Version version1 = Version.builder()
                .id("ID of the first version")
                .name("Name of the first version")
                .description("Description of the first version")
                .filename("Name of the file associated to the version")
                .length(42L)
                .md5("md5")
                .addMetadata("aKey", "aValue")
                .status(VersionStatus.IS_IN_USE)
                .build();

        final Version version2 = Version.builder()
                .id("ID of the second version")
                .name("Name of the second version")
                .description("Description of the second version")
                .filename("Name of the file associated to the version")
                .length(42L)
                .md5("md5")
                .addMetadata("anotherKey", true)
                .status(VersionStatus.NEVER_USED)
                .build();

        final Page<Version> page = new PageImpl<>(Arrays.asList(version1, version2));
        final PagedResources<Resource<Version>> versions = PagedResourcesUtils.<Version>getPagedResourcesAssembler(baseUrl).toResource(page);
        doReturn(versions).when(resource).getVersions(eq(packageRef), eq(principal), eq(pageable));

        // When
        final ResultActions result = mvc.perform(
                RestDocumentationRequestBuilders.request(endpoint.getMethod(), endpoint.withBase(baseUrl).pageable(pageable).getURI(packageRef))
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .principal(principal)
        );

        // Then
        verify(resource).getVersions(eq(packageRef), eq(principal), eq(pageable));
        result.andExpect(status().isOk())
                .andDo(
                        document(
                                "list",
                                responseFields(
                                        fieldWithPath("_embedded.versions").description("The list of versions"),
                                        fieldWithPath("_links").ignored(),
                                        fieldWithPath("page").ignored()
                                )
                        )
                );
    }

    @Test
    public void getVersions_shouldCallResource_andReturnResults() throws Exception {
        // Given
        final Endpoint endpoint = GET_VERSIONS_ENDPOINT;
        final Pageable pageable = new PageRequest(0, 10);
        final String packageRef = UUID.randomUUID().toString();
        final Version version1 = getVersion();
        final Version version2 = getVersion();
        final Page<Version> page = new PageImpl<>(Arrays.asList(version1, version2));
        final PagedResources expected = assembler.toResource(page);

        doReturn(expected).when(resource).getVersions(packageRef, principal, pageable);

        // When
        final ResultActions result = mvc.perform(
                request(endpoint.getMethod(), endpoint.withBase(baseUrl).pageable(pageable).getURI(packageRef))
                        .accept(MediaType.APPLICATION_JSON_UTF8)
                        .principal(principal)
        );

        // Then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.versions", hasSize(page.getNumberOfElements())))
                .andExpect(jsonPath("$._embedded.versions[0].name").value(version1.getName()))
                .andExpect(jsonPath("$._embedded.versions[1].name").value(version2.getName()));

    }

    @Test
    public void documentGetVersion() throws Exception {
        // Given
        final Endpoint endpoint = GET_VERSION_ENDPOINT;
        final String packageRef = "io.barracks.example";
        final String versionId = "1.0.0";
        final Version expected = Version.builder()
                .id(versionId)
                .name("A version")
                .description("A version example")
                .filename("update.bin")
                .length(42L)
                .md5("deadbeefbadc0ffee")
                .status(VersionStatus.IS_IN_USE)
                .addMetadata("critical", true)
                .build();
        doReturn(expected).when(resource).getVersion(packageRef, versionId, principal);

        // When
        final ResultActions result = mvc.perform(
                RestDocumentationRequestBuilders.request(endpoint.getMethod(), endpoint.getPath(), packageRef, versionId)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .principal(principal)
        );

        // Then
        verify(resource).getVersion(packageRef, versionId, principal);
        result.andExpect(status().isOk())
                .andExpect(content().json(json.writeValueAsString(expected)))
                .andDo(document(
                        "get",
                        pathParameters(
                                parameterWithName("packageRef").description("Reference to the version's package"),
                                parameterWithName("versionId").description("Version's unique identifier")
                        ),
                        responseFields(
                                fieldWithPath("id").description("The version's unique identifier"),
                                fieldWithPath("name").description("The version's name"),
                                fieldWithPath("description").description("The version's description"),
                                fieldWithPath("filename").description("The version's file name"),
                                fieldWithPath("md5").description("The version's file md5 hash"),
                                fieldWithPath("length").description("The version's file length"),
                                fieldWithPath("metadata").description("The version's metadata"),
                                fieldWithPath("status").description("The version's status")
                        )
                        )
                );
    }

}
