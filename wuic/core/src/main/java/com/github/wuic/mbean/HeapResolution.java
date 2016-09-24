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


package com.github.wuic.mbean;

import java.util.List;

/**
 * <p>
 * Statistics object for a particular heap resolution.
 * </p>
 *
 * @author Guillaume DROUET
 * @since 0.5.3
 */
public class HeapResolution implements HeapResolutionMXBean {

    /**
     * All the resolved nut of this heap.
     */
    private List<NutResolution> resolutions;

    /**
     * Duration of the resolution.
     */
    private long duration;

    /**
     * <p>
     * Builds a new instance.
     * </p>
     *
     * @param resolutions the nuts resolutions
     * @param duration the total duration
     */
    public HeapResolution(final List<NutResolution> resolutions, final long duration) {
        this.resolutions = resolutions;
        this.duration = duration;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<NutResolution> getResolutions() {
        return resolutions;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getDuration() {
        return duration;
    }
}