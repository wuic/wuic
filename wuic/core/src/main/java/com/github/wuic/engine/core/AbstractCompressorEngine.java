/*
 * "Copyright (c) 2014   Capgemini Technology Services (hereinafter "Capgemini")
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

import com.github.wuic.engine.NodeEngine;
import com.github.wuic.exception.WuicException;
import com.github.wuic.nut.ConvertibleNut;

import java.io.InputStream;
import java.util.List;

import com.github.wuic.engine.EngineRequest;
import com.github.wuic.util.IOUtils;
import com.github.wuic.util.Pipe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * This {@link NodeEngine engine} defines the base treatments to execute when
 * compressing a set of files. It could be extended by different sub-classes
 * which provide compression support.
 * </p>
 * 
 * <p>
 * The compression is never performed if the {@link AbstractCompressorEngine#doCompression} flag is set to {@code true}.
 * </p>
 * 
 * @author Guillaume DROUET
 * @version 1.7
 * @since 0.1.0
 */
public abstract class AbstractCompressorEngine extends NodeEngine implements Pipe.Transformer<ConvertibleNut> {
 
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
     * Builds a new instance.
     * </p>
     *
     * @param compress activate compression or not
     * @param rnp the extension prefix used to compute new nut name after compression.
     */
    public AbstractCompressorEngine(final Boolean compress, final String rnp) {
        doCompression = compress;
        renameExtensionPrefix = rnp;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<ConvertibleNut> internalParse(final EngineRequest request) throws WuicException {

        // Compress only if needed
        if (works()) {
            // Compress each path
            for (final ConvertibleNut nut : request.getNuts()) {
                compress(nut);
            }
        }
        
        if (getNext() != null) {
            return getNext().parse(request);
        } else {
            return request.getNuts();
        }
    }

    /**
     * <p>
     * Compresses the given nut.
     * </p>
     *
     * @param nut the nut to be compressed
     * @throws WuicException if an I/O error occurs
     */
    private void compress(final ConvertibleNut nut) throws WuicException {
        if (!nut.isTextReducible() || nut.getName().contains(renameExtensionPrefix)) {
            return;
        }

        // Compression has to be implemented by sub-classes
        InputStream is = null;

        try {
            log.debug("Compressing {}", nut.getName());

            // Build new name
            final StringBuilder nameBuilder = new StringBuilder(nut.getName());
            nameBuilder.insert(nut.getName().lastIndexOf('.'), renameExtensionPrefix);

            nut.setNutName(nameBuilder.toString());
            nut.addTransformer(this);

            // Also compress referenced nuts
            if (nut.getReferencedNuts() != null) {
                for (final ConvertibleNut ref : nut.getReferencedNuts()) {
                    compress(ref);
                }
            }
        } finally {
            IOUtils.close(is);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canAggregateTransformedStream() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Boolean works() {
        return doCompression;
    }
}
