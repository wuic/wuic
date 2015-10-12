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


package com.github.wuic.util;

import com.github.wuic.nut.CompositeNut;
import com.github.wuic.nut.ConvertibleNut;
import com.github.wuic.nut.NutWrapper;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * <p>
 * This class helps to chains transformers that read {@link InputStream} and writes some transformed bytes to a
 * {@link ByteArrayOutputStream}.
 * </p>
 *
 * @param <T> the type of convertible object
 * @author Guillaume DROUET
 * @version 1.0
 * @since 0.5.0
 */
public final class Pipe<T extends ConvertibleNut> {

    /**
     * <p>
     * Executes the given pipe and writes the result into the given {@link OutputStream}.
     * </p>
     *
     * @param pipe the pipe to execute
     * @param os   the stream to write
     * @param <T>  the type of object converted by the piped transformers
     * @throws IOException if transformation fails
     */
    public static <T extends ConvertibleNut> void executeAndWriteTo(final Pipe<T> pipe, final List<OnReady> callbacks, final OutputStream os)
            throws IOException {
        final OnReady onReady = new DefaultOnReady(os);

        if (callbacks != null) {
            callbacks.add(onReady);
            pipe.execute(callbacks.toArray(new OnReady[callbacks.size()]));
        } else {
            pipe.execute(onReady);
        }
    }

    /**
     * <p>
     * This interface describes an object which is able to write into an {@link OutputStream} some transformed bytes
     * read from {@link InputStream}.
     * </p>
     * <p/>
     * <p>
     * When registered with the {@link Pipe}, the close method is invoked transparently.
     * </p>
     * <p/>
     * <p>
     * That transformer is also able to change the state of on 'convertible' object identified as the origin of the
     * source input stream.
     * </p>
     *
     * @param <T> the type of convertible object
     * @author Guillaume DROUET
     * @version 1.0
     * @since 0.5.0
     */
    public interface Transformer<T> {

        /**
         * <p>
         * Reads the stream and writes the transformed bytes.
         * </p>
         *
         * @param is          the input
         * @param os          the output
         * @param convertible the object that provides the original input stream
         * @throws IOException if an I/O error occurs
         */
        void transform(InputStream is, OutputStream os, T convertible) throws IOException;

        /**
         * <p>
         * Indicates if the content generated when {@link #transform(InputStream, OutputStream, Object)} is called can
         * be aggregated to an other one. This is usually the case but won't be possible for instance when a magic
         * number should appears only at the beginning of the composite stream and not in each stream of the aggregation.
         * </p>
         *
         * @return {@code true} if aggregation is possible, {@code false} otherwise
         */
        boolean canAggregateTransformedStream();
    }

    /**
     * <p>
     * This class represents an object notified when a transformation is done.
     * </p>
     *
     * @author Guillaume DROUET
     * @version 1.0
     * @since 0.5.0
     */
    public interface OnReady {

        /**
         * <p>
         * Called once all transformers have been called to transform the source stream.
         * </p>
         *
         * @param e the current execution
         * @throws IOException if callback throws an I/O error
         */
        void ready(Execution e) throws IOException;
    }

    /**
     * <p>
     * Default implementation which writes the result to a wrapped output stream (binary) or writer (text) and close if automatically.
     * </p>
     *
     * @author Guillaume DROUET
     * @version 1.0
     * @since 0.5.0
     */
    public static class DefaultOnReady implements OnReady {

        /**
         * The output stream.
         */
        private OutputStream outputStream;

        /**
         * The charset.
         */
        private String charset;

        /**
         * <p>
         * Builds a new instance for binary copy.
         * </p>
         *
         * @param os the stream to write
         */
        public DefaultOnReady(final OutputStream os) {
            this(os, null);
        }

        /**
         * <p>
         * Builds a new instance for text copy.
         * </p>
         *
         * @param os the stream to write
         * @param cs the charset for character stream
         */
        public DefaultOnReady(final OutputStream os, final String cs) {
            this.outputStream = os;
            this.charset = cs;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void ready(final Pipe.Execution e) throws IOException {
            try {
                e.writeResultTo(outputStream, charset);
            } finally {
                IOUtils.close(outputStream);
            }
        }
    }

