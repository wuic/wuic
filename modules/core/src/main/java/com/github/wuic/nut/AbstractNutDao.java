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
import com.github.wuic.util.PollingScheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * <p>
 * An abstract implementation of a {@link NutDao}. As any implementation should provides it, this class defines a base
 * path when retrieved nuts, a set of proxies URIs and a polling feature.
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
        setPollingInterleave(pollingSeconds);
    }

    /**
     * {@inheritDoc}
     */
    public void run() {
        log.info("Running polling operation for {}", toString());

        // Keep in this set all listeners that told that they don't want to be notified until the end of the operation
        final Set<NutDaoListener> exclusions = new HashSet<NutDaoListener>();

        synchronized (getNutObservers()) {

            // Poll each nut's path
            for (final Map.Entry<String, ? extends Polling> entry : getNutObservers().entrySet()) {
                final String nut = entry.getKey();
                final Polling pollingData = entry.getValue();

                try {
                    final Set<String> paths = new HashSet<String>(listNutsPaths(pollingData.getPattern()));
                    Map<String, Long> timestamps = null;

                    // Notifies listeners
                    for (final NutDaoListener o : pollingData.getListeners()) {
                        final boolean excluded = exclusions.contains(o);

                        // Not already excluded and asks for exclusion
                        if (!excluded && !o.polling(paths)) {
                            exclusions.add(o);
                        } else if (!excluded) {

                            // Timestamps not already retrieved
                            if (timestamps == null) {
                                timestamps = new HashMap<String, Long>(paths.size());

                                for (final String path : paths) {
                                    final Long timestamp = getLastUpdateTimestampFor(path);
                                    timestamps.put(path, timestamp);

                                    // Stop notifying
                                    if (!o.nutPolled(this, path, timestamp)) {
                                        exclusions.add(o);
                                        break;
                                    }
                                }
                            } else {
                                for (final Map.Entry<String, Long> pathEntry : timestamps.entrySet()) {
                                    // Stop notifying
                                    if (!o.nutPolled(this, pathEntry.getKey(), pathEntry.getValue())) {
                                        exclusions.add(o);
                                        break;
                                    }
                                }
                            }
                        }
                    }
                } catch (StreamException se) {
                    log.error(String.format("Unable to poll nut %s", nut), se);
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<Nut, Long> create(final String pathName) throws StreamException {
        final List<String> pathNames = computeRealPaths(pathName);
        final Map<Nut, Long> retval = new HashMap<Nut, Long>(pathNames.size());

        for (final String p : pathNames) {
            final int index = p.lastIndexOf('.');

            if (index < 0) {
                log.warn(String.format("'%s' does not contains any extension, ignoring nut", p));
                continue;
            }

            final String ext = p.substring(index);
            final NutType type = NutType.getNutTypeForExtension(ext);
            final Nut res = accessFor(p, type);

            // Poll only if polling interleave is enabled
            retval.put(res, getPollingInterleave() != -1 ? getLastUpdateTimestampFor(p) : -1L);
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
                return IOUtils.mergePath(String.valueOf(proxyUris[nextProxyIndex.getAndIncrement()]), nut.getName());
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
     * {@inheritDoc}
     */
    @Override
    public void shutdown() {
        log.info("Shutdown DAO {}", getClass().getName());

        // Will stop any scheduled operation
        if (getPollingInterleave() != -1) {
            setPollingInterleave(-1);
        }
    }

    /**
     * <p>
     * Returns a list of paths depending of the behavior of the dao with the given path.
     * In fact, the dao could consider the path as a regex, a directory, etc.
     * </p>
     *
     * @param pathName the path access
     * @return the resulting real paths
     * @throws StreamException if an I/O error occurs when creating the nut
     */
    public List<String> computeRealPaths(final String pathName) throws StreamException {
        final List<String> paths = listNutsPaths(pathName);
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
     * Lists all the nuts path matching the given pattern.
     * </p>
     *
     * @param pattern the pattern
     * @throws com.github.wuic.exception.wrapper.StreamException if any I/O error occurs while reading nuts
     */
    protected abstract List<String> listNutsPaths(String pattern) throws StreamException;

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
     * {@inheritDoc}
     */
    public String toString() {
        return String.format("%s with base path %s", getClass().getName(), getBasePath());
    }
}
