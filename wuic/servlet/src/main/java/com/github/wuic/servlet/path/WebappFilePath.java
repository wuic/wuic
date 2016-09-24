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
import com.github.wuic.path.FilePath;
import com.github.wuic.path.core.SimplePath;
import com.github.wuic.util.DefaultInput;
import com.github.wuic.util.Input;

import javax.servlet.ServletContext;
import java.io.IOException;
import java.io.InputStream;

/**
 * <p>
 * Represents a file in the war file.
 * </p>
 *
 * @author Guillaume DROUET
 * @since 0.4.2
 */
public class WebappFilePath extends SimplePath implements FilePath {

    /**
     * Servlet context.
     */
    private ServletContext context;

    /**
     * <p>
     * Creates a new instance.
     * </p>
     *
     * @param cs the charset
     * @param n the name
     * @param dp the parent
     * @param context the servlet context used to access nut's stream
     */
    public WebappFilePath(final String n, final DirectoryPath dp, final ServletContext context, final String cs) {
        super(n, dp, cs);
        this.context = context;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getLastUpdate() throws IOException {
        // In JEE, war is can't be reached so we are not able to get last timestamp
        return -1L;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Input openStream() throws IOException {
        final InputStream is = context.getResourceAsStream(getAbsolutePath());
        return is == null ? null : new DefaultInput(is, getCharset());
    }
}
