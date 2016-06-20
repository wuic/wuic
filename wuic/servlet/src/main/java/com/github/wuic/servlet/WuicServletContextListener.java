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

import com.github.wuic.ApplicationConfig;
import com.github.wuic.ClassPathResourceResolver;
import com.github.wuic.WuicFacade;
import com.github.wuic.WuicFacadeBuilder;
import com.github.wuic.WuicTask;
import com.github.wuic.engine.core.StaticEngine;
import com.github.wuic.exception.WuicException;
import com.github.wuic.nut.ConvertibleNut;
import com.github.wuic.nut.dao.servlet.WebappNutDaoBuilderInspector;
import com.github.wuic.servlet.jetty.PathMap;
import com.github.wuic.util.BiFunction;
import com.github.wuic.util.Consumer;
import com.github.wuic.util.IOUtils;
import com.github.wuic.util.PropertyResolver;
import com.github.wuic.util.UrlProvider;
import com.github.wuic.util.UrlUtils;
import com.github.wuic.util.WuicScheduledThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration;
import javax.servlet.Registration;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletRegistration;
import javax.servlet.annotation.WebListener;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

/**
 * <p>
 * Servlet context listener that injects into the {@link javax.servlet.ServletContext} the {@link WuicFacade} as an
 * attribute mapped to {@link ApplicationConfig#WEB_WUIC_FACADE} name.
 * </p>
 *
 * <p>
 * The {@link ServletContext} is also used when retrieving classpath resources if necessary
 * (see {@link ServletContextClasspathResourceResolver}).
 * </p>
 *
 * @author Guillaume DROUET
 * @since 0.4.1
 */
@WebListener
public class WuicServletContextListener implements ServletContextListener {

    /**
     * Message logged when a custom implementation could not be applied.
     */
    private static final String CUSTOM_PARAM_PROVIDER_MESSAGE = "Cannot apply custom parameter provider";

    /**
     * Logger.
     */
    private final Logger log = LoggerFactory.getLogger(getClass());

    /**
     * <p>
     * Gets the {@link WuicFacadeBuilder} injected into the given context.
     * </p>
     *
     * @param servletContext the context that must contains the facade
     * @return the facade
     */
    public static WuicFacadeBuilder getWuicFacadeBuilder(final ServletContext servletContext) {
        final Object builder = servletContext.getAttribute(ApplicationConfig.WEB_WUIC_FACADE_BUILDER);

        if (builder == null) {
            final String message = String.format("WuicFacadeBuilder is null, seems the %s was not initialized successfully.", WuicServletContextListener.class.getName());
            WuicException.throwBadStateException(new IllegalArgumentException(message));
        }

        return WuicFacadeBuilder.class.cast(builder);
    }

    /**
     * <p>
     * Gets the {@link WuicFacade} injected into the given context.
     * </p>
     *
     * @param servletContext the context that must contains the facade
     * @return the facade
     */
    public static WuicFacade getWuicFacade(final ServletContext servletContext) {
        final Object facade = servletContext.getAttribute(ApplicationConfig.WEB_WUIC_FACADE);

        if (facade == null) {
            try {
                final WuicFacade wuicFacade = getWuicFacadeBuilder(servletContext).build();
                servletContext.setAttribute(ApplicationConfig.WEB_WUIC_FACADE, wuicFacade);
                return wuicFacade;
            } catch (WuicException we) {
                WuicException.throwBadStateException(new IllegalArgumentException("Unable to initialize WuicServletContextListener", we));
            }
        }

        return WuicFacade.class.cast(facade);
    }

