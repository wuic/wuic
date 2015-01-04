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
import com.github.wuic.exception.WuicException;
import com.github.wuic.exception.wrapper.BadArgumentException;
import com.github.wuic.nut.ConvertibleNut;

import java.util.Collections;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

/**
 * <p>
 * Linkable {@link Engine}.
 * </p>
 *
 * <p>
 * Fundamental design inside WUIC is to use a set of {@link NodeEngine} to be executed.
 * They are structured using the chain of responsibility design pattern. Each engine is
 * in charge of the execution of the next engine and could decide not to execute it.
 * </p>
 *
 * @author Guillaume DROUET
 * @version 1.0
 * @since 0.4.4
 */
public abstract class NodeEngine extends Engine {

    /**
     * The next engine.
     */
    private NodeEngine nextEngine;

    /**
     * Previous engine.
     */
    private NodeEngine previousEngine;

    /**
     * <p>
     * Link the given {@link NodeEngine engines}. They will be linked respecting the order of the implied by their
     * {@link NodeEngine#getEngineType()}.
     * </p>
     *
     * <p>
     * If an {@link NodeEngine} is already chained to other {@link NodeEngine engines}, any engine won't be added
     * as the next engine but to the end of the existing chain.
     * </p>
     *
     * <p>
     * If two different instances of the same class appear in the chain, then the first one will be replaced by the
     * second one, keeping the original position.
     * </p>
     *
     * @param engines the engines
     * @return the first engine of the given array
     */
    public static NodeEngine chain(final NodeEngine ... engines) {
        if (engines.length == 0) {
            throw new BadArgumentException(new IllegalArgumentException(
                    "A chain must be built with a non-empty array of engines"));
        }

        final List<NodeEngine> flatten = new LinkedList<NodeEngine>();
        final Deque<NodeEngine> retval = new LinkedList<NodeEngine>();

        // Flat the all the chains to improve data structure manipulations
        for (final NodeEngine engine : engines) {
            NodeEngine next = engine;

            if (engine == null) {
                continue;
            }

            do {
                flatten.add(next);
                next = next.nextEngine;
            } while (next != null);
        }

        Collections.sort(flatten);

        // Going to reorganize the chain to keep one instance per class
        forLoop :
        for (final NodeEngine engine : flatten) {

            // Descending iteration to keep duplicate instance on the right and not on the left
            final ListIterator<NodeEngine> it = flatten.listIterator(flatten.size());

            for (; it.hasPrevious();) {
                final NodeEngine previous = it.previous();

                // Already added in the chain, nothing to add
                if (retval.contains(previous)) {
                    break;
                    // Two instances of the same class, keep only one
                } else if (engine.getClass().equals(previous.getClass())) {
                    if (!retval.isEmpty()) {
                        retval.getLast().setNext(previous);
                    } else {
                        // This is the head of the chain
                        previous.previousEngine = null;
                    }

                    retval.add(previous);
                    continue forLoop;
                }
            }

            if (!retval.contains(engine)) {
                if (!retval.isEmpty()) {
                    retval.getLast().setNext(engine);
                }

                retval.add(engine);
            }
        }

        return retval.getFirst();
    }
    
    /**
     * <p>
     * Gets the all {@link com.github.wuic.NutType types} supported by this engine.
     * </p>
     *
     * @return the {@link com.github.wuic.NutType}
     */
    public abstract List<NutType> getNutTypes();

    /**
     * {@inheritDoc}
     */
    @Override
    public List<ConvertibleNut> parse(final EngineRequest request) throws WuicException {

        // Skip this engine parsing
        if (request.shouldSkip(getEngineType())) {

            // Go to next engine if present
            if (getNext() != null) {
                return getNext().parse(request);
            } else {
                // Nothing to do
                return request.getNuts();
            }
        } else {
            // Delegate to subclass
            return internalParse(request);
        }
    }

    /**
     * <p>
     * The next {@link NodeEngine} to be execute din the chain of responsibility. If
     * it is not set, then this {@link NodeEngine} is the last one to be executed.
     * </p>
     *
     * @param next the next {@link Engine}
     */
    public void setNext(final NodeEngine next) {
        nextEngine = next;

        if (nextEngine != null) {
            nextEngine.previousEngine = this;
        }
    }

    /**
     * <p>
     * Returns the next engine previously set with {@link NodeEngine#setNext(NodeEngine)}
     * method.
     * </p>
     *
     * @return the next {@link Engine}
     */
    public NodeEngine getNext() {
        return nextEngine;
    }

    /**
     * <p>
     * Returns the previous engine in the chain.
     * </p>
     *
     * @return the previous {@link Engine}
     */
    public NodeEngine getPrevious() {
        return previousEngine;
    }
}
