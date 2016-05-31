/*
 * "Copyright (c) 2016   Capgemini Technology Services (hereinafter "Capgemini")
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
 * @since 0.3.1
 */
public interface ApplicationConfig {

    /**
     * If this constant is set as a system property, WUIC will use "line.separator" property to write line delimiter.
     * Otherwise \n will be used.
     */
    String USE_SYSTEM_LINE_SEPARATOR = "wuic.useSystemLineSeparator";

    /**
     * Basic prefix for all properties.
     */
    String PREFIX = "c.g.wuic.";

    /**
     * Prefix for engine properties.
     */
    String ENGINE_PREFIX = PREFIX + "engine.";

    /**
     * Prefix for filter properties.
     */
    String FILTER_PREFIX = PREFIX + "filter.";

    /**
     * Prefix for DAO properties.
     */
    String DAO_PREFIX = PREFIX + "dao.";

    /**
     * Prefix for facade properties.
     */
    String FACADE_PREFIX = PREFIX + "facade.";

    /**
     * The fixed version number.
     */
    String FIXED_VERSION_NUMBER = PREFIX + "fixedVersionNumber";

    /**
     * Authorize WUIC to compute version number asynchronously or not.
     */
    String COMPUTE_VERSION_ASYNCHRONOUSLY = PREFIX + "computeVersionAsynchronously";

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
    String BASE_PATH = DAO_PREFIX + "basePath";

    /**
     * <p>
     * Uses the {@code include} method from {@code RequestDispatcher} to create nut from any path matching the corresponding pattern.
     * </p>
     */
    String USE_INCLUDE_FOR_PATH_PATTERN = DAO_PREFIX + "useIncludeForPathPattern";

    /**
     * Polling interval for nuts refresher.
     */
    String POLLING_INTERVAL = DAO_PREFIX + "pollingInterval";

    /**
     * Boolean which indicates if path are evaluated as regex or not.
     */
    String REGEX = DAO_PREFIX + "regex";

    /**
     * Boolean which indicates if path are evaluated as wildcard or not.
     */
    String WILDCARD = DAO_PREFIX + "wildcard";

    /**
     * Proxies that can be use to access the nuts.
     */
    String PROXY_URIS = DAO_PREFIX + "proxyUris";

    /**
     * The server's domain when accessing nuts remotely.
     */
    String SERVER_DOMAIN = DAO_PREFIX + "serverDomain";

    /**
     * The server's port when accessing nuts remotely.
     */
    String SERVER_PORT = DAO_PREFIX + "serverPort";

    /**
     * The bucket usually defined in cloud storage services.
     */
    String CLOUD_BUCKET = DAO_PREFIX + "cloudBucket";

    /**
     * Use the secured version of the protocol.
     */
    String SECRET_PROTOCOL = DAO_PREFIX + "secret";

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
    String LOGIN = DAO_PREFIX + "login";

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
    String PASSWORD = DAO_PREFIX + "password";

    /**
     * <p>
     * Tells the DAO to computes version number not with the last modification timestamp but with the whole content.
     * </p>
     */
    String CONTENT_BASED_VERSION_NUMBER = DAO_PREFIX + "contentBasedVersionNumber";

    /**
     * <p>
     * If the underlying API does not provides a way to create input stream, this setting can be used to indicates that
     * resources should be stored on disk and not in an in memory byte array.
     * </p>
     */
    String DOWNLOAD_TO_DISK = DAO_PREFIX + "downloadToDisk";

    /**
     * <p>
     * Indicates if the engine should cache or not.
     * </p>
     */
    String CACHE = ENGINE_PREFIX + "cache";

    /**
     * Indicates the time to live for a cache.
     */
    String TIME_TO_LIVE = ENGINE_PREFIX + "timeToLive";

    /**
     * <p>
     * Indicates a particular {@link com.github.wuic.engine.CacheProvider} implementation.
     * </p>
     */
    String CACHE_PROVIDER_CLASS = ENGINE_PREFIX + "cacheProviderClass";

    /**
     * <p>
     * Specify the best effort mode.
     * </p>
     */
    String BEST_EFFORT = ENGINE_PREFIX + "bestEffort";

    /**
     * <p>
     * Indicates if the engine should compress or not.
     * </p>
     */
    String COMPRESS = ENGINE_PREFIX + "compress";

    /**
     * <p>
     * Indicates if the engine should convert or not.
     * </p>
     */
    String CONVERT = ENGINE_PREFIX + "convert";

    /**
     * <p>
     * Indicates if the engine should use NodeJS command line or not.
     * </p>
     */
    String USE_NODE_JS = ENGINE_PREFIX + "useNodeJs";

    /**
     * <p>
     * Indicates the ECMA script version the engine should use to compile javascript.
     * </p>
     */
    String ECMA_SCRIPT_VERSION = ENGINE_PREFIX + "ecmaScriptVersion";

    /**
     * <p>
     * Indicates if the engine should aggregate or not.
     * </p>
     */
    String AGGREGATE = ENGINE_PREFIX + "aggregate";

    /**
     * <p>
     * Indicates if the engine should inspects and eventually transform or not.
     * </p>
     */
    String INSPECT = ENGINE_PREFIX + "inspect";

    /**
     * <p>
     * Indicates the class name of a {@link com.github.wuic.engine.DimensionPacker} implementation to use when
     * aggregating images.
     * </p>
     */
    String PACKER_CLASS_NAME = ENGINE_PREFIX + "packerClassName";

    /**
     * <p>
     * Indicates the class name of a {@link com.github.wuic.engine.SpriteProvider} implementation to use when
     * aggregating images.
     * </p>
     */
    String SPRITE_PROVIDER_CLASS_NAME = ENGINE_PREFIX + "spriteProviderClassName";

