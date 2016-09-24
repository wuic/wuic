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
 * This interface represents a path which is a directory.
 * </p>
 *
 * @author Guillaume DROUET
 * @since 0.3.4
 */
public interface DirectoryPath extends Path {

    /**
     * <p>
     * Gets the {@link Path} corresponding to the given {@code String} which should be interpreted relatively to
     * this directory.
     * </p>
     *
     * @param path the path representing the child relatively to this directory
     * @return the result of resolution
     * @throws IOException if any I/O error occurs
     */
    Path getChild(String path) throws IOException;

    /**
     * <p>
     * Lists all the children path names of this directory.
     * </p>
     *
     * @return the children path names
     * @throws IOException if any I/O error occurs
     */
    String[] list() throws IOException;
}
