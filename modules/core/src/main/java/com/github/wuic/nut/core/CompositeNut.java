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


package com.github.wuic.nut.core;

import com.github.wuic.exception.NutNotFoundException;
import com.github.wuic.nut.AbstractNut;
import com.github.wuic.nut.Nut;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

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
public class CompositeNut extends AbstractNut {

    /**
     * The composition list.
     */
    private Nut[] compositionList;

    /**
     * The nut name.
     */
    private String name;

    /**
     * The bytes between each stream.
     */
    private byte[] streamSeparator;

    /**
     * <p>
     * Builds a new instance.
     * </p>
     *
     * @param composition the nuts of this composition
     * @param separator the bytes between each stream, {@code null} if nothing
     * @param specificName the name of the composition
     */
    public CompositeNut(final String specificName, final byte[] separator, final Nut ... composition) {
        this(composition, "", separator);
        name = specificName;
    }

    /**
     * <p>
     * Builds a new instance.
     * </p>
     *
     * @param composition the nuts of this composition
     * @param prefixName a prefix to add to the nut name
     * @param separator the bytes between each stream, {@code null} if nothing
     */
    public CompositeNut(final Nut[] composition, final String prefixName, final byte[] separator) {
        super(composition[0]);

        if (separator != null) {
            streamSeparator = new byte[separator.length];
            System.arraycopy(separator, 0, streamSeparator, 0, streamSeparator.length);
        }

        compositionList = new Nut[composition.length];
        System.arraycopy(composition, 0, compositionList, 0, composition.length);
        setNutName(new StringBuilder(getName()).insert(getName().lastIndexOf('/') + 1, prefixName).toString());

        // Eventually add each referenced nut in the composition (excluding first element taken in consideration by super constructor)
        for (int i = 1; i < composition.length; i++) {
            final Nut nut = composition[i];

            if (nut.getReferencedNuts() != null) {
                for (final Nut ref : nut.getReferencedNuts()) {
                    addReferencedNut(ref);
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public String getName() {
        return name != null ? name : super.getName();
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
        public List<Nut> mergeNuts(final List<Nut> nuts) {
            final List<Nut> retval = new ArrayList<Nut>(nuts.size());
            int start = 0;
            int end = 1;
            String current = null;

            for (final Iterator<Nut> it = nuts.iterator();;) {
                final Nut nut = it.hasNext() ? it.next() : null;

                // New sequence
                if (nut != null && current == null) {
                    current = nut.getName();
                    // Nut name is the same as previous nut name, will be included in same composition
                } else if (nut != null && current.equals(nut.getName())) {
                    end++;
                    // Nut name does not equals previous nut name, add previous composition
                } else {
                    final List<Nut> subList = nuts.subList(start, end);
                    final Nut[] composition = subList.toArray(new Nut[subList.size()]);
                    final String prefix;

                    // Create the composition with a potential prefix to avoid duplicate
                    if (names.add(composition[0].getName())) {
                        prefix = "";
                    } else {
                        prefix = String.valueOf(prefixCount);
                    }

                    retval.add(new CompositeNut(composition, prefix, null));

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
    public static List<Nut> mergeNuts(final List<Nut> nuts) {
        return new Combiner().mergeNuts(nuts);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public InputStream openStream() throws NutNotFoundException {
        return new CompositeInputStream();
    }

    /**
     * <p>
     * Inner class that represents an {@link InputStream} on the stream of each {@link Nut} of the composition in the
     * enclosing class.
     * </p>
     *
     * @author Guillaume DROUET
     * @version 1.0
     * @since 0.4.2
     */
    private class CompositeInputStream extends InputStream {

        /**
         * Current read stream.
         */
        private InputStream current;

        /**
         * Current nut's index in the composition.
         */
        private int index;

        /**
         * Current stream is actually serving the {@link #streamSeparator} bytes.
         */
        private Boolean separating = Boolean.FALSE;

        /**
         * {@inheritDoc}
         */
        @Override
        public int read() throws IOException {
            try {
                // First call
                if (current == null) {
                    current = CompositeNut.this.compositionList[index].openStream();
                }

                // Read the stream
                final int retval = current.read();

                // End of the stream, we can start reading the next stream
                if (retval == -1 && index < CompositeNut.this.compositionList.length - 1) {

                    // We have a stream separator to serve
                    if (streamSeparator != null && !separating) {
                        separating = Boolean.TRUE;
                        current = new ByteArrayInputStream(streamSeparator);
                    } else {
                        separating = Boolean.FALSE;
                        current = CompositeNut.this.compositionList[++index].openStream();
                    }

                    return current.read();
                // Stream not ended or no stream to read anymore
                } else {
                    return retval;
                }
            } catch (NutNotFoundException nnfe) {
                throw new IOException(nnfe);
            }
        }
    }
}
