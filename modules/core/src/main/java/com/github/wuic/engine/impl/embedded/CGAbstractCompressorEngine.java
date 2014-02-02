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
import com.github.wuic.exception.wrapper.StreamException;
import com.github.wuic.nut.core.ByteArrayNut;
import com.github.wuic.nut.Nut;
import com.github.wuic.engine.Engine;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.github.wuic.engine.EngineRequest;
import com.github.wuic.util.IOUtils;
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
 * The compression is never performed if the {@link CGAbstractCompressorEngine#doCompression} flag is set to {@code true}.
 * </p>
 * 
 * @author Guillaume DROUET
 * @version 1.6
 * @since 0.1.0
 */
public abstract class CGAbstractCompressorEngine extends Engine {
 
    /**
     * Logger.
     */
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    /**
     * Activate compression or not.
     */
    private Boolean doCompression;

    /**
     * The extension prefix used to compute new nut name after compression.
     */
    private String renameExtensionPrefix;

    /**
     * <p>
     * Compress a stream (the source) into a target. The target is overridden if
     * it already exists.
     * </p>
     * 
     * @param source the source
     * @param target the path where to compressed content should be written
     * @throws com.github.wuic.exception.wrapper.StreamException if an I/O error occurs during compression
     */
    protected abstract void compress(InputStream source, OutputStream target) throws StreamException;

    /**
     * <p>
     * Builds a new instance.
     * </p>
     *
     * @param compress activate compression or not
     * @param rnp the extension prefix used to compute new nut name after compression.
     */
    public CGAbstractCompressorEngine(final Boolean compress, final String rnp) {
        doCompression = compress;
        renameExtensionPrefix = rnp;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Nut> internalParse(final EngineRequest request) throws WuicException {
        // Return the same number of files
        final List<Nut> retval = new ArrayList<Nut>(request.getNuts().size());
        
        // Compress only if needed
        if (works()) {
            // Compress each path
            for (final Nut nut : request.getNuts()) {
                final Nut compress = compress(nut);
                compress.setProxyUri(request.getHeap().proxyUriFor(compress));
                retval.add(compress);
            }
        } else {
            retval.addAll(request.getNuts());
        }
        
        if (getNext() != null) {
            return getNext().parse(new EngineRequest(retval, request));
        } else {
            return retval;
        }
    }

    /**
     * <p>
     * Compresses the given nut.
     * </p>
     *
     * @param nut the nut to be compressed
     * @return the compressed nut
     * @throws WuicException if an I/O error occurs
     */
    private Nut compress(final Nut nut) throws WuicException {
        if (!nut.isTextCompressible()) {
            return nut;
        }

        // Compression has to be implemented by sub-classes
        InputStream is = null;

        try {
            log.debug("Compressing {}", nut.getName());

            // Source
            is = nut.openStream();

            // Where compression result will be written
            final ByteArrayOutputStream os = new ByteArrayOutputStream();

            // Do compression
            compress(is, os);

            // Build new name
            final StringBuilder nameBuilder = new StringBuilder(nut.getName());
            nameBuilder.insert(nut.getName().lastIndexOf('.'), renameExtensionPrefix);

            // Now create nut
            final Nut res = new ByteArrayNut(os.toByteArray(), nameBuilder.toString(), nut.getNutType(), Arrays.asList(nut));
            res.setAggregatable(nut.isAggregatable());
            res.setBinaryCompressible(nut.isBinaryCompressible());
            res.setTextCompressible(nut.isTextCompressible());
            res.setCacheable(nut.isCacheable());

            // Also compress referenced nuts
            if (nut.getReferencedNuts() != null) {
                for (Nut ref : nut.getReferencedNuts()) {
                    res.addReferencedNut(compress(ref));
                }
            }

            return res;
        } finally {
            IOUtils.close(is);
        }
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public Boolean works() {
        return doCompression;
    }
}
