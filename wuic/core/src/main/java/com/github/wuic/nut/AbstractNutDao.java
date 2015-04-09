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


package com.github.wuic.nut;

import com.github.wuic.Logging;
import com.github.wuic.NutType;
import com.github.wuic.ProcessContext;
import com.github.wuic.exception.WuicException;
import com.github.wuic.nut.dao.NutDao;
import com.github.wuic.nut.dao.NutDaoListener;
import com.github.wuic.util.FutureLong;
import com.github.wuic.util.IOUtils;
import com.github.wuic.util.NumberUtils;
import com.github.wuic.util.PollingScheduler;
import com.github.wuic.util.StringUtils;
import com.github.wuic.util.WuicScheduledThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * <p>
 * An abstract implementation of a {@link NutDao}. As any implementation should provides it, this class defines a base
 * path when retrieved nuts, a set of proxies URIs and a polling feature.
 * </p>
 * <p/>
 * <p>
 * The class is designed to be thread safe.
 * </p>
 *
 * @author Guillaume DROUET
 * @version 1.4
 * @since 0.3.1
 */
public abstract class AbstractNutDao extends PollingScheduler<NutDaoListener> implements NutDao {

    /**
     * The logger.
     */
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    /**
     * Base path which prefix any given path to create a nut.
     */
    private String basePath;

    /**
     * The proxy URIs.
     */
    private String[] proxyUris;

    /**
     * Index of the next proxy URI to use.
     */
    private final AtomicInteger nextProxyIndex;

    /**
     * The version number management strategy.
     */
    private final VersionNumberStrategy versionNumberStrategy;

    /**
     * <p>
     * Builds a new instance.
     * </p>
     *
     * @param base              prefix for all paths (an {@link IllegalArgumentException} is thrown is {@code null}
     * @param basePathAsSysProp {@code true} if the base path is a system property
     * @param proxies           proxy URIs serving the nut
     * @param pollingSeconds    interval in seconds for polling feature (-1 to disable)
     * @param vns               fixed version number
     */
    public AbstractNutDao(final String base,
                          final Boolean basePathAsSysProp,
                          final String[] proxies,
                          final int pollingSeconds,
                          final VersionNumberStrategy vns) {
        final String b = basePathAsSysProp ? System.getProperty(base) : base;

        if (b == null) {
            WuicException.throwBadArgumentException(new IllegalArgumentException("Base path can't be null"));
        } else {
            basePath = !b.isEmpty() && b.charAt(0) == '.' ? b : IOUtils.mergePath("/", b);
        }

        versionNumberStrategy = vns;
        proxyUris = proxies == null ? null : Arrays.copyOf(proxies, proxies.length);
        nextProxyIndex = new AtomicInteger(0);
        setPollingInterval(pollingSeconds);
        Disposer.INSTANCE.register(this);
    }

    /**
     * {@inheritDoc}
     */
    public void run() {
        // Log duration
        final Long start = System.currentTimeMillis();
        Logging.POLL.log("Running polling operation for {}", toString());

        // Keep in this set all listeners that told that they don't want to be notified until the end of the operation
        final Set<NutDaoListener> exclusions = new HashSet<NutDaoListener>();

        // Keep all paths already retrieved associated to their pattern
        final Map<String, List<String>> nutsPathByPattern = new HashMap<String, List<String>>();

        // We keep all timestamps already retrieved for each path
        final Map<String, Long> timestamps = new HashMap<String, Long>();

        synchronized (getNutObservers()) {

            // Poll each nut's path
            for (final Map.Entry<NutDaoListener, ? extends Polling> entry : getNutObservers().entrySet()) {
                final NutDaoListener listener = entry.getKey();
                final Polling pollingData = entry.getValue();

                for (final String pattern : pollingData.getPatterns()) {
                    List<String> nutPaths = nutsPathByPattern.get(pattern);

                    try {
                        if (nutPaths == null) {
                            nutPaths = listNutsPaths(pattern);
                            nutsPathByPattern.put(pattern, nutPaths);
                        }
                    } catch (IOException se) {
                        log.error("Unable to list path for {}", pattern, se);
                        continue;
                    }

                    evaluatePolling(timestamps, nutPaths, exclusions, listener, pattern);
                }
            }
        }

        Disposer.INSTANCE.dispose(exclusions);

        Logging.POLL.log("Polling operation for {} run in {} seconds", getClass().getName(),
                (float) (System.currentTimeMillis() - start) / (float) NumberUtils.ONE_THOUSAND);
    }

