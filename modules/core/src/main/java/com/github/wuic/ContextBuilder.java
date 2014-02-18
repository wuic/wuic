/*
 * "Copyright (c) 2014   Capgemini Technology Services (hereinafter "Capgemini")
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
import com.github.wuic.engine.core.*;
import com.github.wuic.exception.BuilderPropertyNotSupportedException;
import com.github.wuic.exception.WorkflowTemplateNotFoundException;
import com.github.wuic.exception.wrapper.BadArgumentException;
import com.github.wuic.exception.wrapper.StreamException;
import com.github.wuic.engine.EngineBuilder;
import com.github.wuic.nut.NutDao;
import com.github.wuic.nut.NutDaoBuilder;
import com.github.wuic.nut.NutsHeap;
import com.github.wuic.util.AbstractBuilderFactory;
import com.github.wuic.util.CollectionUtils;
import com.github.wuic.util.GenericBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Pattern;

/**
 * <p>
 * This builder can be configured to build contexts in an expected state by the user. It is designed to be used in a
 * multi-threaded environment.
 * </p>
 *
 * <p>
 * The builder tracks all settings by associated to them a tag. With that tag, the user is able to delete
 * all settings defined at a moment. Example :
 * <pre>
 *     final ContextBuilder contextBuilder = new ContextBuilder();
 *
 *     try {
 *         // Create a context with some settings tagged as "custom"
 *         final Context ctx = contextBuilder.tag("custom")
 *                      .nutDaoBuilder("FtpNutDaoBuilder", daoBuilder, daoProps)
 *                      .heap("heap", "FtpNutDaoBuilder", "dark.js", "vador.js")
 *                      .engineBuilder("engineId", engineBuilder, engineProps)
 *                      .workflow("starwarsWorkflow", "heap", "engineId", true)
 *                      .releaseTag()
 *                      .build();
 *         ctx.isUpToDate(); // returns true
 *
 *         // Clear settings
 *         contextBuilder.clearTag("custom");
 *         ctx.isUpToDate(); // returns false
 *     } finally {
 *         contextBuilder.releaseTag();
 *     }
 * </pre>
 * </p>
 *
 *
 * <p>
 * If any operation is performed without any tag, then an exception will be thrown. Moreover, when the
 * {@link ContextBuilder#tag(String)} method is called, the current threads holds a lock on the object.
 * It will be released when the {@link com.github.wuic.ContextBuilder#releaseTag()} will be called.
 * Consequenlty, it is really important to always call this last method in a finally block.
 * </p>
 *
 * @author Guillaume DROUET
 * @version 1.1
 * @since 0.4.0
 */
public class ContextBuilder extends Observable {

    /**
     * The logger.
     */
    private final Logger log = LoggerFactory.getLogger(getClass());

    /**
     * The internal lock for tags.
     */
    private ReentrantLock lock;

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
        lock = new ReentrantLock();
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
         * All {@link EngineBuilder engines} associated to their ID.
         */
        private Map<String, EngineBuilder> engineMap = new HashMap<String, EngineBuilder>();

        /**
         * All {@link NutsHeap heaps} associated to their ID.
         */
        private Map<String, NutsHeap> nutsHeaps = new HashMap<String, NutsHeap>();

        /**
         * All {@link WorkflowTemplate templates} associated to their ID.
         */
        private Map<String, WorkflowTemplate> templates = new HashMap<String, WorkflowTemplate>();

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
         * Gets the {@link EngineBuilder} associated to an ID.
         * </p>
         *
         * @return the map
         */
        public Map<String, EngineBuilder> getEngineMap() {
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

        /**
         * <p>
         * Gets the {@link WorkflowTemplate} associated to an ID.
         * </p>
         *
         * @return the map
         */
        public Map<String, WorkflowTemplate> getTemplateMap() {
            return templates;
        }
    }