    /**
     * <p>
     * Completes some configurations from information generated at build time thanks to the {@link WuicTask}.
     * </p>
     *
     * @param buildInfoUrl the detected file information location
     * @param builder the builder to configure
     * @param classpathResourceResolver the {@link com.github.wuic.ClassPathResourceResolver} based on the {@link ServletContext}
     */
    private void installBuildInfo(final URL buildInfoUrl,
                                  final WuicFacadeBuilder builder,
                                  final ServletContextClasspathResourceResolver classpathResourceResolver) {
        final Properties properties = new Properties();
        InputStream is = null;

        try {
            // Load properties
            is = buildInfoUrl.openStream();
            properties.load(is);

            // Configure context path defined at build time
            builder.contextPath(properties.getProperty(ApplicationConfig.WUIC_SERVLET_CONTEXT_PARAM));

            // Install a filter for each URLs corresponding to a nut owned by a workflow
            final String[] workflowList = properties.getProperty("workflowList").split("\\t");
            final ServletContext sc = classpathResourceResolver.getServletContext();
            final FilterRegistration filterRegistration = sc.addFilter("staticWorkflowFilter", ResponseOptimizerFilter.class);

            // Already registered elsewhere
            if (filterRegistration == null) {
                return;
            }

            // Register the mapping for each URL
            for (final String workflow : workflowList) {
                final UrlProvider urlProvider = UrlUtils.urlProviderFactory().create(workflow);
                final List<ConvertibleNut> result = StaticEngine.getNuts(classpathResourceResolver, workflow);

                // Recursive call on referenced nuts
                addFilterMapping(result, filterRegistration, urlProvider);
            }
        } catch (IOException ioe) {
            WuicException.throwBadStateException(new IllegalStateException(ioe));
        } catch (WuicException we) {
            WuicException.throwBadStateException(new IllegalStateException(we));
        } finally {
            IOUtils.close(is);
        }
    }

