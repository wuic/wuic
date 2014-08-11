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


package com.github.wuic;

import com.github.wuic.engine.EngineRequest;
import com.github.wuic.engine.HeadEngine;
import com.github.wuic.exception.NutNotFoundException;
import com.github.wuic.exception.WorkflowNotFoundException;
import com.github.wuic.exception.WuicException;
import com.github.wuic.nut.*;
import com.github.wuic.util.NutUtils;

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
 * @version 1.2
 * @since 0.4.0
 */
public class Context implements Observer {

    /**
     * All possible workflow mapped to their ID.
     */
    private Map<String, Workflow> workflowMap;

    /**
     * All interceptors.
     */
    private List<ContextInterceptor> interceptors;

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
     * @param interceptorsList some interceptors
     */
    Context(final ContextBuilder cb, final Map<String, Workflow> wm, final List<ContextInterceptor> interceptorsList) {
        contextBuilder = cb;
        contextBuilder.addObserver(this);
        workflowMap = wm;
        upToDate = true;
        interceptors = interceptorsList;
    }

    /**
     * <p>
     * Processes a workflow and returns the resulting nuts. If no workflow is associated to the given ID, then an
     * exception will be thrown.
     * </p>
     *
     * @param workflowId the workflow ID
     * @param contextPath the context path where nuts will be referenced
     * @return the resulting nuts
     * @throws com.github.wuic.exception.WuicException if any exception related to WUIC occurs
     */
    public List<Nut> process(final String contextPath, final String workflowId) throws WuicException {
        return process(contextPath, workflowId, getWorkflow(workflowId));
    }

    /**
     * <p>
     * Processes a workflow an returns the nut inside the result with a name that equals to the specified one.
     * </p>
     *
     * @param contextPath the context path where nuts will be referenced
     * @param wId the workflow ID
     * @param path the nut name
     * @return the nut corresponding to the nut name
     * @throws WuicException if workflow fails to be processed
     */
    public Nut process(final String contextPath, final String wId, final String path) throws WuicException {
        return process(contextPath, wId, getWorkflow(wId), path);
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
     * <p>
     * Returns the workflow associated to the given ID
     * </p>
     *
     * @param workflowId the ID
     * @return the workflow that corresponds to the ID
     * @throws WorkflowNotFoundException if no workflow is associated to the ID.
     */
    private Workflow getWorkflow(final String workflowId) throws WorkflowNotFoundException {
        String wId = workflowId;

        for (final ContextInterceptor i : interceptors) {
            wId = i.beforeGetWorkflow(wId);
        }

        Workflow workflow = workflowMap.get(wId);

        for (final ContextInterceptor i : interceptors) {
            workflow = i.afterGetWorkflow(wId, workflow);
        }

        if (workflow == null) {
            throw new WorkflowNotFoundException(wId);
        } else {
            return workflow;
        }
    }

    /**
     * <p>
     * Processes a workflow with its associated ID and returns the resulting nut associated to the given path.
     * </p>
     *
     * @param wId the workflow ID
     * @param contextPath the context path where nuts will be referenced
     * @param path the path corresponding to desired nut
     * @return the resulting nuts
     * @throws com.github.wuic.exception.WuicException if any exception related to WUIC occurs
     */
    private Nut process(final String contextPath, final String wId, final Workflow workflow, final String path) throws WuicException {
        EngineRequest request = new EngineRequest(wId, contextPath, workflow.getHeap(), workflow.getHeap().getNuts(), workflow.getChains(), "");

        for (final ContextInterceptor interceptor : interceptors) {
            request = interceptor.beforeProcess(request, path);
        }

        Nut retval;
        if (workflow.getHead() != null) {
            retval = workflow.getHead().parse(request, path);
        } else {
            retval = NutUtils.findByName(HeadEngine.runChains(request, Boolean.FALSE), path);
        }

        for (final ContextInterceptor interceptor : interceptors) {
            retval = interceptor.afterProcess(retval, path);
        }

        if (retval == null) {
            throw new NutNotFoundException(path, wId);
        } else {
            return retval;
        }
    }

    /**
     * <p>
     * Processes a workflow with its associated ID and returns the resulting nuts.
     * </p>
     *
     * @param wId the workflow ID
     * @param contextPath the context path where nuts will be referenced
     * @return the resulting nuts
     * @throws com.github.wuic.exception.WuicException if any exception related to WUIC occurs
     */
    private List<Nut> process(final String contextPath, final String wId, final Workflow workflow) throws WuicException {
        EngineRequest request = new EngineRequest(wId, contextPath, workflow.getHeap(), workflow.getHeap().getNuts(), workflow.getChains(), "");

        for (final ContextInterceptor interceptor : interceptors) {
            request = interceptor.beforeProcess(request);
        }

        List<Nut> retval;

        if (workflow.getHead() != null) {
            retval = workflow.getHead().parse(request);
        } else {
            retval = HeadEngine.runChains(request, Boolean.FALSE);
        }

        for (final ContextInterceptor interceptor : interceptors) {
            retval = interceptor.afterProcess(retval);
        }

        return retval;
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

    /**
     * <p>
     * Returns the workflow IDs.
     * </p>
     *
     * @return the IDs
     */
    public Set<String> workflowIds() {
        return workflowMap.keySet();
    }
}
