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


package com.github.wuic;

import com.github.wuic.engine.Engine;
import com.github.wuic.engine.core.CssInspectorEngineBuilder;
import com.github.wuic.engine.core.ImageAggregatorEngineBuilder;
import com.github.wuic.engine.core.ImageCompressorEngineBuilder;
import com.github.wuic.engine.core.TextAggregatorEngineBuilder;
import com.github.wuic.engine.impl.embedded.CGCompositeEngine;
import com.github.wuic.exception.BuilderPropertyNotSupportedException;
import com.github.wuic.exception.wrapper.BadArgumentException;
import com.github.wuic.exception.wrapper.StreamException;
import com.github.wuic.factory.EngineBuilder;
import com.github.wuic.nut.NutDao;
import com.github.wuic.nut.NutDaoBuilder;
import com.github.wuic.nut.NutsHeap;
import com.github.wuic.util.GenericBuilder;

import java.util.Arrays;

import java.util.Map;
import java.util.HashMap;
import java.util.Observable;

/**
 * <p>
 * This builder can be configured to build contexts in an expected state by the user.
 * </p>
 *
 * <p>
 * The builder tracks all settings by associated to them a tag. With that tag, the user is able to delete
 * all settings defined at a moment. Example :
 * <pre>
 *     final ContextBuilder contextBuilder = new ContextBuilder();
 *
 *     // Create a context with some settings tagged as "custom"
 *     final Context ctx = contextBuilder.tag("custom")
 *                  .nutDaoBuilder("FtpNutDaoBuilder", daoBuilder, daoProps)
 *                  .heap("heap", "FtpNutDaoBuilder", "dark.js", "vador.js")
 *                  .engineBuilder("engineId", engineBuilder, engineProps)
 *                  .workflow("starwarsWorkflow", "heap", "engineId", true)
 *                  .releaseTag()
 *                  .build();
 *     ctx.isUpToDate(); // returns true
 *
 *     // Clear settings
 *     contextBuilder.clearTag("custom");
 *     ctx.isUpToDate(); // returns false
 * </pre>
 * </p>
 *
 * @author Guillaume DROUET
 * @version 1.0
 * @since 0.4.0
 */
public class ContextBuilder extends Observable {

    /**
     * The current tag.
     */
    private String currentTag;

    /**
     * All settings associated to their tag.
     */
    private Map<String, ContextSetting> taggedSettings;

    /**
     * <p>
     * Creates a new instance.
     * </p>
     */
    public ContextBuilder() {
        taggedSettings = new HashMap<String, ContextSetting>();
    }

    /**
     * <p>
     * Internal class used to track settings associated to a particular tag.
     * </p>
     *
     * @author Guillaume DROUET
     * @version 1.0
     * @since 0.4.0
     */
    private static class ContextSetting {

        /**
         * All {@link NutDao daos} associated to their builder ID.
         */
        private Map<String, NutDao> nutDaoMap = new HashMap<String, NutDao>();

        /**
         * All {@link Engine engines} associated to their builder ID.
         */
        private Map<String, Engine> engineMap = new HashMap<String, Engine>();

        /**
         * All {@link NutsHeap heap} associated to their ID.
         */
        private Map<String, NutsHeap> nutsHeaps = new HashMap<String, NutsHeap>();

        /**
         * All {@link Workflow workflows} associated to their ID.
         */
        private Map<String, Workflow> workflowMap = new HashMap<String, Workflow>();

        /**
         * <p>
         * Gets the {@link NutDao} associated to an ID.
         * </p>
         *
         * @return the map
         */
        public Map<String, NutDao> getNutDaoMap() {
            return nutDaoMap;
        }

        /**
         * <p>
         * Gets the {@link Engine} associated to an ID.
         * </p>
         *
         * @return the map
         */
        public Map<String, Engine> getEngineMap() {
            return engineMap;
        }

        /**
         * <p>
         * Gets the {@link NutsHeap} associated to an ID.
         * </p>
         *
         * @return the map
         */
        public Map<String, NutsHeap> getNutsHeaps() {
            return nutsHeaps;
        }

        /**
         * <p>
         * Gets the {@link Workflow} associated to an ID.
         * </p>
         *
         * @return the map
         */
        public Map<String, Workflow> getWorkflowMap() {
            return workflowMap;
        }
    }

    /**
     * <p>
     * Gets the setting associated to the current tag.
     * </p>
     *
     * @return the setting
     */
    private ContextSetting getSetting() {
        final ContextSetting setting = taggedSettings.get(currentTag);

        if (setting == null) {
            return new ContextSetting();
        } else {
            return setting;
        }
    }

