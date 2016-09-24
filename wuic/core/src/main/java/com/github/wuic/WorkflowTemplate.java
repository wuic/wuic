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
     * <p>
     * Creates a new instance.
     * </p>
     *
     * @param c the chains
     */
    public WorkflowTemplate(final HeadEngine h, final Map<NutType, ? extends NodeEngine> c) {
        chains = c;
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
}
