/*
 * "Copyright (c) 2015   Capgemini Technology Services (hereinafter "Capgemini")
 *
 * License/Terms of Use
 * Permission is hereby granted, free of charge and for the term of intellectual
 * property rights on the Software, to any person obtaining a copy of this software
 * and associated documentation files (the "Software"), to use, copy, modify and
 * propagate free of charge, anywhere in the world, all or part of the Software
 * subject to the following mandatory conditions:
 *
 * -   The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * Any failure to comply with the above shall automatically terminate the license
 * and be construed as a breach of these Terms of Use causing significant harm to
 * Capgemini.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, PEACEFUL ENJOYMENT,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS
 * OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 * Except as contained in this notice, the name of Capgemini shall not be used in
 * advertising or otherwise to promote the use or other dealings in this Software
 * without prior written authorization from Capgemini.
 *
 * These Terms of Use are subject to French law.
 *
 * IMPORTANT NOTICE: The WUIC software implements software components governed by
 * open source software licenses (BSD and Apache) of which CAPGEMINI is not the
 * author or the editor. The rights granted on the said software components are
 * governed by the specific terms and conditions specified by Apache 2.0 and BSD
 * licenses."
 */


package com.github.wuic.jee;

import com.github.wuic.ProcessContext;
import com.github.wuic.exception.WuicException;

import javax.servlet.http.HttpServletRequest;

/**
 * <p>
 * A {@link ProcessContext} created in a {@link javax.servlet.http.HttpServletRequest} execution scope.
 * </p>
 *
 * @author Guillaume DROUET
 * @version 1.0
 * @since 0.5.2
 */
public class ServletProcessContext extends ProcessContext {

    /**
     * The wrapped request.
     */
    private final HttpServletRequest httpServletRequest;

    /**
     * <p>
     * Builds a new instance with a {@link HttpServletRequest request} to wrap.
     * </p>
     *
     * @param request the request to wrap
     */
    public ServletProcessContext(final HttpServletRequest request) {
        httpServletRequest = request;
    }

    /**
     * <p>
     * Try to cast the given object in the instance of this class. An {@link IllegalStateException} will be thrown if
     * the process context is {@code null} or an instance of a different class.
     * </p>
     *
     * @param processContext the process context
     * @return the process context
     */
    public static ServletProcessContext cast(final ProcessContext processContext) {
        if (processContext == null || !(processContext instanceof ServletProcessContext)) {
            WuicException.throwBadStateException(new IllegalStateException(
                    "Process context must wrap an HTTP request when WUIC is deployed in any servlet container."));
        }

        return ServletProcessContext.class.cast(processContext);
    }

    /**
     * <p>
     * Gets the wrapped request.
     * </p>
     *
     * @return the request
     */
    public HttpServletRequest getHttpServletRequest() {
        return httpServletRequest;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return String.format("ProcessContext[request=%s]", httpServletRequest);
    }
}
