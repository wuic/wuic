/*
 * "Copyright (c) 2013   Capgemini Technology Services (hereinafter "Capgemini")
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
 * @version 1.0
 * @since 0.4.2
 */
public class CompositeNut extends AbstractNut {

    /**
     * The composition list.
     */
    private List<Nut> compositionList;

    /**
     * <p>
     * Builds a new instance.
     * </p>
     *
     * @param composition the nuts of this composition
     * @param prefixName a prefix to add to the nut name
     */
    public CompositeNut(final List<Nut> composition, final String prefixName) {
        super(composition.get(0));
        compositionList = composition;
        nutName = prefixName + nutName;

        // Eventually add each referenced nut in the composition (excluding first element taken in consideration by super constructor)
        for (int i = 1; i < composition.size(); i++) {
            final Nut nut = composition.get(i);

            if (nut.getReferencedNuts() != null) {
                for (final Nut ref : nut.getReferencedNuts()) {
                    addReferencedNut(ref);
                }
            }
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
     * where the first element is a composition.
     * </p>
     *
     * @param nuts the nuts to merge
     * @return a list containing all the nuts with some compositions
     */
    public static List<Nut> mergeNuts(final List<Nut> nuts) {
        final List<Nut> retval = new ArrayList<Nut>(nuts.size());
        int start = 0;
        int end = 1;
        String current = null;
        Nut previous = null;

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
                // Manage case where composition has only one nut
                if ((end - start) == 1) {
                    retval.add(previous);
                } else {
                    // Many nuts in composition, create new object
                    retval.add(new CompositeNut(nuts.subList(start, end), String.valueOf(start)));
                }

                if (nut != null) {
                    // New sequence
                    current = nut.getName();
                    start = end;
                    end++;
                } else {
                    // End of iterator
                    break;
                }
            }

            previous = nut;
        }

        return retval;
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
         * {@inheritDoc}
         */
        @Override
        public int read() throws IOException {
            try {
                // First call
                if (current == null) {
                    current = CompositeNut.this.compositionList.get(index).openStream();
                }

                // Read the stream
                final int retval = current.read();

                // End of the stream, we can start reading the next stream
                if (retval == -1 && index < CompositeNut.this.compositionList.size() - 1) {
                    current = CompositeNut.this.compositionList.get(++index).openStream();
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
