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

import com.github.wuic.NutType;
import com.github.wuic.NutTypeFactory;
import com.github.wuic.ProcessContext;
import com.github.wuic.context.HeapResolutionEvent;
import com.github.wuic.nut.AbstractNutDao;
import com.github.wuic.nut.HeapListener;
import com.github.wuic.nut.Nut;
import com.github.wuic.nut.NutsHeap;
import com.github.wuic.nut.dao.NutDao;
import com.github.wuic.util.FutureLong;
import com.github.wuic.util.Input;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mockito;

import java.io.IOException;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
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
            init("/", null, pollingSeconds);
            init(false, true, null);
            setNutTypeFactory(new NutTypeFactory(Charset.defaultCharset().displayName()));
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected List<String> listNutsPaths(final String pattern) throws IOException {
            return new ArrayList<String>(mockPaths.keySet());
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected Nut accessFor(final String realPath, final NutType type, final ProcessContext processContext) throws IOException {
            final Nut retval = Mockito.mock(Nut.class);
            Mockito.when(retval.getInitialName()).thenReturn(realPath);
            Mockito.when(retval.getVersionNumber()).thenReturn(new FutureLong(getLastUpdateTimestampFor(realPath)));

            return retval;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected Long getLastUpdateTimestampFor(final String path) throws IOException {
            return mockPaths.get(path);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Input newInputStream(final String path, final ProcessContext processContext) throws IOException {
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Boolean exists(final String path, final ProcessContext processContext) throws IOException {
            return null;
        }
    }

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
     * <p>
     * Tests when the first path level in an absolute nut name is a number, which is reserved for version number.
     * </p>
     *
     * @throws IOException if test fails
     */
    @Test(expected = IllegalArgumentException.class)
    public void illegalAbsoluteNutNameTest() throws IOException {
        final MockNutDao dao = new MockNutDao(-1);
        dao.mockPaths.put("/000/hey.js", 1L);
        new NutsHeap(this, Arrays.asList(".*"), dao, "", new NutTypeFactory(Charset.defaultCharset().displayName())).checkFiles(processContext.getProcessContext());
    }

    /**
     * <p>
     * Tests when the first path level in a relative nut name is a number, which is reserved for version number.
     * </p>
     *
     * @throws IOException if test fails
     */
    @Test(expected = IllegalArgumentException.class)
    public void illegalRelativeNutNameTest() throws IOException {
        final MockNutDao dao = new MockNutDao(-1);
        dao.mockPaths.put("000/hey.js", 1L);
        new NutsHeap(this, Arrays.asList(".*"), dao, "", new NutTypeFactory(Charset.defaultCharset().displayName())).checkFiles(processContext.getProcessContext());
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
        final NutsHeap heap = new NutsHeap(this, Arrays.asList(".*"), dao, "", new NutTypeFactory(Charset.defaultCharset().displayName()));
        heap.checkFiles(processContext.getProcessContext());
        final AtomicInteger count = new AtomicInteger();

        heap.addObserver(new HeapListener() {

            /**
             * {@inheritDoc}
             */
            @Override
            public void nutUpdated(final NutsHeap heap) {
                count.incrementAndGet();
            }

            @Override
            public void heapResolved(final HeapResolutionEvent event) {
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
        final NutsHeap firstCompo = new NutsHeap(this, Arrays.asList(".*"), firstDao, "", new NutTypeFactory(Charset.defaultCharset().displayName()));
        firstCompo.checkFiles(processContext.getProcessContext());
        final MockNutDao secondDao = new MockNutDao(1);
        secondDao.mockPaths.put("2.js", 1L);
        final NutsHeap secondCompo = new NutsHeap(this, Arrays.asList(".*"), secondDao, "", new NutTypeFactory(Charset.defaultCharset().displayName()));
        secondCompo.checkFiles(processContext.getProcessContext());

        final MockNutDao dao = new MockNutDao(1);
        dao.mockPaths.put("hey.js", 1L);

        final NutsHeap heap = new NutsHeap(this, Arrays.asList(".*"), dao, "", new NutTypeFactory(Charset.defaultCharset().displayName()), firstCompo, secondCompo);
        heap.checkFiles(processContext.getProcessContext());
        final AtomicInteger count = new AtomicInteger();

        heap.addObserver(new HeapListener() {

            /**
             * {@inheritDoc}
             */
            @Override
            public void nutUpdated(final NutsHeap heap) {
                count.incrementAndGet();
            }

            @Override
            public void heapResolved(final HeapResolutionEvent event) {
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
     * <p>
     * Test polling feature with multiple patterns.
     * </p>
     *
     * @throws Exception if test fails
     */
    @Test
    public void notificationWithManyPatternPath() throws Exception {
        final Map<String, List<String>> patterns = new HashMap<String, List<String>>();
        patterns.put("a", Arrays.asList("a.js", "b.js"));
        patterns.put("b", Arrays.asList("c.js", "d.js", "e.js"));
        patterns.put("c", Arrays.asList("f.js"));

        final AbstractNutDao dao = new AbstractNutDao() {

            /**
             * {@inheritDoc}
             */
            @Override
            protected List<String> listNutsPaths(final String pattern) throws IOException {
                return patterns.get(pattern);
            }

            /**
             * {@inheritDoc}
             */
            @Override
            protected Nut accessFor(final String realPath, final NutType type, final ProcessContext processContext) throws IOException {
                final Nut retval = Mockito.mock(Nut.class);
                Mockito.when(retval.getInitialName()).thenReturn(realPath);
                Mockito.when(retval.getVersionNumber()).thenReturn(new FutureLong(getLastUpdateTimestampFor(realPath)));

                return retval;
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public Input newInputStream(final String path, final ProcessContext processContext) throws IOException {
                return null;
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public Boolean exists(final String path, final ProcessContext processContext) throws IOException {
                return null;
            }

            /**
             * {@inheritDoc}
             */
            @Override
            protected Long getLastUpdateTimestampFor(final String path) throws IOException {
                return 1L;
            }
        };

        dao.init("/", null, 1);
        dao.init(false, true, null);
        dao.setNutTypeFactory(new NutTypeFactory(Charset.defaultCharset().displayName()));

        try {
            final NutsHeap firstCompo = new NutsHeap(this, Arrays.asList("a", "b"), dao, "", new NutTypeFactory(Charset.defaultCharset().displayName()));
            firstCompo.checkFiles(processContext.getProcessContext());
            firstCompo.create(firstCompo.getNuts().get(0), "c", NutDao.PathFormat.ANY, null);
            final CountDownLatch latch1 = new CountDownLatch(1);
            final CountDownLatch latch2 = new CountDownLatch(1);
            patterns.put("b", Arrays.asList("c.js", "d.js"));

            firstCompo.addObserver(new HeapListener() {

                /**
                 * {@inheritDoc}
                 */
                @Override
                public void nutUpdated(final NutsHeap heap) {
                    if (latch1.getCount() == 0) {
                        latch2.countDown();
                    } else {
                        latch1.countDown();
                    }
                }

                @Override
                public void heapResolved(final HeapResolutionEvent event) {
                }
            });

            Assert.assertTrue(latch1.await(3, TimeUnit.SECONDS));
            Assert.assertFalse(latch2.await(3, TimeUnit.SECONDS));
        } finally {
            dao.shutdown();
        }
    }
    
    /**
     * Test when different extensions are defined.
     *
     * @throws IOException if test fails
     */
    @Test
    public void badExtensionTest() throws IOException {
        final MockNutDao dao = new MockNutDao(-1);
        dao.mockPaths.put("hey.js", 1L);
        dao.mockPaths.put("hey.css", 1L);
        new NutsHeap(this, Arrays.asList(""), dao, "", new NutTypeFactory(Charset.defaultCharset().displayName())).checkFiles(processContext.getProcessContext());
    }

    /**
     * Test when no paths are defined.
     *
     * @throws IOException if test fails
     */
    @Test(expected = IllegalArgumentException.class)
    public void noPathTest() throws IOException {
        new NutsHeap(this, Arrays.asList(""), new MockNutDao(-1), "", new NutTypeFactory(Charset.defaultCharset().displayName())).checkFiles(processContext.getProcessContext());
        Assert.fail();
    }

    /**
     * <p>
     * Test observer removal when heap is no more referenced.
     * </p>
     *
     * @throws Exception is test fails
     */
    @Test
    public void removesListenerTest() throws Exception {
        final MockNutDao dao = new MockNutDao(-1);
        dao.mockPaths.put("1.js", 1L);
        NutsHeap heap = new NutsHeap(this, Arrays.asList(".*"), dao, "", new NutTypeFactory(Charset.defaultCharset().displayName()));
        heap.checkFiles(processContext.getProcessContext());
        final ReferenceQueue queue = new ReferenceQueue();
        final WeakReference ref = new WeakReference(heap, queue);

        // set hard reference to null: heap will be garbage collected
        heap = null;
        System.gc();

        // Do some activity in order to trigger garbage collection
        for (int i = 0; i < 10000000; i++) {
        }

        Assert.assertTrue(ref.isEnqueued());
    }
}
