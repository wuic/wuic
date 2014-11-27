/*
 * "Copyright (c) 2014   Capgemini Technology Services (hereinafter "Capgemini")
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

import com.github.wuic.exception.NutNotFoundException;
import com.github.wuic.util.CollectionUtils;
import com.github.wuic.util.NumberUtils;
import com.github.wuic.util.NutUtils;
import com.github.wuic.util.Pipe;
import com.github.wuic.util.WuicScheduledThreadPool;

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
import java.util.LinkedHashSet;
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
     * <p>
     * Builds a new instance.
     * </p>
     *
     * @param composition the nuts of this composition
     * @param separator the bytes between each stream, {@code null} if nothing
     * @param specificName the name of the composition
     */
    public CompositeNut(final String specificName, final byte[] separator, final ConvertibleNut ... composition) {
        super(composition[0]);

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
        setVersionNumber(WuicScheduledThreadPool.getInstance().executeAsap(new Callable<Long>() {

            /**
             * {@inheritDoc}
             */
            @Override
            public Long call() throws Exception {
                return NutUtils.getVersionNumber(Arrays.asList(composition));
            }
        }));

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

            // Collect transformer executed for each nut and group transformers for aggregated content
            if (getTransformers() != null) {
                final List<Pipe.Transformer<ConvertibleNut>> separateStream = new ArrayList<Pipe.Transformer<ConvertibleNut>>();
                final Set<Pipe.Transformer<ConvertibleNut>> aggregatedStream = new LinkedHashSet<Pipe.Transformer<ConvertibleNut>>();

                for (final Pipe.Transformer<ConvertibleNut> transformer : getTransformers()) {
                    if (transformer.canAggregateTransformedStream()) {
                        separateStream.add(transformer);
                    } else {
                        aggregatedStream.add(transformer);
                    }
                }

                final List<InputStream> is =
                        new ArrayList<InputStream>(compositionList.size() * (streamSeparator == null ? 1 : NumberUtils.TWO));

                // First we transform each nut with transformers producing content which could be aggregated
                for (final ConvertibleNut nut : compositionList) {
                    final ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    final Pipe<ConvertibleNut> pipe = new Pipe<ConvertibleNut>(new NutWrapper(this) {

                        /**
                         * {@inheritDoc}
                         */
                        @Override
                        public String getName() {
                            return nut.getInitialName();
                        }
                    }, nut.openStream());

                    for (final Pipe.Transformer<ConvertibleNut> transformer :separateStream) {
                        pipe.register(transformer);
                    }

                    Pipe.executeAndWriteTo(pipe, nut.getReadyCallbacks(), bos);

                    is.add(new ByteArrayInputStream(bos.toByteArray()));

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
        } catch (NutNotFoundException nnfe) {
            throw new IOException(nnfe);
        } finally {
            setTransformed(true);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public InputStream openStream() throws NutNotFoundException {
        final List<InputStream> is = new ArrayList<InputStream>(compositionList.size() * (streamSeparator == null ? 1 : NumberUtils.TWO));

        for (final Nut n : compositionList) {
            is.add(n.openStream());

            if (streamSeparator != null) {
                is.add(new ByteArrayInputStream(streamSeparator));
            }
        }

        return new SequenceInputStream(Collections.enumeration(is));
    }

    /**
     * <p>
     * This class combines different sets of nuts as specified by {@link CompositeNut#mergeNuts(java.util.List)}.
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
         * @return the merged nuts
         * @see CompositeNut#mergeNuts(java.util.List)
         */
        public List<ConvertibleNut> mergeNuts(final List<ConvertibleNut> nuts) {
            final List<ConvertibleNut> retval = new ArrayList<ConvertibleNut>(nuts.size());
            int start = 0;
            int end = 1;
            String current = null;

            for (final Iterator<ConvertibleNut> it = nuts.iterator();;) {
                final ConvertibleNut nut = it.hasNext() ? it.next() : null;

                // New sequence
                if (nut != null && current == null) {
                    current = nut.getName();
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

                    final CompositeNut compositeNut = new CompositeNut(compositionName, null, composition);
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
     * @param nuts the nuts to merge
     * @return a list containing all the nuts with some compositions
     */
    public static List<ConvertibleNut> mergeNuts(final List<ConvertibleNut> nuts) {
        return new Combiner().mergeNuts(nuts);
    }
}
