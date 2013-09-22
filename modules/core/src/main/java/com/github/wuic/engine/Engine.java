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

package com.github.wuic.engine;

import com.github.wuic.NutType;
import com.github.wuic.exception.WuicException;
import com.github.wuic.exception.wrapper.BadArgumentException;
import com.github.wuic.nut.Nut;

import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Deque;
import java.util.Collections;


/**
 * <p>
 * An engine is in charge to parse a set of files. They are generally able to
 * parse an unique kind of path {@link com.github.wuic.NutType type}.
 * </p>
 * 
 * <p>
 * WUIC framework consists of a set of {@link Engine} to be executed. They are
 * structured using the chain of responsibility design pattern. Each engine is
 * in charge of the execution of the next engine and could decide not to execute
 * it.
 * </p>
 *
 * <p>
 * Engines comparisons is based on their {@link EngineType}.
 * </p>
 * 
 * @author Guillaume DROUET
 * @version 1.3
 * @since 0.1.0
 */
public abstract class Engine implements Comparable<Engine> {

    /**
     * The next engine.
     */
    private Engine nextEngine;

    /**
     * Previous engine.
     */
    private Engine previousEngine;

    /**
     * <p>
     * Link the given {@link Engine engines}. They will be linked respecting the order of the implied by their
     * {@link Engine#getEngineType()}.
     * </p>
     *
     * <p>
     * If an {@link Engine} is already chained to other {@link Engine engines}, any engine won't be added
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
    public static Engine chain(final Engine ... engines) {
        if (engines.length == 0) {
            throw new BadArgumentException(new IllegalArgumentException(
                    "A chain must be built with a non-empty array of engines"));
        }

        final List<Engine> flatten = new LinkedList<Engine>();
        final Deque<Engine> retval = new LinkedList<Engine>();

        // Flat the all the chains to improve data structure manipulations
        for (final Engine engine : engines) {
            Engine next = engine;

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
        for (final Engine engine : flatten) {

            // Descending iteration to keep duplicate instance on the right and not on the left
            final ListIterator<Engine> it = flatten.listIterator(flatten.size());

            for (;it.hasPrevious();) {
                final Engine previous = it.previous();

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
     * Parses the given files and returns the result of this operation.
     * </p>
     * 
     * <p>
     * Should throw an {@link com.github.wuic.exception.wrapper.BadArgumentException} the files type is not
     * supported by this {@link Engine}.
     * </p>
     *
     * @param request the request with files to parse
     * @return the parsed files
     * @throws com.github.wuic.exception.wrapper.StreamException if any kind of I/O error occurs
     */
    public abstract List<Nut> parse(EngineRequest request) throws WuicException;

    /**
     * <p>
     * Gets the all {@link NutType types} supported by this engine.
     * </p>
     *
     * @return the {@link NutType}
     */
    public abstract List<NutType> getNutTypes();

    /**
     * <p>
     * Gets the type of engine.
     * </p>
     *
     * @return the type of process done by with engine
     */
    public abstract EngineType getEngineType();

    /**
     * <p>
     * Returns a flag indicating if the engine is configured to do something
     * when {@link Engine#parse(EngineRequest)} is called or not.
     * </p>
     * 
     * @return {@code true} is something will be done, {@code false} otherwise
     */
    public abstract Boolean works();
    
    /**
     * <p>
     * The next {@link Engine} to be execute din the chain of responsibility. If
     * it is not set, then this {@link Engine} is the last one to be executed.
     * </p>
     * 
     * @param next the next {@link Engine}
     */
    public void setNext(final Engine next) {
        nextEngine = next;

        if (nextEngine != null) {
            nextEngine.previousEngine = this;
        }
    }
    
    /**
     * <p>
     * Returns the next engine previously set with {@link Engine#setNext(Engine)}
     * method.
     * </p>
     * 
     * @return the next {@link Engine}
     */
    public Engine getNext() {
        return nextEngine;
    }

    /**
     * <p>
     * Returns the previous engine in the chain.
     * </p>
     *
     * @return the previous {@link Engine}
     */
    public Engine getPrevious() {
        return previousEngine;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final int compareTo(final Engine other) {
        return getEngineType().compareTo(other.getEngineType());
    }
}
