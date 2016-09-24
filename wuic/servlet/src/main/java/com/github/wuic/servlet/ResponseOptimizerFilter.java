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


package com.github.wuic.servlet;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import java.io.IOException;

/**
 * <p>
 * The filter sets far "Expiry" header since the URL should be versioned by WUIC.
 * If the client supports GZIP, this optimizer also GZIP the response.
 * </p>
 *
 * @author Guillaume DROUET
 * @since 0.5.2
 */
public class ResponseOptimizerFilter implements Filter {

    /**
     * {@inheritDoc}
     */
    @Override
    public void init(final FilterConfig filterConfig) throws ServletException {
        // Nothing to do
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain)
            throws IOException, ServletException {
        final HttpServletResponse httpServletResponse = HttpServletResponse.class.cast(response);

        // Set header
        HttpUtil.INSTANCE.setExpireHeader(httpServletResponse);

        // Delegate call to the chain with a wrapper that GZIP the stream
        if (HttpUtil.INSTANCE.canGzip(HttpServletRequest.class.cast(request))) {
            final GzipHttpServletResponseWrapper gzip = new GzipHttpServletResponseWrapper(httpServletResponse);
            chain.doFilter(request, new HttpServletResponseWrapper(gzip));
            gzip.close();
        } else {
            // Delegate call to the chain
            chain.doFilter(request, response);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void destroy() {
        // Nothing to do
    }
}
