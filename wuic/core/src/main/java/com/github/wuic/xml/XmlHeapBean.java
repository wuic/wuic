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


package com.github.wuic.xml;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import java.util.List;

/**
 * <p>
 * This class describes the XML representation of a {@link com.github.wuic.nut.NutsHeap}.
 * </p>
 *
 * @author Guillaume DROUET
 * @version 1.0
 * @since 0.4.0
 */
public class XmlHeapBean {

    /**
     * The ID.
     */
    @XmlAttribute(name = "id")
    private String id;

    /**
     * The ID of the DAO builder.
     */
    @XmlAttribute(name = "dao-builder-id")
    private String daoBuilderId;

    /**
     * The paths representing the nuts of the heap.
     */
    @XmlElement(name = "nut-path")
    private List<String> nutPaths;

    /**
     * Composition from other nested heaps.
     */
    @XmlElement(name = "heap")
    private List<XmlHeapBean> nestedComposition;


    /**
     * Composition from other referenced heaps.
     */
    @XmlElement(name = "heap-id")
    private List<String> referencedComposition;

    /**
     * <p>
     * Gets the paths.
     * </p>
     *
     * @return the paths corresponding to the {@link com.github.wuic.nut.Nut nuts}
     */
    public List<String> getNutPaths() {
        return nutPaths;
    }

    /**
     * <p>
     * Gets the DAO builder's ID.
     * </p>
     *
     * @return an ID identifying a DAO builder
     */
    public String getDaoBuilderId() {
        return daoBuilderId;
    }

    /**
     * <p>
     * Gets the ID.
     * </p>
     *
     * @return the ID identifying the heap
     */
    public String getId() {
        return id;
    }

    /**
     * <p>
     * Gets nested heaps.
     * </p>
     *
     * @return the heaps
     */
    public List<XmlHeapBean> getNestedComposition() {
        return nestedComposition;
    }

    /**
     * <p>
     * Gets referenced heaps.
     * </p>
     *
     * @return the heaps
     */
    public List<String> getReferencedComposition() {
        return referencedComposition;
    }
}
