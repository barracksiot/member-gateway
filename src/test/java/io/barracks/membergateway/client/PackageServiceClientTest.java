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

package io.barracks.membergateway.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.barracks.commons.util.Endpoint;
import io.barracks.membergateway.client.exception.ComponentServiceClientException;
import io.barracks.membergateway.model.Package;
import io.barracks.membergateway.model.Version;
import io.barracks.membergateway.utils.VersionUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.ResponseActions;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static io.barracks.membergateway.utils.PackageUtils.getPackage;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

@RunWith(SpringRunner.class)
@RestClientTest(ComponentServiceClient.class)
public class PackageServiceClientTest {
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private MockRestServiceServer mockServer;
    @Autowired
    private ComponentServiceClient componentServiceClient;

    @Value("${io.barracks.componentservice.base_url}")
    private String baseUrl;
    @Value("classpath:io/barracks/membergateway/client/package.json")
    private Resource aPackage;
    @Value("classpath:io/barracks/membergateway/client/packages.json")
    private Resource packages;
    @Value("classpath:io/barracks/membergateway/client/packages-empty.json")
    private Resource packagesEmpty;
    @Value("classpath:io/barracks/membergateway/client/versions.json")
    private Resource versions;
    @Value("classpath:io/barracks/membergateway/client/versions-empty.json")
    private Resource versionsEmpty;
    @Value("classpath:io/barracks/membergateway/version.json")
    private Resource version;

    @Test
    public void createPackage_whenServiceSucceeds_shouldReturnValue() throws Exception {
        // Given
        final Endpoint endpoint = ComponentServiceClient.CREATE_PACKAGE_ENDPOINT;
        final String userId = UUID.randomUUID().toString();
        final Package toCreate = getPackage();

        getDefaultServer().andExpect(method(endpoint.getMethod()))
                .andExpect(requestTo(endpoint.withBase(baseUrl).getURI(userId)))
                .andExpect(content().string(objectMapper.writeValueAsString(toCreate)))
                .andRespond(withStatus(HttpStatus.CREATED).body(aPackage));

        // When
        final Package result = componentServiceClient.createPackage(userId, toCreate);

        // Then
        mockServer.verify();
        assertThat(result).isEqualTo(objectMapper.readValue(aPackage.getInputStream(), Package.class));
    }

    @Test
    public void createPackage_whenServiceFails_shouldThrowClientException() throws Exception {
        // Given
        final Endpoint endpoint = ComponentServiceClient.CREATE_PACKAGE_ENDPOINT;
        final String userId = UUID.randomUUID().toString();
        final Package aPackage = getPackage();
        getDefaultServer().andExpect(method(endpoint.getMethod()))
                .andExpect(requestTo(endpoint.withBase(baseUrl).getURI(userId)))
                .andExpect(content().string(objectMapper.writeValueAsString(aPackage)))
                .andRespond(withBadRequest());

        // Then When
        assertThatExceptionOfType(ComponentServiceClientException.class).isThrownBy(
                () -> componentServiceClient.createPackage(userId, aPackage)
        );
        mockServer.verify();
    }

