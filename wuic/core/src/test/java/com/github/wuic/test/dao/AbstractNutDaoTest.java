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


package com.github.wuic.test.dao;

import com.github.wuic.NutType;
import com.github.wuic.nut.AbstractNutDao;
import com.github.wuic.nut.ConvertibleNut;
import com.github.wuic.nut.Nut;
import com.github.wuic.nut.dao.NutDao;
import com.github.wuic.nut.dao.NutDaoListener;
import com.github.wuic.util.FutureLong;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * <p>
 * Test for features provided by {@link com.github.wuic.nut.AbstractNutDao}.
 * </p>
 *
 * @author Guillaume DROUET
 * @version 1.2
 * @since 0.4.0
 */
@RunWith(JUnit4.class)
public class AbstractNutDaoTest {

    /**
     * Logger.
     */
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    /**
     * A byte array.
     */
    private static final byte[] BYTES = new byte[4000];

    /**
     * Age for mock DAO.
     */
    private static final Long BEGIN_AGE = System.currentTimeMillis();

    static {
        new Random().nextBytes(BYTES);
    }

    /**
     * <p>
     * A mocked DAO for tests.
     * </p>
     *
     * @author Guillaume DROUET
     * @version 1.0
     * @since 0.4.0
     */
    private class MockNutDaoTest extends AbstractNutDao {

        /**
         * Age of last nut.
         */
        private Long age;

        /**
         * Change timestamps after x ms.
         */
        private Long updateAfterMs;

        /**
         * <p>
         * Creates a new instance.
         * </p>
         *
         * @param pollingSeconds polling interval
         * @param updateAfter change timestamps after x ms
         * @param contentBasedVersionNumber use version number computed from content
         */
        private MockNutDaoTest(final int pollingSeconds, final Long updateAfter, final boolean contentBasedVersionNumber) {
            super("/", false, new String[] { "1", "2", "3", "4", }, pollingSeconds, contentBasedVersionNumber, true);
            age = BEGIN_AGE;
            updateAfterMs = updateAfter;
        }

        /**
         * <p>
         * Creates a new instance.
         * </p>
         *
         * @param pollingSeconds polling interval
         */
        private MockNutDaoTest(final int pollingSeconds) {
            this(pollingSeconds, 1500L, false);
        }

