/*
 * Copyright (c) 2016   The authors of WUIC
 *
 * License/Terms of Use
 * Permission is hereby granted, free of charge and for the term of intellectual
 * property rights on the Software, to any person obtaining a copy of this software
 * and associated documentation files (the "Software"), to use, copy, modify and
 * propagate free of charge, anywhere in the world, all or part of the Software
 * subject to the following mandatory conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, PEACEFUL ENJOYMENT,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS
 * OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */


package com.github.wuic.util;

import com.github.wuic.Logging;
import com.github.wuic.engine.core.EngineRequestTransformer;
import com.github.wuic.exception.WuicException;
import com.github.wuic.mbean.TransformationStat;
import com.github.wuic.nut.CompositeNut;
import com.github.wuic.nut.ConvertibleNut;
import com.github.wuic.nut.NutWrapper;
import com.github.wuic.nut.Source;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * <p>
 * This class helps to chains transformers that read {@link InputStream} and writes some transformed bytes to a
 * {@link ByteArrayOutputStream}.
 * </p>
 *
 * @param <T> the type of convertible object
 * @author Guillaume DROUET
 * @since 0.5.0
 */
public final class Pipe<T extends ConvertibleNut> {

    /**
     * <p>
     * Executes the given pipe and writes the result into the given {@link Output}.
     * </p>
     *
     * @param pipe the pipe to execute
     * @param os   the stream to write
     * @param <T>  the type of object converted by the piped transformers
     * @throws IOException if transformation fails
     */
    public static <T extends ConvertibleNut> void executeAndWriteTo(final Pipe<T> pipe, final List<OnReady> callbacks, final Output os)
            throws IOException {
        final OnReady onReady = new DefaultOnReady(os);

        if (callbacks != null) {
            final OnReady[] cb = callbacks.toArray(new OnReady[callbacks.size() + 1]);
            cb[callbacks.size()] = onReady;
            pipe.execute(cb);
        } else {
            pipe.execute(onReady);
        }
    }

    /**
     * <p>
     * This interface describes an object which is able to write into an {@link OutputStream} some transformed bytes
     * read from {@link InputStream}.
     * </p>
     *
     * <p>
     * When registered with the {@link Pipe}, the close method is invoked transparently.
     * </p>
     *
     *  <p>
     * That transformer is also able to change the state of on 'convertible' object identified as the origin of the
     * source input stream.
     * </p>
     *
     * @param <T> the type of convertible object
     * @author Guillaume DROUET
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
         * @return {@code true} if the input stream has been read and the output stream used, {@code false} otherwise
         * @throws IOException if an I/O error occurs
         */
        boolean transform(Input is, Output os, T convertible) throws IOException;

        /**
         * <p>
         * Indicates if the content generated when {@link #transform(Input, Output, Object)} is called can
         * be aggregated to an other one. This is usually the case but won't be possible for instance when a magic
         * number should appears only at the beginning of the composite stream and not in each stream of the aggregation.
         * </p>
         *
         * @return {@code true} if aggregation is possible, {@code false} otherwise
         */
        boolean canAggregateTransformedStream();

