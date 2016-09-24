
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


package com.github.wuic.test.ctx;

import com.github.wuic.ProcessContext;
import com.github.wuic.config.Config;
import com.github.wuic.nut.Nut;
import com.github.wuic.nut.dao.NutDao;
import com.github.wuic.nut.dao.NutDaoListener;
import com.github.wuic.nut.dao.NutDaoService;
import com.github.wuic.util.Input;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * <p>
 * Mocked DAO builder.
 * </p>
 *
 * @author Guillaume DROUET
 * @since 0.4.4
 */
@NutDaoService
public class MockSimpleDao implements NutDao {

    /**
     * Builds a new instance.
     */
    @Config
    public MockSimpleDao() {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void observe(final String realPath, final NutDaoListener... listeners) throws IOException {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Nut> create(final String path, final ProcessContext processContext) throws IOException {
        return Collections.emptyList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Nut> create(final String path, final PathFormat format, final ProcessContext processContext) throws IOException {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String proxyUriFor(final Nut nut) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void shutdown() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NutDao withRootPath(final String rootPath) {
        return null;
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
