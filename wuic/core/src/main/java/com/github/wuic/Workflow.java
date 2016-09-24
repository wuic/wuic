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


package com.github.wuic;

import com.github.wuic.engine.HeadEngine;
import com.github.wuic.engine.NodeEngine;
import com.github.wuic.nut.NutsHeap;

import java.util.Map;

/**
 * <p>
 * A workflow represents a possible manner to process {@link com.github.wuic.nut.Nut nuts} in a {@link com.github.wuic.context.Context}.
 * </p>
 *
 * <p>
 * For each {@link NutType}, the workflow knows a set of {@link NodeEngine engines} chain of responsibility. It's also
 * linked to a {@link NutsHeap heap} which should be processed by the chain which has the same {@link NutType}. The workflow
 * has one chain for each possible type to allow the {@link NodeEngine} to invoke another with if one {@link com.github.wuic.nut.Nut}
 * references a {@link com.github.wuic.nut.Nut} with another {@link NutType}. Optionally, a {@link HeadEngine} could be
 * set to delegate the chain invocation.
 * </p>
 *
 * @author Guillaume DROUET
 * @since 0.4.0
 */
public class Workflow extends WorkflowTemplate {

    /**
     * The heap to be processed.
     */
    private NutsHeap heap;

    /**
     * <p>
     * Creates a new instance.
     * </p>
     *
     * @param c the chains
     * @param h the heap
     * @param head the head (could be {@code null})
     */
    public Workflow(final HeadEngine head, final Map<NutType, ? extends NodeEngine> c, final NutsHeap h) {
        super(head, c);
        heap = h;
    }

    /**
     * <p>
     * Gets the heap.
     * </p>
     *
     * @return the heap
     */
    public NutsHeap getHeap() {
        return heap;
    }
}
