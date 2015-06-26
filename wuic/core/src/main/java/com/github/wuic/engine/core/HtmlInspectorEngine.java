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


package com.github.wuic.engine.core;

import com.github.wuic.ApplicationConfig;
import com.github.wuic.Logging;
import com.github.wuic.NutType;
import com.github.wuic.config.BooleanConfigParam;
import com.github.wuic.config.ConfigConstructor;
import com.github.wuic.config.StringConfigParam;
import com.github.wuic.engine.EngineRequest;
import com.github.wuic.engine.EngineRequestBuilder;
import com.github.wuic.engine.EngineService;
import com.github.wuic.engine.EngineType;
import com.github.wuic.engine.HeadEngine;
import com.github.wuic.engine.NodeEngine;
import com.github.wuic.exception.WorkflowNotFoundException;
import com.github.wuic.exception.WuicException;
import com.github.wuic.nut.ConvertibleNut;
import com.github.wuic.nut.dao.NutDao;
import com.github.wuic.nut.NutsHeap;
import com.github.wuic.nut.ByteArrayNut;
import com.github.wuic.nut.dao.core.ProxyNutDao;
import com.github.wuic.nut.filter.NutFilter;
import com.github.wuic.nut.filter.NutFilterHolder;
import com.github.wuic.util.NutUtils;
import com.github.wuic.util.Pipe;
import com.github.wuic.util.TerFunction;
import com.github.wuic.util.CollectionUtils;
import com.github.wuic.util.HtmlUtil;
import com.github.wuic.util.IOUtils;
import com.github.wuic.util.NumberUtils;
import com.github.wuic.util.StringUtils;

import com.github.wuic.util.UrlProvider;
import com.github.wuic.util.UrlUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <p>
 * This engine is able to parse an HTML to collect all CSS and JS scripts, refer them through a heap and set
 * reference to associated workflow URL in DOM.
 * </p>
 *
 * <p>
 * Note that version number is based on content hash because this is the unique strategy that applies to inline scripts.
 * </p>
 *
 * <p>
 * When a {@link ConvertibleNut} is parsed, the registered a transformer which is able to cache transformation operations
 * is added for future usages.
 * </p>
 *
 * <p>
 * If the result of the transformation performed by this engine is not served by WUIC, it means that some information
 * like caching or hints won't be specified in the HTTP response headers. In that case, this engines fallback to the
 * Application cache and the "link" tags mechanism by modifying the HTML content.
 * </p>
 *
 * @author Guillaume DROUET
 * @version 1.1
 * @since 0.4.4
 */
@EngineService(injectDefaultToWorkflow = true, isCoreEngine = true)
public class HtmlInspectorEngine extends NodeEngine implements NutFilterHolder {

    /**
     * Attribute name for script URL.
     */
    private static final String SRC = "src";

    /**
     * We use a specific parser for each matched group. Store them in a TreeMap to ensure that lower groups will be tested before higher.
     */
    private static final Map<Integer, TerFunction<String, ProxyNutDao, String, ResourceParser.ApplyResult>> PARSERS =
            new TreeMap<Integer, TerFunction<String, ProxyNutDao, String, ResourceParser.ApplyResult>>() {
        {
            put(NumberUtils.SIX, new HrefParser());
        }
        {
            put(NumberUtils.FOURTEEN, new DefaultParser());
        }
        {
            put(1, new JsParser());
        }
        {
            put(NumberUtils.THIRTEEN, new CssParser());
        }
        {
            put(NumberUtils.FIFTEEN, new ImgParser());
        }
        {
            put(NumberUtils.TWENTY, new HtmlParser());
        }
    };

    /**
     * Regex that matches JS script import or JS declaration.
     */
    private static final String JS_SCRIPT_PATTERN = String.format("(<%1$s.*?(%2$s=)?(([^>]*>[^<]*</%1$s>)|([^/]*/>)))", "script", SRC);

    /**
     * Regex that matches CSS script import.
     */
    private static final String HREF_SCRIPT_PATTERN = String.format("(<%1$s.*?(%2$s=)?(([^>]*>)(([^<]*</%1$s>)|([^/]*/>))?))", "link", "href");

    /**
     * Regex that matches images import.
     */
    private static final String IMG_PATTERN = String.format("(<%1$s.*?(%2$s=)?(([^>]*>[^<]*</%1$s>)|([^/]*/>)))", "img", SRC);

    /**
     * Regex that matches CSS declaration.
     */
    private static final String CSS_SCRIPT_PATTERN = "(<style>.*?</style>)";

    /**
     * Regex that matches HTML comment.
     */
    private static final String HTML_COMMENT_PATTERN = "(<!--.*?-->)";

    /**
     * Regex that matches HTML import.
     */
    private static final String HTML_IMPORT_PATTERN = "(<wuic:html-import.*?/>)";

    /**
     * The entire regex that collects desired data.
     */
    private static final String REGEX = String.format("%s|%s|%s|%s|%s|%s",
            JS_SCRIPT_PATTERN, HREF_SCRIPT_PATTERN, CSS_SCRIPT_PATTERN, HTML_COMMENT_PATTERN, IMG_PATTERN, HTML_IMPORT_PATTERN);