    /**
     * <p>
     * Decorates the current builder with a new builder associated to a specified tag. Tagging the context allows to
     * isolate a set of configurations that could be erased by calling {@link ContextBuilder#clearTag(String)}.
     * This way, this feature is convenient when you need to poll the configurations to reload it.
     * </p>
     *
     * <p>
     * All configurations will be associated to the tag until the {@link com.github.wuic.ContextBuilder#releaseTag()}
     * method is called. If tag is currently set, then it is released when this method is called with a new tag.
     * </p>
     *
     * @param tagName the tag name
     * @return the current builder which will associates all configurations to the tag
     * @see ContextBuilder#clearTag(String)
     * @see com.github.wuic.ContextBuilder#releaseTag()
     */
    public ContextBuilder tag(final String tagName) {
        if (currentTag != null) {
            releaseTag();
        }

        currentTag = tagName;
        notifyObservers(tagName);
        setChanged();
        return this;
    }

    /**
     * <p>
     * Clears all configurations associated to the given tag.
     * </p>
     *
     * @param tagName the tag name
     * @return this {@link ContextBuilder}
     */
    public ContextBuilder clearTag(final String tagName) {
        taggedSettings.remove(tagName);
        notifyObservers(tagName);
        setChanged();

        return this;
    }

    /**
     * <p>
     * Releases the current tag of this context. When the configurations associated to a tag are finished, it could be
     * released by calling this method to not tag next configurations.
     * </p>
     *
     * @return this current builder without tag
     */
    public ContextBuilder releaseTag() {
        currentTag = null;
        notifyObservers();
        setChanged();

        return this;
    }

    /**
     * <p>
     * Add a new {@link com.github.wuic.nut.NutDaoBuilder} identified by the specified ID.
     * </p>
     *
     * <p>
     * If some properties are not supported by the builder, then an exception will be thrown.
     * </p>
     *
     * @param id the ID which identifies the builder in the context
     * @param daoBuilder the builder associated to its ID
     * @param properties the properties to use to configure the builder
     * @return this {@link ContextBuilder}
     * @throws com.github.wuic.exception.NutDaoBuilderPropertyNotSupportedException if a property is not supported by the builder
     */
    public ContextBuilder nutDaoBuilder(final String id,
                                        final NutDaoBuilder daoBuilder,
                                        final Map<String, Object> properties)
                                        throws BuilderPropertyNotSupportedException {
        final ContextSetting setting = getSetting();

        // Will override existing element
        for (ContextSetting s : taggedSettings.values()) {
            s.nutDaoMap.remove(id);
        }

        setting.nutDaoMap.put(id, configureAndBuild(daoBuilder, properties));
        taggedSettings.put(currentTag, setting);
        notifyObservers(id);
        setChanged();

        return this;
    }

    /**
     * <p>
     * Defines a new {@link com.github.wuic.nut.NutsHeap heap} in this context. A group is always identified
     * by an ID and is associated to {@link com.github.wuic.nut.NutDaoBuilder} to use to convert paths into
     * {@link com.github.wuic.nut.Nut}. A list of paths needs also to be specified to know which underlying
     * resources compose the group.
     * </p>
     *
     * <p>
     * If the {@link NutDaoBuilder} ID is not known, a {@link com.github.wuic.exception.wrapper.BadArgumentException}
     * will be thrown.
     * </p>
     *
     * @param id the group ID
     * @param ndbId the {@link com.github.wuic.nut.NutDaoBuilder} the heap is based on
     * @param path the path
     * @return this {@link ContextBuilder}
     * @throws StreamException if the HEAP could not be created
     */
    public ContextBuilder heap(final String id, final String ndbId, final String ... path) throws StreamException {
        final ContextSetting setting = getSetting();
        final NutDao dao = setting.getNutDaoMap().get(ndbId);

        if (dao == null) {
            final String msg = String.format("'%s' does not correspond to any %s, add it with nutDaoBuilder() first ",
                    ndbId, NutDaoBuilder.class.getName());
            throw new BadArgumentException(new IllegalArgumentException(msg));
        }

        // Will override existing element
        for (ContextSetting s : taggedSettings.values()) {
            s.getNutsHeaps().remove(id);
        }

        setting.getNutsHeaps().put(id, new NutsHeap(Arrays.asList(path), dao, id));
        taggedSettings.put(currentTag, setting);
        notifyObservers(id);
        setChanged();

        return this;
    }

