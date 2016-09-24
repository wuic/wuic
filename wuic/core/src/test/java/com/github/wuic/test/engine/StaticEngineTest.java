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


package com.github.wuic.test.engine;

import com.github.wuic.ClassPathResourceResolver;
import com.github.wuic.NutTypeFactory;
import com.github.wuic.engine.EngineRequestBuilder;
import com.github.wuic.engine.core.StaticEngine;
import com.github.wuic.exception.WuicException;
import com.github.wuic.nut.ConvertibleNut;
import com.github.wuic.nut.Nut;
import com.github.wuic.nut.NutsHeap;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mockito;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * {@link com.github.wuic.engine.core.StaticEngine} tests.
 * </p>
 *
 * @author Guillaume DROUET
 * @since 0.4.1
 */
@RunWith(JUnit4.class)
public class StaticEngineTest {

    /**
     * Timeout.
     */
    @Rule
    public Timeout globalTimeout = Timeout.seconds(60);

    /**
     * <p>
     * Nominal test with a known workflow.
     * </p>
     *
     * @throws WuicException if test fails
     * @throws IOException if test fails
     */
    @Test
    public void staticExistingWorkflowTest() throws WuicException, IOException {
        final StaticEngine engine = new StaticEngine();
        engine.setNutTypeFactory(new NutTypeFactory(Charset.defaultCharset().displayName()));
        engine.setClasspathResourceResolver(new ClassPathResourceResolver() {
            @Override
            public URL getResource(String resourcePath) throws MalformedURLException {
                return getClass().getResource("/" + resourcePath);
            }

            @Override
            public InputStream getResourceAsStream(String resourcePath) {
                return getClass().getResourceAsStream("/" + resourcePath);
            }
        });

        final NutsHeap heap = Mockito.mock(NutsHeap.class);
        Mockito.when(heap.getNuts()).thenReturn(new ArrayList<Nut>());

        // Three lines in /wuic-static/workflow
        List<ConvertibleNut> res = engine.parse(new EngineRequestBuilder("workflow", heap, null, new NutTypeFactory(Charset.defaultCharset().displayName())).build());
        Assert.assertEquals(res.size(), 3);

        try {
            res.get(0).openStream();
            Assert.fail();
        } catch (IOException we) {
            // Normal behavior
        }

        // Test cache
        res = engine.parse(new EngineRequestBuilder("workflow", heap, null, new NutTypeFactory(Charset.defaultCharset().displayName())).build());
        Assert.assertEquals(res.size(), 3);
    }

    /**
     * Test when an unknown workflow is retrieved.
     *
     * @throws WuicException if test fails
     */
    @Test
    public void staticWorkflowNotFoundTest() throws WuicException {
        final StaticEngine engine = new StaticEngine();
        engine.setClasspathResourceResolver(new ClassPathResourceResolver() {
            @Override
            public URL getResource(String resourcePath) throws MalformedURLException {
                return getClass().getResource(resourcePath);
            }

            @Override
            public InputStream getResourceAsStream(String resourcePath) {
                return getClass().getResourceAsStream(resourcePath);
            }
        });
        final NutsHeap heap = Mockito.mock(NutsHeap.class);
        Mockito.when(heap.getNuts()).thenReturn(new ArrayList<Nut>());

        try {
            engine.parse(new EngineRequestBuilder("foo", heap, null, new NutTypeFactory(Charset.defaultCharset().displayName())).build());
            Assert.fail();
        } catch (WuicException we) {
            // Normal behavior
        }
    }
}
