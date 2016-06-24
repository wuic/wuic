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
import com.github.wuic.config.bean.json.FileJsonContextBuilderConfigurator;
import com.github.wuic.config.bean.xml.FileXmlContextBuilderConfigurator;
import com.github.wuic.context.ContextBuilder;
import com.github.wuic.context.ContextBuilderConfigurator;
import com.github.wuic.context.ContextInterceptor;
import com.github.wuic.exception.WorkflowTemplateNotFoundException;
import com.github.wuic.exception.WuicException;
import com.github.wuic.nut.HeapListener;
import com.github.wuic.nut.Nut;
import com.github.wuic.nut.dao.NutDao;
import com.github.wuic.util.Consumer;
import com.github.wuic.util.EnhancedPropertyResolver;
import com.github.wuic.util.IOUtils;
import com.github.wuic.util.MapPropertyResolver;
import com.github.wuic.util.PropertyResolver;
import com.github.wuic.util.SystemPropertyResolver;
import com.github.wuic.util.UrlUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
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
     * The URL where configuration files are located.
     */
    private List<URL> wuicConfigurationPaths;

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
        wuicConfigurationPaths = new ArrayList<URL>();
        inspectors = new ArrayList<ObjectBuilderInspector>();
        configurators = new ArrayList<ContextBuilderConfigurator>();
        contextBuilder = new ContextBuilderFacade();
        classpathResourceResolver = this;
        propertyResolver = new EnhancedPropertyResolver();
        propertyResolver.addPropertyResolver(new SystemPropertyResolver());
    }

    /**
     * <p>
     * Builds an instance by copy.
     * </p>
     *
     * @param other the builder to copy
     */
    public WuicFacadeBuilder(final WuicFacadeBuilder other) {
        contextPath = other.contextPath;
        warmUpStrategy = other.warmUpStrategy;
        multipleConfigInTagSupport = other.multipleConfigInTagSupport;
        wuicConfigurationPaths = other.wuicConfigurationPaths;
        propertyResolver = other.propertyResolver;
        useDefaultContextBuilderConfigurator = other.useDefaultContextBuilderConfigurator;
        inspectors = other.inspectors;
        configurators = other.configurators;
        contextBuilder = other.contextBuilder;
        classpathResourceResolver = other.classpathResourceResolver;
    }

    /**
     * <p>
     * Builds an instance with settings initialized with the given properties.
     * </p>
     *
     * @param properties the properties
     */
    public WuicFacadeBuilder(final PropertyResolver properties) {
        this();
        propertyResolver.addPropertyResolver(properties);
    }

    /**
     * <p>
     * Detects any additional property file, configuration and {@link ObjectBuilderInspector}
     * declared with their corresponding properties.
     * </p>
     *
     * @param profiles the active profiles
     * @throws MalformedURLException if an URL is not well formed
     * @throws IllegalAccessException if a declared class does not expose a default constructor
     * @throws InstantiationException if a class can't be instantiated
     * @throws ClassNotFoundException if a declared class is not found in the classpath
     */
    private void additionalComponents(final String ... profiles)
            throws MalformedURLException, IllegalAccessException, InstantiationException, ClassNotFoundException {
        // Choose specific location for property file
        final String propertyPath = propertyResolver.resolveProperty(ApplicationConfig.WUIC_PROPERTIES_PATH_PARAM, null);

        if (propertyPath != null) {
            createResolverFromPath(new URL(propertyPath));
        }

        additionalContextBuilderConfigurators();
        additionalObjectBuilderInspectors();

        final String xmlPath = propertyResolver.resolveProperty(ApplicationConfig.WUIC_SERVLET_XML_PATH_PARAM, null);
        final String jsonPath = propertyResolver.resolveProperty(ApplicationConfig.WUIC_SERVLET_JSON_PATH_PARAM, null);

        if (wuicConfigurationPaths != null) {
            additionalConfigurationPath(profiles);

            // Choose specific location for XML file
            if (xmlPath != null) {
                wuicConfigurationPath(new URL(xmlPath));
            }

            // Choose specific location for JSON file
            if (jsonPath != null) {
                wuicConfigurationPath(new URL(jsonPath));
            }
        }
    }

    /**
     * <p>
     * Adds to the facade a list of additional {@link ContextBuilderConfigurator configurators} according to the
     * {@link ApplicationConfig#WUIC_ADDITIONAL_BUILDER_CONFIGURATORS} property.
     * </p>
     *
     * @throws ClassNotFoundException if class does not exists
     * @throws IllegalAccessException if class can be instantiated
     * @throws InstantiationException if an error occurs during instantiation
     */
    private void additionalContextBuilderConfigurators()
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
    }

    /**
     * <p>
     * Adds to the facade a list of additional {@link ObjectBuilderInspector inspectors} according to the
     * {@link ApplicationConfig#WUIC_ADDITIONAL_BUILDER_INSPECTOR} property.
     * </p>
     *
     * @throws ClassNotFoundException if class does not exists
     * @throws IllegalAccessException if class can be instantiated
     * @throws InstantiationException if an error occurs during instantiation
     */
    private void additionalObjectBuilderInspectors()
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
    }

    /**
     * <p>
     * Detects in the {@link #classpathResourceResolver} any {@code wuic.xml} or {@code wuic.json} file and install them
     * if they exist. Moreover, for each enabled profile, the existence of a file called {@code wuic-[profile].xml} or
     * {@code wuic-[profile].json} will be tested and added if detected, overriding the properties defined in
     * {@code wuic.json} or {@code wuic.xml} file.
     * </p>
     *
     * @param profiles the active profiles
     */
    private void additionalConfigurationPath(final String ... profiles) {
        detectInClassesLocation("wuic", new String[] {".xml", ".json"}, new Consumer<URL>() {

            /**
             * {@inheritDoc}
             */
            @Override
            public void apply(final URL consumed) {
                wuicConfigurationPath(consumed);
            }
        }, profiles);
    }

    /**
     * <p>
     * Loads the active profiles by checking the {@link ApplicationConfig#PROFILES} property.
     * Moreover, if the {@link ApplicationConfig#ADDITIONAL_PROFILES_PROPERTIES} is defined, each property defined in
     * the associated value is also read to enrich the returned array of profiles.
     * </p>
     *
     * @return the array of profiles
     */
    private String[] loadProfiles() {

        // Gets active profiles
        final List<String> profilesList = new ArrayList<String>();
        final String profilesProperty = propertyResolver.resolveProperty(ApplicationConfig.PROFILES);

        // Somep profiles are defined
        if (profilesProperty != null) {
            profilesList.addAll(Arrays.asList(profilesProperty.split(",")));
        }

        // Check for additional properties
        final String additionalProfilesProperties = propertyResolver.resolveProperty(ApplicationConfig.ADDITIONAL_PROFILES_PROPERTIES);

        // Additional properties are defined
        if (additionalProfilesProperties != null) {
            for (final String additionalProfilesProperty : additionalProfilesProperties.split(",")) {
                final String p = propertyResolver.resolveProperty(additionalProfilesProperty);

                // Profiles are associated to this additional property
                if (p != null) {
                    profilesList.addAll(Arrays.asList(p.split(",")));
                }
            }
        }

        return profilesList.toArray(new String[profilesList.size()]);
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
                addPropertyResolver(new MapPropertyResolver(props));
            } catch (IOException ioe) {
                log.error("Unable to load properties", ioe);
            } finally {
                IOUtils.close(is);
            }
        }
    }

    /**
     * <p>
     * Adds the {@code wuic.properties} file if detected and configures the activated profiles and the associated
     * property files if any. For each enabled profile, the existence of a file called {@code wuic-[profile].properties}
     * will be tested and added if detected, overriding the properties defined in {@code wuic.properties} file.
     * </p>
     *
     * @param profiles the active profiles
     */
    private void additionalPropertyPath(final String... profiles) {
        detectInClassesLocation("wuic", new String[] { ".properties" }, new Consumer<URL>() {

            /**
             * {@inheritDoc}
             */
            @Override
            public void apply(final URL consumed) {
                wuicPropertiesPath(consumed);
            }
        }, profiles);
    }

    /**
     * <p>
     * Detects in the classpath any file corresponding to {@code [prefix][suffix]} and for each profile a file corresponding
     * to {@code [prefix]-[profile][suffix]}. Not that suffix is an array where each element will be used to create all
     * the possible prefix/suffic combinations.
     * </p>
     *
     * @param prefix the prefix
     * @param suffix all the possible suffix
     * @param consumer the notified consumer when a file is detected
     * @param profiles the profiles
     */
    private void detectInClassesLocation(final String prefix,
                                         final String[] suffix,
                                         final Consumer<URL> consumer,
                                         final String ... profiles) {
        final String[] files;

        // Load default name
        if (profiles.length == 0) {
            files = new String[suffix.length];

            // Create a path for each suffix
            for (int i = 0; i < suffix.length; i++) {
                files[i] = prefix + suffix[i];
            }
        } else {
            files = new String[(profiles.length + 1) * suffix.length];
            int cpt = suffix.length;

            for (int i = 0; i < suffix.length; i++) {
                files[i] = prefix + suffix[i];
            }

            // Try to detect also property files specific to each active profile
            for (final String p : profiles) {

                // For each suffix, create a file per profile
                for (final String s : suffix) {
                    files[cpt++] = String.format("%s-%s%s", prefix, p, s);
                }
            }
        }

        UrlUtils.detectInClassesLocation(classpathResourceResolver, consumer, files);
    }

    /**
     * <p>
     * Configures the default {@link NutDao} class specified in {@link ApplicationConfig#WUIC_DEFAULT_NUT_DAO_CLASS} property.
     * </p>
     *
     * @throws ClassNotFoundException if specified class does not exists
     */
    @SuppressWarnings("unchecked")
    private void configureDefaultNutDaoClass() throws ClassNotFoundException {
        final String defaultDaoClass = propertyResolver.resolveProperty(ApplicationConfig.WUIC_DEFAULT_NUT_DAO_CLASS);

        // The default class property has been specified
        if (defaultDaoClass != null) {
            final Class<?> clazz = Class.forName(defaultDaoClass);

            // Checks that class IS-A NutDao
            if (!NutDao.class.isAssignableFrom(clazz)) {
                WuicException.throwBadArgumentException(new IllegalArgumentException(String.format(
                        "class %s specified in %s property must be %s.",
                        defaultDaoClass,
                        ApplicationConfig.WUIC_DEFAULT_NUT_DAO_CLASS,
                        NutDao.class.getName())));
            }

            contextBuilder.defaultNutDaoClass((Class<? extends NutDao>) clazz);
        }
    }

    /**
     * <p>
     * Adds the given resolver to this builder.
     * </p>
     *
     * @param resolver the resolver
     * @return this builder
     */
    public final WuicFacadeBuilder addPropertyResolver(final PropertyResolver resolver) {
        propertyResolver.addPropertyResolver(resolver);
        return this;
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
     * Adds a new location for configuration file.
     * </p>
     *
     * @param path the new location
     * @return this
     */
    public final WuicFacadeBuilder wuicConfigurationPath(final URL path) {
        if (this.wuicConfigurationPaths == null) {
            WuicException.throwBadStateException(new IllegalStateException(
                    "You can't add a configuration path after noConfigurationPah() method has been called"));
        }

        if (path != null) {
            this.wuicConfigurationPaths.add(path);
        }

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
     * Do not use any configuration file to configure the context.
     * </p>
     *
     * @return this
     */
    public final WuicFacadeBuilder noConfigurationPath() {
        this.wuicConfigurationPaths = null;
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
     * Adds a new configurator based on a given configuration path.
     * No configurator will be added if the path name does not ends with .json or .xml extension.
     * </p>
     *
     * @param path the path to be added
     * @throws IOException if any I/O error occurs
     * @throws JAXBException if an XML error occurs
     */
    public final void addConfigurator(final URL path) throws IOException, JAXBException {
        if (path.toString().endsWith(".xml")) {
            getConfigurators().add(new FileXmlContextBuilderConfigurator(path));
        } else if (path.toString().endsWith(".json")) {
            getConfigurators().add(new FileJsonContextBuilderConfigurator(path));
        } else {
            log.warn("Configuration file path {} does not ends with .xml or .json, ignoring...", path.toString());
        }
    }

    /**
     * <p>
     * Gets the property resolver.
     * </p>
     *
     * @return the property resolver
     */
    public PropertyResolver getPropertyResolver() {
        return propertyResolver;
    }

    /**
     * <p>
     * Builds a new facade. If the attributes have not been explicitly defined by calling the configuration method, the
     * builder looks for properties defined in the {@link PropertyResolver} and fallback to a default value if it's not
     * the case.
     * </p>
     *
     * @return the facade using the settings defines in this builder
     * @throws WuicException if facade cannot be built
     */
    public WuicFacade build() throws WuicException {
        log.info("Building facade.");

        try {
            contextBuilder.enableProfile(loadProfiles());
            final String[] profiles = contextBuilder.getActiveProfiles();
            additionalPropertyPath(profiles);
            additionalComponents(profiles);
            configureDefaultNutDaoClass();

            if (contextPath == null) {
                // Context where nuts will be exposed
                contextPath = propertyResolver.resolveProperty(ApplicationConfig.WUIC_SERVLET_CONTEXT_PARAM, "");
                final String wuicCp = IOUtils.mergePath("/", contextPath);
                log.info("WUIC's full context path is {}", wuicCp);
                contextPath(wuicCp);
            }

            if (multipleConfigInTagSupport == null) {
                multipleConfigInTagSupport = Boolean.parseBoolean(propertyResolver.resolveProperty(
                        ApplicationConfig.WUIC_SERVLET_MULTIPLE_CONG_IN_TAG_SUPPORT, "true"));
            }

            if (warmUpStrategy == null) {
                final String warmupStrategyStr = propertyResolver.resolveProperty(ApplicationConfig.WUIC_WARMUP_STRATEGY, WuicFacade.WarmupStrategy.NONE.name());
                final WuicFacade.WarmupStrategy warmupStrategy = WuicFacade.WarmupStrategy.valueOf(warmupStrategyStr);
                warmUpStrategy(warmupStrategy);
            }

            if (useDefaultContextBuilderConfigurator == null) {
                useDefaultContextBuilderConfigurator = Boolean.parseBoolean(propertyResolver.resolveProperty(
                        ApplicationConfig.WUIC_USE_DEFAULT_CONTEXT_BUILDER_CONFIGURATORS, "true"));
            }
        } catch (MalformedURLException mue) {
            WuicException.throwBadStateException(new IllegalStateException("Unable to initialize WuicFacade", mue));
        } catch (ClassNotFoundException cnfe) {
            WuicException.throwBadStateException(cnfe);
        } catch (InstantiationException ie) {
            WuicException.throwBadStateException(ie);
        } catch (IllegalAccessException iae) {
            WuicException.throwBadStateException(iae);
        }

        return new WuicFacade(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public URL getResource(final String resourcePath) {
        return getClass().getResource("/" + resourcePath);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public InputStream getResourceAsStream(final String resourcePath) {
        return getClass().getResourceAsStream("/" + resourcePath);
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
     * Gets the configuration files locations if any.
     * </p>
     *
     * @return the location
     */
    List<URL> wuicConfigurationPaths() {
        return wuicConfigurationPaths;
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
        public ContextBuilderFacade tag(final Object tag, final String ... profiles) {
            super.tag(tag, profiles);
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
        public ContextBuilderFacade enableProfile(final String... profiles) {
            super.enableProfile(profiles);
            return ContextBuilderFacade.this;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public ContextBuilderFacade disableProfile(final String... profiles) {
            super.disableProfile(profiles);
            return ContextBuilderFacade.this;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public ContextBuilderFacade heap(final String id, final String ndbId, final String[] path, final HeapListener ... listeners) {
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
                                         final HeapListener ... listeners) {
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
                                             final String[] ebTypesExclusion,
                                             final Boolean includeDefaultEngines,
                                             final String... ndbIds)
                throws IOException {
            super.template(id, ebIds, ebTypesExclusion, includeDefaultEngines, ndbIds);
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
