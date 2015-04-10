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

import com.github.wuic.ProcessContext;
import com.github.wuic.context.ContextBuilder;
import com.github.wuic.context.ContextBuilderConfigurator;
import com.github.wuic.NutType;
import com.github.wuic.WuicFacade;
import com.github.wuic.exception.WuicException;
import com.github.wuic.jee.ServletProcessContext;
import com.github.wuic.jee.WuicServletContextListener;
import com.github.wuic.nut.ConvertibleNut;
import com.github.wuic.nut.HeapListener;
import com.github.wuic.nut.NutsHeap;
import com.github.wuic.nut.ByteArrayNut;
import com.github.wuic.nut.dao.servlet.RequestDispatcherNutDao;
import com.github.wuic.util.IOUtils;
import com.github.wuic.util.StringUtils;
import com.github.wuic.util.UrlProvider;
import com.github.wuic.util.UrlProviderFactory;
import com.github.wuic.util.UrlUtils;
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
import java.util.Collections;
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
 * This filters uses an internal {@link com.github.wuic.nut.dao.NutDao} to retrieve referenced nuts when parsing HTML.
 * By default, the DAO built from a {@link com.github.wuic.nut.dao.servlet.RequestDispatcherNutDao}. DAO is configured
 * like this for consistency reason because the version number must be computed from content when scripts are declared
 * inside tag. User can takes control over {@link com.github.wuic.nut.dao.NutDao} creation by extending this class and
 * overriding the {@link #createContextNutDaoBuilder(String, com.github.wuic.context.ContextBuilder, String)} method.
 * </p>
 *
 * <p>
 * This filter supports server-hint mode. Server-hint is enabled by default. It associates to all resources that should
 * be loaded as soon as possible by the HTML page a "Link" header with "subresource" rel value (supported by Chrome).
 * Other resources are associated to a "preload" rel value to tell the browser to download them with a low priority.
 * Proxy like "nghttpx" can use this header to push the resource over HTTP/2. The push will be enabled only for response
 * with "preload" rel value in the Link header, which is in conflict with chrome "subresource" design. If you want to
 * use "nghttpx" HTTP/2 push feature, set the {@link #PRE_LOAD_SUB_RESOURCES} init-param to {@code true} in order to
 * associate the "preload" rel value to sub resources and "preconnect" to others.
 * </p>
 *
 * @author Guillaume DROUET
 * @version 1.0
 * @since 0.4.4
 */
public class HtmlParserFilter extends ContextBuilderConfigurator implements Filter {

    /**
     * Adds a "preload" rel value instead of "subresource" in Link Header for the sub resources.
     */
    public static final String PRE_LOAD_SUB_RESOURCES = "c.g.wuic.preLoadSubResources";

    /**
     * If an attribute with this name is defined in the filtered request, then this filter will skip its related operation.
     */
    public static final String SKIP_FILTER = HtmlParserFilter.class.getName() + ".skip";

    /**
     * The logger.
     */
    private final Logger logger = LoggerFactory.getLogger(getClass());

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
     * The root nut DAO builder ID;
     */
    private String rootNuDaoBuilderId;

    /**
     * The "rel" value in link header for sub resources.
     */
    private String subResourceRel;

    /**
     * The "rel" value in link header for non sub resources.
     */
    private String nonSubResourceRel;

    /**
     * <p>
     * Builds a new instance with a specific {@link WuicFacade} and a root {@link com.github.wuic.nut.dao.NutDao} builder.
     * </p>
     *
     * @param wuicFacade the WUIC facade
     * @param rootNuDaoBuilderId the root nut DAO builder ID
     */
    public HtmlParserFilter(final WuicFacade wuicFacade, final String rootNuDaoBuilderId) {
        this.workflowIds = new HashMap<String, String>();
        this.wuicFacade = wuicFacade;
        this.rootNuDaoBuilderId = rootNuDaoBuilderId;
    }

    /**
     * <p>
     * Builds a new instance with a specific {@link WuicFacade} and the default {@link RequestDispatcherNutDao} builder
     * as root {@link com.github.wuic.nut.dao.NutDao} builder.
     * </p>
     *
     * @param wuicFacade the WUIC facade
     */
    public HtmlParserFilter(final WuicFacade wuicFacade) {
        this(wuicFacade, ContextBuilder.getDefaultBuilderId(RequestDispatcherNutDao.class));
    }

    /**
     * <p>
     * Builds a new instance. The internal {@link WuicFacade} will be initialized when {@link #init(javax.servlet.FilterConfig)}
     * method will be invoked. At this moment, this class will get the object from {@link WuicServletContextListener}.
     * In this case, the listener must be declared in th servlet container configuration.
     * </p>
     */
    public HtmlParserFilter() {
        this(null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void init(final FilterConfig filterConfig) throws ServletException {
        try {
            if (wuicFacade == null) {
                wuicFacade = WuicServletContextListener.getWuicFacade(filterConfig.getServletContext());
            }

            wuicFacade.configure(this);
            virtualContextPath = !filterConfig.getServletContext().getContextPath().isEmpty();

            configureServerHint(filterConfig);
        } catch (WuicException we) {
            throw new ServletException(we);
        }
    }

    /**
     * <p>
     * Creates a new {@link com.github.wuic.context.ContextBuilder.ContextNutDaoBuilder} with the given {@link ContextBuilder}.
     * </p>
     *
     * @param id the ID
     * @param contextBuilder the context builder to use
     * @param rootNutDaoBuilderId the root {@link com.github.wuic.nut.dao.NutDao} builder which should be extended by the new builder
     * @return the nut DAO
     */
    protected ContextBuilder.ContextNutDaoBuilder createContextNutDaoBuilder(final String id,
                                                                             final ContextBuilder contextBuilder,
                                                                             final String rootNutDaoBuilderId) {
        try {
            return contextBuilder.cloneContextNutDaoBuilder(id, rootNutDaoBuilderId);
        } catch (IllegalArgumentException ie) {
            logger.info("Cannot clone the builder, create a new one", ie);
            return contextBuilder.contextNutDaoBuilder(id, RequestDispatcherNutDao.class);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain)
            throws IOException, ServletException {

        // Skip
        if (request.getAttribute(SKIP_FILTER) != null) {
            chain.doFilter(request, response);
            return;
        }

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
                    } else {
                        workflowId = workflowIds.get(key);
                    }
                }

                if (!exists) {
                    configureBuilder(contextBuilder, workflowId, key, bytes);
                    contextBuilder.build();
                    wuicFacade.refreshContext();

                    synchronized (workflowIds) {
                        workflowIds.put(key, workflowId);
                    }
                }

                final HttpServletRequest httpRequest = HttpServletRequest.class.cast(request);
                final List<ConvertibleNut> nuts = wuicFacade.runWorkflow(workflowId, new ServletProcessContext(httpRequest));
                final ConvertibleNut htmlNut = nuts.get(0);
                InputStream is = null;
                final Runnable r = HttpRequestThreadLocal.INSTANCE.canGzip(httpRequest);
                final HttpServletResponse httpResponse = HttpServletResponse.class.cast(response);
                final UrlProvider provider = getUrlProvider(httpRequest).create(workflowId);
                final Map<String, Boolean> collectedNut = collectReferenceNut(provider, htmlNut);

                // Adds to the given response all referenced nuts URLs associated to the "Link" header.
                for (final Map.Entry<String, Boolean> entry : collectedNut.entrySet()) {
                    final String strategy = entry.getValue() ? subResourceRel : nonSubResourceRel;
                    httpResponse.addHeader("Link", String.format("<%s>; rel=%s", entry.getKey(), strategy));
                }

                try {
                    HttpRequestThreadLocal.INSTANCE.write(htmlNut, httpResponse);
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
     * Configures the server hint support regarding the {@link #PRE_LOAD_SUB_RESOURCES} setting.
     * </p>
     *
     * @param filterConfig the filter config instance
     */
    private void configureServerHint(final FilterConfig filterConfig) {
        final String psr = filterConfig.getInitParameter(PRE_LOAD_SUB_RESOURCES);

        if ("true".equals(psr)) {
            subResourceRel = "preload";
            nonSubResourceRel = "preconnect";
        } else {
            subResourceRel = "subresource";
            nonSubResourceRel = "preload";
        }
    }

    /**
     * <p>
     * Retrieves an optional {@link UrlProviderFactory} instance registered in request's attributes.
     * </p>
     *
     * @param httpServletRequest the request
     * @return the factory bound to the request, {@link com.github.wuic.util.UrlUtils.DefaultUrlProviderFactory} otherwise
     */
    private UrlProviderFactory getUrlProvider(final HttpServletRequest httpServletRequest) {
        final Object attribute = httpServletRequest.getAttribute(UrlProviderFactory.class.getName());
        return attribute != null ? UrlProviderFactory.class.cast(attribute) : new UrlUtils.DefaultUrlProviderFactory();
    }

    /**
     * <p>
     * Adds recursively all URLs from referenced nuts of the given nut in the returned list
     * </p>
     *
     * @param urlProvider the provider
     * @param nut the referenced nuts owner
     * @return a map of all collected URLs associated to a boolean indicating if it refers a sub resource ot not
     */
    private Map<String, Boolean> collectReferenceNut(final UrlProvider urlProvider, final ConvertibleNut nut) {
        if (nut.getReferencedNuts() != null && !nut.getReferencedNuts().isEmpty()) {
            final Map<String, Boolean> retval = new HashMap<String, Boolean>();

            for (final ConvertibleNut ref : nut.getReferencedNuts()) {
                retval.put(urlProvider.getUrl(ref), ref.isSubResource());
                retval.putAll(collectReferenceNut(urlProvider, ref));
            }

            return retval;
        } else {
            return Collections.emptyMap();
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
            final String key = request.getRequestURI().substring(request.getServletContext().getContextPath().length());
            keyBuilder.append(key.startsWith("/") ? key.substring(1) : key);
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
     * @throws IOException if any I/O error occurs
     */
    protected void configureBuilder(final ContextBuilder contextBuilder, final String workflowId, final String path, final byte[] content)
            throws IOException {
        try {
            final String name = path.endsWith("/") ? (path + NutType.HTML.getExtensions()[0]) : path;
            createContextNutDaoBuilder(path, contextBuilder.tag(getClass()), rootNuDaoBuilderId)
                    .proxyPathForNut(path, new ByteArrayNut(content, name, NutType.HTML, ByteBuffer.wrap(IOUtils.digest(content)).getLong()))
                    .toContext()
                    .disposableHeap(workflowId, path, new String[]{path}, new HeapListener() {
                        @Override
                        public void nutUpdated(final NutsHeap heap) {
                            synchronized (workflowIds) {
                                workflowIds.remove(path);
                            }
                        }
                    });
        } finally {
            contextBuilder.releaseTag();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void destroy() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int internalConfigure(final ContextBuilder ctxBuilder) {
        contextBuilder = ctxBuilder;
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
    protected Long getLastUpdateTimestampFor(final String path) throws IOException {
        return -1L;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ProcessContext getProcessContext() {
        return null;
    }
}
