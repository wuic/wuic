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

import com.github.wuic.NutType;
import com.github.wuic.engine.EngineType;
import com.github.wuic.exception.WuicException;
import com.github.wuic.nut.core.ByteArrayNut;
import com.github.wuic.nut.Nut;
import com.github.wuic.engine.Engine;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.github.wuic.engine.EngineRequest;
import com.github.wuic.util.IOUtils;

/**
 * <p>
 * This {@link Engine engine} can aggregate all the specified files in one path.
 * Files are aggregated in the order of apparition in the given list. Note that
 * nothing will be done if {@link CGTextAggregatorEngine#doAggregation} flag is {@code false}.
 * </p>
 * 
 * @author Guillaume DROUET
 * @version 1.7
 * @since 0.1.0
 */
public class CGTextAggregatorEngine extends Engine {

    /**
     * Activate aggregation or not.
     */
    private Boolean doAggregation;

    /**
     * <p>
     * Builds the engine.
     * </p>
     *
     * @param aggregate activate aggregation or not
     */
    public CGTextAggregatorEngine(final Boolean aggregate) {
        this.doAggregation = aggregate;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public List<Nut> parse(final EngineRequest request)
            throws WuicException {

        // Do nothing if the configuration says that no aggregation should be done
        if (!works()) {
            return request.getNuts();
        }
        
        // In memory buffer for the aggregated nuts
        final String nutName = "aggregate" + request.getNuts().get(0).getNutType().getExtensions()[0];
        final ByteArrayOutputStream target = new ByteArrayOutputStream();
        
        // Append each path
        InputStream is = null;
        NutType nutType = null;
        final byte[] buffer = new byte[com.github.wuic.util.IOUtils.WUIC_BUFFER_LEN];

        final List<Nut> retval = new ArrayList<Nut>();
        final List<Nut> referencedNuts = new ArrayList<Nut>();

        // Aggregate each nut
        for (final Nut nut : request.getNuts()) {
            // Nut must be aggregatable
            if (nut.isAggregatable()) {
                try {
                    nutType = nut.getNutType();
                    is = nut.openStream();
                    IOUtils.copyStream(is, target);

                    // Begin content path writing on a new line when no compression is configured
                    for (Engine previous = getPrevious(); previous != null; previous = previous.getPrevious()) {

                        // Text compression is done before aggregation so it is a previous engine
                        if (previous instanceof CGAbstractCompressorEngine && !previous.works()) {
                            buffer[0] = '\n';
                            target.write(buffer, 0, 1);
                        }
                    }

                    if (nut.getReferencedNuts() != null) {
                        referencedNuts.addAll(nut.getReferencedNuts());
                    }
                } finally {
                    IOUtils.close(is);
                }
            } else {
                retval.add(nut);
            }
        }

        // Create the a nut containing all the aggregated nuts
        final Nut aggregate = new ByteArrayNut(target.toByteArray(), nutName, nutType);

        // Eventually add some extracted referenced nuts
        if (!referencedNuts.isEmpty()) {
            for (final Nut ref : referencedNuts) {
                aggregate.addReferencedNut(ref);
            }
        }

        retval.add(aggregate);

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
        return doAggregation;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<NutType> getNutTypes() {
        return Arrays.asList(NutType.CSS, NutType.JAVASCRIPT);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public EngineType getEngineType() {
        return EngineType.AGGREGATOR;
    }
}
