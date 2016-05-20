/*
 * "Copyright (c) 2016   Capgemini Technology Services (final hereinafter "Capgemini")
 *
 * License/Terms of Use
 * Permission is hereby granted, free of charge and for the term of intellectual
 * property rights on the Software, to any person obtaining a copy of this software
 * and associated documentation files (final the "Software"), to use, copy, modify and
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
 * open source software licenses (final BSD and Apache) of which CAPGEMINI is not the
 * author or the editor. The rights granted on the said software components are
 * governed by the specific terms and conditions specified by Apache 2.0 and BSD
 * licenses."
 */


package com.github.wuic;

import com.github.wuic.config.ObjectBuilderInspector;
import com.github.wuic.context.ContextBuilder;
import com.github.wuic.context.ContextBuilderConfigurator;
import com.github.wuic.context.ContextInterceptor;
import com.github.wuic.exception.WorkflowTemplateNotFoundException;
import com.github.wuic.exception.WuicException;
import com.github.wuic.nut.HeapListener;
import com.github.wuic.nut.Nut;
import com.github.wuic.util.EnhancedPropertyResolver;
import com.github.wuic.util.IOUtils;
import com.github.wuic.util.MapPropertyResolver;
import com.github.wuic.util.PropertyResolver;
import com.github.wuic.util.SystemPropertyResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

/**
 * <p>
 * This class handles several settings in its state and is required to create a {@link WuicFacade}.
 * </p>
 *
 * @author Guillaume DROUET
 * @since 0.5.0
 */
public class WuicFacadeBuilder implements ClassPathResourceResolver {

    /**
     * The logger.
     */
    private final Logger log = LoggerFactory.getLogger(WuicFacadeBuilder.class.getName());

    /**
     * The context path where public URL will be exposed.
     */
    private String contextPath;

    /**
     * How the workflow should be executed when facade is created.
     */
    private WuicFacade.WarmupStrategy warmUpStrategy;

    /**
     * Indicates to view templates if they should reload their own configurations for each requests.
     */
    private Boolean multipleConfigInTagSupport;

    /**
     * The URL where wuic.xml file is located.
     */
    private URL wuicXmlPath;

    /**
     * Use or not configurator that creates default DAOs and engines.
     */
    private Boolean useDefaultContextBuilderConfigurator;

    /**
     * The classpath resource resolver.
     */
    private ClassPathResourceResolver classpathResourceResolver;

    /**
     * An list of inspectors of any built object.
     */
    private final List<ObjectBuilderInspector> inspectors;

    /**
     * Additional custom context configurators.
     */
    private final List<ContextBuilderConfigurator> configurators;

    /**
     * Central {@link com.github.wuic.context.ContextBuilder}.
     */
    private final ContextBuilderFacade contextBuilder;

    /**
     * The {@link com.github.wuic.util.PropertyResolver} that will be used by the builder to get properties.
     */
    private final EnhancedPropertyResolver propertyResolver;

    /**
     * <p>
     * Builds an instance with default settings.
     * </p>
     */
    public WuicFacadeBuilder() {
        contextPath = "";
        warmUpStrategy = WuicFacade.WarmupStrategy.NONE;
        multipleConfigInTagSupport = Boolean.TRUE;
        wuicXmlPath = getClass().getResource("/wuic.xml");
        propertyResolver = new EnhancedPropertyResolver();
        useDefaultContextBuilderConfigurator = Boolean.TRUE;
        inspectors = new ArrayList<ObjectBuilderInspector>();
        configurators = new ArrayList<ContextBuilderConfigurator>();
        contextBuilder = new ContextBuilderFacade();
        classpathResourceResolver = this;
        createResolverFromPath(getClass().getResource("/wuic.properties"));
        propertyResolver.addPropertyResolver(new SystemPropertyResolver());
    }

