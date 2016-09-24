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

import com.github.wuic.NutType;
import com.github.wuic.util.DefaultInput;
import com.github.wuic.util.Input;

import java.io.IOException;
import java.net.URL;
import java.util.concurrent.Future;

/**
 * <p>
 * A {@link com.github.wuic.nut.Nut} implementation for HTTP accesses.
 * </p>
 *
 * @author Guillaume DROUET
 * @since 0.3.1
 */
public class HttpNut extends AbstractNut {

    /**
     * The nut URL.
     */
    private URL nutUrl;

    /**
     * <p>
     * Builds a new instance.
     * </p>
     *
     * @param name the name
     * @param url the URL
     * @param nutType the path type
     * @param version the version
     */
    public HttpNut(final String name, final URL url, final NutType nutType, final Future<Long> version) {
        super(name, nutType, version);
        nutUrl = url;
    }

    /**               
     * {@inheritDoc}
     */
    @Override
    public Input openStream() throws IOException {
        return new DefaultInput(nutUrl.openStream(), getInitialNutType().getCharset());
    }
}
