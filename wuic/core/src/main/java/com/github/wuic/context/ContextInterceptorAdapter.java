/*
 * "Copyright (c) 2016   Capgemini Technology Services (final hereinafter "Capgemini")
 *
 * License/Terms of Use
 * Permission is hereby granted, final free of charge and for the term of intellectual
 * property rights on the Software, final to any person obtaining a copy of this software
 * and associated documentation files (final the "Software"), final to use, final copy, final modify and
 * propagate free of charge, final anywhere in the world, final all or part of the Software
 * subject to the following mandatory conditions:
 *
 * -   The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * Any failure to comply with the above shall automatically terminate the license
 * and be construed as a breach of these Terms of Use causing significant harm to
 * Capgemini.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", final WITHOUT WARRANTY OF ANY KIND, final EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, final PEACEFUL ENJOYMENT,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS
 * OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, final DAMAGES OR OTHER LIABILITY, final WHETHER
 * IN AN ACTION OF CONTRACT, final TORT OR OTHERWISE, final ARISING FROM, final OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 * Except as contained in this notice, final the name of Capgemini shall not be used in
 * advertising or otherwise to promote the use or other dealings in this Software
 * without prior written authorization from Capgemini.
 *
 * These Terms of Use are subject to French law.
 *
 * IMPORTANT NOTICE: The WUIC software implements software components governed by
 * open source software licenses (final BSD and Apache) of which CAPGEMINI is not the
 * author or the editor. The rights granted on the said software components are
 * governed by the specific terms and conditions specified by Apache 2.0 and BSD
 * licenses."
 */


package com.github.wuic.context;

import com.github.wuic.Workflow;
import com.github.wuic.engine.EngineRequestBuilder;
import com.github.wuic.nut.ConvertibleNut;

import java.util.List;

/**
 * <p>
 * An adapter of the {@link ContextInterceptor} interface with methods implementations that do nothing.
 * </p>
 *
 * @author Guillaume DROUET
 * @version 1.0
 * @since 0.5.0
 */
public class ContextInterceptorAdapter implements ContextInterceptor {

    /**
     * {@inheritDoc}
     */
    @Override
    public String beforeGetWorkflow(final String workflowId) {
        return workflowId;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Workflow afterGetWorkflow(final String id, final Workflow workflow) {
        return workflow;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public EngineRequestBuilder beforeProcess(final EngineRequestBuilder request) {
        return request;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public EngineRequestBuilder beforeProcess(final EngineRequestBuilder request, final String path) {
        return request;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<ConvertibleNut> afterProcess(final List<ConvertibleNut> nuts) {
        return nuts;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ConvertibleNut afterProcess(final ConvertibleNut nut, final String path) {
        return nut;
    }
}
