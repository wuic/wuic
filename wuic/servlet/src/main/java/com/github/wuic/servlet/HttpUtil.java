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


package com.github.wuic.servlet;

import com.github.wuic.nut.ConvertibleNut;
import com.github.wuic.util.Pipe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * <p>
 * Tool that can be used to deal with an HTTP request state to know if GZIP is supported and then write according to
 * this information a {@link ConvertibleNut} to the HTTP response.
 * </p>
 *
 * @author Guillaume DROUET
 * @version 1.0
 * @since 0.5.0
 */
public enum HttpUtil {

    /**
     * Singleton.
     */
    INSTANCE;

    /**
     * The logger.
     */
    private final Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * The charset used to write the response.
     */
    private final String charset;

    /**
     * Builds a new instance.
     */
    private HttpUtil() {
        charset = System.getProperty("file.encoding");
    }

    /**
     * <p>
     * Indicates if the given {@link javax.servlet.http.HttpServletRequest} supports GZIP or not.
     * </p>
     *
     * @param request the request indicating GZIP support
     * @return {@code true} if GZIP is supported, {@code false} otherwise
     */
    public boolean canGzip(final HttpServletRequest request) {
        final boolean can;

        if (request != null) {
            final String acceptEncoding = request.getHeader("Accept-Encoding");

            // Accept-Encoding must be set and GZIP specific
            can = acceptEncoding != null && acceptEncoding.contains("gzip");
        } else {
            can = true;
        }

        return can;
    }

    /**
     * <p>
     * Serves the given nut by changing the specified response's state. The method sets headers and writes response.
     * </p>
     *
     * @param nut the nut to write
     * @param response the response
     * @throws IOException if stream could not be opened
     */
    public void write(final ConvertibleNut nut, final HttpServletResponse response)
            throws IOException {
        logger.info("Writing to the response the content read from nut '{}'", nut.getName());
        response.setCharacterEncoding(charset);
        response.setContentType(nut.getNutType().getMimeType());

        // We set a far expiration date because we assume that polling will change the timestamp in path
        response.setHeader("Expires", "Sat, 06 Jun 2086 09:35:00 GMT");
        nut.transform(new WriteResponseOnReady(response, nut));
    }

    /**
     * <p>
     * This listener writes the response one the transformation is done.
     * </p>
     *
     * @author Guillaume DROUET
     * @version 1.0
     * @since 0.5.0
     */
    private final class WriteResponseOnReady implements Pipe.OnReady {

        /**
         * The response.
         */
        private final HttpServletResponse response;

        /**
         * The nut.
         */
        private final ConvertibleNut nut;

        /**
         * <p>
         * Builds a new instance.
         * </p>
         *
         * @param res the response
         * @param n the nut
         */
        private WriteResponseOnReady(final HttpServletResponse res, final ConvertibleNut n) {
            this.response = res;
            this.nut = n;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void ready(final Pipe.Execution e) {
            try {
                if (nut.isCompressed()) {
                    response.setHeader("Content-Encoding", "gzip");
                    response.setHeader("Vary", "Accept-Encoding");
                }

                response.setContentLength(e.getContentLength());
                e.writeResultTo(response.getOutputStream());
            } catch (IOException ioe) {
                logger.error("Cannot write response.", ioe);
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        }
    }
}