    /**
     * The pattern that collects desired data
     */
    private static final Pattern PATTERN = Pattern.compile(REGEX, Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    /**
     * The applied filters
     */
    private List<NutFilter> nutFilters;

    /**
     * Inspects or not.
     */
    private Boolean doInspection;

    /**
     * The charset of inspected file.
     */
    private String charset;

    /**
     * Use server hint.
     */
    private Boolean serverHint;

    /**
     * <p>
     * Builds a new instance.
     * </p>
     *
     * @param inspect activate inspection or not
     * @param cs files charset
     * @param sh activate server hint or not
     */
    @ConfigConstructor
    public HtmlInspectorEngine(
            @BooleanConfigParam(defaultValue = true, propertyKey = ApplicationConfig.INSPECT) final Boolean inspect,
            @StringConfigParam(defaultValue = "UTF-8", propertyKey = ApplicationConfig.CHARSET) final String cs,
            @BooleanConfigParam(defaultValue = true, propertyKey = ApplicationConfig.SERVER_HINT) final Boolean sh) {
        doInspection = inspect;
        charset = cs;
        serverHint = sh;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<ConvertibleNut> internalParse(final EngineRequest request) throws WuicException {
        // Will contains both heap's nuts eventually modified or extracted nuts.
        final List<ConvertibleNut> retval = new ArrayList<ConvertibleNut>();

        if (works()) {
            for (final ConvertibleNut nut : request.getNuts()) {
                nut.addTransformer(new HtmlTransformer(request, charset, serverHint, nutFilters));
                retval.add(nut);
            }
        }

        return retval;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<NutType> getNutTypes() {
        return Arrays.asList(NutType.HTML);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public EngineType getEngineType() {
        return EngineType.INSPECTOR;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Boolean works() {
        return doInspection;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setNutFilter(final List<NutFilter> nutFilters) {
        this.nutFilters = nutFilters;
    }

    /**
     * <p>
     * This class is a transformer that appends to a particular nut an extracted resource added as a referenced nut.
     * </p>
     *
     * @author Guillaume DROUET
     * @version 1.0
     * @since 0.5.0
     */
    private static final class HtmlTransformer extends Pipe.DefaultTransformer<ConvertibleNut> implements Serializable {

        /**
         * Logger.
         */
        private static final Logger LOGGER = LoggerFactory.getLogger(HtmlTransformer.class);

        /**
         * The request. This transformer should not be serialized before a first call to
         * {@link #transform(InputStream, OutputStream, ConvertibleNut)} is performed.
         */
        private final transient EngineRequest request;

        /**
         * The nut filters.
         */
        private final transient List<NutFilter> nutFilters;

        /**
         * Charset.
         */
        private final String charset;

        /**
         * Server hint activation.
         */
        private final boolean serverHint;

        /**
         * All replacements performed by the transformer.
         */
        private Map<Object, List<String>> replacements;

        /**
         * <p>
         * Builds a new instance.
         * </p>
         *
         * @param r the request
         * @param cs the charset
         * @param sh server hint
         * @param nutFilterList the nut filters
         */
        private HtmlTransformer(final EngineRequest r, final String cs, final boolean sh, final List<NutFilter> nutFilterList) {
            this.request = r;
            this.charset = cs;
            this.serverHint = sh;
            this.nutFilters = nutFilterList;
        }

        /**
         * <p>
         * Parses the given HTML content and returns all the information collected during the operation.
         * </p>
         *
         * <p>
         * When a script is referenced, the given {@link NutDao} will be used to retrieve the corresponding
         * {@link ConvertibleNut}.
         * </p>
         *
         * @param content the HTML content to parse
         * @param dao the DAO
         * @param nutName the nut name
         * @return the parsed HTML
         * @throws WuicException if WUIC fails to configure context or process created workflow
         * @throws IOException if any I/O error occurs
         */
        private List<ParseInfo> parse(final String nutName, final String content, final NutDao dao, final String rootPath)
                throws WuicException, IOException {
            // Create a proxy that maps inline scripts
            final ProxyNutDao proxy = new ProxyNutDao(rootPath, dao);

            // Create the matcher from the given content, we will keep in an integer the end position of group that  previously matched
            final Matcher matcher = PATTERN.matcher(content);
            int previousGroupEnd = -1;

            // All the paths we have currently collected
            final Map<String, Integer> paths = new LinkedHashMap<String, Integer>();
            final List<ParseInfo> retval = new ArrayList<ParseInfo>();
            int no = 0;

            // Finds desired groups
            while (matcher.find()) {

                // There is something to parse
                if (!matcher.group().trim().isEmpty()) {

                    /*
                     * We've already matched some scripts and there is something (excluding comment and whitespace) between
                     * the previous script and the script currently matched that implies that they should not be imported
                     * together. Consequently, we create here a separate heap.
                     */
                    if (previousGroupEnd != -1 && !content.substring(previousGroupEnd, matcher.start()).trim().isEmpty()) {
                        new ParseInfo(nutName + (no++), paths, proxy, rootPath, request, nutFilters).addTo(retval);
                        paths.clear();
                    }

                    // Now find the appropriate parser
                    for (final Integer groupPosition : PARSERS.keySet()) {

                        // Test value at the associated group position to find the appropriate parser
                        if (matcher.group(groupPosition) != null) {
                            paths.put(matcher.group(), groupPosition);
                            break;
                        }
                    }

                    previousGroupEnd = matcher.end();
                }
            }

            // Create a heap for remaining paths
            if (!paths.isEmpty()) {
                new ParseInfo(nutName + (no), paths, proxy, rootPath, request, nutFilters).addTo(retval);
            }

            return retval;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void transform(final InputStream is, final OutputStream os, final ConvertibleNut convertible)
                throws IOException {
            final long now = System.currentTimeMillis();
            final String content = IOUtils.readString(new InputStreamReader(is, charset));
            final StringBuilder transform = new StringBuilder(content);
            int end = 0;

            // Perform cached replacement
            if (this.replacements != null) {
                for (final Map.Entry<Object, List<String>> entry : this.replacements.entrySet()) {
                    final Object replacement = entry.getKey();
                    end = replace(transform, replacement, entry.getValue(), end);
                }
            } else {
                this.replacements = new LinkedHashMap<Object, List<String>>();
                final int endParent = convertible.getName().lastIndexOf('/');
                final String rootPath = endParent == -1 ? "" : convertible.getName().substring(0, endParent);
                final List<ParseInfo> parseInfoList;

                try {
                    parseInfoList = parse(convertible.getName(), content, request.getHeap().findDaoFor(convertible), rootPath);
                } catch (WuicException we) {
                    throw new IOException(we);
                }

                final List<ConvertibleNut> referenced = new ArrayList<ConvertibleNut>();

                final String prefix = IOUtils.mergePath(request.getContextPath(), request.getWorkflowId());
                final UrlProvider urlProvider = UrlUtils.urlProviderFactory().create(prefix);

                // A workflow have been created for each heap
                for (final ParseInfo parseInfo : parseInfoList) {
                    // Perform replacement
                    final String replacement = parseInfo.replacement(request, urlProvider, referenced);
                    end = replace(transform, replacement, parseInfo.getCapturedStatements(), end);
                    this.replacements.put(replacement, parseInfo.getCapturedStatements());
                }

                for (final ConvertibleNut ref : referenced) {
                    convertible.addReferencedNut(ref);
                }

                // Modify the content to give more information to the client directly inside the page
                if (!request.isStaticsServedByWuicServlet()) {

                    // The hint resource
                    if (serverHint) {
                         hintResources(urlProvider, transform, referenced);
                    }

                    applicationCache(convertible, urlProvider, transform);
                }
            }

            IOUtils.copyStream(new ByteArrayInputStream(transform.toString().getBytes()), os);

            Logging.TIMER.log("HTML transformation in {}ms", System.currentTimeMillis() - now);
        }

        /**
         * <p>
         * Inserts in the given HTML content a application cache file as an attribute in the "html" tag.
         * If the tag is missing, nothing is done.
         * </p>
         *
         * @param nut the nut representing the HTML page
         * @param urlProvider the URL provider
         * @param content the page content
         */
        private void applicationCache(final ConvertibleNut nut,
                                      final UrlProvider urlProvider,
                                      final StringBuilder content) {
            int index = content.indexOf("<html");

            if (index == -1) {
                LOGGER.warn("Filtered HTML does not have any <html>. Application cache file won't be inserted.");
            } else {
                // Compute content
                final StringBuilder sb = new StringBuilder();
                final List<ConvertibleNut> cached = CollectionUtils.newList(nut);
                collect(urlProvider, sb, cached);
                final Long versionNumber = NutUtils.getVersionNumber(cached);
                sb.append("\nNETWORK:\n*");
                sb.insert(0, String.format("CACHE MANIFEST\n# Version number: %d", versionNumber));

                // Create the nut
                final String name = nut.getName().concat(".appcache");
                final byte[] bytes = sb.toString().getBytes();
                final ConvertibleNut appCache = new ByteArrayNut(bytes, name, NutType.APP_CACHE, versionNumber, false);
                nut.addReferencedNut(appCache);

                // Modify the HTML content
                index += NumberUtils.FIVE;
                final String replacement = String.format(" manifest=\"%s\"", urlProvider.getUrl(appCache));
                content.insert(index, replacement);
                this.replacements.put(index, Arrays.asList(replacement));
            }
        }

        /**
         * <p>
         * Creates nut representing an 'appcache' for the given nuts.
         * </p>
         *
         * @param urlProvider the URL provider
         * @param sb the string builder where cached nuts URL will be added
         * @param nuts the nuts to put in cache
         */
        private void collect(final UrlProvider urlProvider, final StringBuilder sb, final List<ConvertibleNut> nuts) {
            for (final ConvertibleNut nut : nuts) {
                sb.append("\n").append(urlProvider.getUrl(nut));

                if (nut.getReferencedNuts() != null) {
                    collect(urlProvider, sb, nut.getReferencedNuts());
                }
            }
        }

        /**
         * <p>
         * Inserts in the given HTML content all resources hint computed from the given nuts. The "link" tag will be
         * inserted in a "head" tag that could be created in it does not exists. Nothing will be done if the given
         * content does not contain any "html" tag.
         * </p>
         *
         * @param urlProvider the provider
         * @param content the content
         * @param convertibleNuts the nuts
         */
        private void hintResources(final UrlProvider urlProvider,
                                   final StringBuilder content,
                                   final List<ConvertibleNut> convertibleNuts) {
            int index = content.indexOf("<head>");

            if (index == -1) {
                index = content.indexOf("<head ");
            }

            if (index == -1) {
                index = content.indexOf("<html>");

                if (index == -1) {
                    index = content.indexOf("<html ");
                }

                if (index == -1) {
                    LOGGER.warn("Filtered HTML does not have any <html>. Server hint directives won't be inserted.");
                    return;
                } else {
                    // Closing <html> tag
                    index = content.indexOf(">", index) + 1;
                    content.insert(index, "<head></head>");
                    index += NumberUtils.FIVE;
                }
            } else {
                // Closing <head> tag
                index = content.indexOf(">", index);
            }

            final StringBuilder hints = new StringBuilder();
            appendHint(urlProvider, hints, convertibleNuts);
            index++;
            final String replacement = hints.toString();
            content.insert(index, replacement);
            this.replacements.put(index, Arrays.asList(replacement));
        }

        /**
         * <p>
         * Appends recursively to the given builder all the hints corresponding to each nut specified in parameter
         * </p>
         *
         * @param urlProvider the URL provider
         * @param builder the builder
         * @param convertibleNuts the resource hints
         */
        private void appendHint(final UrlProvider urlProvider,
                                final StringBuilder builder,
                                final List<ConvertibleNut> convertibleNuts) {
            for (final ConvertibleNut ref : convertibleNuts) {
                final NutType nutType = ref.getNutType();
                final String as = nutType.getHintInfo() == null ? "" : " as=\"" + nutType.getHintInfo() + "\"";
                final String strategy = ref.isSubResource() ? "preload" : "prefetch";
                builder.append(String.format("<link rel=\"%s\" href=\"%s\"%s />\n", strategy, urlProvider.getUrl(ref), as));

                if (ref.getReferencedNuts() != null) {
                    appendHint(urlProvider, builder, ref.getReferencedNuts());
                }
            }
        }

        /**
         * <p>
         * Replaces in the given {@link StringBuilder} all the statements specified in parameter by an empty
         * {@code String} except the first one which will be replaced by a particular replacement also specified
         * in parameter.
         * </p>
         *
         * @param transform the builder
         * @param replacement the replacement
         * @param statements the statements to replace
         * @param startIndex the index where the method could start to search statements in the builder
         * @return the updated index
         */
        private int replace(final StringBuilder transform,
                            final Object replacement,
                            final List<String> statements,
                            final int startIndex) {
            if (replacement instanceof Integer) {
                final int index = Integer.class.cast(replacement);

                // Insert all captured statements at the given index
                for (final String statement : statements) {
                    transform.insert(index, statement);
                }

                return startIndex;
            } else {
                int end = startIndex;

                // Replace all captured statements with HTML generated from WUIC process
                for (int i = 0; i < statements.size(); i++) {
                    final String toReplace = statements.get(i);
                    end = replace(transform, replacement.toString(), toReplace, end, i == 0);
                }

                return end;
            }
        }

        /**
         * <p>
         * Replaces in the given {@link StringBuilder} the statement specified in parameter by an empty {@code String}
         * except if the statement is the first one which will be replaced by a particular replacement also specified
         * in parameter.
         * </p>
         *
         * @param transform the builder
         * @param replacement the replacement
         * @param startIndex the index where the method could start to search statements in the builder
         * @param toReplace the string to replace
         * @param isFirst if the given statement is the first one
         * @return the updated index
         */
        private int replace(final StringBuilder transform,
                            final String replacement,
                            final String toReplace,
                            final int startIndex,
                            final boolean isFirst) {

            final int start = transform.indexOf(toReplace, startIndex);
            int end = start + toReplace.length();

            // Add the WUIC result in place of the first statement
            if (isFirst) {
                transform.replace(start, end, replacement);
                end = start + replacement.length();
            } else {
                transform.replace(start, end, "");
                end = start;
            }

            return end;
        }
    }

    /**
     * <p>
     * This abstract class is a base for script references.
     * </p>
     *
     * @author Guillaume DROUET
     * @version 1.1
     * @since 0.4.4
     */
    private abstract static class ResourceParser implements TerFunction<String, ProxyNutDao, String, ResourceParser.ApplyResult> {

        /**
         * <p>
         * Represents the result generated when a {@link ResourceParser} is applied.
         * </p>
         *
         * @author Guillaume DROUET
         * @version 1.0
         * @since 0.5.0
         */
        public static final class ApplyResult {

            /**
             * The URL to extract.
             */
            private final String url;

            /**
             * Some detected additional attributes.
             */
            private final Map<String, String> attributes;

            /**
             * <p>
             * Builds a new instance.
             * </p>
             *
             * @param url the url
             * @param attributes the attributes
             */
            private ApplyResult(final String url, final Map<String, String> attributes) {
                this.url = url;
                this.attributes = attributes;
            }

            /**
             * <p>
             * Returns the URL.
             * </p>
             *
             * @return the URL
             */
            private String getUrl() {
                return url;
            }

            /**
             * <p>
             * Returns the attributes.
             * </p>
             *
             * @return the attributes
             */
            private Map<String, String> getAttributes() {
                return attributes;
            }
        }

        /**
         * <p>
         * Returns the token that refers the URL.
         * </p>
         *
         * @return the token
         */
        protected abstract String urlToken();

        /**
         * <p>
         * Returns the {@link NutType} of created nut.
         * </p>
         *
         */
        protected abstract NutType getNutType();

        /**
         * <p>
         * Indicates if the parser should tries to read ant inline content.
         * </p>

         * @return {@code true} if inline content needs to be read, {@code false} otherwise
         */
        protected abstract Boolean readInlineIfTokenNotFound();

        /**
         * <p>
         * Gets all other supported tokens to be collected.
         * </p>
         *
         * @return the tokens to collect
         */
        protected abstract String[] otherTokens();

        /**
         * <p>
         * Extract the value of the attribute identified by the given token in the specified {@code String}.
         * </p>
         *
         * @param s the string
         * @param t the token
         * @return the value
         */
        public static String extractValue(final String s, final String t) {
            final String token = " ".concat(t);
            int index = 0;
            char c = 0;

            // Token found
            if (!token.isEmpty()) {

                // Check that we do not found another token starting with this desired token
                boolean valid = false;

                while ((index = s.indexOf(token, index)) != -1) {
                    index += token.length();
                    c = s.charAt(index);

                    // Token can be followed by a space or an '=' character
                    if (c == '=' || c == ' ') {
                        valid = true;
                        break;
                    }
                }

                if (!valid) {
                    return null;
                }
            } else {
                // Token not found
                return null;
            }

            // Read until we leave spaces
            while (c == ' ') {
                c = s.charAt(++index);
            }

            // There is a value
            if (c == '=') {

                // Read until we leave spaces
                do {
                    c = s.charAt(++index);
                } while (c == ' ');

                final String retval;

                if (c == '\'' || c == '"') {
                    retval = s.substring(index + 1, s.indexOf(c, index + 1));
                } else {
                    int end = s.indexOf(' ', index);

                    // No space, search />
                    if (end == -1) {
                        end = s.indexOf('>', index);

                        if (s.charAt(end - 1) == '/') {
                            end--;
                        }
                    }
                    retval = s.substring(index, end);
                }

                // Looking for a delimiter adding extra characters after the extracted name
                int delimiter = retval.indexOf('#');

                if (delimiter == -1) {
                    delimiter = retval.indexOf('?');
                }

                return (delimiter == -1 ? retval : retval.substring(0, delimiter)).trim();
            } else {
                // There is no value
                return "";
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public ApplyResult apply(final String s, final ProxyNutDao proxy, final String nutName) {
            if (s.contains("data-wuic-skip")) {
                return null;
            }

            final String url = extractValue(s, urlToken());

            if (url != null && !url.isEmpty()) {
                if (url.startsWith("http://") || url.startsWith("https://") || url.startsWith("//")) {
                    return null;
                } else {
                    Map<String, String> attributes = null;
                    final String[] otherTokens = otherTokens();

                    // Collect all attributes and their values also declared in the tag
                    if (otherTokens != null) {
                        for (final String attribute : otherTokens()) {
                            final String value = extractValue(s, attribute);

                            if (value != null) {
                                if (attributes == null) {
                                    attributes = new HashMap<String, String>();
                                }

                                attributes.put(attribute, value);
                            }
                        }
                    }

                    return new ApplyResult(url, attributes);
                }
            } else if (readInlineIfTokenNotFound()) {
                // Looking for content
                final int start = s.indexOf('>') + 1;
                final int end = s.indexOf('<', start - 1);
                final byte[] content = s.substring(start, end).getBytes();

                // Create nut
                final NutType nt = getNutType();
                final String retval = String.format("%s%s", nutName, nt.getExtensions()[0]);

                // Sign content
                final MessageDigest md = IOUtils.newMessageDigest();
                md.update(content);
                proxy.addRule(retval, new ByteArrayNut(content, retval, nt, ByteBuffer.wrap(md.digest()).getLong(), false));

                return new ApplyResult(retval, null);
            } else {
                return null;
            }
        }
    }

    /**
     * <p>
     * This class parses links to JS scripts and inline JS scripts.
     * </p>
     *
     * @author Guillaume DROUET
     * @version 1.1
     * @since 0.4.4
     */
    private static class JsParser extends ResourceParser {

        /**
         * {@inheritDoc}
         */
        @Override
        public String urlToken() {
            return SRC;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Boolean readInlineIfTokenNotFound() {
            return Boolean.TRUE;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public NutType getNutType() {
            return NutType.JAVASCRIPT;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected String[] otherTokens() {
            return null;
        }
    }

    /**
     * <p>
     * This class parses links to CSS.
     * </p>
     *
     * @author Guillaume DROUET
     * @version 1.1
     * @since 0.4.4
     */
    private static class HrefParser extends ResourceParser {

        /**
         * {@inheritDoc}
         */
        @Override
        public String urlToken() {
            return "href";
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected String[] otherTokens() {
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Boolean readInlineIfTokenNotFound() {
            return Boolean.FALSE;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public NutType getNutType() {
            return NutType.CSS;
        }
    }

    /**
     * <p>
     * This class parses 'img' tags.
     * </p>
     *
     * @author Guillaume DROUET
     * @version 1.0
     * @since 0.5.0
     */
    private static class ImgParser extends ResourceParser {

        /**
         * {@inheritDoc}
         */
        @Override
        public String urlToken() {
            return SRC;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected String[] otherTokens() {
            return new String[] {
                    "align", "alt", "border", "crossorigin", "height", "hspace", "ismap", "longdesc", "usemap", "vspace", "width",
            };
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Boolean readInlineIfTokenNotFound() {
            return Boolean.FALSE;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public NutType getNutType() {
            return NutType.PNG;
        }
    }

    /**
     * <p>
     * This class parses '<wuic:html-import/>' tags.
     * </p>
     *
     * @author Guillaume DROUET
     * @version 1.0
     * @since 0.5.0
     */
    private static class HtmlParser extends ResourceParser {

        /**
         * {@inheritDoc}
         */
        @Override
        public String urlToken() {
            return "workflowId";
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected String[] otherTokens() {
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Boolean readInlineIfTokenNotFound() {
            return Boolean.FALSE;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public NutType getNutType() {
            return null;
        }
    }


    /**
     * <p>
     * This class parses inline CSS.
     * </p>
     *
     * @author Guillaume DROUET
     * @version 1.1
     * @since 0.4.4
     */
    private static class CssParser extends ResourceParser {

        /**
         * {@inheritDoc}
         */
        @Override
        public String urlToken() {
            return "";
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected String[] otherTokens() {
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Boolean readInlineIfTokenNotFound() {
            return Boolean.TRUE;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public NutType getNutType() {
            return NutType.CSS;
        }
    }

    /**
     * <p>
     * This class returns always {@code null}, telling the caller to not replace the parsed {@code String}.
     * </p>
     *
     * @author Guillaume DROUET
     * @version 1.0
     * @since 0.4.4
     */
    private static class DefaultParser implements TerFunction<String, ProxyNutDao, String, ResourceParser.ApplyResult> {

        /**
         * {@inheritDoc}
         */
        @Override
        public ResourceParser.ApplyResult apply(final String s, final ProxyNutDao proxyNutDao, final String nutName) {
            return null;
        }
    }

    /**
     * <p>
     * Provides information from collected data in a HTML content.
     * </p>
     *
     * @author Guillaume DROUET
     * @version 1.0
     * @since 0.4.4
     */
    public static final class ParseInfo {

        /**
         * Logger.
         */
        private final Logger logger = LoggerFactory.getLogger(getClass());

        /**
         * The heap generated during collect.
         */
        private NutsHeap heap;

        /**
         * Collected statements.
         */
        private List<String> capturedStatements;

        /**
         * Additional attributes.
         */
        private Map<String, String> attributes;

        /**
         * Nut name corresponding to a captured image that should not be considered as a sprite.
         */
        private Set<String> skipSprites;

        /**
         * Nut filters.
         */
        private List<NutFilter> nutFilters;

        /**
         * <p>
         * Builds a new instance.
         * </p>
         *
         * @param groupName the nut name
         * @param groups captured statements
         * @param proxyNutDao the DAO to use when computing path from collected data
         * @param rootPath the root path of content
         * @param request the associated request
         * @param nutFilterList the nut filters
         * @throws IOException if any I/O error occurs
         * @throws WorkflowNotFoundException if a workflow is described
         */
        private ParseInfo(final String groupName,
                          final Map<String, Integer> groups,
                          final ProxyNutDao proxyNutDao,
                          final String rootPath,
                          final EngineRequest request,
                          final List<NutFilter> nutFilterList)
                throws IOException, WorkflowNotFoundException {
            final String[] groupPaths = new String[groups.size()];
            final List<NutsHeap> composition = new ArrayList<NutsHeap>();
            this.capturedStatements = new ArrayList<String>(groups.keySet());
            this.skipSprites = new HashSet<String>();
            this.nutFilters = nutFilterList;

            int start = 0;
            int cpt = 0;

            // Gets the appropriate parser for each captured group according to their position and compute path
            for (final Map.Entry<String, Integer> entry : groups.entrySet()) {
                final TerFunction<String, ProxyNutDao, String, ResourceParser.ApplyResult> parser = PARSERS.get(entry.getValue());
                final ResourceParser.ApplyResult result = parser.apply(entry.getKey(), proxyNutDao, groupName + cpt);

                if (result != null) {
                    if (parser instanceof HtmlParser) {
                        if (cpt > 0) {
                            composition.add(createHeap(groupPaths, start, cpt, request, proxyNutDao));
                            start += cpt;
                            cpt = 0;
                        }

                        composition.add(request.getHeap(result.getUrl()));
                    } else {
                        final String path = result.getUrl();

                        try {
                            // Will raise an exception if type is not supported
                            final NutType nutType = NutType.getNutType(path);

                            // If we are in best effort and the captured path corresponds to a NutType processed by any
                            // converter engine, we must capture the statement to transform it
                            final boolean canRemove = canRemove(request.getChainFor(nutType), request);

                            if (canRemove) {
                                this.capturedStatements.remove(entry.getKey());
                            } else {
                                final String simplified = sanitize(rootPath, path);

                                // Now we collect the attributes of the tag to add them in the future statement
                                if (result.getAttributes() != null) {
                                    populateAttributes(result.getAttributes());
                                }

                                groupPaths[start + cpt++] = simplified;

                                // Any <img> tag should not be converted to a sprite
                                if (NutType.PNG.equals(nutType)) {
                                    skipSprites.add(simplified);
                                }
                            }
                        } catch (Exception e) {
                            logger.debug("Fail to get the NutType", e);
                            logger.warn("{} does not ends with an extension supported by WUIC, skipping...", path);
                            this.capturedStatements.remove(entry.getKey());
                        }
                    }
                } else {
                    // Path is null, do not replace anything
                    this.capturedStatements.remove(entry.getKey());
                }
            }

            createHeap(cpt, start, groupPaths, request, composition, proxyNutDao);
        }

        /**
         * <p>
         * Simplifies the result of concatenation between the two given strings and make sure the result if valid.
         * </p>
         *
         * @param rootPath the left side
         * @param path the right side
         * @return both sides concatenated and sanitized
         */
        private String sanitize(final String rootPath, final String path) {
            final String simplify = rootPath.isEmpty() ? path : IOUtils.mergePath(rootPath, path);
            final String simplified = StringUtils.simplifyPathWithDoubleDot(simplify);

            if (simplified == null) {
                WuicException.throwBadArgumentException(new IllegalArgumentException(
                        String.format("%s does not represents a reachable path", simplify)));
            }

            return simplified;
        }

        /**
         * <p>
         * Populates the cached attributes with the map retrieved from the given map.
         * </p>
         *
         * @param result the attributes to cache
         */
        private void populateAttributes(final Map<String, String> result) {
            if (attributes == null) {
                attributes = new HashMap<String, String>();
            }

            for (final Map.Entry<String, String> attrEntry : result.entrySet()) {
                final String old = attributes.put(attrEntry.getKey(), attrEntry.getValue());

                // Conflict
                if (old != null && !old.equals(attrEntry.getValue())) {
                    logger.info("Possibly merged tags have different values for the attribute {}, keeping {} instead of {}",
                            attrEntry.getKey(),
                            attrEntry.getValue(),
                            old);
                }
            }
        }

        /**
         * <p>
         * Indicates if we can remove a path to skip processing or not according to the given request and the chain that
         * will process it.
         * </p>
         *
         * @param engine the engine chain
         * @param request the request
         * @return {@code true} if path could be skipped, {@code false} otherwise
         */
        private boolean canRemove(final NodeEngine engine, final EngineRequest request) {
            NodeEngine e = engine;
            boolean retval;

            if (request.isBestEffort()) {
                retval = true;

                while (e != null && retval) {
                    retval = !EngineType.CONVERTER.equals(e.getEngineType());
                    e = e.getNext();
                }
            } else {
                retval = false;
            }

            return retval;
        }

        /**
         * <p>
         * Creates the heap for this object. Any remaining paths will be used to create a heap. If the composition
         * contains only on heap, then this one will be used, otherwise a composite heap will be created.
         * </p>
         *
         * @param count the number of remaining paths
         * @param start the first remaining path position
         * @param groupPaths all the paths
         * @param request the request
         * @param composition the composition of already created heaps
         * @param proxyNutDao the dao
         * @throws IOException if any I/O error occurs
         */
        private void createHeap(final int count,
                                final int start,
                                final String[] groupPaths,
                                final EngineRequest request,
                                final List<NutsHeap> composition,
                                final NutDao proxyNutDao) throws IOException {
            // Create a heap remaining paths
            if (count > 0) {
                composition.add(createHeap(groupPaths, start, count, request, proxyNutDao));
            }

            if (!composition.isEmpty()) {
                if (composition.size() == 1) {
                    heap = composition.get(0);
                } else {
                    heap = createHeap(null, request, proxyNutDao, composition.toArray(new NutsHeap[composition.size()]));
                }
            }
        }

        /**
         * <p>
         * Creates an internal heap with given parameters.
         * </p>
         *
         * @param groupPaths all the captured statements
         * @param start the first statement index
         * @param count number of statements in the heap
         * @param request the request
         * @param dao the DAO
         * @return the heap
         * @throws IOException if creation fails
         */
        private NutsHeap createHeap(final String[] groupPaths,
                                    final int start,
                                    final int count,
                                    final EngineRequest request,
                                    final NutDao dao)
                throws IOException {
            // All paths computed from captured statements.
            final String[] paths = new String[count];
            System.arraycopy(groupPaths, start, paths, 0, count);

            List<String> filteredPath = CollectionUtils.newList(paths);

            if (nutFilters != null) {
                for (final NutFilter filter : nutFilters) {
                    filteredPath = filter.filterPaths(filteredPath);
                }
            }

            return createHeap(filteredPath, request, dao);
        }

        /**
         * <p>
         * Creates an internal heap with given parameters.
         * </p>
         *
         * @param filteredPath the heap paths
         * @param request the request
         * @param dao the DAO
         * @param composition the composition
         * @return the heap
         * @throws IOException if any I/O error occurs
         */
        private NutsHeap createHeap(final List<String> filteredPath,
                                    final EngineRequest request,
                                    final NutDao dao,
                                    final NutsHeap ... composition)
                throws IOException {
            final byte[] hash;

            if (filteredPath != null) {
                hash = IOUtils.digest(filteredPath.toArray(new String[filteredPath.size()]));
            } else {
                final String[] ids = new String[composition.length];

                for (int i = 0; i < composition.length; i++) {
                    ids[i] = composition[i].getId();
                }

                hash = IOUtils.digest(ids);
            }

            final String heapId = StringUtils.toHexString(hash);
            final NutsHeap h = new NutsHeap(request.getHeap().getFactory(), filteredPath, true, dao, heapId, composition);
            h.checkFiles(request.getProcessContext());
            h.addObserver(request.getHeap());
            NutsHeap.ListenerHolder.INSTANCE.add(h);
            return h;
        }

        /**
         * <p>
         * Creates a replacement for this information
         * </p>
         *
         * @param request the request
         * @param urlProvider object that computes URL
         * @param referenced any nut created nut will be added to this list
         * @return the replacement {@code String}
         * @throws IOException if any I/O error occurs
         */
        public String replacement(final EngineRequest request, final UrlProvider urlProvider, final List<ConvertibleNut> referenced)
                throws IOException {
            // Render HTML for workflow result
            final StringBuilder html = new StringBuilder();
            final EngineRequest parseRequest = new EngineRequestBuilder(request)
                    .nuts(heap.getNuts())
                    .heap(heap)
                    .excludeFromSprite(skipSprites)
                    .skip(request.alsoSkip(EngineType.CACHE))
                    .build();
            final List<ConvertibleNut> merged;

            try {
                merged = HeadEngine.runChains(parseRequest);
            } catch (WuicException we) {
                throw new IOException(we);
            }

            for (final ConvertibleNut n : merged) {
                // Just add the heap ID as prefix to refer many nuts with same name but from different heaps
                if (request.getPrefixCreatedNut().isEmpty()){
                    n.setNutName(heap.getId() + n.getName());
                } else {
                    n.setNutName(IOUtils.mergePath(request.getPrefixCreatedNut(), heap.getId() + n.getName()));
                }

                referenced.add(ByteArrayNut.toByteArrayNut(n));

                // Some additional attributes
                if (getAttributes() != null) {
                    final String[] attr = new String[getAttributes().size()];
                    int index = 0;

                    for (final Map.Entry<String, String> entry : getAttributes().entrySet()) {
                        attr[index++] = entry.getKey() + "=\"" + entry.getValue() + '"';
                    }

                    html.append(HtmlUtil.writeScriptImport(n, urlProvider, attr)).append("\r\n");
                } else {
                    html.append(HtmlUtil.writeScriptImport(n, urlProvider)).append("\r\n");
                }
            }

            return html.toString();
        }

        /**
         * <p>
         * Returns the additional attributes.
         * </p>
         *
         * @return the attributes
         */
        public Map<String, String> getAttributes() {
            return attributes;
        }

        /**
         * <p>
         * Gets the captured statement by parsing.
         * </p>
         *
         * @return the statements
         */
        public List<String> getCapturedStatements() {
            return capturedStatements;
        }

        /**
         * <p>
         * Add this info to the given list if and only if some statements have been captured
         * </p>
         *
         * @param list the list
         */
        public void addTo(final List<ParseInfo> list) {
            if (!capturedStatements.isEmpty()) {
                list.add(this);
            }
        }
    }
}
