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


package com.github.wuic.engine;

import com.github.wuic.Logging;
import com.github.wuic.NutType;
import com.github.wuic.exception.WuicException;
import com.github.wuic.nut.ConvertibleNut;
import com.github.wuic.nut.CompositeNut;
import com.github.wuic.nut.NotReachableNut;
import com.github.wuic.util.NumberUtils;
import com.github.wuic.util.Timer;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * <p>
 * Represents an {@link Engine} that could be the head of a chain of responsibility. This engine can parse a request
 * containing {@link ConvertibleNut} of different {@link com.github.wuic.NutType type} and provides the way to access to
 * a particular nut of a process result.
 * </p>
 *
 * <p>
 * The particular {@link Engine} essentially exists to expose specific features from cache support.
 * </p>
 *
 * @author Guillaume DROUET
 * @since 0.4.4
 */
public abstract class HeadEngine extends Engine {

    /**
     * {@inheritDoc}
     */
    @Override
    public List<ConvertibleNut> parse(final EngineRequest request) throws WuicException {
        if (works()) {
            final Timer timer = request.createTimer();
            timer.start();

            try {
                return internalParse(request);
            } finally {
                final long elapsed = timer.end();
                Logging.TIMER.log("Parse operation by head engine executed in {}s", (float) (elapsed) / (float) NumberUtils.ONE_THOUSAND);
                request.reportParseEngine(elapsed);
            }
        } else {
            return runChains(request);
        }
    }

    /**
     * <p>
     * This method parses the sequences of {@link ConvertibleNut} of the same {@link com.github.wuic.NutType type} and
     * aggregates the nuts with same names between the results.
     * </p>
     *
     * @param request the request providing engine chains
     * @return the process result
     * @throws WuicException if WUIC fails to process nuts
     */
    public static List<ConvertibleNut> runChains(final EngineRequest request) throws WuicException {
        final List<ConvertibleNut> retval = new ArrayList<ConvertibleNut>();
        final Iterator<List<? extends ConvertibleNut>> it = request.iterator();

        // We parse a request for each sequence of nuts having the same type
        while (it.hasNext()) {
            final List<? extends ConvertibleNut> nuts = it.next();
            final NutType nutType = nuts.get(0).getInitialNutType();
            final NodeEngine chain = request.getChainFor(nutType);
            retval.addAll(chain == null ? nuts : chain.parse(new EngineRequestBuilder(request).nuts(nuts).build()));
        }

        for (final ConvertibleNut nut : retval) {

            // One nut can't be read, don't try to merge it
            if (nut instanceof NotReachableNut) {
                return retval;
            }
        }

        // Merges all nuts with same type (for instance two 'aggregate.js' nuts will be wrapped by one composite nut
        return CompositeNut.mergeNuts(request.getProcessContext(), retval, request.getCharset());
    }

    /**
     * <p>
     * Parses the given request and returns the nut associated to the given path.
     * </p>
     *
     * @param request the request
     * @param path the nut name
     * @return the associated nut
     * @throws WuicException if request could not be executed successfully
     */
    public abstract ConvertibleNut parse(EngineRequest request, String path) throws WuicException;
}
