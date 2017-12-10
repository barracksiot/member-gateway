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

package io.barracks.membergateway.client.util;

import org.junit.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;

public class PageableHelperTest {

    @Test
    public void whenOnlyPageAndSize_toUriQuery_shouldOnlyHavePageAndSize() {
        Pageable pageable = new PageRequest(4, 15);
        String query = PageableHelper.toUriQuery(pageable);
        URI uri = URI.create("null/?" + query);
        UriComponents components = UriComponentsBuilder.fromUri(uri).build();
        assertThat(components.getQueryParams().getFirst("page")).isEqualTo("4");
        assertThat(components.getQueryParams().getFirst("size")).isEqualTo("15");
        assertThat(components.getQueryParams().get("sort")).isNullOrEmpty();
    }

    @Test
    public void whenPageSizeAndSort_toUriQuery_shouldHavePageSizeAndSort() {
        Pageable pageable = new PageRequest(4, 15, new Sort(new Sort.Order(Sort.Direction.ASC, "name"), new Sort.Order(Sort.Direction.DESC, "age")));
        String query = PageableHelper.toUriQuery(pageable);
        URI uri = URI.create("null/?" + query);
        UriComponents components = UriComponentsBuilder.fromUri(uri).build();
        assertThat(components.getQueryParams().getFirst("page")).isEqualTo("4");
        assertThat(components.getQueryParams().getFirst("size")).isEqualTo("15");
        assertThat(components.getQueryParams().get("sort")).hasSize(2);
        assertThat(components.getQueryParams().get("sort").get(0)).isEqualTo("name,asc");
        assertThat(components.getQueryParams().get("sort").get(1)).isEqualTo("age,desc");
    }

    @Test
    public void whenPropertyMustBeEscaped_toUriQuery_shouldEscapeCharacter() {
        Pageable pageable = new PageRequest(4, 15, new Sort(new Sort.Order(Sort.Direction.ASC, "weird stuff/wtfbbq")));
        String query = PageableHelper.toUriQuery(pageable);
        URI uri = URI.create("null/?" + query);
        UriComponents components = UriComponentsBuilder.fromUri(uri).build();
        UriComponents encoded = UriComponentsBuilder.fromPath("null/").queryParam("sort", "weird stuff/wtfbbq,asc").build().encode();
        assertThat(components.getQueryParams().getFirst("page")).isEqualTo("4");
        assertThat(components.getQueryParams().getFirst("size")).isEqualTo("15");
        assertThat(components.getQueryParams().get("sort")).hasSize(1);
        assertThat(components.getQueryParams().get("sort").get(0)).isEqualTo(encoded.getQueryParams().get("sort").get(0));
    }

}
