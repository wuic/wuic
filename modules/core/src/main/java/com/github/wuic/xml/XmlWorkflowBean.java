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


package com.github.wuic.xml;

import javax.xml.bind.annotation.*;

import java.util.List;

/**
 * <p>
 * This class corresponds to the XML representation of a {@link com.github.wuic.Workflow}.
 * </p>
 *
 * @author Guillaume DROUET
 * @version 1.1
 * @since 0.4.0
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class XmlWorkflowBean {

    /**
     * The ID prefix.
     */
    @XmlAttribute(name = "id-prefix")
    private String idPrefix;

    /**
     * The heap ID pattern.
     */
    @XmlAttribute(name = "heap-id-pattern")
    private String heapIdPattern;

    /**
     * Include default engine.
     */
    @XmlAttribute(name = "use-default-engines")
    private Boolean useDefaultEngines;

    /**
     * The list of engine builders ID.
     */
    @XmlElementWrapper(name = "engine-chain")
    @XmlElement(name = "engine-builder-id")
    private List<String> engineBuilderIds;

    /**
     * The list of default engine builders ID to exclude.
     */
    @XmlElementWrapper(name = "without")
    @XmlElement(name = "engine-builder-id")
    private List<String> withoutEngineBuilderIds;

    /**
     * Some DAO where to store process result.
     */
    @XmlElementWrapper(name = "store-to")
    @XmlElement(name = "dao-builder-id")
    private List<String> daoBuilderIds;

    /**
     * <p>
     * Gets the ID.
     * </p>
     *
     * @return the ID identifying the workflow
     */
    public String getIdPrefix() {
        return idPrefix;
    }

    /**
     * <p>
     * Gets the DAO builders ID.
     * </p>
     *
     * @return the IDs identifying the builder's DAO to use when storing process result
     */
    public List<String> getDaoBuilderIds() {
        return daoBuilderIds;
    }

    /**
     * <p>
     * Gets the engine builders ID.
     * </p>
     *
     * @return the IDs identifying the builder's engine to use to process nuts
     */
    public List<String> getEngineBuilderIds() {
        return engineBuilderIds;
    }

    /**
     * <p>
     * Gets the engine builders ID to be excluded.
     * </p>
     *
     * @return the IDs identifying the builder's engine to exclude
     */
    public List<String> getWithoutEngineBuilderIds() {
        return withoutEngineBuilderIds;
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
     * Indicates if the chain should include default engines.
     * </p>
     *
     * @return {@code true} if we include default engines or if value is {@code null}, {@link false} otherwise
     */
    public Boolean getUseDefaultEngines() {
        return useDefaultEngines == null ? Boolean.TRUE : useDefaultEngines;
    }

    /**
     * <p>
     * Sets the engine builder IDs.
     * </p>
     *
     * @param engineBuilderIds the IDs
     */
    public void setEngineBuilderIds(final List<String> engineBuilderIds) {
        this.engineBuilderIds = engineBuilderIds;
    }

    /**
     * <p>
     * Sets the use of default engines.
     * </p>
     *
     * @param useDefaultEngines the use of default engines
     */
    public void setUseDefaultEngines(final Boolean useDefaultEngines) {
        this.useDefaultEngines = useDefaultEngines;
    }

    /**
     * <p>
     * Sets the heap ID pattern.
     * </p>
     *
     * @param heapIdPattern the pattern
     */
    public void setHeapIdPattern(final String heapIdPattern) {
        this.heapIdPattern = heapIdPattern;
    }

    /**
     * <p>
     * Sets the ID prefix.
     * </p>
     *
     * @param idPrefix the new ID prefix
     */
    public void setIdPrefix(final String idPrefix) {
        this.idPrefix = idPrefix;
    }
}