    /**
     * <p>
     * Declares a new {@link com.github.wuic.factory.EngineBuilder} with its specific properties.
     * The builder is identified by an unique ID and produces in fine {@link com.github.wuic.engine.Engine engines}
     * that could be chained.
     * </p>
     *
     * @param id the {@link EngineBuilder} ID
     * @param engineBuilder the {@link com.github.wuic.factory.EngineBuilder} to configure
     * @param properties the builder's properties (must be supported by the builder)
     * @return this {@link ContextBuilder}
     * @throws BuilderPropertyNotSupportedException if a property is not supported
     */
    public ContextBuilder engineBuilder(final String id,
                                        final EngineBuilder engineBuilder,
                                        final Map<String, Object> properties)
            throws BuilderPropertyNotSupportedException {
        final ContextSetting setting = getSetting();

        // Will override existing element
        for (ContextSetting s : taggedSettings.values()) {
            s.getEngineMap().remove(id);
        }

        setting.getEngineMap().put(id, configureAndBuild(engineBuilder, properties));
        taggedSettings.put(currentTag, setting);
        notifyObservers(id);
        setChanged();

        return this;
    }

    /**
     * <p>
     * Creates a new workflow. Any resource processing will be done through an existing workflow.
     * </p>
     *
     * <p>
     * A workflow consists to chain a set of engines produced by the specified {@link com.github.wuic.factory.EngineBuilder builders}
     * with a {@link com.github.wuic.nut.NutsHeap heap} as data to be processed. There is a chain for each possible {@link NutType}.
     * A chain that processes a particular {@link NutType} of {@link com.github.wuic.nut.Nut} is composed of {@link Engine}
     * ordered by type. All engines specified in parameter as array are simply organized following those two criteria to
     * create the chains. Moreover, default engines could be injected in the chain to perform common operations to be done
     * on resources. If an {@link com.github.wuic.factory.EngineBuilder} is specified in a chain while it is injected
     * by default, then the configuration of the given builder will overrides the default one.
     * </p>
     *
     * <p>
     * A set of {@link com.github.wuic.nut.NutDaoBuilder} could be specified to store processed resources. When the client
     * will retrieve the resources, it will access it through a proxy URI configured in the protocol. This URI corresponds
     * to a server in front of the location where resources have been stored. For that reason the {@link NutDao} must
     * support {@link NutDao#save(com.github.wuic.nut.Nut)} operation.
     * </p>
     *
     * <p>
     * An {@link IllegalStateException} will be thrown if the context is not correctly configured. Bad settings are :
     *  <ul>
     *      <li>Unknown {@link EngineBuilder} ID</li>
     *      <li>Unknown {@link NutsHeap} ID</li>
     *      <li>Unknown {@link NutDaoBuilder} ID</li>
     *      <li>A {@link NutDao} does not supports {@link NutDao#save(com.github.wuic.nut.Nut)} method</li>
     *  </ul>
     * </p>
     *
     * @param id the workflow ID
     * @param heapId the heap that needs to be processed
     * @param ebIds the set of {@link com.github.wuic.factory.EngineBuilder} to use
     * @param ndbIds the set of {@link com.github.wuic.nut.NutDaoBuilder} where to eventually upload processed resources
     * @param includeDefaultEngines include or not default engines
     * @return this {@link ContextBuilder}
     */
    public ContextBuilder workflow(final String id,
                                   final String heapId,
                                   final String[] ebIds,
                                   final Boolean includeDefaultEngines,
                                   final String ... ndbIds) {
        final ContextSetting setting = getSetting();

        // Retrieve each DAO associated to all provided IDs
        final NutDao[] nutDaos = new NutDao[ndbIds.length];

        for (int i = 0; i < ndbIds.length; i++) {
            final String ndbId = ndbIds[i];
            final NutDao dao = getNutDao(ndbId);

            if (dao == null) {
                throw new IllegalStateException(String.format("'%s' not associated to any %s", ndbId, NutDaoBuilder.class.getName()));
            }

            if (!dao.saveSupported()) {
                throw new IllegalStateException(String.format("DAO built by '%s' does not supports save", ndbId));
            }

            nutDaos[i] = dao;
        }

        // Retrieve HEAP
        final NutsHeap heap = getNutsHeap(heapId);

        if (heap == null) {
            throw new IllegalStateException(String.format("'%s' not associated to any %s", heapId, NutsHeap.class.getName()));
        }

        // Retrieve each engine associated to all provided IDs and group them by nut type
        final Map<NutType, CGCompositeEngine> chains = new HashMap<NutType, CGCompositeEngine>();

        // Include default engines
        if (includeDefaultEngines) {
            chains.put(NutType.CSS, new CGCompositeEngine(new TextAggregatorEngineBuilder().build(), new CssInspectorEngineBuilder().build()));
            chains.put(NutType.PNG, new CGCompositeEngine(new ImageAggregatorEngineBuilder().build(), new ImageCompressorEngineBuilder().build()));
            chains.put(NutType.JAVASCRIPT, new CGCompositeEngine(new TextAggregatorEngineBuilder().build()));
            // TODO : when created, include embedded cache, JS minification, CSS minification and GZIP compressor
        }

        for (final String ebId : ebIds) {
            final Engine engine = getEngine(ebId);

            if (engine == null) {
                throw new IllegalStateException(String.format("'%s' not associated to any %s", ebId, EngineBuilder.class.getName()));
            } else {
                // Already exists
                if (chains.containsKey(engine.getConfiguration().getNutType())) {
                    chains.get(engine.getConfiguration().getNutType()).add(engine);
                } else {
                    // Create first entry
                    chains.put(engine.getConfiguration().getNutType(), new CGCompositeEngine(engine));
                }
            }
        }

        // Will override existing element
        for (ContextSetting s : taggedSettings.values()) {
            s.getWorkflowMap().remove(id);
        }

        setting.getWorkflowMap().put(id, new Workflow(chains, heap, nutDaos));
        taggedSettings.put(currentTag, setting);
        notifyObservers(id);
        setChanged();

        return this;
    }

