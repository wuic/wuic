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


package com.github.wuic.engine;

import com.github.wuic.exception.WuicException;
import com.github.wuic.nut.CompositeNut;
import com.github.wuic.nut.ConvertibleNut;
import com.github.wuic.nut.Nut;
import com.github.wuic.nut.NutsHeap;
import com.github.wuic.util.IOUtils;
import com.github.wuic.util.NutUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <p>
 * Represents an object providing replacement functionality inside a line for a group of character matching a particular
 * pattern.
 * </p>
 *
 * @author Guillaume DROUET
 * @version 1.3
 * @since 0.3.3
 */
public abstract class LineInspector {

    /**
     * Possible value for path name.
     */
    protected static final String STRING_LITERAL_REGEX = "(\"(?:\\.|[^\\\"])*\"|'(?:\\.|[^\\'])*')";

    /**
     * Logger.
     */
    private final Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * The pattern.
     */
    private Pattern pattern;

    /**
     * <p>
     * Builds a new instance.
     * </p>
     *
     * @param p the pattern.
     */
    public LineInspector(final Pattern p) {
        pattern = p;
        logger.info("LineInspector {} will be inspected with pattern '{}'", getClass().getName(), p.toString());
    }

    /**
     * <p>
     * Manages the given nut that corresponds to the specified referenced path and append the transformation with proper
     * path.
     * </p>
     *
     * @param nut the nut retrieved with path
     * @param replacement the string builder to append
     * @param request the engine request
     * @param heap the heap that contains the original nut
     * @param skippedEngine the engine to skip when processing resulting nuts
     * @return the processed nuts specified in parameter
     * @throws WuicException if processing fails
     */
    public static List<? extends ConvertibleNut> manageAppend(final ConvertibleNut nut,
                                                              final StringBuilder replacement,
                                                              final EngineRequest request,
                                                              final NutsHeap heap,
                                                              final EngineType ... skippedEngine) throws WuicException {
        List<? extends ConvertibleNut> res;

        // If nut name is null, it means that nothing has been changed by the inspector
        res = Arrays.asList(nut);

        // Process nut
        final NodeEngine engine = request.getChainFor(nut.getInitialNutType());
        if (engine != null) {
            res = engine.parse(new EngineRequestBuilder(request).nuts(res).heap(heap).skip(skippedEngine).build());
        }

        // Use proxy URI if DAO provide it
        final String proxy = nut.getProxyUri();

        final ConvertibleNut resNut = res.isEmpty() ? null: res.get(0);
        final ConvertibleNut renamed;

        if (resNut != null) {
            renamed = resNut;
            resNut.setNutName(resNut.getName().replace("../", "a/../"));
        } else {
            renamed = nut;
        }

        if (proxy == null) {
            replacement.append(IOUtils.mergePath(
                    "/",
                    request.getContextPath(),
                    request.getWorkflowId(),
                    String.valueOf(NutUtils.getVersionNumber(nut)),
                    renamed.getName()));
        } else {
            replacement.append(proxy);
        }

        return res;
    }

    /**
     * <p>
     * Gets the {@link NutsHeap} pointing to the right {@link com.github.wuic.nut.dao.NutDao} according to the given
     * original nut.
     * </p>
     *
     * @param request the request
     * @param originalNut the original nut
     * @param cis the composite stream if any
     * @param matcher the matcher that inspects the stream
     * @param groupIndex the group index of currently matched value
     * @return the heap
     */
    public static NutsHeap getHeap(final EngineRequest request,
                                   final ConvertibleNut originalNut,
                                   final CompositeNut.CompositeInputStream cis,
                                   final Matcher matcher,
                                   final int groupIndex) {
        final NutsHeap heap = new NutsHeap(request.getHeap());
        final String name;

        // Extracts the location where nut is listed in order to compute the location of the extracted imported nuts
        if (cis == null) {
            name = originalNut.getName();
        } else {
            name = cis.nutAt(matcher.start(groupIndex)).getName();
        }

        final int lastIndexOfSlash = name.lastIndexOf('/') + 1;
        final String nutLocation = lastIndexOfSlash == 0 ? "" : name.substring(0, lastIndexOfSlash);
        heap.setNutDao(request.getHeap().withRootPath(nutLocation, originalNut), originalNut);
        return heap;
    }

    /**
     * <p>
     * Gets the pattern to find text to be replaced inside the lines.
     * </p>
     *
     * @return the pattern to use
     */
    public final Pattern getPattern() {
        return pattern;
    }

