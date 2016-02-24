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


package com.github.wuic;

import com.github.wuic.engine.HeadEngine;
import com.github.wuic.engine.NodeEngine;
import com.github.wuic.nut.dao.NutDao;

import java.util.Map;

/**
 * <p>
 * A workflow template is the base to build a {@link Workflow} with all its characteristics expects the heap to use.
 * </p>
 *
 * @author Guillaume DROUET
 * @since 0.4.3
 */
public class WorkflowTemplate {

    /**
     * The head of this template's chains.
     */
    private HeadEngine head;

    /**
     * All chains for each {@link com.github.wuic.NutType}.
     */
    private Map<NutType, ? extends NodeEngine> chains;

    /**
     * DAO where processing result will be saved.
     */
    private NutDao[] stores;

    /**
     * <p>
     * Creates a new instance.
     * </p>
     *
     * @param c the chains
     * @param store the DAO stores
     */
    public WorkflowTemplate(final HeadEngine h, final Map<NutType, ? extends NodeEngine> c, final NutDao... store) {
        chains = c;
        stores = store;
        head = h;
    }

    /**
     * <p>
     * Gets the {@link HeadEngine} of this workflow.
     * </p>
     *
     * @return the head, {@code null} is no engine has been set
     */
    public HeadEngine getHead() {
        return head;
    }

    /**
     * <p>
     * Gets the chains.
     * </p>
     *
     * @return the chains
     */
    public Map<NutType, ? extends NodeEngine> getChains() {
        return chains;
    }

    /**
     * <p>
     * Gets the DAO stores.
     * </p>
     *
     * @return the stores
     */
    public NutDao[] getStores() {
        return stores;
    }
}
