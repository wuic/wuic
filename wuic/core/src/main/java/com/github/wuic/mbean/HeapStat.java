/*
 * "Copyright (c) 2016   Capgemini Technology Services (hereinafter "Capgemini")
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


package com.github.wuic.mbean;

import java.util.LinkedList;
import java.util.List;

/**
 * <p>
 * Statistics object for a all resolutions of a particular heap.
 * </p>
 *
 * @author Guillaume DROUET
 * @since 0.5.3
 */
public class HeapStat implements HeapStatMXBean {

    /**
     * The ID identifying the heap.
     */
    private final String id;

    /**
     * All the resolution for that heap.
     */
    private final List<HeapResolution> resolutions;

    /**
     * Maximum number of resolutions.
     */
    private final int maxResolutions;

    /**
     * The average duration of the resolutions.
     */
    private long averageDuration;

    /**
     * The total time of resolution.
     */
    private long total;

    /**
     * <p>
     * Builds a new instance.
     * </p>
     *
     * @param id the heap ID
     * @param maxResolutions maximum number of resolutions
     */
    public HeapStat(final String id, final int maxResolutions) {
        this.id = id;
        this.averageDuration = -1;
        this.total = 0;
        this.resolutions = new LinkedList<HeapResolution>();
        this.maxResolutions = maxResolutions;
    }

    /**
     * <p>
     * Adds a new resolution.
     * </p>
     *
     * @param resolution the new resolution
     */
    public void addResolution(final HeapResolution resolution) {
        if (resolutions.size() == maxResolutions) {
            total -= resolutions.remove(0).getDuration();
        }

        total += resolution.getDuration();
        resolutions.add(resolution);
        averageDuration = total / resolutions.size();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getId() {
        return id;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<HeapResolution> getResolutions() {
        return resolutions;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getAverageDuration() {
        return averageDuration;
    }
}