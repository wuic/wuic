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

import com.github.wuic.ApplicationConfig;
import com.github.wuic.NutType;
import com.github.wuic.config.BooleanConfigParam;
import com.github.wuic.config.ConfigConstructor;
import com.github.wuic.config.IntegerConfigParam;
import com.github.wuic.config.StringConfigParam;
import com.github.wuic.exception.wrapper.StreamException;
import com.github.wuic.nut.AbstractNutDao;
import com.github.wuic.nut.HttpNut;
import com.github.wuic.nut.Nut;
import com.github.wuic.nut.dao.NutDaoService;
import com.github.wuic.util.IOUtils;
import com.github.wuic.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

/**
 * <p>
 * A {@link com.github.wuic.nut.dao.NutDao} implementation for HTTP accesses.
 * </p>
 *
 * @author Guillaume DROUET
 * @version 1.5
 * @since 0.3.1
 */
@NutDaoService
public class HttpNutDao extends AbstractNutDao {

    /**
     * Default port.
     */
    private static final int DEFAULT_PORT = 80;

    /**
     * Logger.
     */
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    /**
     * The URL (protocol, domain, port, base path) to prefix the nut name.
     */
    private String baseUrl;

    /**
     * <p>
     * Builds a new instance thanks to the specified HTTP information.
     * </p>
     *
     * @param https use HTTPS protocol instead of HTTP ?
     * @param domain the HTTP server domain name
     * @param port the HTTP server port
     * @param path the base path where nuts are provided
     * @param basePathAsSysProp {@code true} if the base path is a system property
     * @param pollingSeconds the interval for polling operations in seconds (-1 to deactivate)
     * @param contentBasedVersionNumber  {@code true} if version number is computed from nut content, {@code false} if based on timestamp
     */
    @ConfigConstructor
    public HttpNutDao(@BooleanConfigParam(defaultValue = false, propertyKey = ApplicationConfig.SECRET_PROTOCOL)final Boolean https,
                      @StringConfigParam(defaultValue = "localhost", propertyKey = ApplicationConfig.SERVER_DOMAIN) final String domain,
                      @IntegerConfigParam(defaultValue = DEFAULT_PORT, propertyKey = ApplicationConfig.SERVER_PORT) final Integer port,
                      @StringConfigParam(defaultValue = "", propertyKey = ApplicationConfig.BASE_PATH) final String path,
                      @BooleanConfigParam(defaultValue = false, propertyKey = ApplicationConfig.BASE_PATH_AS_SYS_PROP) final Boolean basePathAsSysProp,
                      @IntegerConfigParam(defaultValue = -1, propertyKey = ApplicationConfig.POLLING_INTERVAL) final int pollingSeconds,
                      @BooleanConfigParam(defaultValue = false, propertyKey = ApplicationConfig.BASE_PATH_AS_SYS_PROP) final Boolean contentBasedVersionNumber) {
        super(path, basePathAsSysProp, null, pollingSeconds, contentBasedVersionNumber);
        final StringBuilder builder = new StringBuilder().append(https ? "https://" : "http://").append(domain);

        if (port != null) {
            builder.append(":").append(port);
        }

        builder.append("/");

        baseUrl = builder.toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected List<String> listNutsPaths(final String pattern) throws StreamException {
        // Finding nuts with a regex through HTTP protocol is tricky
        // Until this feature is implemented, we only expect pattern that represent a real nut
        return Arrays.asList(pattern);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Nut accessFor(final String realPath, final NutType type) throws StreamException {
        final String p = IOUtils.mergePath(baseUrl, IOUtils.mergePath(StringUtils.simplifyPathWithDoubleDot(IOUtils.mergePath(getBasePath(), realPath))));
        log.debug("Opening HTTP access for {}", p);

        try {
            final URL url = new URL(p);
            return new HttpNut(realPath, url, type, getVersionNumber(p));
        } catch (IOException ioe) {
            throw new StreamException(ioe);
        }
    }

    /**
     * Gets the 'LastModified' header from the given URL.
     */
    private Long getLastUpdateTimestampFor(final URL url) throws StreamException {
        try {
            return url.openConnection().getLastModified();
        } catch (IOException ioe) {
            throw new StreamException(ioe);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Long getLastUpdateTimestampFor(final String path) throws StreamException {
        final String url = IOUtils.mergePath(baseUrl, getBasePath(), path);
        log.debug("Polling HTTP nut for {}", url);

        try {
            return getLastUpdateTimestampFor(new URL(url));
        } catch (IOException ioe) {
            throw new StreamException(ioe);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public InputStream newInputStream(final String path) throws StreamException {
        try {
            return new URL(path).openStream();
        } catch (IOException ioe) {
            throw new StreamException(ioe);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Boolean exists(final String path) throws StreamException {
        final String url = IOUtils.mergePath(baseUrl, getBasePath(), path);
        HttpURLConnection.setFollowRedirects(false);

        try {
            final HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
            con.setRequestMethod("HEAD");
            return (con.getResponseCode() == HttpURLConnection.HTTP_OK);
        } catch (IOException ioe) {
            throw new StreamException(ioe);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return String.format("%s with base URL %s", getClass().getName(), IOUtils.mergePath(baseUrl, getBasePath()));
    }
}
