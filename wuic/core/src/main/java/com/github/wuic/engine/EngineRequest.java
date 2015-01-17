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


package com.github.wuic.engine;

import com.github.wuic.NutType;
import com.github.wuic.exception.NutNotFoundException;
import com.github.wuic.exception.wrapper.StreamException;
import com.github.wuic.nut.ConvertibleNut;
import com.github.wuic.nut.NutsHeap;
import com.github.wuic.util.CollectionUtils;
import com.github.wuic.util.UrlProviderFactory;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.HashSet;
import java.util.Arrays;

/**
 * <p>
 * This class represents a request to be indicated to an engine.
 * </p>
 *
 * <p>
 * The user which invokes {@link NodeEngine#parse(EngineRequest)} should indicates in the parameter the nuts
 * to be parsed and the context path to use to expose the generated nuts.
 * </p>
 *
 * @author Guillaume DROUET
 * @version 1.6
 * @since 0.3.0
 */
public final class EngineRequest {

    /**
     * The key generated for the request.
     */
    private Key key;

    /**
     * The builder which built this instance.
     */
    private final EngineRequestBuilder engineRequestBuilder;

    /**
     * <p>
     * Builds a new {@code EngineRequest}.
     * </p>
     *
     * @param erb the builder with request state
     */
    EngineRequest(final EngineRequestBuilder erb) {
        engineRequestBuilder = erb;
    }

    /**
     * <p>
     * Returns the object that built this request.
     * </p>
     *
     * @return the builder
     */
    EngineRequestBuilder getBuilder() {
        return engineRequestBuilder;
    }

    /**
     * <p>
     * Returns the prefix created nut
     * </p>
     *
     * @return the created nut
     */
    public String getPrefixCreatedNut() {
        return engineRequestBuilder.getPrefixCreatedNut();
    }

    /**
     * <p>
     * Gets the nuts.
     * </p>
     *
     * @return the nuts
     */
    public List<ConvertibleNut> getNuts() {
        return engineRequestBuilder.getNuts();
    }

    /**
     * <p>
     * Gets the context path.
     * </p>
     *
     * @return the context path
     */
    public String getContextPath() {
        return engineRequestBuilder.getContextPath();
    }

    /**
     * <p>
     * Returns the heap.
     * </p>
     *
     * @return the heap
     */
    public NutsHeap getHeap() {
        return engineRequestBuilder.getHeap();
    }

    /**
     * <p>
     * Gets the workflow ID.
     * </p>
     *
     * @return the workflow ID.
     */
    public String getWorkflowId() {
        return engineRequestBuilder.getWorkflowId();
    }

    /**
     * <p>
     * Creates a new iterator on the {@link com.github.wuic.nut.Nut nuts}.
     * </p>
     *
     * @return the iterator
     */
    public Iterator<List<? extends ConvertibleNut>> iterator() {
        return new NutsIterator();
    }

    /**
     * <p>
     * Gets the chains which can treat nuts of the given {@link NutType}.
     * </p>
     *
     * @param nutType the nut type
     * @return the chains that can treat this nut type
     */
    public NodeEngine getChainFor(final NutType nutType) {
        return engineRequestBuilder.getChainFor(nutType);
    }

    /**
     * <p>
     * Indicates if a engine of the given type should skip its treatment when this request is submitted.
     * </p>
     *
     * @param engineType the {@link EngineType}
     * @return {@code true} if treatment should be skipped, {@code false} otherwise.
     */
    public boolean shouldSkip(final EngineType engineType) {
        return engineRequestBuilder.shouldSkip(engineType);
    }

    /**
     * <p>
     * Creates a new key that identifies this request if {@code null} and return it.
     * </p>
     *
     * @return the key
     * @throws NutNotFoundException if nuts of this request are not correctly built
     * @throws StreamException if I/O error occurs
     */
    public Key getKey() throws NutNotFoundException, StreamException {
        if (key == null) {
            key = new Key(engineRequestBuilder.getWorkflowKey(), engineRequestBuilder.getNuts());
        }

        return key;
    }

