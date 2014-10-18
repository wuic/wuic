/*
 * "Copyright (c) 2014   Capgemini Technology Services (hereinafter "Capgemini")
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


package com.github.wuic.servlet;

import com.github.wuic.exception.NutNotFoundException;
import com.github.wuic.exception.wrapper.StreamException;
import com.github.wuic.nut.ConvertibleNut;
import com.github.wuic.util.IOUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

/**
 * <p>
 * Tool that can be used to bind to a {@link ThreadLocal} an HTTP request state to know if GZIP is supported and then
 * write according to this information a {@link ConvertibleNut} to the HTTP response.
 * </p>
 *
 * @author Guillaume DROUET
 * @version 1.0
 * @since 0.5.0
 */
public enum HttpRequestThreadLocal implements Runnable {

    /**
     * Singleton.
     */
    INSTANCE;

    /**
     * Servlet request supporting GZIP or not during workflow processing.
     */
    private final ThreadLocal<Boolean> canGzipThreadLocal = new ThreadLocal<Boolean>();

    /**
     * The charset used to write the response.
     */
    private final String charset;

    /**
     * Builds a new instance.
     */
    private HttpRequestThreadLocal() {
        charset = System.getProperty("file.encoding");
    }

    /**
     * <p>
     * Indicates if the {@link javax.servlet.http.HttpServletRequest} bound to the current {@link #canGzipThreadLocal}
     * thread local supports GZIP.
     * </p>
     *
     * @return {@code true} if it supports GZIP or if not http request is bound, {@code false} otherwise
     */
    public boolean canGzip() {
        final Boolean canGzip = canGzipThreadLocal.get();
        return canGzip == null || canGzip;
    }

    /**
     * <p>
     * Indicates if the given {@link javax.servlet.http.HttpServletRequest} supports GZIP or not in
     * {@link #canGzipThreadLocal} thread local.
     * </p>
     */
    public Runnable canGzip(final HttpServletRequest request) {
        final Boolean can;

        if (request != null) {
            final String acceptEncoding = request.getHeader("Accept-Encoding");

            // Accept-Encoding must be set and GZIP specific
            can = acceptEncoding != null && acceptEncoding.contains("gzip");
        } else {
            can = Boolean.TRUE;
        }

        canGzipThreadLocal.set(can);
        return this;
    }

    /**
     * <p>
     * Serves the given nut by changing the specified response's state. The method sets headers and writes response.
     * </p>
     *
     * @param nut the nut to write
     * @param response the response
     * @throws NutNotFoundException if stream could not be opened
     * @throws StreamException if stream could not be written
     */
    public void write(final ConvertibleNut nut, final HttpServletResponse response)
            throws NutNotFoundException, StreamException {
        response.setCharacterEncoding(charset);
        response.setContentType(nut.getNutType().getMimeType());

        // We set a far expiration date because we assume that polling will change the timestamp in path
        response.setHeader("Expires", "Sat, 06 Jun 2086 09:35:00 GMT");

        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            nut.transform(bos);

            if (canGzip() && nut.isCompressed()) {
                response.setHeader("Content-Encoding", "gzip");
                response.setHeader("Vary", "Accept-Encoding");
            } else if (nut.isCompressed()) {
                InputStream is = null;

                try {
                    is = new GZIPInputStream(new ByteArrayInputStream(bos.toByteArray()));
                    bos = new ByteArrayOutputStream();
                    IOUtils.copyStream(is, bos);
                } finally {
                    IOUtils.close(is);
                }
            }

            response.setContentLength(bos.toByteArray().length);
            response.getOutputStream().write(bos.toByteArray());
        } catch (IOException ioe) {
            throw new StreamException(ioe);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void run() {
        canGzipThreadLocal.remove();
    }
}
