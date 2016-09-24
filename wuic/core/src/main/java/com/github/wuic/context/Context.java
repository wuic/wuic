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


package com.github.wuic.context;

import com.github.wuic.ProcessContext;
import com.github.wuic.Workflow;
import com.github.wuic.engine.EngineRequest;
import com.github.wuic.engine.EngineRequestBuilder;
import com.github.wuic.engine.EngineType;
import com.github.wuic.engine.HeadEngine;
import com.github.wuic.exception.WorkflowNotFoundException;
import com.github.wuic.exception.WuicException;
import com.github.wuic.nut.ConvertibleNut;
import com.github.wuic.nut.Source;
import com.github.wuic.util.NutUtils;
import com.github.wuic.util.Pipe;
import com.github.wuic.util.UrlProviderFactory;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
 * @since 0.4.0
 */
public class Context implements PropertyChangeListener {

    /**
     * All possible workflow mapped to their ID.
     */
    private final Map<String, Workflow> workflowMap;

    /**
     * All interceptors.
     */
    private final List<ContextInterceptor> interceptors;

    /**
     * The {@link ContextBuilder} which created this instance.
     */
    private final ContextBuilder contextBuilder;

    /**
     * Property change support used to notify new workflow execution statistic report.
     */
    private final PropertyChangeSupport propertyChangeSupport;

    /**
     * Indicates if this context is up to date or not.
     */
    private Boolean upToDate;

    /**
     * <p>
     * Creates a new instance. Package level access to let to the {@link ContextBuilder} the total control on instantiation.
     * </p>
     *
     * @param cb the builder
     * @param wm the workflow map
     * @param interceptorsList some interceptors
     */
    Context(final ContextBuilder cb,
            final Map<String, Workflow> wm,
            final List<ContextInterceptor> interceptorsList) {
        contextBuilder = cb;
        contextBuilder.addExpirationListener(this);
        workflowMap = wm;
        upToDate = true;
        interceptors = interceptorsList;
        propertyChangeSupport = new PropertyChangeSupport(this);
    }

    /**
     * <p>
     * Processes a workflow and returns the resulting nuts. If no workflow is associated to the given ID, then an
     * exception will be thrown.
     * </p>
     *
     * @param workflowId the workflow ID
     * @param contextPath the context path where nuts will be referenced
     * @param urlProviderFactory the {@link UrlProviderFactory}
     * @param processContext the process context
     * @param skip the skipped engines
     * @return the resulting nuts
     * @throws com.github.wuic.exception.WuicException if any exception related to WUIC occurs
     */
    public List<ConvertibleNut> process(final String contextPath,
                                        final String workflowId,
                                        final UrlProviderFactory urlProviderFactory,
                                        final ProcessContext processContext,
                                        final EngineType ... skip)
            throws WuicException {
        return process(contextPath, workflowId, getWorkflow(workflowId), urlProviderFactory, processContext, skip);
    }

    /**
     * <p>
     * Processes a workflow an returns the nut inside the result with a name that equals to the specified one.
     * </p>
     *
     * @param skip the skipped engine types
     * @param contextPath the context path where nuts will be referenced
     * @param wId the workflow ID
     * @param path the nut name
     * @param urlProviderFactory the URL provider
     * @param processContext the process context
     * @return the nut corresponding to the nut name
     * @throws WuicException if workflow fails to be processed
     * @throws IOException if any I/O error occurs
     */
    public ConvertibleNut process(final String contextPath,
                                  final String wId,
                                  final String path,
                                  final UrlProviderFactory urlProviderFactory,
                                  final ProcessContext processContext,
                                  final EngineType ... skip)
            throws WuicException, IOException {
        return process(contextPath, wId, getWorkflow(wId), path, urlProviderFactory, processContext, skip);
    }

