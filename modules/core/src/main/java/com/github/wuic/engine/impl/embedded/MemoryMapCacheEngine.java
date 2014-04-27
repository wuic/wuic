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


package com.github.wuic.engine.impl.embedded;

import com.github.wuic.engine.EngineRequest;

import java.util.Map;
import java.util.HashMap;

/**
 * <p>
 * This {@link com.github.wuic.engine.Engine engine} reads from a {@link java.util.Map} kept in memory
 * the nuts associated to a workflow to be processed.
 * </p>
 *
 * @author Guillaume DROUET
 * @version 1.1
 * @since 0.4.0
 */
public class MemoryMapCacheEngine extends ScheduledCacheEngine {

    /**
     * Memory map.
     */
    private Map<EngineRequest.Key, CacheResult> cache;

    /**
     * <p>
     * Builds a new engine.
     * </p>
     *
     * @param work if cache should be activated or not
     * @param timeToLiveSeconds the time this cache could live
     * @param bestEffort enable best effort mode or not
     */
    public MemoryMapCacheEngine(final Boolean work, final int timeToLiveSeconds, final Boolean bestEffort) {
        super(timeToLiveSeconds, work, bestEffort);
        cache = new HashMap<EngineRequest.Key, CacheResult>();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void putToCache(final EngineRequest.Key request, final CacheResult nuts) {
        cache.put(request, nuts);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeFromCache(final EngineRequest.Key request) {
        cache.remove(request);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CacheResult getFromCache(final EngineRequest.Key request) {
        return cache.get(request);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clearCache() {
        this.cache.clear();
    }
}
