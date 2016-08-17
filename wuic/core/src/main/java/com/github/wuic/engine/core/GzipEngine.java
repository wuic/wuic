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

import com.github.wuic.EnumNutType;
import com.github.wuic.NutType;
import com.github.wuic.config.Alias;
import com.github.wuic.config.BooleanConfigParam;
import com.github.wuic.config.Config;
import com.github.wuic.engine.EngineRequest;
import com.github.wuic.engine.EngineService;
import com.github.wuic.engine.EngineType;
import com.github.wuic.engine.NodeEngine;
import com.github.wuic.exception.WuicException;
import com.github.wuic.nut.ConvertibleNut;
import com.github.wuic.util.IOUtils;
import com.github.wuic.util.Input;
import com.github.wuic.util.Output;
import com.github.wuic.util.Pipe;

import java.io.IOException;
import java.util.List;
import java.util.zip.GZIPOutputStream;

import static com.github.wuic.ApplicationConfig.COMPRESS;

/**
 * <p>
 * This engine GZIP nut content.
 * </p>
 *
 * @author Guillaume DROUET
 * @since 0.5.0
 */
@EngineService(injectDefaultToWorkflow = false, isCoreEngine = true)
@Alias("gzip")
public class GzipEngine extends NodeEngine {

    /**
     * Do compression or not.
     */
    private Boolean works;

    /**
     * <p>
     * Initializes a new instance.
     * </p>
     *
     * @param compress compress or not
     */
    @Config
    public void init(@BooleanConfigParam(propertyKey = COMPRESS, defaultValue = true) Boolean compress) {
        works = compress;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<NutType> getNutTypes() {
        return getNutTypeFactory().getNutType(EnumNutType.values());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public EngineType getEngineType() {
        return EngineType.BINARY_COMPRESSION;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected List<ConvertibleNut> internalParse(final EngineRequest request) throws WuicException {
        if (!works()) {
            return request.getNuts();
        }

        for (final ConvertibleNut nut : request.getNuts()) {
            compress(nut);
        }

        return request.getNuts();
    }

    /**
     * <p>
     * Compress the given {@link ConvertibleNut} by adding a transformer that GZIP the stream. Also compress any
     * referenced nut.
     * </p>
     *
     * @param nut the nut
     * @throws WuicException if compression fails
     */
    private void compress(final ConvertibleNut nut) throws WuicException {

        if (!nut.isCompressed()) {
            nut.setIsCompressed(Boolean.TRUE);
            nut.addTransformer(GzipTransformer.INSTANCE);
        }

        if (nut.getReferencedNuts() != null) {
            // Also add all the referenced nuts
            for (final ConvertibleNut ref : nut.getReferencedNuts()) {
                compress(ref);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Boolean works() {
        return works;
    }

    /**
     * This transformer simply GZIP the stream.
     */
    private enum GzipTransformer implements Pipe.Transformer<ConvertibleNut> {

        /**
         * Singleton.
         */
        INSTANCE;

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean canAggregateTransformedStream() {
            return false;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int order() {
            return EngineType.BINARY_COMPRESSION.ordinal();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean transform(final Input is, final Output os, final ConvertibleNut convertibleNut) throws IOException {
            final GZIPOutputStream gos = new GZIPOutputStream(os.outputStream());
            IOUtils.copyStream(is.inputStream(), gos);
            gos.close();

            // Make sure the nut state is changed in case of this transformer is reused.
            convertibleNut.setIsCompressed(Boolean.TRUE);

            return true;
        }
    }
}
