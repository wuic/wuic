/*
 * "Copyright (c) 2015   Capgemini Technology Services (hereinafter "Capgemini")
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


package com.github.wuic.context;

import com.github.wuic.ProcessContext;
import com.github.wuic.config.ObjectBuilder;
import com.github.wuic.engine.Engine;
import com.github.wuic.nut.filter.NutFilter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * Internal class used to track settings associated to a particular tag.
 * </p>
 *
 * @author Guillaume DROUET
 * @version 1.1
 * @since 0.4.0
 */
public class ContextSetting {

    /**
     * The process context.
     */
    private ProcessContext processContext;

    /**
     * All DAO registration with their {@link com.github.wuic.config.ObjectBuilder} associated to their builder ID.
     */
    private Map<String, ContextBuilder.NutDaoRegistration> nutDaoMap = new HashMap<String, ContextBuilder.NutDaoRegistration>();

    /**
     * All {@link com.github.wuic.nut.filter.NutFilter daos} associated to their builder ID.
     */
    private Map<String, NutFilter> nutFilterMap = new HashMap<String, NutFilter>();

    /**
     * All {@link com.github.wuic.config.ObjectBuilder} building {@link com.github.wuic.engine.Engine} associated to their builder ID.
     */
    private Map<String, ObjectBuilder<Engine>> engineMap = new HashMap<String, ObjectBuilder<Engine>>();

    /**
     * All {@link com.github.wuic.context.ContextBuilder.HeapRegistration heaps} associated to their ID.
     */
    private Map<String, ContextBuilder.HeapRegistration> nutsHeaps = new HashMap<String, ContextBuilder.HeapRegistration>();

    /**
     * All {@link com.github.wuic.WorkflowTemplate templates} {@link ContextBuilder.WorkflowTemplateRegistration registration}
     * associated to their ID.
     */
    private Map<String, ContextBuilder.WorkflowTemplateRegistration> templates = new HashMap<String, ContextBuilder.WorkflowTemplateRegistration>();

    /**
     * All {@link com.github.wuic.Workflow workflows} associated to their ID.
     */
    private Map<String, ContextBuilder.WorkflowRegistration> workflowMap = new HashMap<String, ContextBuilder.WorkflowRegistration>();

    /**
     * All {@link ContextInterceptor interceptors}.
     */
    private List<ContextInterceptor> interceptorsList = new ArrayList<ContextInterceptor>();

    /**
     * <p>
     * Sets the process context.
     * </p>
     *
     * @param pc the process context
     */
    public void setProcessContext(final ProcessContext pc) {
        this.processContext = pc;
    }

    /**
     * <p>
     * Gets the process context.
     * </p>
     *
     * @return the process context
     */
    public ProcessContext getProcessContext() {
        return processContext;
    }

    /**
     * <p>
     * Gets the {@link com.github.wuic.nut.dao.NutDao} associated to an ID.
     * </p>
     *
     * @return the map
     */
    Map<String, ContextBuilder.NutDaoRegistration> getNutDaoMap() {
        return nutDaoMap;
    }

    /**
     * <p>
     * Gets the {@link NutFilter} associated to an ID.
     * </p>
     *
     * @return the map
     */
    Map<String, NutFilter> getNutFilterMap() {
        return nutFilterMap;
    }

    /**
     * <p>
     * Gets the {@link com.github.wuic.config.ObjectBuilder} associated to an ID.
     * </p>
     *
     * @return the map
     */
    Map<String, ObjectBuilder<Engine>> getEngineMap() {
        return engineMap;
    }

    /**
     * <p>
     * Gets the {@link com.github.wuic.context.ContextBuilder.HeapRegistration} associated to an ID.
     * </p>
     *
     * @return the map
     */
    Map<String, ContextBuilder.HeapRegistration> getNutsHeaps() {
        return nutsHeaps;
    }

    /**
     * <p>
     * Gets the {@link com.github.wuic.context.ContextBuilder.WorkflowRegistration} associated to an ID.
     * </p>
     *
     * @return the map
     */
    Map<String, ContextBuilder.WorkflowRegistration> getWorkflowMap() {
        return workflowMap;
    }

    /**
     * <p>
     * Gets the {@link com.github.wuic.context.ContextBuilder.WorkflowTemplateRegistration} associated to an ID.
     * </p>
     *
     * @return the map
     */
    Map<String, ContextBuilder.WorkflowTemplateRegistration> getTemplateMap() {
        return templates;
    }

    /**
     * <p>
     * Gets the {@link ContextInterceptor interceptors}.
     * </p>
     *
     * @return the list
     */
    List<ContextInterceptor> getInterceptorsList() {
        return interceptorsList;
    }
}