    /**
     * <p>
     * Position where \n is inserted in text compressor.
     * </p>
     */
    String LINE_BREAK_POS = ENGINE_PREFIX + "lineBreakPos";

    /**
     * <p>
     * Indicates if javascript processor should obfuscate code.
     * </p>
     */
    String OBFUSCATE = ENGINE_PREFIX + "obfuscate";

    /**
     * <p>
     * Indicates if javascript processor should preserve unnecessary semicolons.
     * </p>
     */
    String PRESERVE_SEMICOLONS = ENGINE_PREFIX + "preserveSemiColons";

    /**
     * <p>
     * Indicates if html processor should preserve line break position.
     * </p>
     */
    String PRESERVE_LINE_BREAK = ENGINE_PREFIX + "preserveLineBreakPos";

    /**
     * <p>
     * Indicates if javascript processor should not apply extra optimizations.
     * </p>
     */
    String DISABLE_OPTIMIZATIONS = ENGINE_PREFIX + "disableOptimizations";

    /**
     * <p>
     * Indicates if the engine should be verbose when processing nuts.
     * </p>
     */
    String VERBOSE = ENGINE_PREFIX + "verbose";

    /**
     * <p>
     * Indicates the charset for text processor.
     * </p>
     */
    String CHARSET = PREFIX + "charset";

    /**
     * <p>
     * Indicates if the server hint is enabled or not.
     * </p>
     */
    String SERVER_HINT = ENGINE_PREFIX + "serverHint";

    /**
     * <p>
     * A pattern wrapping a value to capture.
     * </p>
     */
    String WRAP_PATTERN = ENGINE_PREFIX + "wrapPattern";

    /**
     * <p>
     * Command line to be executed.
     * </p>
     */
    String COMMAND = ENGINE_PREFIX + "command";

    /**
     * <p>
     * Input nut type.
     * </p>
     */
    String INPUT_NUT_TYPE = ENGINE_PREFIX + "inputNutType";

    /**
     * <p>
     * Output nut type.
     * </p>
     */
    String OUTPUT_NUT_TYPE = ENGINE_PREFIX + "outputNutType";

    /**
     * The path separator.
     */
    String PATH_SEPARATOR = ENGINE_PREFIX + "pathSeparator";

    /**
     * A comma separated list of resources paths available in the classpath.
     * Those libraries will be copied to the working directory where the command line is executed.
     */
    String LIBRARIES = ENGINE_PREFIX + "libraries";

    /**
     * A boolean value that tells an engine to generate files in the same directory as the one containing source file.
     * This allows to avoid copy of source files to a temporary directory (with the assumption that sources and generated
     * files should be in the same directory).
     */
    String RESOLVED_FILE_DIRECTORY_AS_WORKING_DIR = ENGINE_PREFIX + "resolvedFileDirectoryAsWorkingDirectory";

    /**
     * <p>
     * Indicates all the regex expressions configured in a filter.
     * </p>
     */
    String REGEX_EXPRESSIONS = FILTER_PREFIX + "regexExpressions";

    /**
     * <p>
     * Specify the filter activation.
     * </p>
     */
    String ENABLE = FILTER_PREFIX + "enable";

    /**
     * Init parameter which indicates if configurations injected by tag supports (JSP, Thymeleaf, etc) should be done
     * each time a page is processed or not.
     */
    String WUIC_SERVLET_MULTIPLE_CONG_IN_TAG_SUPPORT = PREFIX + "facade.multipleConfigInTagSupport";

    /**
     * Init parameter which indicates the WUIC context path.
     */
    String WUIC_SERVLET_CONTEXT_PARAM = FACADE_PREFIX + "contextPath";

    /**
     * Init parameter which indicates the WUIC property file.
     */
    String WUIC_PROPERTIES_PATH_PARAM = FACADE_PREFIX + "propertiesPath";

    /**
     * Init parameter which indicates the WUIC xml file.
     */
    String WUIC_SERVLET_XML_PATH_PARAM = FACADE_PREFIX + "xmlPath";

    /**
     * Init parameter which indicates the WUIC json file.
     */
    String WUIC_SERVLET_JSON_PATH_PARAM = FACADE_PREFIX + "jsonPath";

    /**
     * A list of comma-separated class names implementing {@link com.github.wuic.config.ObjectBuilderInspector} with
     * a default constructor that will be used in the facade.
     */
    String WUIC_ADDITIONAL_BUILDER_INSPECTOR = FACADE_PREFIX + "additionalBuilderInspectorClasses";

    /**
     * A list of comma-separated class names extending {@link com.github.wuic.context.ContextBuilderConfigurator} with
     * a default constructor that will be used in the facade.
     */
    String WUIC_ADDITIONAL_BUILDER_CONFIGURATORS = FACADE_PREFIX + "additionalBuilderConfiguratorClasses";

    /**
     * Init parameter which indicates to use or not context builder configurators which inject default DAOs and engines.
     */
    String WUIC_USE_DEFAULT_CONTEXT_BUILDER_CONFIGURATORS = FACADE_PREFIX + "useDefaultContextBuilderConfigurators";

    /**
     * Which warmup strategy to use when {@link WuicFacade} initializes the context. Possible values are enumerated by
     * {@link com.github.wuic.WuicFacade.WarmupStrategy}.
     */
    String WUIC_WARMUP_STRATEGY = FACADE_PREFIX + "warmupStrategy";

    /**
     * This is the property name of the {@link WuicFacade} instance shared inside a web context.
     */
    String WEB_WUIC_FACADE = PREFIX + "webWuicFacade";

    /**
     * This class provides the {@link com.github.wuic.util.BiFunction} that provides parameters.
     */
    String INIT_PARAM_FUNCTION = PREFIX + "initParameterClass";
}
