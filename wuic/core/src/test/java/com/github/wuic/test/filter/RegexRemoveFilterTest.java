/*
 * Copyright (c) 2016   The authors of WUIC
 *
 * License/Terms of Use
 * Permission is hereby granted, free of charge and for the term of intellectual
 * property rights on the Software, to any person obtaining a copy of this software
 * and associated documentation files (the "Software"), to use, copy, modify and
 * propagate free of charge, anywhere in the world, all or part of the Software
 * subject to the following mandatory conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, PEACEFUL ENJOYMENT,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS
 * OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */


package com.github.wuic.test.filter;

import com.github.wuic.ApplicationConfig;
import com.github.wuic.config.ObjectBuilderFactory;
import com.github.wuic.nut.filter.NutFilter;
import com.github.wuic.nut.filter.NutFilterService;
import com.github.wuic.nut.filter.core.RegexRemoveNutFilter;
import com.github.wuic.config.ObjectBuilder;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.Arrays;

/**
 * <p>
 * Tests suite for {@link com.github.wuic.nut.filter.core.RegexRemoveNutFilter} and its builder.
 * </p>
 *
 * @author Guillaume DROUET
 * @since 0.4.5
 */
@RunWith(JUnit4.class)
public class RegexRemoveFilterTest {

    /**
     * Timeout.
     */
    @Rule
    public Timeout globalTimeout = Timeout.seconds(60);

    /**
     * Tests the builder with default state.
     */
    @Test
    public void defaultBuilderTest() {
        final ObjectBuilderFactory<NutFilter> factory = new ObjectBuilderFactory<NutFilter>(NutFilterService.class, RegexRemoveNutFilter.class);
        final ObjectBuilder<NutFilter> b = factory.create("RegexRemoveNutFilterBuilder");
        final NutFilter f = b.build();
        Assert.assertEquals(3, f.filterPaths(Arrays.asList("a", "b", "c")).size());
    }

    /**
     * Tests the builder with one regex.
     *
     * @throws Exception if test fails
     */
    @Test
    public void oneRegexBuilderTest() throws Exception {
        final ObjectBuilderFactory<NutFilter> factory = new ObjectBuilderFactory<NutFilter>(NutFilterService.class, RegexRemoveNutFilter.class);
        final ObjectBuilder<NutFilter> b = factory.create("RegexRemoveNutFilterBuilder");
        final NutFilter f = NutFilter.class.cast(b.property(ApplicationConfig.REGEX_EXPRESSIONS, "b").build());
        Assert.assertEquals(2, f.filterPaths(Arrays.asList("a", "b", "c")).size());
    }

    /**
     * Tests the builder in a disabled test.
     *
     * @throws Exception if test fails
     */
    @Test
    public void disabledTest() throws Exception {
        final ObjectBuilderFactory<NutFilter> factory = new ObjectBuilderFactory<NutFilter>(NutFilterService.class, RegexRemoveNutFilter.class);
        final ObjectBuilder<NutFilter> b = factory.create("RegexRemoveNutFilterBuilder")
                .property(ApplicationConfig.REGEX_EXPRESSIONS, "b")
                .property(ApplicationConfig.ENABLE, false);
        final NutFilter f = NutFilter.class.cast(b.build());
        Assert.assertEquals(3, f.filterPaths(Arrays.asList("a", "b", "c")).size());
    }

    /**
     * Tests the builder with bad property.
     *
     * @throws Exception if test fails
     */
    @Test(expected = IllegalArgumentException.class)
    public void badPropertyTest() throws Exception {
        final ObjectBuilderFactory<NutFilter> factory = new ObjectBuilderFactory<NutFilter>(NutFilterService.class, RegexRemoveNutFilter.class);
        factory.create("RegexRemoveNutFilterBuilder").property("foo", "b");
    }

    /**
     * Tests the builder with two regex.
     *
     * @throws Exception if test fails
     */
    @Test
    public void twoRegexBuilderTest() throws Exception {
        final ObjectBuilderFactory<NutFilter> factory = new ObjectBuilderFactory<NutFilter>(NutFilterService.class, RegexRemoveNutFilter.class);
        final ObjectBuilder<NutFilter> b = factory.create("RegexRemoveNutFilterBuilder");        final NutFilter f = NutFilter.class.cast(b.property(ApplicationConfig.REGEX_EXPRESSIONS, "b\nc").build());
        Assert.assertEquals(1, f.filterPaths(Arrays.asList("a", "b", "c")).size());
    }

    /**
     * Tests the builder with line feed regex.
     *
     * @throws Exception if test fails
     */
    @Test
    public void lineFeedRegexBuilderTest() throws Exception {
        final ObjectBuilderFactory<NutFilter> factory = new ObjectBuilderFactory<NutFilter>(NutFilterService.class, RegexRemoveNutFilter.class);
        final ObjectBuilder<NutFilter> b = factory.create("RegexRemoveNutFilterBuilder");        final NutFilter f = NutFilter.class.cast(b.property(ApplicationConfig.REGEX_EXPRESSIONS, "b\\nc").build());
        Assert.assertEquals(0, f.filterPaths(Arrays.asList("b\nc")).size());
    }
}
