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


package com.github.wuic.nut;

import com.github.wuic.util.IOUtils;

/**
 * <p>
 * This class wraps a {@link com.github.wuic.nut.Nut} that should be named with a particular prefix. The prefix could be
 * a path or not.
 * </p>
 *
 * @author Guillaume DROUET
 * @since 0.4.4
 */
public class PrefixedNut extends NutWrapper {

    /**
     * The name with prefix path.
     */
    private String name;

    /**
     * <p>
     * Builds a new instance.
     * </p>
     *
     * @param nut the nut to wrap
     * @param prefix the prefix
     * @param asPath if the prefix is a path
     */
    public PrefixedNut(final ConvertibleNut nut, final String prefix, final Boolean asPath) {
        super(nut);
        name = asPath ? IOUtils.mergePath(prefix, nut.getName()) : prefix + nut.getName();
    }

    /**
     * <p>
     * Builds a new instance.
     * </p>
     *
     * @param nut the nut to wrap
     * @param prefixPath the prefix path
     */
    public PrefixedNut(final ConvertibleNut nut, final String prefixPath) {
        this(nut, prefixPath, Boolean.TRUE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return name;
    }
}
