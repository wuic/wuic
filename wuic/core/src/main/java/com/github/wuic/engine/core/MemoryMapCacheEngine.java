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

import com.github.wuic.ApplicationConfig;
import com.github.wuic.config.Alias;
import com.github.wuic.config.BooleanConfigParam;
import com.github.wuic.config.Config;
import com.github.wuic.config.IntegerConfigParam;
import com.github.wuic.config.StringConfigParam;
import com.github.wuic.engine.EngineRequest;
import com.github.wuic.engine.EngineService;
import com.github.wuic.exception.WuicException;
import com.github.wuic.util.IOUtils;
import com.github.wuic.util.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Map;
import java.util.HashMap;

/**
 * <p>
 * This {@link com.github.wuic.engine.Engine engine} reads from a {@link java.util.Map} kept in memory the nuts
 * associated to a workflow to be processed. A maximum amount of memory to be used by the cache can be specified.
 * If the limit exceed, the cache writes additional data to the disk. The size of the memory is only computed from the
 * content of nuts that are stored. Size of objects themselves is not included, which can lead to a map using a little
 * bit more memory than the configured limit. This should be taken into consideration when configuring the limit.
 * </p>
 *
 * @author Guillaume DROUET
 * @since 0.4.0
 */
@EngineService(injectDefaultToWorkflow = true, isCoreEngine = true)
@Alias("defaultCache")
public class MemoryMapCacheEngine extends ScheduledCacheEngine {

    /**
     * Exception message when {@link ApplicationConfig#MAX_SIZE_IN_MEMORY} setting is not correct.
     */
    private static final String MAX_MEMORY_PARAM_ERROR_MSG = String.format(
            "%s must be a numeric value optionally suffixed by MB or KB.", ApplicationConfig.MAX_SIZE_IN_MEMORY);

    /**
     * The logger.
     */
    private final Logger log = LoggerFactory.getLogger(getClass());

    /**
     * Memory map.
     */
    private Map<EngineRequest.Key, CacheEntry> cache;

    /**
     * Memory size limit.
     */
    private long limit;

    /**
     * Memory currently used.
     */
    private long used;

