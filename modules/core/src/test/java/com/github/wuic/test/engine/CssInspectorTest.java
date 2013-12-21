/*
 * "Copyright (c) 2013   Capgemini Technology Services (hereinafter "Capgemini")
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
import com.github.wuic.engine.Engine;
import com.github.wuic.engine.EngineRequest;
import com.github.wuic.engine.impl.embedded.CGCssInspectorEngine;
import com.github.wuic.nut.Nut;
import com.github.wuic.nut.NutDao;
import com.github.wuic.nut.NutsHeap;
import junit.framework.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.ByteArrayInputStream;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * <p>
 * {@link com.github.wuic.engine.impl.embedded.CGCssInspectorEngine} tests.
 * </p>
 *
 * @author Guillaume DROUET
 * @version 1.0
 * @since 0.4.1
 */
@RunWith(JUnit4.class)
public class CssInspectorTest {

    /**
     * <p>
     * Test when multiple @import/background are declared on the same line.
     * </p>
     *
     * @throws Exception if test fails
     */
    @Test
    public void multipleImportPerLineTest() throws Exception {
        final AtomicInteger createCount = new AtomicInteger(0);
        final NutDao dao = Mockito.mock(NutDao.class);
        Mockito.when(dao.withRootPath(Mockito.anyString())).thenReturn(dao);
        Mockito.when(dao.create(Mockito.anyString())).thenAnswer(new Answer<Object>() {

            /**
             * {@inheritDoc}
             */
            @Override
            public Object answer(final InvocationOnMock invocationOnMock) throws Throwable {
                final Map<Nut, Long> retval = new HashMap<Nut, Long>();
                final Nut nut = Mockito.mock(Nut.class);
                Mockito.when(nut.getName()).thenReturn(String.valueOf(createCount.incrementAndGet()));
                Mockito.when(nut.getNutType()).thenReturn(NutType.CSS);
                Mockito.when(nut.openStream()).thenReturn(new ByteArrayInputStream("".getBytes()));
                retval.put(nut, -1L);
                return retval;
            }
        });
        final Engine engine = new CGCssInspectorEngine(true, "UTF-8");
        final NutsHeap heap = Mockito.mock(NutsHeap.class);
        Mockito.when(heap.getNutDao()).thenReturn(dao);
        Mockito.when(heap.getId()).thenReturn("heap");
        final Set<Nut> nuts = new HashSet<Nut>();
        final Nut nut = Mockito.mock(Nut.class);
        Mockito.when(nut.getNutType()).thenReturn(NutType.CSS);
        Mockito.when(nut.getName()).thenAnswer(new Answer<Object>() {

            /**
             * {@inheritDoc}
             */
            @Override
            public Object answer(final InvocationOnMock invocationOnMock) throws Throwable {
                return createCount.get() + ".css";
            }
        });
        nuts.add(nut);

        final StringBuilder builder = new StringBuilder();
        builder.append("@import url(\"jquery.ui.core.css\");");
        builder.append("@import \"jquery.ui.accordion.css\";");
        builder.append("@import 'jquery.ui.autocomplete.css';");
        builder.append("@import url('jquery.ui.button.css');");
        builder.append("@import \"jquery.ui.datepicker.css\";");
        builder.append("@import 'jquery.ui.dialog.css';");
        builder.append("@import url(  \"jquery.ui.menu.css\");");
        builder.append("background: url(\"sprite.png\");");
        builder.append("background: url(sprite2.png);");
        builder.append("background: url('sprite3.png');");
        builder.append("background: #FFF url('sprite4.png');");
        builder.append("@import /* some comments */ url(\"jquery.ui.spinner.css\");");

        Mockito.when(nut.openStream()).thenReturn(new ByteArrayInputStream(builder.toString().getBytes()));
        Mockito.when(heap.getNuts()).thenReturn(nuts);
        final EngineRequest request = new EngineRequest("wid", "cp", heap, new HashMap<NutType, Engine>());
        engine.parse(request);
        Assert.assertEquals(createCount.get(), 12);
    }
}
