/*
 * Copyright (c) 2016   The authors of WUIC
 *
 * License/Terms of Use
 * Permission is hereby granted, free of charge and for the term of intellectual
 * property rights on the Software, to any person obtaining a copy of this software
 * and associated documentation files (the "Software"), to use, copy, modify and
 * propagate free of charge, anywhere in the world, all or part of the Software
 * subject to the following mandatory conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, PEACEFUL ENJOYMENT,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS
 * OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */


package com.github.wuic.nut.dao.core;

import com.github.wuic.ApplicationConfig;
import com.github.wuic.NutType;
import com.github.wuic.ProcessContext;
import com.github.wuic.config.Alias;
import com.github.wuic.config.BooleanConfigParam;
import com.github.wuic.config.Config;
import com.github.wuic.config.IntegerConfigParam;
import com.github.wuic.config.StringConfigParam;
import com.github.wuic.nut.AbstractNutDao;
import com.github.wuic.nut.HttpNut;
import com.github.wuic.nut.Nut;
import com.github.wuic.nut.dao.NutDaoService;
import com.github.wuic.util.DefaultInput;
import com.github.wuic.util.IOUtils;
import com.github.wuic.util.Input;
import com.github.wuic.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
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
 * @since 0.3.1
 */
@NutDaoService
@Alias("http")
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
     * Initializes a new instance thanks to the specified HTTP information.
     * </p>
     *
     * @param https use HTTPS protocol instead of HTTP ?
     * @param domain the HTTP server domain name
     * @param port the HTTP server port
     * @param path the base path where nuts are provided
     * @param pollingSeconds the interval for polling operations in seconds (-1 to deactivate)
     */
    @Config
    public void init(@BooleanConfigParam(defaultValue = false, propertyKey = ApplicationConfig.SECRET_PROTOCOL)final Boolean https,
                     @StringConfigParam(defaultValue = "localhost", propertyKey = ApplicationConfig.SERVER_DOMAIN) final String domain,
                     @IntegerConfigParam(defaultValue = DEFAULT_PORT, propertyKey = ApplicationConfig.SERVER_PORT) final Integer port,
                     @StringConfigParam(defaultValue = "", propertyKey = ApplicationConfig.BASE_PATH) final String path,
                     @IntegerConfigParam(defaultValue = -1, propertyKey = ApplicationConfig.POLLING_INTERVAL) final int pollingSeconds) {
        init(path, null, pollingSeconds);
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
    protected List<String> listNutsPaths(final String pattern) throws IOException {
        // Finding nuts with a regex through HTTP protocol is tricky
        // Until this feature is implemented, we only expect pattern that represent a real nut
        return Arrays.asList(pattern);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Nut accessFor(final String realPath, final NutType type, final ProcessContext processContext) throws IOException {
        final String p = IOUtils.mergePath(baseUrl, IOUtils.mergePath(StringUtils.simplifyPathWithDoubleDot(IOUtils.mergePath(getBasePath(), realPath))));
        log.debug("Opening HTTP access for {}", p);
        final URL url = new URL(p);
        return new HttpNut(realPath, url, type, getVersionNumber(realPath, processContext));
    }

    /**
     * Gets the 'LastModified' header from the given URL.
     *
     * @throws IOException if any I/O error occurs
     */
    private Long getLastUpdateTimestampFor(final URL url) throws IOException {
        return url.openConnection().getLastModified();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Long getLastUpdateTimestampFor(final String path) throws IOException {
        final String url = IOUtils.mergePath(baseUrl, getBasePath(), path);
        log.debug("Polling HTTP nut for {}", url);
        return getLastUpdateTimestampFor(new URL(url));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Input newInputStream(final String path, final ProcessContext processContext) throws IOException {
        return newInput(new URL(path).openStream());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Boolean exists(final String path, final ProcessContext processContext) throws IOException {
        final String url = IOUtils.mergePath(baseUrl, getBasePath(), path);
        HttpURLConnection.setFollowRedirects(false);

        final HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
        con.setRequestMethod("HEAD");
        return (con.getResponseCode() == HttpURLConnection.HTTP_OK);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return String.format("%s with base URL %s", getClass().getName(), IOUtils.mergePath(baseUrl, getBasePath()));
    }
}
