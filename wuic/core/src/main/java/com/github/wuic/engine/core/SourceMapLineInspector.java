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

import com.github.wuic.engine.EngineRequest;
import com.github.wuic.engine.EngineType;
import com.github.wuic.engine.LineInspector;
import com.github.wuic.engine.NodeEngine;
import com.github.wuic.engine.RegexLineInspector;
import com.github.wuic.engine.ScriptLineInspector;
import com.github.wuic.engine.WithScriptLineInspector;
import com.github.wuic.exception.WuicException;
import com.github.wuic.nut.CompositeNut;
import com.github.wuic.nut.ConvertibleNut;
import com.github.wuic.nut.Nut;
import com.github.wuic.nut.PipedConvertibleNut;
import com.github.wuic.nut.SourceMapNutImpl;
import com.github.wuic.nut.dao.NutDao;
import com.github.wuic.nut.NutsHeap;
import com.github.wuic.util.IOUtils;
import com.github.wuic.util.NutUtils;

import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * <p>
 * This inspector extracts the source map. It removes from scripts the "sourceMappingURL" statement when the path can't
 * be resolved and the chain is going to minify/aggregate it. "sourceMappingURL" is also no rewritten if the nuts are
 * served by WUIC since it will refers the source map with "X-SourceMap" header.
 * </p>
 *
 * <p>
 * A source map is not a sub resource since this is not required before loading a page.
 * </p>
 *
 * @author Guillaume DROUET
 * @since 0.4.5
 */
@WithScriptLineInspector(condition = ScriptLineInspector.ScriptMatchCondition.SINGLE_LINE_COMMENT)
public class SourceMapLineInspector extends RegexLineInspector {

    /**
     * The variable used in comments to indicate source map URL.
     */
    public static final String SOURCE_MAPPING_URL = "sourceMappingURL";

    /**
     * The pattern of 'sourceMappingURL'.
     */
    public static final Pattern SOURCE_MAPPING_PATTERN = Pattern.compile(SOURCE_MAPPING_URL + "=([^\\s]*)");

    /**
     * The engine that uses this inspector.
     */
    private NodeEngine engine;

    /**
     * <p>
     * Builds a new instance.
     * </p>
     *
     * @param enclosingEngine the engine that uses this injector
     */
    public SourceMapLineInspector(final NodeEngine enclosingEngine) {
        super(SOURCE_MAPPING_PATTERN);
        engine = enclosingEngine;
    }

    /**
     * <p>
     * Writes to the given stream the source map comment specified in parameter.
     * An attribute with {@link #SOURCE_MAPPING_URL} as key and the source map name as value is also added to the request.
     * </p>
     *
     * @param request the request
     * @param writer the stream
     * @param name the source map name
     * @throws IOException if any I/O error occurs
     */
    public static void writeSourceMapComment(final EngineRequest request, final Writer writer, final String name)
            throws IOException {
        // Add source map comment
        if (!request.isStaticsServedByWuicServlet()) {
            writer.write(String.format("%s//# sourceMappingURL=%s%s", IOUtils.NEW_LINE, name, IOUtils.NEW_LINE));
        }
    }

    /**
     * <p>
     * Creates a new instance.
     * </p>
     *
     * @param nodeEngine the enclosing engine
     * @return the new instance
     */
    public static LineInspector newInstance(final NodeEngine nodeEngine) {
        return ScriptLineInspector.wrap(new SourceMapLineInspector(nodeEngine), ScriptLineInspector.ScriptMatchCondition.SINGLE_LINE_COMMENT);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String toString(final ConvertibleNut convertibleNut) throws IOException {
        // Source map could only be referenced with an URL
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<AppendedTransformation> appendTransformation(final LineMatcher matcher,
                                                             final EngineRequest request,
                                                             final CompositeNut.CompositeInput cis,
                                                             final ConvertibleNut originalNut) throws WuicException {
        final StringBuilder replacement = new StringBuilder();
        final String referencedPath = matcher.group(1);

        // If a nut is already added, then skip
        if (NutUtils.findByName(originalNut, referencedPath) != null) {
            return null;
        }

        // Extract the nut
        final NutsHeap heap = getHeap(request, originalNut, cis, matcher.start(1));
        final List<Nut> nuts;

        try {
            nuts = heap.create(originalNut, referencedPath, NutDao.PathFormat.RELATIVE_FILE, request.getProcessContext());
        } catch (IOException ioe) {
            WuicException.throwWuicException(ioe);
            return null;
        }

        // There is a result
        if (!nuts.isEmpty()) {
            final Nut n = nuts.iterator().next();
            final List<? extends ConvertibleNut> res;

            // Do not rewrite the sourceMappingURL comment since the X-SourceMap header will be added by WUIC
            if (request.isStaticsServedByWuicServlet()) {
                res = manageAppend(new PipedConvertibleNut(n), new StringBuilder(), request, heap);
            } else {
                replacement.append(SOURCE_MAPPING_URL).append("=");
                res = manageAppend(new PipedConvertibleNut(n), replacement, request, heap);
            }

            // Should have only one nut
            for (final ConvertibleNut nut : res) {
                nut.setIsSubResource(false);
                originalNut.setSource(new SourceMapNutImpl(heap, originalNut, nut, request.getProcessContext()));
            }
        } else {
            // .map will be broken in case of minification or aggregation
            if (!rewriteStatement()) {
                return null;
            }

            // No result, at least add the version number in query string
            replacement.append(SOURCE_MAPPING_URL).append("=");
            fallbackToVersionNumberInQueryString(replacement, referencedPath, originalNut);
        }

        final String sourceMap = replacement.toString();

        // We don't need to do anything more with the source map
        return Arrays.asList(new AppendedTransformation(matcher.start(), matcher.end(), null, sourceMap));
    }

    /**
     * <p>
     * Indicates if an engine in the chain is either a working engine of type {@link EngineType#AGGREGATOR} or
     * {@link EngineType#MINIFICATION}. In that case, the source map URL statement should not be rewritten.
     * </p>
     *
     * @return {@code true} if source map URL can be written, {@code false} otherwise
     */
    private boolean rewriteStatement() {
        NodeEngine next = engine.getNext();

        while (next != null) {
            // Do not rewrite the statement
            if ((next.getEngineType().equals(EngineType.AGGREGATOR)
                    || next.getEngineType().equals(EngineType.MINIFICATION))
                    && next.works()) {
                return false;
            }

            next = next.getNext();
        }

        return true;
    }
}
