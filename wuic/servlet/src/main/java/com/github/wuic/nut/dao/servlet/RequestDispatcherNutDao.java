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


package com.github.wuic.nut.dao.servlet;

import com.github.wuic.ApplicationConfig;
import com.github.wuic.NutType;
import com.github.wuic.ProcessContext;
import com.github.wuic.config.BooleanConfigParam;
import com.github.wuic.config.ConfigConstructor;
import com.github.wuic.config.IntegerConfigParam;
import com.github.wuic.config.ObjectConfigParam;
import com.github.wuic.config.StringConfigParam;
import com.github.wuic.exception.WuicException;
import com.github.wuic.jee.ServletProcessContext;
import com.github.wuic.jee.WuicServletContextListener;
import com.github.wuic.nut.AbstractNut;
import com.github.wuic.nut.AbstractNutDao;
import com.github.wuic.nut.Nut;
import com.github.wuic.nut.dao.NutDaoService;
import com.github.wuic.nut.dao.jee.ServletContextHandler;
import com.github.wuic.nut.setter.ProxyUrisPropertySetter;
import com.github.wuic.servlet.ByteArrayHttpServletResponseWrapper;
import com.github.wuic.servlet.HtmlParserFilter;
import com.github.wuic.servlet.HttpServletRequestAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Future;

/**
 * <p>
 * This {@link com.github.wuic.nut.dao.NutDao} relies on {@link ServletContext#getRequestDispatcher(String)} to create
 * {@link Nut nuts}.
 * </p>
 *
 * <p>
 * User should be aware that servlet specifications only require to use a request dispatcher with a real {@link HttpServletRequest}.
 * This real request is only available when an instance is created when a client requires its creation (e.g a workflow result
 * is not in the cache). When WUIC is configured to use a RequestDispatcherNutDao when the server starts (warm up) or when
 * a polling operation is going to be performed (polling interval activated), a mocked request is created. It is not
 * mandatory for a servlet container work with a mocked request, so using the RequestDispatcherNutDao may fail in that case.
 * </p>
 *
 * <p>
 * Here is the state of the different servlet containers not supporting polling or warm up with RequestDispatcherNutDao:
 * </p>
 * <ul>
 *     <li>Undertow does not support mocked {@link HttpServletRequest} until undertow 1.2</li>
 *     <li>Jetty as removed support for mocked {@link HttpServletRequest} starting jetty 9.3</li>
 *     <li>Tomcat supports mocked {@link HttpServletRequest}</li>
 * </ul>
 *
 * @author Guillaume DROUET
 * @version 1.0
 * @since 0.5.0
 */
@NutDaoService
public class RequestDispatcherNutDao extends AbstractNutDao implements ServletContextHandler {

    /**
     * Warning message for mocked request usage.
     */
    private static final String WARN_MOCK_REQUEST_MESSAGE =
            String.format("Going to use a %s with a mocked %s because the context '{}' does not wrap a real one. "
                    + "Supporting it is not required by specs and could not be supported by the servlet container! "
                    + "Check the documentation for more information.",
                    RequestDispatcher.class.getName(), HttpServletRequest.class.getName());

    /**
     * The servlet context providing the request dispatcher.
     */
    private ServletContext servletContext;

    /**
     * The logger.
     */
    private final Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * <p>
     * Builds a new instance.
     * </p>
     *
     *
     * @param base the directory where we have to look up
     * @param basePathAsSysProp {@code true} if the base path is a system property
     * @param pollingSeconds the interval for polling operations in seconds (-1 to deactivate)
     * @param proxies the proxies URIs in front of the nut
     * @param computeVersionAsynchronously (@code true} if version number can be computed asynchronously, {@code false} otherwise
     */
    @ConfigConstructor
    public RequestDispatcherNutDao(
            @StringConfigParam(defaultValue = "/", propertyKey = ApplicationConfig.BASE_PATH) final String base,
            @BooleanConfigParam(defaultValue = false, propertyKey = ApplicationConfig.BASE_PATH_AS_SYS_PROP) final Boolean basePathAsSysProp,
            @ObjectConfigParam(defaultValue = "", propertyKey = ApplicationConfig.PROXY_URIS, setter = ProxyUrisPropertySetter.class) final String[] proxies,
            @IntegerConfigParam(defaultValue = -1, propertyKey = ApplicationConfig.POLLING_INTERVAL) final int pollingSeconds,
            @BooleanConfigParam(defaultValue = true, propertyKey = ApplicationConfig.COMPUTE_VERSION_ASYNCHRONOUSLY) final Boolean computeVersionAsynchronously) {
        // path can correspond dynamic resources (JSP, servlet) so this DAO can support only content-based version number
        super(base, basePathAsSysProp, proxies, pollingSeconds, true, computeVersionAsynchronously);
    }

