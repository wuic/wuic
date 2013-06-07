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

import com.github.wuic.resource.impl.ByteArrayWuicResource;
import com.github.wuic.FileType;
import com.github.wuic.resource.WuicResource;
import com.github.wuic.engine.Engine;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import com.github.wuic.engine.EngineRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * This {@link Engine engine} defines the base treatments to execute when
 * compressing a set of files. It could be extended by different sub-classes
 * which provide compression support.
 * </p>
 * 
 * <p>
 * The compression is never performed if the {@link com.github.wuic.configuration.Configuration#compress()}
 * method returns {@code false}.
 * </p>
 * 
 * @author Guillaume DROUET
 * @version 1.3
 * @since 0.1.0
 */
public abstract class CGAbstractCompressorEngine extends Engine {
 
    /**
     * Logger.
     */
    private final Logger log = LoggerFactory.getLogger(this.getClass());
    
    /**
     * <p>
     * Compress a stream (the source) into a target. The target is overridden if
     * it already exists.
     * </p>
     * 
     * @param source the source
     * @param target the file where to compressed content should be written
     * @throws IOException if an I/O error occurs during compression
     */
    protected abstract void compress(InputStream source, OutputStream target) throws IOException;
    
    /**
     * {@inheritDoc}
     */
    @Override
    public List<WuicResource> parse(final EngineRequest request) throws IOException {
        // Return the same number of files
        final List<WuicResource> retval = new ArrayList<WuicResource>(request.getResources().size());
        
        // Compress only if needed
        if (works()) {
            
            // Compress each file
            for (WuicResource resource : request.getResources()) {

                if (resource.isTextCompressible()) {
                    // Compression has to be implemented by sub-classes
                    InputStream is = null;

                    try {
                        log.debug("Compressing {}", resource.getName());

                        // Source
                        is = resource.openStream();

                        // Where compression result will be written
                        final ByteArrayOutputStream os = new ByteArrayOutputStream();

                        // Do compression
                        compress(is, os);

                        // Now create resource
                        final byte[] bytes = os.toByteArray();
                        final String name = resource.getName();
                        final FileType fileType = resource.getFileType();
                        final WuicResource res = new ByteArrayWuicResource(bytes, name, fileType);
                        res.setAggregatable(resource.isAggregatable());
                        res.setBinaryCompressible(resource.isBinaryCompressible());
                        res.setTextCompressible(resource.isTextCompressible());
                        res.setCacheable(resource.isCacheable());

                        retval.add(res);
                    } finally {
                        if (is != null) {
                            is.close();
                        }
                    }
                } else {
                    retval.add(resource);
                }
            }
        } else {
            retval.addAll(request.getResources());
        }
        
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
    public Boolean works() {
        return getConfiguration().compress();
    }
}
