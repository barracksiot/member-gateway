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
import com.fasterxml.jackson.databind.ObjectMapper;
import io.barracks.membergateway.exception.InvalidOwnerException;
import io.barracks.membergateway.manager.DeviceConfigurationManager;
import io.barracks.membergateway.model.DeviceConfiguration;
import io.barracks.membergateway.utils.RandomPrincipal;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.client.HttpServerErrorException;

import java.security.Principal;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@BarracksResourceTest(controllers = DeviceConfigurationResource.class)
public class DeviceConfigurationResourceTest {
    @Autowired
    private ObjectMapper objectMapper;
    @Value("classpath:io/barracks/membergateway/rest/DeviceConfigurationResourceTest-setConfiguration-request.json")
    private Resource setConfigurationRequestResource;
    private JsonNode setConfigurationRequest;
    @Autowired
    private MockMvc mvc;
    @MockBean
    private DeviceConfigurationManager deviceConfigurationManager;
    private Principal principal;

    @Before
    public void setUp() throws Exception {
        this.principal = new RandomPrincipal();
        setConfigurationRequest = objectMapper.readTree(setConfigurationRequestResource.getInputStream());
    }

    @Test
    public void getConfiguration_whenSucceeds_shouldReturnConfiguration() throws Exception {
        // Given
        final String userId = principal.getName();
        final String unitId = UUID.randomUUID().toString();
        final DeviceConfiguration configuration = DeviceConfiguration.builder().build();
        doReturn(configuration).when(deviceConfigurationManager).getConfiguration(userId, unitId);

        // When
        final ResultActions result = mvc.perform(
                MockMvcRequestBuilders.get("/devices/configuration/" + unitId)
                        .accept(MediaType.APPLICATION_JSON_UTF8)
                        .principal(principal)
        );

        // Then
        verify(deviceConfigurationManager).getConfiguration(userId, unitId);
        result.andExpect(status().isOk());
    }

    @Test
    public void getConfiguration_whenExceptionThrown_shouldReturnError() throws Exception {
        // Given
        final String userId = principal.getName();
        final String unitId = UUID.randomUUID().toString();
        final Exception exception = new AnyBarracksClientException(
                new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR)
        );
        doThrow(exception).when(deviceConfigurationManager).getConfiguration(userId, unitId);

        // When
        final ResultActions result = mvc.perform(
                MockMvcRequestBuilders.get("/devices/configuration/" + unitId)
                        .accept(MediaType.APPLICATION_JSON_UTF8)
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .principal(principal)
        );

        // Then
        verify(deviceConfigurationManager).getConfiguration(userId, unitId);
        result.andExpect(status().isInternalServerError());
    }

    @Test
    public void setConfiguration_whenSucceeds_shouldReturnCreatedConfiguration() throws Exception {
        // Given
        final String userId = principal.getName();
        final String unitId = UUID.randomUUID().toString();

        final DeviceConfiguration requested = DeviceConfiguration.builder().build();
        final DeviceConfiguration created = DeviceConfiguration.builder().build();
        doReturn(created).when(deviceConfigurationManager).setConfiguration(userId, unitId, requested);

        // When
        final ResultActions result = mvc.perform(
                MockMvcRequestBuilders.post("/devices/configuration/" + unitId)
                        .accept(MediaType.APPLICATION_JSON_UTF8)
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .content(setConfigurationRequest.toString())
                        .principal(principal)
        );

        // Then
        verify(deviceConfigurationManager).setConfiguration(userId, unitId, requested);
        result.andExpect(status().isOk());
    }

    @Test
    public void setConfiguration_whenExceptionThrown_shouldReturnError() throws Exception {
        // Given
        final String userId = principal.getName();
        final String unitId = UUID.randomUUID().toString();
        final Exception exception = new AnyBarracksClientException(
                new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR)
        );
        final DeviceConfiguration requested = DeviceConfiguration.builder().build();
        doThrow(exception).when(deviceConfigurationManager).setConfiguration(userId, unitId, requested);

        // When
        final ResultActions result = mvc.perform(
                MockMvcRequestBuilders.post("/devices/configuration/" + unitId)
                        .accept(MediaType.APPLICATION_JSON_UTF8)
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .principal(principal)
                        .content(setConfigurationRequest.toString())
        );

        // Then
        verify(deviceConfigurationManager).setConfiguration(userId, unitId, requested);
        result.andExpect(status().isInternalServerError());
    }

    @Test
    public void setConfiguration_whenInvalidOwnerThrown_shouldReturnError() throws Exception {
        // Given
        final String userId = principal.getName();
        final String unitId = UUID.randomUUID().toString();
        final Exception exception = new InvalidOwnerException("Expected failure");
        final DeviceConfiguration requested = DeviceConfiguration.builder().build();
        doThrow(exception).when(deviceConfigurationManager).setConfiguration(userId, unitId, requested);

        // When
        final ResultActions result = mvc.perform(
                MockMvcRequestBuilders.post("/devices/configuration/" + unitId)
                        .accept(MediaType.APPLICATION_JSON_UTF8)
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .principal(principal)
                        .content(setConfigurationRequest.toString())
        );

        // Then
        verify(deviceConfigurationManager).setConfiguration(userId, unitId, requested);
        result.andExpect(status().isForbidden());
    }

}