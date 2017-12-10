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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import io.barracks.commons.test.PagedResourcesUtils;
import io.barracks.membergateway.Application;
import io.barracks.membergateway.client.DeploymentServiceClient;
import io.barracks.membergateway.client.DeviceServiceClient;
import io.barracks.membergateway.exception.FilterInUseException;
import io.barracks.membergateway.model.BarracksQuery;
import io.barracks.membergateway.model.Device;
import io.barracks.membergateway.model.Filter;
import io.barracks.membergateway.utils.DeviceUtils;
import io.barracks.membergateway.utils.FilterUtils;
import org.assertj.core.util.Lists;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.PagedResources;

import java.io.IOException;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
@SpringBootTest(classes = Application.class)
public class FilterManagerTest {
    @Mock
    private DeviceServiceClient deviceServiceClient;
    @Mock
    private DeploymentServiceClient deploymentServiceClient;

    @InjectMocks
    @Spy
    private FilterManager filterManager;

    @Test
    public void createFilter_shouldCallRepository_andReturnResult() {
        // Given
        final String userId = UUID.randomUUID().toString();
        final Filter toCreate = FilterUtils.getFilter().toBuilder().name("test").build();
        final Filter expected = FilterUtils.getFilter().toBuilder().name("gerard").deploymentCount(2L).deviceCount(1L).build();
        doReturn(expected).when(deviceServiceClient).createFilter(userId, toCreate);
        doReturn(2L).when(filterManager).getAssociatedDeploymentCount(userId, expected);
        doReturn(1L).when(filterManager).getAssociatedDevicesCount(userId, expected);

        // When
        final Filter result = filterManager.createFilter(userId, toCreate);

        // Then
        verify(deviceServiceClient).createFilter(userId, toCreate);
        assertThat(result).isEqualTo(expected);
    }

    @Test
    public void updateFilter_shouldCallRepository_andReturnResult() {
        // Given
        final String userId = UUID.randomUUID().toString();
        final JsonNode query = JsonNodeFactory.instance.objectNode().put(UUID.randomUUID().toString(), UUID.randomUUID().toString());
        final Filter toUpdate = FilterUtils.getFilter().toBuilder().name("test").build();
        final Filter expected = FilterUtils.getFilter().toBuilder().name("gerard").query(query).deploymentCount(2L).deviceCount(1L).build();
        doReturn(expected).when(deviceServiceClient).updateFilter(userId, toUpdate.getName(), query);

        doReturn(2L).when(filterManager).getAssociatedDeploymentCount(userId, expected);
        doReturn(1L).when(filterManager).getAssociatedDevicesCount(userId, expected);

        // When
        final Filter result = filterManager.updateFilter(userId, toUpdate.getName(), query);

        // Then
        verify(deviceServiceClient).updateFilter(userId, toUpdate.getName(), query);
        assertThat(result).isEqualTo(expected);
    }

    @Test
    public void getFilters_shouldCallClient_andReturnResult() {
        // Given
        final String userId = UUID.randomUUID().toString();
        final List<Filter> list = Collections.singletonList(FilterUtils.getFilter().toBuilder().deploymentCount(20).deviceCount(1).build());
        final Pageable pageable = new PageRequest(0, 10);
        final Page<Filter> expected = new PageImpl<>(list, pageable, 1);
        final PagedResources<Filter> response = PagedResourcesUtils.buildPagedResources(pageable, list);

        doReturn(response).when(deviceServiceClient).getFilters(userId, pageable);
        doReturn(20L).when(filterManager).getAssociatedDeploymentCount(eq(userId), any());
        doReturn(1L).when(filterManager).getAssociatedDevicesCount(eq(userId), any());

        // When
        final Page<Filter> result = filterManager.getFilters(userId, pageable);

        // Then
        verify(deviceServiceClient).getFilters(userId, pageable);
        assertThat(result.getContent()).isEqualTo(expected.getContent());
    }

    @Test
    public void getFilter_shouldCallClient_andReturnResult() {
        // Given
        final String userId = UUID.randomUUID().toString();
        final String name = UUID.randomUUID().toString();

        final Filter getFilter = FilterUtils.getFilter();
        final Filter expected = getFilter.toBuilder().deploymentCount(20).deviceCount(1).build();

        doReturn(getFilter).when(deviceServiceClient).getFilterByUserIdAndName(userId, name);
        doReturn(20L).when(filterManager).getAssociatedDeploymentCount(userId, getFilter);
        doReturn(1L).when(filterManager).getAssociatedDevicesCount(userId, getFilter);

        // When
        final Filter result = filterManager.getFilterByUserIdAndName(userId, name);

        // Then
        verify(deviceServiceClient).getFilterByUserIdAndName(userId, name);
        verify(filterManager).getAssociatedDeploymentCount(userId, getFilter);
        verify(filterManager).getAssociatedDevicesCount(userId, getFilter);
        assertThat(result).isEqualTo(expected);
    }

    @Test
    public void getDeploymentCount_whenMultiplePages_ShouldReturnPlanCount() throws IOException {
        //Given
        final String userId = UUID.randomUUID().toString();
        final Filter filter = FilterUtils.getFilter();
        final Pageable pageable = new PageRequest(0, 5);

        final List<JsonNode> deploymentList = new ArrayList<>();
        for (int i = 0; i < 23; i++) {
            deploymentList.add(new ObjectMapper().valueToTree(UUID.randomUUID().toString()));
        }

        final PageImpl<JsonNode> expectedDeploymentPlans = new PageImpl<>(deploymentList, pageable, deploymentList.size());
        doReturn(expectedDeploymentPlans).when(deploymentServiceClient).getDeploymentPlansByFilterName(eq(userId), eq(filter.getName()), any(Pageable.class));

        //When
        final long result = filterManager.getAssociatedDeploymentCount(userId, filter);

        //Then
        verify(deploymentServiceClient).getDeploymentPlansByFilterName(eq(userId), eq(filter.getName()), any(Pageable.class));
        assertThat(result).isEqualTo(23);
    }

