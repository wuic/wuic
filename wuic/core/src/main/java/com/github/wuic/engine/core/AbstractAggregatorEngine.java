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

import com.github.wuic.ApplicationConfig;
import com.github.wuic.config.BooleanConfigParam;
import com.github.wuic.config.Config;
import com.github.wuic.engine.EngineRequestBuilder;
import com.github.wuic.engine.EngineType;
import com.github.wuic.engine.NodeEngine;
import com.github.wuic.exception.WuicException;
import com.github.wuic.nut.ConvertibleNut;
import com.github.wuic.nut.Nut;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.github.wuic.engine.EngineRequest;
import com.github.wuic.nut.PipedConvertibleNut;
import com.github.wuic.util.Pipe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * This {@link NodeEngine engine} is an abstraction for aggregation nut aggregation.
 * </p>
 *
 * @author Guillaume DROUET
 * @since 0.4.1
 */
public abstract class AbstractAggregatorEngine extends NodeEngine {

    /**
     * Logger.
     */
    private final Logger log = LoggerFactory.getLogger(getClass());

    /**
     * Activate aggregation or not.
     */
    private Boolean doAggregation;

    /**
     * Transformers.
     */
    private List<Pipe.Transformer<ConvertibleNut>> transformers;

    /**
     * <p>
     * Initializes a new aggregator engine.
     * </p>
     *
     * @param aggregate if aggregation should be activated or not
     */
    @Config
    public void init(@BooleanConfigParam(defaultValue = true, propertyKey = ApplicationConfig.AGGREGATE) final Boolean aggregate) {
        this.doAggregation = aggregate;
        this.transformers = new ArrayList<Pipe.Transformer<ConvertibleNut>>();
        this.log.info("Aggregator initialized with activation={}", aggregate);
    }

    /**
     * <p>
     * Computes the name of an aggregated set of nuts for a given type's extensions.
     * </p>
     *
     * @param nutTypeExtensions the type extensions
     * @return the aggregation name
     */
    public static String aggregationName(final String[] nutTypeExtensions) {
        return "aggregate" + nutTypeExtensions[0];
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final List<ConvertibleNut> internalParse(final EngineRequest request)
            throws WuicException {
        // Aggregate only static nuts
        final List<Nut> staticNuts = new ArrayList<Nut>();
        final List<ConvertibleNut> retval = new ArrayList<ConvertibleNut>();

        for (final Nut nut : request.getNuts()) {
            if (nut.isDynamic()) {
                retval.add(nut instanceof ConvertibleNut ? ConvertibleNut.class.cast(nut) : new PipedConvertibleNut(nut));
            } else {
                staticNuts.add(nut);
            }
        }

        log.info("Aggregating nuts {}", Arrays.toString(staticNuts.toArray()));
        retval.addAll(aggregationParse(new EngineRequestBuilder(request).nuts(staticNuts).build()));

        // Compute proxy URIs and add transformers
        for (final ConvertibleNut nut : retval) {
            nut.setProxyUri(request.getHeap().proxyUriFor(nut));

            if (works()) {
                for (final Pipe.Transformer<ConvertibleNut> t : transformers) {
                    nut.addTransformer(t);
                }
            }
        }

        return retval;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Boolean works() {
        return doAggregation;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final EngineType getEngineType() {
        return EngineType.AGGREGATOR;
    }

    /**
     * <p>
     * Adds some transformers this engine should add to the parsed nuts.
     * </p>
     *
     * @param transformer the transformer to add
     */
    public void addTransformer(final Pipe.Transformer<ConvertibleNut> transformer) {
        this.transformers.add(transformer);
    }

    /**
     * <p>
     * Do aggregation parsing.
     * </p>
     *
     * @param request the request
     * @return the aggregated nuts
     * @throws WuicException if an error occurs
     */
    protected abstract List<ConvertibleNut> aggregationParse(EngineRequest request) throws WuicException ;
}
