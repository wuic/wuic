/*
 * "Copyright (final c) 2014   Capgemini Technology Services (final hereinafter "Capgemini")
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
import com.github.wuic.exception.WuicException;
import com.github.wuic.exception.wrapper.BadArgumentException;
import com.github.wuic.util.BiFunction;
import com.github.wuic.util.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * <p>
 * This class handles several settings in its state and is required to create a {@link WuicFacade}.
 * </p>
 *
 * @author Guillaume DROUET
 * @version 1.0
 * @since 0.5.0
 */
public class WuicFacadeBuilder {

    /**
     * The logger.
     */
    private final Logger log = LoggerFactory.getLogger(WuicFacadeBuilder.class.getName());

    /**
     * The context path where public URL will be exposed.
     */
    String contextPath;

    /**
     * How the workflow should be executed when facade is created.
     */
    WuicFacade.WarmupStrategy warmUpStrategy;

    /**
     * Indicates to view templates if they should reload their own configurations for each requests.
     */
    Boolean multipleConfigInTagSupport;

    /**
     * The URL where wuic.xml file is located.
     */
    URL wuicXmlPath;

    /**
     * Use or not configurator that creates default DAOs and engines.
     */
    Boolean useDefaultContextBuilderConfigurator;

    /**
     * An inspector of any built object.
     */
    ObjectBuilderInspector objectBuilderInspector;

    /**
     * Additional custom context configurators.
     */
    List<ContextBuilderConfigurator> configurators;

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
        useDefaultContextBuilderConfigurator = Boolean.TRUE;
        objectBuilderInspector = null;
        configurators = new ArrayList<ContextBuilderConfigurator>();
    }

    /**
     * <p>
     * Builds an instance with settings initialized with the given properties. The @link BiFunction} must returns the
     * value corresponding to the key (first parameter) or the second parameter if the return value is {@code null}.
     * </p>
     *
     * @param properties the properties
     */
    public WuicFacadeBuilder(final BiFunction<String, String, String> properties) {
        this();
        contextPath = properties.apply(ApplicationConfig.WUIC_SERVLET_CONTEXT_PARAM, "");

        // Context where nuts will be exposed
        final String wuicCp = IOUtils.mergePath("/", contextPath);

        log.info("WUIC's full context path is {}", wuicCp);

        try {
            final String multipleConfStr = properties.apply(ApplicationConfig.WUIC_SERVLET_MULTIPLE_CONG_IN_TAG_SUPPORT, "true");

            if (!Boolean.parseBoolean(multipleConfStr)) {
                disableMultipleConfigInTagSupport();
            }

            final String warmupStrategyStr = properties.apply(ApplicationConfig.WUIC_WARMUP_STRATEGY, WuicFacade.WarmupStrategy.NONE.name());
            final WuicFacade.WarmupStrategy warmupStrategy = WuicFacade.WarmupStrategy.valueOf(warmupStrategyStr);

            final String xmlPath = properties.apply(ApplicationConfig.WUIC_SERVLET_XML_PATH_PARAM, null);
            contextPath(wuicCp);
            warmUpStrategy(warmupStrategy);
            disableMultipleConfigInTagSupport();

            final String useDefaultConfStr = properties.apply(ApplicationConfig.WUIC_USE_DEFAULT_CONTEXT_BUILDER_CONFIGURATORS, "true");

            if (!Boolean.parseBoolean(useDefaultConfStr)) {
                noDefaultContextBuilderConfigurator();
            }

            // Choose specific location for XML file
            if (xmlPath != null) {
                if (Boolean.parseBoolean(properties.apply(ApplicationConfig.WUIC_SERVLET_XML_SYS_PROP_PARAM, "false"))) {
                    wuicXmlPath(new URL(System.getProperty(xmlPath)));
                } else {
                    wuicXmlPath(new URL(xmlPath));
                }
            }
        } catch (MalformedURLException mue) {
            throw new BadArgumentException(new IllegalArgumentException("Unable to initialize WuicServlet", mue));
        }
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
     * Sets an inspector for any object creation.
     * </p>
     *
     * @param obi the {@link ObjectBuilderInspector}
     * @return this
     */
    public final WuicFacadeBuilder objectBuilderInspector(final ObjectBuilderInspector obi) {
        this.objectBuilderInspector = obi;
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
}
