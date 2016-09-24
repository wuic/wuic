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

import com.github.wuic.exception.WuicException;
import com.github.wuic.nut.ConvertibleNut;

import java.util.List;

/**
 * <p>
 * This interface represents a listener to be notified when an {@link LineInspector} matches some data in an inspected
 * stream.
 * </p>
 *
 * @author Guillaume DROUET
 * @since 0.5.3
 */
public interface LineInspectorListener {

    /**
     * <p>
     * Notification method called by the {@link LineInspector}.
     * A string representation of the matched data can be created with {@code new String(data, offset, length)}.
     * </p>
     *
     * @param data the data buffer
     * @param offset the start position of matched data
     * @param length the number of matched characters
     * @param replacement the replacement for this data
     * @param extracted the nuts extracted from the matched data (could be {@code null})
     * @throws WuicException if event processing fails
     */
    void onMatch(char[] data, int offset, int length, String replacement, List<? extends ConvertibleNut> extracted)
            throws WuicException;
}