    /**
     * <p>
     * Builds an instance with settings initialized with the given properties. The @link BiFunction} must returns the
     * value corresponding to the key (first parameter) or the second parameter if the return value is {@code null}.
     * </p>
     *
     * @param properties the properties
     */
    public WuicFacadeBuilder(final PropertyResolver properties) {
        this();
        propertyResolver.addPropertyResolver(properties);

        try {
            // Choose specific location for property file
            final String propertyPath = propertyResolver.resolveProperty(ApplicationConfig.WUIC_PROPERTIES_PATH_PARAM, null);

            if (propertyPath != null) {
                createResolverFromPath(new URL(propertyPath));
            }

            contextPath = propertyResolver.resolveProperty(ApplicationConfig.WUIC_SERVLET_CONTEXT_PARAM, "");

            // Context where nuts will be exposed
            final String wuicCp = IOUtils.mergePath("/", contextPath);

            log.info("WUIC's full context path is {}", wuicCp);

            final String multipleConfStr = propertyResolver.resolveProperty(ApplicationConfig.WUIC_SERVLET_MULTIPLE_CONG_IN_TAG_SUPPORT, "true");

            if (!Boolean.parseBoolean(multipleConfStr)) {
                disableMultipleConfigInTagSupport();
            }

            final String warmupStrategyStr = propertyResolver.resolveProperty(ApplicationConfig.WUIC_WARMUP_STRATEGY, WuicFacade.WarmupStrategy.NONE.name());
            final WuicFacade.WarmupStrategy warmupStrategy = WuicFacade.WarmupStrategy.valueOf(warmupStrategyStr);

            final String xmlPath = propertyResolver.resolveProperty(ApplicationConfig.WUIC_SERVLET_XML_PATH_PARAM, null);

            contextPath(wuicCp);
            warmUpStrategy(warmupStrategy);

            final String useDefaultConfStr = propertyResolver.resolveProperty(ApplicationConfig.WUIC_USE_DEFAULT_CONTEXT_BUILDER_CONFIGURATORS, "true");

            if (!Boolean.parseBoolean(useDefaultConfStr)) {
                noDefaultContextBuilderConfigurator();
            }

            // Choose specific location for XML file
            if (xmlPath != null) {
                wuicXmlPath(new URL(xmlPath));
            }

            additionalContextBuilderConfigurators();
            additionalObjectBuilderInspectors();
        } catch (MalformedURLException mue) {
            WuicException.throwBadStateException(new IllegalStateException("Unable to initialize WuicFacade", mue));
        } catch (ClassNotFoundException cnfe) {
            WuicException.throwBadStateException(cnfe);
        } catch (InstantiationException ie) {
            WuicException.throwBadStateException(ie);
        } catch (IllegalAccessException iae) {
            WuicException.throwBadStateException(iae);
        }
    }

    /**
     * <p>
     * Creates a new {@link PropertyResolver} based on the properties at the given URL.
     * </p>
     *
     * @param properties the properties URL
     */
    private void createResolverFromPath(final URL properties) {
        if (properties != null) {
            InputStream is = null;

            try {
                is = properties.openStream();
                final Properties props = new Properties();
                props.load(is);
                propertyResolver.addPropertyResolver(new MapPropertyResolver(props));
            } catch (IOException ioe) {
                log.error("Unable to load properties", ioe);
            } finally {
                IOUtils.close(is);
            }
        }
    }

    /**
     * <p>
     * Defines the {@link ClassPathResourceResolver} of this builder.
     * </p>
     *
     * @param crr the new {@link ClassPathResourceResolver}
     * @return this
     */
    public final WuicFacadeBuilder classpathResourceResolver(final ClassPathResourceResolver crr) {
        this.classpathResourceResolver = crr;
        return this;
    }

    /**
     * <p>
     * Adds to the facade a list of additional {@link ContextBuilderConfigurator configurators} according to the
     * {@link ApplicationConfig#WUIC_ADDITIONAL_BUILDER_CONFIGURATORS} property.
     * </p>
     *
     * @return this
     * @throws ClassNotFoundException if class does not exists
     * @throws IllegalAccessException if class can be instantiated
     * @throws InstantiationException if an error occurs during instantiation
     */
    public final WuicFacadeBuilder additionalContextBuilderConfigurators()
            throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        final String value = propertyResolver.resolveProperty(ApplicationConfig.WUIC_ADDITIONAL_BUILDER_CONFIGURATORS, "");

        if (!value.isEmpty()) {
            final String[] classes = value.split(",");
            final ContextBuilderConfigurator[] configurators = new ContextBuilderConfigurator[classes.length];

            for (int i = 0; i < classes.length; i++) {
                configurators[i] = ContextBuilderConfigurator.class.cast(Class.forName(classes[i]).newInstance());
            }

            contextBuilderConfigurators(configurators);
        }

