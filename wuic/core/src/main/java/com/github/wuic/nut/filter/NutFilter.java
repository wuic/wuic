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


package com.github.wuic.nut.filter;

import java.util.List;

/**
 * <p>
 * A filter can be defined to alter the set of paths used by the {@link com.github.wuic.nut.NutsHeap heap} to create its
 * {@link com.github.wuic.nut.Nut nuts}.
 * </p>
 *
 * @author Guillaume DROUET
 * @since 0.4.5
 */
public interface NutFilter {

    /**
     * <p>
     * Filters the given paths list provided by the {@link com.github.wuic.nut.NutsHeap} associated to the specified ID
     * and returns the result.
     * </p>
     *
     * <p>
     * Depending of the implementation, the result could be the instance specified in parameter or a new one
     * </p>
     *
     * @param paths the paths to filter
     * @return the filtering result
     */
    List<String> filterPaths(List<String> paths);
}