    /**
     * <p>
     * Adds a mapping in the given filter for each nut.
     * </p>
     *
     * @param nuts the nuts
     * @param registration the registration
     * @param provider the provider computing nut URL
     */
    public void addFilterMapping(final List<ConvertibleNut> nuts, final FilterRegistration registration, final UrlProvider provider) {

        // Not referenced nuts
        if (nuts != null) {
            for (final ConvertibleNut nut : nuts) {
                registration.addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST), true, '/' + provider.getUrl(nut));

                // Call recursively on referenced nuts
                addFilterMapping(nut.getReferencedNuts(), registration, provider);
            }
        }
    }

    /**
     * <p>
     * Gets the {@link BiFunction} providing parameters.
     * </p>
     *
     * @param servletContext the context that must contains the function
     * @return the function
     */
    @SuppressWarnings("unchecked")
    public static PropertyResolver getParamProvider(final ServletContext servletContext) {
        final Object fct = servletContext.getAttribute(ApplicationConfig.INIT_PARAM_FUNCTION);

        if (fct == null) {
            final String message = String.format("PropertyResolver is null, seems the %s was not initialized successfully.", WuicServletContextListener.class.getName());
            WuicException.throwBadStateException(new IllegalArgumentException(message));
        }

        return (PropertyResolver) fct;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void contextInitialized(final ServletContextEvent sce) {
        final ServletContext sc = sce.getServletContext();
        detectFilter(sc);

        final String paramClass = sc.getInitParameter(ApplicationConfig.INIT_PARAM_FUNCTION);
        PropertyResolver paramProvider;

        // No specific provider, use default
        if (paramClass == null) {
            paramProvider = paramProvider(sc);
        } else {
            try {
                paramProvider = newParamClassInstance(paramClass, sc);
            } catch (Exception ex) {
                throw new IllegalStateException(CUSTOM_PARAM_PROVIDER_MESSAGE, ex);
            }
        }

        paramProvider = new PropertiesWrapper(sce.getServletContext(), paramProvider);
        sc.setAttribute(ApplicationConfig.INIT_PARAM_FUNCTION, paramProvider);

        final ServletContextClasspathResourceResolver classpathResourceResolver = new ServletContextClasspathResourceResolver(sc);
        final WuicFacadeBuilder builder = new WuicFacadeBuilder(paramProvider)
                .objectBuilderInspector(new WebappNutDaoBuilderInspector(sc))
                .classpathResourceResolver(classpathResourceResolver);

        UrlUtils.detectInClassesLocation(classpathResourceResolver, new Consumer<URL>() {

            /**
             * {@inheritDoc}
             */
            @Override
            public void apply(final URL consumed) {
                installBuildInfo(consumed, builder, classpathResourceResolver);
            }
        }, WuicTask.BUILD_INFO_FILE);

        sc.setAttribute(ApplicationConfig.WEB_WUIC_FACADE_BUILDER, builder);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void contextDestroyed(final ServletContextEvent sce) {
        WuicScheduledThreadPool.INSTANCE.shutdown();
    }

    /**
     * <p>
     * Detects if the {@link HtmlParserFilter} is installed. In that case, the servlet {@link ServletRegistration registrations}
     * will be scanned to find which ones serves the resources filtered by the filter. This servlet will be wrapped in
     * a servlet with the 'async-supported' flag turned to on. In that case, the filter will be able to create asynchronous
     * tasks since all chained components will support it.
     * </p>
     *
     * @param servletContext the servlet context
     */
    private void detectFilter(final ServletContext servletContext) {
        FilterRegistration filterRegistration = null;

        for (final FilterRegistration registration : servletContext.getFilterRegistrations().values()) {
            if (registration.getClassName().equals(HtmlParserFilter.class.getName())) {
                filterRegistration = registration;
                break;
            }
        }

        if (filterRegistration != null) {
            setAsyncSupported(servletContext, filterRegistration);
        }
    }

    /**
     * <p>
     * Takes the given filter and tries to detect if async-supported flag is {@code true}.
     * If the flag is set to {@code true}, we try to set the same state for filtered servlet.
     * </p>
     *
     * @param servletContext the context providing registrations
     * @param filterRegistration the filter
     */
    private void setAsyncSupported(final ServletContext servletContext, final FilterRegistration filterRegistration) {
        log.info("Collecting servlet mapping do detect which ones are filtered by {}", filterRegistration.getClassName());
        final PathMap<ServletRegistration> pathMap = new PathMap<ServletRegistration>();

        // We use the PathMap implemented by jetty
        for (final ServletRegistration registration : servletContext.getServletRegistrations().values()) {
            for (final String mapping : registration.getMappings()) {
                pathMap.put(mapping, registration);
            }
        }

        final Set<ServletRegistration> servletRegistrations = new HashSet<ServletRegistration>();

        // Collect the servlet targeted by the filter mapping
        for (final String mapping : filterRegistration.getUrlPatternMappings()) {
            servletRegistrations.add(pathMap.match(mapping));
        }

        // Collect the servlet explicitly mapped
        for (final String servletName : filterRegistration.getServletNameMappings()) {
            servletRegistrations.add(servletContext.getServletRegistration(servletName));
        }

        // Now try to turn on async-supported flag
        for (final ServletRegistration registration : servletRegistrations) {
            if (registration instanceof Registration.Dynamic) {
                log.info("{}");
                Registration.Dynamic.class.cast(registration).setAsyncSupported(true);
            } else {
                log.warn("ServletRegistration {} should be an instance of Dynamic.", registration.getClassName());
                log.warn("If {} has the async-supported flag turned on, make sure the targeted servlet is in the same state.",
                        filterRegistration.getClassName());
            }
        }
    }

    /**
     * <p>
     * Builds a {@link BiFunction} with the given class. The class BiFunction implementation must be parameterized only
     * with java.lang.String types. If it does not expose a default constructor, it must provide a constructor expecting
     * a ServletContext.
     * </p>
     *
     * @param paramClass the class
     * @param sc         the context
     * @return the function
     * @throws Exception if class can't be instantiated
     */
    @SuppressWarnings("unchecked")
    private PropertyResolver newParamClassInstance(final String paramClass, final ServletContext sc)
            throws Exception {
        final Class<?> clazz = Class.forName(paramClass);

        Constructor<?> defaultConstructor = null;
        Constructor<?> scConstructor = null;

        // Lookup constructor
        for (final Constructor<?> c : clazz.getDeclaredConstructors()) {
            if (c.getParameterTypes().length == 0) {
                defaultConstructor = c;
            } else if (c.getParameterTypes().length == 1 && c.getParameterTypes()[0].equals(ServletContext.class)) {
                scConstructor = c;
            }
        }

        // Check if constructor exists
        if (scConstructor != null) {
            return (PropertyResolver) scConstructor.newInstance(sc);
        } else if (defaultConstructor != null) {
            return (PropertyResolver) defaultConstructor.newInstance();
        } else {
            throw new IllegalStateException(
                    String.format("'%s' provide at least a default constructor or a constructor expecting one parameter of type '%s'",
                            paramClass, ServletContext.class.getName()));
        }
    }

    /**
     * <p>
     * Returns the function the {@link WuicFacadeBuilder} will use to retrieve properties.
     * </p>
     *
     * @param sc the servlet context
     * @return the function to apply
     */
    protected PropertyResolver paramProvider(final ServletContext sc) {
        return new InitParamProperties(sc);
    }

    /**
     * <p>
     * A {@link com.github.wuic.ClassPathResourceResolver} that checks thanks to the {@link Class} class if any resource is in the
     * classpath. If the result is {@code null}, it fallback to a wrapped {@link ServletContext} where path is read
     * under "WEB-INF/classes" folder.
     * </p>
     *
     * @author Guillaume DROUET
     * @since 0.5.2
     */
    public static final class ServletContextClasspathResourceResolver implements ClassPathResourceResolver {

        /**
         * The servlet context.
         */
        private ServletContext servletContext;

        /**
         * <p>
         * Builds a new instance.
         * </p>
         *
         * @param servletContext the servlet context
         */
        public ServletContextClasspathResourceResolver(final ServletContext servletContext) {
            this.servletContext = servletContext;
        }

        /**
         * <p>
         * Gets the servlet context
         * </p>
         *
         * @return the servlet context
         */
        public ServletContext getServletContext() {
            return servletContext;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public URL getResource(final String resourcePath) throws MalformedURLException {
            final URL retval = getClass().getResource("/" + resourcePath);

            return retval == null ? servletContext.getResource("/WEB-INF/classes/" + resourcePath) : retval;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public InputStream getResourceAsStream(final String resourcePath) {
            final InputStream retval = getClass().getResourceAsStream("/" + resourcePath);

            return retval == null ? servletContext.getResourceAsStream("/WEB-INF/classes/" + resourcePath) : retval;
        }
    }

    /**
     * <p>
     * A properties that delegate call to a wrapped instance and modify the value associated to
     * {@link ApplicationConfig#WUIC_SERVLET_CONTEXT_PARAM} to make sure it starts with the servlet
     * context's path. If the value is {@code null}, then it tries first to retrieve a mapping from
     * any installed {@link WuicServlet}. If no mapping can be retrieved, then the default value is applied.
     * </p>
     *
     * @author Guillaume DROUET
     * @since 0.5.2
     */
    public final class PropertiesWrapper implements PropertyResolver {

        /**
         * Wraps the function.
         */
        private final PropertyResolver wrap;

        /**
         * Servlet context.
         */
        private final ServletContext servletContext;

        /**
         * <p>
         * Builds a new instance.
         * </p>
         *
         * @param sc the servlet context
         * @param w  the function
         */
        private PropertiesWrapper(final ServletContext sc, PropertyResolver w) {
            wrap = w;
            servletContext = sc;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String resolveProperty(final String key) {
            final String wrapResult = wrap.resolveProperty(key);

            if (ApplicationConfig.WUIC_SERVLET_CONTEXT_PARAM.equals(key)) {
                final ServletRegistration r = WuicServlet.findServletRegistration(servletContext);
                final String retval;

                // There is a registration for WUIC servlet, try extract the value from mapping instead of using the context param
                if (r != null && r.getMappings() != null && !r.getMappings().isEmpty()) {
                    final String mapping = r.getMappings().iterator().next();
                    final int star = mapping.indexOf('*');

                    // We expect a star in the mapping
                    if (star != -1) {
                        // The user has defined a context-param explicitly, it will be ignored and replace by servlet mapping
                        if (wrapResult != null) {
                            WuicServletContextListener.this.log.warn(
                                    "WuicServlet is installed and its mapping will be used to resolve '{}' property. 'context-param' configured with value '{}' will be ignored.",
                                    ApplicationConfig.WUIC_SERVLET_CONTEXT_PARAM,
                                    wrapResult,
                                    new IllegalStateException());
                        }

                        retval = mapping.substring(0, star);
                    } else {
                        // The user does not specified any star in the mapping, fallback to initial result
                        WuicServletContextListener.this.log.warn("WuicServlet mapping '{}' does not contain any '*' character. Using '{}' as property for '{}'.",
                                mapping,
                                wrapResult,
                                ApplicationConfig.WUIC_SERVLET_CONTEXT_PARAM,
                                new IllegalStateException());

                        retval = wrapResult;
                    }
                } else {
                    retval = wrapResult;
                }

                return retval == null ? servletContext.getContextPath() : IOUtils.mergePath(servletContext.getContextPath(), retval);
            } else {
                return wrapResult;
            }
        }
    }

    /**
     * <p>
     * A class that retrieves properties from init-param configured inside a servlet context.
     * </p>
     *
     * @author Guillaume DROUET
     * @since 0.5.0
     */
    public static final class InitParamProperties implements PropertyResolver {

        /**
         * The servlet context.
         */
        private ServletContext servletContext;

        /**
         * <p>
         * Builds a new instance.
         * </p>
         *
         * @param sc the servlet context
         */
        public InitParamProperties(final ServletContext sc) {
            servletContext = sc;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String resolveProperty(final String key) {
            return servletContext.getInitParameter(key);
        }
    }
}
