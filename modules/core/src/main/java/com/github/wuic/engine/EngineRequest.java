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


package com.github.wuic.engine;

import com.github.wuic.NutType;
import com.github.wuic.nut.NutsHeap;
import com.github.wuic.exception.wrapper.StreamException;
import com.github.wuic.nut.Nut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * This class represents a request to be indicated to an engine.
 * </p>
 *
 * <p>
 * The user which invokes {@link Engine#parse(EngineRequest)} should indicates in the parameter the nuts
 * to be parsed and the context path to use to expose the generated nuts.
 * </p>
 *
 * @author Guillaume DROUET
 * @since 0.3.0
 * @version 1.2
 */
public final class EngineRequest {

    /**
     * Logger.
     */
    private final Logger log = LoggerFactory.getLogger(getClass());

    /**
     * The nuts.
     */
    private List<Nut> nuts;

    /**
     * The context path.
     */
    private String contextPath;

    /**
     * The heap.
     */
    private NutsHeap heap;

    /**
     * The workflow ID.
     */
    private String workflowId;

    /**
     * The engine chains for each type.
     */
    private Map<NutType, ? extends Engine> chains;

    /**
     * <p>
     * Builds a new {@code EngineRequest} with some nuts specific, a specified context path to be used and a workflow ID.
     * </p>
     *
     * @param wId the workflow ID
     * @param n the nuts to be parsed
     * @param other the request to copy
     */
    public EngineRequest(final String wId, final List<Nut> n, final EngineRequest other) {
        nuts = n;
        contextPath = other.getContextPath();
        heap = other.getHeap();
        chains = other.chains;
        workflowId = wId;
    }

    /**
     * <p>
     * Builds a new {@code EngineRequest} with some nuts specific and a specified context path to be used.
     * </p>
     *
     * @param n the nuts to be parsed
     * @param other the request to copy
     */
    public EngineRequest(final List<Nut> n, final EngineRequest other) {
        nuts = n;
        contextPath = other.getContextPath();
        heap = other.getHeap();
        chains = other.chains;
        workflowId = other.workflowId;
    }

    /**
     * <p>
     * Builds a new {@code EngineRequest} with a heap, engines chains and a specified context path to be used.
     * </p>
     *
     * @param wid the workflow ID
     * @param cp the context root where the generated nuts should be exposed
     * @param g the heap
     * @param c the engine chains
     * @throws StreamException if an I/O error occurs while getting nuts
     */
    public EngineRequest(final String wid, final String cp, final NutsHeap g, final Map<NutType, ? extends Engine> c) throws StreamException {
        nuts = new ArrayList<Nut>(g.getNuts());
        contextPath = cp;
        heap = g;
        chains = c;
        workflowId = wid;
    }

    /**
     * <p>
     * Gets the nuts.
     * </p>
     *
     * @return the nuts
     */
    public List<Nut> getNuts() {
        return nuts;
    }

    /**
     * <p>
     * Gets the context path.
     * </p>
     *
     * @return the context path
     */
    public String getContextPath() {
        return contextPath;
    }

    /**
     * <p>
     * Returns the heap.
     * </p>
     *
     * @return the heap
     */
    public NutsHeap getHeap() {
        return heap;
    }

    /**
     * <p>
     * Gets the workflow ID.
     * </p>
     *
     * @return the workflow ID.
     */
    public String getWorkflowId() {
        return workflowId;
    }

    /**
     * <p>
     * Gets the chains which can treat nuts of the given {@link NutType}.
     * </p>
     *
     * @param nutType the nut type
     * @return the chains that can treat this nut type
     */
    public Engine getChainFor(final NutType nutType) {
        final Engine retval = chains.get(nutType);
        log.warn("No chain exists for the heap '{}' and the nut type {}.", heap.getId(), nutType.name());
        return retval;
    }
}
