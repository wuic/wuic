/*
 * "Copyright (c) 2015   Capgemini Technology Services (hereinafter "Capgemini")
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

import com.github.wuic.NutType;
import com.github.wuic.engine.Engine;
import com.github.wuic.engine.EngineRequest;
import com.github.wuic.engine.EngineRequestBuilder;
import com.github.wuic.engine.EngineType;
import com.github.wuic.engine.NodeEngine;
import com.github.wuic.exception.WuicException;
import com.github.wuic.nut.ConvertibleNut;
import com.github.wuic.nut.PipedConvertibleNut;
import com.github.wuic.util.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * <p>
 * This {@link com.github.wuic.engine.NodeEngine engine} defines the base treatments to execute when a
 * {@link com.github.wuic.nut.Nut} should by converted from a {@link com.github.wuic.NutType} to another.
 * </p>
 *
 * <p>
 * When a {@link com.github.wuic.NutType} is changed, the engine chain related to the type is executed and the result is
 * returned. Contrary to engines that {@link com.github.wuic.engine.EngineType#INSPECTOR instect}, the nut won't be added
 * as a referenced nut ({@link com.github.wuic.nut.ConvertibleNut#getReferencedNuts()}).
 * </p>
 *
 * @author Guillaume DROUET
 * @version 1.0
 * @since 0.5.1
 */
public abstract class AbstractConverterEngine
        extends NodeEngine
        implements EngineRequestTransformer.RequireEngineRequestTransformer {

    /**
     * Logger.
     */
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    /**
     * Activate conversion or not.
     */
    private final Boolean doConversion;

    /**
     * Aggregator.
     */
    private final Engine aggregator;

    /**
     * <p>
     * Builds a new instance.
     * </p>
     *
     * @param convert activate compression or not
     * @param asynchronous computes asynchronously the version number or not
     */
    public AbstractConverterEngine(final Boolean convert, final Boolean asynchronous) {
        doConversion = convert;
        aggregator = new TextAggregatorEngine(Boolean.TRUE, asynchronous);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<ConvertibleNut> internalParse(final EngineRequest request) throws WuicException {

        // Compress only if needed
        if (works()) {
            final List<ConvertibleNut> aggregate = aggregator.parse(request);
            final List<ConvertibleNut> retval = new ArrayList<ConvertibleNut>(aggregate.size());

            // Compress each path
            for (final ConvertibleNut nut : aggregate) {
                retval.add(convert(nut, request));
            }

            return retval;
        } else {
            return request.getNuts();
        }
    }

    /**
     * <p>
     * Converts the given nut.
     * </p>
     *
     * @param convertibleNut the nut to be compressed
     * @param request the request
     * @return the new convertible nut (the given nut has been set has an original nut
     * @throws WuicException if an I/O error occurs
     */
    private ConvertibleNut convert(final ConvertibleNut convertibleNut, final EngineRequest request) throws WuicException {
        // Compression has to be implemented by sub-classes
        InputStream is = null;

        try {
            if (targetNutType().equals(convertibleNut.getInitialNutType())) {
                throw new IllegalStateException("NutType must be changed by the transformer.");
            }

            log.debug("Converting {}", convertibleNut.getName());

            final ConvertibleNut nut = new PipedConvertibleNut(convertibleNut);

            // Set target state
            nut.setNutName(nut.getName() + targetNutType().getExtensions()[0]);
            nut.setNutType(targetNutType());

            nut.addTransformer(new EngineRequestTransformer(request, this));

            // Apply transformation for target type
            NodeEngine chain = request.getChainFor(nut.getNutType());

            if (chain != null) {
                chain.parse(new EngineRequestBuilder(request)
                        .skip(request.alsoSkip(EngineType.CACHE))
                        .nuts(Arrays.asList(nut))
                        .build());
            }

            // Also convert referenced nuts
            if (convertibleNut.getReferencedNuts() != null) {
                for (final ConvertibleNut ref : convertibleNut.getReferencedNuts()) {
                    nut.addReferencedNut(convert(ref, request));
                }
            }

            nut.addReferencedNut(convertibleNut);

            return nut;
        } finally {
            IOUtils.close(is);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final EngineType getEngineType() {
        return EngineType.CONVERTER;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Boolean works() {
        return doConversion;
    }

    /**
     * <p>
     * Returns the {@link NutType} corresponding to the converted nut.
     * </p>
     *
     * @return the target type
     */
    protected abstract NutType targetNutType();
}
