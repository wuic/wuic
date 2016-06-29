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

import com.github.wuic.NutType;
import com.github.wuic.config.Alias;
import com.github.wuic.config.BooleanConfigParam;
import com.github.wuic.config.Config;
import com.github.wuic.engine.EngineService;
import com.github.wuic.engine.LineInspector;
import com.github.wuic.engine.LineInspectorListener;
import com.github.wuic.engine.ScriptLineInspector;
import com.github.wuic.exception.WuicException;
import com.github.wuic.nut.ConvertibleNut;

import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;

import com.github.wuic.engine.EngineRequest;
import com.github.wuic.nut.CompositeNut;
import com.github.wuic.nut.SourceMapNut;
import com.github.wuic.nut.SourceMapNutImpl;
import com.github.wuic.util.IOUtils;

import static com.github.wuic.ApplicationConfig.COMPUTE_VERSION_ASYNCHRONOUSLY;

/**
 * <p>
 * This {@link com.github.wuic.engine.NodeEngine engine} can aggregate all the specified files in one path.
 * Files are aggregated in the order of apparition in the given list. Note that
 * nothing will be done if {@link TextAggregatorEngine#doAggregation} flag is {@code false}.
 * </p>
 *
 * <p>
 * When aggregation is enabled, this {@link com.github.wuic.engine.Engine} is added as a
 * {@link com.github.wuic.util.Pipe.Transformer} that captures all the "sourceMappingURL" statements
 * and create a new one that aggregates all of them.
 * </p>
 * 
 * @author Guillaume DROUET
 * @since 0.1.0
 */
@EngineService(injectDefaultToWorkflow = true, isCoreEngine = true)
@Alias("textAggregator")
public class TextAggregatorEngine extends AbstractAggregatorEngine implements EngineRequestTransformer.RequireEngineRequestTransformer {

    /**
     * For version number computation.
     */
    private Boolean canReadNutAsynchronously;

    /**
     * <p>
     * Configures asynchronous support.
     * </p>
     *
     * @param asynchronous activates asynchronous version number computation
     */
    @Config
    public void async(@BooleanConfigParam(defaultValue = true, propertyKey = COMPUTE_VERSION_ASYNCHRONOUSLY) final Boolean asynchronous) {
        canReadNutAsynchronously = asynchronous;
    }

