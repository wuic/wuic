/*
 * "Copyright (c) 2016   Capgemini Technology Services (hereinafter "Capgemini")
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
import com.github.wuic.nut.ConvertibleNut;
import com.github.wuic.nut.HeapListener;
import com.github.wuic.nut.NutsHeap;
import com.github.wuic.nut.ByteArrayNut;
import com.github.wuic.nut.dao.servlet.RequestDispatcherNutDao;
import com.github.wuic.util.IOUtils;
import com.github.wuic.util.NutUtils;
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
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

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
 * WUIC considers that a filtered content is static by default and could be cached as any other resource. This will be
 * the cache for many applications that does not use server-side HTML generation. However, if the content is dynamic,
 * e.g it is not the same for two different users, WUIC should not cache it to serve an up-to-date response body. The
 * filter detects a dynamic content when it has been generated with a tag officially provided by WUIC (like taglib for
 * JSP or processor for Thymeleaf). In case of a dynamic content generated otherwise, you can use the init-param
 * {@link #FORCE_DYNAMIC_CONTENT} to tell WUIC to consider the content always dynamic.
 * </p>
 *
 * <p>
 * This filter supports server-hint mode. Server-hint is enabled by default. It associates to all resources that should
 * be loaded as soon as possible by the HTML page a "Link" header with "preload" rel value. Other resources are
 * associated to a "prefetch" rel value to tell the browser to download them with a low priority. Note that proxy like
 * "nghttpx" can use this header to push the resource over HTTP/2.
 * </p>
 *
 * <p>
 * Server-push is also supported. It relies on a {@link PushService} implementation discovered in the classpath.
 * It will be enabled by default if an implementation is found by the {@link ServiceLoader}. You can disable the
 * server-push with the {@link #DISABLE_SERVER_PUSH} init-param.
 * </p>
 *
 * @author Guillaume DROUET
 * @version 1.0
 * @since 0.4.4
 */
public class HtmlParserFilter extends ContextBuilderConfigurator implements Filter {

    /**
     * Property that tells the filter to use or not HTTP2 server push. Server push is enabled by default.
     */
    public static final String DISABLE_SERVER_PUSH = "c.g.wuic.filter.disableServerPush";

    /**
     * An init parameter that tells the filter how to consider the filtered content dynamic or not.
     */
    public static final String FORCE_DYNAMIC_CONTENT = "c.g.wuic.forceDynamicContent";

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
     * Workflow IDs and nuts generated by this filter.
     */
    private final Map<String, ByteArrayNut> filterDataMap;

    /**
     * The WUIC facade.
     */
    private WuicFacade wuicFacade;

    /**
     * The root nut DAO builder ID;
     */
    private String rootNuDaoBuilderId;

    /**
     * HTTP/2 server push support. Default is server-hint.
     */
    private PushService pushService;

    /**
     * Particular setting that specifies how WUIC should check if filtered content is dynamic or not.
     */
    private boolean forceDynamicContent;

    /**
     * <p>
     * Builds a new instance with a specific {@link WuicFacade} and a root {@link com.github.wuic.nut.dao.NutDao} builder.
     * </p>
     *
     * @param wuicFacade the WUIC facade
     * @param rootNuDaoBuilderId the root nut DAO builder ID
     */
    public HtmlParserFilter(final WuicFacade wuicFacade, final String rootNuDaoBuilderId) {
        this.filterDataMap = new HashMap<String, ByteArrayNut>();
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

            configureServerPush(filterConfig);
            forceDynamicContent = "true".equals(filterConfig.getInitParameter(FORCE_DYNAMIC_CONTENT));
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

        request.setAttribute(HtmlParserFilter.class.getName(), Boolean.TRUE);
        final HttpServletResponse httpServletResponse = HttpServletResponse.class.cast(response);
        final ByteArrayHttpServletResponseWrapper wrapper = new ByteArrayHttpServletResponseWrapper(httpServletResponse);

        chain.doFilter(request, new HttpServletResponseWrapper(wrapper));
        final byte[] bytes = wrapper.toByteArray();

        // There is some content to parse
        if (bytes.length > 0) {
            try {
                final HttpServletRequest httpRequest = HttpServletRequest.class.cast(request);
                final String workflowId = "W" + StringUtils.toHexString(IOUtils.digest(extractWorkflowId(httpRequest)));
                final String path = buildPath(httpRequest, httpServletResponse);
                updateWorkflow(httpRequest, bytes, workflowId, path);

                final List<ConvertibleNut> nuts = wuicFacade.runWorkflow(workflowId, new ServletProcessContext(httpRequest));
                logger.info("Finding nut {} for run workflow {}", path, workflowId);
                final ConvertibleNut htmlNut = NutUtils.findByName(nuts, path);

                if (htmlNut == null) {
                    WuicException.throwBadStateException(new IllegalStateException("The filtered page has not been found in parsed result."));
                }

                final HttpServletResponse httpResponse = HttpServletResponse.class.cast(response);
                final UrlProvider provider = getUrlProvider(httpRequest).create(workflowId);
                final Map<String, ConvertibleNut> collectedNut = collectReferenceNut(provider, htmlNut);

                if (pushService != null) {
                    pushService.push(httpRequest, httpResponse, collectedNut.keySet());
                } else {
                    hint(collectedNut, httpResponse);
                }

                HttpUtil.INSTANCE.write(htmlNut, httpResponse, false);
            } catch (WuicException we) {
                logger.error("Unable to parse HTML", we);
                response.getOutputStream().print(new String(bytes));
            }
        }
    }

