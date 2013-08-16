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


package com.github.wuic;

/**
 * <p>
 * All the application configurations supported by WUIC are defined here.
 * </p>
 *
 * @author Guillaume DROUET
 * @version 1.2
 * @since 0.3.1
 */
public interface ApplicationConfig {

    /**
     * <p>
     * The base path when accessing resources.
     * </p>
     *
     * <p>
     * For the Google Storage path for search resources without bucket name, example :
     * empty : search in root and sub directories
     * wuic : search in "wuic/*"
     * wuic/test : search in "wuic/test/*"
     * </p>
     */
    String BASE_PATH = "c.g.wuic.dao.basePath";

    /**
     * Consider the base path as a system property associated to actual value.
     */
    String BASE_PATH_AS_SYS_PROP = "c.g.wuic.dao.basePathAsSystemProperty";

    /**
     * Polling interleave for resources refresher.
     */
    String POLLING_INTERLEAVE = "c.g.wuic.dao.pollingInterleave";

    /**
     * Boolean which indicates if path are evaluated as regex or not.
     */
    String REGEX = "c.g.wuic.dao.regex";

    /**
     * Proxies that can be use to access the resources.
     */
    String PROXY_URIS = "c.g.wuic.dao.proxyUris";

    /**
     * The server's domain when accessing resources remotely.
     */
    String SERVER_DOMAIN = "c.g.wuic.dao.serverDomain";

    /**
     * The server's port when accessing resources remotely.
     */
    String SERVER_PORT = "c.g.wuic.dao.serverPort";

    /**
     * The bucket usually defined in cloud storage services.
     */
    String CLOUD_BUCKET = "c.g.wuic.dao.cloudBucket";

    /**
     * Use the secured version of the protocol.
     */
    String SECRET_PROTOCOL = "c.g.wuic.dao.secret";

    /**
     * <p>
     * The login to use when accessing resources protected by authentication.
     * </p>
     *
     * <p>
     * For Google Cloud API client id, you can generate a new key on this page :
     * https://code.google.com/apis/console/
     * - Go on : API Access > "Create another client ID..."
     * - Installed application type : select "Service account"
     * - Copy here the "Email address" field, "xxxxx-yyyyy@developer.gserviceaccount.com
     * </p>
     */
    String LOGIN = "c.g.wuic.dao.login";

    /**
     * <p>
     * The password to use when accessing resources protected by authentication.
     * </p>
     *
     * <p>
     * You can generate a new key path on this page : https://code.google.com/apis/console/
     * - Go on : API Access > "Create another client ID..."
     * - Installed application type : select "Service account"
     * - Download key path or Generate new key
     * </p>
     */
    String PASSWORD = "c.g.wuic.dao.password";

    /**
     * <p>
     * Indicates if the engine should cache or not.
     * </p>
     */
    String CACHE = "c.g.wuic.engine.cache";

    /**
     * <p>
     * Indicates a particular
     * </p>
     */
    String CACHE_PROVIDER_CLASS = "c.g.wuic.engine.cacheProviderClass";

    /**
     * <p>
     * Indicates if the engine should compress or not.
     * </p>
     */
    String COMPRESS = "c.g.wuic.engine.compress";

    /**
     * <p>
     * Indicates if the engine should aggregate or not.
     * </p>
     */
    String AGGREGATE = "c.g.wuic.engine.aggregate";

    /**
     * <p>
     * Position where \n is inserted in text compressor.
     * </p>
     */
    String LINE_BREAK_POS = "c.g.wuic.engine.lineBreakPos";

    /**
     * <p>
     * Indicates if javascript processor should obfuscate code.
     * </p>
     */
    String OBFUSCATE = "c.g.wuic.engine.obfuscate";

    /**
     * <p>
     * Indicates if javascript processor should preserve unnecessary semicolons.
     * </p>
     */
    String PRESERVE_SEMICOLONS = "c.g.wuic.engine.preserveSemiColons";

    /**
     * <p>
     * Indicates if javascript processor should not apply extra optimizations.
     * </p>
     */
    String DISABLE_OPTIMIZATIONS = "c.g.wuic.engine.disableOptimizations";

    /**
     * <p>
     * Indicates if the engine should be verbose when processing resources.
     * </p>
     */
    String VERBOSE = "c.g.wuic.engine.verbose";

    /**
     * <p>
     * Indicates the charset for text processor.
     * </p>
     */
    String CHARSET = "c.g.wuic.engine.charset";
}
