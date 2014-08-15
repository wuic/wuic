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

import com.github.wuic.ApplicationConfig;
import com.github.wuic.NutType;
import com.github.wuic.config.BooleanConfigParam;
import com.github.wuic.config.ConfigConstructor;
import com.github.wuic.engine.EngineRequest;
import com.github.wuic.engine.EngineService;
import com.github.wuic.engine.EngineType;
import com.github.wuic.engine.NodeEngine;
import com.github.wuic.exception.WuicException;
import com.github.wuic.exception.wrapper.StreamException;
import com.github.wuic.nut.ByteArrayNut;
import com.github.wuic.nut.Nut;
import com.github.wuic.util.IOUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.GZIPOutputStream;

/**
 * <p>
 * This engine GZIP nut content.
 * </p>
 *
 * @author Guillaume DROUET
 * @version 1.0
 * @since 0.5.0
 */
@EngineService(injectDefaultToWorkflow = false, isCoreEngine = true)
public class GzipEngine extends NodeEngine {

    /**
     * Do compression or not.
     */
    private Boolean works;

    /**
     * <p>
     * Builds a new instance.
     * </p>
     *
     * @param compress compress or not
     */
    @ConfigConstructor
    public GzipEngine(@BooleanConfigParam(propertyKey = ApplicationConfig.COMPRESS, defaultValue = true) Boolean compress) {
        works = compress;
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
        return EngineType.BINARY_COMPRESSION;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected List<Nut> internalParse(final EngineRequest request) throws WuicException {
        if (!works()) {
            return request.getNuts();
        }

        final List<Nut> retval = new ArrayList<Nut>(request.getNuts().size());

        for (final Nut nut : request.getNuts()) {
            retval.add(compress(nut));
        }

        return retval;
    }

    /**
     * <p>
     * Compress the given {@link Nut} and return it. Also compress any referenced nut.
     * </p>
     *
     * @param nut the nut
     * @return the compressed nut
     * @throws WuicException if compression fails
     */
    private Nut compress(final Nut nut) throws WuicException {
        if (nut.isCompressed()) {
            return nut;
        }

        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        GZIPOutputStream gos;
        InputStream is = nut.openStream();

        try {
            gos = new GZIPOutputStream(bos);
            IOUtils.copyStream(is, gos);
            gos.close();
            final Nut compress = new ByteArrayNut(bos.toByteArray(), nut.getName(), nut.getNutType(), Arrays.asList(nut));
            compress.setIsCompressed(Boolean.TRUE);

            if (nut.getReferencedNuts() != null) {

                // Also add all the referenced nuts
                for (final Nut ref : nut.getReferencedNuts()) {
                    compress.addReferencedNut(compress(ref));
                }
            }

            return compress;
        } catch (IOException ioe) {
            throw new StreamException(ioe);
        } finally {
            IOUtils.close(is);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Boolean works() {
        return works;
    }
}
