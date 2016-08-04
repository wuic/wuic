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


package com.github.wuic.nut;

import com.github.wuic.ProcessContext;
import com.github.wuic.exception.WuicException;
import com.github.wuic.util.AbstractInput;
import com.github.wuic.util.DefaultInput;
import com.github.wuic.util.FutureLong;
import com.github.wuic.util.IOUtils;
import com.github.wuic.util.Input;
import com.github.wuic.util.NumberUtils;
import com.github.wuic.util.NutUtils;
import com.github.wuic.util.Pipe;
import com.github.wuic.util.SequenceReader;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.SequenceInputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

/**
 * <p>
 * A composite {@link Nut} is able to chain all the streams provided by each element of the composition.
 * We consider that all the {@link Nut nuts} of the composition should have the same state (name, etc).
 * </p>
 *
 * @author Guillaume DROUET
 * @since 0.4.2
 */
public class CompositeNut extends PipedConvertibleNut {

    /**
     * The bytes between each stream.
     */
    private byte[] streamSeparator;

    /**
     * The composition list.
     */
    private List<ConvertibleNut> compositionList;

    /**
     * Computes version number asynchronously or not.
     */
    private Boolean asynchronousVersionNumber;

    /**
     * The charset.
     */
    private final String charset;

    /**
     * <p>
     * Builds a new instance.
     * </p>
     *
     * @param cs the charset
     * @param avn indicates if version computation is done asynchronously or not
     * @param composition the nuts of this composition
     * @param separator the bytes between each stream, {@code null} if nothing
     * @param specificName the name of the composition
     * @param processContext the process context
     */
    public CompositeNut(final String cs,
                        final Boolean avn,
                        final String specificName,
                        final byte[] separator,
                        final ProcessContext processContext,
                        final ConvertibleNut ... composition) {
        super(composition[0]);

        charset = cs;
        asynchronousVersionNumber = avn;

        if (separator != null) {
            streamSeparator = Arrays.copyOf(separator, separator.length);
        }

        // Populate composition
        compositionList = new ArrayList<ConvertibleNut>();

        for (final ConvertibleNut nut : composition) {
            addToComposition(nut);
        }

        // Make a composite version number
        if (asynchronousVersionNumber) {
            setVersionNumber(processContext.executeAsap(new Callable<Long>() {

                /**
                 * {@inheritDoc}
                 */
                @Override
                public Long call() {
                    return NutUtils.getVersionNumber(Arrays.asList(composition));
                }
            }));
        } else {
            setVersionNumber(new FutureLong(NutUtils.getVersionNumber(Arrays.asList(composition))));
        }

        // Eventually add each referenced nut in the composition (excluding first element taken in consideration by super constructor)
        for (int i = 1; i < composition.length; i++) {
            final ConvertibleNut nut = composition[i];

            if (nut.getReferencedNuts() != null) {
                for (final ConvertibleNut ref : nut.getReferencedNuts()) {
                    addReferencedNut(ref);
                }
            }
        }

        setNutName(specificName);
    }

    /**
     * <p>
     * Adds the given nut to the composition and checks if it's not already a composition.
     * </p>
     *
     * @param nut the nut to add to the composition
     */
    private void addToComposition(final ConvertibleNut nut) {
        // Merge the nested composition
        if (CompositeNut.class.isAssignableFrom(nut.getClass())) {
            compositionList.addAll(CompositeNut.class.cast(nut).compositionList);
        } else {
            Input is = null;

            try {
                // Try to detect a composition through the InputStream
                is = nut.openStream();

                if (CompositeInput.class.isAssignableFrom(is.getClass())) {
                    compositionList.addAll(CompositeInput.class.cast(is).getCompositeNut().getCompositionList());
                } else {
                    // Does not seems to be a composition
                    compositionList.add(nut);

                    // Avoid transformers from composition[0]
                    if (getTransformers() != null) {
                        getTransformers().clear();
                    }
                }

            } catch (IOException ioe) {
                WuicException.throwBadStateException(new IllegalStateException("Unable to open a stream", ioe));
            } finally {
                IOUtils.close(is);
            }
        }
    }

