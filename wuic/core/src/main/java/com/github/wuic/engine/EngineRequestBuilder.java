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


package com.github.wuic.engine;

import com.github.wuic.NutType;
import com.github.wuic.nut.ConvertibleNut;
import com.github.wuic.nut.Nut;
import com.github.wuic.nut.NutsHeap;
import com.github.wuic.nut.PipedConvertibleNut;
import com.github.wuic.util.CollectionUtils;
import com.github.wuic.util.UrlProviderFactory;
import com.github.wuic.util.UrlUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * This class builds {@link EngineRequest} objects.
 * </p>
 *
 * @author Guillaume DROUET
 * @version 1.0
 * @since 0.5.0
 */
public final class EngineRequestBuilder {

    /**
     * Logger.
     */
    private final Logger log = LoggerFactory.getLogger(getClass());

    /**
     * The nuts.
     */
    private List<ConvertibleNut> nuts;

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
     * The key this request uses to compute the key.
     */
    private String workflowKey;

    /**
     * The engine chains for each type.
     */
    private Map<NutType, NodeEngine> chains;

    /**
     * {@link EngineType} that should be skipped during workflow execution.
     */
    private EngineType[] skip;

    /**
     * The prefix path of created nuts.
     */
    private String prefixCreatedNut;

    /**
     * The URL provider.
     */
    private UrlProviderFactory urlProviderFactory;

    /**
     * Indicates if this request requires a best effort or not.
     */
    private boolean bestEffort;

    /**
     * <p>
     * Builds a new instance with mandatory workflow ID and {@link NutsHeap}.
     * </p>
     *
     * @param wId the workflow id
     * @param h the heap
     */
    public EngineRequestBuilder(final String wId, final NutsHeap h) {
        workflowId(wId);
        heap(h);
        workflowKey("");
        prefixCreatedNut("");
        contextPath("");
        bestEffort = false;
    }

    /**
     * <p>
     * Builds a new instance with the {@link EngineRequestBuilder} wrapped inside the given {@link EngineRequest request}.
     * </p>
     *
     * @param request the request
     */
    public EngineRequestBuilder(final EngineRequest request) {
        final EngineRequestBuilder other = request.getBuilder();
        nuts = other.nuts;
        contextPath = other.contextPath;
        heap = other.heap;
        workflowId = other.workflowId;
        workflowKey = other.workflowKey;
        chains = other.chains;
        skip = other.skip;
        prefixCreatedNut = other.prefixCreatedNut;
        urlProviderFactory = other.urlProviderFactory;
        bestEffort = other.bestEffort;
    }

    /**
     * <p>
     * Disables best effort for this request.
     * </p>
     *
     * @return this
     */
    public EngineRequestBuilder disableBestEffort() {
        bestEffort = false;
        return this;
    }

    /**
     * <p>
     * Enables best effort for this request.
     * </p>
     *
     * @return this
     */
    public EngineRequestBuilder bestEffort() {
        bestEffort = true;
        return this;
    }

    /**
     * <p>
     * Sets the nuts. Each nut will be wrapped in a {@link PipedConvertibleNut} to be exposed as {@link ConvertibleNut}.
     * </p>
     *
     * @param n the nuts
     * @return this
     */
    public EngineRequestBuilder nuts(final List<? extends Nut> n) {
        nuts = new ArrayList<ConvertibleNut>(n.size());

        for (final Nut nut : n) {
            nuts.add(new PipedConvertibleNut(nut));
        }

        return this;
    }

    /**
     * <p>
     * Sets the workflow key.
     * </p>
     *
     * @param wk the key
     * @return this
     */
    public EngineRequestBuilder workflowKey(final String wk) {
        workflowKey = wk;
        return this;
    }

    /**
     * <p>
     * Sets the workflow ID.
     * </p>
     *
     * @param wId the workflow ID
     * @return this
     */
    public EngineRequestBuilder workflowId(final String wId) {
        workflowId = wId;
        return this;
    }

    /**
     * <p>
     * Sets the prefix of created nut.
     * </p>
     *
     * @param pcn the prefix
     * @return this
     */
    public EngineRequestBuilder prefixCreatedNut(final String pcn) {
        prefixCreatedNut = pcn;
        return this;
    }

    /**
     * <p>
     * Sets the {@link UrlProviderFactory}.
     * </p>
     *
     * @param upf the factory
     * @return this
     */
    public EngineRequestBuilder urlProviderFactory(final UrlProviderFactory upf) {
        urlProviderFactory = upf;
        return this;
    }

