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


package com.github.wuic.engine;

import com.github.wuic.NutType;
import com.github.wuic.exception.NutNotFoundException;
import com.github.wuic.exception.wrapper.StreamException;
import com.github.wuic.nut.NutsHeap;
import com.github.wuic.nut.Nut;
import com.github.wuic.util.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
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
     * Logger.
     */
    private final Logger log = LoggerFactory.getLogger(getClass());

    /**
     * The nuts.
     */
    private List<Nut> nuts;

    /**
     * The context path.
     */
    private String contextPath;

    /**
     * The heap.
     */
    private NutsHeap heap;

    /**
     * The workflow ID.
     */
    private String workflowId;

    /**
     * The key this request uses to compute the key.
     */
    private String workflowKey;

    /**
     * The engine chains for each type.
     */
    private Map<NutType, ? extends NodeEngine> chains;

    /**
     * {@link EngineType} that should be skipped during workflow execution.
     */
    private EngineType[] skip;

    /**
     * The key generated for the request.
     */
    private Key key;

    /**
     * The prefix path of created nuts.
     */
    private String prefixCreatedNut;

    /**
     * <p>
     * Builds a new {@code EngineRequest} with some specific nuts and a workflow ID.
     * </p>
     *
     * @param wId the workflow ID
     * @param n the nuts to be parsed
     * @param other the request to copy
     * @param pcn prefix created nut
     * @param toSkip the engine's type that should be skipped when request is sent to an engine chain
     */
    public EngineRequest(final String wId, final List<Nut> n, final EngineRequest other, final String pcn, final EngineType ... toSkip) {
        this(wId, other.contextPath, other.heap, n, other.chains, pcn, toSkip);
    }

    /**
     * <p>
     * Builds a new {@code EngineRequest} with some specific nuts and a workflow ID.
     * </p>
     *
     * @param wId the workflow ID
     * @param n the nuts to be parsed
     * @param other the request to copy
     * @param toSkip the engine's type that should be skipped when request is sent to an engine chain
     */
    public EngineRequest(final String wId, final List<Nut> n, final EngineRequest other, final EngineType ... toSkip) {
        this(wId, other.contextPath, other.heap, n, other.chains, other.prefixCreatedNut, toSkip);
    }

    /**
     * <p>
     * Builds a new {@code EngineRequest} with some specific nuts and a specified context path to be used.
     * </p>
     *
     * @param n the nuts to be parsed
     * @param other the request to copy
     * @param toSkip the engines type to skip
     */
    public EngineRequest(final List<Nut> n, final EngineRequest other, final EngineType[] toSkip) {
        this(other.workflowId, other.contextPath, other.heap, n, other.chains, other.prefixCreatedNut, toSkip);
    }

    /**
     * <p>
     * Builds a new {@code EngineRequest} with some specific nuts to be used.
     * </p>
     *
     * @param n the nuts to be parsed
     * @param other the request to copy
     */
    public EngineRequest(final List<Nut> n, final EngineRequest other) {
        this(other.workflowId, other.contextPath, other.heap, n, other.chains, other.prefixCreatedNut, other.skip);
    }

    /**
     * <p>
     * Builds a new {@code EngineRequest} with some specific nuts to be used.
     * </p>
     *
     * @param workflowId the workflowId
     * @param workflowKey a specific workflow key
     * @param other the request to copy
     */
    public EngineRequest(final String workflowId, final String workflowKey, final EngineRequest other) {
        this(workflowId, workflowKey, other.contextPath, other.heap, other.getNuts(), other.chains, other.prefixCreatedNut, other.skip);
    }

    /**
     * <p>
     * Builds a new {@code EngineRequest} with specific workflow ID.
     * </p>
     *
     * @param wid the workflow ID
     * @param other the request to copy
     */
    public EngineRequest(final String wid, final EngineRequest other) {
        this(wid, other.contextPath, other.heap, other.getNuts(), other.chains, other.prefixCreatedNut, other.skip);
    }

    /**
     * <p>
     * Builds a new {@code EngineRequest} with a specific timestamp identifying nuts version path.
     * </p>
     *
     * @param other the request to copy
     */
    public EngineRequest(final EngineRequest other) {
        this(other.workflowId, other.contextPath, other.heap, other.nuts, other.chains, other.prefixCreatedNut, other.skip);
    }

    /**
     * <p>
     * Builds a new {@code EngineRequest} with some nuts specific and a specified heap to be used.
     * </p>
     *
     * @param n the nuts to be parsed
     * @param h the heap
     * @param other the request to copy
     * @param toSkip the engine's type that should be skipped when request is sent to an engine chain
     */
    public EngineRequest(final List<Nut> n, final NutsHeap h, final EngineRequest other, final EngineType[] toSkip) {
        this(other.workflowId, other.contextPath, h, n, other.chains, other.prefixCreatedNut, toSkip);
    }

    /**
     * <p>
     * Builds a new {@code EngineRequest} with some nuts specific and a specified heap to be used.
     * </p>
     *
     * @param n the nuts to be parsed
     * @param h the heap
     * @param other the request to copy
     */
    public EngineRequest(final List<Nut> n, final NutsHeap h, final EngineRequest other) {
        this(other.workflowId, other.contextPath, h, n, other.chains, other.prefixCreatedNut, other.skip);
    }

    /**
     * <p>
     * Builds a new {@code EngineRequest} with a timestamp version which equals to 0 and the nuts retrieved from the specified
     * heap.
     * </p>
     *
     * @param wid the workflow ID
     * @param cp the context root where the generated nuts should be exposed
     * @param h the heap
     * @param c the engine chains
     * @param toSkip some engine types to skip
     */
    public EngineRequest(final String wid,
                         final String cp,
                         final NutsHeap h,
                         final Map<NutType, ? extends NodeEngine> c,
                         final EngineType ... toSkip) {
        this(wid, cp, h, new ArrayList<Nut>(h.getNuts()), c, "", toSkip);
    }

    /**
     * <p>
     * Builds a new {@code EngineRequest} with all elements of the state (attributes) specified in parameter except the
     * workflow key which is the workflow ID by default.
     * </p>
     *
     * @param wid the workflow ID
     * @param cp the context root where the generated nuts should be exposed
     * @param h the heap
     * @param c the engine chains
     * @param n the nuts
     * @param pcn prefix created nut
     * @param toSkip some engine types to skip
     */
    public EngineRequest(final String wid,
                         final String cp,
                         final NutsHeap h,
                         final List<Nut> n,
                         final Map<NutType, ? extends NodeEngine> c,
                         final String pcn,
                         final EngineType ... toSkip) {
        this(wid, wid, cp, h, n, c, pcn, toSkip);
    }

    /**
     * <p>
     * Builds a new {@code EngineRequest} with all elements of the state (attributes) specified in parameter.
     * </p>
     *
     * @param wid the workflow ID
     * @param wk the workflow key
     * @param cp the context root where the generated nuts should be exposed
     * @param h the heap
     * @param c the engine chains
     * @param n the nuts
     * @param pcn prefix created nut
     * @param toSkip some engine types to skip
     */
    public EngineRequest(final String wid,
                         final String wk,
                         final String cp,
                         final NutsHeap h,
                         final List<Nut> n,
                         final Map<NutType, ? extends NodeEngine> c,
                         final String pcn,
                         final EngineType ... toSkip) {
        nuts = new ArrayList<Nut>(n);
        workflowKey = wk;
        contextPath = cp;
        heap = h;
        chains = c;
        workflowId = wid;
        prefixCreatedNut = pcn;
        skip = new EngineType[toSkip.length];
        System.arraycopy(toSkip, 0, skip, 0, toSkip.length);
    }

    /**
     * <p>
     * Returns the prefix created nut
     * </p>
     *
     * @return the created nut
     */
    public String getPrefixCreatedNut() {
        return prefixCreatedNut;
    }

    /**
     * <p>
     * Gets the nuts.
     * </p>
     *
     * @return the nuts
     */
    public List<Nut> getNuts() {
        return nuts;
    }

    /**
     * <p>
     * Gets the context path.
     * </p>
     *
     * @return the context path
     */
    public String getContextPath() {
        return contextPath;
    }

    /**
     * <p>
     * Returns the heap.
     * </p>
     *
     * @return the heap
     */
    public NutsHeap getHeap() {
        return heap;
    }

    /**
     * <p>
     * Gets the workflow ID.
     * </p>
     *
     * @return the workflow ID.
     */
    public String getWorkflowId() {
        return workflowId;
    }

    /**
     * <p>
     * Creates a new iterator on the {@link Nut nuts}.
     * </p>
     *
     * @return the iterator
     * @see NutsIterator
     */
    public Iterator<List<Nut>> iterator() {
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
        final NodeEngine retval = chains.get(nutType);

        if (retval == null) {
            log.warn("No chain exists for the heap '{}' and the nut type {}.", heap.getId(), nutType.name());
        }

        return retval;
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
        return CollectionUtils.indexOf(engineType, skip) != -1;
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
            key = new Key(workflowKey, nuts);
        }

        return key;
    }

    /**
     * <p>
     * Returns an array containing all items of {@link EngineRequest#skip} and also the specified types.
     * </p>
     *
     * @param type the array to add
     * @return the skipped engines including the parameter
     */
    public EngineType[] alsoSkip(final EngineType ... type) {
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
     * <p>
     * Internal class which helps iterating on all its {@link Nut nuts}. The {@link Nut nuts}
     * are read and returned by sequence of elements having the same {@link NutType}.
     * </p>
     *
     * @author Guillaume DROUET
     * @version 1.0
     * @since 0.4.3
     */
    private final class NutsIterator implements Iterator<List<Nut>> {

        /**
         * Iterator.
         */
        private Iterator<Nut> iterator;

        /**
         * Next element.
         */
        private Nut next;

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
        public List<Nut> next() {
            if (next == null) {
                next = iterator.next();
            }

            final LinkedList<Nut> retval = new LinkedList<Nut>();
            retval.add(next);
            next = null;

            // Iterate until the engine type change
            while (iterator.hasNext()) {
                next = iterator.next();

                if (next.getNutType().equals(retval.getLast().getNutType())) {
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
        public Key(final String wKey, final List<Nut> nutsList, final EngineType ... s) throws NutNotFoundException, StreamException {
            workflowKey = wKey;
            nuts = new ArrayList<String>(nutsList.size());
            skip = s;

            for (final Nut n : nutsList) {
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
