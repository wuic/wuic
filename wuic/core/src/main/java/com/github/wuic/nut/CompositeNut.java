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
import com.github.wuic.util.FutureLong;
import com.github.wuic.util.IOUtils;
import com.github.wuic.util.NumberUtils;
import com.github.wuic.util.NutUtils;
import com.github.wuic.util.Pipe;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.SequenceInputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
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
 * A composite {@link Nut} is able to chain all the stream provided by each element of the composition. We consider that
 * all the {@link Nut nuts} of the composition should have the same state (name, etc).
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
     * <p>
     * Builds a new instance.
     * </p>
     *
     * @param avn indicates if version computation is done asynchronously or not
     * @param composition the nuts of this composition
     * @param separator the bytes between each stream, {@code null} if nothing
     * @param specificName the name of the composition
     * @param processContext the process context
     */
    public CompositeNut(final Boolean avn,
                        final String specificName,
                        final byte[] separator,
                        final ProcessContext processContext,
                        final ConvertibleNut ... composition) {
        super(composition[0]);

        asynchronousVersionNumber = avn;

        if (separator != null) {
            streamSeparator = new byte[separator.length];
            System.arraycopy(separator, 0, streamSeparator, 0, streamSeparator.length);
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
            InputStream is = null;

            try {
                // Try to detect a composition through the InputStream
                is = nut.openStream();

                if (CompositeInputStream.class.isAssignableFrom(is.getClass())) {
                    compositionList.addAll(CompositeInputStream.class.cast(is).getCompositeNut().getCompositionList());
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
    public InputStream openStream() throws IOException {
        return openStream(null);
    }

    /**
     * <p>
     * Returns a {@link CompositeInputStream} that uses the given streams for each nut inside the composition.
     * </p>
     *
     * @param inputStreams the streams, {@code null} if the streams should be created from the nuts inside the composition
     * @return the new {@link CompositeInputStream}
     * @throws IOException if any I/O error occurs
     */
    public CompositeInputStream openStream(final List<InputStream> inputStreams) throws IOException {
        return new CompositeInputStream(inputStreams);
    }

    /**
     * <p>
     * This class use the position of each nut in the read content to update the source map.
     * When this stream has been entirely read, it's possible to use the source map to retrieve the nut containing the
     * byte at a specified position.
     * </p>
     *
     * @author Guillaume DROUET
     * @since 0.5.0
     */
    public final class CompositeInputStream extends InputStream {

        /**
         * <p>
         * This class gives information about the starting position of a particular nut inside the {@link CompositeInputStream}.
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
         * The sequence containing each steam of this composition and their separator.
         */
        private InputStream sequence;

        /**
         * In case of text stream, the reader used to count read characters.
         */
        private InputStreamReader sequenceReader;

        /**
         * The byte array corresponding to the character currently read if the stream is text.
         */
        private byte[] charBuffer;

        /**
         * Current byte in the {@link #charBuffer}.
         */
        private int charOffset;

        /**
         * The total length.
         */
        private long length;

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
        private CompositeInputStream(final List<InputStream> streams) throws IOException {
            final List<InputStream> is = new ArrayList<InputStream>(compositionList.size() * NumberUtils.TWO);

            // Use nut streams
            if (streams == null) {
                for (final Nut n : compositionList) {
                    addStream(is, n.openStream());
                }
            } else {
                // Check assertion
                if (streams.size() != compositionList.size()) {
                    WuicException.throwBadArgumentException(new IllegalArgumentException(
                            "A specific list of streams can be specified it has the same size as the composition list."));
                }

                // Use specific streams
                for (final InputStream inputStream : streams) {
                    addStream(is, inputStream);
                }
            }

            positions = new Position[compositionList.size()];
            sequence = new SequenceInputStream(Collections.enumeration(is));

            if (CompositeNut.this.getNutType().isText()) {
                sequenceReader = new InputStreamReader(sequence);
            }
        }

        /**
         * <p>
         * Adds the given {@link InputStream} to the list specified in parameter. A second {@link InputStream} is used
         * to mark the separation between all the objects added to the list.
         * </p>
         *
         * @param targetList the list to populated
         * @param is the stream
         */
        private void addStream(final List<InputStream> targetList, final InputStream is) {
            targetList.add(is);

            if (streamSeparator != null) {
                // Keep the separation position when stream is closed
                targetList.add(new ByteArrayInputStream(streamSeparator) {
                    @Override
                    public void close() throws IOException {
                        positions[currentIndex++] = new Position(lines, columns, length);
                    }
                });
            } else {
                // No separator stream, just add a marker
                targetList.add(new InputStream() {
                    @Override
                    public int read() throws IOException {
                        positions[currentIndex++] = new Position(lines, columns, length);
                        return -1;
                    }
                });
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
            final int nutPos = compositionList.indexOf(convertibleNut);

            if (nutPos != -1) {
                return positions[nutPos];
            }

            return null;
        }

        /**
         * <p>
         * Retrieves the nut owning the byte at the given position/
         * </p>
         *
         * @param position the position
         * @return the nut, {@code null} if the given position is not in the interval [0, length - 1]
         */
        public ConvertibleNut nutAt(final int position) {
            for (int i = 0; i < positions.length; i++) {
                if (position < positions[i].getIndex()) {
                    return compositionList.get(i);
                }
            }

            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return String.format("separatorPositions: %s%sisText: %b", Arrays.toString(positions), IOUtils.NEW_LINE, sequenceReader != null);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int read() throws IOException {

            // Reading an array of bytes corresponding to a character
            if (charBuffer != null) {

                // End of character reached
                if (charOffset == charBuffer.length) {
                    charBuffer = null;
                    charOffset = 0;
                } else {
                    // Read the next byte
                    return charBuffer[charOffset++];
                }
            }

            // Read a byte or a character
            final int retval = sequenceReader != null ? sequenceReader.read() : sequence.read();

            // Count stream
            if (retval != -1) {
                length++;
                columns++;

                if (sequenceReader != null) {
                    final char[] chars = Character.toChars(retval);

                    // Count new lines only for character streams.
                    if (chars[0] == '\n') {
                        lines++;
                        columns = 0;
                    }

                    // Just read a character, convert it to a byte array
                    final CharBuffer cbuf = CharBuffer.wrap(chars);

                    // TODO: avoid default Charset
                    final ByteBuffer bbuf = Charset.forName("UTF-8").encode(cbuf);
                    charBuffer = bbuf.array();
                    return charBuffer[charOffset++];
                }
            }

            return retval;
        }
    }

    /**
     * <p>
     * This class combines different sets of nuts as specified by {@link CompositeNut#mergeNuts(ProcessContext, List)}
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
         * @param nuts the nuts to merge
         * @param processContext the process context
         * @return the merged nuts
         * @see CompositeNut#mergeNuts(com.github.wuic.ProcessContext, java.util.List)
         */
        public List<ConvertibleNut> mergeNuts(final ProcessContext processContext, final List<ConvertibleNut> nuts) {
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

                    final CompositeNut compositeNut = new CompositeNut(asynchronous, compositionName, null, processContext, composition);
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
     * @param processContext the process context
     * @param nuts the nuts to merge
     * @return a list containing all the nuts with some compositions
     */
    public static List<ConvertibleNut> mergeNuts(final ProcessContext processContext, final List<ConvertibleNut> nuts) {
        return new Combiner().mergeNuts(processContext, nuts);
    }
}
