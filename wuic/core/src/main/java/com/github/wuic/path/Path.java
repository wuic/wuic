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


package com.github.wuic.path;

import java.io.IOException;

/**
 * <p>
 * A path is an element which is could be linked to another path as parent to represent a hierarchy.
 * </p>
 *
 * @author Guillaume DROUET
 * @since 0.3.4
 */
public interface Path {

    /**
     * <p>
     * Returns the path name.
     * </p>
     *
     * @return the path name
     */
    String getName();

    /**
     * <p>
     * Returns the parent path. Should be {@code null} if this path is considered as a root.
     * </p>
     *
     * @return the parent
     */
    DirectoryPath getParent();

    /**
     * <p>
     * Returns the absolute path of this path by separating each parent path with a standard
     * {@link com.github.wuic.util.IOUtils#STD_SEPARATOR separator}.
     * </p>
     *
     * @return the absolute path
     */
    String getAbsolutePath();

    /**
     * <p>
     * Gets the last update date of this path.
     * </p>
     *
     * @return a date representing the date when this path was updated
     * @throws IOException if an I/O error occurs while accessing path data
     */
    long getLastUpdate() throws IOException;


}
