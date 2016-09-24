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


package com.github.wuic.servlet.path;

import com.github.wuic.path.DirectoryPath;
import com.github.wuic.path.DirectoryPathFactory;

import javax.servlet.ServletContext;

/**
 * <p>
 * {@link DirectoryPathFactory} in charge of creating new {@link WebappDirectoryPath}.
 * </p>
 *
 * @author Guillaume DROUET
 * @since 0.4.2
 */
public class WebappDirectoryPathFactory implements DirectoryPathFactory {

    /**
     * The servlet context used by any created {@link WebappDirectoryPath}.
     */
    private final ServletContext context;

    /**
     * The charset.
     */
    private final String charset;

    /**
     * <p>
     * Creates a new instance.
     * </p>
     *
     * @param cs the charset
     * @param ctx the servlet context used to create {@link WebappDirectoryPath}
     */
    public WebappDirectoryPathFactory(final ServletContext ctx, final String cs) {
        context = ctx;
        charset = cs;
    }

    /**
     * {@inheritDoc}
     */
    public DirectoryPath create(final String path) {
        return new WebappDirectoryPath(path, null, context, charset);
    }
}