    /**
     * <p>
     * Evaluates each path by polling the associated resource and notifying any detected change.
     * </p>
     *
     * @param timestamps the timestamps of polled resources
     * @param nutPaths the paths of resources to poll
     * @param exclusions the listeners asking for exclusion
     * @param listener the listener which will evaluates the poll for this call
     * @param pattern the pattern associated to all given paths
     */
    private void evaluatePolling(final Map<String, Long> timestamps,
                                 final List<String> nutPaths,
                                 final Set<NutDaoListener> exclusions,
                                 final NutDaoListener listener,
                                 final String pattern) {
        // Notify listener
        final boolean excluded = exclusions.contains(listener);

        // Not already excluded and asks for exclusion
        if (!excluded && !listener.polling(pattern, new HashSet<String>(nutPaths))) {
            exclusions.add(listener);
        } else if (!excluded) {
            for (final String path : nutPaths) {
                Long timestamp = timestamps.get(path);

                // Timestamps not already retrieved
                if (timestamp == null) {
                    try {
                        timestamp = getVersionNumber(path, null).get();
                        timestamps.put(path, timestamp);
                    } catch (IOException se) {
                        log.error("Unable to poll nut {}", path, se);
                    } catch (InterruptedException ie) {
                        log.error("Thread retrieving the version number for {} has been interrupted", path, ie);
                    } catch (ExecutionException ee) {
                        log.error("Thread retrieving the version number for {} has raised an exception", path, ee);
                    }
                }

                // Stop notifying
                if (!listener.nutPolled(this, path, timestamp)) {
                    exclusions.add(listener);
                    break;
                }
            }
        }
    }

    /**
     * <p>
     * Gets the version number for the {@link Nut} the given path.
     * </p>
     * <p/>
     * <p>
     * If the {@link AbstractNutDao#versionNumberStrategy#contentBasedVersionNumber} value related to
     * {@link com.github.wuic.ApplicationConfig#CONTENT_BASED_VERSION_NUMBER} is {@code true}, then the content is read
     * to compute the hash value. However, it uses the last modification timestamp.
     * </p>
     *
     * @param processContext the process context
     * @param path the nut's path
     * @return the version number
     * @throws IOException if version number could not be computed
     */
    protected Future<Long> getVersionNumber(final String path, final ProcessContext processContext) throws IOException {
        if (versionNumberStrategy.getFixedVersionNumber() != null) {
            return new FutureLong(versionNumberStrategy.getFixedVersionNumber());
        } else if (versionNumberStrategy.getComputeVersionAsynchronously()) {
            log.debug("Computing version number asynchronously");
            return WuicScheduledThreadPool.getInstance().executeAsap(new VersionNumberCallable(path, processContext));
        } else {
            log.debug("Computing version number synchronously");
            return new FutureLong(new VersionNumberCallable(path, processContext).call());
        }
    }

