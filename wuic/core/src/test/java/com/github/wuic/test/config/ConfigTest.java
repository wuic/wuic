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


package com.github.wuic.test.config;

import com.github.wuic.config.ObjectBuilder;
import com.github.wuic.config.ObjectBuilderFactory;
import com.github.wuic.exception.BuilderPropertyNotSupportedException;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Configuration API tests.
 *
 * @author Guillaume DROUET
 * @version 1.0
 * @since 0.5
 */
@RunWith(JUnit4.class)
public class ConfigTest {

    /**
     * The factory.
     */
    private static ObjectBuilderFactory<I> factory;

    /**
     * Initializes the factory.
     */
    @BeforeClass
    public static void scan() {
        factory = new ObjectBuilderFactory<I>(IService.class, "com.github.wuic.test.config");
    }

    /**
     * Tests a default build.
     */
    @Test
    public void builderDefaultValueTest() {
        final ObjectBuilder<I> b = factory.create("MyServiceBuilder");
        final I i = b.build();

        Assert.assertEquals(MyService.class, i.getClass());
        Assert.assertEquals(1, MyService.class.cast(i).foo);
    }

    /**
     * Tests a build with a property not set by default.
     *
     * @throws Exception if test fails
     */
    @Test
    public void builderSpecificValueTest() throws Exception {
        final ObjectBuilder<I> b = factory.create("MyServiceBuilder");
        b.property("int", 2);
        final I i = b.build();

        Assert.assertEquals(MyService.class, i.getClass());
        Assert.assertEquals(2, MyService.class.cast(i).foo);
    }

    /**
     * Tests a build with an unknown property.
     *
     * @throws Exception if test succeed
     */
    @Test(expected = BuilderPropertyNotSupportedException.class)
    public void builderBadPropertyTest() throws Exception {
        final ObjectBuilder<I> b = factory.create("MyServiceBuilder");
        b.property("bar", 2);
    }

    /**
     * Tests bad usage detection.
     */
    @Test
    public void badServiceTest() {
        Assert.assertNull(factory.create("MyBadServiceBuilder"));
    }

    /**
     * Tests bad usage detection.
     */
    @Test(expected = IllegalArgumentException.class)
    public void badInnerServiceTest() {
        factory.create("MyInnerBadServiceBuilder");
    }
}
