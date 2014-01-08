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
import com.github.wuic.nut.Nut;
import com.github.wuic.util.CollectionUtils;
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
 * @version 1.5
 * @since 0.3.0
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
     * {@link EngineType} that should be skipped during workflow execution.
     */
    private EngineType[] skip;

    /**
     * The timestamp version.
     */
    private String timestampVersion;

    /**
     * <p>
     * Builds a new {@code EngineRequest} with some nuts specific, a specified context path to be used and a workflow ID.
     * </p>
     *
     * @param wId the workflow ID
     * @param n the nuts to be parsed
     * @param other the request to copy
     * @param toSkip the engine's type that should be skipped when request is sent to an engine chain
     */
    public EngineRequest(final String wId, final List<Nut> n, final EngineRequest other, final EngineType ... toSkip) {
        this(wId, other.contextPath, other.timestampVersion, other.heap, n, other.chains, toSkip);
    }

    /**
     * <p>
     * Builds a new {@code EngineRequest} with some specific nuts and a specified context path to be used.
     * </p>
     *
     * @param n the nuts to be parsed
     * @param other the request to copy
     */
    public EngineRequest(final List<Nut> n, final EngineRequest other) {
        this(other.workflowId, other.contextPath, other.timestampVersion, other.heap, n, other.chains, other.skip);
    }


    /**
     * <p>
     * Builds a new {@code EngineRequest} with a specific timestamp identifying nuts version path.
     * </p>
     *
     * @param timestampVersion the specific timestamp version
     * @param other the request to copy
     */
    public EngineRequest(final String timestampVersion, final EngineRequest other) {
        this(other.workflowId, other.contextPath, timestampVersion, other.heap, other.nuts, other.chains, other.skip);
    }

    /**
     * <p>
     * Builds a new {@code EngineRequest} with some nuts specific and a specified heap to be used.
     * </p>
     *
     * @param n the nuts to be parsed
     * @param h the heap
     * @param other the request to copy
     * @param toSkip the engine's type that should be skipped when request is sent to an engine chain
     */
    public EngineRequest(final List<Nut> n, final NutsHeap h, final EngineRequest other, final EngineType[] toSkip) {
        this(other.workflowId, other.contextPath, other.timestampVersion, h, n, other.chains, toSkip);
    }

    /**
     * <p>
     * Builds a new {@code EngineRequest} with some nuts specific and a specified heap to be used.
     * </p>
     *
     * @param n the nuts to be parsed
     * @param h the heap
     * @param other the request to copy
     */
    public EngineRequest(final List<Nut> n, final NutsHeap h, final EngineRequest other) {
        this(other.workflowId, other.contextPath, other.timestampVersion, h, n, other.chains, other.skip);
    }


    /**
     * <p>
     * Builds a new {@code EngineRequest} with a timestamp version which equals to 0 and the nuts retrieved from the specified
     * heap.
     * </p>
     *
     * @param wid the workflow ID
     * @param cp the context root where the generated nuts should be exposed
     * @param h the heap
     * @param c the engine chains
     * @param toSkip some engine types to skip
     */
    public EngineRequest(final String wid,
                         final String cp,
                         final NutsHeap h,
                         final Map<NutType, ? extends Engine> c,
                         final EngineType ... toSkip) {
        this(wid, cp, "", h, new ArrayList<Nut>(h.getNuts()), c, toSkip);
    }

    /**
     * <p>
     * Builds a new {@code EngineRequest} with all elements of the state (attributes) specified in parameter.
     * </p>
     *
     * @param wid the workflow ID
     * @param cp the context root where the generated nuts should be exposed
     * @param tsVersion a timestamp version for nuts
     * @param h the heap
     * @param c the engine chains
     * @param n the nuts.
     * @param toSkip some engine types to skip
     */
    public EngineRequest(final String wid,
                         final String cp,
                         final String tsVersion,
                         final NutsHeap h,
                         final List<Nut> n,
                         final Map<NutType, ? extends Engine> c,
                         final EngineType ... toSkip) {
        nuts = new ArrayList<Nut>(n);
        contextPath = cp;
        heap = h;
        chains = c;
        workflowId = wid;
        timestampVersion = tsVersion;
        skip = new EngineType[toSkip.length];
        System.arraycopy(toSkip, 0, skip, 0, toSkip.length);
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
     * Gets the timestamp version.
     * </p>
     *
     * @return the timestamp version
     */
    public String getTimestampVersion() {
        return timestampVersion;
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

    /**
     * <p>
     * Indicates if a engine of the given type should skip its treatment when this request is submitted.
     * </p>
     *
     * @param engineType the {@link EngineType}
     * @return {@code true} if treatment should be skipped, {@code false} otherwise.
     */
    public boolean shouldSkip(final EngineType engineType) {
        return CollectionUtils.indexOf(engineType, skip) != -1;
    }
}