        /**
         * <p>
         * Order of execution of that transformer comparing to the others.
         * </p>
         *
         * @return the order of that transformer
         */
        int order();
    }

    /**
     * <p>
     * This class represents an object notified when a transformation is done.
     * </p>
     *
     * @author Guillaume DROUET
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
     * This class holds a set of transformers associated to a convertible nut.
     * </p>
     *
     * @author Guillaume DROUET
     * @since 0.5.0
     */
    private static class PerNutTransformation {

        /**
         * The transformers.
         */
        private Set<Transformer<ConvertibleNut>> transformers;

        /**
         * The nut.
         */
        private ConvertibleNut convertibleNut;

        /**
         * <p>
         * Builds a new instance.
         * </p>
         *
         * @param transformers the transformers
         * @param convertibleNut the nut
         */
        public PerNutTransformation(final Set<Transformer<ConvertibleNut>> transformers, final ConvertibleNut convertibleNut) {
            this.transformers = transformers;
            this.convertibleNut = convertibleNut;
        }

        /**
         * <p>
         * Gets the transformers.
         * </p>
         *
         * @return the transformers
         */
        public Set<Transformer<ConvertibleNut>> getTransformers() {
            return transformers;
        }

        /**
         * <p>
         * Gets the nut to transform.
         * </p>
         *
         * @return the nut
         */
        public ConvertibleNut getConvertibleNut() {
            return convertibleNut;
        }
    }

    /**
     * <p>
     * Default implementation which writes the result to a wrapped output stream (binary) or writer (text) and close if automatically.
     * </p>
     *
     * @author Guillaume DROUET
     * @since 0.5.0
     */
    public static class DefaultOnReady implements OnReady {

        /**
         * The output stream.
         */
        private Output output;

        /**
         * <p>
         * Builds a new instance for text copy.
         * </p>
         *
         * @param os the stream to write
         */
        public DefaultOnReady(final Output os) {
            this.output = os;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void ready(final Pipe.Execution e) throws IOException {
            try {
                if (e.isText()) {
                    e.writeResultTo(output.writer());
                } else {
                    e.writeResultTo(output.outputStream());
                }
            } finally {
                IOUtils.close(output);
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
     * @since 0.5.0
     */
    public static class DefaultTransformer<T> implements Transformer<T> {

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean transform(final Input is, final Output os, final T convertible) throws IOException {
            return false;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean canAggregateTransformedStream() {
            return true;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int order() {
            return 0;
        }
    }

    /**
     * <p>
     * This comparator allows to sort  {@link Transformer transformers} by {@link com.github.wuic.util.Pipe.Transformer#order() order}.
     * </p>
     *
     * @author Guillaume DROUET
     * @since 0.5.3
     */
    public static final class TransformerComparator implements Comparator<Transformer> {

        /**
         * {@inheritDoc}
         */
        @Override
        public int compare(final Transformer o1, final Transformer o2) {
            return o1.order() - o2.order();
        }
    }

    /**
     * <p>
     * This class represents the result of the transformation made by thanks to piped transformers.
     * </p>
     *
     * @author Guillaume DROUET
     * @since 0.5.0
     */
    public static final class Execution {

        /**
         * The result in bytes.
         */
        private byte[] byteResult;

        /**
         * The result in chars.
         */
        private char[] charResult;

        /**
         * The charset for {@code String} conversion.
         */
        private Charset charset;

        /**
         * Indicates if the base stream is in byte (binary) or char (text).
         */
        private final boolean text;

        /**
         * <p>
         * Builds a new instance. All execution must provide the charset.
         * </p>
         *
         * @param e the executions
         */
        public Execution(final List<Execution> e) {
            if (e.isEmpty()) {
                WuicException.throwBadArgumentException(new IllegalArgumentException("Executions list can't be empty."));
            }

            char[][] charArray = null;
            byte[][] byteArray = null;
            int length = 0;

            // Collect the byte or char array from each execution
            for (int i = 0; i < e.size(); i++) {
                final Execution execution = e.get(i);

                if (i == 0) {
                    charset = execution.charset;

                    if (execution.isText()) {
                        charArray = new char[e.size()][];
                        charArray[i] = execution.charResult;
                        length += execution.charResult.length;
                    } else {
                        byteArray = new byte[e.size()][];
                        byteArray[i] = execution.byteResult;
                        length += execution.byteResult.length;
                    }
                } else {
                    if (!charset.equals(execution.charset)) {
                        WuicException.throwBadArgumentException(new IllegalArgumentException(
                                String.format("Executions don't share the same charset: %s != %s", execution.charset, charset)));
                    }

                    if (charArray != null) {
                        final char[] array = execution.isText() ? execution.charResult : IOUtils.toChars(charset, execution.byteResult);

                        charArray[i] = array;
                        length += array.length;
                    } else {
                        final byte[] array = execution.isText()
                                ? IOUtils.toBytes(charset, execution.getCharResult()) : execution.byteResult;

                        byteArray[i] = array;
                        length += array.length;
                    }
                }
            }

            // Aggregate byte stream
            if (byteArray != null) {
                byteResult = new byte[length];
                int offset = 0;

                for (final byte[] b : byteArray) {
                    System.arraycopy(b, 0, byteResult, offset, b.length);
                    offset += b.length;
                }
            } else if (charArray != null) {
                // Aggregate char stream
                charResult = new char[length];
                int offset = 0;

                for (final char[] c : charArray) {
                    System.arraycopy(c, 0, charResult, offset, c.length);
                    offset += c.length;
                }
            }

            text = charResult != null;
        }

        /**
         * <p>
         * Builds a new instance.
         * </p>
         *
         * @param charset the charset
         * @param b the result in bytes
         */
        public Execution(final byte[] b, final String charset) {
            if (b == null) {
                throw new IllegalArgumentException("Given byte array is null.");
            }

            this.byteResult = Arrays.copyOf(b, b.length);
            this.charset = Charset.forName(charset);
            this.text = false;
        }

        /**
         * <p>
         * Builds a new instance.
         * </p>
         *
         * @param charset the charset
         * @param c the result in chars
         */
        public Execution(final char[] c, final String charset) {
            if (c == null) {
                throw new IllegalArgumentException("Given char array is null.");
            }

            this.charResult = Arrays.copyOf(c, c.length);
            this.charset = Charset.forName(charset);
            this.text = true;
        }

        /**
         * <p>
         * Gets the result length.
         * </p>
         *
         * @return the result
         */
        public int getContentLength() {
            return charResult == null ? byteResult.length : charResult.length;
        }

        /**
         * <p>
         * Gets the byte array.
         * </p>
         *
         * @return the byte array
         */
        public byte[] getByteResult() {
            if (byteResult == null) {
                byteResult = IOUtils.toBytes(charset, charResult);
            }

            return byteResult;
        }

        /**
         * <p>
         * Gets the char array.
         * </p>
         *
         * @return the char array
         */
        public char[] getCharResult() {
            if (charResult == null) {
                charResult = IOUtils.toChars(charset, byteResult);
            }

            return charResult;
        }

        /**
         * <p>
         * Indicates if this executions writes chars or bytes.
         * If {@code true}, {@link #writeResultTo(java.io.Writer)} should be used.
         * Otherwise use {@link #writeResultTo(java.io.OutputStream)}.
         * </p>
         *
         * @return {@code true} in case of text data (chars), {@code false} in case of binary data (bytes)
         */
        public boolean isText() {
            return text;
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
            os.write(getByteResult());
            os.flush();
        }

        /**
         * <p>
         * Writes the result to the given writer.
         * </p>
         *
         * @param writer the writer
         * @throws IOException if copy fails
         */
        public void writeResultTo(final Writer writer) throws IOException {
            writer.write(getCharResult());
            writer.flush();
        }

        /**
         * <p>
         * Creates a {@code String} from this execution.
         * </p>
         *
         * @return the {@code String} representation of execution's content
         */
        @Override
        public String toString() {
            return isText() ? new String(getCharResult()) : new String(getByteResult(), charset);
        }
    }

    /**
     * The timer tree factory.
     */
    private TimerTreeFactory timerTreeFactory;

    /**
     * Input stream of the pipe.
     */
    private Input inputStream;

    /**
     * Nodes that wrap registered {@link Transformer transformers}.
     */
    private Deque<Transformer<T>> transformers;

    /**
     * The convertible object.
     */
    private T convertible;

    /**
     * Execution statistics grouped by transformer.
     */
    private Map<String, List<TransformationStat>> statistics;

    /**
     * <p>
     * Creates a new instance with an input stream.
     * </p>
     *
     * @param c  the convertible bound to the input stream
     * @param is the {@link InputStream}
     */
    public Pipe(final T c, final Input is) {
        this(c, is, new TimerTreeFactory());
    }

    /**
     * <p>
     * Creates a new instance with an input stream.
     * </p>
     *
     * @param c  the convertible bound to the input stream
     * @param is the {@link InputStream}
     * @param ttf an existing {@code TimerTreeFactory}
     */
    public Pipe(final T c, final Input is, final TimerTreeFactory ttf) {
        convertible = c;
        inputStream = is;
        transformers = new LinkedList<Transformer<T>>();
        statistics = new TreeMap<String, List<TransformationStat>>();
        timerTreeFactory = ttf;
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
     * If the given {@code InputStream} is a {@link com.github.wuic.nut.CompositeNut.CompositeInput} that should not be
     * ignored, each nut of the composition is transformed and the result is aggregated and is used as an input for the
     * transformers that support transformation of aggregated streams only.
     * </p>
     *
     * @param ignoreCompositeStream ignores the fact that the {@code InputStream} is a {@link CompositeNut.CompositeInput}
     * @param onReady callback
     * @throws IOException if an I/O error occurs
     */
    public void execute(final boolean ignoreCompositeStream, final OnReady... onReady)
            throws IOException {
        final Output os = new InMemoryOutput(inputStream.getCharset());

        if (!ignoreCompositeStream && (inputStream instanceof CompositeNut.CompositeInput)) {
            final Pipe<ConvertibleNut>  p = transform(convertible, CompositeNut.CompositeInput.class.cast(inputStream));
            p.execute(true, onReady);
            CollectionUtils.merge(p.statistics, statistics);
        } else {
            boolean written = false;
            Input is = inputStream;

            // Make transformation
            for (final Transformer<T> t : transformers) {
                final Timer timer = timerTreeFactory.getTimerTree();
                timer.start();

                // Pipe transformers with in memory byte arrays
                if (!t.equals(transformers.getLast())) {
                    final InMemoryOutput out;

                    // Keep the composition information
                    out = new InMemoryOutput(inputStream.getCharset());

                    try {
                        written = t.transform(is, out, convertible);
                    } finally {
                        if (written) {
                            IOUtils.close(is, out);
                        }
                    }

                    // Retrieve the new CompositeInput
                    if (written) {
                        is = new InMemoryInput(out, is.getCharset());
                    }
                } else {
                    try {
                        // Last transformation
                        written = t.transform(is, os, convertible);
                    } finally {
                        if (written) {
                            IOUtils.close(is, os);
                        }
                    }
                }

                final long elapsed = timer.end();
                Logging.TIMER.log("Transformer {} executed in {}ms", t.getClass().getName(), elapsed);

                // Report the stat
                report(written, is, elapsed, t);
            }

            final Execution e;

            if (!written) {
                if (is == null) {
                    is = inputStream;
                }

                try {
                    e = is.execution();
                } finally {
                    IOUtils.close(is, os);
                }
            } else {
                e = os.execution();
            }

            // Notify callbacks
            NutUtils.invokeCallbacks(e, onReady);
        }
    }

    /**
     * <p>
     * Reports a new transformation statistic according to the given parameters.
     * </p>
     *
     * @param written if something has been actually transformed
     * @param is the input source of the transformer
     * @param elapsed the time spent by the transformer
     * @param t teh transformer itself
     */
    private void report(final boolean written, final Input is, final long elapsed, final Transformer t) {
        final String transformerClass;

        // Ignore EngineRequestTransformer which wrap the really interesting transformer
        if (EngineRequestTransformer.class.isAssignableFrom(t.getClass())) {
            transformerClass = EngineRequestTransformer.class.cast(t).getRequireEngineRequestTransformer().getClass().getName();
        } else {
            transformerClass = t.getClass().getName();
        }

        List<TransformationStat> transformationStats = statistics.get(transformerClass);

        // New transformer
        if (transformationStats == null) {
            transformationStats = new LinkedList<TransformationStat>();
            statistics.put(transformerClass, transformationStats);
        }

        transformationStats.add(new TransformationStat(written, is, elapsed, convertible.toString()));
    }

    /**
     * <p>
     * Executes the transformation for the given {@link com.github.wuic.nut.CompositeNut.CompositeInput}.
     * The method isolate the transformation of each nut inside the composition and then returns a {@code Pipe} with
     * the remaining transformers that should be applied on the aggregated stream.
     * </p>
     *
     * @param nut the {@link ConvertibleNut}
     * @param cis the opened {@link com.github.wuic.nut.CompositeNut.CompositeInput}
     * @return the pipe with remaining transformations
     * @throws IOException if transformation fails
     */
    private Pipe<ConvertibleNut> transform(final ConvertibleNut nut, final CompositeNut.CompositeInput cis)
            throws IOException {
        final Pipe<ConvertibleNut> finalPipe;
        final boolean hasTransformers = (convertible.getTransformers() != null && !convertible.getTransformers().isEmpty())
                || hasTransformers(cis.getCompositeNut().getCompositionList());

        // Collect transformer executed for each nut and group transformers for aggregated content
        if (hasTransformers) {
            // Get transformers
            final Set<Transformer<ConvertibleNut>> aggregatedStream = new TreeSet<Transformer<ConvertibleNut>>(new TransformerComparator());
            final List<PerNutTransformation> nuts = new ArrayList<PerNutTransformation>();
            final CompositeNut composite = cis.getCompositeNut();
            populateTransformers(convertible.getTransformers(), aggregatedStream, nuts, composite.getCompositionList());

            // First: transform each nut
            final CompositeNut.CompositeInput is =
                    composite.openStream(transformBeforeAggregate(nuts, composite.getCompositionList(), nut));

            // Aggregate the results
            finalPipe = new Pipe<ConvertibleNut>(nut, is, timerTreeFactory);

            // Transform the aggregated results
            for (final Pipe.Transformer<ConvertibleNut> transformer : aggregatedStream) {
                finalPipe.register(transformer);
            }
        } else {
            finalPipe = new Pipe<ConvertibleNut>(nut, nut.openStream(), timerTreeFactory);
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
     * Returns the generated transformation statistics.
     * </p>
     *
     * @return the stats
     */
    public Map<String, List<TransformationStat>> getStatistics() {
        return statistics;
    }

    /**
     * <p>
     * Transforms each nut with transformers producing content which could be aggregated.
     * The transformation result of each nut is returned as a list of {@link Input} that should be used to create
     * a {@link com.github.wuic.nut.CompositeNut.CompositeInput}.
     * </p>
     *
     * @param nuts the nuts to transform with their transformers
     * @param compositionList the composition list
     * @param convertibleNut the nut representing the composition
     * @return the input streams of corresponding to the composition where transformed content is accessible
     * @throws IOException if transformation fails
     */
    private List<Input> transformBeforeAggregate(final List<PerNutTransformation> nuts,
                                                 final List<ConvertibleNut> compositionList,
                                                 final ConvertibleNut convertibleNut)
            throws IOException {
        final List<Input> is = new ArrayList<Input>(compositionList.size());

        for (final PerNutTransformation perNutTransformation : nuts) {
            final Output bos = new InMemoryOutput(inputStream.getCharset());

            if (!perNutTransformation.getTransformers().isEmpty()) {

                // We pass this composition in order to receive any referenced nut or whatever state change
                final Pipe<ConvertibleNut> pipe = new Pipe<ConvertibleNut>(new NutWrapper(convertibleNut) {

                    /**
                     * {@inheritDoc}
                     */
                    @Override
                    public String getName() {
                        return perNutTransformation.getConvertibleNut().getName();
                    }

                    /**
                     * {@inheritDoc}
                     */
                    @Override
                    public String getInitialName() {
                        return perNutTransformation.getConvertibleNut().getInitialName();
                    }

                    /**
                     * {@inheritDoc}
                     */
                    @Override
                    public void setSource(final Source source) {
                        perNutTransformation.getConvertibleNut().setSource(source);
                    }
                }, perNutTransformation.getConvertibleNut().openStream(), timerTreeFactory);

                for (final Pipe.Transformer<ConvertibleNut> transformer : perNutTransformation.getTransformers()) {
                    pipe.register(transformer);
                }

                Pipe.executeAndWriteTo(pipe, perNutTransformation.getConvertibleNut().getReadyCallbacks(), bos);
                CollectionUtils.merge(pipe.statistics, statistics);
                is.add(bos.input(perNutTransformation.getConvertibleNut().getNutType().getCharset()));
            } else {
                is.add(perNutTransformation.getConvertibleNut().openStream());
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
                                      final List<PerNutTransformation> nuts,
                                      final Collection<ConvertibleNut> compositionList) {
        for (final ConvertibleNut nut : compositionList) {
            final Set<Pipe.Transformer<ConvertibleNut>> separateStream = new TreeSet<Transformer<ConvertibleNut>>(new TransformerComparator());
            populateTransformers(separateStream, aggregatedStream, nut.getTransformers());
            populateTransformers(separateStream, aggregatedStream, commonTransformers);
            nuts.add(new PerNutTransformation(separateStream, nut));
        }
    }

    /**
     * <p>
     * Populate the given maps with the specified transformers as explained in
     * {@link #populateTransformers(java.util.Set, java.util.Set, java.util.List, java.util.Collection)}.
     * </p>
     *
     * @param separateStream the transformers to apply for each nut
     * @param aggregatedStream the transformers to apply to the aggregated stream
     * @param transformers the transformers to populate the map
     * @see #populateTransformers(java.util.Set, java.util.Set, java.util.List, java.util.Collection)
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
