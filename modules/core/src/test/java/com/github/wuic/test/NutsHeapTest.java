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


package com.github.wuic.test;

import com.github.wuic.NutType;
import com.github.wuic.exception.wrapper.BadArgumentException;
import com.github.wuic.exception.wrapper.StreamException;
import com.github.wuic.nut.AbstractNutDao;
import com.github.wuic.nut.HeapListener;
import com.github.wuic.nut.Nut;
import com.github.wuic.nut.NutsHeap;
import com.github.wuic.util.FutureLong;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mockito;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * <p>
 * An abstract implementation of a {@link com.github.wuic.nut.dao.NutDao}. As any implementation should provides it, this class defines a base
 * path when retrieved nuts, a set of proxies URIs and a polling feature.
 * </p>
 *
 * <p>
 * The class is designed to be thread safe.
 * </p>
 *
 * @author Guillaume DROUET
 * @version 1.2
 * @since 0.4.0
 */
@RunWith(JUnit4.class)
public class NutsHeapTest {

    /**
     * <p>
     * Mocked DAO.
     * </p>
     *
     * @author Guillaume DROUET
     * @version 1.0
     * @since 0.4.0
     */
    final class MockNutDao extends AbstractNutDao {

        /**
         * Path returned by mock DAO.
         */
        private Map<String, Long> mockPaths = new HashMap<String, Long>();

        /**
         * <p>
         * Builds an instance.
         * </p>
         *
         * @param pollingSeconds polling interval
         */
        MockNutDao(final int pollingSeconds) {
            super("/", false, null, pollingSeconds, false);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected List<String> listNutsPaths(final String pattern) throws StreamException {
            return new ArrayList<String>(mockPaths.keySet());
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected Nut accessFor(final String realPath, final NutType type) throws StreamException {
            final Nut retval = Mockito.mock(Nut.class);
            Mockito.when(retval.getName()).thenReturn(realPath);
            Mockito.when(retval.getVersionNumber()).thenReturn(new FutureLong(getLastUpdateTimestampFor(realPath)));

            return retval;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected Long getLastUpdateTimestampFor(final String path) throws StreamException {
            return mockPaths.get(path);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public InputStream newInputStream(final String path) throws StreamException {
            return null;
        }
    }

    /**
     * <p>
     * Test notification when changes are detected.
     * </p>
     *
     * @throws Exception is test fails
     */
    @Test
    public void notificationTest() throws Exception {
        final MockNutDao dao = new MockNutDao(1);
        dao.mockPaths.put("hey.js", 1L);
        final NutsHeap heap = new NutsHeap(Arrays.asList(".*"), dao, "");
        final AtomicInteger count = new AtomicInteger();

        heap.addObserver(new HeapListener() {

            /**
             * {@inheritDoc}
             */
            @Override
            public void nutUpdated(final NutsHeap heap) {
                count.incrementAndGet();
            }
        });

        Thread.sleep(1500L);
        Assert.assertEquals(count.intValue(), 0);

        dao.mockPaths.put("foo.js", 10L);
        Thread.sleep(3000L);
        Assert.assertEquals(count.intValue(), 1);

        Thread.sleep(1000L);
        Assert.assertEquals(count.intValue(), 1);

        dao.mockPaths.put("foo.js", 20L);
        Thread.sleep(1000L);
        Assert.assertEquals(count.intValue(), 2);
    }

    /**
     * <p>
     * Test notification when changes are detected in heap composition.
     * </p>
     *
     * @throws Exception is test fails
     */
    @Test
    public void notificationByCompositionTest() throws Exception {
        final MockNutDao firstDao = new MockNutDao(1);
        firstDao.mockPaths.put("1.js", 1L);
        final NutsHeap firstCompo = new NutsHeap(Arrays.asList(".*"), firstDao, "");
        final MockNutDao secondDao = new MockNutDao(1);
        secondDao.mockPaths.put("2.js", 1L);
        final NutsHeap secondCompo = new NutsHeap(Arrays.asList(".*"), secondDao, "");

        final MockNutDao dao = new MockNutDao(1);
        dao.mockPaths.put("hey.js", 1L);

        final NutsHeap heap = new NutsHeap(Arrays.asList(".*"), dao, "", firstCompo, secondCompo);
        final AtomicInteger count = new AtomicInteger();

        heap.addObserver(new HeapListener() {

            /**
             * {@inheritDoc}
             */
            @Override
            public void nutUpdated(final NutsHeap heap) {
                count.incrementAndGet();
            }
        });

        Thread.sleep(1500L);
        Assert.assertEquals(count.intValue(), 0);

        firstDao.mockPaths.put("foo.js", 10L);
        Thread.sleep(3000L);
        Assert.assertEquals(count.intValue(), 1);

        Thread.sleep(1000L);
        Assert.assertEquals(count.intValue(), 1);

        firstDao.mockPaths.put("foo.js", 20L);
        Thread.sleep(1000L);
        Assert.assertEquals(count.intValue(), 2);

        secondDao.mockPaths.put("foo.js", 30L);
        Thread.sleep(1000L);
        Assert.assertEquals(count.intValue(), 3);
    }

    /**
     * Test when different extensions are defined.
     *
     * @throws StreamException if test fails
     */
    @Test
    public void badExtensionTest() throws StreamException {
        final MockNutDao dao = new MockNutDao(-1);
        dao.mockPaths.put("hey.js", 1L);
        dao.mockPaths.put("hey.css", 1L);
        new NutsHeap(Arrays.asList(""), dao, "");
    }

    /**
     * Test when no paths are defined.
     *
     * @throws StreamException if test fails
     */
    @Test
    public void noPathTest() throws StreamException {
        try {
            new NutsHeap(Arrays.asList(""), new MockNutDao(-1), "");
            Assert.fail();
        } catch (BadArgumentException be) {
            // normal behavior
        }
    }
}
