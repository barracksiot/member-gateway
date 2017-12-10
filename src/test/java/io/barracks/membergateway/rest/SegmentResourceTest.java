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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.barracks.membergateway.manager.SegmentManager;
import io.barracks.membergateway.manager.UpdateManager;
import io.barracks.membergateway.model.DetailedUpdate;
import io.barracks.membergateway.model.Device;
import io.barracks.membergateway.model.Segment;
import io.barracks.membergateway.rest.entity.SegmentsOrder;
import io.barracks.membergateway.utils.DeviceUtils;
import io.barracks.membergateway.utils.RandomPrincipal;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.util.FileCopyUtils;

import java.security.Principal;
import java.util.*;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@RunWith(SpringRunner.class)
@BarracksResourceTest(controllers = SegmentResource.class)
public class SegmentResourceTest {
    @MockBean
    private SegmentManager segmentManager;
    @MockBean
    private UpdateManager updateManager;
    @Autowired
    private SegmentResource segmentResource;
    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private PagedResourcesAssembler<Device> pagedResourcesAssembler;
    @Value("classpath:io/barracks/membergateway/rest/segment.json")
    private Resource segment;

    private Principal principal = new RandomPrincipal();

    @Test
    public void createSegment_shouldCallManagerAndReturnSegment() throws Exception {
        // Given
        final Segment request = objectMapper.readValue(segment.getInputStream(), Segment.class);
        final Segment response = Segment.builder()
                .id(UUID.randomUUID().toString())
                .userId(principal.getName())
                .name(request.getName())
                .query(request.getQuery())
                .build();
        doReturn(response).when(segmentManager).createSegment(principal.getName(), request);

        // When
        ResultActions result = mvc.perform(
                post("/segments")
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(FileCopyUtils.copyToByteArray(segment.getInputStream()))
                        .principal(principal)
        );

        // Then
        verify(segmentManager).createSegment(principal.getName(), request);
        result.andExpect(status().isOk())
                .andExpect(content().json(objectMapper.writeValueAsString(response)));
    }

    @Test
    public void updateSegment_shouldCallManagerAndReturnSegment() throws Exception {
        // Given
        final String segmentId = UUID.randomUUID().toString();
        final Segment request = objectMapper.readValue(segment.getInputStream(), Segment.class);
        final Segment response = Segment.builder()
                .id(segmentId)
                .userId(principal.getName())
                .name(request.getName())
                .query(request.getQuery())
                .build();
        doReturn(response).when(segmentManager).updateSegment(principal.getName(), segmentId, request);

        // When
        ResultActions result = mvc.perform(
                MockMvcRequestBuilders.put("/segments/" + segmentId)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(FileCopyUtils.copyToByteArray(segment.getInputStream()))
                        .principal(principal)
        );

        // Then
        verify(segmentManager).updateSegment(principal.getName(), segmentId, request);
        result.andExpect(status().isOk())
                .andExpect(content().json(objectMapper.writeValueAsString(response)));
    }

    @Test
    public void getSegment_shouldCallManagerAndReturnSegment() throws Exception {
        // Given
        final String segmentId = UUID.randomUUID().toString();
        final Segment response = Segment.builder()
                .id(segmentId)
                .userId(principal.getName())
                .name("aName")
                .query(new ObjectNode(new JsonNodeFactory(false)))
                .build();
        doReturn(response).when(segmentManager).getSegmentForUser(principal.getName(), segmentId);

        // When
        ResultActions result = mvc.perform(
                MockMvcRequestBuilders.get("/segments/" + segmentId)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .principal(principal)
        );

        // Then
        verify(segmentManager).getSegmentForUser(principal.getName(), segmentId);
        result.andExpect(status().isOk())
                .andExpect(content().json(objectMapper.writeValueAsString(response)));
    }