    /**
     * <p>
     * Adds a property listener notified when a new {@link com.github.wuic.mbean.WorkflowExecution} is available.
     * The {@code PropertyChangeEvent} passed to the listener provides a {@link WorkflowExecutionEvent} through
     * {@code getNewValue()}.
     * </p>
     *
     * @param propertyChangeListener the property change listener
     */
    public void addPropertyChangeListener(final PropertyChangeListener propertyChangeListener) {
        propertyChangeSupport.addPropertyChangeListener(Event.WORKFLOW_EXECUTION.name(), propertyChangeListener);
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
     * Returns the workflow associated to the given ID.
     * </p>
     *
     * @param workflowId the ID
     * @return the workflow that corresponds to the ID
     * @throws WorkflowNotFoundException if no workflow is associated to the ID.
     */
    public Workflow getWorkflow(final String workflowId) throws WorkflowNotFoundException {
        String wId = workflowId;

        for (final ContextInterceptor i : interceptors) {
            wId = i.beforeGetWorkflow(wId);
        }

        Workflow workflow = workflowMap.get(wId);

        for (final ContextInterceptor i : interceptors) {
            workflow = i.afterGetWorkflow(wId, workflow);
        }

        if (workflow == null) {
            WuicException.throwWorkflowNotFoundException(wId);
        }

        return workflow;
    }

    /**
     * <p>
     * Builds a new request to submit to a chain.
     * </p>
     *
     * @param contextPath the context path
     * @param wId the workflow ID
     * @param workflow the workflow associated to the ID
     * @param path a nut name to particularly retrieve in the result (could be {@code null})
     * @param urlProviderFactory the URL provider
     * @param processContext the process context
     * @param skip the engines to skip
     * @return the created request
     */
    private EngineRequest newRequest(final String contextPath,
                                     final String wId,
                                     final Workflow workflow,
                                     final String path,
                                     final UrlProviderFactory urlProviderFactory,
                                     final ProcessContext processContext,
                                     final EngineType ... skip) {
        EngineRequestBuilder request = new EngineRequestBuilder(wId, workflow.getHeap(), this, contextBuilder.getNutTypeFactory())
                .contextPath(contextPath)
                .chains(workflow.getChains())
                .urlProviderFactory(urlProviderFactory)
                .skip(skip)
                .processContext(processContext);

        for (final ContextInterceptor interceptor : interceptors) {
            if (path == null) {
                request = interceptor.beforeProcess(request);
            } else {
                request = interceptor.beforeProcess(request, path);
            }
        }

        return request.build();
    }

    /**
     * <p>
     * Processes a workflow with its associated ID and returns the resulting nut associated to the given path.
     * </p>
     *
     * @param wId the workflow ID
     * @param skip the skipped engine types
     * @param contextPath the context path where nuts will be referenced
     * @param path the path corresponding to desired nut
     * @param urlProviderFactory the URL provider
     * @param processContext the process context
     * @return the resulting nuts
     * @throws com.github.wuic.exception.WuicException if any exception related to WUIC occurs
     */
    private ConvertibleNut process(final String contextPath,
                                   final String wId,
                                   final Workflow workflow,
                                   final String path,
                                   final UrlProviderFactory urlProviderFactory,
                                   final ProcessContext processContext,
                                   final EngineType ... skip)
            throws IOException, WuicException {
        final EngineRequest request = newRequest(contextPath, wId, workflow, path, urlProviderFactory, processContext, skip);

        ConvertibleNut retval;
        if (workflow.getHead() != null) {
            retval = workflow.getHead().parse(request, path);
        } else {
            retval = NutUtils.findByName(HeadEngine.runChains(request), path);
        }

        for (final ContextInterceptor interceptor : interceptors) {
            retval = interceptor.afterProcess(retval, path);
        }

        if (retval == null) {
            WuicException.throwNutNotFoundException(path, wId);
        } else {
            request.notifyHeapResolutionsTo(propertyChangeSupport);
            registerStatsReporter(retval, new ReportStatsOnReady(wId, request));
        }

        return retval;
    }

    /**
     * <p>
     * Processes a workflow with its associated ID and returns the resulting nuts.
     * </p>
     *
     * @param skip the skipped engine types
     * @param wId the workflow ID
     * @param contextPath the context path where nuts will be referenced
     * @param urlProviderFactory the URL provider
     * @param processContext the process context
     * @return the resulting nuts
     * @throws com.github.wuic.exception.WuicException if any exception related to WUIC occurs
     */
    private List<ConvertibleNut> process(final String contextPath,
                                         final String wId,
                                         final Workflow workflow,
                                         final UrlProviderFactory urlProviderFactory,
                                         final ProcessContext processContext,
                                         final EngineType ... skip)
            throws WuicException {
        final EngineRequest request = newRequest(contextPath, wId, workflow, null, urlProviderFactory, processContext, skip);

        List<ConvertibleNut> retval;

        if (workflow.getHead() != null) {
            retval = workflow.getHead().parse(request);
        } else {
            retval = HeadEngine.runChains(request);
        }

        for (final ContextInterceptor interceptor : interceptors) {
            retval = interceptor.afterProcess(retval);
        }

        final ReportStatsOnReady onReady =  new ReportStatsOnReady(wId, request);

        for (final ConvertibleNut convertibleNut : retval) {
            registerStatsReporter(convertibleNut, onReady);
        }

        request.notifyHeapResolutionsTo(propertyChangeSupport);

        return retval;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void propertyChange(final PropertyChangeEvent evt) {
         // This context is not usable anymore, stop observing to not still referenced anymore
         contextBuilder.removePropertyChangeListener(this);
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

    /**
     * <p>
     * Registers the given {@link ReportStatsOnReady} to be notified when the given nut is transformed.
     * </p>
     *
     * @param convertibleNut the convertible nut
     * @param onReady the callback
     */
    private void registerStatsReporter(final ConvertibleNut convertibleNut, final ReportStatsOnReady onReady) {
        // Source objects are never transformed
        if (!(convertibleNut instanceof Source)) {
            convertibleNut.onReady(onReady, true);
        }

        if (convertibleNut.getReferencedNuts() != null) {
            for (final ConvertibleNut ref : convertibleNut.getReferencedNuts()) {
                registerStatsReporter(ref, onReady);
            }
        }
    }

    /**
     * <p>
     * This {@code OnReady} reports the workflow statistics to the context's listeners once the nut is transformed.
     * </p>
     *
     * @author Guillaume DROUET
     * @since 0.5.3
     */
    private class ReportStatsOnReady implements Pipe.OnReady {

        /**
         * The workflow ID.
         */
        private final String workflowId;

        /**
         * The request initiating the transformation
         */
        private final EngineRequest engineRequest;

        /**
         * <p>
         * Builds a new instance.
         * </p>
         *
         * @param workflowId the workflow ID
         * @param engineRequest the request
         */
        private ReportStatsOnReady(final String workflowId, final EngineRequest engineRequest) {
            this.workflowId = workflowId;
            this.engineRequest = engineRequest;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void ready(final Pipe.Execution e) throws IOException {
            final WorkflowExecutionEvent evt = new WorkflowExecutionEvent(workflowId, engineRequest.getWorkflowStatistics());
            propertyChangeSupport.firePropertyChange(Event.WORKFLOW_EXECUTION.name(), null, evt);
        }
    }
}
