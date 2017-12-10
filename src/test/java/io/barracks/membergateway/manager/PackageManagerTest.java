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

import io.barracks.membergateway.client.ComponentServiceClient;
import io.barracks.membergateway.model.Package;
import io.barracks.membergateway.utils.PackageUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class PackageManagerTest {
    @Mock
    private ComponentServiceClient componentServiceClient;
    @InjectMocks
    private PackageManager packageManager;

    @Test
    public void createPackage_shouldCallClient_andReturnResult() {
        // Given
        final String userId = UUID.randomUUID().toString();
        final Package aPackage = PackageUtils.getPackage();
        final Package expected = PackageUtils.getPackage();
        doReturn(expected).when(componentServiceClient).createPackage(userId, aPackage);

        // When
        final Package result = packageManager.createPackage(userId, aPackage);

        // Then
        verify(componentServiceClient).createPackage(userId, aPackage);
        assertThat(result).isEqualTo(expected);
    }

    @Test
    public void getPackages_shouldCallClient_andReturnResult() {
        // Given
        final String userId = UUID.randomUUID().toString();
        final List<Package> list = Collections.singletonList(PackageUtils.getPackage());
        final Pageable pageable = new PageRequest(0, 10);
        final PageImpl<Package> response = new PageImpl<>(list, pageable, list.size());

        when(componentServiceClient.getPackages(userId, pageable)).thenReturn(response);

        // When
        final Page<Package> result = packageManager.getPackages(userId, pageable);

        // Then
        verify(componentServiceClient).getPackages(userId, pageable);
        assertThat(result).isEqualTo(response);
    }

    @Test
    public void getPackage_shouldCallClient_andReturnResult() {
        // Given
        final String userId = UUID.randomUUID().toString();
        final Package expected = PackageUtils.getPackage();

        when(componentServiceClient.getPackage(userId, expected.getReference())).thenReturn(expected);

        // When
        final Package result = packageManager.getPackage(userId, expected.getReference());

        // Then
        verify(componentServiceClient).getPackage(userId, expected.getReference());
        assertThat(result).isEqualTo(expected);
    }
}
