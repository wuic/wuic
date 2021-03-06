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


package com.github.wuic.engine;

import com.github.wuic.NutType;
import com.github.wuic.NutTypeFactory;
import com.github.wuic.ProcessContext;
import com.github.wuic.context.Event;
import com.github.wuic.context.HeapResolutionEvent;
import com.github.wuic.exception.WorkflowNotFoundException;
import com.github.wuic.mbean.HeapResolution;
import com.github.wuic.mbean.TransformationStat;
import com.github.wuic.mbean.TransformerStat;
import com.github.wuic.mbean.WorkflowExecution;
import com.github.wuic.nut.ConvertibleNut;
import com.github.wuic.nut.HeapListener;
import com.github.wuic.nut.Nut;
import com.github.wuic.nut.NutsHeap;
import com.github.wuic.util.CollectionUtils;
import com.github.wuic.util.Timer;
import com.github.wuic.util.TimerTreeFactory;
import com.github.wuic.util.UrlProviderFactory;

import java.beans.PropertyChangeSupport;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.HashSet;
import java.util.Arrays;
import java.util.Map;

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
 * <p>
 * This request should be used as {@code HeapListener} of any heap created during the process initiated by this object
 * to report the heap resolution statistics.
 * </p>
 *
 * @author Guillaume DROUET
 * @since 0.3.0
 */
public final class EngineRequest implements HeapListener {

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
     * {@inheritDoc}
     */
    @Override
    public void nutUpdated(final NutsHeap heap) {
        // ignore
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void heapResolved(final HeapResolutionEvent event) {
        final List<HeapResolution> list = new LinkedList<HeapResolution>();
        list.add(event.getResolution());
        CollectionUtils.merge(event.getId(), list, engineRequestBuilder.getHeapResolutionStats());
    }

    /**
     * <p>
     * Reports a transformer statistics.
     * </p>
     *
     * @param transformationStats the transformation statistics grouped by transformer
     */
    public void reportTransformerStat(final Map<String, List<TransformationStat>> transformationStats) {
        CollectionUtils.merge(transformationStats, engineRequestBuilder.getTransformationStats());
    }

    /**
     * <p>
     * Reports a parse engine.
     * </p>
     *
     * @param elapsed the time elapsed during parse operation
     */
    public void reportParseEngine(final long elapsed) {
        engineRequestBuilder.incrementParseEngines(elapsed);
    }

    /**
     * <p>
     * Gets a new timer from this request.
     * </p>
     *
     * @return the new timer
     */
    public Timer createTimer() {
        return engineRequestBuilder.getTimerTreeFactory().getTimerTree();
    }

    /**
     * <p>
     * Notifies the statistics related to heap resolutions reported to this request thanks to the given
     * {@code PropertyChangeSupport}.
     * </p>
     *
     * @param propertyChangeSupport the observable
     */
    public void notifyHeapResolutionsTo(final PropertyChangeSupport propertyChangeSupport) {
        for (final Map.Entry<String, List<HeapResolution>> resolutions : getBuilder().getHeapResolutionStats().entrySet()) {
            for (final HeapResolution heapResolution : resolutions.getValue()) {
                final HeapResolutionEvent evt = new HeapResolutionEvent(resolutions.getKey(), heapResolution);
                propertyChangeSupport.firePropertyChange(Event.HEAP_RESOLUTION.name(), null, evt);
            }
        }
    }

    /**
     * <p>
     * Get the statistics reported to this request objects considered as the result of a workflow execution.
     * </p>
     *
     * @return the statistics
     */
    public WorkflowExecution getWorkflowStatistics() {
        long elapsed = 0;

        final List<TransformerStat> transformerStats = new ArrayList<TransformerStat>(getBuilder().getTransformationStats().size());

        for (final Map.Entry<String, List<TransformationStat>> t : getBuilder().getTransformationStats().entrySet()) {
            final TransformerStat s = new TransformerStat(t.getKey(), new LinkedList<TransformationStat>());
            transformerStats.add(s);

            for (final TransformationStat transformationStat : t.getValue()) {
                elapsed += transformationStat.getDuration();
                s.getTransformations().add(transformationStat);
            }
        }

        return new WorkflowExecution(transformerStats, getBuilder().getParseEngines(), elapsed);
    }

    /**
     * <p>
     * Returns the prefix created nut.
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
     * Indicates if an engine of the given type should skip its treatment when this request is submitted.
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
     * Indicates if this request requires best effort mode.
     * </p>
     *
     * @return {@code true} if the request requires best effort, {@code false} otherwise
     */
    public boolean isBestEffort() {
        return engineRequestBuilder.isBestEffort();
    }

    /**
     * <p>
     * Indicates if result is served by WUIC servlet.
     * </p>
     *
     * @return {@code true} if statics are served by WUIC servlet, {@code false} otherwise
     */
    public boolean isStaticsServedByWuicServlet() {
        return engineRequestBuilder.isStaticsServedByWuicServlet();
    }

    /**
     * <p>
     * Creates a new key that identifies this request if {@code null} and return it.
     * </p>
     *
     * @return the key
     */
    public Key getKey() {
        if (key == null) {
            key = new Key(engineRequestBuilder.getWorkflowId(), engineRequestBuilder.getNuts(), engineRequestBuilder.getSkip());
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
     * Gets the {@link ProcessContext}.
     * </p>
     *
     * @return the process context
     */
    public ProcessContext getProcessContext() {
        return engineRequestBuilder.getProcessContext();
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
     * <p>
     * Gets the heap associated to the given workflow.
     * </p>
     *
     * @param workflowId the workflow ID
     * @return the associated heap
     * @throws WorkflowNotFoundException if workflow has not been found
     */
    public NutsHeap getHeap(final String workflowId) throws WorkflowNotFoundException {
        return getBuilder().getContext().getWorkflow(workflowId).getHeap();
    }

    /**
     * <p>
     * Indicates if the given nut should be excluded from any sprite computation.
     * </p>
     *
     * @param nut the nut to exclude or not
     * @return {@code true} if nut is excluded, {@code false} otherwise
     */
    public boolean isExcludedFromSpriteComputation(final Nut nut) {
        return getBuilder().getExcludeFromSprite() != null && getBuilder().getExcludeFromSprite().contains(nut.getInitialName());
    }

    /**
     * <p>
     * Returns the charset.
     * </p>
     *
     * @return the charset
     */
    public String getCharset() {
        return getNutTypeFactory().getCharset();
    }

    /**
     * <p>
     * Gets the nut type factory.
     * </p>
     *
     * @return the nut type factory
     */
    public NutTypeFactory getNutTypeFactory() {
        return getBuilder().getNutTypeFactory();
    }

    /**
     * <p>
     * Gets the nut who originally created this request.
     * </p>
     *
     * @return the origin, {@code null} if the request has not been created by a nut
     */
    public ConvertibleNut getOrigin() {
        return getBuilder().getOrigin();
    }

    /**
     * <p>
     * Gets the {@code TimerTreeFactory}.
     * </p>
     *
     * @return the factory
     */
    public TimerTreeFactory getTimerTreeFactory() {
        return getBuilder().getTimerTreeFactory();
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
         */
        public Key(final String wKey, final List<ConvertibleNut> nutsList, final EngineType ... s)  {
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
