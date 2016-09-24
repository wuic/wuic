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


package com.github.wuic.nut.dao;

import com.github.wuic.ProcessContext;
import com.github.wuic.nut.Nut;
import com.github.wuic.util.Input;

import java.io.IOException;
import java.util.List;

/**
 * <p>
 * This wrapper delegates all implemented DAO to another {@link NutDao} instance.
 * </p>
 *
 * @author Guillaume DROUET
 * @since 0.5.3
 */
public class NutDaoWrapper implements NutDao {

    /**
     * Wrapped instance.
     */
    private final NutDao dao;

    /**
     * <p>
     * Builds a new instance.
     * </p>
     *
     * @param dao the wrapped DAO
     */
    public NutDaoWrapper(final NutDao dao) {
        this.dao = dao;
    }

    /**
     * <p>
     * Returns the wrapped DAO.
     * </p>
     *
     * @return the DAO
     */
    public final NutDao getNutDao() {
        return dao;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void observe(final String realPath, final NutDaoListener... listeners) throws IOException {
        dao.observe(realPath, listeners);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Nut> create(final String path, final ProcessContext processContext) throws IOException {
        return dao.create(path, processContext);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Nut> create(final String path, final PathFormat format, final ProcessContext processContext) throws IOException {
        return dao.create(path, format, processContext);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String proxyUriFor(final Nut nut) {
        return dao.proxyUriFor(nut);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void shutdown() {
        dao.shutdown();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NutDao withRootPath(final String rootPath) {
        return dao.withRootPath(rootPath);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Input newInputStream(final String path, final ProcessContext processContext) throws IOException {
        return dao.newInputStream(path, processContext);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Boolean exists(final String path, final ProcessContext processContext) throws IOException {
        return dao.exists(path, processContext);
    }
}
