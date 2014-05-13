/*
 * "Copyright (c) 2014   Capgemini Technology Services (hereinafter "Capgemini")
 *
 * License/Terms of Use
 * Permission is hereby granted, free of charge and for the term of intellectual
 * property rights on the Software, to any person obtaining a copy of this software
 * and associated documentation files (the "Software"), to use, copy, modify and
 * propagate free of charge, anywhere in the world, all or part of the Software
 * subject to the following mandatory conditions:
 *
 * -   The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * Any failure to comply with the above shall automatically terminate the license
 * and be construed as a breach of these Terms of Use causing significant harm to
 * Capgemini.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, PEACEFUL ENJOYMENT,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS
 * OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 * Except as contained in this notice, the name of Capgemini shall not be used in
 * advertising or otherwise to promote the use or other dealings in this Software
 * without prior written authorization from Capgemini.
 *
 * These Terms of Use are subject to French law.
 *
 * IMPORTANT NOTICE: The WUIC software implements software components governed by
 * open source software licenses (BSD and Apache) of which CAPGEMINI is not the
 * author or the editor. The rights granted on the said software components are
 * governed by the specific terms and conditions specified by Apache 2.0 and BSD
 * licenses."
 */


package com.github.wuic.test.filter;

import com.github.wuic.ApplicationConfig;
import com.github.wuic.exception.BuilderPropertyNotSupportedException;
import com.github.wuic.nut.filter.NutFilter;
import com.github.wuic.nut.filter.RegexRemoveNutFilterBuilder;
import com.github.wuic.util.GenericBuilder;
import junit.framework.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.Arrays;

/**
 * <p>
 * Tests suite for {@link com.github.wuic.nut.filter.RegexRemoveNutFilter} and its builder.
 * </p>
 *
 * @author Guillaume DROUET
 * @version 1.0
 * @since 0.4.5
 */
@RunWith(JUnit4.class)
public class RegexRemoveFilterTest {

    /**
     * Tests the builder with default state.
     */
    @Test
    public void defaultBuilderTest() {
        final RegexRemoveNutFilterBuilder b = new RegexRemoveNutFilterBuilder();
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
        final RegexRemoveNutFilterBuilder b = new RegexRemoveNutFilterBuilder();
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
        final GenericBuilder b = new RegexRemoveNutFilterBuilder()
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
    @Test(expected = BuilderPropertyNotSupportedException.class)
    public void badPropertyTest() throws Exception {
        new RegexRemoveNutFilterBuilder().property("foo", "b");
    }

    /**
     * Tests the builder with two regex.
     *
     * @throws Exception if test fails
     */
    @Test
    public void twoRegexBuilderTest() throws Exception {
        final RegexRemoveNutFilterBuilder b = new RegexRemoveNutFilterBuilder();
        final NutFilter f = NutFilter.class.cast(b.property(ApplicationConfig.REGEX_EXPRESSIONS, "b\nc").build());
        Assert.assertEquals(1, f.filterPaths(Arrays.asList("a", "b", "c")).size());
    }

    /**
     * Tests the builder with line feed regex.
     *
     * @throws Exception if test fails
     */
    @Test
    public void lineFeedRegexBuilderTest() throws Exception {
        final RegexRemoveNutFilterBuilder b = new RegexRemoveNutFilterBuilder();
        final NutFilter f = NutFilter.class.cast(b.property(ApplicationConfig.REGEX_EXPRESSIONS, "b\\nc").build());
        Assert.assertEquals(0, f.filterPaths(Arrays.asList("b\nc")).size());
    }
}
