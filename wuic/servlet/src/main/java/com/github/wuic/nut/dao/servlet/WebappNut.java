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


package com.github.wuic.nut.dao.servlet;

import com.github.wuic.NutType;
import com.github.wuic.nut.AbstractNut;
import com.github.wuic.util.DefaultInput;
import com.github.wuic.util.Input;

import javax.servlet.ServletContext;
import java.io.IOException;
import java.util.concurrent.Future;

/**
 * <p>
 * Represents a nut on the path system provided or to be managed by the WUIC framework. Thanks to
 * {@link com.github.wuic.path.FilePath}, the nut could also be a ZIP entry.
 * </p>
 *
 * @author Guillaume DROUET
 * @since 0.4.2
 */
public class WebappNut extends AbstractNut {

    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 1L;

    /**
     * The servlet context which can open stream.
     */
    private ServletContext context;

    /**
     * Path in webapp.
     */
    private String path;

    /**
     * The charset.
     */
    private final String charset;

    /**
     * <p>
     * Builds a new {@code Nut} based on a context and path.
     * </p>
     *
     * @param ctx the context path
     * @param p the path
     * @param name the nut name
     * @param ft the path type
     * @param versionNumber the nuts's version number
     * @param cs the charset
     */
    public WebappNut(final ServletContext ctx,
                     final String p,
                     final String name,
                     final NutType ft,
                     final Future<Long> versionNumber,
                     final String cs) {
        super(name, ft, versionNumber);
        context = ctx;
        path = p;
        charset = cs;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Input openStream() throws IOException {
        return new DefaultInput(context.getResourceAsStream(path), charset);
    }
}