    /**
     * <p>
     * Builds a new {@link ReplacementInfo}.
     * </p>
     *
     * @param startIndex the start index
     * @param endIndex the end index
     * @param referencer the referencer
     * @param convertibleNuts the nuts
     * @return the new instance
     */
    public ReplacementInfo replacementInfo(final int startIndex,
                                           final int endIndex,
                                           final Nut referencer,
                                           final List<? extends ConvertibleNut> convertibleNuts) {
        return new ReplacementInfo(startIndex, endIndex, referencer, convertibleNuts);
    }

    /**
     * <p>
     * Computes the replacement to be made inside the text for the given {@code Matcher} which its {@code find()}
     * method as just been called.
     * </p>
     *
     * @param matcher the matcher which provides found text thanks to its {@code group()} method.
     * @param replacement the text which will replace the matching text
     * @param request the request that orders this transformation
     * @param cis a composite stream which indicates what nut owns the transformed text, {@code null} if the nut is not a composition
     * @param originalNut the original nut
     * @return the nut that was referenced in the matching text, {@code null} if the inspector did not perform any change
     * @throws WuicException if an exception occurs
     */
    public abstract List<? extends ConvertibleNut> appendTransformation(Matcher matcher,
                                                                        StringBuilder replacement,
                                                                        EngineRequest request,
                                                                        CompositeNut.CompositeInputStream cis,
                                                                        ConvertibleNut originalNut) throws WuicException;

    /**
     * <p>
     * Converts the given {@link ConvertibleNut} to a {@code String} representation that is possible to append directly
     * to the referencer. This could be used as an alternative to URL statements.
     * </p>
     *
     * @param convertibleNut the convertible nut to convert
     * @return the {@code String} representation, {@code null} if no inclusion is possible
     * @throws IOException if the transformation fails
     */
    protected abstract String toString(final ConvertibleNut convertibleNut) throws IOException;

    /**
     * <p>
     * This object indicates the index of the rewritten statement and the associated list of nuts.
     * </p>
     *
     * @author Guillaume DROUET
     * @version 1.0
     * @since 0.5.1
     */
    public final class ReplacementInfo {

        /**
         * Start index.
         */
        private int startIndex;

        /**
         * End index.
         */
        private int endIndex;

        /**
         * The nut referencing the convertibles nuts.
         */
        private Nut referencer;

        /**
         * The convertible nut.
         */
        private List<? extends ConvertibleNut> convertibleNuts;

        /**
         * <p>
         * Builds a new instance.
         * </p>
         *
         * @param startIndex the start index
         * @param endIndex the end index
         * @param referencer the referencer
         * @param convertibleNuts the nuts
         */
        private ReplacementInfo(final int startIndex,
                                final int endIndex,
                                final Nut referencer,
                                final List<? extends ConvertibleNut> convertibleNuts) {
            this.startIndex = startIndex;
            this.convertibleNuts = convertibleNuts;
            this.referencer = referencer;
            this.endIndex = endIndex;
            logger.debug("Statement between position {} and {} is referenced for replacement in referencer '{}'",
                    startIndex, endIndex, referencer.getInitialName());
        }

        /**
         * <p>
         * Gets the start index.
         * </p>
         *
         * @return the index
         */
        public int getStartIndex() {
            return startIndex;
        }

        /**
         * <p>
         * Gets the end index.
         * </p>
         *
         * @return the index
         */
        public int getEndIndex() {
            return endIndex;
        }

        /**
         * <p>
         * Gets the referencer.
         * </p>
         *
         * @return the referencer
         */
        public Nut getReferencer() {
            return referencer;
        }

        /**
         * <p>
         * Gets the nuts
         * </p>
         *
         * @return the list
         */
        public List<? extends ConvertibleNut> getConvertibleNuts() {
            return convertibleNuts;
        }

        /**
         * <p>
         * Returns a concatenated {@code String} representation of each  nut returned {@link #getConvertibleNuts()}.
         * </p>
         *
         * @return the string, {@code null} if no {@code String} representation could be provided
         */
        public String asString() throws IOException {
            final StringBuilder stringBuilder = new StringBuilder();

            for (final ConvertibleNut convertibleNut : getConvertibleNuts()) {
                final String str = LineInspector.this.toString(convertibleNut);

                if (str == null) {
                    return str;
                }

                stringBuilder.append(str);
            }

            return stringBuilder.toString();
        }
    }
}
