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

import com.github.wuic.exception.WuicException;
import com.github.wuic.resource.impl.ByteArrayWuicResource;
import com.github.wuic.FileType;
import com.github.wuic.resource.WuicResource;
import com.github.wuic.configuration.Configuration;
import com.github.wuic.engine.Engine;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import com.github.wuic.engine.EngineRequest;
import com.github.wuic.util.IOUtils;

/**
 * <p>
 * This {@link Engine engine} can aggregate all the specified files in one file.
 * Files are aggregated in the order of apparition in the given list. Note that
 * nothing will be done is {@link Configuration#aggregate()} returns {@code false}
 * in the given {@link Configuration}.
 * </p>
 * 
 * @author Guillaume DROUET
 * @version 1.7
 * @since 0.1.0
 */
public class CGTextAggregatorEngine extends Engine {

    /**
     * The configuration to use.
     */
    private Configuration configuration;
    
    /**
     * <p>
     * Builds the engine.
     * </p>
     * 
     * @param config the {@link Configuration} to use
     */
    public CGTextAggregatorEngine(final Configuration config) {
        this.configuration = config;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public List<WuicResource> parse(final EngineRequest request)
            throws WuicException {

        // Do nothing if the configuration says that no aggregation should be done
        if (!works()) {
            return request.getResources();
        }
        
        // In memory buffer for the aggregated resources
        final String fileName = "aggregate" + configuration.getFileType().getExtensions()[0];
        final ByteArrayOutputStream target = new ByteArrayOutputStream();
        
        // Append each file
        InputStream is = null;
        FileType fileType = null;
        final byte[] buffer = new byte[com.github.wuic.util.IOUtils.WUIC_BUFFER_LEN];

        final List<WuicResource> retval = new ArrayList<WuicResource>();
        final List<WuicResource> referencedResources = new ArrayList<WuicResource>();

        // Aggregate each resource
        for (WuicResource resource : request.getResources()) {
            // Resource must be aggregatable
            if (resource.isAggregatable()) {
                try {
                    fileType = resource.getFileType();
                    is = resource.openStream();
                    IOUtils.copyStream(is, target);

                    // Begin content file writing on a new line when no compression is configured
                    if (!configuration.compress()) {
                        buffer[0] = '\n';
                        target.write(buffer, 0, 1);
                    }

                    if (resource.getReferencedResources() != null) {
                        referencedResources.addAll(resource.getReferencedResources());
                    }
                } finally {
                    IOUtils.close(is);
                }
            } else {
                retval.add(resource);
            }
        }

        // Create the a resource containing all the aggregated resources
        final WuicResource aggregate = new ByteArrayWuicResource(target.toByteArray(), fileName, fileType);

        // Eventually add some extracted referenced resources
        if (!referencedResources.isEmpty()) {
            for (WuicResource ref : referencedResources) {
                aggregate.addReferencedResource(ref);
            }
        }

        retval.add(aggregate);

        if (getNext() != null) {
            return getNext().parse(new EngineRequest(retval, request));
        } else {
            return retval;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Configuration getConfiguration() {
        return configuration;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Boolean works() {
        return configuration.aggregate();
    }
}
