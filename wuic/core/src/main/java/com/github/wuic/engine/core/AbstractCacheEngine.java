/*
 * "Copyright (c) 2015   Capgemini Technology Services (hereinafter "Capgemini")
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

import com.github.wuic.Logging;
import com.github.wuic.engine.EngineRequest;
import com.github.wuic.engine.EngineRequestBuilder;
import com.github.wuic.engine.EngineType;
import com.github.wuic.engine.HeadEngine;
import com.github.wuic.exception.WuicException;
import com.github.wuic.nut.ConvertibleNut;
import com.github.wuic.nut.HeapListener;
import com.github.wuic.nut.NutsHeap;
import com.github.wuic.nut.PrefixedNut;
import com.github.wuic.nut.ByteArrayNut;
import com.github.wuic.util.NumberUtils;
import com.github.wuic.util.NutUtils;
import com.github.wuic.util.WuicScheduledThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * <p>
 * This class is an abstraction of an {@link com.github.wuic.engine.Engine engine}
 * which reads from a cache the nuts associated to a workflow to be processed.
 * If an entry exists, then the nuts are returned and no more engine is executed.
 * Otherwise, the chain is executed and the result is put in the cache.
 * </p>
 *
 * <p>
 * The engine could be configured to be in best effort mode. In that case, if the result of the request is not already
 * in the cache, it just call only the mandatory operations in the chain to deliver as fast as possible a response to the
 * client. All operations are done asynchronously and result is added to the cache when finished. This mode allows to not
 * increase response time for the user when he is the first to send the request even if nuts are not fully processed.
 * </p>
 *
 * <p>
 * The cache itself is abstract here and it needs to be provided by subclass.
 * </p>
 *
 * @author Guillaume DROUET
 * @version 1.3
 * @since 0.4.0
 */
public abstract class AbstractCacheEngine extends HeadEngine {

    /**
     * Logger.
     */
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    /**
     * If cache or not.
     */
    private Boolean doCache;

    /**
     * Indicates if this engine applies best effort or not.
     */
    private Boolean bestEffort;

    /**
     * The future default parsing done asynchronously.
     */
    private final Map<EngineRequest.Key, Future<Map<String, ConvertibleNut>>> parsingDefault;

    /**
     * The future best effort parsing done asynchronously.
     */
    private final Map<EngineRequest.Key, ParseBestEffortCall> parsingBestEffort;