    /**
     * <p>
     * Gets the setting associated to the current tag. If no tag is defined, then an {@link IllegalStateException} will
     * be thrown.
     * </p>
     *
     * @return the setting
     */
    private ContextSetting getSetting() {
        if (currentTag == null) {
            throw new IllegalStateException("Call tag() method first");
        }

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
        lock.lock();
        log.debug("ContextBuilder locked by {}", Thread.currentThread().toString());

        if (currentTag != null) {
            releaseTag();
        }

        currentTag = tagName;
        setChanged();
        notifyObservers(tagName);
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
        try {
            if (!lock.isHeldByCurrentThread()) {
                lock.lock();
            }

            final ContextSetting setting = taggedSettings.remove(tagName);

            // Shutdown all DAO (scheduled jobs, etc)
            if (setting != null) {
                for (final NutDao dao : setting.nutDaoMap.values()) {
                    dao.shutdown();
                }
            }

            setChanged();
            notifyObservers(tagName);

            return this;
        } finally {
            lock.unlock();
        }
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
        try {
            // Won't block if the thread already own the
            if (!lock.isHeldByCurrentThread()) {
                lock.lock();
                log.debug("ContextBuilder locked by {}", Thread.currentThread().toString());
            }

            // Check that a tag exists
            getSetting();
            currentTag = null;
            setChanged();
            notifyObservers();

            return this;
        } finally {
            // Release the lock
            lock.unlock();
            log.debug("ContextBuilder unlocked by {}", Thread.currentThread().toString());
        }
    }

    /*

    TODO: #84

    public class ContextNutDaoBuilder {

        public ContextNutDaoBuilder(final String id, final String type) {

        }

        public ContextNutDaoBuilder property(final String key, final Object value) {
            return this;
        }

        public ContextBuilder toContext() {
            return ContextBuilder.this;
        }
    }*/

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

        setting.nutDaoMap.put(id, configure(daoBuilder, properties).build());
        taggedSettings.put(currentTag, setting);
        setChanged();
        notifyObservers(id);

