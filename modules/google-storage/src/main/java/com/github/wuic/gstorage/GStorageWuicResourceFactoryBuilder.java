/*
 * "Copyright (c) 2013   Capgemini Technology Services (hereinafter "Capgemini")
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


package com.github.wuic.gstorage;

import com.github.wuic.ApplicationConfig;
import com.github.wuic.resource.WuicResourceFactory;
import com.github.wuic.resource.WuicResourceFactoryBuilder;
import com.github.wuic.resource.impl.AbstractWuicResourceFactory;
import com.github.wuic.resource.impl.AbstractWuicResourceFactoryBuilder;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * <p>
 * Builder for resource access on a Google Storage Cloud.
 * </p>
 *
 * @author Corentin AZELART
 * @version 1.0
 * @since 0.3.3
 */
public class GStorageWuicResourceFactoryBuilder extends AbstractWuicResourceFactoryBuilder {

    /**
     * Supported properties with their default value.
     */
    private Map<String, Object> supportedProperties;

    /**
     * Delegate concrete implementation.
     */
    private AbstractWuicResourceFactory delegate;

    /**
     * <p>
     * Creates a new instance.
     * </p>
     */
    public GStorageWuicResourceFactoryBuilder() {
        this(new GStorageWuicResourceFactory(new AbstractWuicResourceFactory.DefaultWuicResourceFactory(null)));
    }

    /**
     * <p>
     * Creates a new instance thanks to an already built factory.
     * </p>
     *
     * @param built the already built factory.
     */
    public GStorageWuicResourceFactoryBuilder(final WuicResourceFactory built) {
        super(built);
    }

    /**
     * <p>
     * Creates a new factory supporting regex.
     * </p>
     *
     * @return the regex factory
     */
    @Override
    protected WuicResourceFactoryBuilder newRegexFactoryBuilder() {
        return new GStorageWuicResourceFactoryBuilder(
                new GStorageWuicResourceFactory(
                        new AbstractWuicResourceFactory.RegexWuicResourceFactory(null)));
    }

    /**
     * <p>
     * Default factory validating particular properties.
     * </p>
     *
     * @author Corentin AZELART
     * @version 1.0
     * @since 0.3.3
     */
    public static class GStorageWuicResourceFactory extends AbstractWuicResourceFactory {

        /**
         * Supported properties with their default value.
         */
        private Map<String, Object> supportedProperties;

        /**
         * Delegate concrete implementation.
         */
        private AbstractWuicResourceFactory delegate;

        /**
         * <p>
         * Builds a new instance.
         * </p>
         *
         * @param toDecorate a factory to be decorated
         */
        public GStorageWuicResourceFactory(final AbstractWuicResourceFactory toDecorate) {
            super(null);

            delegate = toDecorate;

            // Init default property
            supportedProperties = new HashMap<String, Object>();
            supportedProperties.put(ApplicationConfig.GS_BUCKET, null);
            supportedProperties.put(ApplicationConfig.GS_CLIENT_ID, null);
            supportedProperties.put(ApplicationConfig.GS_PATH, "");
            supportedProperties.put(ApplicationConfig.GS_PRIVATE_KEY_PATH, "");
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void setProperty(final String key, final String value) {

            // Try to override an existing property
            if (!supportedProperties.containsKey(key)) {
                throw new IllegalArgumentException(key + " is not a property which is supported by the GStorageWuicResourceFactory");
            } else {
                supportedProperties.put(key, value);
            }

            // Set new protocol with the new property
            setWuicProtocol(new GStorageWuicResourceProtocol(
                    (String) supportedProperties.get(ApplicationConfig.GS_BUCKET),
                    (String) supportedProperties.get(ApplicationConfig.GS_CLIENT_ID),
                    (String) supportedProperties.get(ApplicationConfig.GS_PATH),
                    (String) supportedProperties.get(ApplicationConfig.GS_PRIVATE_KEY_PATH)));
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Pattern getPattern(final String path) {
            return delegate.getPattern(path);
        }
    }
}