    /**
     * <p>
     * Builds a new engine.
     * </p>
     *
     * @param work if cache should be activated or not
     * @param be apply best effort
     */
    public AbstractCacheEngine(final Boolean work, final Boolean be) {
        doCache = work;
        bestEffort = be;
        parsingDefault = new HashMap<EngineRequest.Key, Future<Map<String, ConvertibleNut>>>();
        parsingBestEffort = new HashMap<EngineRequest.Key, ParseBestEffortCall>();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked")
    public List<ConvertibleNut> internalParse(final EngineRequest request) throws WuicException {
        // Log duration
        final Long start = System.currentTimeMillis();
        List<ConvertibleNut> retval;

        final EngineRequest.Key key = request.getKey();
        final CacheResult value = getFromCache(key);

        // Nuts exist in cache, returns them
        if (value != null) {
            log.info("Nuts for request '{}' found in cache", request);
            retval = new ArrayList<ConvertibleNut>((value.getDefaultResult() != null ? value.getDefaultResult() : value.getBestEffortResult()).values());
        } else {
            // Removes from cache when an update is detected
            request.getHeap().addObserver(new InvalidateCache(key));

            final Map<String, ConvertibleNut> toCache;

            // We are in best effort, do the minimal of operations and return the resulting nut
            if (bestEffort) {
                final EngineRequest req = new EngineRequestBuilder(request)
                        .prefixCreatedNut("best-effort")
                        .skip(EngineType.AGGREGATOR, EngineType.BINARY_COMPRESSION, EngineType.MINIFICATION)
                        .bestEffort()
                        .build();

                final List<ConvertibleNut> result = runChains(req);
                final List<ConvertibleNut> prefixed = new ArrayList<ConvertibleNut>(result.size());

                for (final ConvertibleNut nut : result) {
                    // Nut will differ from full processed version thanks to its prefix
                    prefixed.add(new PrefixedNut(nut, "best-effort"));
                }

                try {
                    retval = ByteArrayNut.toByteArrayNut(prefixed);
                } catch (IOException ioe) {
                    WuicException.throwWuicException(ioe);
                    return null;
                }

                final Map<String, ConvertibleNut> bestEffortResult = new HashMap<String, ConvertibleNut>(retval.size());

                for (final ConvertibleNut nut : retval) {
                    bestEffortResult.put(nut.getName(), nut);
                }

                // Get and create if not exists the job that parse nuts
                synchronized (parsingBestEffort) {
                    if (parsingBestEffort.get(key) == null) {
                        final ParseBestEffortCall call = new ParseBestEffortCall(request, bestEffortResult);
                        WuicScheduledThreadPool.getInstance().executeAsap(call);
                        parsingBestEffort.put(key, call);
                    }
                }
            } else {
                // Not in best effort, we can wait for the end of the job
                toCache = new ParseDefaultCall(request).call();
                retval = new ArrayList<ConvertibleNut>(toCache.values());
            }
        }

        Logging.TIMER.log("Cache engine run in {} seconds", (float) (System.currentTimeMillis() - start) / (float) NumberUtils.ONE_THOUSAND);

        return retval;
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
    public EngineType getEngineType() {
        return EngineType.CACHE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ConvertibleNut parse(final EngineRequest request, final String path) throws WuicException {
        // Log duration
        final Long start = System.currentTimeMillis();
        ConvertibleNut retval = null;

        // Apply cache support
        if (works()) {
            // Retrieving the result form the cache first
            final EngineRequest.Key key = request.getKey();
            final Future<Map<String, ConvertibleNut>> future;
            final Map<String, ConvertibleNut> value;

            // Indicates if we are looking for a nut from best effort process or not
            final Boolean isBestEffort = path.startsWith("best-effort");

            if (isBestEffort) {
                final ParseBestEffortCall call;
                synchronized (parsingBestEffort) {
                    call = parsingBestEffort.get(key);
                }

                if (call != null) {
                    value = call.bestEffortResult;
                } else {
                    CacheResult result = getFromCache(key);

                    if (result == null) {
                        parse(request, path);
                        result = getFromCache(key);
                    }

                    value = result.bestEffortResult;
                }
            } else {
                synchronized (parsingDefault) {
                    future = parsingDefault.get(key);
                }

                if (future != null) {
                    waitAndGet(future);
                }

                final CacheResult result = getFromCache(key);

                if (result == null) {
                    parse(request);
                    return parse(request, path);
                } else {
                    value = result.defaultResult;
                }
            }

            // Find the value
            retval = value.get(path);

            // TODO : we should also add the referenced nut in the map to not iterate in the list which is slower
            if (retval == null) {
                for (final Map.Entry<String, ConvertibleNut> entry : value.entrySet()) {
                    if (entry.getValue().getReferencedNuts() != null) {
                        retval = NutUtils.findByName(entry.getValue().getReferencedNuts(), path);

                        if (retval != null) {
                            break;
                        }
                    }
                }
            }
        // we don't cache so just call the next engine if exists
        } else {
            final List<ConvertibleNut> list = runChains(request);

            for (final ConvertibleNut nut : list) {
                if (nut.getName().equals(path)) {
                    retval = nut;
                    break;
                }
            }
        }

        Logging.TIMER.log("'{}' retrieved from cache engine in {} ms", path, (System.currentTimeMillis() - start));

        return retval;
    }

    /**
     * <p>
     * Waits for the end of the given future and returns the result. If any {@link InterruptedException} occurs, then it
     * is wrapped in a {@link IllegalArgumentException} which is unchecked. If any {@link ExecutionException} occurs, then
     * it is also wrapped to a {@link IllegalArgumentException} except if its cause IS-A {@link WuicException}. In that
     * case, the cause is just re-thrown.
     * </p>
     *
     * @param future the future to wait for
     * @return the result of future
     * @throws WuicException if the cause of an {@link ExecutionException} is a {@link WuicException}
     */
    private Map<String, ConvertibleNut> waitAndGet(final Future<Map<String, ConvertibleNut>> future) throws WuicException {
        try {
            return future.get();
        } catch (InterruptedException ie) {
            WuicException.throwBadArgumentException(new IllegalArgumentException(new IllegalArgumentException(ie)));
        } catch (ExecutionException ee) {
            if (ee.getCause() instanceof WuicException) {
                throw (WuicException) ee.getCause();
            } else {
                WuicException.throwBadArgumentException(new IllegalArgumentException(new IllegalArgumentException(ee)));
            }
        }

        return null;
    }

    /**
     * <p>
     * Puts the given list of nuts associated to the specified request to the cache.
     * </p>
     *
     * @param request the request key
     * @param nuts the nuts
     */
    public abstract void putToCache(EngineRequest.Key request, CacheResult nuts);

    /**
     * <p>
     * Removes the given list of nuts associated to the specified request from the cache.
     * </p>
     *
     * @param request request key
     */
    public abstract void removeFromCache(EngineRequest.Key request);

    /**
     * <p>
     * Gets the list of nuts associated to the specified request from the cache.
     * </p>
     *
     * @param request the request key
     * @return the list of nuts
     */
    public abstract CacheResult getFromCache(final EngineRequest.Key request);

    /**
     * <p>
     * Internal class that invalidates a cache entry identified with a workflow ID when it's notified that a nut has been
     * updated in an associated heap.
     * </p>
     *
     * @author Guillaume DROUET
     * @version 1.0
     * @since 0.4.0
     */
    private final class InvalidateCache implements HeapListener {

        /**
         * The request key as cache key.
         */
        private EngineRequest.Key key;

        /**
         * <p>
         * Builds a new instance.
         * </p>
         *
         * @param k the request key
         */
        private InvalidateCache(final EngineRequest.Key k) {
            key = k;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void nutUpdated(final NutsHeap heap) {
            removeFromCache(key);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean equals(final Object obj) {
            return obj instanceof InvalidateCache && key.equals(InvalidateCache.class.cast(obj).key);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int hashCode() {
            return key.hashCode();
        }
    }

    /**
     * <p>
     * This callable put the best effort result into the cache.
     * </p>
     *
     * @author Guillaume DROUET
     * @version 1.0
     * @since 0.4.4
     */
    private final class ParseBestEffortCall implements Callable<Map<String, ConvertibleNut>> {

        /**
         * The request to parse.
         */
        private EngineRequest request;

        /**
         * The best effort result.
         */
        private Map<String, ConvertibleNut> bestEffortResult;

        /**
         * <p>
         * Builds a new instance.
         * </p>
         *
         * @param er the engine request
         * @param ber the best effort result
         */
        private ParseBestEffortCall(final EngineRequest er, final Map<String, ConvertibleNut> ber) {
            request = er;
            bestEffortResult = ber;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Map<String, ConvertibleNut> call() throws WuicException {
            try {
                final Map<String, ConvertibleNut> toCache = new LinkedHashMap<String, ConvertibleNut>(bestEffortResult.size());
                final List<ConvertibleNut> nuts = new ArrayList<ConvertibleNut>(bestEffortResult.values());

                for (final ConvertibleNut nut : nuts) {
                    final ConvertibleNut byteArray = ByteArrayNut.toByteArrayNut(nut);
                    toCache.put(byteArray.getName(), byteArray);

                    if (byteArray.getReferencedNuts() != null) {
                        for (final ConvertibleNut ref : byteArray.getReferencedNuts()) {
                            toCache.put(ref.getName(), ref);
                        }
                    }
                }

                log.debug("Caching nut with key '{}'", request);
                putToCache(request.getKey(), new CacheResult(toCache, null));

                // Now let's parse the default result asynchronously
                final Future<Map<String, ConvertibleNut>> future = WuicScheduledThreadPool.getInstance().executeAsap(new ParseDefaultCall(request));

                synchronized (parsingDefault) {
                    parsingDefault.put(request.getKey(), future);
                }

                return toCache;
            } catch (IOException ioe) {
                WuicException.throwWuicException(ioe);
                return null;
            } finally {
                // Finished parsing
                synchronized (parsingBestEffort) {
                    parsingBestEffort.remove(request.getKey());
                }
            }
        }
    }

    /**
     * <p>
     * This callable parses some nuts and add it to the cache.
     * </p>
     *
     * @author Guillaume DROUET
     * @version 1.0
     * @since 0.4.4
     */
    private final class ParseDefaultCall implements Callable<Map<String, ConvertibleNut>> {

        /**
         * The request to parse.
         */
        private EngineRequest request;

        /**
         * <p>
         * Builds a new instance.
         * </p>
         *
         * @param er the engine request
         */
        private ParseDefaultCall(final EngineRequest er) {
            request = er;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Map<String, ConvertibleNut> call() throws WuicException {
            try {
                final List<ConvertibleNut> nuts = runChains(new EngineRequestBuilder(request).disableBestEffort().build());
                final Map<String, ConvertibleNut> toCache = new LinkedHashMap<String, ConvertibleNut>(nuts.size());

                for (final ConvertibleNut nut : nuts) {
                    toCache.put(nut.getName(), ByteArrayNut.toByteArrayNut(nut));
                }

                CacheResult cached = getFromCache(request.getKey());

                // Not in best effort mode
                if (cached == null) {
                    cached = new CacheResult(null, toCache);
                } else {
                    // Add the default result to the cache
                    cached.setDefaultResult(toCache);
                }

                // Update cache
                log.debug("Caching nuts with key '{}'", request);
                putToCache(request.getKey(), cached);

                return toCache;
            } catch (IOException ioe) {
                WuicException.throwWuicException(ioe);
                return null;
            } finally {
                // Finished parsing
                synchronized (parsingDefault) {
                    parsingDefault.remove(request.getKey());
                }
            }
        }
    }

    /**
     * <p>
     * Represents the value to be cached. The class wraps two maps: one for best effort process result and one for
     * classic and full process result.
     * </p>
     *
     * @author Guillaume DROUET
     * @version 1.0
     * @since 0.4.4
     */
    public static class CacheResult implements Serializable {

        /**
         * The best effort result.
         */
        private Map<String, ConvertibleNut> bestEffortResult;

        /**
         * The default result.
         */
        private Map<String, ConvertibleNut> defaultResult;

        /**
         * <p>
         * Builds a new instance.
         * </p>
         *
         * @param bestEffortResult the best effort map
         * @param defaultResult the default map
         */
        public CacheResult(final Map<String, ConvertibleNut> bestEffortResult, final Map<String, ConvertibleNut> defaultResult) {
            this.bestEffortResult = bestEffortResult;
            this.defaultResult = defaultResult;
        }

        /**
         * <p>
         * Gets the default result.
         * </p>
         *
         * @return the default result
         */
        public Map<String, ConvertibleNut> getDefaultResult() {
            return defaultResult;
        }

        /**
         * <p>
         * Gets the best effort result.
         * </p>
         *
         * @return the best effort result
         */
        public Map<String, ConvertibleNut> getBestEffortResult() {
            return bestEffortResult;
        }

        /**
         * <p>
         * Sets the default result
         * </p>
         *
         * @param defaultResult the default result
         */
        public void setDefaultResult(final Map<String, ConvertibleNut> defaultResult) {
            this.defaultResult = defaultResult;
        }
    }
}
