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


package com.github.wuic.nut;

import com.github.wuic.context.HeapResolutionEvent;

/**
 * <p>
 * This interface represents a listener which expects to be notified of changes when they occur on a particular
 * heap.
 * </p>
 *
 * <p>
 * To be notified, it must be registered to the {@link NutsHeap} thanks to its  {@link NutsHeap#addObserver(HeapListener)}
 * method.
 * </p>
 *
 * @author Guillaume DROUET
 * @since 0.4.0
 */
public interface HeapListener {

    /**
     * <p>
     * Called when a nut has been updated in the heap.
     * </p>
     *
     * @param heap the updated heap
     */
    void nutUpdated(NutsHeap heap);

    /**
     * <p>
     * Called when a heap has been resolved at the end of {@link NutsHeap#checkFiles(com.github.wuic.ProcessContext)}
     * execution.
     * </p>
     *
     * @param event the heap resolution event
     */
    void heapResolved(HeapResolutionEvent event);
}
