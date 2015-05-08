/*
 * "Copyright (c) 2015   Capgemini Technology Services (hereinafter "Capgemini")
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

import com.github.wuic.NutType;
import com.github.wuic.engine.EngineRequest;
import com.github.wuic.engine.EngineRequestBuilder;
import com.github.wuic.engine.core.GzipEngine;
import com.github.wuic.nut.ByteArrayNut;
import com.github.wuic.nut.ConvertibleNut;
import com.github.wuic.nut.Nut;
import com.github.wuic.nut.NutsHeap;
import com.github.wuic.util.NutUtils;
import com.github.wuic.util.Pipe;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mockito;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PushbackInputStream;
import java.util.Arrays;
import java.util.List;

/**
 * <p>
 * Test for GZIP support.
 * </p>
 *
 * @author Guillaume DROUET
 * @version 1.0
 * @since 0.5.0
 */
@RunWith(JUnit4.class)
public class GzipEngineTest {

    /**
     * Tests when GZIP is enabled.
     *
     * @throws Exception if test fails
     */
    @Test
    public void enableGzipTest() throws Exception {
        final GzipEngine gzipEngine = new GzipEngine(true);

        final Nut nut = new ByteArrayNut("var foo = 1;".getBytes(), "foo.js", NutType.JAVASCRIPT, 1L, false);
        final NutsHeap heap = Mockito.mock(NutsHeap.class);
        Mockito.when(heap.getNuts()).thenReturn(Arrays.asList(nut));

        final EngineRequest request = new EngineRequestBuilder("workflow", heap, null).build();
        final List<ConvertibleNut> res = gzipEngine.parse(request);
        Assert.assertEquals(1, res.size());
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        res.get(0).transform(new Pipe.DefaultOnReady(bos));
        final PushbackInputStream pb = new PushbackInputStream(new ByteArrayInputStream(bos.toByteArray()), 2 );
        final byte[] signature = new byte[2];
        pb.read(signature);
        pb.unread(signature);

        // Check magic number
        Assert.assertEquals(signature[0], (byte) 0x1f);
        Assert.assertEquals(signature[1], (byte) 0x8b );
    }

    /**
     * Disables GZIP.
     *
     * @throws Exception if test fails
     */
    @Test
    public void disableGzipTest() throws Exception {
        final GzipEngine gzipEngine = new GzipEngine(false);

        final Nut nut = new ByteArrayNut("var foo = 1;".getBytes(), "foo.js", NutType.JAVASCRIPT, 1L, false);
        final NutsHeap heap = Mockito.mock(NutsHeap.class);
        Mockito.when(heap.getNuts()).thenReturn(Arrays.asList(nut));

        final EngineRequest request = new EngineRequestBuilder("workflow", heap, null).build();
        final List<ConvertibleNut> res = gzipEngine.parse(request);
        Assert.assertEquals("var foo = 1;", NutUtils.readTransform(res.get(0)));
    }
}
