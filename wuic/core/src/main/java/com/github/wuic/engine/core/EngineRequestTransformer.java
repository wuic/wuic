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

import com.github.wuic.engine.EngineRequest;
import com.github.wuic.nut.ConvertibleNut;
import com.github.wuic.util.Input;
import com.github.wuic.util.Output;
import com.github.wuic.util.Pipe;

import java.io.IOException;

/**
 * <p>
 * This {@link com.github.wuic.util.Pipe.Transformer} handles {@link ConvertibleNut} transformation and perform it
 * by delegating the job to a {@link RequireEngineRequestTransformer}. The delegated transform method will expect the
 * usual parameters with an additional {@link EngineRequest}, which is wrapped by this class.
 * </p>
 *
 * @author Guillaume DROUET
 * @since 0.5.1
 */
public class EngineRequestTransformer extends Pipe.DefaultTransformer<ConvertibleNut> {

    /**
     * <p>
     * This interface is implemented by {@link com.github.wuic.util.Pipe.Transformer} which needs a {@link EngineRequest}
     * to perform transformation.
     * </p>
     *
     * @author Guillaume DROUET
     * @since 0.5.1
     */
    public interface RequireEngineRequestTransformer {

        /**
         * <p>
         * Matches the {@link com.github.wuic.util.Pipe.Transformer#transform(com.github.wuic.util.Input, com.github.wuic.util.Output, Object)}
         * signature with an additional {@link EngineRequest}.
         * </p>
         *
         * @param request the {@link EngineRequest}
         * @param is the input
         * @param os the output
         * @param nut the object that provides the original input stream
         * @return {@code true} if the input stream has been read and the output stream used, {@code false} otherwise
         * @throws IOException if an I/O error occurs
         */
        boolean transform(Input is, Output os, ConvertibleNut nut, EngineRequest request) throws IOException;
    }

    /**
     * The engine request.
     */
    private final EngineRequest engineRequest;

    /**
     * The transformer which will use the engine request.
     */
    private final RequireEngineRequestTransformer requireEngineRequestTransformer;

    /**
     * Can aggregate transformed stream.
     */
    private final boolean canAggregateTransformedStream;

    /**
     * The order.
     */
    private final int order;

    /**
     * <p>
     * Builds a new instance.
     * </p>
     *
     * @param request the request to wrap
     * @param transformer the delegated transformer
     * @param cats can aggregate transformed stream
     * @param o the order
     */
    public EngineRequestTransformer(final EngineRequest request,
                                    final RequireEngineRequestTransformer transformer,
                                    final boolean cats,
                                    final int o) {
        this.engineRequest = request;
        this.requireEngineRequestTransformer = transformer;
        this.canAggregateTransformedStream = cats;
        this.order = o;
    }

    /**
     * <p>
     * Builds a new instance.
     * </p>
     *
     * @param request the request to wrap
     * @param transformer the delegated transformer
     * @param o the order
     */
    public EngineRequestTransformer(final EngineRequest request, final RequireEngineRequestTransformer transformer, final int o) {
        this(request, transformer, true, o);
    }

    /**
     * <p>
     * Returns the internal {@code RequireEngineRequestTransformer}.
     * </p>
     *
     * @return the wrapped object
     */
    public RequireEngineRequestTransformer getRequireEngineRequestTransformer() {
        return requireEngineRequestTransformer;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean transform(final Input is, final Output os, final ConvertibleNut convertible)
            throws IOException {
        return requireEngineRequestTransformer.transform(is, os, convertible, engineRequest);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canAggregateTransformedStream() {
        return canAggregateTransformedStream;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int order() {
        return order;
    }
}
