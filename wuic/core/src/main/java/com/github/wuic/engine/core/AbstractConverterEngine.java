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
import com.github.wuic.util.Input;
import com.github.wuic.util.Output;
import com.github.wuic.util.Pipe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
     * {@link #transform(Input, Output, ConvertibleNut, EngineRequest)}
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
         * <p>
         * Builds a new instance with an empty list of transformers.
         * </p>
         *
         * @param request the request
         */
        public ConverterTransformer(final EngineRequest request) {
            this(request, Collections.<Pipe.Transformer<ConvertibleNut>>emptyList());
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean transform(final Input is, final Output os, final ConvertibleNut nut, final EngineRequest request)
                throws IOException {
            Input result = null;

            try {
                result = AbstractConverterEngine.this.transform(is, nut, request);

                if (transformers.isEmpty()) {
                    IOUtils.copyStream(result, os);
                } else {
                    // Continue transformation of conversion result
                    final Pipe<ConvertibleNut> finalPipe = new Pipe<ConvertibleNut>(nut, result, request.getTimerTreeFactory());

                    for (final Pipe.Transformer<ConvertibleNut> t : transformers) {
                        finalPipe.register(t);
                    }

                    Pipe.executeAndWriteTo(finalPipe, null, os);
                    request.reportTransformerStat(finalPipe.getStatistics());
                }
            } finally {
                IOUtils.close(result);
            }

            return true;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean transform(final Input is, final Output os, final ConvertibleNut convertible) throws IOException {
           return engineRequestTransformer.transform(is, os, convertible);
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
                final List<ConvertibleNut> res = chain.parse(new EngineRequestBuilder(request)
                        .skip(request.alsoSkip(EngineType.CACHE, EngineType.AGGREGATOR, EngineType.INSPECTOR))
                        .nuts(Arrays.asList(nut))
                        .build());

                if (res.isEmpty()) {
                    log.warn("No result found after parsing the nut '{}'.", nut.getName());
                    nut.addTransformer(new ConverterTransformer(request));
                } else {
                    nut = adapt(request, res.get(0));
                }
            } else {
                nut.addTransformer(new ConverterTransformer(request));
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
    private ConvertibleNut adapt(final EngineRequest request, final ConvertibleNut convertibleNut) {
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

        if (finalTransformers == null) {
            nut.addTransformer(new ConverterTransformer(request));
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
     * @return the result of transformed stream
     * @throws IOException if transformation fails
     */
    protected abstract Input transform(Input is, ConvertibleNut nut, EngineRequest request) throws IOException;
}
