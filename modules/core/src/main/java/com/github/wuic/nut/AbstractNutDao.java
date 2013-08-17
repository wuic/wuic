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


package com.github.wuic.nut;

import com.github.wuic.NutType;
import com.github.wuic.exception.SaveOperationNotSupportedException;
import com.github.wuic.exception.wrapper.BadArgumentException;
import com.github.wuic.exception.wrapper.StreamException;
import com.github.wuic.util.IOUtils;
import com.github.wuic.util.WuicScheduledThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.HashMap;
import java.util.List;
import java.util.Arrays;
import java.util.Collections;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashSet;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * <p>
 * An abstract implementation of a {@link NutDao}. As any implementation should provides it, this class defines a base
 * path when retrieved resources, a set of proxies URIs and a polling feature.
 * </p>
 *
 * <p>
 * The class is designed to be thread safe.
 * </p>
 *
 * @author Guillaume DROUET
 * @version 1.1
 * @since 0.3.1
 */
public abstract class AbstractNutDao implements NutDao, Runnable {

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
     * Polling interleave in seconds (-1 to disable).
     */
    private int pollingInterleave;

    /**
     * Index of the next proxy URI to use.
     */
    private final AtomicInteger nextProxyIndex;

    /**
     * Help to know when a polling operation is done.
     */
    private Future<?> pollingResult;

    /**
     * All observers per nut.
     */
    private final Map<String, NutPolling> resourceObservers;

    /**
     * <p>
     * Builds a new instance.
     * </p>
     *
     * @param base prefix for all paths (a {@link BadArgumentException} is thrown is {@code null}
     * @param basePathAsSysProp {@code true} if the base path is a system property
     * @param proxies proxy URIs serving the nut
     * @param pollingSeconds interleave in seconds for polling feature (-1 to disable)
     */
    public AbstractNutDao(final String base,
                          final Boolean basePathAsSysProp,
                          final String[] proxies,
                          final int pollingSeconds) {
        if (base == null) {
            throw new BadArgumentException(new IllegalArgumentException("Base path can't be null"));
        }

        basePath = IOUtils.mergePath("/", basePathAsSysProp ? System.getProperty(base) : base);
        proxyUris = proxies == null ? null : Arrays.copyOf(proxies, proxies.length);
        nextProxyIndex = new AtomicInteger(0);
        resourceObservers = new HashMap<String, NutPolling>();
        setPollingInterleave(pollingSeconds);
    }

    /**
     * <p>
     * Returns the polling interleave.
     * </p>
     *
     * @return the polling interleave
     */
    public final int getPollingInterleave() {
        return pollingInterleave;
    }