    /**
     * <p>
     * The default transformer just copies the {@link InputStream} to the {@link OutputStream} and produces a content
     * which can be aggregated to an other one.
     * </p>
     *
     * @param <T> the type of convertible object
     * @author Guillaume DROUET
     * @version 1.0
     * @since 0.5.0
     */
    public static class DefaultTransformer<T> implements Transformer<T> {

        /**
         * {@inheritDoc}
         */
        @Override
        public void transform(final InputStream is, final OutputStream os, final T convertible) throws IOException {
            IOUtils.copyStream(is, os);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean canAggregateTransformedStream() {
            return true;
        }
    }

    /**
     * <p>
     * This class represents the result of the transformation made by thanks to piped transformers.
     * </p>
     *
     * @author Guillaume DROUET
     * @version 1.0
     * @since 0.5.0
     */
    public static final class Execution {

        /**
         * The result.
         */
        private byte[] result;

        /**
         * <p>
         * Builds a new instance.
         * </p>
         *
         * @param r the result
         */
        public Execution(final byte[] r) {
            this.result = new byte[r.length];
            System.arraycopy(r, 0, result, 0, r.length);
        }

        /**
         * <p>
         * Gets the result length.
         * </p>
         *
         * @return the result
         */
        public int getContentLength() {
            return result.length;
        }

        /**
         * <p>
         * Writes the result to the given output stream (for binary stream).
         * </p>
         *
         * @param os the output stream
         * @throws IOException if copy fails
         */
        public void writeResultTo(final OutputStream os) throws IOException {
            writeResultTo(os, null);
        }

        /**
         * <p>
         * Writes the result to the given output stream. Charset could be specified for char stream.
         * </p>
         *
         * @param os      the output stream
         * @param charset the charset for character stream ({@code null} for binary stream)
         * @throws IOException if copy fails
         */
        public void writeResultTo(final OutputStream os, final String charset) throws IOException {
            InputStream is = null;

            try {
                is = new ByteArrayInputStream(result);

                if (charset == null) {
                    IOUtils.copyStream(is, os);
                } else {
                    IOUtils.copyStreamToWriterIoe(is, new OutputStreamWriter(os, charset));
                }
            } finally {
                IOUtils.close(is);
            }
        }
    }

    /**
     * Input stream of the pipe.
     */
    private InputStream inputStream;

    /**
     * Nodes that wrap registered {@link Transformer transformers}.
     */
    private Deque<Transformer<T>> transformers;

    /**
     * The convertible object.
     */
    private T convertible;

    /**
     * <p>
     * Creates a new instance with an input.
     * </p>
     *
     * @param c  the convertible bound to the input stream
     * @param is the {@link InputStream}
     */
    public Pipe(final T c, final InputStream is) {
        convertible = c;
        inputStream = is;
        transformers = new LinkedList<Transformer<T>>();
    }

    /**
     * <p>
     * Registers the given {@link Transformer} to the pipe. The {@link InputStream} sent to this transformer will be
     * connected to the {@link OutputStream} of the {@link Transformer} previously registered.
     * </p>
     *
     * @param transformer the transformer
     * @throws IOException if streams could not be piped
     */
    public void register(final Transformer<T> transformer) throws IOException {
        if (!transformers.isEmpty()
                && !transformers.getLast().canAggregateTransformedStream()
                && transformer.canAggregateTransformedStream()) {
            throw new IllegalArgumentException(
                    "You can't add a transformer which produces a stream which could be aggregated after a stream which doesn't.");
        }

        transformers.addLast(transformer);
    }

    /**
     * <p>
     * Executes this pipe by writing to a byte array the result generated by all registered {@link Transformer transformers}.
     * </p>
     *
     * @param onReady callback
     * @throws IOException if an I/O error occurs
     */
    public void execute(final OnReady... onReady) throws IOException {
        execute(false, onReady);
    }