    /**
     * <p>
     * Builds a new composite nut.
     * </p>
     *
     * @param request the request that initiates this call
     * @return the composition
     */
    public CompositeNut newCompositeNut(final EngineRequest request) {
        final String name = aggregationName(request.getNuts().get(0).getInitialNutType());
        return new CompositeNut(request.getCharset(),
                canReadNutAsynchronously,
                request.getPrefixCreatedNut().isEmpty() ? name : IOUtils.mergePath(request.getPrefixCreatedNut(), name),
                IOUtils.NEW_LINE.getBytes(),
                request.getProcessContext(),
                request.getNuts().toArray(new ConvertibleNut[request.getNuts().size()]));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean transform(final InputStream is, final OutputStream os, final ConvertibleNut convertible, final EngineRequest request) throws IOException {
        os.write(removeSourceMap(is, request, convertible));

        // We are able to compute the source map
        if (is instanceof CompositeNut.CompositeInputStream) {
            final CompositeNut.CompositeInputStream cis = CompositeNut.CompositeInputStream.class.cast(is);
            final List<ConvertibleNut> composition = cis.getCompositeNut().getCompositionList();

            try {
                //
                boolean firstNut = true;
                final ConvertibleNut first = composition.get(0);
                final SourceMapNut sourceMapNut;

                // Try to reuse the source map of the first nut if it already exists
                if (first.getSource() instanceof SourceMapNutImpl) {
                    sourceMapNut = SourceMapNutImpl.class.cast(first.getSource());
                    sourceMapNut.setNutName(convertible.getName() + NutType.MAP.getExtensions()[0]);
                } else {
                    sourceMapNut = new SourceMapNutImpl(convertible);
                    addToSource(sourceMapNut, first, cis);
                }

                // Add the range covered by each convertible nut to the source map
                for (final ConvertibleNut convertibleNut : composition) {

                    // Ignore the first nut as it source map is the composite one
                    if (firstNut) {
                        firstNut = false;

                        // Make sure the nut is added to the sources
                        sourceMapNut.addOriginalNut(convertibleNut);
                        continue;
                    }

                    addToSource(sourceMapNut, convertibleNut, cis);
                }

                // Set the aggregated source map
                convertible.setSource(sourceMapNut);

                // Write the statement at the end of the stream
                if (!request.isStaticsServedByWuicServlet()) {
                    os.write(IOUtils.NEW_LINE.getBytes());
                    os.write("//# sourceMappingURL=".getBytes());
                    os.write(sourceMapNut.getName().getBytes());
                    os.write(IOUtils.NEW_LINE.getBytes());
                }
            } catch (WuicException ex) {
                throw new IOException("Unable to build aggregated source map.", ex);
            }
        }

        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<ConvertibleNut> aggregationParse(final EngineRequest request) throws WuicException {

        // Do nothing if the configuration says that no aggregation should be done
        if (!works()) {
            return request.getNuts();
        }
        
        final List<ConvertibleNut> retval = new ArrayList<ConvertibleNut>();
        final CompositeNut compositeNut = newCompositeNut(request);

        // Proceed source map of each nut to aggregate all of them
        compositeNut.addTransformer(new EngineRequestTransformer(request, this, false, getEngineType().ordinal()));

        retval.add(compositeNut);

        return retval;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<NutType> getNutTypes() {
        return Arrays.asList(NutType.CSS, NutType.JAVASCRIPT);
    }

    /**
     * <p>
     * Adds to the given source a the mapping corresponding to the given {@link ConvertibleNut}.
     * </p>
     *
     * @param sourceMapNut the source to populate
     * @param convertibleNut the mapped nut
     * @param cis the {@link com.github.wuic.nut.CompositeNut.CompositeInputStream} indicating where mapping starts and ends
     */
    private void addToSource(final SourceMapNut sourceMapNut, final ConvertibleNut convertibleNut, final CompositeNut.CompositeInputStream cis) {
        // Get the start and end positions covered form the composite stream
        final CompositeNut.CompositeInputStream.Position lastPosition = cis.position(convertibleNut);
        final CompositeNut.CompositeInputStream.Position startPosition = lastPosition.startPosition();

        // Add the source mapping for the entire content
        sourceMapNut.addSource(startPosition.getLine(),
                startPosition.getColumn(),
                lastPosition.getLine(),
                lastPosition.getColumn(),
                convertibleNut);
    }

    /**
     * <p>
     * Removes from the given source map all the sourcemap comments.
     * </p>
     *
     * @param is the stream to read
     * @param request the request initiating this process
     * @param convertible the nut to transform
     * @return a byte array containing the content without sourcemap comments
     * @throws IOException if any I/O error occurs
     */
    private byte[] removeSourceMap(final InputStream is, final EngineRequest request, final ConvertibleNut convertible)
            throws IOException {
        // First read the stream and collect the single line comment ranges
        final CharArrayWriter charArrayWriter = new CharArrayWriter();
        IOUtils.copyStreamToWriterIoe(is, charArrayWriter);
        final char[] chars = charArrayWriter.toCharArray();

        final List<LineInspector.IndexRange> ranges = new ArrayList<LineInspector.IndexRange>();

        // This inspector just adds a range for each comment
        final ScriptLineInspector lineInspector = new ScriptLineInspector(ScriptLineInspector.ScriptMatchCondition.SINGLE_LINE_COMMENT) {
            @Override
            public Range doFind(final char[] buffer,
                                final int offset,
                                final int length,
                                final EngineRequest request,
                                final CompositeNut.CompositeInputStream cis,
                                final ConvertibleNut originalNut) {
                ranges.add(new IndexRange(offset, offset + length));
                return new Range(Range.Delimiter.END_SINGLE_LINE_OF_COMMENT, offset, offset + length);
            }

            @Override
            protected List<AppendedTransformation> appendTransformation(final char[] buffer, final int offset, final int length, final EngineRequest request, final CompositeNut.CompositeInputStream cis, final ConvertibleNut originalNut) throws WuicException {
                return Collections.emptyList();
            }

            @Override
            protected String toString(final ConvertibleNut convertibleNut) throws IOException {
                return null;
            }
        };

        try {
            // Perform range collection
            lineInspector.inspect(new LineInspectorListener() {
                @Override
                public void onMatch(final char[] data,
                                    final int offset,
                                    final int length,
                                    final String replacement,
                                    final List<? extends ConvertibleNut> extracted)
                        throws WuicException {
                }
            }, chars, request, null, convertible);
        } catch (WuicException we) {
            throw new IOException(we);
        }

        final StringBuilder builder = new StringBuilder();
        int offset = 0;

        // Erase all sourceMappingURL occurrences
        for (final LineInspector.IndexRange indexRange : ranges) {
            // Adds the content before the comment
            builder.append(chars, offset, indexRange.getStart() - offset);

            // Read the comment and extracts the sourcemaps
            final String comment = new String(chars, indexRange.getStart(), indexRange.getEnd() - indexRange.getStart());
            final Matcher matcher = SourceMapLineInspector.SOURCE_MAPPING_PATTERN.matcher(comment);
            final StringBuffer replacement = new StringBuffer();

            // Rewrite the comment without sourcemaps
            while (matcher.find()) {
                matcher.appendReplacement(replacement, "");
            }

            matcher.appendTail(replacement);
            builder.append(replacement);
            offset = indexRange.getEnd();
        }

        // Append tail
        if (offset < chars.length - 1) {
            builder.append(chars, offset, chars.length - offset);
        }

        return builder.toString().getBytes();
    }
}
