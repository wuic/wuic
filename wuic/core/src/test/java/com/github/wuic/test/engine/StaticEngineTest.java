/*
 * "Copyright (c) 2016   Capgemini Technology Services (hereinafter "Capgemini")
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


package com.github.wuic.test.engine;

import com.github.wuic.ClassPathResourceResolver;
import com.github.wuic.engine.EngineRequestBuilder;
import com.github.wuic.engine.core.StaticEngine;
import com.github.wuic.exception.WuicException;
import com.github.wuic.nut.ConvertibleNut;
import com.github.wuic.nut.Nut;
import com.github.wuic.nut.NutsHeap;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mockito;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
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
     * <p>
     * Nominal test with a known workflow.
     * </p>
     *
     * @throws WuicException if test fails
     * @throws IOException if test fails
     */
    @Test(timeout = 60000)
    public void staticExistingWorkflowTest() throws WuicException, IOException {
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

        // Three lines in /wuic-static/workflow
        List<ConvertibleNut> res = engine.parse(new EngineRequestBuilder("workflow", heap, null).build());
        Assert.assertEquals(res.size(), 3);

        try {
            res.get(0).openStream();
            Assert.fail();
        } catch (IOException we) {
            // Normal behavior
        }

        // Test cache
        res = engine.parse(new EngineRequestBuilder("workflow", heap, null).build());
        Assert.assertEquals(res.size(), 3);
    }

    /**
     * Test when an unknown workflow is retrieved.
     *
     * @throws WuicException if test fails
     */
    @Test(timeout = 60000)
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
            engine.parse(new EngineRequestBuilder("foo", heap, null).build());
            Assert.fail();
        } catch (WuicException we) {
            // Normal behavior
        }
    }
}
