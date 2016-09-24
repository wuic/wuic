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


package com.github.wuic.servlet.test;

import com.github.wuic.EnumNutType;
import com.github.wuic.servlet.HtmlParserFilter;
import com.github.wuic.util.NumberUtils;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.IOException;

/**
 * <p>
 * Filter that helps testing {@link HtmlParserFilter} by skipping the issue 123.
 * </p>
 *
 * @author Guillaume DROUET
 * @since 0.5.0
 */
public class SkipIssue123 extends HtmlParserFilter {

    /**
     * {@inheritDoc}
     */
    @Override
    public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain)
            throws IOException, ServletException {
        super.doFilter(request, new HttpServletResponseWrapper(HttpServletResponse.class.cast(response)) {

            /**
             * {@inheritDoc}
             */
            @Override
            public String getContentType() {
                return EnumNutType.HTML.getMimeType();
            }
        }, chain);
    }

    // tag::extractWorkflowId[]
    /**
     * {@inheritDoc}
     */
    @Override
    protected byte[][] extractWorkflowId(final HttpServletRequest httpRequest) {
        final byte[][] bytes = new byte[NumberUtils.TWO][];
        bytes[0] = httpRequest.getServletPath().getBytes();
        bytes[1] = httpRequest.getQueryString() != null ? httpRequest.getQueryString().getBytes() : new byte[0];

        return bytes;
    }
    // tag::extractWorkflowId[]
}
