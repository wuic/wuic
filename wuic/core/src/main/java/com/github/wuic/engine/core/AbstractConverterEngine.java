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

import com.github.wuic.NutType;
import com.github.wuic.config.BooleanConfigParam;
import com.github.wuic.config.Config;
import com.github.wuic.engine.EngineRequest;
import com.github.wuic.engine.EngineRequestBuilder;
import com.github.wuic.engine.EngineType;
import com.github.wuic.engine.NodeEngine;
import com.github.wuic.exception.WuicException;
import com.github.wuic.nut.ConvertibleNut;
import com.github.wuic.nut.NutWrapper;
import com.github.wuic.nut.PipedConvertibleNut;
import com.github.wuic.util.IOUtils;
import com.github.wuic.util.Pipe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static com.github.wuic.ApplicationConfig.COMPUTE_VERSION_ASYNCHRONOUSLY;
import static com.github.wuic.ApplicationConfig.CONVERT;

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
 * @since 0.5.1
 */
public abstract class AbstractConverterEngine extends NodeEngine {

    /**
     * <p>
     * This {@link com.github.wuic.util.Pipe.Transformer} is an adapter that calls
     * {@link #transform(InputStream, OutputStream, ConvertibleNut, EngineRequest)}
     * from the enclosing instance.
     * </p>
     *
     * @author Guillaume DROUET
     * @since 0.5.3
     */
    public final class ConverterTransformer
            implements EngineRequestTransformer.RequireEngineRequestTransformer, Pipe.Transformer<ConvertibleNut> {

        /**
         * The transformers.
         */
        private final List<Pipe.Transformer<ConvertibleNut>> transformers;

        /**
         * The request transformer that calls this instance.
         */
        private final EngineRequestTransformer engineRequestTransformer;

        /**
         * <p>
         * Builds a new instance.
         * </p>
         *
         * @param request the request
         * @param transformerList the transformer list
         */
        public ConverterTransformer(final EngineRequest request,
                                    final List<Pipe.Transformer<ConvertibleNut>> transformerList) {
            transformers = transformerList;
            engineRequestTransformer = new EngineRequestTransformer(request, this, getEngineType().ordinal());
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void transform(final InputStream is, final OutputStream os, final ConvertibleNut nut, final EngineRequest request)
                throws IOException {
            InputStream result = null;

            try {
                result = AbstractConverterEngine.this.transform(is, nut, request);

                if (transformers.isEmpty()) {
                    IOUtils.copyStream(result, os);
                } else {
                    // Continue transformation of conversion result
                    final ByteArrayOutputStream conversionOs = new ByteArrayOutputStream();
                    IOUtils.copyStream(result, conversionOs);
                    final Pipe<ConvertibleNut> finalPipe = new Pipe<ConvertibleNut>(nut, new ByteArrayInputStream(conversionOs.toByteArray()));

                    for (final Pipe.Transformer<ConvertibleNut> t : transformers) {
                        finalPipe.register(t);
                    }

                    Pipe.executeAndWriteTo(finalPipe, null, os);
                }
            } finally {
                IOUtils.close(result);
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void transform(final InputStream is, final OutputStream os, final ConvertibleNut convertible) throws IOException {
           engineRequestTransformer.transform(is, os, convertible);
        }

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
            return getEngineType().ordinal();
        }
    }

    /**
     * Logger.
     */
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    /**
     * Activate conversion or not.
     */
    private Boolean doConversion;

    /**
     * Aggregator.
     */
    private TextAggregatorEngine aggregator;

    /**
     * <p>
     * Initializes a new instance.
     * </p>
     *
     * @param convert activate compression or not
     * @param asynchronous computes asynchronously the version number or not
     */
    @Config
    public void init(@BooleanConfigParam(propertyKey = CONVERT, defaultValue = true) final Boolean convert,
                     @BooleanConfigParam(propertyKey = COMPUTE_VERSION_ASYNCHRONOUSLY, defaultValue = true) final Boolean asynchronous) {
        doConversion = convert;
        aggregator = new TextAggregatorEngine();
        aggregator.init(Boolean.TRUE);
        aggregator.async(asynchronous);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<ConvertibleNut> internalParse(final EngineRequest request) throws WuicException {

        // Compress only if needed
        if (works()) {
            if (aggregator.works()) {
                return Arrays.asList(convert(aggregator.newCompositeNut(request), request));
            } else {
                final List<ConvertibleNut> retval = new ArrayList<ConvertibleNut>(request.getNuts().size());

                // Compress each path
                for (final ConvertibleNut nut : request.getNuts()) {

                    // Transformer from the aggregator is not required here
                    retval.add(convert(nut, request));
                }

                return retval;
            }
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

            ConvertibleNut nut = new PipedConvertibleNut(convertibleNut);

            // Set target state
            nut.setNutName(nut.getName() + targetNutType().getExtensions()[0]);
            nut.setNutType(targetNutType());

            // Apply transformation for target type
            NodeEngine chain = request.getChainFor(nut.getNutType());

            if (chain != null) {
                final Set<Pipe.Transformer<ConvertibleNut>> initialTransformers = nut.getTransformers() != null ?
                        new LinkedHashSet<Pipe.Transformer<ConvertibleNut>>(nut.getTransformers()) : Collections.EMPTY_SET;
                final List<ConvertibleNut> res = chain.parse(new EngineRequestBuilder(request)
                        .skip(request.alsoSkip(EngineType.CACHE, EngineType.AGGREGATOR, EngineType.INSPECTOR))
                        .nuts(Arrays.asList(nut))
                        .build());

                if (res.isEmpty()) {
                    log.warn("No result found after parsing the nut '{}'.", nut.getName());
                    nut.addTransformer(new ConverterTransformer(request, Collections.EMPTY_LIST));
                } else {
                    nut = adapt(initialTransformers, request, res.get(0));
                }
            } else {
                nut.addTransformer(new ConverterTransformer(request, Collections.EMPTY_LIST));
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
     * <p>
     * Adapts the given {@code ConvertibleNut} by adding the right transformers.
     * </p>
     *
     * @param request the request the request
     * @param convertibleNut the convertible nut
     * @return the adapted nut
     */
    private ConvertibleNut adapt(final Set<Pipe.Transformer<ConvertibleNut>> initialTransformers,
                                 final EngineRequest request,
                                 final ConvertibleNut convertibleNut) {
        // Let our transformer deal with aggregation stuffs
        final ConvertibleNut nut = new NutWrapper(convertibleNut) {

            /**
             * {@inheritDoc}
             */
            @Override
            public boolean ignoreCompositeStreamOnTransformation() {
                return true;
            }
        };

        final Set<Pipe.Transformer<ConvertibleNut>> finalTransformers = nut.getTransformers();

        // No initial transformer, just execute the final transformer on the aggregated content
        if (initialTransformers == null) {
            if (finalTransformers != null) {
                final List<Pipe.Transformer<ConvertibleNut>> transformers =
                        new ArrayList<Pipe.Transformer<ConvertibleNut>>(finalTransformers);
                finalTransformers.removeAll(finalTransformers);
                nut.addTransformer(new ConverterTransformer(request, transformers));
            } else {
                nut.addTransformer(new ConverterTransformer(request, Collections.EMPTY_LIST));
            }
        } else if (finalTransformers == null) {
            nut.addTransformer(new ConverterTransformer(request, Collections.EMPTY_LIST));
        } else {
            // Move post conversion transformers execution to our own transformer
            final List<Pipe.Transformer<ConvertibleNut>> postConversionTransformers =
                    new ArrayList<Pipe.Transformer<ConvertibleNut>>(finalTransformers);
            finalTransformers.clear();
            nut.addTransformer(new ConverterTransformer(request, postConversionTransformers));
        }

        return nut;
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

    /**
     * <p>
     * Transforms a stream as specified by {@link com.github.wuic.engine.core.EngineRequestTransformer.RequireEngineRequestTransformer}.
     * </p>
     *
     * @param is the input stream
     * @param nut the nut corresponding to the stream
     * @param request the request that initiated transformation
     * @throws IOException if transformation fails
     */
    protected abstract InputStream transform(InputStream is, ConvertibleNut nut, EngineRequest request) throws IOException;
}
