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


package com.github.wuic.engine;

import com.github.wuic.NutType;
import com.github.wuic.exception.WuicException;
import com.github.wuic.nut.Nut;
import com.github.wuic.nut.core.CompositeNut;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * <p>
 * Represents an {@link Engine} that could be the head of a chain of responsibility. This engine can parse a request
 * containing {@link Nut} of different {@link com.github.wuic.NutType type} and provides the way to access to a particular
 * nut of a process result.
 * </p>
 *
 * <p>
 * The particular {@link Engine} essentially exists to expose specific features from cache support.
 * </p>
 *
 * @author Guillaume DROUET
 * @version 1.0
 * @since 0.4.4
 */
public abstract class HeadEngine extends Engine {

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Nut> parse(final EngineRequest request) throws WuicException {
        if (works()) {
            return internalParse(request);
        } else {
            return runChains(request, Boolean.FALSE);
        }
    }

    /**
     * <p>
     * This method parses the sequences of {@link Nut} of the same {@link com.github.wuic.NutType type} and aggregates
     * the nuts with same names between the results.
     * </p>
     *
     * @param request the request providing engine chains
     * @param bestEffort performs request in best effort
     * @return the process result
     * @throws WuicException if WUIC fails to process nuts
     */
    public static List<Nut> runChains(final EngineRequest request, final Boolean bestEffort) throws WuicException {
        final List<Nut> retval = new ArrayList<Nut>();
        final Iterator<List<Nut>> it = request.iterator();

        // We parse a request for each sequence of nuts having the same type
        while (it.hasNext()) {
            final List<Nut> nuts = it.next();
            final NutType nutType = nuts.get(0).getNutType();
            final NodeEngine chain = request.getChainFor(nutType);
            final EngineRequest req = bestEffort ? new EngineRequest(nuts, request, nutType.getRequiredForBestEffort()) : new EngineRequest(nuts, request);
            retval.addAll(chain == null ? nuts : chain.parse(req));
        }

        // Merges all nuts with same type (for instance two 'aggregate.js' nuts will be wrapped by one composite nut
        return CompositeNut.mergeNuts(retval);
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
    public abstract Nut parse(EngineRequest request, String path) throws WuicException;
}
