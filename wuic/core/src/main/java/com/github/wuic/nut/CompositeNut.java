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


package com.github.wuic.nut;

import com.github.wuic.ProcessContext;
import com.github.wuic.util.CollectionUtils;
import com.github.wuic.util.FutureLong;
import com.github.wuic.util.NumberUtils;
import com.github.wuic.util.NutUtils;
import com.github.wuic.util.Pipe;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

/**
 * <p>
 * A composite {@link Nut} is able to chain all the stream provided by each element of the composition. We consider that
 * all the {@link Nut nuts} of the composition should have the same state (name, etc).
 * </p>
 *
 * @author Guillaume DROUET
 * @version 1.2
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

        compositionList = new ArrayList<ConvertibleNut>();

        for (final ConvertibleNut nut : composition) {
            if (CompositeNut.class.isAssignableFrom(nut.getClass())) {
                compositionList.addAll(CompositeNut.class.cast(nut).compositionList);
            } else {
                compositionList.add(nut);
            }
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
     * Indicates if at least one nut of the composition has a transformer.
     * </p>
     *
     * @return {@code true} is a transformer exists inside the composition, {@code false} otherwise
     */
    public boolean hasTransformers() {
        for (final ConvertibleNut nut : compositionList) {
            if (nut.getTransformers() != null) {
                return true;
            }
        }

        return false;
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
    public void transform(final Pipe.OnReady... onReady) throws IOException {
        if (isTransformed()) {
            throw new IllegalStateException("Could not call transform(java.io.OutputStream) method twice.");
        }

        try {
            final List<Pipe.OnReady> merge = CollectionUtils.newList(onReady);

            if (getReadyCallbacks() != null) {
                merge.addAll(getReadyCallbacks());
            }

            final Pipe<ConvertibleNut> finalPipe;
            final boolean hasTransformers = hasTransformers();

            // Collect transformer executed for each nut and group transformers for aggregated content
            if (hasTransformers) {
                // Get transformers
                final Set<Pipe.Transformer<ConvertibleNut>> aggregatedStream = new LinkedHashSet<Pipe.Transformer<ConvertibleNut>>();
                final Map<ConvertibleNut, List<Pipe.Transformer<ConvertibleNut>>> nuts = new LinkedHashMap<ConvertibleNut, List<Pipe.Transformer<ConvertibleNut>>>();
                populateTransformers(aggregatedStream, nuts);

                final List<InputStream> is = new ArrayList<InputStream>(compositionList.size() * (streamSeparator == null ? 1 : NumberUtils.TWO));

                // First we transform each nut with transformers producing content which could be aggregated
                for (final Map.Entry<ConvertibleNut, List<Pipe.Transformer<ConvertibleNut>>> entry : nuts.entrySet()) {
                    final ByteArrayOutputStream bos = new ByteArrayOutputStream();

                    if (!entry.getValue().isEmpty()) {

                        // We pass this composition in order to receive any referenced nut or whatever state change
                        final Pipe<ConvertibleNut> pipe = new Pipe<ConvertibleNut>(new NutWrapper(this) {

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

                    if (streamSeparator != null) {
                        is.add(new ByteArrayInputStream(streamSeparator));
                    }
                }

                // Aggregate the results
                finalPipe = new Pipe<ConvertibleNut>(this, new SequenceInputStream(Collections.enumeration(is)));

                // Transform the aggregated results
                for (final Pipe.Transformer<ConvertibleNut> transformer : aggregatedStream) {
                    finalPipe.register(transformer);
                }
            } else {
                finalPipe = new Pipe<ConvertibleNut>(this, openStream());
            }

            finalPipe.execute(merge.toArray(new Pipe.OnReady[merge.size()]));
        } finally {
            setTransformed(true);
        }
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
    public InputStream openStream() throws IOException {
        return new CompositeInputStream();
    }

    /**
     * <p>
     * Populates the given {@code Set} and {@code Map} with the transformers inside the composition.
     * If the {@link com.github.wuic.util.Pipe.Transformer#canAggregateTransformedStream()} returns {@code true}, then
     * it's added to a list associated to the nut with the given map. Otherwise the transformer will be added in the
     * set specified in parameter.
     * </p>
     *
     * @param aggregatedStream transformer that operate on aggregated streams
     * @param nuts all nuts with their transformer that produce a stream that can be aggregated later
     */
    private void populateTransformers(final Set<Pipe.Transformer<ConvertibleNut>> aggregatedStream,
                                      final Map<ConvertibleNut, List<Pipe.Transformer<ConvertibleNut>>> nuts) {
        for (final ConvertibleNut nut : compositionList) {
            final List<Pipe.Transformer<ConvertibleNut>> separateStream = new ArrayList<Pipe.Transformer<ConvertibleNut>>();

            if (nut.getTransformers() != null) {
                for (final Pipe.Transformer<ConvertibleNut> transformer : nut.getTransformers()) {
                    if (transformer.canAggregateTransformedStream()) {
                        separateStream.add(transformer);
                    } else {
                        aggregatedStream.add(transformer);
                    }
                }
            }

            nuts.put(nut, separateStream);
        }
    }

    /**
     * <p>
     * This class keeps the position of each nut in the read content. When this stream has been entirely read,
     * it's possible to retrieve the nut containing the byte at a specified position.
     * </p>
     *
     * @author Guillaume DROUET
     * @version 1.0
     * @since 0.5.0
     */
    public final class CompositeInputStream extends InputStream {

        /**
         * The sequence containing each steam of this composition and their separator.
         */
        private InputStream sequence;

        /**
         * The position of each separator between streams.
         */
        private long[] separatorPositions;

        /**
         * The total length.
         */
        private long length;

        /**
         * The nut's index currently read.
         */
        private int currentIndex;

        /**
         * <p>
         * Builds a new instance.
         * </p>
         *
         * @throws IOException if a stream could not be opened
         */
        private CompositeInputStream() throws IOException {
            final List<InputStream> is = new ArrayList<InputStream>(compositionList.size() * NumberUtils.TWO);
            separatorPositions = new long[compositionList.size()];

            for (final Nut n : compositionList) {
                is.add(n.openStream());

                if (streamSeparator != null) {
                    // Keep the separation position when stream is closed
                    is.add(new ByteArrayInputStream(streamSeparator) {
                        @Override
                        public void close() throws IOException {
                            separatorPositions[currentIndex++] = length;
                        }
                    });
                } else {
                    // No separator stream, just add a marker
                    is.add(new InputStream() {
                        @Override
                        public int read() throws IOException {
                            separatorPositions[currentIndex] = length;
                            return -1;
                        }
                    });
                }
            }

            sequence = new SequenceInputStream(Collections.enumeration(is));
        }

        /**
         * <p>
         * Gets the outer composition list.
         * </p>
         *
         * @return the composition list
         */
        public List<ConvertibleNut> getComposition() {
            return CompositeNut.this.compositionList;
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
            for (int i = 0; i < separatorPositions.length; i++) {
                if (position < separatorPositions[i]) {
                    return compositionList.get(i);
                }
            }

            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int read() throws IOException {
            final int retval = sequence.read();

            if (retval != -1) {
                length++;
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
     * @version 1.0
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
