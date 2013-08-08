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

import com.github.wuic.FileType;
import com.github.wuic.exception.SaveOperationNotSupportedException;
import com.github.wuic.exception.wrapper.BadArgumentException;
import com.github.wuic.exception.wrapper.StreamException;
import com.github.wuic.util.IOUtils;
import com.github.wuic.util.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
/**
 * <p>
 * An abstract implementation of a {@link NutDao}. As any implementation should
 * provides it, this class defines a base path when retrieved resources, a set of proxies URIs and a polling feature.
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
    private int nextProxyIndex;

    /**
     * Thread that polls resources.
     */
    private Thread pollThread;

    /**
     * All observers per nut.
     */
    private Map<Nut, NutDaoListener[]> resourceObservers;

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
        pollingInterleave = pollingSeconds;
        nextProxyIndex = 0;
        resourceObservers = new HashMap<Nut, NutDaoListener[]>();

        if (pollingInterleave != -1) {
            pollThread = new Thread(this);
            pollThread.start();
        }
    }

    /**
     * {@inheritDoc}
     */
    public void run() {

        // Note : if polling is disabled at runtime, this thread needs to be created again when re-enabling
        while (pollingInterleave > 0) {
            try {
                // interleave is in seconds, sleep between each call
                Thread.sleep(pollingInterleave * NumberUtils.ONE_THOUSAND);

                for (final Map.Entry<Nut, NutDaoListener[]> entry : resourceObservers.entrySet()) {
                    final Nut res = entry.getKey();

                    try {
                        if (res.lastUpdate(getLastUpdateTimestampFor(res.getName()))) {
                            for (NutDaoListener o : entry.getValue()) {
                                o.resourceUpdated(this, res);
                            }
                        }
                    } catch (StreamException se) {
                        log.warn(String.format("Unable to poll nut %s", res.getName()), se);
                    }
                }
            } catch (InterruptedException ie) {
                log.info("Poll thread has been interrupted", ie);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Nut> create(final String pathName, final NutDaoListener... observers) throws StreamException {
        final List<String> pathNames = computeRealPaths(pathName);
        final List<Nut> retval = new ArrayList<Nut>(pathNames.size());

        for (final String p : pathNames) {
            final String ext = p.substring(p.lastIndexOf('.'));
            final FileType type = FileType.getFileTypeForExtension(ext);
            final Nut res = accessFor(p, type);

            // last update is an information only required when polling
            if (pollingInterleave > 0) {
                res.lastUpdate(getLastUpdateTimestampFor(p));
            }

            resourceObservers.put(res, observers);
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

            // End of round, restart
            if (nextProxyIndex >= proxyUris.length) {
                nextProxyIndex = 0;
            }

            // Do the round-robin by incrementing nextProxyIndex counter
            return IOUtils.mergePath(String.valueOf(proxyUris[nextProxyIndex++]), resource.getName());
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
    protected abstract Nut accessFor(String realPath, FileType type) throws StreamException;

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
    @Override
    protected void finalize() throws Throwable {
        if (pollThread != null) {
            pollThread.interrupt();
        }

        super.finalize();
    }
}
