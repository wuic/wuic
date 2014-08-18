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

import com.github.wuic.ContextBuilder;
import com.github.wuic.ContextBuilderConfigurator;
import com.github.wuic.ContextInterceptorAdapter;
import com.github.wuic.engine.EngineRequest;
import com.github.wuic.exception.ErrorCode;
import com.github.wuic.exception.NutNotFoundException;
import com.github.wuic.exception.wrapper.BadArgumentException;
import com.github.wuic.exception.wrapper.StreamException;
import com.github.wuic.exception.WuicException;
import com.github.wuic.jee.WuicJeeContext;
import com.github.wuic.jee.WuicServletContextListener;
import com.github.wuic.nut.Nut;
import com.github.wuic.util.IOUtils;
import com.github.wuic.util.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <p>
 * Servlet which initializes the servlet context and the servlet mapping used for WUIC.
 * </p>
 *
 * @author Guillaume DROUET
 * @version 2.0
 * @since 0.1.1
 */
public class WuicServlet extends HttpServlet {

    /**
     * Servlet request supporting GZIP or not during workflow processing.
     */
    private static final ThreadLocal<Boolean> CAN_GZIP = new ThreadLocal<Boolean>();

    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = -7678202861072625737L;

    /**
     * The logger.
     */
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    /**
     * The expected pattern in request URI's.
     */
    private Pattern urlPattern;

    /**
     * Mapping error code to their HTTP code.
     */
    private Map<Long, Integer> errorCodeToHttpCode;

    /**
     * The charset used to write the response.
     */
    private final String charset;

    /**
     * <p>
     * Builds a new instance.
     * </p>
     */
    public WuicServlet() {
        errorCodeToHttpCode = new HashMap<Long, Integer>();
        errorCodeToHttpCode.put(ErrorCode.NUT_NOT_FOUND, HttpURLConnection.HTTP_NOT_FOUND);
        errorCodeToHttpCode.put(ErrorCode.WORKFLOW_NOT_FOUND, HttpURLConnection.HTTP_NOT_FOUND);
        charset = System.getProperty("file.encoding");
    }

    /**
     * <p>
     * Indicates if the {@link HttpServletRequest} bound to the current {@link #CAN_GZIP} thread local supports GZIP.
     * </p>
     *
     * @return {@code true} if it supports GZIP or if not http request is bound, {@code false} otherwise
     */
    public static boolean canGzip() {
        final Boolean canGzip = CAN_GZIP.get();
        return canGzip == null || canGzip;
    }

