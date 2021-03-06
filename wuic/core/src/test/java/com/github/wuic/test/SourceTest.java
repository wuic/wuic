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


package com.github.wuic.test;

import com.github.wuic.EnumNutType;
import com.github.wuic.NutType;
import com.github.wuic.NutTypeFactory;
import com.github.wuic.ProcessContext;
import com.github.wuic.exception.WuicException;
import com.github.wuic.nut.ConvertibleNut;
import com.github.wuic.nut.Nut;
import com.github.wuic.nut.NutsHeap;
import com.github.wuic.nut.Source;
import com.github.wuic.nut.SourceImpl;
import com.github.wuic.nut.SourceMapNut;
import com.github.wuic.nut.SourceMapNutImpl;
import com.github.wuic.nut.dao.NutDao;
import com.github.wuic.util.FutureLong;
import com.github.wuic.util.IOUtils;
import com.github.wuic.util.InMemoryInput;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * Test for source objects of nuts.
 * </p>
 *
 * @author Guillaume DROUET
 * @since 0.5.3
 */
@RunWith(JUnit4.class)
public class SourceTest {

    /**
     * Process context.
     */
    @ClassRule
    public static ProcessContextRule processContext = new ProcessContextRule();

    /**
     * Timeout.
     */
    @Rule
    public Timeout globalTimeout = Timeout.seconds(60);

    /**
     * Tests {@link SourceImpl}.
     */
    @Test
    public void defaultSource() {
        final Source src = new SourceImpl();
        Assert.assertNotNull(src.getOriginalNuts());
        src.addOriginalNut(Mockito.mock(ConvertibleNut.class));
        Assert.assertEquals(1, src.getOriginalNuts().size());
    }

    /**
     * Checks that aggregating nut without any existing source map will generate a correct simple source map.
     *
     * @throws Exception if test fails
     */
    @Test
    public void customSourceMapNutTest() throws Exception {
        final String aggregated =
                "aaaaaaffffff\n" +
                "ffffffffffff\n" +
                "fffffffggggg\n" +
                "gggggggggggg\n" +
                "gggggggggggg\n" +
                "gggggg";

        final int startLineAl1 = 0;
        final int startColAl1 = 0;
        final int endLineAl1 = 0;
        final int endColAl1 = 6;

        final int startLineFl13 = 0;
        final int startColFl13 = 6;
        final int endLineFl13 = 2;
        final int endColFl13 = 7;

        final int startLineGl3 = 2;
        final int startColGl3 = 7;
        final int endLineGl3 = 2;
        final int endColGl3 = 13;

        final int startLineGl4 = 3;
        final int startColGl4 = 0;
        final int endLineGl4 = 3;
        final int endColGl4 = 13;

        final int startLineGl5 = 4;
        final int startColGl5 = 0;
        final int endLineGl5 = 4;
        final int endColGl5 = 13;

        final int startLineGl6 = 5;
        final int startColGl6 = 0;
        final int endLineGl6 = 5;
        final int endColGl6 = 6;

        final ConvertibleNut convertibleNut = Mockito.mock(ConvertibleNut.class);
        Mockito.when(convertibleNut.getVersionNumber()).thenReturn(new FutureLong(1L));
        final ConvertibleNut a = Mockito.mock(ConvertibleNut.class);
        final ConvertibleNut f = Mockito.mock(ConvertibleNut.class);
        final ConvertibleNut g = Mockito.mock(ConvertibleNut.class);

        Mockito.when(convertibleNut.getName()).thenReturn("owner");
        Mockito.when(a.getInitialName()).thenReturn("a");
        Mockito.when(f.getInitialName()).thenReturn("f");
        Mockito.when(g.getInitialName()).thenReturn("g");

        Mockito.when(convertibleNut.toString()).thenReturn("owner");
        Mockito.when(a.toString()).thenReturn("a");
        Mockito.when(f.toString()).thenReturn("f");
        Mockito.when(g.toString()).thenReturn("g");

        final SourceMapNut sourceMapNut = new SourceMapNutImpl(convertibleNut, new NutTypeFactory(Charset.defaultCharset().displayName()));

        sourceMapNut.addSource(startLineAl1, startColAl1, endLineAl1, endColAl1, a);
        sourceMapNut.addSource(startLineFl13, startColFl13, endLineFl13, endColFl13, f);
        sourceMapNut.addSource(startLineGl3, startColGl3, endLineGl3, endColGl3, g);
        sourceMapNut.addSource(startLineGl4, startColGl4, endLineGl4, endColGl4, g);
        sourceMapNut.addSource(startLineGl5, startColGl5, endLineGl5, endColGl5, g);
        sourceMapNut.addSource(startLineGl6, startColGl6, endLineGl6, endColGl6, g);

        int lines = 0;

        for (final String line : aggregated.split("\n")) {
            for (int col = 0; col < line.length(); col++) {
                switch (line.charAt(col)) {
                    case 'a':
                        Assert.assertEquals("line: " + lines + ", col: " + col, a, sourceMapNut.getNutAt(lines, col));
                        break;
                    case 'g':
                        Assert.assertEquals("line: " + lines + ", col: " + col, g, sourceMapNut.getNutAt(lines, col));
                        break;
                    case 'f':
                        Assert.assertEquals("line: " + lines + ", col: " + col, f, sourceMapNut.getNutAt(lines, col));
                        break;
                }
            }

            lines++;
        }
    }

