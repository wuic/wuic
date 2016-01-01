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


package com.github.wuic.engine;

import com.github.wuic.exception.WuicException;
import com.github.wuic.nut.CompositeNut;
import com.github.wuic.nut.ConvertibleNut;

import java.util.List;
import java.util.regex.MatchResult;

/**
 * <p>
 * This line inspector inspects a stream thanks to a {@link LineMatcher} to be provided by the subclass through the
 * {@link #lineMatcher(String)} method.
 * </p>
 *
 * @author Guillaume DROUET
 * @since 0.5.3
 */
public abstract class LineMatcherInspector extends LineInspector {

    /**
     * {@inheritDoc}
     */
    @Override
    public void inspect(final LineInspectorListener listener,
                        final char[] data,
                        final EngineRequest request,
                        final CompositeNut.CompositeInputStream cis,
                        final ConvertibleNut originalNut) throws WuicException {
        final LineMatcher matcher = lineMatcher(new String(data));

        while (matcher.find()) {
            // Compute replacement, extract nut name and referenced nuts
            final StringBuilder replacement = new StringBuilder();
            final List<? extends ConvertibleNut> res = appendTransformation(matcher, replacement, request, cis, originalNut);
            listener.onMatch(matcher.group(), matcher.start(), matcher.end(), replacement.toString(), res);
        }
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
     * @throws com.github.wuic.exception.WuicException if an exception occurs
     */
    protected abstract List<? extends ConvertibleNut> appendTransformation(LineMatcher matcher,
                                                                           StringBuilder replacement,
                                                                           EngineRequest request,
                                                                           CompositeNut.CompositeInputStream cis,
                                                                           ConvertibleNut originalNut) throws WuicException;
    /**
     * <p>
     * Creates a new line matcher.
     * </p>
     *
     * @param line the characters to match
     * @return the new object
     */
    public abstract LineMatcher lineMatcher(String line);

    /**
     * <p>
     * Represents a matcher for a given line and provides all the matching characters through the character streams.
     * The next {@link java.util.regex.MatchResult} state can be reached by calling the {@link #find()} method.
     * </p>
     *
     * @author Guillaume DROUET
     * @version 1.0
     * @since 0.5.3
     */
    public abstract static class LineMatcher implements MatchResult {

        /**
         * The character stream.
         */
        private final String line;

        /**
         * <p>
         * Builds a new instance.
         * </p>
         *
         * @param line the line
         */
        public LineMatcher(final String line) {
            this.line = line;
        }

        /**
         * <p>
         * Gets the character stream.
         * </p>
         *
         * @return the line
         */
        public String getLine() {
            return line;
        }

        /**
         * <p>
         * Returns {@code true} until the current state represents an existing match result.
         * Each time the method is called, the state of this object moves to the next matching characters in the stream.
         * </p>
         *
         * @return {@code true} is there is still a matching result, {@code false} otherwise
         */
        public abstract boolean find();
    }
}