    /**
     * <p>
     * Executes this pipe by writing to a byte array the result generated by the given {@link Transformer transformers}.
     * </p>
     *
     * <p>
     * Once transformation has been done, callback is notified.
     * </p>
     *
     * <p>
     * If the given {@code InputStream} is a {@link com.github.wuic.nut.CompositeNut.CompositeInputStream} that should not
     * be ignored, each nut of the composition is transformed and the result is aggregated and is used as an input for the
     * transformers that support transformation of aggregated streams only.
     * </p>
     *
     * @param ignoreCompositeStream ignores the fact that the {@code InputStream} is a {@link com.github.wuic.nut.CompositeNut.CompositeInputStream}
     * @param onReady callback
     * @throws IOException if an I/O error occurs
     */
    public void execute(final boolean ignoreCompositeStream, final OnReady... onReady)
            throws IOException {
        final ByteArrayOutputStream os = new ByteArrayOutputStream();

        if (!ignoreCompositeStream && (inputStream instanceof CompositeNut.CompositeInputStream)) {
            transform(convertible, CompositeNut.CompositeInputStream.class.cast(inputStream)).execute(true, onReady);
        } else {

            // No transformer, simply copy streams
            if (transformers.isEmpty()) {
                try {
                    IOUtils.copyStream(inputStream, os);
                } finally {
                    IOUtils.close(inputStream, os);
                }
            } else {
                InputStream is = inputStream;

                // Make transformation
                for (final Transformer<T> t : transformers) {
                    // Pipe transformers with in memory byte arrays
                    if (!t.equals(transformers.getLast())) {
                        final OutputStream out;

                        // Keep the composition information
                        out = new ByteArrayOutputStream();

                        try {
                            t.transform(is, out, convertible);
                        } finally {
                            IOUtils.close(is, out);
                        }

                        // Retrieve the new CompositeInputStream
                        is = new ByteArrayInputStream(ByteArrayOutputStream.class.cast(out).toByteArray());
                    } else {
                        try {
                            // Last transformation
                            t.transform(is, os, convertible);
                        } finally {
                            IOUtils.close(is, os);
                        }
                    }
                }
            }

            final Execution e = new Execution(os.toByteArray());

            // Notify callbacks
            for (final OnReady callback : onReady) {
                callback.ready(e);
            }
        }
    }

    /**
     * <p>
     * Executes the transformation for the given {@link com.github.wuic.nut.CompositeNut.CompositeInputStream}.
     * The method isolate the transformation of each nut inside the composition and then returns a {@code Pipe} with
     * the remaining transformers that should be applied on the aggregated stream.
     * </p>
     *
     * @param nut the {@link ConvertibleNut}
     * @param cis the opened {@link com.github.wuic.nut.CompositeNut.CompositeInputStream}
     * @return the pipe with remaining transformations
     * @throws IOException if transformation fails
     */
    private Pipe<ConvertibleNut> transform(final ConvertibleNut nut, final CompositeNut.CompositeInputStream cis)
            throws IOException {
        final Pipe<ConvertibleNut> finalPipe;
        final boolean hasTransformers = (convertible.getTransformers() != null & !convertible.getTransformers().isEmpty())
                || hasTransformers(cis.getCompositeNut().getCompositionList());

        // Collect transformer executed for each nut and group transformers for aggregated content
        if (hasTransformers) {
            // Get transformers
            final Set<Transformer<ConvertibleNut>> aggregatedStream = new LinkedHashSet<Transformer<ConvertibleNut>>();
            final Map<ConvertibleNut, Set<Transformer<ConvertibleNut>>> nuts = new LinkedHashMap<ConvertibleNut, Set<Transformer<ConvertibleNut>>>();
            final CompositeNut composite = cis.getCompositeNut();
            populateTransformers(convertible.getTransformers(), aggregatedStream, nuts, composite.getCompositionList());

            // First: transform each nut
            final CompositeNut.CompositeInputStream is = composite.openStream(
                    transformBeforeAggregate(nuts, composite.getCompositionList(), nut));

            // Aggregate the results
            finalPipe = new Pipe<ConvertibleNut>(nut, is);

            // Transform the aggregated results
            for (final Pipe.Transformer<ConvertibleNut> transformer : aggregatedStream) {
                finalPipe.register(transformer);
            }
        } else {
            finalPipe = new Pipe<ConvertibleNut>(nut, nut.openStream());
        }

        return finalPipe;
    }

    /**
     * <p>
     * Indicates if at least one nut of the composition has a transformer.
     * </p>
     *
     * @param compositionList the composition
     * @return {@code true} is a transformer exists inside the composition, {@code false} otherwise
     */
    public boolean hasTransformers(final List<ConvertibleNut> compositionList) {
        for (final ConvertibleNut nut : compositionList) {
            if (nut.getTransformers() != null) {
                return true;
            }
        }

        return false;
    }