        return this;
    }


    /**
     * <p>
     * Creates a new heap as specified by {@link ContextBuilder#heap(String, String, String[], String...)} without any
     * composition.
     * </p>
     *
     * @param id the heap ID
     * @param ndbId the {@link com.github.wuic.nut.NutDaoBuilder} the heap is based on
     * @param path the path
     * @return this {@link ContextBuilder}
     * @throws StreamException if the HEAP could not be created
     */
    public ContextBuilder heap(final String id, final String ndbId, final String ... path) throws StreamException {
        return heap(id, ndbId, null, path);
    }

    /**
     * <p>
     * Defines a new {@link com.github.wuic.nut.NutsHeap heap} in this context. A heap is always identified
     * by an ID and is associated to {@link com.github.wuic.nut.NutDaoBuilder} to use to convert paths into
     * {@link com.github.wuic.nut.Nut}. A list of paths needs also to be specified to know which underlying
     * nuts compose the heap.
     * </p>
     *
     * <p>
     * The heap could be composed in part or totally of other heaps.
     * </p>
     *
     * <p>
     * If the {@link NutDaoBuilder} ID is not known, a {@link com.github.wuic.exception.wrapper.BadArgumentException}
     * will be thrown.
     * </p>
     *
     * @param id the heap ID
     * @param heapIds the heaps composition
     * @param ndbId the {@link com.github.wuic.nut.NutDaoBuilder} the heap is based on
     * @param path the path
     * @return this {@link ContextBuilder}
     * @throws StreamException if the HEAP could not be created
     */
    public ContextBuilder heap(final String id, final String ndbId, final String[] heapIds, final String ... path) throws StreamException {
        NutDao dao = null;

        // Will override existing element
        for (final ContextSetting s : taggedSettings.values()) {
            s.getNutsHeaps().remove(id);

            // Find DAO
            if (ndbId != null && dao == null) {
                dao = s.getNutDaoMap().get(ndbId);
            }
        }

        if (dao == null && path.length != 0) {
            final String msg = String.format("'%s' does not correspond to any %s, add it with nutDaoBuilder() first ",
                    ndbId, NutDaoBuilder.class.getName());
            throw new BadArgumentException(new IllegalArgumentException(msg));
        }

        final ContextSetting setting = getSetting();

        // Composition detected, collected nested and referenced heaps
        if (heapIds != null && heapIds.length != 0) {
            final List<NutsHeap> composition = new ArrayList<NutsHeap>();

            for (final String regex : heapIds) {
                composition.addAll(getNutsHeap(regex));
            }

            setting.getNutsHeaps().put(id, new NutsHeap(Arrays.asList(path), dao, id, composition.toArray(new NutsHeap[composition.size()])));
        } else {
            setting.getNutsHeaps().put(id, new NutsHeap(Arrays.asList(path), dao, id));
        }

        taggedSettings.put(currentTag, setting);
        setChanged();
        notifyObservers(id);

        return this;
    }

    /**
     * <p>
     * Declares a new {@link com.github.wuic.engine.EngineBuilder} with its specific properties.
     * The builder is identified by an unique ID and produces in fine {@link com.github.wuic.engine.Engine engines}
     * that could be chained.
     * </p>
     *
     * @param id the {@link EngineBuilder} ID
     * @param engineBuilder the {@link com.github.wuic.engine.EngineBuilder} to configure
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
        for (final ContextSetting s : taggedSettings.values()) {
            s.getEngineMap().remove(id);
        }

        setting.getEngineMap().put(id, configure(engineBuilder, properties));
        taggedSettings.put(currentTag, setting);
        setChanged();
        notifyObservers(id);

        return this;
    }


    /**
     * <p>
     * Builds a new template with no exclusion and default engine usages.
     * </p>
     *
     * @param id the template's id
     * @param ebIds the set of {@link com.github.wuic.engine.EngineBuilder} to use
     * @param daos the DAO
     * @return this {@link ContextBuilder}
     * @throws StreamException if an I/O error occurs
     * @see ContextBuilder#template(String, String[], String[], Boolean, String...)
     */
    public ContextBuilder template(final String id,
                                   final String[] ebIds,
                                   final String ... daos) throws StreamException {
        return template(id, ebIds, null, Boolean.TRUE, daos);
    }

    /**
     * <p>
     * Creates a new workflow template.
     * </p>
     *
     * <p>
     * The template consists to chain a set of engines produced by the specified {@link com.github.wuic.engine.EngineBuilder builders}.
     * There is a chain for each possible {@link NutType}. A chain that processes a particular {@link NutType} of
     * {@link com.github.wuic.nut.Nut} is composed of {@link Engine engines} ordered by type. All engines specified in
     * parameter as array are simply organized following those two criteria to create the chains. Moreover, default engines
     * could be injected in the chain to perform common operations to be done on nuts. If an {@link com.github.wuic.engine.EngineBuilder}
     * is specified in a chain while it is injected by default, then the configuration of the given builder will overrides
     * the default one.
     * </p>
     *
     * <p>
     * A set of {@link com.github.wuic.nut.NutDaoBuilder} could be specified to store processed nuts. When the client
     * will retrieve the nuts, it will access it through a proxy URI configured in the protocol. This URI corresponds
     * to a server in front of the location where nuts have been stored. For that reason the {@link NutDao} must
     * support {@link NutDao#save(com.github.wuic.nut.Nut)} operation.
     * </p>
     *
     * <p>
     * If the context builder should include engines by default, then a set of default engine to be excluded could be specified.
     * </p>
     *
     * <p>
     * An {@link IllegalStateException} will be thrown if the context is not correctly configured. Bad settings are :
     *  <ul>
     *      <li>Unknown {@link EngineBuilder} ID</li>
     *      <li>Unknown {@link NutDaoBuilder} ID</li>
     *      <li>A {@link NutDao} does not supports {@link NutDao#save(com.github.wuic.nut.Nut)} method</li>
     *  </ul>
     * </p>
     *
     * @param id the template's id
     * @param ebIds the set of {@link com.github.wuic.engine.EngineBuilder} to use
     * @param ebIdsExclusion some default builder to be excluded in the chain
     * @param ndbIds the set of {@link com.github.wuic.nut.NutDaoBuilder} where to eventually upload processed nuts
     * @param includeDefaultEngines include or not default engines
     * @return this {@link ContextBuilder}
     * @throws StreamException if an I/O error occurs
     */
    public ContextBuilder template(final String id,
                                   final String[] ebIds,
                                   final String[] ebIdsExclusion,
                                   final Boolean includeDefaultEngines,
                                   final String ... ndbIds) throws StreamException {
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

        // Retrieve each engine associated to all provided IDs and heap them by nut type
        final Map<NutType, Engine> chains = createChains(includeDefaultEngines, ebIdsExclusion);

        for (final String ebId : ebIds) {
            // Create a different instance per chain
            final Engine engine = newEngine(ebId);

            if (engine == null) {
                throw new IllegalStateException(String.format("'%s' not associated to any %s", ebId, EngineBuilder.class.getName()));
            } else {
                final List<NutType> nutTypes = engine.getNutTypes();

                for (final NutType nt : nutTypes) {
                    // Already exists
                    if (chains.containsKey(nt)) {
                        chains.put(nt, Engine.chain(chains.get(nt), newEngine(ebId)));
                    } else {
                        // Create first entry
                        chains.put(nt, engine);
                    }
                }
            }
        }

        setting.getTemplateMap().put(id, new WorkflowTemplate(chains, nutDaos));

        taggedSettings.put(currentTag, setting);
        setChanged();
        notifyObservers(id);

        return this;
    }

    /**
     * <p>
     * Creates a new workflow. Any nut processing will be done through an existing workflow.
     * </p>
     *
     * <p>
     * A workflow is based on a {@link WorkflowTemplate} with a specified ID.
     * </p>
     *
     * <p>
     * The {@link NutsHeap heap} to be used is represented by a regex. The forEachHeap parameter indicates if only one
     * workflow should be created having as {@link NutsHeap heap} a composition of all heaps matching the pattern. If the
     * parameter is {@code false}, then a workflow is created for each matching {@link NutsHeap}. In this case, the workflow
     * ID will be the concatenation if the given identifier and the heap's ID.
     * </p>
     *
     * <p>
     * An {@link IllegalStateException} will be thrown if the context is not correctly configured. Bad settings are :
     *  <ul>
     *      <li>Unknown {@link NutsHeap} ID</li>
     *  </ul>
     * </p>
     *
     * @param identifier the identifier used to build the workflow ID, is the prefix if create one for each heap
     * @param forEachHeap {@code true} if a dedicated workflow must be created for each matching heap, {@code false} for a composition
     * @param heapIdPattern the regex matching the heap IDs that needs to be processed
     * @return this {@link ContextBuilder}
     * @throws StreamException if an I/O error occurs
     * @throws WorkflowTemplateNotFoundException if the specified template ID does not exists
     */
    public ContextBuilder workflow(final String identifier,
                                   final Boolean forEachHeap,
                                   final String heapIdPattern,
                                   final String workflowTemplateId)
            throws StreamException, WorkflowTemplateNotFoundException {
        final ContextSetting setting = getSetting();

        final WorkflowTemplate template = getWorkflowTemplate(workflowTemplateId);

        if (template == null) {
            throw new WorkflowTemplateNotFoundException(workflowTemplateId);
        }

        final Map<NutType, ? extends Engine> chains = template.getChains();
        final NutDao[] nutDaos = template.getStores();

        // Retrieve HEAP
        final List<NutsHeap> heaps = getNutsHeap(heapIdPattern);

        if (heaps.isEmpty()) {
            throw new IllegalStateException(String.format("'%s' is a regex which doesn't match any %s", heapIdPattern, NutsHeap.class.getName()));
        }

        if (forEachHeap) {
            for (final NutsHeap heap : heaps) {
                final String id = identifier + heap.getId();

                // Will override existing element
                for (final ContextSetting s : taggedSettings.values()) {
                    s.getWorkflowMap().remove(id);
                }

                setting.getWorkflowMap().put(id, new Workflow(chains, heap, nutDaos));
            }
        } else {
            final NutsHeap[] array = heaps.toArray(new NutsHeap[heaps.size()]);
            setting.getWorkflowMap().put(identifier, new Workflow(chains, new NutsHeap(null, null, heapIdPattern, array)));
        }

        taggedSettings.put(currentTag, setting);
        setChanged();
        notifyObservers(identifier);

        return this;
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

        // This thread needs to lock to build the context
        // However, if it is already done, the method should not unlock at the end of the method
        final boolean requiresLock = !lock.isHeldByCurrentThread();

        try {
            if (requiresLock) {
                lock.lock();
            }

            final Map<String, Workflow> workflowMap = new HashMap<String, Workflow>();
            final Map<String, NutsHeap> heapMap = new HashMap<String, NutsHeap>();

            // Add all specified workflow
            for (final ContextSetting setting : taggedSettings.values()) {
                workflowMap.putAll(setting.workflowMap);

                for (final NutsHeap heap : setting.getNutsHeaps().values()) {
                    heapMap.put(heap.getId(), heap);
                }
            }

            // Create a default workflow for heaps not referenced by any workflow
            heapLoop :
            for (final NutsHeap heap : heapMap.values()) {
                for (final Workflow workflow : workflowMap.values()) {
                    if (workflow.getHeap().equals(heap)) {
                        continue heapLoop;
                    }
                }

                // No workflow has been found : create a default with the heap ID as ID
                workflowMap.put(heap.getId(), new Workflow(createChains(Boolean.TRUE, null), heap));
            }

            return new Context(this, workflowMap);
        } finally {
            if (requiresLock) {
                lock.unlock();
            }
        }
    }

    /**
     * <p>
     * Creates a new set of chains. If we don't include default engines, then the returned map will be empty.
     * </p>
     *
     * @param includeDefaultEngines include default or not
     * @param ebIdsExclusions the default engines to exclude
     * @return the different chains
     */
    private Map<NutType, Engine> createChains(final Boolean includeDefaultEngines, final String[] ebIdsExclusions) {
        final Map<NutType, Engine> chains = new HashMap<NutType, Engine>();

        // Include default engines
        if (includeDefaultEngines) {
            chains.put(NutType.CSS, Engine.chain(defaultCache(ebIdsExclusions), defaultTextAggregator(ebIdsExclusions), defaultCssInspector(ebIdsExclusions)));
            chains.put(NutType.PNG, Engine.chain(defaultCache(ebIdsExclusions), defaultImageAggregator(ebIdsExclusions), defaultImageCompressor(ebIdsExclusions)));
            chains.put(NutType.JAVASCRIPT, Engine.chain(defaultCache(ebIdsExclusions), defaultTextAggregator(ebIdsExclusions)));
            // TODO : when created, include GZIP compressor
        }

        return chains;
    }

    /**
     * <p>
     * Creates a default image compressor.
     * </p>
     *
     * @param ebIdsExclusions exclusions
     * @return the default engine
     */
    private Engine defaultImageCompressor(final String[] ebIdsExclusions) {
        final String name = AbstractBuilderFactory.ID_PREFIX + ImageCompressorEngineBuilder.class.getSimpleName();

        if (ebIdsExclusions != null && CollectionUtils.indexOf(name, ebIdsExclusions) != -1) {
            return null;
        }

        final Engine retval = newEngine(name);

        if (retval == null) {
            return new ImageCompressorEngineBuilder().build();
        } else {
            return retval;
        }
    }

    /**
     * <p>
     * Creates a default image aggregator.
     * </p>
     *
     * @param ebIdsExclusions exclusions
     * @return the default engine
     */
    private Engine defaultImageAggregator(final String[] ebIdsExclusions) {
        final String name = AbstractBuilderFactory.ID_PREFIX + ImageAggregatorEngineBuilder.class.getSimpleName();

        if (ebIdsExclusions != null && CollectionUtils.indexOf(name, ebIdsExclusions) != -1) {
            return null;
        }

        final Engine retval = newEngine(name);

        if (retval == null) {
            return new ImageAggregatorEngineBuilder().build();
        } else {
            return retval;
        }
    }

    /**
     * <p>
     * Creates a default css inspector.
     * </p>
     *
     * @param ebIdsExclusions exclusions
     * @return the default engine
     */
    private Engine defaultCssInspector(final String[] ebIdsExclusions) {
        final String name = AbstractBuilderFactory.ID_PREFIX + CssInspectorEngineBuilder.class.getSimpleName();

        if (ebIdsExclusions != null && CollectionUtils.indexOf(name, ebIdsExclusions) != -1) {
            return null;
        }

        final Engine retval = newEngine(name);

        if (retval == null) {
            return new CssInspectorEngineBuilder().build();
        } else {
            return retval;
        }
    }

    /**
     * <p>
     * Creates a default text aggregator.
     * </p>
     *
     * @param ebIdsExclusions exclusions
     * @return the default engine
     */
    private Engine defaultTextAggregator(final String[] ebIdsExclusions) {
        final String name = AbstractBuilderFactory.ID_PREFIX + TextAggregatorEngineBuilder.class.getSimpleName();

        if (ebIdsExclusions != null && CollectionUtils.indexOf(name, ebIdsExclusions) != -1) {
            return null;
        }

        final Engine retval = newEngine(name);

        if (retval == null) {
            return new TextAggregatorEngineBuilder().build();
        } else {
            return retval;
        }
    }

    /**
     * <p>
     * Creates a default cache engine.
     * </p>
     *
     * @param ebIdsExclusions exclusions
     * @return the default cache
     */
    private Engine defaultCache(final String[] ebIdsExclusions) {
        final String name = AbstractBuilderFactory.ID_PREFIX + MemoryMapCacheEngineBuilder.class.getSimpleName();

        if (ebIdsExclusions != null && CollectionUtils.indexOf(name, ebIdsExclusions) != -1) {
            return null;
        }

        final Engine retval = newEngine(name);

        if (retval == null) {
            return new MemoryMapCacheEngineBuilder().build();
        } else {
            return retval;
        }
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
     * Gets the {@link WorkflowTemplate} associated to the given ID.
     * </p>
     *
     * @param id the ID
     * @return the matching {@link WorkflowTemplate template}
     */
    private WorkflowTemplate getWorkflowTemplate(final String id) {
        final Iterator<ContextSetting> it = taggedSettings.values().iterator();
        WorkflowTemplate retval = null;

        while (it.hasNext() && retval == null) {
            retval = it.next().getTemplateMap().get(id);
        }

        return retval;
    }

    /**
     * <p>
     * Gets the {@link NutsHeap} associated to an ID matching the given regex.
     * </p>
     *
     * @param regex the regex ID
     * @return the matching {@link NutsHeap heaps}
     */
    private List<NutsHeap> getNutsHeap(final String regex) {
        final List<NutsHeap> retval = new ArrayList<NutsHeap>();
        final Pattern pattern = Pattern.compile(regex);

        for (final ContextSetting setting : taggedSettings.values()) {
            for (final NutsHeap heap : setting.getNutsHeaps().values()) {
                if (pattern.matcher(heap.getId()).matches()) {
                    retval.add(heap);
                }
            }
        }

        return retval;
    }

    /**
     * <p>
     * Gets the {@link Engine} produced by the builder associated to the given ID.
     * </p>
     *
     * @param engineBuilderId the builder ID
     * @return the {@link Engine}, {@code null} if nothing is associated to the ID
     */
    private Engine newEngine(final String engineBuilderId) {
        for (ContextSetting setting : taggedSettings.values()) {
            if (setting.engineMap.containsKey(engineBuilderId)) {
                return setting.engineMap.get(engineBuilderId).build();
            }
        }

        return null;
    }

    /**
     * <p>
     * Configures the given builder with the specified properties and then return it.
     * </p>
     *
     * @param builder the builder
     * @param properties the properties to use to configure the builder
     * @param <O> the type produced by the builder
     * @param <T> the type of builder
     * @return the given builder
     * @throws BuilderPropertyNotSupportedException if a specified property is not supported by the builder
     */
    private <O, T extends GenericBuilder<O>> T configure(final T builder,  final Map<String, Object> properties)
            throws BuilderPropertyNotSupportedException {
        for (final Map.Entry entry : properties.entrySet()) {
            builder.property(String.valueOf(entry.getKey()), entry.getValue());
        }

        return builder;
    }
}
