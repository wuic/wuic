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


package com.github.wuic.engine.impl.ehcache;

import com.github.wuic.NutType;
import com.github.wuic.engine.EngineType;
import com.github.wuic.exception.WuicException;
import com.github.wuic.nut.core.ByteArrayNut;
import com.github.wuic.nut.Nut;
import com.github.wuic.engine.Engine;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.github.wuic.engine.EngineRequest;
import com.github.wuic.util.IOUtils;
import com.github.wuic.util.NumberUtils;
import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * This {@link Engine engine} reads from a doCache specified in the WUIC XML path
 * the given set of elements. If they exists, then they are returned and no more
 * engine is executed. Otherwise, the chain is executed and the result is put in
 * doCache.
 * </p>
 * 
 * @author Guillaume DROUET
 * @version 1.7
 * @since 0.1.1
 */
public class EhCacheEngine extends Engine {

    /**
     * Logger.
     */
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    /**
     * If doCache or not.
     */
    private Boolean doCache;

    /**
     * The wrapped cache.
     */
    private Cache ehCache;

    /**
     * <p>
     * Builds a new engine.
     * </p>
     *
     * @param work if cache should be activated or not
     * @param cache the cache to be wrapped
     */
    public EhCacheEngine(final Boolean work, final Cache cache) {
        doCache = work;
        ehCache = cache;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked")
    public List<Nut> parse(final EngineRequest request) throws WuicException {
        // Log duration
        final Long start = System.currentTimeMillis();
        List<Nut> retval = null;

        if (works()) {
            final String key = request.getGroup().getId();
            final Element value = ehCache.get(key);

            // Resources exist in doCache, returns them
            if (value != null) {
                log.info("Resources for group '{}' found in doCache", key);
                retval = (List<Nut>) value.getObjectValue();
            } else if (getNext() != null) {
                final List<Nut> resources = getNext().parse(request);
                final List<Nut> toCache = new ArrayList<Nut>(resources.size());

                for (Nut resource : resources) {
                    if (resource.isCacheable()) {
                        toCache.add(toByteArrayResource(resource));
                    }
                }

                log.debug("Caching nut with {}", key);

                ehCache.put(new Element(key, toCache));

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
     * Converts the given nut and its referenced resources into resources wrapping an in memory byte array.
     * </p>
     *
     * @param resource the nut to convert
     * @return the byte array nut
     * @throws WuicException if an I/O error occurs
     */
    private Nut toByteArrayResource(final Nut resource) throws WuicException {
        InputStream is = null;

        try {
            is = resource.openStream();
            final ByteArrayOutputStream os = new ByteArrayOutputStream();
            IOUtils.copyStream(is, os);
            final Nut bytes = new ByteArrayNut(os.toByteArray(), resource.getName(), resource.getNutType());

            if (resource.getReferencedNuts() != null) {
                for (Nut ref : resource.getReferencedNuts()) {
                    bytes.addReferencedResource(toByteArrayResource(ref));
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
}
