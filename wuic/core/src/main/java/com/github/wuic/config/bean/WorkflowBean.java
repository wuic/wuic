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


package com.github.wuic.config.bean;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

/**
 * <p>
 * This class corresponds to the bean representation of a {@link com.github.wuic.Workflow}.
 * </p>
 *
 * @author Guillaume DROUET
 * @since 0.4.3
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class WorkflowBean {

    /**
     * The workflow ID.
     */
    @XmlAttribute(name = "id")
    private String id;

    /**
     * The workflow prefix for ID.
     */
    @XmlAttribute(name = "id-prefix")
    private String idPrefix;

    /**
     * The heap ID pattern.
     */
    @XmlAttribute(name = "heap-id-pattern")
    private String heapIdPattern;

    /**
     * The workflow ID to copy.
     */
    @XmlAttribute(name = "workflow-template-id")
    private String workflowTemplateId;

    /**
     * <p>
     * Gets the ID prefix (id attribute should be {@code null}).
     * </p>
     *
     * @return the string prefixing the workflow ID
     */
    public String getIdPrefix() {
        return idPrefix;
    }

    /**
     * <p>
     * Sets the prefix ID.
     * </p>
     *
     * @param prefix the prefix ID
     */
    public void setIdPrefix(final String prefix) {
        idPrefix = prefix;
    }

    /**
     * <p>
     * Gets the ID (idPrefix attribute should be {@code null}).
     * </p>
     *
     * @return the ID identifying the workflow
     */
    public String getId() {
        return id;
    }

    /**
     * <p>
     * Gets the heap ID.
     * </p>
     *
     * @return the heap ID
     */
    public String getHeapIdPattern() {
        return heapIdPattern;
    }

    /**
     * <p>
     * Sets the heap ID.
     * </p>
     *
     * @param pattern the heap ID pattern
     */
    public void setHeapIdPattern(final String pattern) {
        heapIdPattern = pattern;
    }

    /**
     * <p>
     * Sets the workflow template ID.
     * </p>
     *
     * @param tplId the workflow template ID
     */
    public void setWorkflowTemplateId(final String tplId) {
        workflowTemplateId = tplId;
    }

    /**
     * <p>
     * Gets the workflow ID.
     * </p>
     *
     * @return the workflow ID
     */
    public String getWorkflowTemplateId() {
        return workflowTemplateId;
    }
}
