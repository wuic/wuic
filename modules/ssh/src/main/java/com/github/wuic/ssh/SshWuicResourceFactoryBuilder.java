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


package com.github.wuic.ssh;

import com.github.wuic.ApplicationConfig;
import com.github.wuic.exception.WuicRfPropertyNotSupportedException;
import com.github.wuic.exception.wrapper.BadArgumentException;
import com.github.wuic.resource.WuicResourceFactory;
import com.github.wuic.resource.WuicResourceFactoryBuilder;
import com.github.wuic.resource.impl.AbstractWuicResourceFactory;
import com.github.wuic.resource.impl.AbstractWuicResourceFactoryBuilder;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * <p>
 * Builder for resource access on a FTP server.
 * </p>
 *
 * @author Guillaume DROUET
 * @version 1.1
 * @since 0.3.1
 */
public class SshWuicResourceFactoryBuilder extends AbstractWuicResourceFactoryBuilder {

    /**
     * <p>
     * Creates a new instance.
     * </p>
     */
    public SshWuicResourceFactoryBuilder() {
        this(new SshWuicResourceFactory());
    }

    /**
     * <p>
     * Creates a new instance thanks to an already built factory.
     * </p>
     *
     * @param built the already built factory.
     */
    public SshWuicResourceFactoryBuilder(final WuicResourceFactory built) {
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
        return new SshWuicResourceFactoryBuilder(new SshWuicResourceFactory());
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
    public static class SshWuicResourceFactory extends AbstractWuicResourceFactory {

        /**
         * Default SSH port.
         */
        private static final int DEFAULT_PORT = 22;

        /**
         * Default time to sleep after the thread as executed the command.
         */
        private static final long DEFAULT_TIME_TO_SLEEP_AFTER_EXEC = 3000L;

        /**
         * Supported properties with their default value.
         */
        private Map<String, Object> supportedProperties;

        /**
         * <p>
         * Builds a new instance.
         * </p>
         */
        public SshWuicResourceFactory() {
            super(null);

            // Init default property
            supportedProperties = new HashMap<String, Object>();
            supportedProperties.put(ApplicationConfig.SSH_SERVER_DOMAIN, "localhost");
            supportedProperties.put(ApplicationConfig.SSH_SERVER_PORT, DEFAULT_PORT);
            supportedProperties.put(ApplicationConfig.SSH_SERVER_BASE_PATH, ".");
            supportedProperties.put(ApplicationConfig.SSH_SERVER_BASE_PATH_AS_SYS_PROP, Boolean.FALSE);
            supportedProperties.put(ApplicationConfig.SSH_USERNAME, null);
            supportedProperties.put(ApplicationConfig.SSH_PASSWORD, null);
            supportedProperties.put(ApplicationConfig.SSH_INTERPRETER, "/bin/sh");
            supportedProperties.put(ApplicationConfig.SSH_TIME_TO_SLEEP_AFTER_EXEC, DEFAULT_TIME_TO_SLEEP_AFTER_EXEC);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void setProperty(final String key, final String value) throws WuicRfPropertyNotSupportedException {

            // Try to override an existing property
            if (!supportedProperties.containsKey(key)) {
                throw new WuicRfPropertyNotSupportedException(key, this.getClass());
            } else if (ApplicationConfig.SSH_SERVER_PORT.equals(key)) {
                supportedProperties.put(key, Integer.parseInt(value));
            } else if (ApplicationConfig.SSH_SERVER_BASE_PATH_AS_SYS_PROP.equals(key)) {
                supportedProperties.put(key, Boolean.parseBoolean(value));
            } else if (ApplicationConfig.SSH_TIME_TO_SLEEP_AFTER_EXEC.equals(key)) {
                supportedProperties.put(key, Long.parseLong(value));
            } else {
                supportedProperties.put(key, value);
            }

            final Boolean sysProp = (Boolean) supportedProperties.get(ApplicationConfig.SSH_SERVER_BASE_PATH_AS_SYS_PROP);
            final String basePathProp = supportedProperties.get(ApplicationConfig.SSH_SERVER_BASE_PATH).toString();
            final String basePath = sysProp ? System.getProperty(basePathProp) : basePathProp;
            final SshCommandManager manager;

            if ("/bin/sh".equals(supportedProperties.get(ApplicationConfig.SSH_INTERPRETER))) {
                manager = new ShellSshCommandManager();
            } else if ("cmd.exe".equals(supportedProperties.get(ApplicationConfig.SSH_INTERPRETER))) {
                manager = new CmdSshCommandManager();
            } else {
                throw new BadArgumentException(new IllegalArgumentException(String.format("%s is not a supported interpreter.", supportedProperties.get(ApplicationConfig.SSH_INTERPRETER))));
            }

            // Set new protocol with the new property
            setWuicProtocol(new SshWuicResourceProtocol(
                    (String) supportedProperties.get(ApplicationConfig.SSH_SERVER_DOMAIN),
                    (Integer) supportedProperties.get(ApplicationConfig.SSH_SERVER_PORT),
                    basePath,
                    (String) supportedProperties.get(ApplicationConfig.SSH_USERNAME),
                    (String) supportedProperties.get(ApplicationConfig.SSH_PASSWORD),
                    manager,
                    (Long) supportedProperties.get(ApplicationConfig.SSH_TIME_TO_SLEEP_AFTER_EXEC)));
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Pattern getPattern(final String path) {
            return Pattern.compile(path);
        }
    }
}