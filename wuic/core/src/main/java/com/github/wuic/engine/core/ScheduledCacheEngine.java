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


package com.github.wuic.engine.core;

import com.github.wuic.nut.NutsHeap;
import com.github.wuic.util.WuicScheduledThreadPool;

import java.util.concurrent.Future;

/**
 * <p>
 * This abstract engine is able to clear its cache in a particular interval.
 * </p>
 *
 * @author Guillaume DROUET
 * @since 0.4.0
 */
public abstract class ScheduledCacheEngine extends AbstractCacheEngine implements Runnable {

    /**
     * Help to know when a polling operation is done.
     */
    private Future<?> clearCacheResult;

    /**
     * <p>
     * Initializes a new instance.
     * </p>
     *
     * @param timeToLiveSeconds time to live in seconds for this cache
     * @param cache if cache is activated or not
     * @param bestEffort enable best effort mode or not
     */
    public void init(final int timeToLiveSeconds, final Boolean cache, final Boolean bestEffort) {
        init(cache, bestEffort);
        setTimeToLive(timeToLiveSeconds);
    }

    /**
     * <p>
     * Schedules the time to live for this cache. If the specified value is a positive number, a thread will be executed
     * at the specified interval to clear the cache. Otherwise, any scheduled operation will be canceled.
     * </p>
     *
     * @param timeToLiveSeconds new time to live of cache in seconds
     */
    public final synchronized void setTimeToLive(final int timeToLiveSeconds) {

        // Stop current scheduling
        if (clearCacheResult != null) {
            clearCacheResult.cancel(false);
            clearCacheResult = null;
        }

        // Create new scheduling if necessary
        if (timeToLiveSeconds > 0) {
            clearCacheResult = WuicScheduledThreadPool.getInstance().executeEveryTimeInSeconds(this, timeToLiveSeconds);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void run() {
        clearCache();
        NutsHeap.ListenerHolder.INSTANCE.clear();
    }

    /**
     * <p>
     * Clears this cache.
     * </p>
     */
    protected abstract void clearCache();
}