    /**
     * <p>
     * Sets the skipped {@link EngineType}.
     * </p>
     *
     * @param toSkip the skipped types
     * @return this
     */
    public EngineRequestBuilder skip(final EngineType... toSkip) {
        skip = new EngineType[toSkip.length];
        System.arraycopy(toSkip, 0, skip, 0, toSkip.length);
        return this;
    }

    /**
     * <p>
     * Sets the context path.
     * </p>
     *
     * @param cp the context path
     * @return this
     */
    public EngineRequestBuilder contextPath(final String cp) {
        contextPath = cp;
        return this;
    }

    /**
     * <p>
     * Sets the {@link NutsHeap}.
     * </p>
     *
     * @param h the heap
     * @return this
     */
    public EngineRequestBuilder heap(final NutsHeap h) {
        heap = h;
        return this;
    }

    /**
     * <p>
     * Sets the chains of {@link NodeEngine engines} for each {@link NutType type}.
     * </p>
     *
     * @param c the chains
     * @return this
     */
    public EngineRequestBuilder chains(final Map<NutType, ? extends NodeEngine> c) {
        for (final Map.Entry<NutType, ? extends NodeEngine> chain : c.entrySet()) {
            chain(chain.getKey(), chain.getValue());
        }

        return this;
    }

    /**
     * <p>
     * Sets the chains of {@link NodeEngine engines} for each {@link NutType type}.
     * </p>
     *
     * @param nutType the type
     * @param nodeEngine the chain's root
     * @return this
     */
    public EngineRequestBuilder chain(final NutType nutType, final NodeEngine nodeEngine) {
        if (chains == null) {
            chains = new HashMap<NutType, NodeEngine>();
        }

        chains.put(nutType, nodeEngine);

        return this;
    }

    /**
     * <p>
     * Builds a bew instance. Default state is applied here if some attributes have not been initialized.
     * </p>
     *
     * @return the built {@link EngineRequest}
     */
    public EngineRequest build() {
        if (nuts == null) {
            nuts(heap.getNuts());
        }

        if (urlProviderFactory == null) {
            urlProviderFactory = UrlUtils.urlProviderFactory();
        }

        if (skip == null) {
            skip();
        }

        if (chains == null) {
            chains = new HashMap<NutType, NodeEngine>();
        }

        return new EngineRequest(this);
    }

    /**
     * <p>
     * Returns the prefix created nut
     * </p>
     *
     * @return the created nut
     */
    public String getPrefixCreatedNut() {
        return prefixCreatedNut;
    }

    /**
     * <p>
     * Gets the nuts.
     * </p>
     *
     * @return the nuts
     */
    public List<ConvertibleNut> getNuts() {
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
    public NodeEngine getChainFor(final NutType nutType) {
        final NodeEngine retval = chains.get(nutType);

        if (retval == null) {
            log.warn("No chain exists for the heap '{}' and the nut type {}.", heap.getId(), nutType.name());
        }

        return retval;
    }

    /**
     * <p>
     * Indicates if an engine of the given type should skip its treatment when this request is submitted.
     * </p>
     *
     * @param engineType the {@link EngineType}
     * @return {@code true} if treatment should be skipped, {@code false} otherwise.
     */
    public boolean shouldSkip(final EngineType engineType) {
        return CollectionUtils.indexOf(engineType, skip) != -1;
    }

    /**
     * <p>
     * Gets the {@link UrlProviderFactory}.
     * </p>
     *
     * @return the factory
     */
    public UrlProviderFactory getUrlProviderFactory() {
        return urlProviderFactory;
    }

    /**
     * <p>
     * Indicates if this request requires best effort.
     * </p>
     *
     * @return {@code true} in case of best effort, {@code false} otherwise
     */
    public boolean isBestEffort() {
        return bestEffort;
    }

    /**
     * <p>
     * Gets the workflow key. Package access for {@link EngineRequest} only.
     * </p>
     *
     * @return the key
     */
    String getWorkflowKey() {
        return workflowKey;
    }

    /**
     * <p>
     * Gets the skipped {@link EngineType types}. Package access for {@link EngineRequest} only.
     * </p>
     *
     * @return the skipped engine types
     */
    EngineType[] getSkip() {
        return skip;
    }
}
