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

import com.github.wuic.ContextBuilder;
import com.github.wuic.ContextBuilderConfigurator;
import com.github.wuic.NutType;
import com.github.wuic.WuicFacade;
import com.github.wuic.config.ObjectBuilder;
import com.github.wuic.exception.BuilderPropertyNotSupportedException;
import com.github.wuic.exception.WuicException;
import com.github.wuic.exception.wrapper.StreamException;
import com.github.wuic.jee.WuicServletContextListener;
import com.github.wuic.nut.ConvertibleNut;
import com.github.wuic.nut.dao.NutDao;
import com.github.wuic.nut.ByteArrayNut;
import com.github.wuic.nut.dao.core.ProxyNutDao;
import com.github.wuic.nut.dao.servlet.RequestDispatcherNutDao;
import com.github.wuic.util.IOUtils;
import com.github.wuic.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * This filter uses the {@link com.github.wuic.WuicFacade} to configure the related {@link ContextBuilder} with workflow
 * built on the fly when an HTML page is filtered. The created workflow uses default engines.
 * </p>
 *
 * <p>
 * This filters uses an internal {@link NutDao} to retrieve referenced nuts when parsing HTML. By default, the DAO built from
 * a {@link com.github.wuic.nut.dao.servlet.RequestDispatcherNutDao}. DAO is configured like this for consistency reason because
 * the version number must computed from content when scripts are declared inside tag. User can takes control over {@link NutDao}
 * creation by extending this class and overriding the {@link com.github.wuic.servlet.HtmlParserFilter#createDao()} method.
 * </p>
 *
 * @author Guillaume DROUET
 * @version 1.0
 * @since 0.4.4
 */
public class HtmlParserFilter extends ContextBuilderConfigurator implements Filter {

    /**
     * The logger.
     */
    private final Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * The {@link NutDao} to use.
     */
    private NutDao nutDao;

    /**
     * {@link ContextBuilder} to use.
     */
    private ContextBuilder contextBuilder;

    /**
     * Workflow IDs generated by this filter.
     */
    private final Map<String, String> workflowIds;

    /**
     * Indicates if the context is virtual.
     */
    private boolean virtualContextPath;

    /**
     * The WUIC facade.
     */
    private WuicFacade wuicFacade;

    /**
     * <p>
     * Builds a new instance.
     * </p>
     */
    public HtmlParserFilter() {
        workflowIds = new HashMap<String, String>();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void init(final FilterConfig filterConfig) throws ServletException {
        try {
            wuicFacade = WuicServletContextListener.getWuicFacade(filterConfig.getServletContext());
            wuicFacade.configure(this);
            nutDao = createDao();
            virtualContextPath = !filterConfig.getServletContext().getContextPath().isEmpty();
        } catch (BuilderPropertyNotSupportedException bpnse) {
            throw new ServletException(bpnse);
        } catch (WuicException we) {
            throw new ServletException(we);
        }
    }

    /**
     * <p>
     * Creates a new {@link NutDao} from the {@link com.github.wuic.nut.dao.jee.WebappNutDao} builder.
     * </p>
     *
     * @return the nut DAO
     * @throws BuilderPropertyNotSupportedException if {@link com.github.wuic.ApplicationConfig#CONTENT_BASED_VERSION_NUMBER} property not supported
     */
    protected NutDao createDao() throws BuilderPropertyNotSupportedException {
        final NutDao def = contextBuilder.nutDao("wuicDefault" + RequestDispatcherNutDao.class.getSimpleName() + "Builder");

        if (def == null) {
            final ObjectBuilder<NutDao> b = wuicFacade.newNutDaoBuilder(RequestDispatcherNutDao.class.getSimpleName() + "Builder");
            return NutDao.class.cast(b.build());
        } else {
            return def;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain)
            throws IOException, ServletException {
        final HttpServletResponse httpServletResponse = HttpServletResponse.class.cast(response);
        final ByteArrayHttpServletResponseWrapper wrapper = new ByteArrayHttpServletResponseWrapper(httpServletResponse);

        chain.doFilter(request, new HttpServletResponseWrapper(wrapper));
        final byte[] bytes = wrapper.toByteArray();

        // There is some content to parse
        if (bytes.length > 0) {
            try {
                final String key = buildKey(HttpServletRequest.class.cast(request), httpServletResponse);
                final String workflowId;
                Boolean exists;

                synchronized (workflowIds) {
                    exists = workflowIds.containsKey(key);

                    // Workflow not already created, compute it
                    if (!exists) {
                        workflowId = StringUtils.toHexString(IOUtils.digest(key));
                        workflowIds.put(key, workflowId);
                    } else {
                        workflowId = workflowIds.get(key);
                    }
                }

                if (!exists) {
                    configureBuilder(contextBuilder, workflowId, key, bytes);
                    contextBuilder.build();
                }

                final List<ConvertibleNut> nuts = wuicFacade.runWorkflow(workflowId);
                InputStream is = null;
                final Runnable r = HttpRequestThreadLocal.INSTANCE.canGzip(HttpServletRequest.class.cast(request));

                try {
                    HttpRequestThreadLocal.INSTANCE.write(nuts.get(0), HttpServletResponse.class.cast(response));
                } finally {
                    r.run();
                    IOUtils.close(is);
                }
            } catch (WuicException we) {
                logger.error("Unable to parse HTML", we);
                response.getOutputStream().print(new String(bytes));
            }
        }
    }

    /**
     * <p>
     * Builds the key for cache from the request URI.
     * </p>
     *
     * @param request the request
     * @param response the response
     * @return the key
     */
    private String buildKey(final HttpServletRequest request, final HttpServletResponse response) {
        final StringBuilder keyBuilder = new StringBuilder();

        // Ignore the context path if virtual
        if (virtualContextPath) {
            keyBuilder.append(request.getRequestURI().substring(1 + request.getServletContext().getContextPath().length()));
        } else {
            keyBuilder.append(request.getRequestURI().substring(1));
        }

        final NutType nutType = NutType.getNutTypeForMimeType(response.getContentType());

        // Check that key ends with valid extension
        if (nutType == null) {
            logger.warn(String.format("%s is not a supported mime type. URI must ends with a supported extension.", response.getContentType()));
        } else {
            for (final String ext : nutType.getExtensions()) {
                final int index = keyBuilder.lastIndexOf(ext);

                // Good extension already set
                if (keyBuilder.length() - ext.length() == index) {
                    return keyBuilder.toString();
                }
            }

            // No valid extension set, force one
            keyBuilder.append(nutType.getExtensions()[0]);
        }

        return keyBuilder.toString();
    }

    /**
     * <p>
     * Configures the given workflow in the specified context builder.
     * </p>
     *
     * @param contextBuilder the builder
     * @param workflowId the workflow
     * @param path the path
     * @throws StreamException if any I/O error occurs
     */
    protected void configureBuilder(final ContextBuilder contextBuilder, final String workflowId, final String path, final byte[] content)
            throws StreamException {
        try {
            final ProxyNutDao dao = new ProxyNutDao("", nutDao);
            final String name = path.endsWith("/") ? (path + NutType.HTML.getExtensions()[0]) : path;
            dao.addRule(path, new ByteArrayNut(content, name, NutType.HTML, ByteBuffer.wrap(IOUtils.digest(content)).getLong()));

            contextBuilder.tag(getClass().getName())
                    .nutDao(path, dao)
                    .heap(workflowId, path, path);
        } finally {
            contextBuilder.releaseTag();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void destroy() {
        nutDao.shutdown();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int internalConfigure(final ContextBuilder ctxBuilder) {
        contextBuilder = ctxBuilder;
        contextBuilder.nutDao(getClass().getName(), nutDao);
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
