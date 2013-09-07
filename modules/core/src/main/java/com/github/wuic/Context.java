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


package com.github.wuic;

import com.github.wuic.engine.Engine;
import com.github.wuic.engine.EngineRequest;
import com.github.wuic.exception.WorkflowNotFoundException;
import com.github.wuic.exception.WuicException;
import com.github.wuic.nut.Nut;

import java.util.*;

/**
 * <p>
 * The context is the core element of WUIC which allows to process nuts inside workflow.
 * </p>
 *
 * <p>
 * It is built thank to a {@link ContextBuilder} and is linked to it during it life cycles to know if the context is
 * up to date regarding configuration changes since it has been built.
 * </p>
 *
 * @author Guillaume DROUET
 * @version 1.0
 * @since 0.4.0
 */
public class Context implements Observer {

    /**
     * All possible workflow mapped to their ID.
     */
    private Map<String, Workflow> workflowMap;

    /**
     * Indicates if this context is up to date or not.
     */
    private Boolean upToDate;

    /**
     * The {@link ContextBuilder} which created this instance.
     */
    private ContextBuilder contextBuilder;

    /**
     * <p>
     * Creates a new instance. Package level access to let to the {@link ContextBuilder} the total control on instantiation.
     * </p>
     *
     * @param cb the builder
     * @param wm the workflow map
     */
    Context(final ContextBuilder cb, final Map<String, Workflow> wm) {
        contextBuilder = cb;
        contextBuilder.addObserver(this);
        workflowMap = wm;
        upToDate = true;
    }

    /**
     * <p>
     * Processes a workflow and returns the resulting nuts.
     * </p>
     *
     * @param workflowId the workflow ID
     * @param contextPath the context path where nuts will be referenced
     * @return the resulting nuts
     * @throws com.github.wuic.exception.WuicException if any exception related to WUIC occurs
     */
    public List<Nut> process(final String workflowId, final String contextPath) throws WuicException {
        final Workflow workflow = workflowMap.get(workflowId);

        if (workflow == null) {
            throw new WorkflowNotFoundException(workflowId);
        }

        final EngineRequest request = new EngineRequest(workflowId, contextPath, workflow.getHeap(), workflow.getChains());
        final Engine chain = request.getChainFor(workflow.getHeap().getNutType());

        if (chain == null) {
            return new ArrayList<Nut>(workflow.getHeap().getNuts());
        } else {
            return chain.parse(request);
        }
    }

    /**
     * <p>
     * Indicates if this context is up to date regarding the changes performed on the builder that built it.
     * </p>
     *
     * @return {@code true} if this context is up to date, {@code false} if the builder has been modified since the
     * context has been generated
     */
    public Boolean isUpToDate() {
        return upToDate;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void update(final Observable o, final Object arg) {

        // This context is not usable anymore, stop observing to not still referenced anymore
        contextBuilder.deleteObserver(this);
        upToDate = false;
    }
}