    /**
     * <p>
     * Initiates server-hint.
     * </p>
     *
     * @param collectedNut nuts to hint
     * @param httpResponse the response where headers will be added
     */
    private void hint(final Map<String, ConvertibleNut> collectedNut, final HttpServletResponse httpResponse) {
        // Adds to the given response all referenced nuts URLs associated to the "Link" header.
        for (final Map.Entry<String, ConvertibleNut> entry : collectedNut.entrySet()) {
            final String strategy = entry.getValue().isSubResource() ? "preload" : "prefetch";
            final String as = entry.getValue().getNutType().getHintInfo();
            httpResponse.addHeader("Link",
                    String.format("<%s>; rel=%s%s", entry.getKey(), strategy, as == null ? "" : "; as=".concat(as)));
        }
    }

    /**
     * <p>
     * Updates the workflow for the given ID or creates it if it does not exists.
     * </p>
     *
     * @param httpRequest the request
     * @param bytes the bytes
     * @param workflowId the workflow ID
     * @param path the filtered nut path
     * @throws IOException if an I/O error occurs
     * @throws WuicException if context construction fails
     */
    private void updateWorkflow(final HttpServletRequest httpRequest,
                                final byte[] bytes,
                                final String workflowId,
                                final String path)
            throws IOException, WuicException {
        logger.info("Filtering content associated to workflow {}", workflowId);
        Boolean exists;

        synchronized (filterDataMap) {
            exists = filterDataMap.containsKey(workflowId);
        }

        if (!exists) {
            final ByteArrayNut nut = configureBuilder(httpRequest, path, contextBuilder, workflowId, bytes);

            synchronized (filterDataMap) {
                filterDataMap.put(workflowId, nut);
            }
        } else {
            synchronized (filterDataMap) {
                final ByteArrayNut nut = filterDataMap.get(workflowId);

                if (nut.isDynamic()) {
                    nut.setByteArray(bytes);
                }
            }
        }
    }