    @Test
    public void getDeploymentCount_whenOnlyOnePage_ShouldReturnPlanCount() throws IOException {
        //Given
        final String userId = UUID.randomUUID().toString();
        final Filter filter = FilterUtils.getFilter();
        final Pageable pageable = new PageRequest(0, 5);

        final List<JsonNode> deploymentList = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            deploymentList.add(new ObjectMapper().valueToTree(UUID.randomUUID().toString()));
        }

        final long expected = deploymentList.size();
        final PageImpl<JsonNode> expectedDeploymentPlans = new PageImpl<>(deploymentList, pageable, deploymentList.size());
        doReturn(expectedDeploymentPlans).when(deploymentServiceClient).getDeploymentPlansByFilterName(eq(userId), eq(filter.getName()), any(Pageable.class));

        //When
        final long result = filterManager.getAssociatedDeploymentCount(userId, filter);

        //Then
        verify(deploymentServiceClient).getDeploymentPlansByFilterName(eq(userId), eq(filter.getName()), any(Pageable.class));
        assertThat(result).isEqualTo(expected);
    }

    @Test
    public void getAssociatedDevicesCount_whenMultiplePages_ShouldReturnFilterWithTotalNumberOfElements() {
        //Given
        final String userId = UUID.randomUUID().toString();
        final Filter filter = FilterUtils.getFilter();
        final Pageable pageable = new PageRequest(0, 5);

        final List<Device> deviceList = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            deviceList.add(DeviceUtils.getDevice());
        }
        final PagedResources<Device> expectedDevices = PagedResourcesUtils.buildPagedResources(pageable, deviceList);
        final long expected = 20;

        doReturn(expectedDevices).when(deviceServiceClient).getDevices(eq(userId), any(Pageable.class), any(BarracksQuery.class));

        //When
        final long result = filterManager.getAssociatedDevicesCount(userId, filter);

        //Then
        verify(deviceServiceClient).getDevices(eq(userId), any(Pageable.class), any(BarracksQuery.class));
        assertThat(result).isEqualTo(expected);
    }

    @Test
    public void getAssociatedDevicesCount_whenOnePage_ShouldReturnFilterWithTotalNumberOfElements() {
        //Given
        final String userId = UUID.randomUUID().toString();
        final Filter filter = FilterUtils.getFilter();
        final Pageable pageable = new PageRequest(0, 5);

        final List<Device> deviceList = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            deviceList.add(DeviceUtils.getDevice());
        }
        final PagedResources<Device> expectedDevices = PagedResourcesUtils.buildPagedResources(pageable, deviceList);
        final long expected = 3;

        doReturn(expectedDevices).when(deviceServiceClient).getDevices(eq(userId), any(Pageable.class), any(BarracksQuery.class));

        //When
        final long result = filterManager.getAssociatedDevicesCount(userId, filter);

        //Then
        verify(deviceServiceClient).getDevices(eq(userId), any(Pageable.class), any(BarracksQuery.class));
        assertThat(result).isEqualTo(expected);
    }

    @Test
    public void deleteFilter_whenFilterIsNotUsed_shouldCallClient() throws Exception {
        // Given
        final String userId = UUID.randomUUID().toString();
        final String filterName = UUID.randomUUID().toString();
        final Pageable pageable = new PageRequest(0, 10);
        final Page<JsonNode> page = new PageImpl<>(Lists.emptyList(), pageable, 0);
        doReturn(page).when(deploymentServiceClient).getDeploymentPlansByFilterName(eq(userId), eq(filterName), any(Pageable.class));

        // When
        filterManager.deleteFilter(filterName, userId);

        // Then
        verify(deploymentServiceClient).getDeploymentPlansByFilterName(eq(userId), eq(filterName), any(Pageable.class));
        verify(deviceServiceClient).deleteFilter(userId, filterName);
    }

    @Test
    public void deleteFilter_whenFilterIsUsed_shouldCallClientThrowException() throws Exception {
        // Given
        final String userId = UUID.randomUUID().toString();
        final String filterName = UUID.randomUUID().toString();
        final Pageable pageable = new PageRequest(0, 10);
        final JsonNode jsonNode = new ObjectMapper().valueToTree(UUID.randomUUID().toString());
        final JsonNode jsonNode2 = new ObjectMapper().valueToTree(UUID.randomUUID().toString());
        final List<JsonNode> list = Arrays.asList(jsonNode, jsonNode2);
        final Page<JsonNode> page = new PageImpl<>(list, pageable, list.size());
        doReturn(page).when(deploymentServiceClient).getDeploymentPlansByFilterName(eq(userId), eq(filterName), any(Pageable.class));

        // When Then
        assertThatExceptionOfType(FilterInUseException.class).isThrownBy(
                () -> filterManager.deleteFilter(filterName, userId)
        );
        verify(deploymentServiceClient).getDeploymentPlansByFilterName(eq(userId), eq(filterName), any(Pageable.class));
        verifyZeroInteractions(deviceServiceClient);
    }
}