    /**
     * <p>
     * Creates a new workflow by injecting default {@link Engine engines}.
     * </p>
     *
     * <p>
     * See {@link ContextBuilder#workflow(String, String, String[], Boolean, String...)} for full documentation.
     * </p>
     *
     * @param id the workflow ID
     * @param heapId the heap that needs to be processed
     * @param ebIds the set of {@link com.github.wuic.factory.EngineBuilder} to use
     * @param ndbIds the set of {@link com.github.wuic.nut.NutDaoBuilder} where to eventually upload processed resources
     * @return this {@link ContextBuilder}
     */
    public ContextBuilder workflow(final String id,
                                   final String heapId,
                                   final String[] ebIds,
                                   final String ... ndbIds) {
        return workflow(id, heapId, ebIds, true, ndbIds);
    }

    /**
     * <p>
     * Builds the context. Should throws an {@link IllegalStateException} if the context is not correctly configured.
     * For instance : associate a heap to an undeclared {@link com.github.wuic.nut.NutDaoBuilder} ID.
     * </p>
     *
     * @return the new {@link Context}
     */
    public Context build() {
        final Map<String, Workflow> workflowMap = new HashMap<String, Workflow>();

        for (ContextSetting setting : taggedSettings.values()) {
            workflowMap.putAll(setting.workflowMap);
        }

        return new Context(this, workflowMap);
    }

    /**
     * <p>
     * Gets the {@link NutDao} associated to the given builder ID.
     * </p>
     *
     * @param ndbId the builder ID
     * @return the {@link NutDao}, {@code null} if nothing is associated to the ID
     */
    private NutDao getNutDao(final String ndbId) {
        for (ContextSetting setting : taggedSettings.values()) {
            if (setting.nutDaoMap.containsKey(ndbId)) {
                return setting.nutDaoMap.get(ndbId);
            }
        }

        return null;
    }

    /**
     * <p>
     * Gets the {@link NutsHeap} associated to the given ID.
     * </p>
     *
     * @param heapId the ID
     * @return the {@link NutsHeap}, {@code null} if nothing is associated to the ID
     */
    private NutsHeap getNutsHeap(final String heapId) {
        for (ContextSetting setting : taggedSettings.values()) {
            if (setting.nutsHeaps.containsKey(heapId)) {
                return setting.nutsHeaps.get(heapId);
            }
        }

        return null;
    }

    /**
     * <p>
     * Gets the {@link Engine} associated to the given builder ID.
     * </p>
     *
     * @param engineId the builder ID
     * @return the {@link Engine}, {@code null} if nothing is associated to the ID
     */
    private Engine getEngine(final String engineId) {
        for (ContextSetting setting : taggedSettings.values()) {
            if (setting.engineMap.containsKey(engineId)) {
                return setting.engineMap.get(engineId);
            }
        }

        return null;
    }

    /**
     * <p>
     * Configures the given builder with the specified properties and then return the result of the build() invocation.
     * </p>
     *
     * @param builder the builder
     * @param properties the properties to use to configure the builder
     * @param <O> the type produced by the builder
     * @param <T> the type of builder
     * @return the result of build operation
     * @throws BuilderPropertyNotSupportedException if a specified property is not supported by the builder
     */
    private <O, T extends GenericBuilder<O>> O configureAndBuild(final T builder,  final Map<String, Object> properties)
            throws BuilderPropertyNotSupportedException {
        for (final Map.Entry entry : properties.entrySet()) {
            builder.property(String.valueOf(entry.getKey()), entry.getValue());
        }

        return builder.build();
    }
}
