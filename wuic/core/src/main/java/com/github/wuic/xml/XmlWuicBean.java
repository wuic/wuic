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


package com.github.wuic.xml;

import javax.xml.bind.annotation.*;

import java.util.List;

/**
 * <p>
 * Represents the root element in wuic.xml file.
 * </p>
 *
 * @author Guillaume DROUET
 * @version 1.1
 * @since 0.4.0
 */
@XmlRootElement(name = "wuic")
@XmlAccessorType(XmlAccessType.FIELD)
public class XmlWuicBean {

    /**
     * Polling interval in seconds.
     */
    @XmlAttribute(name = "polling-interval-seconds")
    private Integer pollingIntervalSeconds;

    /**
     * Some filter builders definitions.
     */
    @XmlElementWrapper(name = "nut-filter-builders")
    @XmlElement(name = "nut-filter-builder")
    private List<XmlBuilderBean> filterBuilders;

    /**
     * Some DAO builders definitions.
     */
    @XmlElementWrapper(name = "nut-dao-builders")
    @XmlElement(name = "nut-dao-builder")
    private List<XmlBuilderBean> daoBuilders;

    /**
     * Some engine builders definitions
     */
    @XmlElementWrapper(name = "engine-builders")
    @XmlElement(name = "engine-builder")
    private List<XmlBuilderBean> engineBuilders;

    /**
     * Some heaps definitions.
     */
    @XmlElementWrapper(name = "heaps")
    @XmlElement(name = "heap")
    private List<XmlHeapBean> heaps;

    /**
     * Some workflow template definitions.
     */
    @XmlElementWrapper(name = "workflow-templates")
    @XmlElement(name = "workflow-template")
    private List<XmlWorkflowTemplateBean> workflowTemplates;

    /**
     * Some binding definitions.
     */
    @XmlElementWrapper(name = "workflows")
    @XmlElement(name = "workflow")
    private List<XmlWorkflowBean> workflows;

    /**
     * <p>
     * Gets the polling interval. If value is not filled in wuic.xml, returns -1.
     * </p>
     *
     * @return the interval
     */
    public Integer getPollingIntervalSeconds() {
        return pollingIntervalSeconds == null ? -1 : pollingIntervalSeconds;
    }

    /**
     * <p>
     * Gets the filter builders.
     * </p>
     *
     * @return the builders
     */
    public List<XmlBuilderBean> getFilterBuilders() {
        return filterBuilders;
    }

    /**
     * <p>
     * Gets the DAO builders.
     * </p>
     *
     * @return the builders
     */
    public List<XmlBuilderBean> getDaoBuilders() {
        return daoBuilders;
    }

    /**
     * <p>
     * Gets the engine builders.
     * </p>
     *
     * @return the builders
     */
    public List<XmlBuilderBean> getEngineBuilders() {
        return engineBuilders;
    }

    /**
     * <p>
     * Gets the heaps.
     * </p>
     *
     * @return the heaps
     */
    public List<XmlHeapBean> getHeaps() {
        return heaps;
    }

    /**
     * <p>
     * Gets the workflows.
     * </p>
     *
     * @return the workflows
     */
    public List<XmlWorkflowTemplateBean> getWorkflowTemplates() {
        return workflowTemplates;
    }

    /**
     * <p>
     * Gets the binding between heap and workflow.
     * </p>
     *
     * @return the binding
     */
    public List<XmlWorkflowBean> getWorkflows() {
        return workflows;
    }


    /**
     * <p>
     * Sets the workflow templates.
     * </p>
     *
     * @param workflowTemplatesList the new list
     */
    public void setWorkflowTemplates(final List<XmlWorkflowTemplateBean> workflowTemplatesList) {
        workflowTemplates = workflowTemplatesList;
    }

    /**
     * <p>
     * Sets the workflows.
     * </p>
     *
     * @param workflowList the new list
     */
    public void setWorkflows(final List<XmlWorkflowBean> workflowList) {
        workflows = workflowList;
    }

    /**
     * <p>
     * Sets the DAO builders.
     * </p>
     *
     * @param daoBuilders the builders
     */
    public void setDaoBuilders(final List<XmlBuilderBean> daoBuilders) {
        this.daoBuilders = daoBuilders;
    }

    /**
     * <p>
     * Sets the engine builders.
     * </p>
     *
     * @param engineBuilders the new builders
     */
    public void setEngineBuilders(final List<XmlBuilderBean> engineBuilders) {
        this.engineBuilders = engineBuilders;
    }
}
