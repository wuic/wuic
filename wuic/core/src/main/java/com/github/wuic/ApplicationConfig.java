/*
 * "Copyright (c) 2015   Capgemini Technology Services (hereinafter "Capgemini")
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
 * @version 1.4
 * @since 0.3.1
 */
public interface ApplicationConfig {

    /**
     * Authorize WUIC to compute version number asynchronously or not.
     */
    String COMPUTE_VERSION_ASYNCHRONOUSLY = "c.g.wuic.computeVersionAsynchronously";

    /**
     * <p>
     * The base path when accessing nuts.
     * </p>
     *
     * <p>
     * For the Google Storage path for search nuts without bucket name, example :
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
     * Polling interval for nuts refresher.
     */
    String POLLING_INTERVAL = "c.g.wuic.dao.pollingInterval";

    /**
     * Boolean which indicates if path are evaluated as regex or not.
     */
    String REGEX = "c.g.wuic.dao.regex";

    /**
     * Boolean which indicates if path are evaluated as wildcard or not.
     */
    String WILDCARD = "c.g.wuic.dao.wildcard";

    /**
     * Proxies that can be use to access the nuts.
     */
    String PROXY_URIS = "c.g.wuic.dao.proxyUris";

    /**
     * The server's domain when accessing nuts remotely.
     */
    String SERVER_DOMAIN = "c.g.wuic.dao.serverDomain";

    /**
     * The server's port when accessing nuts remotely.
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
     * The login to use when accessing nuts protected by authentication.
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
     * The password to use when accessing nuts protected by authentication.
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
     * Tells the DAO to computes version number not with the last modification timestamp but with the whole content.
     * </p>
     */
    String CONTENT_BASED_VERSION_NUMBER = "c.g.wuic.dao.contentBasedVersionNumber";

    /**
     * <p>
     * If the underlying API does not provides a way to create input stream, this setting can be used to indicates that
     * resources should be stored on disk and not in an in memory byte array.
     * </p>
     */
    String DOWNLOAD_TO_DISK = "c.g.wuic.dao.downloadToDisk";

    /**
     * <p>
     * Indicates if the engine should cache or not.
     * </p>
     */
    String CACHE = "c.g.wuic.engine.cache";

    /**
     * Indicates the time to live for a cache.
     */
    String TIME_TO_LIVE = "c.g.wuic.engine.timeToLive";

    /**
     * <p>
     * Indicates a particular {@link com.github.wuic.engine.CacheProvider} implementation.
     * </p>
     */
    String CACHE_PROVIDER_CLASS = "c.g.wuic.engine.cacheProviderClass";

    /**
     * <p>
     * Specify the best effort mode.
     * </p>
     */
    String BEST_EFFORT = "c.g.wuic.engine.bestEffort";

    /**
     * <p>
     * Indicates if the engine should compress or not.
     * </p>
     */
    String COMPRESS = "c.g.wuic.engine.compress";

    /**
     * <p>
     * Indicates if the engine should convert or not.
     * </p>
     */
    String CONVERT = "c.g.wuic.engine.convert";

    /**
     * <p>
     * Indicates if the engine should use NodeJS command line or not.
     * </p>
     */
    String USE_NODE_JS = "c.g.wuic.engine.useNodeJs";

    /**
     * <p>
     * Indicates the ECMA script version the engine should use to compile javascript.
     * </p>
     */
    String ECMA_SCRIPT_VERSION = "c.g.wuic.engine.ecmaScriptVersion";

    /**
     * <p>
     * Indicates if the engine should aggregate or not.
     * </p>
     */
    String AGGREGATE = "c.g.wuic.engine.aggregate";

    /**
     * <p>
     * Indicates if the engine should inspects and eventually transform or not.
     * </p>
     */
    String INSPECT = "c.g.wuic.engine.inspect";

    /**
     * <p>
     * Indicates the class name of a {@link com.github.wuic.engine.DimensionPacker} implementation to use when
     * aggregating images.
     * </p>
     */
    String PACKER_CLASS_NAME = "c.g.wuic.engine.packerClassName";

    /**
     * <p>
     * Indicates the class name of a {@link com.github.wuic.engine.SpriteProvider} implementation to use when
     * aggregating images.
     * </p>
     */
    String SPRITE_PROVIDER_CLASS_NAME = "c.g.wuic.engine.spriteProviderClassName";

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
     * Indicates if html processor should preserve line break position.
     * </p>
     */
    String PRESERVE_LINE_BREAK = "c.g.wuic.engine.preserveLineBreakPos";

    /**
     * <p>
     * Indicates if javascript processor should not apply extra optimizations.
     * </p>
     */
    String DISABLE_OPTIMIZATIONS = "c.g.wuic.engine.disableOptimizations";

    /**
     * <p>
     * Indicates if the engine should be verbose when processing nuts.
     * </p>
     */
    String VERBOSE = "c.g.wuic.engine.verbose";

    /**
     * <p>
     * Indicates the charset for text processor.
     * </p>
     */
    String CHARSET = "c.g.wuic.engine.charset";

    /**
     * A pattern wrapping a value to capture.
     */
    String WRAP_PATTERN = "c.g.wuic.engine.wrapPattern";

    /**
     * <p>
     * Indicates all the regex expressions configured in a filter.
     * </p>
     */
    String REGEX_EXPRESSIONS = "c.g.wuic.filter.regexExpressions";

    /**
     * <p>
     * Specify the filter activation.
     * </p>
     */
    String ENABLE = "c.g.wuic.filter.enable";

    /**
     * Init parameter which indicates if configurations injected by tag supports (JSP, Thymeleaf, etc) should be done
     * each time a page is processed or not.
     */
    String WUIC_SERVLET_MULTIPLE_CONG_IN_TAG_SUPPORT = "c.g.wuic.facade.multipleConfigInTagSupport";

    /**
     * Init parameter which indicates the WUIC context path.
     */
    String WUIC_SERVLET_CONTEXT_PARAM = "c.g.wuic.facade.contextPath";

    /**
     * Init parameter which indicates the WUIC xml file.
     */
    String WUIC_SERVLET_XML_PATH_PARAM = "c.g.wuic.facade.xmlPath";

    /**
     * Init parameter which indicates that the WUIC context path is a system property.
     */
    String WUIC_SERVLET_XML_SYS_PROP_PARAM = "c.g.wuic.facade.xmlPathAsSystemProperty";

    /**
     * Init parameter which indicates to use or not context builder configurators which inject default DAOs and engines.
     */
    String WUIC_USE_DEFAULT_CONTEXT_BUILDER_CONFIGURATORS = "c.g.w.useDefaultContextBuilderConfigurators";

    /**
     * Which warmup strategy to use when {@link WuicFacade} initializes the context. Possible values are enumerated by
     * {@link com.github.wuic.WuicFacade.WarmupStrategy}.
     */
    String WUIC_WARMUP_STRATEGY = "c.g.wuic.facade.warmupStrategy";

    /**
     * This is the property name of the {@link WuicFacade} instance shared inside a web context.
     */
    String WEB_WUIC_FACADE = "c.g.wuic.webWuicFacade";

    /**
     * This class provides the {@link com.github.wuic.util.BiFunction} that provides parameters.
     */
    String INIT_PARAM_FUNCTION = "c.g.wuic.initParameterClass";
}