    /**
     * <p>
     * Initializes a new engine.
     * </p>
     *
     * @param work if cache should be activated or not
     * @param timeToLiveSeconds the time this cache could live
     * @param bestEffort enable best effort mode or not
     * @param maxMemorySize maximum amount of memory used
     */
    @Config
    public void init(
            @BooleanConfigParam(defaultValue = true, propertyKey = ApplicationConfig.CACHE) final Boolean work,
            @IntegerConfigParam(defaultValue = -1, propertyKey = ApplicationConfig.TIME_TO_LIVE) final int timeToLiveSeconds,
            @BooleanConfigParam(defaultValue = false, propertyKey = ApplicationConfig.BEST_EFFORT) final Boolean bestEffort,
            @StringConfigParam(defaultValue = "10MB", propertyKey = ApplicationConfig.MAX_SIZE_IN_MEMORY) final String maxMemorySize) {
        super.init(timeToLiveSeconds, work, bestEffort);
        cache = new HashMap<EngineRequest.Key, CacheEntry>();

        // Read the limit: default value is byte but KB and MB are supported
        final String end = maxMemorySize.length() > 1 ? maxMemorySize.substring(maxMemorySize.length() - NumberUtils.TWO) : null;

        if ("KB".equalsIgnoreCase(end)) {
            limit = extractSize(maxMemorySize) * NumberUtils.ONE_KB;
        } else if ("MB".equals(end)) {
            limit = extractSize(maxMemorySize) * NumberUtils.ONE_KB * NumberUtils.ONE_KB;
        } else if (NumberUtils.isNumber(maxMemorySize)) {
            limit = Long.parseLong(maxMemorySize);
        } else {
            WuicException.throwBadArgumentException(new IllegalArgumentException(MAX_MEMORY_PARAM_ERROR_MSG));
        }

        // Clear the cache and clean the disk when application stops
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                clearCache();
            }
        }));
    }

    /**
     * <p>
     * Extracts the size from the given value, ignoring the two last characters as they are supposed to indicate the unit.
     * </p>
     *
     * @param value the value
     * @return the extracted size
     */
    private long extractSize(final String value) {
        if (value.length() <= NumberUtils.TWO) {
            WuicException.throwBadArgumentException(new IllegalArgumentException(MAX_MEMORY_PARAM_ERROR_MSG));
        }

        final String sub = value.substring(0, value.length() - NumberUtils.TWO);

        if (!NumberUtils.isNumber(sub)) {
            WuicException.throwBadArgumentException(new IllegalArgumentException(MAX_MEMORY_PARAM_ERROR_MSG));
        }

        return Long.parseLong(sub);
    }

    /**
     * <p>
     * Gets the amount of memory used.
     * </p>
     *
     * @return the number of bytes
     */
    public long getMemoryInUse() {
        return used;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void putToCache(final EngineRequest.Key request, final CacheResult nuts) {
        final long size = computeSize(nuts);

        if (size + used > limit) {
            log.info("Cache memory limit exceeded, writing object to the disk.");
            cache.put(request, new DiskCacheEntry(nuts, size));
        } else {
            cache.put(request, new MemoryCacheEntry(nuts, size));
            used += size;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeFromCache(final EngineRequest.Key request) {
        final CacheEntry cacheEntry = cache.remove(request);

        if (cacheEntry != null) {
            if (cacheEntry instanceof DiskCacheEntry) {
                DiskCacheEntry.class.cast(cacheEntry).clean();
            } else {
                used -= cacheEntry.getSize();
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CacheResult getFromCache(final EngineRequest.Key request) {
        final CacheEntry entry = cache.get(request);
        return entry != null ? entry.getCacheResult() : null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clearCache() {
        for (final Object key : cache.keySet().toArray()) {
            removeFromCache(EngineRequest.Key.class.cast(key));
        }
    }

    /**
     * <p>
     * Computes the size of nuts wrapped by the given entry.
     * </p>
     *
     * @param nuts the nuts
     * @return the size
     */
    private static long computeSize(final CacheResult nuts) {
        long retval = 0L;

        if (nuts.getBestEffortResult() != null) {
            for (final CacheResult.Entry entry : nuts.getBestEffortResult().values()) {
                retval += entry.size();
            }
        }

        if (nuts.getDefaultResult() != null) {
            for (final CacheResult.Entry entry : nuts.getDefaultResult().values()) {
                retval += entry.size();
            }
        }

        return retval;
    }

    /**
     * <p>
     * A cache entry wraps a {@link CacheResult} and stores its associated size.
     * </p>
     *
     * @author Guillaume DROUET
     * @since 0.5.3
     */
    private abstract static class CacheEntry {

        /**
         * Size.
         */
        private long size;

        /**
         * <p>
         * Builds a new instance.
         * </p>
         *
         * @param size the size
         */
        protected CacheEntry(final long size) {
            this.size = size;
        }

        /**
         * <p>
         * Gets the size
         * </p>
         *
         * @return the size
         */
        long getSize() {
            return size;
        }

        /**
         * <p>
         * Gets the wrapped {@link CacheResult}.
         * </p>
         *
         * @return the result
         */
        abstract CacheResult getCacheResult();
    }

    /**
     * <p>
     * This entry keeps the {@link CacheResult} in an attribute.
     * </p>
     *
     * @author Guillaume DROUET
     * @since 0.5.3
     */
    private static class MemoryCacheEntry extends CacheEntry {

        /**
         * The wrapped result.
         */
        private CacheResult cacheResult;

        /**
         * <p>
         * Builds a new instance.
         * </p>
         *
         * @param cacheResult the result to retain in memory
         * @param size the size of associated cache result
         */
        private MemoryCacheEntry(final CacheResult cacheResult, final long size) {
            super(size);
            this.cacheResult = cacheResult;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        CacheResult getCacheResult() {
            return cacheResult;
        }
    }

    /**
     * <p>
     * This entry keeps the {@link CacheResult} on the disk.
     * </p>
     *
     * @author Guillaume DROUET
     * @since 0.5.3
     */
    private class DiskCacheEntry extends CacheEntry {

        /**
         * The file where result is stored.
         */
        private File file;

        /**
         * <p>
         * Builds a new instance.
         * </p>
         *
         * @param cacheResult the result to retain on the disk
         * @param size the size of associated cache result
         */
        private DiskCacheEntry(final CacheResult cacheResult, final long size) {
            super(size);
            ObjectOutputStream oos = null;

            try {
                // Store on temporary directory
                file = File.createTempFile("wuic-default-cache", ".cache");
                oos = new ObjectOutputStream(new FileOutputStream(file));
                oos.writeObject(cacheResult);
            } catch (IOException ioe) {
                WuicException.throwBadStateException(ioe);
            } finally {
                IOUtils.close(oos);
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        CacheResult getCacheResult() {
            ObjectInputStream ois = null;

            try {
                ois = new ObjectInputStream(new FileInputStream(file));
                return CacheResult.class.cast(ois.readObject());
            } catch (ClassNotFoundException cnfe) {
                WuicException.throwBadStateException(cnfe);
                return null;
            } catch (IOException ioe) {
                WuicException.throwBadStateException(ioe);
                return null;
            } finally {
                IOUtils.close(ois);
            }
        }

        /**
         * <p>
         * Clean this entry by deleting the file.
         * </p>
         */
        void clean() {
            if (!file.delete()) {
                log.warn("Unable to delete file {}", file.toString());
            }
        }
    }
}
