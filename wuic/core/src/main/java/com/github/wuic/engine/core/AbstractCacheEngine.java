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

import com.github.wuic.Logging;
import com.github.wuic.engine.EngineRequest;
import com.github.wuic.engine.EngineRequestBuilder;
import com.github.wuic.engine.EngineType;
import com.github.wuic.engine.HeadEngine;
import com.github.wuic.exception.WuicException;
import com.github.wuic.nut.CompositeNut;
import com.github.wuic.nut.ConvertibleNut;
import com.github.wuic.nut.HeapListener;
import com.github.wuic.nut.InMemoryNut;
import com.github.wuic.nut.NutsHeap;
import com.github.wuic.nut.PrefixedNut;
import com.github.wuic.nut.SizableNut;
import com.github.wuic.nut.Source;
import com.github.wuic.util.NumberUtils;
import com.github.wuic.util.NutUtils;
import com.github.wuic.util.Pipe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
     * The default parsing done asynchronously.
     */
    private Map<EngineRequest.Key, Future<Map<String, ConvertibleNut>>> parsingDefault;

    /**
     * <p>
     * Initializes a new engine.
     * </p>
     *
     * @param work if cache should be activated or not
     * @param be apply best effort
     */
    public void init(final Boolean work, final Boolean be) {
        doCache = work;
        bestEffort = be;
        parsingDefault = new HashMap<EngineRequest.Key, Future<Map<String, ConvertibleNut>>>();
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
            final Map<String, CacheResult.Entry> entries = value.getDefaultResult() != null ? value.getDefaultResult() : value.getBestEffortResult();
            final Map<String, ConvertibleNut> nuts = toConvertibleNut(request, entries);
            retval = new ArrayList<ConvertibleNut>(nuts.size());

            for (final Map.Entry<String, ConvertibleNut> entry : nuts.entrySet()) {
                retval.add(entry.getValue());
            }
        } else {
            // Removes from cache when an update is detected
            request.getHeap().addObserver(new InvalidateCache(key));

            final Map<String, ConvertibleNut> toCache;

            // We are in best effort, do the minimal of operations and return the resulting nut
            if (bestEffort) {
                try {
                    retval = InMemoryNut.toByteArrayNut(runBestEffortChain(request));
                } catch (IOException ioe) {
                    WuicException.throwWuicException(ioe);
                    return null;
                }

                scheduleBestEffort(request, retval);
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
        return parse(request, path, 0);
    }

    /**
     * <p>
     * Executes the given request in best effort.
     * </p>
     *
     * @param request the request
     * @return the processed nut prefixed according to best effort mode
     * @throws WuicException if execution fails
     */
    private List<ConvertibleNut> runBestEffortChain(final EngineRequest request) throws WuicException {
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

        return prefixed;
    }

    /**
     * <p>
     * Schedules the best effort job.
     * </p>
     *
     * @param processResult the result
     * @throws WuicException if any exception occurs
     */
    private void scheduleBestEffort(final EngineRequest request, final List<ConvertibleNut> processResult)
            throws WuicException {
        final Map<String, ConvertibleNut> bestEffortResult = new LinkedHashMap<String, ConvertibleNut>(processResult.size());

        for (final ConvertibleNut nut : processResult) {
            bestEffortResult.put(nut.getName(), nut);
        }

        try {
            final Map<String, ConvertibleNut> retval = new LinkedHashMap<String, ConvertibleNut>(bestEffortResult.size());
            final List<ConvertibleNut> nuts = new ArrayList<ConvertibleNut>(bestEffortResult.values());
            final Map<String, CacheResult.Entry> toCache = prepareCacheableNuts(nuts, retval);

            log.debug("Caching nut with key '{}'", request);
            final CacheResult result = new CacheResult(toCache, null);
            putToCache(request.getKey(), result);

            // Now let's parse the default result
            final Future<Map<String, ConvertibleNut>> future = request.getProcessContext().executeAsap(new ParseDefaultCall(request));

            synchronized (parsingDefault) {
                parsingDefault.put(request.getKey(), future);
            }
        } catch (IOException ioe) {
            WuicException.throwWuicException(ioe);
        }
    }

    /**
     * <p>
     * Like {@link #parse(com.github.wuic.engine.EngineRequest, String)} with a parameter indicating a number of
     * recursive call.
     * </p>
     *
     * @param request the request
     * @param path the path
     * @param callee number of recursive call
     * @return the nut
     * @throws WuicException if an error occurs
     */
    private ConvertibleNut parse(final EngineRequest request, final String path, final int callee) throws WuicException {
        // Log duration
        final Long start = System.currentTimeMillis();
        ConvertibleNut retval;

        // Apply cache support
        if (works()) {
            // Retrieving the result form the cache first
            final EngineRequest.Key key = request.getKey();
            final Future<Map<String, ConvertibleNut>> future;

            // Indicates if we are looking for a nut from best effort process or not
            final Boolean isBestEffort = path.startsWith("best-effort");

            if (isBestEffort) {
                CacheResult result = getFromCache(key);

                if (result == null) {
                    if (callee > 0) {
                        parse(request);
                    } else {
                        parse(request, path, callee + 1);
                    }

                    result = getFromCache(key);
                }

                retval = result.find(request, path, true);
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
                    retval = parse(request, path);
                } else {
                    retval = result.find(request, path, false);
                }
            }
        // we don't cache so just call the next engine if exists
        } else {
            retval = runAndFind(request, path);
        }

        Logging.TIMER.log("'{}' retrieved from cache engine in {} ms", path, (System.currentTimeMillis() - start));

        return retval;
    }

    /**
     * <p>
     * Run chains for the given request and just after that find the nut corresponding to the given path in the result.
     * </p>
     *
     * @param request the request
     * @param path the nut name
     * @return the nut, {@code null} if not found
     * @throws WuicException if request can't be processed
     */
    private ConvertibleNut runAndFind(final EngineRequest request, final String path) throws WuicException {
        ConvertibleNut retval = null;
        final List<ConvertibleNut> list = runChains(request);

        for (final ConvertibleNut nut : list) {
            if (nut.getName().equals(path)) {
                retval = nut;
                break;
            }
        }

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
     * Reads the given nuts, converts them to {@link Serializable} objects and returns them in a map to add in the cache.
     * The given map is also populated with concrete nut when they are prepared to be cached.
     * </p>
     *
     * @param nuts the nut to serialize
     * @param processed the map where serialized and static nuts will be stored
     * @return the map of processed nuts
     * @throws IOException if an I/O error occurs
     */
    private Map<String, CacheResult.Entry> prepareCacheableNuts(final List<ConvertibleNut> nuts,
                                                                final Map<String, ConvertibleNut> processed)
            throws IOException {
        final Map<String, CacheResult.Entry> retval = new LinkedHashMap<String, CacheResult.Entry>();

        for (final ConvertibleNut nut : nuts) {
            final List<ConvertibleNut> referenced;
            final ConvertibleNut byteArray = InMemoryNut.toByteArrayNut(nut);
            referenced = byteArray.getReferencedNuts();

            // Nut content is static, cache it entirely
            // otherwise transformation will be applied each time the cache entry is retrieved
            if (!nut.isDynamic()) {
                retval.put(nut.getName(), new CacheResult.Entry(byteArray));
            } else {
                final Set<Pipe.Transformer<ConvertibleNut>> transformers;

                // Try to retrieve the transformers from the composition
                if ((nut.getTransformers() == null || nut.getTransformers().isEmpty()) && (nut instanceof CompositeNut)) {
                    transformers = CompositeNut.class.cast(nut).getCompositionList().get(0).getTransformers();
                } else {
                    transformers = nut.getTransformers();
                }

                retval.put(nut.getName(), new CacheResult.Entry(referenced, nut.getName(), transformers));
            }

            if (processed != null) {
                processed.put(nut.getName(), byteArray);
            }
        }

        return retval;
    }

    /**
     * <p>
     * Converts the given entries to {@link ConvertibleNut convertible} nut.
     * </p>
     *
     * @param request the request
     * @param entries the entries
     * @return the converted entries
     */
    private Map<String, ConvertibleNut> toConvertibleNut(final EngineRequest request, final Map<String, CacheResult.Entry> entries) {
        final Map<String, ConvertibleNut> retval = new LinkedHashMap<String, ConvertibleNut>(entries.size());

        for (final Map.Entry<String, CacheResult.Entry> entry : entries.entrySet()) {
            retval.put(entry.getKey(), entry.getValue().toConvertibleNut(request));
        }

        return retval;
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
     * This callable parses some nuts and add it to the cache.
     * </p>
     *
     * @author Guillaume DROUET
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
                final Map<String, ConvertibleNut> retval = new LinkedHashMap<String, ConvertibleNut>(nuts.size());
                final Map<String, CacheResult.Entry> toCache = prepareCacheableNuts(nuts, retval);
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

                return retval;
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
     * @since 0.4.4
     */
    public static class CacheResult implements Serializable {

        /**
         * <p>
         * An entry wraps dynamic and static nuts for a cache result.
         * </p>
         *
         * @author Guillaume DROUET
         * @since 0.5.2
         */
        public static class Entry implements Serializable {

            /**
             * The logger.
             */
            private Logger log = LoggerFactory.getLogger(getClass());

            /**
             * Dynamic nut name if the entry represents a dynamic nut.
             */
            private final String dynamicName;

            /**
             * All referenced nuts in a collection if the entry represents a dynamic nut.
             */
            private List<ConvertibleNut> dynamicReferencedNuts;

            /**
             * Nut to entirely cache when the entry represents a static nut.
             */
            private final ConvertibleNut staticNut;

            /**
             * Nut transformers if the entry represents a dynamic nut.
             */
            private Set<Pipe.Transformer<ConvertibleNut>> dynamicTransformers;

            /**
             * <p>
             * Builds an entry wrapping a dynamic nut.
             * </p>
             *
             * @param dynamicReferencedNuts nuts to reference
             * @param dynamicName the name
             * @param dynamicTransformers the transformers
             */
            public Entry(final List<ConvertibleNut> dynamicReferencedNuts,
                         final String dynamicName,
                         final Set<Pipe.Transformer<ConvertibleNut>> dynamicTransformers) {
                this.dynamicReferencedNuts = dynamicReferencedNuts;
                this.dynamicName = dynamicName;
                this.staticNut = null;

                if (dynamicTransformers != null) {
                    this.dynamicTransformers = new LinkedHashSet<Pipe.Transformer<ConvertibleNut>>();

                    for (final Pipe.Transformer<ConvertibleNut> t : dynamicTransformers) {
                        if (t instanceof Serializable) {
                            this.dynamicTransformers.add(t);
                        } else {
                            log.info("{} is not a {} and can't be cached for future dynamic transformation.",
                                    t.toString(),
                                    Serializable.class.getName());
                        }
                    }
                }
            }

            /**
             * <p>
             * Builds an entry wrapping a static nut.
             * </p>
             *
             * @param staticNut the static nut
             */
            public Entry(final ConvertibleNut staticNut) {
                this.dynamicReferencedNuts = null;
                this.dynamicName = null;
                this.dynamicTransformers = null;
                this.staticNut = staticNut;
            }

            /**
             * <p>
             * Returns the sum of sizes collected from all contained nuts content.
             * If nuts are not a {@link SizableNut}, their size will be ignored.
             * </p>
             *
             * @return the global size of this entry
             */
            long size() {
                long size = 0L;

                if (staticNut != null) {
                    size += computeSize(staticNut);
                }

                if (dynamicReferencedNuts != null) {
                    size += computeSize(dynamicReferencedNuts);
                }

                return size;
            }

            /**
             * <p>
             * Returns the sum of sizes collected from nut contents contained in the given list.
             * If nuts are not a {@link SizableNut}, their size will be ignored.
             * </p>
             *
             * @param nuts the nuts
             * @return the global size of the list
             */
            private long computeSize(final List<ConvertibleNut> nuts) {
                long retval = 0;

                for (final ConvertibleNut nut : nuts) {
                    retval += computeSize(nut);
                }

                return retval;
            }

            /**
             * <p>
             * Returns the size of the given nut's content. Referenced and original nuts are also taken into consideration.
             * If nuts are not a {@link SizableNut}, their size will be ignored.
             * </p>
             *
             * @param nut the nut
             * @return the content size
             */
            private long computeSize(final ConvertibleNut nut) {
                if (nut instanceof SizableNut) {
                    long retval = SizableNut.class.cast(nut).size() + computeSize(nut.getSource().getOriginalNuts());

                    if (nut.getReferencedNuts() != null) {
                        retval += computeSize(nut.getReferencedNuts());
                    }

                    return retval;
                } else {
                    log.warn("{} is supposed to be a {} since it's stored in the cache but it's not the case. Ignoring nut size...",
                            nut.toString(), SizableNut.class.getName());
                    return 0;
                }
            }

            /**
             * <p>
             * Finds the nut with the given name if wrapped in this entry.
             * </p>
             *
             * @param request the request that requires the nut
             * @param name the nut name
             * @return the nut, {@code null} if not found
             */
            ConvertibleNut find(final EngineRequest request, final String name) {
                List<ConvertibleNut> referenced;
                Source source;

                if (staticNut != null) {
                    if (name.equals(staticNut.getName())) {
                        return staticNut;
                    } else {
                        referenced = staticNut.getReferencedNuts();
                        source = staticNut.getSource();
                    }
                } else if (name.equals(dynamicName)) {
                    return toConvertibleNut(request);
                } else {
                    referenced = dynamicReferencedNuts;
                    source = null;
                }

                // Look for nut inside referenced nuts
                if (referenced != null) {
                    final ConvertibleNut ref = NutUtils.findByName(referenced, name);

                    if (ref != null) {
                        return ref;
                    }
                }

                // Look for nut inside sources
                if (source instanceof ConvertibleNut) {
                    // Source map
                    final ConvertibleNut src = NutUtils.findByName(ConvertibleNut.class.cast(source), name);

                    if (src != null) {
                        return src;
                    } else {
                        return NutUtils.findByName(source.getOriginalNuts(), name);
                    }
                }

                return null;
            }

            /**
             * <p>
             * Converts this entry to a {@link ConvertibleNut}.
             * </p>
             *
             * <p>
             * If the entry represents a dynamic nut, the method simulates a parsing from the given request by
             * reusing the wrapped transformers.
             * </p>
             *
             * @param request the request
             * @return the converted nut
             */
            ConvertibleNut toConvertibleNut(final EngineRequest request) {
                ConvertibleNut retval = staticNut;

                if (staticNut == null) {
                    for (final ConvertibleNut nut : request.getNuts()) {
                        if (nut.isDynamic() && nut.getInitialName().equals(dynamicName)) {
                            retval = nut;
                            break;
                        }
                    }

                    if (retval == null) {
                        WuicException.throwBadStateException(
                                new IllegalStateException(
                                        String.format("Cached dynamic nut %s for workflow %s does not exists in request.",
                                                dynamicName, request.getWorkflowId())));
                    } else {
                        if (dynamicTransformers == null) {
                            log.warn("Nut {} in workflow {} is dynamic but cached entry does not contains it transformers.",
                                    dynamicName, request.getWorkflowId());
                        } else {
                            for (final Pipe.Transformer<ConvertibleNut> t : dynamicTransformers) {
                                retval.addTransformer(t);
                            }
                        }

                        if (dynamicReferencedNuts != null) {
                            for (final ConvertibleNut ref : dynamicReferencedNuts) {
                                retval.addReferencedNut(ref);
                            }
                        }
                    }
                }

                return retval;
            }
        }

        /**
         * The best effort result.
         */
        private Map<String, Entry> bestEffortResult;

        /**
         * The default result.
         */
        private Map<String, Entry> defaultResult;

        /**
         * <p>
         * Builds a new instance.
         * </p>
         *
         * @param bestEffortResult the best effort
         * @param defaultResult the default
         */
        public CacheResult(final Map<String, Entry> bestEffortResult,
                           final Map<String, Entry> defaultResult) {
            this.bestEffortResult = bestEffortResult;
            this.defaultResult = defaultResult;
        }

        /**
         * <p>
         * Finds the entry that wraps the nut with the given name and return it.
         * </p>
         *
         * @param request the request
         * @param bestEffort find in the best effort result
         * @param name the nut name
         * @return the found nut, {@code null} if no nut has been found
         */
        ConvertibleNut find(final EngineRequest request, final String name, final boolean bestEffort) {
            final Map<String, Entry> map = !bestEffort ? defaultResult : bestEffortResult;

            if (map == null) {
                return null;
            }

            for (final Map.Entry<String, Entry> entry : map.entrySet()) {
                final ConvertibleNut retval = entry.getValue().find(request, name);

                if (retval != null) {
                    return retval;
                }
            }

            return null;
        }

        /**
         * <p>
         * Gets the default result.
         * </p>
         *
         * @return the default result
         */
        public Map<String, Entry> getDefaultResult() {
            return defaultResult;
        }

        /**
         * <p>
         * Gets the best effort result.
         * </p>
         *
         * @return the best effort result
         */
        public Map<String, Entry> getBestEffortResult() {
            return bestEffortResult;
        }

        /**
         * <p>
         * Sets the default result.
         * </p>
         *
         * @param defaultResult the default result
         */
        public void setDefaultResult(final Map<String, Entry> defaultResult) {
            this.defaultResult = defaultResult;
        }
    }
}
