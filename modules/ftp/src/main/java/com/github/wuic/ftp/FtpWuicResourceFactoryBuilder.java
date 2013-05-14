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


package com.github.wuic.ftp;

import com.github.wuic.ApplicationConfig;
import com.github.wuic.resource.WuicResourceFactory;
import com.github.wuic.resource.WuicResourceFactoryBuilder;
import com.github.wuic.resource.impl.AbstractWuicResourceFactory;
import com.github.wuic.resource.impl.AbstractWuicResourceFactoryBuilder;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPSClient;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * <p>
 * Builder for resource access on a FTP server.
 * </p>
 *
 * @author Guillaume DROUET
 * @version 1.0
 * @since 0.3.1
 */
public class FtpWuicResourceFactoryBuilder extends AbstractWuicResourceFactoryBuilder {

    /**
     * <p>
     * Creates a new instance.
     * </p>
     */
    public FtpWuicResourceFactoryBuilder() {
        this(new FtpWuicResourceFactory(new AbstractWuicResourceFactory.DefaultWuicResourceFactory(null)));
    }

    /**
     * <p>
     * Creates a new instance thanks to an already built factory.
     * </p>
     *
     * @param built the already built factory.
     */
    public FtpWuicResourceFactoryBuilder(final WuicResourceFactory built) {
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
        return new FtpWuicResourceFactoryBuilder(
                new FtpWuicResourceFactory(
                        new AbstractWuicResourceFactory.RegexWuicResourceFactory(null)));
    }

    /**
     * <p>
     * Default factory validating particular properties.
     * </p>
     *
     * @author Guillaume DROUET
     * @version 1.0
     * @since 0.3.1
     */
    public static class FtpWuicResourceFactory extends AbstractWuicResourceFactory {

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
        public FtpWuicResourceFactory(final AbstractWuicResourceFactory toDecorate) {
            super(null);

            delegate = toDecorate;

            // Init default property
            supportedProperties = new HashMap<String, Object>();
            supportedProperties.put(ApplicationConfig.FTP_SERVER_DOMAIN, "localhost");
            supportedProperties.put(ApplicationConfig.FTP_SERVER_PORT, FTPClient.DEFAULT_PORT);
            supportedProperties.put(ApplicationConfig.FTPS_SERVER_PORT, FTPSClient.DEFAULT_FTPS_PORT);
            supportedProperties.put(ApplicationConfig.FTP_SERVER_BASE_PATH, "/");
            supportedProperties.put(ApplicationConfig.FTP_SECRET_PROTOCOL, Boolean.FALSE);
            supportedProperties.put(ApplicationConfig.FTP_USERNAME, null);
            supportedProperties.put(ApplicationConfig.FTP_PASSWORD, null);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void setProperty(final String key, final String value) {

            // Try to override an existing property
            if (!supportedProperties.containsKey(key)) {
                throw new IllegalArgumentException(key + " is not a property which is supported by the FtpWuicResourceFactory");
            } else if (ApplicationConfig.FTP_SERVER_PORT.equals(key) || ApplicationConfig.FTPS_SERVER_PORT.equals(key)) {
                supportedProperties.put(key, Integer.parseInt(value));
            } else if (ApplicationConfig.FTP_SECRET_PROTOCOL.equals(key)) {
                supportedProperties.put(key, Boolean.parseBoolean(value));
            } else {
                supportedProperties.put(key, value);
            }

            final Boolean ftps = (Boolean) supportedProperties.get(ApplicationConfig.FTP_SECRET_PROTOCOL);

            // Set new protocol with the new property
            setWuicProtocol(new FtpWuicResourceProtocol(
                    ftps,
                    (String) supportedProperties.get(ApplicationConfig.FTP_SERVER_DOMAIN),
                    ftps ?  (Integer) supportedProperties.get(ApplicationConfig.FTP_SERVER_PORT)
                            : (Integer) supportedProperties.get(ApplicationConfig.FTP_SERVER_PORT),
                    (String) supportedProperties.get(ApplicationConfig.FTP_SERVER_BASE_PATH),
                    (String) supportedProperties.get(ApplicationConfig.FTP_USERNAME),
                    (String) supportedProperties.get(ApplicationConfig.FTP_PASSWORD)));
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