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


package com.github.wuic.configuration.impl;

import com.github.wuic.FileType;
import com.github.wuic.resource.WuicResource;
import com.github.wuic.configuration.Configuration;
import com.github.wuic.configuration.ImageConfiguration;
import com.github.wuic.engine.DimensionPacker;
import com.github.wuic.engine.impl.embedded.CGBinPacker;

import net.sf.ehcache.Cache;

/**
 * <p>
 * Implementation of the {@link ImageConfiguration} interface.
 * </p>
 * 
 * @author Guillaume DROUET
 * @version 1.2
 * @since 0.2.0
 */
public class ImageConfigurationImpl extends ConfigurationImpl implements ImageConfiguration {
    
    /**
     * <p>
     * Builds a new instance.
     * </p>
     * 
     * @param configId the configuration id
     * @param doCache cache or not the files
     * @param compress compress or not the files
     * @param aggregate aggregate or not the files
     * @param cs the char set of the files
     * @param ehCache the cache
     */
    public ImageConfigurationImpl(final String configId,
            final Boolean doCache,
            final Boolean compress,
            final Boolean aggregate,
            final String cs,
            final Cache ehCache) {
        super(configId, doCache, compress, aggregate, cs, ehCache);
    }
    
    /**
     * <p>
     * Builds a new instance based on another {@link Configuration configuration}.
     * </p>
     * 
     * @param other the other {@link Configuration}
     */
    public ImageConfigurationImpl(final Configuration other) {
        this(other.getId(), other.cache(), other.compress(), other.aggregate(), other.charset(), other.getCache());
    }
    
    /**
     * {@inheritDoc}
     */
    public FileType getFileType() {
        return FileType.PNG;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DimensionPacker<WuicResource> createDimensionPacker() {
        
        // Should we provide the choice between different packing algorithms -
        return new CGBinPacker<WuicResource>();
    }
}