    /**
     * <p>
     * Configures the server push support retarding the {@link #DISABLE_SERVER_PUSH} setting.
     * </p>
     *
     * @param filterConfig the filter config instance
     */
    private void configureServerPush(final FilterConfig filterConfig) {
        final String sp = filterConfig.getInitParameter(DISABLE_SERVER_PUSH);

        if (sp == null || "true".equals(sp)) {
            logger.info("HTTP/2 server push is enabled. Discovering server push support in the classpath...");

            final ServiceLoader<PushService> serviceLoader = ServiceLoader.load(PushService.class);

            for (final PushService ps : serviceLoader) {
                if (pushService == null) {
                    pushService = ps;
                    logger.info("Server push support '{}' has been installed.", pushService);
                } else {
                    logger.warn("Duplicate server push support: '{}' has been found but '{}' is already installed."
                            + " This support will be ignored.", ps.getClass().getName(), pushService.getClass().getName());
                }
            }

            if (pushService == null) {
                logger.info("No HTTP/2 server push support found! No resource will be pushed.");
            }
        } else {
            logger.info("HTTP/2 server push is disabled, no resource will be pushed.");
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
     * @return a map of all collected URLs associated to the nut
     */
    private Map<String, ConvertibleNut> collectReferenceNut(final UrlProvider urlProvider, final ConvertibleNut nut) {
        if (nut.getReferencedNuts() != null && !nut.getReferencedNuts().isEmpty()) {
            final Map<String, ConvertibleNut> retval = new HashMap<String, ConvertibleNut>();

            for (final ConvertibleNut ref : nut.getReferencedNuts()) {
                retval.put('/' + urlProvider.getUrl(ref), ref);
                retval.putAll(collectReferenceNut(urlProvider, ref));
            }

            return retval;
        } else {
            return Collections.emptyMap();
        }
    }

    /**
     * <p>
     * Builds the path of the filtered nut from the servlet path for cache purpose.
     * </p>
     *
     * @param request the request
     * @param response the response
     * @return the nut path
     */
    private String buildPath(final HttpServletRequest request, final HttpServletResponse response) {
        final StringBuilder workflowBuilder = new StringBuilder();
        workflowBuilder.append(request.getServletPath().substring(1));

        final NutType nutType = NutType.getNutTypeForMimeType(response.getContentType());

        // Check that key ends with valid extension
        if (nutType == null) {
            logger.warn(String.format("%s is not a supported mime type. URI must ends with a supported extension.", response.getContentType()));
        } else {
            for (final String ext : nutType.getExtensions()) {
                final int index = workflowBuilder.lastIndexOf(ext);

                // Good extension already set
                if (workflowBuilder.length() - ext.length() == index) {
                    return workflowBuilder.toString();
                }
            }

            // No valid extension set, force one
            workflowBuilder.append(nutType.getExtensions()[0]);
        }

        return workflowBuilder.toString();
    }

    /**
     * <p>
     * Indicates if the content returned for the given request is dynamic or not according to the filter configuration
     * and the request state.
     * </p>
     *
     * @param request the request
     * @return {@code true} if the response content is dynamic, {@code false} otherwise
     */
    private boolean isDynamic(final HttpServletRequest request) {
        return forceDynamicContent || request.getAttribute(FORCE_DYNAMIC_CONTENT) != null;
    }

    /**
     * <p>
     * Extracts the workflow ID from the HTTP request.
     * </p>
     *
     * <p>
     * The method can return any array of byte array containing all the data that represent an unique ID for the filtered page.
     * By default, {@link javax.servlet.http.HttpServletRequest#getServletPath()} is used but if many pages serve different
     * statics and share the save servlet path, this method can be overridden to indicate more data, for instance the parameter
     * values.
     * </p>
     *
     * @param httpRequest the HTTP request associated to the returned workflow ID
     * @return the ID
     */
    protected byte[][] extractWorkflowId(final HttpServletRequest httpRequest) {
        return new byte[][] { httpRequest.getServletPath().getBytes(), };
    }

    /**
     * <p>
     * Configures the workflow corresponding to the given ID in the specified context builder.
     * </p>
     *
     * @param request the request
     * @param path the filtered nut path
     * @param contextBuilder the builder
     * @param workflowId the workflow ID
     * @return the created nut
     * @throws WuicException if context can't be refreshed
     * @throws IOException if any I/O error occurs
     */
    protected ByteArrayNut configureBuilder(final HttpServletRequest request,
                                            final String path,
                                            final ContextBuilder contextBuilder,
                                            final String workflowId,
                                            final byte[] content)
            throws IOException, WuicException {
        final Long versionNumber = ByteBuffer.wrap(IOUtils.digest(content)).getLong();
        final ByteArrayNut retval = new ByteArrayNut(content, path, NutType.HTML, versionNumber, isDynamic(request));

        // The workflow could already exists
        // It could have been initialized by this method or somewhere else
        // TODO: currently only the filtered nut has a chance to be dynamic only if the workflow is created from this method
        if (!wuicFacade.workflowIds().contains(workflowId)) {
            try {
                createContextNutDaoBuilder(workflowId, contextBuilder.tag(getClass()), rootNuDaoBuilderId)
                    .proxyPathForNut(path, retval)
                    .toContext()
                    .disposableHeap(workflowId, workflowId, new String[]{path}, new HeapListener() {
                        @Override
                        public void nutUpdated(final NutsHeap heap) {
                            synchronized (filterDataMap) {
                                filterDataMap.remove(workflowId);
                            }
                        }
                    });
            } finally {
                contextBuilder.releaseTag();
            }

            contextBuilder.build();
            wuicFacade.refreshContext();
        }

        return retval;
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