    /**
     * <p>
     * Returns the composition list.
     * </p>
     *
     * @return the composition list
     */
    public List<ConvertibleNut> getCompositionList() {
        return compositionList;
    }

    /**
     * <p>
     * Indicates if version number is computed asynchronously.
     * </p>
     *
     * @return {@code true} if asynchronous operation is performed, {@code false} otherwise
     */
    public Boolean getAsynchronousVersionNumber() {
        return asynchronousVersionNumber;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isDynamic() {
        return compositionList.get(0).isDynamic();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addTransformer(final Pipe.Transformer<ConvertibleNut> transformer) {
        compositionList.get(0).addTransformer(transformer);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setSource(final Source src) {
        compositionList.get(0).setSource(src);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Source getSource() {
        return compositionList.get(0).getSource();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Input openStream() throws IOException {
        return openStream(null);
    }

    /**
     * <p>
     * Returns a {@link CompositeInput} that uses the given streams for each nut inside the composition.
     * </p>
     *
     * @param inputStreams the streams, {@code null} if the streams should be created from the nuts inside the composition
     * @return the new {@link CompositeInput}
     * @throws IOException if any I/O error occurs
     */
    public CompositeInput openStream(final List<Input> inputStreams) throws IOException {
        return new CompositeInput(inputStreams);
    }

    /**
     * <p>
     * This class use the position of each nut in the read content to update the source map.
     * When this stream has been entirely read, it's possible to use the source map to retrieve the nut containing the
     * char at a specified position.
     * </p>
     *
     * @author Guillaume DROUET
     * @since 0.5.0
     */
    public final class CompositeInput extends AbstractInput {

        /**
         * <p>
         * This class gives information about the starting position of a particular nut inside the {@link CompositeInput}.
         * </p>
         *
         * @author Guillaume DROUET
         * @since 0.5.3
         */
        public final class Position {

            /**
             * The line position.
             */
            private int line;

            /**
             * The column position.
             */
            private int column;

            /**
             * The absolute index.
             */
            private long index;

            /**
             * <p>
             * Builds a new instance.
             * </p>
             *
             * @param line the line
             * @param column the column
             * @param index the index
             */
            public Position(final int line, final int column, final long index) {
                this.line = line;
                this.column = column;
                this.index = index;
            }

            /**
             * <p>
             * Returns the start position of the content which has a end position corresponding to this instance.
             * </p>
             *
             * @return the start position
             */
            public Position startPosition() {
                for (int i = 0; i < positions.length; i++) {
                    if (positions[i] == this) {
                        if (i == 0) {
                            return new Position(0, 0, 0);
                        } else {
                            return positions[i - 1];
                        }
                    }
                }

                return null;
            }

            /**
             * <p>
             * Gets the line.
             * </p>
             *
             * @return the line
             */
            public int getLine() {
                return line;
            }

            /**
             * <p>
             * Gets the index.
             * </p>
             *
             * @return the index
             */
            public long getIndex() {
                return index;
            }

            /**
             * <p>
             * Gets the column.
             * </p>
             *
             * @return the column
             */
            public int getColumn() {
                return column;
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public String toString() {
                return String.format("{index: %s, line: %s, column: %s}", index, line, column);
            }
        }

        /**
         * The positions of all nut in the stream.
         */
        private final Position[] positions;

        /**
         * All the inputs.
         */
        private final List<Input> inputs;

        /**
         * The total length.
         */
        private int length;

        /**
         * Number of detected lines.
         */
        private int lines;

        /**
         * Number of columns on the last line.
         */
        private int columns;

        /**
         * The nut's index currently read.
         */
        private int currentIndex;

        /**
         * <p>
         * Builds a new instance. If the given parameter is {@code null}, the {@link com.github.wuic.nut.Nut#openStream()}
         * method is called for each nut of the composition. Otherwise the given list is used. In that case, the composition
         * list and the given parameter must have the same size.
         * </p>
         *
         * @param streams the stream for each {@link Nut} of the composition, {@code null} if nut's input stream should be used
         * @throws IOException if a stream could not be opened
         */
        private CompositeInput(final List<Input> streams) throws IOException {
            super(charset);
            inputs = new ArrayList<Input>(compositionList.size() * NumberUtils.TWO);

            // Use nut streams
            if (streams == null) {
                for (final Nut n : compositionList) {
                    addStream(inputs, n.openStream());
                }
            } else {
                // Check assertion
                if (streams.size() != compositionList.size()) {
                    WuicException.throwBadArgumentException(new IllegalArgumentException(
                            "A specific list of streams can be specified it has the same size as the composition list."));
                }

                // Use specific streams
                for (final Input inputStream : streams) {
                    addStream(inputs, inputStream);
                }
            }

            positions = new Position[compositionList.size()];
        }

        /**
         * <p>
         * Adds the given {@link Input} to the list specified in parameter. A second {@link Input} is used
         * to mark the separation between all the objects added to the list.
         * </p>
         *
         * @param targetList the list to populated
         * @param is the stream
         */
        private void addStream(final List<Input> targetList, final Input is) {
            targetList.add(is);

            if (streamSeparator != null) {
                // Keep the separation position when stream is closed
                targetList.add(new DefaultInput(new ByteArrayInputStream(streamSeparator) {
                    @Override
                    public void close() throws IOException {
                        positions[currentIndex++] = new Position(lines, columns, length);
                    }
                }, charset));
            } else {
                // No separator stream, just add a marker
                targetList.add(new DefaultInput(new InputStream() {
                    @Override
                    public int read() throws IOException {
                        positions[currentIndex++] = new Position(lines, columns, length);
                        return -1;
                    }
                }, getNutType().getCharset()));
            }
        }

        /**
         * <p>
         * Gets the enclosing class.
         * </p>
         *
         * @return the composition
         */
        public CompositeNut getCompositeNut() {
            return CompositeNut.this;
        }

        /**
         * <p>
         * Gets the position of the given {@link ConvertibleNut} inside the composite stream.
         * </p>
         *
         * @param convertibleNut the nut
         * @return the {@link Position} object, {@code null} if the nut is not part of the composition
         */
        public Position position(final ConvertibleNut convertibleNut) {
            if (!getNutType().isText()) {
                throw new UnsupportedOperationException("Can't call position(ConvertibleNut) in a binary content.");
            }

            final int nutPos = compositionList.indexOf(convertibleNut);

            if (nutPos != -1) {
                return positions[nutPos];
            }

            return null;
        }

        /**
         * <p>
         * Retrieves the nut owning the char at the given position. This method should be used only when
         * {@link #getNutType()} returns a {@link com.github.wuic.NutType} whose {@link com.github.wuic.NutType#isText()}
         * method returns {@code true} because positions are based on char count.
         * </p>
         *
         * @param position the position
         * @return the nut, {@code null} if the given position is not in the interval [0, length - 1]
         */
        public ConvertibleNut nutAt(final int position) {
            if (!getNutType().isText()) {
                throw new UnsupportedOperationException("Can't call nutAt(int) in a binary content.");
            }

            for (int i = 0; i < positions.length; i++) {
                if (position < positions[i].getIndex()) {
                    return compositionList.get(i);
                }
            }

            return null;
        }

        /**
         * <p>
         * Reads the portion of the given char array and increment the {@link #lines}, {@link #columns} and {@link #length}.
         * </p>
         *
         * @param cbuf the char array to read
         * @param off the index where start reading
         * @param len the number of characters to read
         */
        private void updateCoordinates(final char[] cbuf, final int off, final int len) {
            for (int i = off; i < len; i++) {
                length++;
                columns++;

                if (cbuf[i] == '\n') {
                    lines++;
                    columns = 0;
                }
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return String.format("separatorPositions: %s", Arrays.toString(positions));
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isSourceAsByte() {
            return inputs.get(0).isSourceAsByte();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Pipe.Execution execution() throws IOException {
            if (isClosed()) {
                WuicException.throwBadStateException(new IllegalStateException("Input is closed."));
            }

            try {
                final List<Pipe.Execution> executions = new ArrayList<Pipe.Execution>(inputs.size());
                final Charset cs = Charset.forName(charset);

                for (final Input input : inputs) {
                    final Pipe.Execution e = input.execution();
                    final char[] chars;

                    if (e.isText()) {
                        chars = e.getCharResult();
                    } else {
                        chars = IOUtils.toChars(cs, e.getByteResult());
                    }

                    // We update the positions
                    updateCoordinates(chars, 0, chars.length);

                    executions.add(e);
                }

                return new Pipe.Execution(executions);
            } finally {
                close();
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected InputStream internalInputStream(final String charset) throws IOException {
            final List<InputStream> is = new ArrayList<InputStream>(inputs.size());

            for (final Input i : inputs) {
                is.add(i.inputStream());
            }

            final InputStream seq = new SequenceInputStream(Collections.enumeration(is));
            return !getNutType().isText() ? new InternalInputStream(seq) : new InternalInputStream(new InternalReader(new InputStreamReader(seq, charset)));
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected Reader internalReader(final String charset) throws IOException {
            final List<Reader> readers = new ArrayList<Reader>(inputs.size());

            for (final Input i : inputs) {
                readers.add(i.reader());
            }

            return new InternalReader(new SequenceReader(readers));
        }

        /**
         * <p>
         * An {@code InputStream} wrapper that tracks data positions.
         * </p>
         *
         * @author Guillaume DROUET
         * @since 0.5.3
         */
        private class InternalInputStream extends InputStream {

            /**
             * The input stream.
             */
            private InputStream sequence;

            /**
             * The stream reader.
             */
            private Reader sequenceReader;

            /**
             * Buffer used to read chars.
             */
            private byte[] buffer;

            /**
             * Next byte to read from the buffer.
             */
            private int bufferOffset;

            /**
             * Charset object used for char encoding.
             */
            private Charset charsetInstance;

            /**
             * <p>
             * Builds a new instance based on a stream.
             * </p>
             *
             * @param inputStream the input stream
             */
            private InternalInputStream(final InputStream inputStream) {
                this.sequence = inputStream;
            }

            /**
             * <p>
             * Builds a new instance based on a reader.
             * </p>
             *
             * @param reader the reader
             */
            private InternalInputStream(final Reader reader) {
                this.sequenceReader = reader;
                this.charsetInstance = Charset.forName(charset);
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public int read() throws IOException {
                if (sequence == null) {
                    int c;

                    // Read the next sequence of data
                    if (buffer == null) {
                        c = sequenceReader.read();

                        // Nothing to read anymore
                        if (c == -1) {
                            return -1;
                        }

                        buffer = IOUtils.toBytes(charsetInstance, (char) c);
                        bufferOffset = 0;
                    }

                    // Read next byte from the buffer
                    c = buffer[bufferOffset++];

                    // End of buffer reached
                    if (bufferOffset == buffer.length) {
                        buffer = null;
                    }

                    return c;
                } else {
                    return sequence.read();
                }
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public void close() throws IOException {
                IOUtils.close(sequence, sequenceReader);
            }
        }

        /**
         * <p>
         * An {@code Reader} wrapper that tracks data positions.
         * </p>
         *
         * @author Guillaume DROUET
         * @since 0.5.3
         */
        private class InternalReader extends Reader {

            /**
             * The reader.
             */
            private final Reader sequence;

            /**
             * <p>
             * Builds a new instance.
             * </p>
             *
             * @param reader the reader
             */
            private InternalReader(final Reader reader) {
                this.sequence = reader;
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public void close() throws IOException {
                sequence.close();
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public int read(final char[] cbuf, final int off, final int len) throws IOException {
                final int retval = sequence.read(cbuf, off, len);

                if (retval != -1) {
                    updateCoordinates(cbuf, off, retval);
                }

                return retval;
            }
        }
    }

    /**
     * <p>
     * This class combines different sets of nuts as specified by {@link CompositeNut#mergeNuts(ProcessContext, List, String)}
     * It ensures that every names will be unique in returned lists during the entire lifecycle of its instance.
     * </p>
     *
     * @author Guillaume DROUET
     * @since 0.4.4
     */
    public static class Combiner {

        /**
         * The initial count of prefixes.
         */
        private int prefixCount;

        /**
         * The name already used.
         */
        private Set<String> names;

        /**
         * <p>
         * Builds a new instance.
         * </p>
         */
        public Combiner() {
            prefixCount = 0;
            names = new HashSet<String>();
        }

        /**
         * <p>
         * Merges the given nuts.
         * </p>
         *
         * @param charset the charset
         * @param nuts the nuts to merge
         * @param processContext the process context
         * @return the merged nuts
         * @see CompositeNut#mergeNuts(com.github.wuic.ProcessContext, java.util.List, String)
         */
        public List<ConvertibleNut> mergeNuts(final ProcessContext processContext, final List<ConvertibleNut> nuts, final String charset) {
            final List<ConvertibleNut> retval = new ArrayList<ConvertibleNut>(nuts.size());
            int start = 0;
            int end = 1;
            String current = null;
            Boolean asynchronous = Boolean.FALSE;

            for (final Iterator<ConvertibleNut> it = nuts.iterator();;) {
                final ConvertibleNut nut = it.hasNext() ? it.next() : null;

                // New sequence
                if (nut != null && current == null) {
                    current = nut.getName();
                    asynchronous = (nut instanceof CompositeNut) && CompositeNut.class.cast(nut).getAsynchronousVersionNumber();
                    // Nut name is the same as previous nut name, will be included in same composition
                } else if (nut != null && current.equals(nut.getName())) {
                    end++;
                    // Nut name does not equals previous nut name, add previous composition
                } else {
                    final List<ConvertibleNut> subList = nuts.subList(start, end);
                    final ConvertibleNut[] composition = subList.toArray(new ConvertibleNut[subList.size()]);
                    final String name = composition[0].getName();
                    final String compositionName;

                    // Create the composition with a potential prefix to avoid duplicate
                    if (names.add(name)) {
                        compositionName = name;
                    } else {
                        compositionName = new StringBuilder(name).insert(name.lastIndexOf('/') + 1, prefixCount).toString();
                    }

                    final CompositeNut compositeNut = new CompositeNut(charset, asynchronous, compositionName, null, processContext, composition);
                    retval.add(compositeNut);

                    if (nut != null) {
                        // New sequence
                        current = nut.getName();
                        start = end;
                        prefixCount++;
                        end++;
                    } else {
                        // End of iterator
                        break;
                    }
                }
            }

            return retval;
        }
    }

    /**
     * <p>
     * Merges all sequences {@link Nut nuts} with the same name into one {@link CompositeNut}. When a stream is read
     * in a composite object, a composite {@link InputStream} is returned and will read each stream in the order of the
     * list. When a composition is created, its name is prefixed with the its position in the given list.
     * </p>
     *
     * <p>
     * For instance, ['foo.js', 'foo.js', 'bar.css', 'baz.js', 'foo.js'] => ['0foo.js', 'bar.css', 'baz.js', 'foo.js']
     * where the first element is a composition and where "prefixCountStart" equals to 0.
     * </p>
     *
     * @param charset the charset
     * @param processContext the process context
     * @param nuts the nuts to merge
     * @return a list containing all the nuts with some compositions
     */
    public static List<ConvertibleNut> mergeNuts(final ProcessContext processContext, final List<ConvertibleNut> nuts, final String charset) {
        return new Combiner().mergeNuts(processContext, nuts, charset);
    }
}