    @Test
    public void getSegments_shouldCallManagerAndReturnSegments() throws Exception {
        // Given
        final Segment segment = Segment.builder()
                .id(UUID.randomUUID().toString())
                .userId(principal.getName())
                .name("aName")
                .query(new ObjectNode(new JsonNodeFactory(false)))
                .build();
        final Page<Segment> segmentPage = new PageImpl<>(Collections.singletonList(segment));
        doReturn(segmentPage).when(segmentManager).getSegmentsForUser(eq(principal.getName()), any());

        // When
        ResultActions result = mvc.perform(
                MockMvcRequestBuilders.get("/segments")
                        .accept(MediaTypes.HAL_JSON)
                        .principal(principal)
        );

        // Then
        verify(segmentManager).getSegmentsForUser(eq(principal.getName()), any());
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.segments").value(objectMapper.readValue(objectMapper.writeValueAsString(segment), Map.class)));
    }

    @Test
    public void getDevicesBySegment_shouldCallManagerAndReturnDevices() throws Exception {
        // Given
        final String segmentId = UUID.randomUUID().toString();
        final String unitId = UUID.randomUUID().toString();
        final Device device = DeviceUtils.buildDevice(unitId);
        final Page<Device> devicePage = new PageImpl<>(Collections.singletonList(device));
        doReturn(devicePage).when(segmentManager).getDevicesBySegment(eq(principal.getName()), eq(segmentId), any());

        // When
        final ResultActions result = mvc.perform(
                MockMvcRequestBuilders.get("/segments/" + segmentId + "/devices")
                        .contentType(MediaTypes.HAL_JSON)
                        .accept(MediaTypes.HAL_JSON)
                        .principal(principal)
        );

        // Then
        verify(segmentManager).getDevicesBySegment(eq(principal.getName()), eq(segmentId), any());
        result.andExpect(status().isOk())
                .andExpect(jsonPath("_embedded.devices").value(hasSize(1)));
    }

    @Test
    public void getUpdatesBySegment_shouldCallManagerAndReturnUpdate() throws Exception {
        // Given
        final String segmentId = UUID.randomUUID().toString();
        final DetailedUpdate detailedUpdate = DetailedUpdate.builder().uuid(UUID.randomUUID().toString()).build();
        final Page<DetailedUpdate> detailedUpdates = new PageImpl<>(Collections.singletonList(detailedUpdate));
        doReturn(detailedUpdates).when(updateManager)
                .getUpdatesByStatusesAndSegments(any(), eq(principal.getName()), eq(Collections.emptyList()), eq(Collections.singletonList(segmentId)));

        // When
        ResultActions result = mvc.perform(
                MockMvcRequestBuilders.get("/segments/" + segmentId + "/updates")
                        .accept(MediaTypes.HAL_JSON)
                        .principal(principal)
        );

        // Then
        verify(updateManager).getUpdatesByStatusesAndSegments(any(), eq(principal.getName()), eq(Collections.emptyList()), eq(Collections.singletonList(segmentId)));
        result.andExpect(status().isOk())
                .andExpect(jsonPath("_embedded.updates").value(objectMapper.readValue(objectMapper.writeValueAsString(detailedUpdate), Map.class)));
    }

    @Test
    public void getOrderedSegments_shouldCallManager_andReturnResult() throws Exception {
        // Given
        final SegmentsOrder expected = SegmentsOrder.builder().build();
        doReturn(expected).when(segmentManager).getOrderedSegments(principal.getName());

        // When
        final ResultActions result = mvc.perform(
                MockMvcRequestBuilders.get("/segments/order")
                        .accept(MediaType.APPLICATION_JSON)
                        .principal(principal)
        );

        // Then
        verify(segmentManager).getOrderedSegments(principal.getName());
        result.andExpect(status().isOk())
                .andExpect(content().json(objectMapper.writeValueAsString(expected)));
    }

    @Test
    public void updateSegmentsOrder_shouldCallManagerAndReturnResults() throws Exception {
        // Given
        final List<String> order = Arrays.asList(UUID.randomUUID().toString(), UUID.randomUUID().toString());
        final List<String> expected = Arrays.asList(UUID.randomUUID().toString(), UUID.randomUUID().toString());
        doReturn(expected).when(segmentManager).updateSegmentsOrder(principal.getName(), order);

        // When
        final ResultActions result = mvc.perform(
                post("/segments/order")
                        .principal(principal)
                        .content(objectMapper.writeValueAsString(order))
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
        );

        // Then
        verify(segmentManager).updateSegmentsOrder(principal.getName(), order);
        result.andExpect(status().isOk())
                .andExpect(content().json(objectMapper.writeValueAsString(expected)));
    }
}