    private ResponseActions getDefaultServer() {
        return mockServer.expect(header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(request -> request.getHeaders().getAccept().stream().anyMatch(MediaType.APPLICATION_JSON::isCompatibleWith));
    }

    @Test
    public void getPackages_whenSucceeded_shouldReturnPackages() throws Exception {
        // Given
        final Endpoint endpoint = ComponentServiceClient.GET_COMPONENTS_ENDPOINT;
        final String userId = UUID.randomUUID().toString();
        final List<Package> expectedList = getJsonPackages();
        final Pageable pageable = new PageRequest(0, 10);
        mockServer.expect(method(endpoint.getMethod()))
                .andExpect(requestTo(endpoint.withBase(baseUrl).pageable(pageable).getURI(userId)))
                .andRespond(withSuccess().body(packages));

        // When
        final PageImpl<Package> result = componentServiceClient.getPackages(userId, pageable);

        // Then
        mockServer.verify();
        assertThat(result).isNotEmpty()
                .hasSize(expectedList.size())
                .containsAll(expectedList);
    }

    private List<Package> getJsonPackages() throws Exception {
        final JsonNode packagesJson = objectMapper.readTree(packages.getInputStream());
        final List<Package> packages = new ArrayList<>();

        for (int i = 0; i < packagesJson.get("_embedded").get("packages").size(); i++) {
            packages.add(i, objectMapper.readValue(packagesJson.get("_embedded").get("packages").get(i).toString(), Package.class));
        }

        return packages;
    }

    private Package getJsonPackage() throws Exception {
        return objectMapper.readValue(aPackage.getInputStream(), Package.class);
    }


    @Test
    public void getPackages_whenSucceededAndNoPackage_shouldReturnEmptyList() throws Exception {
        // Given
        final Endpoint endpoint = ComponentServiceClient.GET_COMPONENTS_ENDPOINT;
        final String userId = UUID.randomUUID().toString();
        final Pageable pageable = new PageRequest(0, 10);
        mockServer.expect(method(endpoint.getMethod()))
                .andExpect(requestTo(endpoint.withBase(baseUrl).pageable(pageable).getURI(userId)))
                .andRespond(withSuccess().body(packagesEmpty));

        // When
        final PageImpl<Package> result = componentServiceClient.getPackages(userId, pageable);

        // Then
        mockServer.verify();
        assertThat(result).isNotNull().isEmpty();
    }

    @Test
    public void getPackages_whenFails_shouldThrowException() throws Exception {
        // Given
        final Endpoint endpoint = ComponentServiceClient.GET_COMPONENTS_ENDPOINT;
        final String userId = UUID.randomUUID().toString();
        final Pageable pageable = new PageRequest(0, 10);

        mockServer.expect(method(endpoint.getMethod()))
                .andExpect(requestTo(endpoint.withBase(baseUrl).pageable(pageable).getURI(userId)))
                .andRespond(withBadRequest());

        // ThenWhen
        assertThatExceptionOfType(ComponentServiceClientException.class)
                .isThrownBy(() -> componentServiceClient.getPackages(userId, pageable));
        mockServer.verify();
    }

    @Test
    public void getVersions_whenSucceeded_shouldReturnVersions() throws Exception {
        // Given
        final Endpoint endpoint = ComponentServiceClient.GET_VERSIONS_ENDPOINT;
        final String userId = UUID.randomUUID().toString();
        final String packageRef = UUID.randomUUID().toString();
        final List<Version> expectedList = getJsonVersion();
        final Pageable pageable = new PageRequest(0, 10);

        mockServer.expect(method(endpoint.getMethod()))
                .andExpect(requestTo(endpoint.withBase(baseUrl).pageable(pageable).getURI(userId, packageRef)))
                .andRespond(withSuccess().body(versions));

        // When
        final PageImpl<Version> result = componentServiceClient.getVersions(userId, packageRef, pageable);

        // Then
        mockServer.verify();
        assertThat(result).isNotEmpty()
                .hasSize(expectedList.size())
                .containsAll(expectedList);
    }

    private List<Version> getJsonVersion() throws Exception {

        final JsonNode versionsJson = objectMapper.readTree(versions.getInputStream());
        final List<Version> list = new ArrayList<>();

        for (int i = 0; i < versionsJson.get("_embedded").get("versions").size(); i++) {
            list.add(i, objectMapper.readValue(versionsJson.get("_embedded").get("versions").get(i).toString(), Version.class));
        }

        return list;
    }

    @Test
    public void getVersions_whenSucceededAndNoVersions_shouldReturnEmptyList() throws Exception {
        // Given
        final Endpoint endpoint = ComponentServiceClient.GET_VERSIONS_ENDPOINT;
        final String userId = UUID.randomUUID().toString();
        final String packageRef = UUID.randomUUID().toString();
        final Pageable pageable = new PageRequest(0, 10);
        mockServer.expect(method(endpoint.getMethod()))
                .andExpect(requestTo(endpoint.withBase(baseUrl).pageable(pageable).getURI(userId, packageRef)))
                .andRespond(withSuccess().body(versionsEmpty));

        // When
        final PageImpl<Version> result = componentServiceClient.getVersions(userId, packageRef, pageable);

        // Then
        mockServer.verify();
        assertThat(result).isNotNull().isEmpty();
    }

    @Test
    public void getVersions_whenFails_shouldThrowException() throws Exception {
        // Given
        final Endpoint endpoint = ComponentServiceClient.GET_VERSIONS_ENDPOINT;
        final String userId = UUID.randomUUID().toString();
        final String packageRef = UUID.randomUUID().toString();
        final Pageable pageable = new PageRequest(0, 10);

        mockServer.expect(method(endpoint.getMethod()))
                .andExpect(requestTo(endpoint.withBase(baseUrl).pageable(pageable).getURI(userId, packageRef)))
                .andRespond(withBadRequest());

        // ThenWhen
        assertThatExceptionOfType(ComponentServiceClientException.class)
                .isThrownBy(() -> componentServiceClient.getVersions(userId, packageRef, pageable));
        mockServer.verify();
    }

    @Test
    public void getPackage_whenSucceeded_shouldReturnPackage() throws Exception {
        // Given
        final Endpoint endpoint = ComponentServiceClient.GET_COMPONENT_ENDPOINT;
        final String userId = UUID.randomUUID().toString();
        final Package expected = getJsonPackage();


        mockServer.expect(method(endpoint.getMethod()))
                .andExpect(requestTo(endpoint.withBase(baseUrl).getURI(userId, expected.getReference())))
                .andRespond(withSuccess().body(aPackage));

        // When
        final Package result = componentServiceClient.getPackage(userId, expected.getReference());

        // Then
        mockServer.verify();
        assertThat(result).isEqualTo(expected);
    }

    @Test
    public void getPackage_whenFails_shouldThrowException() throws Exception {
        // Given
        final Endpoint endpoint = ComponentServiceClient.GET_COMPONENT_ENDPOINT;
        final String userId = UUID.randomUUID().toString();
        final String reference = UUID.randomUUID().toString();

        mockServer.expect(method(endpoint.getMethod()))
                .andExpect(requestTo(endpoint.withBase(baseUrl).getURI(userId, reference)))
                .andRespond(withBadRequest());

        // ThenWhen
        assertThatExceptionOfType(ComponentServiceClientException.class)
                .isThrownBy(() -> componentServiceClient.getPackage(userId, reference));
        mockServer.verify();
    }

    @Test
    public void getVersion_whenSucceeded_shouldReturnVersion() throws Exception {
        // Given
        final Endpoint endpoint = ComponentServiceClient.GET_VERSION_ENDPOINT;
        final String userId = UUID.randomUUID().toString();
        final String packageRef = UUID.randomUUID().toString();
        final Version expected = objectMapper.readValue(version.getInputStream(), Version.class);

        mockServer.expect(method(endpoint.getMethod()))
                .andExpect(requestTo(endpoint.withBase(baseUrl).getURI(userId, packageRef, expected.getId())))
                .andRespond(withSuccess().body(version));

        // When
        final Version result = componentServiceClient.getVersion(userId, packageRef, expected.getId());

        // Then
        mockServer.verify();
        assertThat(result).isEqualTo(expected);
    }

    @Test
    public void getVersion_whenFails_shouldThrowException() throws Exception {
        // Given
        final Endpoint endpoint = ComponentServiceClient.GET_VERSION_ENDPOINT;
        final String userId = UUID.randomUUID().toString();
        final String packageRef = UUID.randomUUID().toString();
        final Version expected = VersionUtils.getVersion();

        mockServer.expect(method(endpoint.getMethod()))
                .andExpect(requestTo(endpoint.withBase(baseUrl).getURI(userId, packageRef, expected.getId())))
                .andRespond(withBadRequest());

        // ThenWhen
        assertThatExceptionOfType(ComponentServiceClientException.class)
                .isThrownBy(() -> componentServiceClient.getVersion(userId, packageRef, expected.getId()));
        mockServer.verify();
    }
}
