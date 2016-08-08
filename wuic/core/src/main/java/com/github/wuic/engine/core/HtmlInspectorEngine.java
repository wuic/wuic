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


package com.github.wuic.engine.core;

import com.github.wuic.EnumNutType;
import com.github.wuic.NutType;
import com.github.wuic.NutTypeFactory;
import com.github.wuic.config.Alias;
import com.github.wuic.config.BooleanConfigParam;
import com.github.wuic.config.Config;
import com.github.wuic.engine.EngineRequest;
import com.github.wuic.engine.EngineRequestBuilder;
import com.github.wuic.engine.EngineService;
import com.github.wuic.engine.EngineType;
import com.github.wuic.engine.HeadEngine;
import com.github.wuic.engine.NodeEngine;
import com.github.wuic.exception.WorkflowNotFoundException;
import com.github.wuic.exception.WuicException;
import com.github.wuic.nut.CompositeNut;
import com.github.wuic.nut.ConvertibleNut;
import com.github.wuic.nut.InMemoryNut;
import com.github.wuic.nut.Nut;
import com.github.wuic.nut.dao.NutDao;
import com.github.wuic.nut.NutsHeap;
import com.github.wuic.nut.dao.core.ProxyNutDao;
import com.github.wuic.nut.filter.NutFilter;
import com.github.wuic.nut.filter.NutFilterHolder;
import com.github.wuic.util.CollectionUtils;
import com.github.wuic.util.HtmlUtil;
import com.github.wuic.util.IOUtils;
import com.github.wuic.util.StringUtils;

import com.github.wuic.util.UrlProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static com.github.wuic.ApplicationConfig.INSPECT;
import static com.github.wuic.ApplicationConfig.SERVER_HINT;

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
 * @since 0.4.4
 */
@EngineService(injectDefaultToWorkflow = true, isCoreEngine = true)
@Alias("htmlInspector")
public class HtmlInspectorEngine extends NodeEngine implements NutFilterHolder {

    /**
     * The logger.
     */
    private final Logger logger = LoggerFactory.getLogger(HtmlInspectorEngine.class);

    /**
     * The applied filters.
     */
    private List<NutFilter> nutFilters;

    /**
     * Inspects or not.
     */
    private Boolean doInspection;

    /**
     * Use server hint.
     */
    private Boolean serverHint;

    /**
     * The parser service.
     */
    private AssetsMarkupParser parser;