        return this;
    }

    /**
     * <p>
     * Adds to the facade a list of additional {@link ObjectBuilderInspector inspectors} according to the
     * {@link ApplicationConfig#WUIC_ADDITIONAL_BUILDER_INSPECTOR} property.
     * </p>
     *
     * @return this
     * @throws ClassNotFoundException if class does not exists
     * @throws IllegalAccessException if class can be instantiated
     * @throws InstantiationException if an error occurs during instantiation
     */
    public final WuicFacadeBuilder additionalObjectBuilderInspectors()
            throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        final String value = propertyResolver.resolveProperty(ApplicationConfig.WUIC_ADDITIONAL_BUILDER_INSPECTOR, "");

        if (!value.isEmpty()) {
            final String[] classes = value.split(",");
            final ObjectBuilderInspector[] inspectors = new ObjectBuilderInspector[classes.length];

            for (int i = 0; i < classes.length; i++) {
                inspectors[i] = ObjectBuilderInspector.class.cast(Class.forName(classes[i]).newInstance());
            }

            objectBuilderInspector(inspectors);
        }

        return this;
    }

    /**
     * <p>
     * Sets a new location for wuic.xml file.
     * </p>
     *
     * @param xml the new location
     * @return this
     */
    public final WuicFacadeBuilder wuicXmlPath(final URL xml) {
        this.wuicXmlPath = xml;
        return this;
    }

    /**
     * <p>
     * Sets a new location for wuic.properties file.
     * </p>
     *
     * @param properties the new location
     * @return this
     */
    public final WuicFacadeBuilder wuicPropertiesPath(final URL properties) {
        createResolverFromPath(properties);
        return this;
    }

    /**
     * <p>
     * Do not use any wuic.xml file to configure the context.
     * </p>
     *
     * @return this
     */
    public final WuicFacadeBuilder noXmlConfiguration() {
        this.wuicXmlPath = null;
        return this;
    }

    /**
     * <p>
     * Sets a context path.
     * </p>
     *
     * @param cp the context path.
     * @return this
     */
    public final WuicFacadeBuilder contextPath(final String cp) {
        this.contextPath = cp;
        return this;
    }

    /**
     * <p>
     * Specifies a {@link com.github.wuic.WuicFacade.WarmupStrategy}.
     * </p>
     *
     * @param wus the new strategy
     * @return this
     */
    public final WuicFacadeBuilder warmUpStrategy(final WuicFacade.WarmupStrategy wus) {
        this.warmUpStrategy = wus;
        return this;
    }

    /**
     * <p>
     * Disables a re-configuration from view template each time a workflow execution is performed.
     * </p>
     *
     * @return this
     */
    public final WuicFacadeBuilder disableMultipleConfigInTagSupport() {
        this.multipleConfigInTagSupport = Boolean.FALSE;
        return this;
    }

    /**
     * <p>
     * Indicates that configurator creating default DAOs and engines should not be used when the context is initialized.
     * </p>
     *
     * @return this
     */
    public final WuicFacadeBuilder noDefaultContextBuilderConfigurator() {
        this.useDefaultContextBuilderConfigurator = Boolean.FALSE;
        return this;
    }

    /**
     * <p>
     * Adds additional object builder inspector for any object creation.
     * </p>
     *
     * @param obi the {@link ObjectBuilderInspector inspectors}
     * @return this
     */
    public final WuicFacadeBuilder objectBuilderInspector(final ObjectBuilderInspector ... obi) {
        Collections.addAll(this.inspectors, obi);
        return this;
    }

    /**
     * <p>
     * Adds additional context builder configurators.
     * </p>
     *
     * @param contextBuilderConfigurator additional context builder configurator
     * @return this
     */
    public final WuicFacadeBuilder contextBuilderConfigurators(final ContextBuilderConfigurator ... contextBuilderConfigurator) {
        Collections.addAll(this.configurators, contextBuilderConfigurator);
        return this;
    }

    /**
     * <p>
     * Returns the central {@link com.github.wuic.context.ContextBuilder}.
     * </p>
     *
     * @return the context builder
     */
    public final ContextBuilderFacade contextBuilder() {
        return contextBuilder;
    }

    /**
     * <p>
     * Builds a new facade.
     * </p>
     *
     * @return the facade using the settings defines in this builder
     * @throws WuicException if facade cannot be built
     */
    public WuicFacade build() throws WuicException {
        log.info("Building facade.");
        return new WuicFacade(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public URL getResource(final String resourcePath) {
        return getClass().getResource(resourcePath);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public InputStream getResourceAsStream(final String resourcePath) {
        return getClass().getResourceAsStream(resourcePath);
    }

    /**
     * <p>
     * Gets the {@link ClassPathResourceResolver}.
     * </p>
     *
     * @return the instance of {@link ClassPathResourceResolver}
     */
    ClassPathResourceResolver getClasspathResourceResolver() {
        return classpathResourceResolver;
    }

    /**
     * <p>
     * Gets the context path.
     * </p>
     *
     * @return the context path
     */
    String getContextPath() {
        return contextPath;
    }

    /**
     * <p>
     * Gets the warmup strategy.
     * </p>
     *
     * @return the {@link com.github.wuic.WuicFacade.WarmupStrategy}
     */
    WuicFacade.WarmupStrategy getWarmUpStrategy() {
        return warmUpStrategy;
    }

    /**
     * <p>
     * Indicates if multiple configurations inside tag are allowed.
     * </p>
     *
     * @return {@code true} or {@code false}
     */
    Boolean getMultipleConfigInTagSupport() {
        return multipleConfigInTagSupport;
    }

    /**
     * <p>
     * Gets the wuic.xml location if any.
     * </p>
     *
     * @return the location
     */
    URL getWuicXmlPath() {
        return wuicXmlPath;
    }

    /**
     * <p>
     * Gets the property resolver.
     * </p>
     *
     * @return the property resolver
     */
    PropertyResolver getPropertyResolver() {
        return propertyResolver;
    }

    /**
     * <p>
     * Indicates if default context builders should be used.
     * </p>
     *
     * @return {@code true} or {@code false}
     */
    Boolean getUseDefaultContextBuilderConfigurator() {
        return useDefaultContextBuilderConfigurator;
    }

    /**
     * <p>
     * Gets the extra interceptors if any.
     * </p>
     *
     * @return the interceptors
     */
    List<ObjectBuilderInspector> getObjectBuilderInspectors() {
        return inspectors;
    }

    /**
     * <p>
     * Gets extra configurators if any.
     * </p>
     *
     * @return the configurators
     */
    List<ContextBuilderConfigurator> getConfigurators() {
        return configurators;
    }

    /**
     * <p>
     * This class gives a chance to directly define settings for the final {@link com.github.wuic.context.ContextBuilder} of the built
     * {@link WuicFacade}.
     * </p>
     *
     * @author Guillaume DROUET
     * @since 0.5.1
     */
    public final class ContextBuilderFacade extends ContextBuilder {

        /**
         * <p>
         * Backs to the facade builder API.
         * </p>
         *
         * @return the enclosing class
         */
        public WuicFacadeBuilder toFacade() {
            return WuicFacadeBuilder.this;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public ContextBuilderFacade configureDefault() throws IOException {
            super.configureDefault();
            return this;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public ContextBuilderFacade tag(final Object tag) {
            super.tag(tag);
            return this;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public ContextBuilderFacade clearTag(final Object tag) {
            super.clearTag(tag);
            return this;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public ContextBuilderFacade releaseTag() {
            super.releaseTag();
            return this;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public ContextBuilderFacade processContext(final ProcessContext processContext) {
            super.processContext(processContext);
            return this;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public ContextNutDaoBuilderFacade contextNutDaoBuilder(final String id, final String type) {
            return new ContextNutDaoBuilderFacade(id, type);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public ContextNutDaoBuilderFacade contextNutDaoBuilder(final Class<?> type) {
            return new ContextNutDaoBuilderFacade(getDefaultBuilderId(type), type.getSimpleName() + "Builder");
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public ContextNutFilterBuilderFacade contextNutFilterBuilder(final String id, final String type) {
            return new ContextNutFilterBuilderFacade(id, type);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public ContextEngineBuilderFacade contextEngineBuilder(final String id, final String type) {
            return new ContextEngineBuilderFacade(id, type);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public ContextEngineBuilderFacade contextEngineBuilder(final Class<?> type) {
            return new ContextEngineBuilderFacade(getDefaultBuilderId(type), type.getSimpleName() + "Builder");
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public ContextBuilderFacade interceptor(final ContextInterceptor interceptor) {
            super.interceptor(interceptor);
            return ContextBuilderFacade.this;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public ContextBuilderFacade heap(final String id, final String ndbId, final String[] path, final HeapListener ... listeners)
                throws IOException {
            super.heap(id, ndbId, path, listeners);
            return ContextBuilderFacade.this;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public ContextBuilderFacade heap(final boolean disposable,
                                         final String id,
                                         final String ndbId,
                                         final String[] heapIds,
                                         final String[] path,
                                         final HeapListener ... listeners)
                throws IOException {
            super.heap(disposable, id, ndbId, heapIds, path, listeners);
            return ContextBuilderFacade.this;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public ContextBuilderFacade template(final String id, final String[] ebIds, final String... daos) throws IOException {
            super.template(id, ebIds, daos);
            return ContextBuilderFacade.this;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public ContextBuilderFacade template(final String id,
                                             final String[] ebIds,
                                             final String[] ebIdsExclusion,
                                             final Boolean includeDefaultEngines,
                                             final String... ndbIds)
                throws IOException {
            super.template(id, ebIds, ebIdsExclusion, includeDefaultEngines, ndbIds);
            return ContextBuilderFacade.this;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public ContextBuilderFacade workflow(final String identifier,
                                             final Boolean forEachHeap,
                                             final String heapIdPattern,
                                             final String workflowTemplateId)
                throws IOException, WorkflowTemplateNotFoundException {
            super.workflow(identifier, forEachHeap, heapIdPattern, workflowTemplateId);
            return ContextBuilderFacade.this;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public ContextBuilderFacade mergeSettings(final ContextBuilder other) {
            super.mergeSettings(other);
            return ContextBuilderFacade.this;
        }

        /**
         * <p>
         * Exposes {@link ContextBuilderFacade} when {@link #toContext} is called.
         * </p>
         *
         * @author Guillaume DROUET
         * @since 0.5.1
         */
        public final class ContextNutDaoBuilderFacade extends ContextNutDaoBuilder {

            /**
             * <p>
             * Builds a new instance.
             * </p>
             *
             * @param id the ID
             * @param type the type
             */
            public ContextNutDaoBuilderFacade(final String id, final String type) {
                super(id, type);
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public ContextNutDaoBuilderFacade proxyRootPath(final String path) {
                super.proxyRootPath(path);
                return this;
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public ContextNutDaoBuilderFacade proxyPathForDao(final String path, final String id) {
                super.proxyPathForDao(path, id);
                return this;
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public ContextNutDaoBuilderFacade proxyPathForNut(final String path, final Nut nut) {
                super.proxyPathForNut(path, nut);
                return this;
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public ContextNutDaoBuilderFacade property(final String key, final Object value) {
                super.property(key, value);
                return this;
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public ContextBuilderFacade toContext() {
                super.toContext();
                return ContextBuilderFacade.this;
            }
        }

        /**
         * <p>
         * Exposes {@link ContextBuilderFacade} when {@link #toContext} is called.
         * </p>
         *
         * @author Guillaume DROUET
         * @since 0.5.1
         */
        public final class ContextEngineBuilderFacade extends ContextEngineBuilder {

            /**
             * <p>
             * Builds a new instance.
             * </p>
             *
             * @param id the ID
             * @param type the ID
             */
            public ContextEngineBuilderFacade(final String id, final String type) {
                super(id, type);
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public ContextEngineBuilderFacade property(final String key, final Object value) {
                super.property(key, value);
                return this;
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public ContextBuilderFacade toContext() {
                super.toContext();
                return ContextBuilderFacade.this;
            }
        }

        /**
         * <p>
         * Exposes {@link ContextBuilderFacade} when {@link #toContext} is called.
         * </p>
         *
         * @author Guillaume DROUET
         * @since 0.5.1
         */
        public final class ContextNutFilterBuilderFacade extends ContextNutFilterBuilder {

            /**
             * <p>
             * Builds a new instance.
             * </p>
             *
             * @param id the ID
             * @param type the ID
             */
            public ContextNutFilterBuilderFacade(final String id, final String type) {
                super(id, type);
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public ContextNutFilterBuilderFacade property(final String key, final Object value) {
                super.property(key, value);
                return this;
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public ContextBuilderFacade toContext() {
                super.toContext();
                return ContextBuilderFacade.this;
            }
        }
    }
}
