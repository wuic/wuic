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


package com.github.wuic;

import com.github.wuic.engine.Engine;
import com.github.wuic.nut.NutDao;
import com.github.wuic.nut.NutsHeap;

import java.util.Map;

/**
 * <p>
 * A workflow represents a possible manner to process {@link com.github.wuic.nut.Nut nuts} in a {@link Context}.
 * </p>
 *
 * <p>
 * For each {@link NutType}, the workflow knows an {@link Engine engines} chain of responsibility. It's also linked
 * to a {@link NutsHeap heap} which should be processed by the chain which has the same {@link NutType}. The workflow
 * has one chain for each possible type to allow the {@link Engine} to invoke another with if one {@link com.github.wuic.nut.Nut}
 * references a {@link com.github.wuic.nut.Nut} with another {@link NutType}.
 * </p>
 *
 * <p>
 * Finally, the workflow could have zero to many {@link NutDao} where resulting nut should be saved. Consequently,
 * {@link NutDao} must supports {@link NutDao#save(com.github.wuic.nut.Nut)}. This is something which is checked by the
 * {@link ContextBuilder}.
 * </p>
 *
 * @author Guillaume DROUET
 * @version 1.1
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
     * @param store the DAO stores
     */
    public Workflow(final Map<NutType, ? extends Engine> c, final NutsHeap h, final NutDao ... store) {
        super(c, store);
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
