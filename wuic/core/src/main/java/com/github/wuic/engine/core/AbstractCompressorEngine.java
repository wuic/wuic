/*
 * Copyright (c) 2016   The authors of WUIC
 *
 * License/Terms of Use
 * Permission is hereby granted, free of charge and for the term of intellectual
 * property rights on the Software, to any person obtaining a copy of this software
 * and associated documentation files (the "Software"), to use, copy, modify and
 * propagate free of charge, anywhere in the world, all or part of the Software
 * subject to the following mandatory conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, 
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, PEACEFUL ENJOYMENT,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS
 * OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */


package com.github.wuic.engine.core;

import com.github.wuic.config.BooleanConfigParam;
import com.github.wuic.config.Config;
import com.github.wuic.engine.NodeEngine;
import com.github.wuic.exception.WuicException;
import com.github.wuic.nut.ConvertibleNut;

import java.io.InputStream;
import java.util.List;

import com.github.wuic.engine.EngineRequest;
import com.github.wuic.util.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.github.wuic.ApplicationConfig.COMPRESS;

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
 * @since 0.1.0
 */
public abstract class AbstractCompressorEngine extends NodeEngine implements EngineRequestTransformer.RequireEngineRequestTransformer {
 
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
     * Initializes a new {@link com.github.wuic.engine.Engine}.
     * </p>
     *
     * @param compress activate compression or not
     */
    @Config
    public void init(@BooleanConfigParam(propertyKey = COMPRESS, defaultValue = true) final Boolean compress) {
        doCompression = compress;
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
                compress(nut, request);
            }
        }

        return request.getNuts();
    }

    /**
     * <p>
     * Indicates if the transformed stream can be aggregate.
     * </p>
     *
     * @return {@code true} if transformed stream can be aggregated, {@code false} otherwise
     */
    protected boolean canAggregateTransformedStream() {
        return true;
    }

    /**
     * <p>
     * Compresses the given nut.
     * </p>
     *
     * @param nut the nut to be compressed
     * @param request the request
     * @throws WuicException if an I/O error occurs
     */
    private void compress(final ConvertibleNut nut, final EngineRequest request) throws WuicException {
        if (renameExtensionPrefix != null && nut.getName().contains(renameExtensionPrefix)) {
            return;
        }

        // Compression has to be implemented by sub-classes
        InputStream is = null;

        try {
            log.debug("Compressing {}", nut.getName());

            // Build new name
            final StringBuilder nameBuilder = new StringBuilder(nut.getName());

            if (renameExtensionPrefix != null) {
                nameBuilder.insert(nut.getName().lastIndexOf('.'), renameExtensionPrefix);
            }

            nut.setNutName(nameBuilder.toString());
            nut.addTransformer(new EngineRequestTransformer(request, this, canAggregateTransformedStream(), getEngineType().ordinal()));

            // Also compress referenced nuts
            if (nut.getReferencedNuts() != null) {
                for (final ConvertibleNut ref : nut.getReferencedNuts()) {
                    if (getNutTypes().contains(ref.getNutType())) {
                        compress(ref, request);
                    }
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
    public Boolean works() {
        return doCompression;
    }

    /**
     * <p>
     * Sets the extension prefix.
     * </p>
     *
     * @param renameExtensionPrefix the extension prefix used to compute new nut name after compression, {@code null} if no extension is managed
     */
    public void setRenameExtensionPrefix(final String renameExtensionPrefix) {
        this.renameExtensionPrefix = renameExtensionPrefix;
    }
}
