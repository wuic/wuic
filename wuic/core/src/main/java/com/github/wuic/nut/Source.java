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

import java.util.List;

/**
 * <p>
 * This interface represents the source object to keep the information of all nuts originally used in a
 * transformation process.
 * </p>
 *
 * <p>
 * Initially, the object should be created for any {@link ConvertibleNut}.
 * </p>
 *
 * @author Guillaume DROUET
 * @since 0.5.3
 */
public interface Source {

    /**
     * <p>
     * Gets all the {@link ConvertibleNut sources}. Can be immutable.
     * </p>
     *
     * @return the sources (must be a copy of the internal list)
     */
    List<ConvertibleNut> getOriginalNuts();

    /**
     * <p>
     * Adds a new nut.
     * </p>
     *
     * @param convertibleNut the original nut
     */
    void addOriginalNut(ConvertibleNut convertibleNut);
}