    /**
     * Tests {@link com.github.wuic.nut.SourceMapNutImpl}.
     *
     * @throws WuicException if test fails
     * @throws IOException   if test fails
     */
    @Test
    public void sourceMapNutTest() throws WuicException, IOException {
        final NutDao dao = Mockito.mock(NutDao.class);
        Mockito.when(dao.withRootPath(Mockito.anyString())).thenReturn(dao);
        Mockito.when(dao.create(Mockito.anyString(), Mockito.any(NutDao.PathFormat.class), Mockito.any(ProcessContext.class))).thenAnswer(new Answer<Object>() {

            /**
             * {@inheritDoc}
             */
            @Override
            public Object answer(final InvocationOnMock invocationOnMock) throws Throwable {
                final List<Nut> retval = new ArrayList<Nut>();
                final Nut nut = Mockito.mock(Nut.class);

                Mockito.when(nut.getInitialName()).thenReturn(String.valueOf(invocationOnMock.getArguments()[0]));
                Mockito.when(nut.getInitialNutType()).thenReturn(new NutTypeFactory(Charset.defaultCharset().displayName()).getNutType(String.valueOf(invocationOnMock.getArguments()[0])));
                Mockito.when(nut.getVersionNumber()).thenReturn(new FutureLong(1L));
                Mockito.when(nut.openStream()).thenReturn(new InMemoryInput(("content of " + invocationOnMock.getArguments()[0]), Charset.defaultCharset().displayName()));

                retval.add(nut);
                return retval;
            }
        });

        final NutsHeap heap = Mockito.mock(NutsHeap.class);
        Mockito.when(heap.getId()).thenReturn("heap");
        Mockito.when(heap.hasCreated(Mockito.any(Nut.class))).thenReturn(true);
        Mockito.when(heap.findDaoFor(Mockito.any(Nut.class))).thenReturn(dao);
        Mockito.when(heap.getNutDao()).thenReturn(dao);
        final NutsHeap h = new NutsHeap(this, null, dao, "heap", new NutTypeFactory(Charset.defaultCharset().displayName()), heap);
        h.checkFiles(processContext.getProcessContext());

        final ConvertibleNut convertibleNut = Mockito.mock(ConvertibleNut.class);
        Mockito.when(convertibleNut.getVersionNumber()).thenReturn(new FutureLong(1L));
        Mockito.when(convertibleNut.getNutType()).thenReturn(new NutType(EnumNutType.JAVASCRIPT, Charset.defaultCharset().displayName()));
        Mockito.when(convertibleNut.getInitialNutType()).thenReturn(new NutType(EnumNutType.JAVASCRIPT, Charset.defaultCharset().displayName()));
        Mockito.when(convertibleNut.getInitialName()).thenReturn("testcode.min.js");
        Mockito.when(convertibleNut.getName()).thenReturn("testcode.min.js");
        Mockito.when(convertibleNut.getSource()).thenReturn(new SourceImpl());

        final ConvertibleNut sourceMap = Mockito.mock(ConvertibleNut.class);
        Mockito.when(sourceMap.getVersionNumber()).thenReturn(new FutureLong(1L));
        Mockito.when(sourceMap.getNutType()).thenReturn(new NutType(EnumNutType.MAP, Charset.defaultCharset().displayName()));
        Mockito.when(sourceMap.getInitialNutType()).thenReturn(new NutType(EnumNutType.MAP, Charset.defaultCharset().displayName()));
        Mockito.when(sourceMap.getInitialName()).thenReturn("testcode.js.map");

        Mockito.when(sourceMap.openStream()).thenReturn(new InMemoryInput("{"
                + "  \"version\": 3,"
                + "  \"file\": \"testcode.js\","
                + "  \"sections\": ["
                + "    {"
                + "      \"map\": {"
                + "         \"version\": 3,"
                + "         \"mappings\": \"AAAAA,QAASA,UAAS,EAAG;\","
                + "         \"sources\": [\"testcode.js\"],"
                + "         \"names\": [\"foo\"]"
                + "      },"
                + "      \"offset\": {"
                + "        \"line\": 1,"
                + "        \"column\": 1"
                + "      }"
                + "    }"
                + "  ]"
                + "}", Charset.defaultCharset().displayName()));

        final SourceMapNut src = new SourceMapNutImpl(h, convertibleNut, sourceMap, processContext.getProcessContext());
        Assert.assertEquals(1, src.getOriginalNuts().size());
        Assert.assertNotNull(src.getNutAt(1, 1));

        Assert.assertTrue("source map must contain nut name", src.toString().contains("testcode.min.js"));
        Assert.assertTrue("source map must contain nut name", IOUtils.readString(src.openStream().reader()).contains("testcode.min.js"));
    }
}