        /**
         * <p>
         * Creates a new instance.
         * </p>
         *
         * @param contentBasedVersionNumber use version number computed from content
         */
        private MockNutDaoTest(final Boolean contentBasedVersionNumber) {
            this(-1, null, contentBasedVersionNumber);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected List<String> listNutsPaths(final String pattern) throws IOException {
            return Arrays.asList("foo.js");
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected Nut accessFor(final String realPath, final NutType type) throws IOException {
            final ConvertibleNut mock = mock(ConvertibleNut.class);
            when(mock.getName()).thenReturn(realPath);
            when(mock.getInitialName()).thenReturn(realPath);

            try {
                when(mock.getVersionNumber()).thenReturn(new FutureLong(getVersionNumber(realPath).get()));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            return mock;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected Long getLastUpdateTimestampFor(final String path) throws IOException {
            if (updateAfterMs != null && age + updateAfterMs < System.currentTimeMillis()) {
                age = System.currentTimeMillis();
            }

            logger.info("getLastUpdateTimestampFor(java.lang.String) returns {}", age);

            return age;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public InputStream newInputStream(final String path) throws IOException {
            return new ByteArrayInputStream(BYTES);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Boolean exists(final String path) throws IOException {
            return true;
        }
    }

    /**
     * Test version number computation.
     *
     * @throws Exception e
     */
    @Test
    public void versionNumberTest() throws Exception  {
        final NutDao first = new MockNutDaoTest(true);
        final NutDao second = new MockNutDaoTest(true);
        final NutDao third = new MockNutDaoTest(false);
        final NutDao fourth = new MockNutDaoTest(false);

        Assert.assertEquals(first.create("").get(0).getVersionNumber().get(), second.create("").get(0).getVersionNumber().get());
        Assert.assertNotEquals(second.create("").get(0).getVersionNumber().get(), third.create("").get(0).getVersionNumber().get());
        Assert.assertEquals(third.create("").get(0).getVersionNumber().get(), fourth.create("").get(0).getVersionNumber().get());
    }

    /**
     * Test for poll scheduling.
     *
     * @throws Exception if test fails
     */
    @Test
    public void pollSchedulingTest() throws Exception {
        final AtomicInteger count = new AtomicInteger(0);
        final NutDao dao = new MockNutDaoTest(1);
        dao.create("");

        try {
            final long start = System.currentTimeMillis();
            for (int i = 0; i < 100000; i++)
            dao.observe("" + 1, new NutDaoListener() {
                @Override
                public boolean polling(final String p, final Set<String> paths) {
                    return true;
                }

                @Override
                public boolean nutPolled(final NutDao dao, final String path, final Long timestamp) {
                    logger.info("Nut updated");
                    synchronized (AbstractNutDaoTest.class) {
                        if (count.incrementAndGet() > 1) {
                            AbstractNutDaoTest.class.notify();
                        }
                    }

                    return true;
                }
            });

            System.out.println((System.currentTimeMillis() - start) + "ms");

            synchronized (AbstractNutDaoTest.class) {
                AbstractNutDaoTest.class.wait(5000L);
            }

            final long duration = System.currentTimeMillis() - start;
            logger.info("Polling took {} ms", duration);

            Assert.assertTrue(duration < 4500L);
        } finally {
            dao.shutdown();
        }
    }

    /**
     * <p>
     * Test when stop polling.
     * </p>
     *
     * @throws Exception if test fails
     */
    @Test
    public void stopPollingTest() throws Exception {
        final AtomicInteger count = new AtomicInteger(0);
        final MockNutDaoTest dao = new MockNutDaoTest(1);

        try {
            dao.create("");
            dao.observe("", new NutDaoListener() {
                @Override
                public boolean polling(final String p, final Set<String> paths) {
                    return true;
                }

                @Override
                public boolean nutPolled(NutDao dao, String path, Long timestamp) {
                    logger.info("Nut updated");
                    synchronized (AbstractNutDaoTest.class) {
                        count.incrementAndGet();
                    }

                    return true;
                }
            });

            Thread.sleep(1500L);
            dao.setPollingInterval(-1);
            Thread.sleep(2500L);
            Assert.assertEquals(count.intValue(), 1);
        } finally {
            dao.shutdown();
        }
    }

    /**
     * <p>
     * Tests that a listener is not called when it asks for exclusion.
     * </p>
     *
     * @throws Exception
     */
    @Test
    public void excludePollingListenerTest() throws Exception {
        final AtomicInteger count = new AtomicInteger(0);
        final MockNutDaoTest dao = new MockNutDaoTest(1);

        try {
            dao.create("1");
            dao.create("2");

            final NutDaoListener listener = new NutDaoListener() {
                @Override
                public boolean polling(final String p, final Set<String> paths) {
                    logger.info("Polling nut");
                    synchronized (AbstractNutDaoTest.class) {
                        count.incrementAndGet();
                    }

                    // Ask for exclusion by returning false
                    return false;
                }

                @Override
                public boolean nutPolled(final NutDao dao, final String path, final Long timestamp) {
                    return true;
                }
            };

            dao.observe("1", listener);
            dao.observe("2", listener);

            Thread.sleep(2500L);
            Assert.assertEquals(count.intValue(), 2);
        } finally {
            dao.shutdown();
        }
    }

    /**
     * Concurrent test.
     *
     * @throws Exception if test fails
     */
    @Test
    public void concurrentTest() throws Exception {
        final AbstractNutDao a = new MockNutDaoTest(2);
        final AbstractNutDao b = new MockNutDaoTest(2);

        try {
            final ConvertibleNut nut = mock(ConvertibleNut.class);
            when(nut.getName()).thenReturn("mock");
            when(nut.getInitialName()).thenReturn("mock");
            when(nut.getVersionNumber()).thenReturn(new FutureLong(1L));
            final AtomicReference<Exception> ex = new AtomicReference<Exception>();
            final CountDownLatch latch = new CountDownLatch(750);

            for (int i = 0; i < 750; i++) {
                new Thread(new Runnable() {

                    /**
                     * {@inheritDoc}
                     */
                    @Override
                    public void run() {
                        try {
                            a.create("a");
                            a.observe("a", new NutDaoListener() {
                                @Override
                                public boolean polling(final String p, final Set<String> paths) {
                                    return false;
                                }

                                @Override
                                public boolean nutPolled(NutDao dao, String path, Long timestamp) {
                                    return false;
                                }
                            });
                            b.create("b");
                            b.observe("b", new NutDaoListener() {
                                @Override
                                public boolean polling(final String p, final Set<String> paths) {
                                    return false;
                                }

                                @Override
                                public boolean nutPolled(NutDao dao, String path, Long timestamp) {
                                    return false;
                                }
                            });

                            a.setPollingInterval(1);
                            a.computeRealPaths("", NutDao.PathFormat.ANY);
                            a.proxyUriFor(nut);
                            a.run();

                            b.setPollingInterval(1);
                            b.computeRealPaths("", NutDao.PathFormat.ANY);
                            b.proxyUriFor(nut);
                            b.run();

                        } catch (Exception e) {
                            ex.set(e);
                        } finally {
                            latch.countDown();
                        }
                    }
                }).start();
            }

            latch.await(5, TimeUnit.SECONDS);

            if (ex.get() != null) {
                ex.get().printStackTrace();
                Assert.fail(ex.get().getMessage());
            }
        } finally {
            a.shutdown();
            b.shutdown();
        }
    }
}
