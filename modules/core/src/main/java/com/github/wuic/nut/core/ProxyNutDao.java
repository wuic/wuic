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


package com.github.wuic.nut.core;


import com.github.wuic.exception.NutNotFoundException;
import com.github.wuic.exception.wrapper.StreamException;
import com.github.wuic.nut.Nut;
import com.github.wuic.nut.NutDao;
import com.github.wuic.nut.NutDaoListener;
import com.github.wuic.util.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * Internal {@link com.github.wuic.nut.NutDao} class that maps particular path to a particular
 * {@link com.github.wuic.nut.Nut} when the {@link com.github.wuic.nut.NutDao#create(String)}
 * method is invoked. If the path is not mapped, then a delegated DAO is called.
 * </p>
 *
 * @author Guillaume DROUET
 * @version 1.0
 * @since 0.4.4
 */
public class ProxyNutDao implements NutDao {

    /**
     * The delegated DAO.
     */
    private NutDao delegate;

    /**
     * All mapped path to corresponding nut.
     */
    private Map<String, Nut> proxy;

    /**
     * The root path.
     */
    private String rootPath;

    /**
     * <p>
     * Builds a new instance thanks to a delegated {@link NutDao}.
     * </p>
     *
     * @param rootPath the root path that prefixes any rule
     * @param delegate the delegated DAO
     */
    public ProxyNutDao(final String rootPath, final NutDao delegate) {
        this.delegate = delegate;
        this.proxy = new HashMap<String, Nut>();
        this.rootPath = rootPath;
    }

    /**
     * <p>
     * Adds a mapping between a path and a nut.
     * </p>
     *
     * @param path the path
     * @param nut the nut returned when path is used
     */
    public void addRule(final String path, final Nut nut){
        proxy.put(rootPath.isEmpty() ? path : IOUtils.mergePath(rootPath, path), nut);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void observe(final String realPath, final NutDaoListener... listeners) throws StreamException {
        // do not observe anything, feature not available here.
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Nut> create(final String path) throws StreamException {
        final Nut nut = proxy.get(path);
        List<Nut> retval = Arrays.asList(nut);

        // Nut not mapped, delegate call
        if (nut == null) {
            retval = delegate.create(path);
        }

        return retval;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Nut> create(final String path, final PathFormat format) throws StreamException {
        final Nut nut = proxy.get(path);
        List<Nut> retval = Arrays.asList(nut);

        // Nut not mapped, delegate call
        if (nut == null) {
            retval = delegate.create(path, format);
        }

        return retval;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String proxyUriFor(final Nut nut) {
        return delegate.proxyUriFor(nut);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void save(final Nut nut) {
        throw new UnsupportedOperationException();
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
        proxy.clear();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NutDao withRootPath(final String rootPath) {
        return delegate.withRootPath(rootPath);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public InputStream newInputStream(final String path) throws StreamException {
        final Nut nut = proxy.get(path);

        // Path not mapped, call delegate
        if (nut == null) {
            return delegate.newInputStream(path);
        } else {
            try {
                return nut.openStream();
            } catch (NutNotFoundException nnfe) {
                throw new StreamException(new IOException(nnfe));
            }
        }
    }
}