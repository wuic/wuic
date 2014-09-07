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

import com.github.wuic.util.CollectionUtils;

/**
 * <p>
 * This enumeration describes the different kind of {@link Engine} that may exist.
 * </p>
 *
 * <p>
 * In a chain of responsibility, the {@link Engine engines} should be sorted following their {@link EngineType}.
 * Sorting could be performed with this {@link Comparable} implementation. This way, a chain will always as the same
 * behavior:
 * <ul>
 *     <li>See if result is already cached or not.</li>
 *     <li>Inspects all nuts and eventually transform them</li>
 *     <li>Perform text compression on text files like scripts</li>
 *     <li>Perform aggregation</li>
 *     <li>Compress bytes</li>
 * </ul>
 * </p>
 *
 * @author Guillaume DROUET
 * @version 1.1
 * @since 0.4.0
 */
public enum EngineType {

    /**
     * First type in a chain. Cache the result.
     */
    CACHE,

    /**
     * Second type in a chain. Inspects and eventually transforms the {@link com.github.wuic.nut.dao.NutDao}.
     */
    INSPECTOR,

    /**
     * Third type in a chain. Compress the text only.
     */
    MINIFICATION,

    /**
     * Fourth type in a chain. Aggregates the text only.
     */
    AGGREGATOR,

    /**
     * Fifth type in a chain. Compress bytes.
     */
    BINARY_COMPRESSION;

    private EngineType(final EngineType ... requiredForBestEffort) {

    }

    /**
     * <p>
     * Returns all the {@link EngineType} without the specified one.
     * </p>
     *
     * @param type the object to exclude
     * @return the values without the given type
     */
    public static EngineType[] without(final EngineType ... type) {
        final EngineType[] retval = new EngineType[values().length - type.length];
        final int[] exclude = new int[type.length];

        for (int i = 0; i < type.length; i++) {
            exclude[i] = type[i].ordinal();
        }

        CollectionUtils.without(values(), retval, exclude);
        return retval;
    }
}
