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


package com.github.wuic.factory.impl;

import com.github.wuic.FileType;
import com.github.wuic.exception.wrapper.BadArgumentException;
import com.github.wuic.exception.xml.WuicXmlReadException;
import com.github.wuic.configuration.Configuration;
import com.github.wuic.engine.Engine;
import com.github.wuic.engine.impl.ehcache.EhCacheEngine;
import com.github.wuic.factory.EngineFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * <p>
 * This {@link EngineFactory} implementation produces a chain of responsibility
 * composed of engines produced with the {@link AggregationEngineFactory}, a
 * {@link EhCacheEngine} and a compression engine which is chosen according to
 * a given {@link FileType}. 
 * </p>
 * 
 * @author Guillaume DROUET
 * @version 1.2
 * @since 0.1.0
 */
public class EngineFactoryImpl implements EngineFactory {

    /**
     * The configuration to use for each supported {@link FileType}.
     */
    private Map<FileType, Configuration> configurationForFileType;

    /**
     * <p>
     * Builds a new {@link EngineFactoryImpl}.
     * </p>
     */
    public EngineFactoryImpl() {
        configurationForFileType = new HashMap<FileType, Configuration>();
    }
    
    /**
     * <p>
     * Adds a configuration for a defined {@link FileType}.
     * </p>
     * 
     * @param fileType the path type
     * @param conf the configuration
     */
    public void addConfigurationForFileType(final FileType fileType, final Configuration conf) {
        configurationForFileType.put(fileType, conf);
    }
    
    /**
     * {@inheritDoc}
     */
    public Engine create(final FileType fileType) throws WuicXmlReadException {
        
        if (!configurationForFileType.containsKey(fileType)) {
            throw new BadArgumentException(new IllegalArgumentException(String.format("%s has no supported configuration associated", fileType)));
        }
        
        // Chose the factory according to the type
        final Configuration config = configurationForFileType.get(fileType);
        final Engine compressor = new CompressionEngineFactory(config).create(fileType);

        // Cache engine
        final Engine cacheEngine = new EhCacheEngine(config);
        
        // Reuse configuration for the aggregation
        final Engine aggregator = new AggregationEngineFactory(config).create(fileType);
        
        /*
         * Create and return the chain of responsibility engine
         * 
         * Important : we compress first, aggregate next. Aggregation could cause
         * error while compressing javascript. Moreover, It is more safe to
         * load in memory files not too big when compressing.
         * 
         *  Moreover, cache engine is obviously the first one to be executed
         */
        cacheEngine.setNext(compressor);
        compressor.setNext(aggregator);
        
        return cacheEngine;
    }
}