    /**
     * <p>
     * Gets the {@link UrlProviderFactory}.
     * </p>
     *
     * @return the factory
     */
    public UrlProviderFactory getUrlProviderFactory() {
        return engineRequestBuilder.getUrlProviderFactory();
    }

    /**
     * <p>
     * Returns an array containing all items of {@link EngineRequestBuilder#skip} and also the specified types.
     * </p>
     *
     * @param type the array to add
     * @return the skipped engines including the parameter
     */
    public EngineType[] alsoSkip(final EngineType ... type) {
        final EngineType[] skip = engineRequestBuilder.getSkip();
        int length = skip.length;

        for (final EngineType et : type) {
            if (CollectionUtils.indexOf(et, skip) < 0) {
                length++;
            }
        }

        final EngineType[] retval = new EngineType[length];
        System.arraycopy(skip, 0, retval, 0, skip.length);

        for (final EngineType et : type) {
            if (CollectionUtils.indexOf(et, skip) < 0) {
                retval[--length] = et;
            }
        }

        return retval;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return getClass().getSimpleName() + " with nuts '" + Arrays.deepToString(getNuts().toArray());
    }

    /**
     * <p>
     * Internal class which helps iterating on all its {@link com.github.wuic.nut.Nut nuts}.
     * The {@link com.github.wuic.nut.Nut nuts} are read and returned by sequence of elements
     * having the same {@link NutType}.
     * </p>
     *
     * @author Guillaume DROUET
     * @version 1.0
     * @since 0.4.3
     */
    private final class NutsIterator implements Iterator<List<? extends ConvertibleNut>> {

        /**
         * Iterator.
         */
        private Iterator<ConvertibleNut> iterator;

        /**
         * Next element.
         */
        private ConvertibleNut next;

        /**
         * <p>
         * Builds a new instance by initializing the iterator.
         * </p>
         */
        NutsIterator() {
            iterator = getNuts().iterator();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean hasNext() {
            return iterator.hasNext() || next != null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public List<? extends ConvertibleNut> next() {
            if (next == null) {
                next = iterator.next();
            }

            final LinkedList<ConvertibleNut> retval = new LinkedList<ConvertibleNut>();
            retval.add(next);
            next = null;

            // Iterate until the engine type change
            while (iterator.hasNext()) {
                next = iterator.next();

                if (next.getInitialNutType().equals(retval.getLast().getInitialNutType())) {
                    retval.add(next);
                    next = null;
                } else {
                    return retval;
                }
            }

            return retval;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * <p>
     * This class overrides {@link Object#equals(Object)} and {@link Object#hashCode()} to represent an unique key
     * per {@link EngineRequest} based on its workflow ID and its nuts.
     * </p>
     *
     * @author Guillaume DROUET
     * @version 1.0
     * @since 0.4.3
     */
    public static final class Key implements Serializable {

        /**
         * The workflow key.
         */
        private String workflowKey;

        /**
         * The nuts.
         */
        private List<String> nuts;

        /**
         * Skipped engines.
         */
        private EngineType[] skip;

        /**
         * <p>
         * Builds a new instance.
         * </p>
         *
         * @param s the skipped engine types
         * @param wKey the workflow key
         * @param nutsList the nuts
         * @throws StreamException if an I/O error occurs
         * @throws NutNotFoundException if given nut not normally created
         */
        public Key(final String wKey, final List<ConvertibleNut> nutsList, final EngineType ... s)
                throws NutNotFoundException, StreamException {
            workflowKey = wKey;
            nuts = new ArrayList<String>(nutsList.size());
            skip = s;

            for (final ConvertibleNut n : nutsList) {
                nuts.add(n.getName());
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean equals(final Object other) {
            if (other instanceof Key) {
                final Key request = (Key) other;
                return workflowKey.equals(request.workflowKey)
                        && Arrays.equals(skip, request.skip)
                        && CollectionUtils.difference(new HashSet<String>(nuts), new HashSet<String>(request.nuts)).isEmpty();
            } else {
                return false;
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int hashCode() {
            return CollectionUtils.deepHashCode(workflowKey, nuts.toArray(), skip);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return workflowKey + " / " + Arrays.deepToString(nuts.toArray());
        }
    }
}
