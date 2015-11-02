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

import com.github.wuic.engine.EngineRequest;
import com.github.wuic.engine.EngineType;
import com.github.wuic.engine.LineInspector;
import com.github.wuic.engine.RegexLineInspector;
import com.github.wuic.exception.WuicException;
import com.github.wuic.nut.CompositeNut;
import com.github.wuic.nut.ConvertibleNut;
import com.github.wuic.nut.Nut;
import com.github.wuic.nut.PipedConvertibleNut;
import com.github.wuic.nut.dao.NutDao;
import com.github.wuic.nut.NutsHeap;
import com.github.wuic.nut.filter.NutFilter;
import com.github.wuic.nut.filter.NutFilterHolder;
import com.github.wuic.util.CollectionUtils;
import com.github.wuic.util.NumberUtils;
import com.github.wuic.util.NutUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <p>
 * This class inspects CSS files to extract nuts referenced with @import statement to process it. Then it adapts
 * the path of those processed nuts to be accessible when exposed to the browser through WUIC uri.
 * </p>
 *
 * @author Guillaume DROUET
 * @version 1.5
 * @since 0.3.3
 */
public class CssUrlLineInspector extends RegexLineInspector implements NutFilterHolder {

    /**
     * Engines types that will be skipped when processing referenced nuts.
     */
    private static final EngineType[] SKIPPED_ENGINE = new EngineType[] {
            EngineType.AGGREGATOR,
            EngineType.CACHE,
            EngineType.INSPECTOR,
            EngineType.BINARY_COMPRESSION
    };

    /**
     * Url pattern.
     */
    private static final String URL_REGEX = String.format("(?:url\\(\\s*(%s|[^)]*)\\s*\\))", STRING_LITERAL_REGEX);

    /**
     * Finds the @import, background URL or @font-face statements within CSS script. The pattern describes a string:
     * <ul>
     * <li>starting with @import</li>
     * <li>followed any set of characters</li>
     * <li>followed by either a double quote or single quote</li>
     * <li>followed by any set of character until a repetition of the previously viewed quote is found</li>
     * </ul>
     *
     * OR
     *
     * <ul>
     * <li>starting with @import</li>
     * <li>followed by a set of characters with the term 'url' inside them</li>
     * <li>followed by a '('</li>
     * <li>followed by a set of characters</li>
     * <li>followed by a set of characters with ')' at the beginning and a ';' at the end</li>
     * </ul>
     *
     * OR
     * <ul>
     * <li>starting with background</li>
     * <li>followed by a string ending with 'url('</li>
     * <li>followed by either a single quote or a double quote (optional)</li>
     * <li>followed by everything which does not contain a repetition of the quote previously found</li>
     * </ul>
     *
     * OR
     * <ul>
     * <li>Starting with @font-face</li>
     * <li>followed by a string with '{' and ending with '}'</li>
     * </ul>
     */
    private static final Pattern CSS_URL_PATTERN = Pattern.compile(
            String.format("(/\\*(?:.)*?\\*/)|((?:@import.*?(%s|%s);?)|(?:background[(/\\*(?:.\\{\\})*?\\*/)\\s:\\w#-]*?(%s).*?);?)|(?:@font-face.*?\\{.*?\\})", URL_REGEX, STRING_LITERAL_REGEX, URL_REGEX), Pattern.DOTALL);

    /**
     * Three groups could contain the name, test the second one if first returns null.
     */
    private static final int[] GROUP_INDEXES = new int[] {
            // background case
            NumberUtils.HEIGHT,
            // import with url case
            NumberUtils.FOUR,
            // import without url case
            NumberUtils.THREE,
            // comment case
            1
    };

    /**
     * Logger.
     */
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    /**
     * The filters to apply.
     */
    private List<NutFilter> nutFilters;

