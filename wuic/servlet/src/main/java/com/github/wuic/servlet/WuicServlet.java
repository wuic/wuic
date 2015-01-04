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

import com.github.wuic.ApplicationConfig;
import com.github.wuic.ContextBuilder;
import com.github.wuic.ContextBuilderConfigurator;
import com.github.wuic.ContextInterceptorAdapter;
import com.github.wuic.WuicFacade;
import com.github.wuic.engine.EngineRequest;
import com.github.wuic.engine.EngineRequestBuilder;
import com.github.wuic.exception.ErrorCode;
import com.github.wuic.exception.NutNotFoundException;
import com.github.wuic.exception.wrapper.BadArgumentException;
import com.github.wuic.exception.wrapper.StreamException;
import com.github.wuic.exception.WuicException;
import com.github.wuic.jee.WuicServletContextListener;
import com.github.wuic.nut.ConvertibleNut;
import com.github.wuic.util.UrlMatcher;
import com.github.wuic.util.UrlUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.Map;

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
     * Serial version UID.
     */
    private static final long serialVersionUID = -7678202861072625737L;

    /**
     * The logger.
     */
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    /**
     * Mapping error code to their HTTP code.
     */
    private Map<Long, Integer> errorCodeToHttpCode;

    /**
     * Servlet mapping.
     */
    private String servletMapping;

    /**
     * The WUIC facade.
     */
    private WuicFacade wuicFacade;

    /**
     * <p>
     * Builds a new instance.
     * </p>
     */
    public WuicServlet() {
        errorCodeToHttpCode = new HashMap<Long, Integer>();
        errorCodeToHttpCode.put(ErrorCode.NUT_NOT_FOUND, HttpURLConnection.HTTP_NOT_FOUND);
        errorCodeToHttpCode.put(ErrorCode.WORKFLOW_NOT_FOUND, HttpURLConnection.HTTP_NOT_FOUND);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void init(final ServletConfig config) throws ServletException {

        // Validate servlet mapping
        final String key = ApplicationConfig.WUIC_SERVLET_CONTEXT_PARAM;
        servletMapping = WuicServletContextListener.getParamProvider(config.getServletContext()).apply(key, null);

        if (servletMapping == null) {
            throw new BadArgumentException(new IllegalArgumentException(String.format("Init param '%s' must be defined", key)));
        } else if (servletMapping.isEmpty()) {
            throw new BadArgumentException(new IllegalArgumentException(String.format("Init param '%s' could not be empty", key)));
        }

        try {
            wuicFacade = WuicServletContextListener.getWuicFacade(config.getServletContext());
            wuicFacade.configure(new WuicServletContextBuilderConfigurator());
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
        final String requestUri = request.getRequestURI();
        final UrlMatcher matcher = UrlUtils.urlMatcher(requestUri.substring(requestUri.indexOf(servletMapping) + servletMapping.length()));

        if (!matcher.matches()) {
            response.getWriter().println(UrlMatcher.MATCHER_MESSAGE);
            response.setStatus(HttpURLConnection.HTTP_BAD_REQUEST);
        } else {
            final Runnable r = HttpRequestThreadLocal.INSTANCE.canGzip(request);

            try {
                writeNut(matcher.getWorkflowId(), matcher.getNutName(), response);
            } catch (WuicException we) {
                log.error("Unable to retrieve nut", we);

                // Use 500 has default status code
                final Integer httpCode = errorCodeToHttpCode.containsKey(we.getErrorCode()) ?
                        errorCodeToHttpCode.get(we.getErrorCode()) : HttpURLConnection.HTTP_INTERNAL_ERROR;

                response.getWriter().println(we.getMessage());
                response.setStatus(httpCode);
            } finally {
                r.run();
            }
        }
    }

    /**
     * <p>
     * Writes a nut in the {@link HttpServletResponse}.
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
        final ConvertibleNut nut = wuicFacade.runWorkflow(workflowId, nutName);

        // Nut found
        if (nut != null) {
            HttpRequestThreadLocal.INSTANCE.write(nut, response);
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
            final Boolean canGzip = HttpRequestThreadLocal.INSTANCE.canGzip();

            if (canGzip != null && !canGzip) {
                return new EngineRequestBuilder(request).workflowId(request.getWorkflowId()).contextPath(request.getWorkflowId() + "-ungzip").build();
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
