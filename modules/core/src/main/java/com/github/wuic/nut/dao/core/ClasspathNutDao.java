/*
 * "Copyright (c) 2014   Capgemini Technology Services (hereinafter "Capgemini")
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


package com.github.wuic.nut.dao.core;

import com.github.wuic.config.BooleanConfigParam;
import com.github.wuic.config.ConfigConstructor;
import com.github.wuic.config.IntegerConfigParam;
import com.github.wuic.config.ObjectConfigParam;
import com.github.wuic.config.StringConfigParam;
import com.github.wuic.exception.wrapper.BadArgumentException;
import com.github.wuic.nut.dao.NutDaoService;
import com.github.wuic.nut.setter.ProxyUrisPropertySetter;

import java.net.URL;

/**
 * <p>
 * A {@link com.github.wuic.nut.dao.NutDao} implementation for classpath accesses.
 * </p>
 *
 * @author Guillaume DROUET
 * @version 1.0
 * @since 0.5.0
 */
@NutDaoService
public class ClasspathNutDao extends DiskNutDao {

    /**
     * <p>
     * Builds a new instance with a base directory.
     * </p>
     *
     * @param base                      the directory where we have to look up
     * @param basePathAsSysProp         {@code true} if the base path is a system property
     * @param pollingSeconds            the interval for polling operations in seconds (-1 to deactivate)
     * @param proxies                   the proxies URIs in front of the nut
     * @param regex                     if the path should be considered as a regex or not
     * @param contentBasedVersionNumber {@code true} if version number is computed from nut content, {@code false} if based on timestamp
     */
    @ConfigConstructor
    public ClasspathNutDao(@StringConfigParam(defaultValue = "/", propertyKey = BASE_PATH) final String base,
                           @BooleanConfigParam(defaultValue = false, propertyKey = BASE_PATH_AS_SYS_PROP) final Boolean basePathAsSysProp,
                           @ObjectConfigParam(defaultValue = "", propertyKey = PROXY_URIS, setter = ProxyUrisPropertySetter.class) final String[] proxies,
                           @IntegerConfigParam(defaultValue = -1, propertyKey = POLLING_INTERVAL) final int pollingSeconds,
                           @BooleanConfigParam(defaultValue = false, propertyKey = REGEX) final Boolean regex,
                           @BooleanConfigParam(defaultValue = false, propertyKey = CONTENT_BASED_VERSION_NUMBER) final Boolean contentBasedVersionNumber) {
        super(staticProcessBasePath(base), basePathAsSysProp, proxies, pollingSeconds, regex, contentBasedVersionNumber);
    }

    /**
     * <p>
     * Implementation of the process algorithm in a static method to be called in constructor.
     * </p>
     *
     * @param value the value to process
     * @return the processed value
     */
    private static String staticProcessBasePath(final String value) {
        final URL classPathEntry = ClasspathNutDao.class.getResource(value);

        if (classPathEntry == null) {
            throw new BadArgumentException(new IllegalArgumentException(String.format("%s not found in classpath", value)));
        }

        final String path = classPathEntry.toString();

        return path.substring(path.indexOf(":/") + 1);
    }
}