    /**
     * <p>
     * Builds a new instance.
     * </p>
     */
    public CssUrlLineInspector() {
        super(CSS_URL_PATTERN);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<? extends ConvertibleNut> appendTransformation(final LineMatcher matcher,
                                                               final StringBuilder replacement,
                                                               final EngineRequest request,
                                                               final CompositeNut.CompositeInputStream cis,
                                                               final ConvertibleNut originalNut) throws WuicException {
        // Search the right group
        int i = 0;
        int groupIndex;
        String rawPath;
        String group = matcher.group();

        // in comment, ignoring
        if (matcher.group(1) != null)  {
            replacement.append(group);
            return null;
        }

        do {
            groupIndex = GROUP_INDEXES[i++];
            rawPath = matcher.group(groupIndex);
        } while (rawPath == null && i < GROUP_INDEXES.length);

        final NutsHeap heap;

        // @font-face case
        if (rawPath == null) {
            final Pattern patternUrl = Pattern.compile(URL_REGEX);
            final Matcher matcherUrl = patternUrl.matcher(matcher.group());
            final List<ConvertibleNut> retval = new ArrayList<ConvertibleNut>();
            final StringBuffer sb = new StringBuffer();
            heap = getHeap(request, originalNut, cis, matcher, 0);

            // Process each font URL inside the font rule
            while (matcherUrl.find()) {
                final StringBuilder replacementUrl = new StringBuilder();
                retval.addAll(processData(new MatcherData(matcherUrl, 1), replacementUrl, request, heap, originalNut));
                matcherUrl.appendReplacement(sb, replacementUrl.toString());
            }

            matcherUrl.appendTail(sb);
            replacement.append(sb.toString());

            return retval;
        } else {
            if (group.isEmpty()) {
                group = matcher.group(NumberUtils.TWO);
                heap = getHeap(request, originalNut, cis, matcher, NumberUtils.TWO);
            } else {
                heap = getHeap(request, originalNut, cis, matcher, groupIndex);
            }

            // The MatcherAdapter is provided by the inherited class
            final Matcher regex = MatcherAdapter.class.cast(matcher).getMatcher();
            return processData(new MatcherData(rawPath, regex, groupIndex, group), replacement, request, heap, originalNut);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String toString(final ConvertibleNut convertibleNut) throws IOException {
        switch (convertibleNut.getNutType()) {
            case CSS:
                return NutUtils.readTransform(convertibleNut);
            default:
                return null;
        }
    }

    /**
     * <p>
     * Process the given data to append the resulting URL to the specified {@link StringBuilder} and returns the extracted
     * nuts.
     * </p>
     *
     * @param data the data that contains matcher result
     * @param replacement the replacement
     * @param request the request
     * @param heap the heap
     * @param originalNut the original nut
     * @return the extracted nuts
     * @throws WuicException if processing fails
     */
    private List<? extends ConvertibleNut> processData(final MatcherData data,
                                                       final StringBuilder replacement,
                                                       final EngineRequest request,
                                                       final NutsHeap heap,
                                                       final Nut originalNut) throws WuicException {
        final Matcher matcher = data.getMatcher();
        final String rawPath = data.getGroupValue();
        final int groupIndex = data.getGroupIndex();
        final String group = data.getWrapper();

        // Compute once operations performed multiple times
        final int start = matcher.start(groupIndex) - matcher.start();
        String referencedPath = path(rawPath);

        // Ignore absolute CSS
        final Boolean isAbsolute = referencedPath.startsWith("http://") || referencedPath.startsWith("/");

        // Check filters
        if (!isAbsolute) {
             List<String> filtered = CollectionUtils.newList(referencedPath);

            if (nutFilters != null) {
                for (final NutFilter filter : nutFilters) {
                    filtered = filter.filterPaths(filtered);
                }
            }

            // Removed
            if (filtered.isEmpty()) {
                return Collections.emptyList();
            }
        }

        log.info("url statement found for nut {}", referencedPath);

        // Rewrite the statement from its beginning to the beginning of the nut name
        replacement.append(group.substring(0, start));

        // Write path to nut
        replacement.append("\"");

        List<? extends ConvertibleNut> res;

        // Don't change nut if absolute
        if (isAbsolute) {
            log.warn("{} is referenced as an absolute file and won't be processed by WUIC. You should only use relative URL reachable by nut DAO.", referencedPath);
            replacement.append(referencedPath);
            res = Collections.emptyList();
        } else if (referencedPath.startsWith("data:")) {
            // Ignore "data:" URL
            replacement.append(referencedPath);
            res = Collections.emptyList();
        } else {
            final List<Nut> nuts;
            try {
                // Extract the nut
                nuts = heap.create(originalNut, referencedPath, NutDao.PathFormat.RELATIVE_FILE, request.getProcessContext());
            } catch (IOException ioe) {
                WuicException.throwWuicException(ioe);
                return null;
            }

            if (nuts.isEmpty()) {
                fallbackToVersionNumberInQueryString(replacement, referencedPath, originalNut);
                res = Collections.emptyList();
            } else {
                res = manageAppend(new PipedConvertibleNut(nuts.iterator().next()), replacement, request, heap, SKIPPED_ENGINE);
            }
        }

        replacement.append("\"");
        replacement.append(group.substring(start + matcher.group(groupIndex).length()));

        // Return null means we don't change the original nut
        return res;
    }

    /**
     * <p>
     * Returns the nut path from the extracted path.
     * </p>
     *
     * @param referencedPath the referenced path
     * @return the path that could be retrieved from the dao
     */
    private String path(final String referencedPath) {
        String retval = referencedPath;

        // Quotes must be removed
        if (retval.charAt(0) == '\'' || retval.charAt(0) == '"') {
            retval = retval.substring(1, referencedPath.length() - 1);
        }

        // '?' or '#' could follow the extension
        int cutIndex = retval.lastIndexOf('?');

        if (cutIndex == -1) {
            cutIndex = retval.lastIndexOf('#');
        }

        return cutIndex != -1 ? retval.substring(0, cutIndex) : retval;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setNutFilter(final List<NutFilter> filters) {
        nutFilters = filters;
    }

    /**
     * <p>
     * This class wraps the analysis of results provided by a {@link Matcher}.
     * </p>
     *
     * @author Guillaume DROUET
     * @version 1.0
     * @since 0.4.5
     */
    private static final class MatcherData {

        /**
         * The value extracted from the group that corresponds to a path.
         */
        private String groupValue;

        /**
         * The group index that delimit the beginning of the replacement.
         */
        private int groupIndex;

        /**
         * The whole {@code String} that wraps the path.
         */
        private String wrapper;

        /**
         * The matcher.
         */
        private Matcher matcher;

        /**
         * <p>
         * Builds a new instance.
         * </p>
         *
         * @param groupValue the group value
         * @param matcher the matcher
         * @param groupIndex the group index
         * @param wrapper the wrapper
         */
        private MatcherData(final String groupValue, final Matcher matcher, final int groupIndex, final String wrapper) {
            this.groupValue = groupValue;
            this.matcher = matcher;
            this.groupIndex = groupIndex;
            this.wrapper = wrapper;
        }

        /**
         * <p>
         * Builds a new instance with the matcher's group for the specified index as group value and the whole matcher's
         * group as wrapper.
         * </p>
         *
         * @param matcher the matcher
         * @param groupIndex the group index
         */
        private MatcherData(final Matcher matcher, final int groupIndex) {
            this(matcher.group(groupIndex), matcher, groupIndex, matcher.group());
        }

        /**
         * <p>
         * Gets the group value.
         * </p>
         *
         * @return the value
         */
        private String getGroupValue() {
            return groupValue;
        }

        /**
         * <p>
         * Gets the group index.
         * </p>
         *
         * @return the index
         */
        private int getGroupIndex() {
            return groupIndex;
        }

        /**
         * <p>
         * Gets the wrapper.
         * </p>
         *
         * @return the wrapper
         */
        private String getWrapper() {
            return wrapper;
        }

        /**
         * <p>
         * Gets the matcher.
         * </p>
         *
         * @return the matcher
         */
        private Matcher getMatcher() {
            return matcher;
        }
    }
}