    /**
     * <p>
     * Defines a new polling interleave. If current polling operation are currently processed, then they are not interrupted
     * and a new scheduling is created if the given value is a positive number. If the value is not positive, then no
     * polling will occur.
     * </p>
     *
     * @param interleaveSeconds interleave in seconds
     */
    public final synchronized void setPollingInterleave(final int interleaveSeconds) {

        // Stop current scheduling
        if (pollingResult != null) {
            log.info("Cancelling repeated polling operation for {}", getClass().getName());
            pollingResult.cancel(false);
            pollingResult = null;
        }

        pollingInterleave = interleaveSeconds;

        // Create new scheduling if necessary
        if (pollingInterleave > 0) {
            log.info("Start polling operation for {} repeated every {} seconds", getClass().getName(), pollingInterleave);
            pollingResult = WuicScheduledThreadPool.getInstance().executeEveryTimeInSeconds(this, pollingInterleave);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void run() {
        log.info("Running polling operation for {}", toString());

        // Keep in this set all listeners that told that they don't want to be notified until the end of the operation
        final Set<NutDaoListener> exclusions = new HashSet<NutDaoListener>();

        synchronized (resourceObservers) {

            // Poll each nut's path
            for (final Map.Entry<String, NutPolling> entry : resourceObservers.entrySet()) {
                final String nut = entry.getKey();
                final NutPolling pollingData = entry.getValue();

                try {
                    // Nut has changed since its last call
                    if (pollingData.lastUpdate(getLastUpdateTimestampFor(nut))) {
                        for (NutDaoListener o : pollingData.listeners) {
                            // Not already excluded and asks for exclusion
                            if (!exclusions.contains(o) && !o.resourceUpdated(this, nut)) {
                                exclusions.add(o);
                            }
                        }
                    }
                } catch (StreamException se) {
                    log.warn(String.format("Unable to poll nut %s", nut), se);
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void observe(final String realPath, final NutDaoListener... listeners) throws StreamException {
        synchronized (resourceObservers) {
            final NutPolling nutPolling = resourceObservers.containsKey(realPath)
                    ? resourceObservers.get(realPath) : new NutPolling(getLastUpdateTimestampFor(realPath));

            nutPolling.addListeners(listeners);
            resourceObservers.put(realPath, nutPolling);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Nut> create(final String pathName) throws StreamException {
        final List<String> pathNames = computeRealPaths(pathName);
        final List<Nut> retval = new ArrayList<Nut>(pathNames.size());

        for (final String p : pathNames) {
            final int index = p.lastIndexOf('.');

            if (index < 0) {
                log.warn(String.format("'%s' does not contains any extension, ignoring resource", p));
                continue;
            }

            final String ext = p.substring(index);
            final NutType type = NutType.getNutTypeForExtension(ext);
            final Nut res = accessFor(p, type);

            retval.add(res);
        }

        return retval;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String proxyUriFor(final Nut resource) {
        if (proxyUris != null && proxyUris.length > 0) {

            synchronized (nextProxyIndex) {
                // End of round, restart
                if (nextProxyIndex.intValue() >= proxyUris.length) {
                    nextProxyIndex.set(0);
                }

                // Do the round-robin by incrementing nextProxyIndex counter
                return IOUtils.mergePath(String.valueOf(proxyUris[nextProxyIndex.getAndIncrement()]), resource.getName());
            }
        } else {
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void save(final Nut resource) {
        throw new SaveOperationNotSupportedException(this.getClass());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Boolean saveSupported() {
        return false;
    }

    /**
     * <p>
     * Returns a list of paths depending of the behavior of the factory with the given path.
     * In fact, the factory could consider the path as a regex, a directory, etc.
     * </p>
     *
     * @param pathName the path access
     * @return the resulting real paths
     * @throws StreamException if an I/O error occurs when creating the nut
     */
    public List<String> computeRealPaths(final String pathName) throws StreamException {
        final List<String> paths = listResourcesPaths(pathName);
        final List<String> retval = new ArrayList<String>(paths.size());

        for (String p : paths) {
            retval.add(p);
        }

        return retval;
    }

    /**
     * <p>
     * Returns the base path prefixing all paths.
     * </p>
     *
     * @return the base path
     */
    protected String getBasePath() {
        return basePath;
    }

    /**
     * <p>
     * Lists all the resources path matching the given pattern.
     * </p>
     *
     * @param pattern the pattern
     * @throws com.github.wuic.exception.wrapper.StreamException if any I/O error occurs while reading resources
     */
    protected abstract List<String> listResourcesPaths(String pattern) throws StreamException;

    /**
     * <p>
     * Creates an access for the given parameters through a {@link Nut} implementation.
     * </p>
     *
     * @param realPath the real path to use to access the nut
     * @param type the path's type
     * @return the {@link Nut}
     * @throws com.github.wuic.exception.wrapper.StreamException if an I/O error occurs while creating access
     */
    protected abstract Nut accessFor(String realPath, NutType type) throws StreamException;

    /**
     * <p>
     * Retrieves a timestamp that indicates the last time this nut has changed.
     * </p>
     *
     * @param path the real path of the nut
     * @return the timestamp
     * @throws StreamException if any I/O error occurs
     */
    protected abstract Long getLastUpdateTimestampFor(final String path) throws StreamException;

    /**
     * {@inheritDoc}
     */
    public String toString() {
        return String.format("%s with base path %s", getClass().getName(), getBasePath());
    }

    /**
     * <p>
     * Internal class which tracks observers and last update informations for a particular nut.
     * </p>
     *
     * @author Guillaume DROUET
     * @version 1.0
     * @since 0.4.0
     */
    private final class NutPolling {

        /**
         * Listeners.
         */
        private Set<NutDaoListener> listeners;

        /**
         * Last update.
         */
        private Long lastUpdate;

        /**
         * <p>
         * Creates a new instance.
         * </p>
         *
         * @param lastUpdateTimestamp the timestamp representing the last update of the nut
         */
        private NutPolling(final long lastUpdateTimestamp) {
            listeners = new HashSet<NutDaoListener>();
            lastUpdate = lastUpdateTimestamp;
        }

        /**
         * <p>
         * Updates the timestamp indicating when the nut has been updated for the last time.
         * </p>
         *
         * @param timestamp the timestamp
         * @return {@code true} if the timestamp has changed, {@code false} otherwise
         */
        public Boolean lastUpdate(final long timestamp) {
            final Boolean retval = timestamp != this.lastUpdate;
            this.lastUpdate = timestamp;
            return retval;
        }

        /**
         * <p>
         * Adds all the specified listeners.
         * </p>
         *
         * @param listener the array to add
         */
        public void addListeners(final NutDaoListener ... listener) {
            Collections.addAll(listeners, listener);
        }
    }
}