    /**
     * <p>
     * Transforms each nut with transformers producing content which could be aggregated.
     * The transformation result of each nut is returned as a list of {@link InputStream} that should be used to create
     * a {@link com.github.wuic.nut.CompositeNut.CompositeInputStream}.
     * </p>
     *
     * @param nuts the nuts to transform with their transformers
     * @param compositionList the composition list
     * @param convertibleNut the nut representing the composition
     * @return the input streams of corresponding to the composition where transformed content is accessible
     * @throws IOException if transformation fails
     */
    private List<InputStream> transformBeforeAggregate(final Map<ConvertibleNut, Set<Transformer<ConvertibleNut>>> nuts,
                                                       final List<ConvertibleNut> compositionList,
                                                       final ConvertibleNut convertibleNut)
            throws IOException {
        final List<InputStream> is = new ArrayList<InputStream>(compositionList.size());

        for (final Map.Entry<ConvertibleNut, Set<Transformer<ConvertibleNut>>> entry : nuts.entrySet()) {
            final ByteArrayOutputStream bos = new ByteArrayOutputStream();

            if (!entry.getValue().isEmpty()) {

                // We pass this composition in order to receive any referenced nut or whatever state change
                final Pipe<ConvertibleNut> pipe = new Pipe<ConvertibleNut>(new NutWrapper(convertibleNut) {

                    /**
                     * {@inheritDoc}
                     */
                    @Override
                    public String getName() {
                        return entry.getKey().getName();
                    }
                }, entry.getKey().openStream());

                for (final Pipe.Transformer<ConvertibleNut> transformer : entry.getValue()) {
                    pipe.register(transformer);
                }

                Pipe.executeAndWriteTo(pipe, entry.getKey().getReadyCallbacks(), bos);
                is.add(new ByteArrayInputStream(bos.toByteArray()));
            } else {
                is.add(entry.getKey().openStream());
            }
        }

        return is;
    }

    /**
     * <p>
     * Populates the given {@code Set} and {@code Map} with the transformers inside the composition.
     * If the {@link com.github.wuic.util.Pipe.Transformer#canAggregateTransformedStream()} returns {@code true}, then
     * it's added to a list associated to the nut with the given map. Otherwise the transformer will be added in the
     * set specified in parameter.
     * </p>
     *
     * <p>
     * The method also populates each the maps with the given common set of transformers.
     * </p>
     *
     * @param commonTransformers transformers that are common to all nuts
     * @param aggregatedStream transformer that operate on aggregated streams
     * @param nuts all nuts with their transformer that produce a stream that can be aggregated later
     * @param compositionList the composition
     */
    private void populateTransformers(final Set<Transformer<ConvertibleNut>> commonTransformers,
                                      final Set<Pipe.Transformer<ConvertibleNut>> aggregatedStream,
                                      final Map<ConvertibleNut, Set<Transformer<ConvertibleNut>>> nuts,
                                      final Collection<ConvertibleNut> compositionList) {
        for (final ConvertibleNut nut : compositionList) {
            final Set<Pipe.Transformer<ConvertibleNut>> separateStream = new LinkedHashSet<Transformer<ConvertibleNut>>();
            populateTransformers(separateStream, aggregatedStream, nut.getTransformers());
            populateTransformers(separateStream, aggregatedStream, commonTransformers);
            nuts.put(nut, separateStream);
        }
    }

    /**
     * <p>
     * Populate the given maps with the specified transformers as explained in
     * {@link #populateTransformers(java.util.Set, java.util.Set, java.util.Map, java.util.Collection)}.
     * </p>
     *
     * @param separateStream the transformers to apply for each nut
     * @param aggregatedStream the transformers to apply to the aggregated stream
     * @param transformers the transformers to populate the map
     * @see #populateTransformers(java.util.Set, java.util.Set, java.util.Map, java.util.Collection)
     */
    private void populateTransformers(final Set<Pipe.Transformer<ConvertibleNut>> separateStream,
                                      final Set<Pipe.Transformer<ConvertibleNut>> aggregatedStream,
                                      final Collection<Transformer<ConvertibleNut>> transformers) {
        if (transformers != null) {
            for (final Pipe.Transformer<ConvertibleNut> transformer : transformers) {
                if (transformer.canAggregateTransformedStream()) {
                    separateStream.add(transformer);
                } else {
                    aggregatedStream.add(transformer);
                }
            }
        }
    }
}
