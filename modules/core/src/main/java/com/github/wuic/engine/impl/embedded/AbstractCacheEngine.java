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


package com.github.wuic.engine.impl.embedded;

import com.github.wuic.NutType;
import com.github.wuic.engine.Engine;
import com.github.wuic.engine.EngineRequest;
import com.github.wuic.engine.EngineType;
import com.github.wuic.exception.WuicException;
import com.github.wuic.nut.HeapListener;
import com.github.wuic.nut.Nut;
import com.github.wuic.nut.NutsHeap;
import com.github.wuic.nut.core.ByteArrayNut;
import com.github.wuic.util.IOUtils;
import com.github.wuic.util.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.*;

/**
 * <p>
 * This class is an abstraction of an {@link com.github.wuic.engine.Engine engine}
 * which reads from a cache the nuts associated to a workflow to be processed.
 * If an entry exists, then the nuts are returned and no more engine is executed.
 * Otherwise, the chain is executed and the result is put in the cache.
 * </p>
 *
 * <p>
 * The cache itself is abstract here and it needs to be provided by subclass.
 * </p>
 *
 * @author Guillaume DROUET
 * @version 1.0
 * @since 0.4.0
 */
public abstract class AbstractCacheEngine extends Engine {

    /**
     * Logger.
     */
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    /**
     * If cache or not.
     */
    private Boolean doCache;

    /**
     * <p>
     * Builds a new engine.
     * </p>
     *
     * @param work if cache should be activated or not
     */
    public AbstractCacheEngine(final Boolean work) {
        doCache = work;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked")
    public List<Nut> internalParse(final EngineRequest request) throws WuicException {
        // Log duration
        final Long start = System.currentTimeMillis();
        List<Nut> retval = null;

        if (works()) {
            final String key = request.getWorkflowId();
            final List<Nut> value = getFromCache(key);

            // Nuts exist in doCache, returns them
            if (value != null) {
                log.info("Nuts for heap '{}' found in cache", key);
                retval = value;
            } else if (getNext() != null) {
                // Observe and invalidate the cache when updates are notified
                request.getHeap().addObserver(new InvalidateCache(key));

                final List<Nut> nuts = getNext().parse(request);
                final List<Nut> toCache = new ArrayList<Nut>(nuts.size());

                for (final Nut nut : nuts) {
                    if (nut.isCacheable()) {
                        toCache.add(toByteArrayNut(nut));
                    }
                }

                log.debug("Caching nut with {}", key);
                putToCache(key, toCache);

                retval = toCache;
            }
            // we don't cache so just call the next engine if exists
        } else if (getNext() != null) {
            retval = getNext().parse(request);
        }

        log.info("Cache engine run in {} seconds", (float) (System.currentTimeMillis() - start) / (float) NumberUtils.ONE_THOUSAND);

        return retval;
    }

    /**
     * <p>
     * Converts the given nut and its referenced nuts into nuts wrapping an in memory byte array.
     * </p>
     *
     * @param nut the nut to convert
     * @return the byte array nut
     * @throws com.github.wuic.exception.WuicException if an I/O error occurs
     */
    private Nut toByteArrayNut(final Nut nut) throws WuicException {
        InputStream is = null;

        try {
            is = nut.openStream();
            final ByteArrayOutputStream os = new ByteArrayOutputStream();
            IOUtils.copyStream(is, os);
            final Nut bytes = new ByteArrayNut(os.toByteArray(), nut.getName(), nut.getNutType());

            if (nut.getReferencedNuts() != null) {
                for (Nut ref : nut.getReferencedNuts()) {
                    bytes.addReferencedNut(toByteArrayNut(ref));
                }
            }

            return bytes;
        } finally {
            IOUtils.close(is);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Boolean works() {
        return doCache;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<NutType> getNutTypes() {
        return Arrays.asList(NutType.values());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public EngineType getEngineType() {
        return EngineType.CACHE;
    }

    /**
     * <p>
     * Puts the given list of nuts associated to the specified workflow ID to the cache.
     * </p>
     *
     * @param workflowId the workflow ID
     * @param nuts the nuts
     */
    public abstract void putToCache(String workflowId, List<Nut> nuts);

    /**
     * <p>
     * Removes the given list of nuts associated to the specified workflow ID from the cache.
     * </p>
     *
     * @param workflowId the workflow ID
     */
    public abstract void removeFromCache(String workflowId);

    /**
     * <p>
     * Gets the list of nuts associated to the specified workflow ID from the cache.
     * </p>
     *
     * @param workflowId the workflow ID
     * @return the list of nuts
     */
    public abstract List<Nut> getFromCache(String workflowId);

    /**
     * <p>
     * Internal class that invalidates a cache entry identified with a workflow ID when it's notified that a nut has been
     * updated in an associatied heap.
     * </p>
     *
     * @author Guillaume DROUET
     * @version 1.0
     * @since 0.4.0
     */
    private final class InvalidateCache implements HeapListener {

        /**
         * The workflow ID as cache key.
         */
        private String workflowId;

        /**
         * <p>
         * Builds a new instance.
         * </p>
         *
         * @param wid the cache key
         */
        private InvalidateCache(final String wid) {
            workflowId = wid;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void nutUpdated(final NutsHeap heap) {
            removeFromCache(workflowId);
        }
    }
}