    /**
     * <p>
     * Indicates if the given {@link HttpServletRequest} supports GZIP or not in {@link #CAN_GZIP} thread local.
     * </p>
     */
    private static void canGzip(final HttpServletRequest request) {
        final Boolean can;

        if (request != null) {
            final String acceptEncoding = request.getHeader("Accept-Encoding");

            // Accept-Encoding must be set and GZIP specific
            can = acceptEncoding != null && acceptEncoding.contains("gzip");
        } else {
            can = Boolean.TRUE;
        }

        CAN_GZIP.set(can);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void init(final ServletConfig config) throws ServletException {
        // Build expected URL pattern
        final StringBuilder patternBuilder = new StringBuilder();
        final String key = WuicServletContextListener.WUIC_SERVLET_CONTEXT_PARAM;
        final String servletMapping = config.getServletContext().getInitParameter(key);

        if (servletMapping == null) {
            throw new BadArgumentException(new IllegalArgumentException(String.format("Init param '%s' must be defined", key)));
        }

        // Starts with nut mapping
        patternBuilder.append(Pattern.quote(servletMapping));

        if (!servletMapping.endsWith("/")) {
            patternBuilder.append("/");
        }

        // Followed by the workflow ID, a slash, its version timestamp, a new slash and finally the page name
        patternBuilder.append("([^/]*)/-?\\d*/?(.*)");

        urlPattern = Pattern.compile(patternBuilder.toString());

        try {
            WuicJeeContext.getWuicFacade().configure(new WuicServletContextBuilderConfigurator());
        } catch (WuicException we) {
            throw new ServletException(we);
        }

        super.init(config);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void doGet(final HttpServletRequest request, final HttpServletResponse response)
            throws ServletException, IOException {
        final Matcher matcher = urlPattern.matcher(request.getRequestURI());

        if (!matcher.find() || matcher.groupCount() != NumberUtils.TWO) {
            response.getWriter().println("Expected URL pattern: [workflowId]/[timestamp]/[nutName]");
            response.setStatus(HttpURLConnection.HTTP_BAD_REQUEST);
        } else {
            try {
                canGzip(request);
                writeNut(URLDecoder.decode(matcher.group(1), "UTF-8"),
                        URLDecoder.decode(matcher.group(NumberUtils.TWO), "UTF-8"),
                        response);
            } catch (WuicException we) {
                log.error("Unable to retrieve nut", we);

                // Use 500 has default status code
                final Integer httpCode = errorCodeToHttpCode.containsKey(we.getErrorCode()) ?
                        errorCodeToHttpCode.get(we.getErrorCode()) : HttpURLConnection.HTTP_INTERNAL_ERROR;

                response.getWriter().println(we.getMessage());
                response.setStatus(httpCode);
            } finally {
                CAN_GZIP.remove();
            }
        }
    }

    /**
     * <p>
     * Writes a nut in the HTTP response.
     * </p>
     *
     * @param workflowId the workflow ID
     * @param nutName the nut name
     * @param response the response
     * @throws WuicException if an I/O error occurs or nut not found
     */
    private void writeNut(final String workflowId, final String nutName, final HttpServletResponse response)
            throws WuicException {

        // Get the nuts workflow
        final Nut nut = WuicJeeContext.getWuicFacade().runWorkflow(workflowId, nutName);
        InputStream is = null;

        // Nut found
        if (nut != null) {
            try {
                response.setCharacterEncoding(charset);
                response.setContentType(nut.getNutType().getMimeType());

                if (canGzip()) {
                    response.setHeader("Content-Encoding", "gzip");
                    response.setHeader("Vary", "Accept-Encoding");
                }

                // We set a far expiration date because we assume that polling will change the timestamp in path
                response.setHeader("Expires", "Sat, 06 Jun 2086 09:35:00 GMT");

                is = nut.openStream();
                IOUtils.copyStream(is, response.getOutputStream());
                response.getOutputStream().flush();
            } catch (IOException ioe) {
                throw new StreamException(ioe);
            } finally {
                IOUtils.close(is);
            }
        } else {
            throw new NutNotFoundException(nutName, workflowId);
        }
    }

    /**
     * <p>
     * Configurator that injects {@link WuicServletContextInterceptor} to the context.
     * </p>
     *
     * @author Guillaume DROUET
     * @version 1.0
     * @since 0.5.0
     */
    private final class WuicServletContextBuilderConfigurator extends ContextBuilderConfigurator {

        /**
         * {@inheritDoc}
         */
        @Override
        public int internalConfigure(final ContextBuilder ctxBuilder) {
            ctxBuilder.interceptor(new WuicServletContextInterceptor());
            return -1;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getTag() {
            return getClass().getName();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected Long getLastUpdateTimestampFor(final String path) throws StreamException {
            return -1L;
        }
    }

    /**
     * <p>
     * Handles workflow ID to duplicate cache entries for both GZIP and non-GZIP content.
     * </p>
     *
     * @author Guillaume DROUET
     * @version 1.0
     * @since 0.5.0
     */
    private final class WuicServletContextInterceptor extends ContextInterceptorAdapter {

        /**
         * {@inheritDoc}
         */
        @Override
        public String beforeGetWorkflow(final String wId) {
            return wId;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public EngineRequest beforeProcess(final EngineRequest request) {
            final Boolean canGzip = CAN_GZIP.get();

            if (canGzip != null && !canGzip) {
                return new EngineRequest(request.getWorkflowId(), request.getWorkflowId() + "-ungzip", request);
            } else {
                return request;
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public EngineRequest beforeProcess(final EngineRequest request, final String path) {
            return beforeProcess(request);
        }
    }
}
