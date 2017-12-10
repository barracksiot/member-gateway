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

package io.barracks.membergateway.utils;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.hamcrest.number.BigDecimalCloseTo;

import java.math.BigDecimal;

public class NumberCloseTo extends TypeSafeMatcher<Number> {
    private final BigDecimalCloseTo bigDecimalCloseTo;

    private NumberCloseTo(BigDecimal expected) {
        this.bigDecimalCloseTo = new BigDecimalCloseTo(expected, BigDecimal.valueOf(0.001));
    }

    public static Matcher<Number> closeTo(BigDecimal expected) {
        return new NumberCloseTo(expected);
    }

    @Override
    protected boolean matchesSafely(Number item) {
        if (item instanceof Long || item instanceof Integer) {
            return bigDecimalCloseTo.matchesSafely(BigDecimal.valueOf(item.longValue()));
        } else {
            return bigDecimalCloseTo.matchesSafely(BigDecimal.valueOf(item.doubleValue()));
        }
    }

    @Override
    public void describeTo(Description description) {
        bigDecimalCloseTo.describeTo(description);
    }
}