    /**
     * <p>
     * Includes the resources at given path in the specified request and response.
     * </p>
     *
     * @param path the path
     * @param response the response
     * @param request the request
     * @throws IOException if include fails
     */
    private void include(final String path, final HttpServletRequest request, final HttpServletResponse response)
            throws IOException {
        try {
            if (servletContext == null) {
                throw new IllegalArgumentException(
                        String.format("context is null! Use setServletContext first or add %s in your descriptor file",
                                WuicServletContextListener.class.getName()));
            }

            final RequestDispatcher rd = servletContext.getRequestDispatcher(slashPath(path));

            // Wrap request and response since servlet container expects standard wrappers
            rd.include(new HttpServletRequestWrapper(request), new HttpServletResponseWrapper(response));
        } catch (ServletException se) {
            WuicException.throwStreamException(new IOException(se));
        }
    }

    /**
     * <p>
     * Includes the resources at given path in the returned wrapper.
     * </p>
     *
     * @param path the path
     * @param request the request
     * @return the wrapper
     * @throws IOException if include fails
     */
    private ByteArrayHttpServletResponseWrapper include(final String path, final HttpServletRequest request) throws IOException {
        final ByteArrayHttpServletResponseWrapper wrapper = new ByteArrayHttpServletResponseWrapper();
        include(path, request, wrapper);
        return wrapper;
    }

    /**
     * <p>
     * Makes sure the given path begins by a slash character when it's returned.
     * </p>
     *
     * @param path the path
     * @return the path beginning with a slash
     */
    private String slashPath(final String path) {
        return path.charAt(0) == '/' ? path : '/' + path;
    }

    /**
     * <p>
     * Extracts the {@link HttpServletRequest} from the given {@link ProcessContext} if it's a {@link ServletProcessContext}.
     * Otherwise, an adapter wrapping the specified path will be returned.
     * </p>
     *
     * @param path the path of the adapter
     * @param processContext the potential instance wrapping the {@link HttpServletRequest}
     * @return the resolved {@link HttpServletRequest}
     */
    private HttpServletRequest getRequest(final String path, final ProcessContext processContext) {
        final HttpServletRequest retval;
        if (processContext instanceof ServletProcessContext) {
            retval = new HttpServletRequestWrapper(ServletProcessContext.class.cast(processContext).getHttpServletRequest()) {

                /**
                 * {@inheritDoc}
                 */
                @Override
                public String getPathInfo() {
                    return path;
                }
            };
        } else {
            retval = new HttpServletRequestAdapter(slashPath(path));
            logger.warn(WARN_MOCK_REQUEST_MESSAGE, processContext);
        }

        retval.setAttribute(HtmlParserFilter.SKIP_FILTER, "");
        return retval;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setServletContext(final ServletContext sc) {
        this.servletContext = sc;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Long getLastUpdateTimestampFor(final String path) throws IOException {
        throw new UnsupportedOperationException("This DAO can't provide the last timestamp");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected List<String> listNutsPaths(final String pattern) throws IOException {
        return Arrays.asList(pattern);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Nut accessFor(final String realPath, final NutType type, final ProcessContext processContext) throws IOException {
        logger.info("Building {} in process context {}", getClass().getSimpleName(), processContext);
        return new RequestDispatcherNut(realPath, type, getVersionNumber(realPath, processContext), processContext);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public InputStream newInputStream(final String path, final ProcessContext processContext) throws IOException {
        return new ByteArrayInputStream(include(path, getRequest(path, processContext)).toByteArray());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Boolean exists(final String path, final ProcessContext processContext) throws IOException {
        return include(path, getRequest(path, processContext)).toByteArray().length > 0;
    }

    /**
     * <p>
     * This {@link Nut} relies on {@link javax.servlet.RequestDispatcher} to retrieve a stream.
     * </p>
     *
     * @author Guillaume DROUET
     * @version 1.0
     * @since 0.5.0
     */
    private class RequestDispatcherNut extends AbstractNut {

        /**
         * The process context.
         */
        private final ProcessContext processContext;

        /**
         * <p>
         * Builds a new instance.
         * </p>
         *
         * @param name the name
         * @param ft the type
         * @param v the version number
         * @param pc the process context
         */
        protected RequestDispatcherNut(final String name,
                                       final NutType ft,
                                       final Future<Long> v,
                                       final ProcessContext pc) {
            super(name, ft, v);
            processContext = pc;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public InputStream openStream() throws IOException {
            return newInputStream(getInitialName(), processContext);
        }
    }
}