    /**
     * <p>
     * Initializes a new instance.
     * </p>
     *
     * @param inspect activate inspection or not
     * @param sh activate server hint or not
     */
    @Config
    public void init(
            @BooleanConfigParam(defaultValue = true, propertyKey = INSPECT) final Boolean inspect,
            @BooleanConfigParam(defaultValue = true, propertyKey = SERVER_HINT) final Boolean sh) {
        doInspection = inspect;
        serverHint = sh;

        if (doInspection) {
            for (final AssetsMarkupParser p : ServiceLoader.load(AssetsMarkupParser.class)) {

                // Install the first service
                if (parser == null) {
                    parser = p;
                } else {
                    // Ignore any other
                    logger.warn("{} service already installed ({}), ignoring service {}",
                            AssetsMarkupParser.class.getName(), parser.getClass().getName(), p.getClass().getName());
                }
            }

            if (parser == null) {
                logger.info("No {} service has been found, inspection is disabled.", AssetsMarkupParser.class.getName());
                doInspection = false;
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Long apply(final ConvertibleNut nut, final Long version) {
        return version + String.format("%s:%s=%s", getClass().getName(), INSPECT, String.valueOf(doInspection)).hashCode();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<ConvertibleNut> internalParse(final EngineRequest request) throws WuicException {
        if (works()) {
            // Will contains both heap's nuts eventually modified or extracted nuts.
            final List<ConvertibleNut> retval = new ArrayList<ConvertibleNut>();

            for (final ConvertibleNut nut : request.getNuts()) {
                logger.info("New {} for request with workflow ID {}.", HtmlTransformer.class.getName(), request.getWorkflowId());
                nut.addTransformer(new HtmlTransformer(request, request.getCharset(), serverHint, nutFilters, parser));
                retval.add(nut);
            }

            return retval;
        } else {
            return request.getNuts();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<NutType> getNutTypes() {
        return Arrays.asList(getNutTypeFactory().getNutType(EnumNutType.HTML));
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
     * Sets the parser.
     * </p>
     *
     * @param parser the parser
     */
    public void setParser(final AssetsMarkupParser parser) {
        this.parser = parser;
    }

    /**
     * <p>
     * An handler that tracks assets locations. Once an instance is created, the {@link #parser} is automatically used
     * to process a content.
     * </p>
     *
     * @author Guillaume DROUET
     * @since 0.5.3
     */
    static class Handler implements AssetsMarkupHandler {

        /**
         * Matrix (line/column) corresponding to the content.
         */
        private final String[] contentMatrix;

        /**
         * Paths.
         */
        private final List<ResourceParser> paths;

        /**
         * Parsing information.
         */
        private final List<ParseInfo> parseInfoList;

        /**
         * The initiated request.
         */
        private final EngineRequest request;

        /**
         * Nut name.
         */
        private final String nutName;

        /**
         * Root path.
         */
        private final String rootPath;

        /**
         * Proxy.
         */
        private final ProxyNutDao proxy;

        /**
         * The filters.
         */
        private final List<NutFilter> nutFilters;

        /**
         * Last asset number for the current parsing information.
         */
        private int no;

        /**
         * Previous group line end.
         */
        private int previousGroupLineEnd;

        /**
         * Previous group column end.
         */
        private int previousGroupColumnEnd;

        /**
         * <p>
         * Builds a new instance.
         * </p>
         *
         * @param content the content to parse
         * @param nut the nut name
         * @param root the root
         * @param proxyNutDao the proxy
         * @param req the request
         * @param parser the parser service
         * @param nutFilterList the filter list
         */
        Handler(final String content,
                final String nut,
                final String root,
                final ProxyNutDao proxyNutDao,
                final EngineRequest req,
                final AssetsMarkupParser parser,
                final List<NutFilter> nutFilterList) {
            this.contentMatrix = content.split("(\r\n)|(\r)|(\n)");
            this.paths = new ArrayList<ResourceParser>();
            this.parseInfoList = new ArrayList<ParseInfo>();
            this.no = 0;
            this.previousGroupLineEnd = -1;
            this.previousGroupColumnEnd = -1;
            this.nutName = nut;
            this.rootPath = root;
            this.proxy = proxyNutDao;
            this.request = req;
            this.nutFilters = nutFilterList;

            parser.parse(new StringReader(content), this);

            // Create a heap for remaining paths
            if (!paths.isEmpty()) {
                newParseInfo();
            }
        }

        /**
         * <p>
         * Creates a nes {@link ParseInfo} and add it to the internal list.
         * Throws an {@link IllegalStateException} if any exception is caught.
         * </p>
         */
        private void newParseInfo() {
            try {
                new ParseInfo(nutName + (no), paths, proxy, rootPath, request, nutFilters, request.getNutTypeFactory()).addTo(parseInfoList);
            } catch (WuicException we) {
                WuicException.throwBadStateException(we);
            } catch (IOException ioe) {
                WuicException.throwBadStateException(ioe);
            }
        }

        /**
         * <p>
         * Handles a new resources represented by a parsing capable object.
         * </p>
         *
         * @param resourceParser the resource parser
         */
        private void handle(final ResourceParser resourceParser) {

            /*
             * We've already matched some scripts and there is either something (excluding comment and whitespace)
             * between the previous script and the script currently matched or either a "data-wuic-break" attribute
             * in the current script that implies that they should not be imported together.
             * Consequently, we create here a separate heap.
             */
            if (previousGroupLineEnd != -1 && previousGroupColumnEnd != -1) {
                final int l;
                final int c;

                // Move to the character before the position to not read it
                if (resourceParser.getStmt().getStartColumn() == 1) {
                    l = resourceParser.getStmt().getStartLine() - 1;
                    c = contentMatrix[l - 1].length() + 1;
                } else {
                    l = resourceParser.getStmt().getStartLine();
                    c = resourceParser.getStmt().getStartColumn();
                }

                if (resourceParser.attributes.containsKey("data-wuic-break")
                        || !StringUtils.substringMatrix(contentMatrix, previousGroupLineEnd, previousGroupColumnEnd, l, c).trim().isEmpty()) {
                    newParseInfo();
                    paths.clear();
                }
            }

            // Updates the previous end position
            previousGroupLineEnd = resourceParser.getStmt().getEndLine();
            previousGroupColumnEnd = resourceParser.getStmt().getEndColumn();

            paths.add(resourceParser);
        }

        /**
         * <p>
         * Gets the {@link ParseInfo}.
         * </p>
         *
         * @return the list information
         */
        List<ParseInfo> getParseInfoList() {
            return parseInfoList;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void handleComment(final char[] content, final int startLine, final int startColumn, final int length) {
            final AtomicInteger endLine = new AtomicInteger();
            final AtomicInteger endColumn = new AtomicInteger();
            StringUtils.reachEndLineAndColumn(contentMatrix, startLine, startColumn, length, endLine, endColumn);
            handle(new DefaultParser(new Statement(startLine, startColumn, endLine.get(), endColumn.get(), contentMatrix), request.getNutTypeFactory()));
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void handleJavascriptContent(final char[] content,
                                            final Map<String, String> attributes,
                                            final int startLine,
                                            final int startColumn,
                                            final int endLine,
                                            final int endColumn) {
            handle(new ScriptParser(attributes, content, new Statement(startLine, startColumn, endLine, endColumn, contentMatrix), request.getNutTypeFactory()));
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void handleScriptLink(final String link,
                                     final Map<String, String> attributes,
                                     final int startLine,
                                     final int startColumn,
                                     final int endLine,
                                     final int endColumn) {
            handle(new ScriptParser(attributes, link, new Statement(startLine, startColumn, endLine, endColumn, contentMatrix), request.getNutTypeFactory()));
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void handleLink(final String link,
                               final Map<String, String> attributes,
                               final int startLine,
                               final int startColumn,
                               final int endLine,
                               final int endColumn) {
            handle(new HrefParser(attributes, link, new Statement(startLine, startColumn, endLine, endColumn, contentMatrix), request.getNutTypeFactory()));
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void handleCssContent(final char[] content,
                                     final Map<String, String> attributes,
                                     final int startLine,
                                     final int startColumn,
                                     final int endLine,
                                     final int endColumn) {
            handle(new CssParser(attributes, content, new Statement(startLine, startColumn, endLine, endColumn, contentMatrix), request.getNutTypeFactory()));
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void handleImgLink(final String link,
                                  final Map<String, String> attributes,
                                  final int startLine,
                                  final int startColumn,
                                  final int endLine,
                                  final int endColumn) {
            handle(new ImgParser(attributes, link, new Statement(startLine, startColumn, endLine, endColumn, contentMatrix), request.getNutTypeFactory()));
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void handleImport(final String workflowId,
                                 final Map<String, String> attributes,
                                 final int startLine,
                                 final int startColumn,
                                 final int endLine,
                                 final int endColumn) {
            handle(new HtmlParser(attributes, workflowId, new Statement(startLine, startColumn, endLine, endColumn, contentMatrix), request.getNutTypeFactory()));
        }
    }

    /**
     * <p>
     * Provides information from collected data in a HTML content.
     * </p>
     *
     * @author Guillaume DROUET
     * @since 0.4.4
     */
    public static final class ParseInfo {

        /**
         * Logger.
         */
        private final Logger logger = LoggerFactory.getLogger(getClass());

        /**
         * The nut type factory.
         */
        private final NutTypeFactory nutTypeFactory;

        /**
         * The heap generated during collect.
         */
        private NutsHeap heap;

        /**
         * Collected statements.
         */
        private List<String> capturedStatements;

        /**
         * Additional attributes per path.
         */
        private Map<String, Map<String, String>> attributes;

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
         * @param nutTypeFactory the nut type factory
         * @throws IOException if any I/O error occurs
         * @throws WorkflowNotFoundException if a workflow is described
         */
        private ParseInfo(final String groupName,
                          final List<ResourceParser> groups,
                          final ProxyNutDao proxyNutDao,
                          final String rootPath,
                          final EngineRequest request,
                          final List<NutFilter> nutFilterList,
                          final NutTypeFactory nutTypeFactory)
                throws IOException, WorkflowNotFoundException {
            final String[] groupPaths = new String[groups.size()];
            final List<NutsHeap> composition = new ArrayList<NutsHeap>();
            this.capturedStatements = new ArrayList<String>(groups.size());
            this.skipSprites = new HashSet<String>();
            this.nutFilters = nutFilterList;
            this.nutTypeFactory = nutTypeFactory;

            int start = 0;
            int cpt = 0;

            // Applies the appropriate parser for each group
            for (final ResourceParser parser : groups) {

                // Collects nut name
                final String nutName = groupName + cpt;
                final String result = parser.apply(proxyNutDao, nutName);

                if (result != null) {
                    if (parser instanceof HtmlParser) {
                        if (cpt > 0) {
                            composition.add(createHeap(groupPaths, start, cpt, request, proxyNutDao));
                            start += cpt;
                            cpt = 0;
                        }

                        composition.add(request.getHeap(parser.getPath(nutName)));
                        capturedStatements.add(parser.getStmt().toString());
                    } else {
                        // At this moment we know that NutType is not null since parser.apply() would have returned null in that case
                        final NutType nutType = parser.getNutType();

                        // If we are in best effort and the captured path corresponds to a NutType processed by any
                        // converter engine, we must capture the statement to transform it
                        if (!canRemove(request.getChainFor(nutType), request)) {
                            final String path = parser.getPath(nutName);
                            capturedStatements.add(parser.getStmt().toString());
                            final String simplified = sanitize(rootPath, path);

                            // Now we collect the attributes of the tag to add them in the future statement
                            if (parser.attributes != null) {
                                populateAttributes(path, parser.attributes);
                            }

                            groupPaths[start + cpt++] = simplified;

                            // Any <img> tag should not be converted to a sprite
                            if (nutType.isBasedOn(EnumNutType.PNG)) {
                                skipSprites.add(simplified);
                            }
                        }
                    }
                }
            }

            createHeap(cpt, start, groupPaths, request, composition, proxyNutDao);
        }

        /**
         * <p>
         * Simplifies the result of concatenation between the two given strings and make sure the result if valid.
         * The right side will be simply returned if it starts with "/" because it means it's an absolute URL that is
         * not relative to the root path.
         * </p>
         *
         * @param rootPath the left side
         * @param path the right side
         * @return both sides concatenated and sanitized
         */
        private String sanitize(final String rootPath, final String path) {
            if (path.startsWith("/")) {
                return path;
            }

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
        private void populateAttributes(final String path, final Map<String, String> result) {
            if (attributes == null) {
                attributes = new HashMap<String, Map<String, String>>();
            }

            attributes.put(path, result);
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
            final NutsHeap h = new NutsHeap(request.getHeap().getFactory(), filteredPath, true, dao, heapId, nutTypeFactory, composition);
            h.addObserver(request);
            h.checkFiles(request.getProcessContext());
            h.addObserver(request.getHeap());
            NutsHeap.ListenerHolder.INSTANCE.add(h);
            return h;
        }

        /**
         * <p>
         * Creates a replacement for this information.
         * </p>
         *
         * @param request the request
         * @param urlProvider object that computes URL
         * @param referenced any nut created nut will be added to this list
         * @param convertibleNut the convertible nut
         * @return the replacement {@code String}
         * @throws IOException if any I/O error occurs
         */
        public String replacement(final EngineRequest request,
                                  final UrlProvider urlProvider,
                                  final List<ConvertibleNut> referenced,
                                  final ConvertibleNut convertibleNut)
                throws IOException {
            // Render HTML for workflow result
            final StringBuilder html = new StringBuilder();
            final EngineRequest parseRequest = new EngineRequestBuilder(request)
                    .nuts(heap.getNuts())
                    .heap(heap)
                    .excludeFromSprite(skipSprites)
                    .skip(request.alsoSkip(EngineType.CACHE))
                    .origin(convertibleNut)
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

                referenced.add(InMemoryNut.toMemoryNut(n, request));

                // Some additional attributes
                final Map<String, String> additionalAttributes =
                        getAttributes(n instanceof CompositeNut ? CompositeNut.class.cast(n).getCompositionList() : Arrays.asList(n));

                if (additionalAttributes != null) {
                    html.append(HtmlUtil.writeScriptImport(n, urlProvider, additionalAttributes)).append(IOUtils.NEW_LINE);
                } else {
                    html.append(HtmlUtil.writeScriptImport(n, urlProvider)).append(IOUtils.NEW_LINE);
                }
            }

            return html.toString();
        }

        /**
         * <p>
         * Returns the additional attributes collected from all paths found in the given {@link List}.
         * </p>
         *
         * @param forNuts the nuts where attributes will be collected
         * @return the attributes
         */
        public Map<String, String> getAttributes(final List<? extends Nut> forNuts) {
            final Map<String, String> retval = new LinkedHashMap<String, String>();

            if (attributes != null) {
                for (final Nut nut : forNuts) {
                    final Map<String, String> result = attributes.get(nut.getInitialName());

                    if (result != null) {
                        for (final Map.Entry<String, String> attrEntry : result.entrySet()) {
                            final String old = retval.put(attrEntry.getKey(), attrEntry.getValue());

                            // Conflict
                            if (old != null && !old.equals(attrEntry.getValue())) {
                                logger.info("Possibly merged tags have different values for the attribute {}, keeping {} instead of {}",
                                        attrEntry.getKey(),
                                        attrEntry.getValue(),
                                        old);
                            }
                        }
                    }
                }
            }

            return retval;
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
         * Add this info to the given list if and only if some statements have been captured.
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
