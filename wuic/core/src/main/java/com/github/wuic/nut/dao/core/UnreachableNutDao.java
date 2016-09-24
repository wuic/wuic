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


package com.github.wuic.nut.dao.core;

import com.github.wuic.NutType;
import com.github.wuic.ProcessContext;
import com.github.wuic.config.Alias;
import com.github.wuic.config.Config;
import com.github.wuic.nut.AbstractNutDao;
import com.github.wuic.nut.NotReachableNut;
import com.github.wuic.nut.Nut;
import com.github.wuic.nut.dao.NutDaoService;
import com.github.wuic.util.Input;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * <p>
 * A {@link com.github.wuic.nut.dao.NutDao} implementation for unreachable nuts used in coordination with
 * {@link com.github.wuic.engine.core.StaticEngine}.
 * </p>
 *
 * @author Guillaume DROUET
 * @since 0.5.2
 */
@NutDaoService
@Alias("unreachable")
public class UnreachableNutDao extends AbstractNutDao {

    /**
     * <p>
     * Initializes a new instance.
     * </p>
     */
    @Config
    public void init() {
        init("/", null, -1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Long getLastUpdateTimestampFor(final String path) throws IOException {
        return -1L;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected List<String> listNutsPaths(final String pattern) throws IOException {
        return Arrays.asList(pattern);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Nut accessFor(final String realPath, final NutType type, final ProcessContext processContext) throws IOException {
        return new NotReachableNut(realPath, type, "unknown heap", -1L);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Input newInputStream(final String path, final ProcessContext processContext) throws IOException {
        throw new IOException("Nut content is unreachable.");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Boolean exists(final String path, final ProcessContext processContext) throws IOException {
        return Boolean.TRUE;
    }
}
