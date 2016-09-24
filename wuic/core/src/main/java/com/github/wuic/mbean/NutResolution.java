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


package com.github.wuic.mbean;

import com.github.wuic.nut.dao.NutDao;

/**
 * <p>
 * Statistics object for a particular nut resolution.
 * </p>
 *
 * @author Guillaume DROUET
 * @since 0.5.3
 */
public class NutResolution implements NutResolutionMXBean {

    /**
     * The path representing the nut(s).
     */
    private String path;

    /**
     * The DAO class name that has been used for resolution.
     */
    private String daoClass;

    /**
     * The duration of the resolution.
     */
    private long duration;

    /**
     * <p>
     * Builds a new instance.
     * </p>
     *
     * @param path the path representing the nut(s)
     * @param daoClass the DAO class used for resolution
     * @param duration the duration of this resolution
     */
    public NutResolution(final String path, final Class<? extends NutDao> daoClass, final long duration) {
        this.path = path;
        this.daoClass = daoClass.getName();
        this.duration = duration;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getPath() {
        return path;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDaoClass() {
        return daoClass;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getDuration() {
        return duration;
    }
}