    /**
     * <p>
     * Computes the absolute path of the given path relative to the DAO's base path.
     * </p>
     *
     * @param relativePath the relative path
     * @return the absolute path
     */
    protected String absolutePathOf(final String relativePath) {
        return StringUtils.simplifyPathWithDoubleDot(IOUtils.mergePath(getBasePath(), relativePath));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Nut> create(final String path, final ProcessContext processContext) throws IOException {
        return AbstractNutDao.this.create(path, PathFormat.ANY, processContext);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Nut> create(final String pathName, final PathFormat format, final ProcessContext processContext) throws IOException {
        final List<String> pathNames = computeRealPaths(pathName, format, processContext);
        final List<Nut> retval = new ArrayList<Nut>(pathNames.size());

        for (final String p : pathNames) {
            final NutType type = NutType.getNutType(p);

            if (type == null) {
                continue;
            }

            final Nut res = accessFor(p, type, processContext);
            res.setProxyUri(proxyUriFor(res));

            retval.add(res);
        }

        return retval;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String proxyUriFor(final Nut nut) {
        if (proxyUris != null && proxyUris.length > 0) {

            synchronized (nextProxyIndex) {
                // End of round, restart
                if (nextProxyIndex.intValue() >= proxyUris.length) {
                    nextProxyIndex.set(0);
                }

                // Do the round-robin by incrementing nextProxyIndex counter
                return IOUtils.mergePath(String.valueOf(proxyUris[nextProxyIndex.getAndIncrement()]), nut.getInitialName());
            }
        } else {
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void save(final Nut nut) {
        WuicException.throwSaveUnsupportedMethodException(getClass());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Boolean saveSupported() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void shutdown() {
        log.info("Shutdown DAO {}", getClass().getName());

        // Will stop any scheduled operation
        if (getPollingInterval() != -1) {
            setPollingInterval(-1);
        }

        Disposer.INSTANCE.remove(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NutDao withRootPath(final String rootPath) {
        return new WithRootPathNutDao(rootPath);
    }

    /**
     * <p>
     * This class wraps all information required by the {@link AbstractNutDao} regarding version number management.
     * </p>
     *
     * @author Guillaume DROUET
     * @version 1.0
     * @since 0.4.1
     */
    public static final class VersionNumberStrategy {

        /**
         * Computes the version number from content or on timestamp.
         */
        private final Boolean contentBasedVersionNumber;

        /**
         * For version number computation.
         */
        private final Boolean computeVersionAsynchronously;

        /**
         * A fixed version number. If {@code null}, then the last modification timestamp or the hashed content is used
         * regarding {@link #contentBasedVersionNumber} attribute.
         */
        private final Long fixedVersionNumber;

        /**
         * <p>
         * Builds a new instance.
         * </p>
         *
         * @param contentBasedVersionNumber content based version number or not
         * @param computeVersionAsynchronously asynchronous computation or not
         * @param fixedVersionNumber fixed version number ({@code null} if version number is not fixed)
         */
        public VersionNumberStrategy(final Boolean contentBasedVersionNumber,
                                     final Boolean computeVersionAsynchronously,
                                     final String fixedVersionNumber) {
            if (fixedVersionNumber == null || "".equals(fixedVersionNumber)) {
                this.fixedVersionNumber = null;
            } else if (!NumberUtils.isNumber(fixedVersionNumber)) {
                this.fixedVersionNumber = null;
                WuicException.throwBadArgumentException(new IllegalArgumentException(
                        String.format("Fixed version must be a number: %s", fixedVersionNumber)));
            } else {
                this.fixedVersionNumber = Long.parseLong(fixedVersionNumber);
            }

            this.contentBasedVersionNumber = contentBasedVersionNumber;
            this.computeVersionAsynchronously = computeVersionAsynchronously;
        }

        /**
         * <p>
         * Indicates if the version number is based on the content.
         * </p>
         *
         * @return {@code true} if version number is content based, {@code false} otherwise
         */
        public Boolean getContentBasedVersionNumber() {
            return contentBasedVersionNumber;
        }

        /**
         * <p>
         * Indicates if the version number is computed asynchronously.
         * </p>
         *
         * @return {@code true} if version number is computed asynchronously, {@code false} otherwise
         */
        public Boolean getComputeVersionAsynchronously() {
            return computeVersionAsynchronously;
        }

        /**
         * <p>
         * Gets the fixed version number.
         * </p>
         *
         * @return the version number, {@code null} if the version number is not fixed
         */
        public Long getFixedVersionNumber() {
            return fixedVersionNumber;
        }
    }

    /**
     * <p>
     * This class represents a modification of the the enclosing class behavior when the {@link NutDao#create(String, ProcessContext)}
     * method is called. Each time this method is called, the given path is prefixed by a root path.
     * </p>
     *
     * @author Guillaume DROUET
     * @version 1.0
     * @since 0.4.1
     */
    public final class WithRootPathNutDao implements NutDao {

        /**
         * Root path.
         */
        private String rootPath;

        /**
         * <p>
         * Creates a new instance.
         * </p>
         *
         * @param rp the root path.
         */
        private WithRootPathNutDao(final String rp) {
            rootPath = rp;
        }

        /**
         * <p>
         * Gets the root path.
         * </p>
         *
         * @return the root path.
         */
        public String getRootPath() {
            return rootPath;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void observe(final String realPath, final NutDaoListener... listeners) throws IOException {
            AbstractNutDao.this.observe(realPath, listeners);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public List<Nut> create(final String path, final ProcessContext processContext) throws IOException {
            return AbstractNutDao.this.create(IOUtils.mergePath(rootPath, path), processContext);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public List<Nut> create(final String path, final PathFormat format, final ProcessContext processContext) throws IOException {
            return AbstractNutDao.this.create(IOUtils.mergePath(rootPath, path), format, processContext);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String proxyUriFor(final Nut nut) {
            return AbstractNutDao.this.proxyUriFor(nut);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void save(final Nut nut) {
            AbstractNutDao.this.save(nut);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Boolean saveSupported() {
            return AbstractNutDao.this.saveSupported();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void shutdown() {
            AbstractNutDao.this.shutdown();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public NutDao withRootPath(final String rp) {
            return new WithRootPathNutDao(IOUtils.mergePath(rootPath, rp));
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public InputStream newInputStream(final String path, final ProcessContext processContext) throws IOException {
            return AbstractNutDao.this.newInputStream(path, processContext);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Boolean exists(final String path, final ProcessContext processContext) throws IOException {
            return AbstractNutDao.this.exists(path, processContext);
        }
    }

    /**
     * <p>
     * Returns a list of paths depending of the behavior of the dao with the given path.
     * In fact, the dao could consider the path as a regex, a directory, etc.
     * </p>
     *
     * @param pathName the path access
     * @param format   the path format
     * @param processContext the process context
     * @return the resulting real paths
     * @throws IOException if an I/O error occurs when creating the nut
     */
    public List<String> computeRealPaths(final String pathName, final PathFormat format, final ProcessContext processContext)
            throws IOException {
        if (!format.canBeRegex()) {
            try {
                if (exists(pathName, processContext)) {
                    // Nut can be raised, return its path
                    return Arrays.asList(pathName);
                }
            } catch (IOException e) {
                log.warn("'{}' can't be loaded ignoring it. Absolute path is '{}'", pathName, absolutePathOf(pathName), e);
            }

            // Nut can't be raised
            return Collections.emptyList();
        } else {
            final List<String> paths = listNutsPaths(pathName);
            final List<String> retval = new ArrayList<String>(paths.size());
            retval.addAll(paths);

            return retval;
        }
    }

    /**
     * <p>
     * Returns the base path prefixing all paths.
     * </p>
     *
     * @return the base path
     */
    public String getBasePath() {
        return basePath;
    }

    /**
     * <p>
     * Gets the proxy URIs.
     * </p>
     *
     * @return the proxies
     */
    public String[] getProxyUris() {
        return proxyUris;
    }

    /**
     * <p>
     * Gets the version management strategy.
     * </p>
     *
     * @return the strategy
     */
    public VersionNumberStrategy getVersionNumberStrategy() {
        return versionNumberStrategy;
    }

    /**
     * <p>
     * Lists all the nuts path matching the given pattern.
     * </p>
     *
     * @param pattern the pattern
     * @throws IOException if any I/O error occurs while reading nuts
     */
    protected abstract List<String> listNutsPaths(String pattern) throws IOException;

    /**
     * <p>
     * Creates an access for the given parameters through a {@link Nut} implementation.
     * </p>
     *
     * @param realPath the real path to use to access the nut
     * @param type     the path's type
     * @param processContext the process context
     * @return the {@link Nut}
     * @throws IOException if an I/O error occurs while creating access
     */
    protected abstract Nut accessFor(String realPath, NutType type, ProcessContext processContext) throws IOException;

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return String.format("%s with base path %s", getClass().getName(), getBasePath());
    }

    /**
     * <p>
     * This {@link Callable} is used to compute asynchronously the version number according to the value set to
     * {@link #versionNumberStrategy#contentBasedVersionNumber} member.
     * </p>
     *
     * @author Guillaume DROUET
     * @version 1.0
     * @since 0.5.0
     */
    private final class VersionNumberCallable implements Callable<Long> {

        /**
         * The path corresponding to the resource.
         */
        private final String path;

        /**
         * The process context.
         */
        private final ProcessContext processContext;

        /**
         * Builds a new instance.
         *
         * @param p the path
         * @param pc the process context
         */
        private VersionNumberCallable(final String p, final ProcessContext pc) {
            path = p;
            processContext = pc;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Long call() throws IOException {
            log.debug("Computing asynchronously version number for path '{}'. Content based: {}", path, versionNumberStrategy.getContentBasedVersionNumber());

            if (versionNumberStrategy.getContentBasedVersionNumber()) {
                InputStream is = null;

                try {
                    is = newInputStream(path, processContext);
                    final MessageDigest md = IOUtils.newMessageDigest();
                    final byte[] buffer = new byte[IOUtils.WUIC_BUFFER_LEN];
                    int offset;

                    while ((offset = is.read(buffer)) != -1) {
                        md.update(buffer, 0, offset);
                    }

                    return ByteBuffer.wrap(md.digest()).getLong();
                } finally {
                    IOUtils.close(is);
                }
            } else {
                return getLastUpdateTimestampFor(path);
            }
        }
    }

    /**
     * <p>
     * This singleton holds all new {@link AbstractNutDao} instances and helps to clean {@link #getNutObservers() observers}
     * when a {@link com.github.wuic.nut.dao.NutDaoListener#isDisposable() disposible} object is removed in one of them.
     * </p>
     *
     * @author Guillaume DROUET
     * @version 1.0
     * @since 0.5.1
     */
    private enum Disposer {

        /**
         * The singleton.
         */
        INSTANCE;

        /**
         * The registration list.
         */
        private final List<AbstractNutDao> registeredDaoList;

        /**
         * <p>
         * Builds a new instance.
         * </p>
         */
        private Disposer() {
            registeredDaoList = Collections.synchronizedList(new ArrayList<AbstractNutDao>());
        }

        /**
         * <p>
         * Adds a new DAO.
         * </p>
         *
         * @param dao the DAO
         */
        public void register(final AbstractNutDao dao) {
            registeredDaoList.add(dao);
        }

        /**
         * <p>
         * Removes the DAO.
         * </p>
         *
         * @param dao the DAO
         */
        public void remove(final AbstractNutDao dao) {
            registeredDaoList.remove(dao);
        }

        /**
         * <p>
         * Dispose a set of candidates as specified in {@link com.github.wuic.nut.dao.NutDaoListener#isDisposable()}.
         * </p>
         *
         * @param exclusions the candidate list
         */
        public void dispose(final Set<NutDaoListener> exclusions) {
            // Dispose listeners
            for (final NutDaoListener listener : exclusions) {
                if (listener.isDisposable()) {
                    // Check each registered DAO
                    for (final AbstractNutDao registration : registeredDaoList) {
                        synchronized (registration.getNutObservers()) {
                            final Set<NutDaoListener> set = new HashSet<NutDaoListener>(registration.getNutObservers().keySet());
                            final Object excludedFactory = listener.getFactory();

                            // Looking for all listeners created by the same factory and also disposable
                            for (final NutDaoListener dao : set) {
                                final Object daoFactory = dao.getFactory();

                                if (excludedFactory != null && daoFactory != null && excludedFactory.equals(daoFactory) && dao.isDisposable()) {
                                    registration.getNutObservers().remove(dao);
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
