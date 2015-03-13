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


package com.github.wuic;

import com.github.wuic.config.ObjectBuilder;
import com.github.wuic.config.ObjectBuilderFactory;
import com.github.wuic.config.ObjectBuilderInspector;
import com.github.wuic.engine.Engine;
import com.github.wuic.engine.EngineService;
import com.github.wuic.engine.HeadEngine;
import com.github.wuic.engine.NodeEngine;
import com.github.wuic.exception.WorkflowTemplateNotFoundException;
import com.github.wuic.exception.WuicException;
import com.github.wuic.nut.HeapListener;
import com.github.wuic.nut.Nut;
import com.github.wuic.nut.dao.NutDao;
import com.github.wuic.nut.dao.NutDaoService;
import com.github.wuic.nut.NutsHeap;
import com.github.wuic.nut.dao.core.ProxyNutDao;
import com.github.wuic.nut.filter.NutFilter;
import com.github.wuic.nut.filter.NutFilterHolder;
import com.github.wuic.nut.filter.NutFilterService;
import com.github.wuic.util.CollectionUtils;
import com.github.wuic.util.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Observable;
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
 * all settings defined at a moment. Example:
 * <pre>
 *     final ContextBuilder contextBuilder = new ContextBuilder();
 *
 *     try {
 *         // Create a context with some settings tagged as "custom"
 *         final Context ctx = contextBuilder.tag("custom")
 *                      .contextNutDaoBuilder("FtpNutDaoBuilder", "FtpNutDaoBuilder")
 *                      .property("c.g.wuic.dao.basePath", "/statics)"
 *                      .toContext()
 *                      .heap("heap", "FtpNutDaoBuilder", "darth.js", "vader.js")
 *                      .contextNutDaoBuilder("engineId", "TextAggregatorEngineBuilder")
 *                      .toContext()
 *                      .contextNutFilterBuilder("filterId", "RegexRemoveNutFilterBuilder")
 *                      .property(ApplicationConfig.REGEX_EXPRESSION, "(.*)?reload.*")
 *                      .toContext()
 *                      .template("tpl", new String[]{"engineId"}, null, false)
 *                      .workflow("starwarsWorkflow", true, "heap", "tpl")
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
 * <p>
 * If any operation is performed without any tag, then an exception will be thrown. Moreover, when the
 * {@link ContextBuilder#tag(Object)} method is called, the current threads holds a lock on the object.
 * It will be released when the {@link com.github.wuic.ContextBuilder#releaseTag()} will be called.
 * Consequently, it is really important to always call this last method in a finally block.
 * </p>
 *
 * @author Guillaume DROUET
 * @version 1.3
 * @since 0.4.0
 */
public class ContextBuilder extends Observable {

    /**
     * Prefix for default IDs.
     */
    private static final String BUILDER_ID_PREFIX = "wuicDefault";

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
    private Object currentTag;

    /**
     * All settings associated to their tag.
     */
    private Map<Object, ContextSetting> taggedSettings;

    /**
     * Factory for engine builder.
     */
    private ObjectBuilderFactory<Engine> engineBuilderFactory;

    /**
     * Factory for nut dao builder.
     */
    private ObjectBuilderFactory<NutDao> nutDaoBuilderFactory;

    /**
     * Factory for nut filter builder.
     */
    private ObjectBuilderFactory<NutFilter> nutFilterBuilderFactory;

    /**
     * Indicates that {@link #configureDefault()} has been called and default entries are injected.
     */
    private boolean configureDefault;

    /**
     * <p>
     * Creates a new instance with specific builder factories.
     * </p>
     *
     * @param engineBuilderFactory the engine builder factory, {@code null} if default should be created
     * @param nutDaoBuilderFactory the DAO builder factory, {@code null} if default should be created
     * @param nutFilterBuilderFactory the filter builder factory, {@code null} if default should be created
     * @param inspectors the inspectors to add to the factories
     */
    public ContextBuilder(final ObjectBuilderFactory<Engine> engineBuilderFactory,
                          final ObjectBuilderFactory<NutDao> nutDaoBuilderFactory,
                          final ObjectBuilderFactory<NutFilter> nutFilterBuilderFactory,
                          final ObjectBuilderInspector ... inspectors) {
        this.taggedSettings = new HashMap<Object, ContextSetting>();
        this.lock = new ReentrantLock();
        this.configureDefault = false;

        this.engineBuilderFactory = engineBuilderFactory == null ?
                new ObjectBuilderFactory<Engine>(EngineService.class, EngineService.DEFAULT_SCAN_PACKAGE) : engineBuilderFactory;
        this.nutDaoBuilderFactory = nutDaoBuilderFactory == null ?
                new ObjectBuilderFactory<NutDao>(NutDaoService.class, NutDaoService.DEFAULT_SCAN_PACKAGE) : nutDaoBuilderFactory;
        this.nutFilterBuilderFactory = nutFilterBuilderFactory == null ?
                new ObjectBuilderFactory<NutFilter>(NutFilterService.class, NutFilterService.DEFAULT_SCAN_PACKAGE) : nutFilterBuilderFactory;

        final ObjectBuilderInspector inspector = new NutFilterHolderInspector();
        this.engineBuilderFactory.inspector(inspector);
        this.nutDaoBuilderFactory.inspector(inspector);

        for (final ObjectBuilderInspector i : inspectors) {
            this.engineBuilderFactory.inspector(i);
            this.nutDaoBuilderFactory.inspector(i);
        }
    }

    /**
     * <p>
     * Creates a new instance.
     * </p>
     *
     * @param inspectors the inspectors
     */
    public ContextBuilder(final ObjectBuilderInspector ... inspectors) {
        this(null, null, null, inspectors);
    }

    /**
     * <p>
     * This class configures default engines in the {@link com.github.wuic.ContextBuilder}.
     * </p>
     *
     * @author Guillaume DROUET
     * @version 1.1
     * @since 0.4.0
     */
    class DefaultEngineContextBuilderConfigurator extends ContextBuilderConfigurator {

        /**
         * {@inheritDoc}
         */
        @Override
        public int internalConfigure(final ContextBuilder ctxBuilder) {
            for (final ObjectBuilderFactory.KnownType type : engineBuilderFactory.knownTypes()) {
                ctxBuilder.contextEngineBuilder(BUILDER_ID_PREFIX + type.getTypeName(), type.getTypeName()).toContext();
            }

            // Never poll
            return -1;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getTag() {
            return "default.engine";
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected Long getLastUpdateTimestampFor(final String path) throws IOException {
            // Never poll
            return 1L;
        }
    }

    /**
     * <p>
     * Sets the filters configured in the given instance if it's a {@link NutFilterHolder}.
     * </p>
     *
     * @author Guillaume DROUET
     * @version 1.0
     * @since 0.5.0
     */
    private final class NutFilterHolderInspector implements ObjectBuilderInspector {

        /**
         * {@inheritDoc}
         */
        @Override
        public <T> T inspect(final T object) {
            if (NutFilterHolder.class.isAssignableFrom(object.getClass())) {
                NutFilterHolder.class.cast(object).setNutFilter(getFilters());
            }

            return object;
        }
    }

    /**
     * <p>
     * This class configures default DAOs in the {@link com.github.wuic.ContextBuilder}.
     * </p>
     *
     * @author Guillaume DROUET
     * @version 1.1
     * @since 0.4.0
     */
    private final class DefaultDaoContextBuilderConfigurator extends ContextBuilderConfigurator {

        /**
         * {@inheritDoc}
         */
        @Override
        public int internalConfigure(final ContextBuilder ctxBuilder) {
            for (final ObjectBuilderFactory.KnownType type : nutDaoBuilderFactory.knownTypes()) {
                ctxBuilder.contextNutDaoBuilder(BUILDER_ID_PREFIX + type.getTypeName(), type.getTypeName()).toContext();
            }

            // Never poll
            return -1;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getTag() {
            return "default.dao";
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected Long getLastUpdateTimestampFor(final String path) throws IOException {
            // Never poll
            return 1L;
        }
    }

    /**
     * <p>
     * A registration for a {@link NutsHeap} to be created when the context is built.
     * </p>
     *
     * @author Guillaume DROUET
     * @version 1.0
     * @since 0.5.1
     */
    private final class HeapRegistration {

        /**
         * If the heap will be disposable.
         */
        private final boolean disposable;

        /**
         * The {@link NutDao} builder id.
         */
        private final String ndbId;

        /**
         * The paths not already filtered.
         */
        private final String[] paths;

        /**
         * The listener that will be notified by the heap.
         */
        private final HeapListener[] listeners;

        /**
         * The created heap.
         */
        private NutsHeap heap;

        /**
         * Some IDs of other heaps to create a composition.
         */
        private String[] heapIds;

        /**
         * <p>
         * Builds a new registration instance.
         * </p>
         *
         * @param disposable disposable or not
         * @param ndbId ID for DAO resolution
         * @param heapIds the composition
         * @param paths the paths for this heap
         * @param listeners the listeners
         */
        private HeapRegistration(final boolean disposable, final String ndbId, final String[] heapIds, final String[] paths, final HeapListener ... listeners) {
            this.disposable = disposable;
            this.ndbId = ndbId;
            this.listeners = listeners;

            this.paths = new String[paths.length];
            System.arraycopy(paths, 0, this.paths, 0, paths.length);

            if (heapIds != null) {
                this.heapIds = new String[heapIds.length];
                System.arraycopy(heapIds, 0, this.heapIds, 0, heapIds.length);
            }
        }

        /**
         * <p>
         * Gets the {@link NutsHeap} if the object has been already created with a call to {@link #getHeap(String, java.util.Map)}.
         * </p>
         *
         * @return the {@link NutsHeap} if already created, {@code null} otherwise
         */
        private NutsHeap getIfCreated() {
            return heap;
        }

        /**
         * <p>
         * Gets the {@link NutsHeap} created by this registration. The heap is created when the first call to this
         * method performed. Then, the same instance will be returned for future calls.
         * </p>
         *
         * @param id the ID for this heap
         * @param daoCollection a collection of DAOs where {@link #ndbId} will be resolved
         * @return the heap
         * @throws IOException if creation fails
         */
        private NutsHeap getHeap(final String id, final Map<String, NutDao> daoCollection) throws IOException{
            if (heap != null) {
                return heap;
            }

            NutDao dao = null;

            // Find DAO
            if (ndbId != null) {
                dao = daoCollection.get(ndbId);
            }

            // Check content and apply filters
            final List<String> pathList = pathList(dao, ndbId, paths);

            // Composition detected, collected nested and referenced heaps
            if (heapIds != null && heapIds.length != 0) {
                final List<NutsHeap> composition = new ArrayList<NutsHeap>();

                for (final String regex : heapIds) {
                    for (final Map.Entry<String, HeapRegistration> registration : getNutsHeap(regex).entrySet()) {
                        composition.add(registration.getValue().getHeap(registration.getKey(), daoCollection));
                    }
                }

                heap = new NutsHeap(currentTag, pathList, disposable, dao, id, composition.toArray(new NutsHeap[composition.size()]));
            } else {
                heap = new NutsHeap(currentTag, pathList, disposable, dao, id);
            }

            for (final HeapListener l : listeners) {
                heap.addObserver(l);
            }

            return heap;
        }
    }

    /**
     * <p>
     * A registration for a {@link WorkflowTemplate} to be created when the context is built.
     * </p>
     *
     * @author Guillaume DROUET
     * @version 1.0
     * @since 0.5.1
     */
    private final class WorkflowTemplateRegistration {

        /**
         * The engine builder IDs to include in the template.
         */
        private final String[] ebIds;

        /**
         * The excluded IDs.
         */
        private final String[] ebIdsExclusion;

        /**
         * Include default engines or not.
         */
        private final Boolean includeDefaultEngines;

        /**
         * The DAO builder IDs to store nut.
         */
        private final String[] ndbIds;

        /**
         * <p>
         * Builds a new registration.
         * </p>
         *
         * @param ebIds the specific engines
         * @param ebIdsExclusion engines to exclude
         * @param includeDefaultEngines include default engines or not
         * @param ndbIds some DAO builder IDs to store nut
         */
        private WorkflowTemplateRegistration(final String[] ebIds,
                                             final String[] ebIdsExclusion,
                                             final Boolean includeDefaultEngines,
                                             final String[] ndbIds) {
            this.includeDefaultEngines = includeDefaultEngines;
            this.ebIds = new String[ebIds.length];
            System.arraycopy(ebIds, 0, this.ebIds, 0, ebIds.length);

            if (ebIdsExclusion != null) {
                this.ebIdsExclusion = new String[ebIdsExclusion.length];
                System.arraycopy(ebIdsExclusion, 0, this.ebIdsExclusion, 0, ebIdsExclusion.length);
            } else {
                this.ebIdsExclusion = ebIdsExclusion;
            }

            this.ndbIds = new String[ndbIds.length];
            System.arraycopy(ndbIds, 0, this.ndbIds, 0, ndbIds.length);
        }

        /**
         * <p>
         * Gets a new {@link WorkflowTemplate} for this registration.
         * </p>
         *
         * <p>
         * If the registered DAO does not exists don't support save operations
         * ({@link com.github.wuic.nut.dao.NutDao#saveSupported()}), then an {@link IllegalStateException}
         * is thrown.
         * </p>
         *
         * @param daoCollection the collection of DAO for ID resolution
         * @return the template
         */
        private WorkflowTemplate getTemplate(final Map<String, NutDao> daoCollection) {
            final NutDao[] nutDaos = collect(daoCollection);

            // Retrieve each engine associated to all provided IDs and heap them by nut type
            final Map<NutType, NodeEngine> chains = createChains(includeDefaultEngines, ebIdsExclusion);
            HeadEngine head = null;

            for (final String ebId : ebIds) {
                // Create a different instance per chain
                final Engine engine = newEngine(ebId);

                if (engine == null) {
                    throw new IllegalStateException(String.format("'%s' not associated to any %s", ebId, EngineService.class.getName()));
                } else {

                    if (engine instanceof HeadEngine) {
                        head = HeadEngine.class.cast(engine);
                    } else {
                        final NodeEngine node = NodeEngine.class.cast(engine);
                        final List<NutType> nutTypes = node.getNutTypes();

                        for (final NutType nt : nutTypes) {
                            // Already exists
                            if (chains.containsKey(nt)) {
                                chains.put(nt, NodeEngine.chain(chains.get(nt), NodeEngine.class.cast(newEngine(ebId))));
                            } else {
                                // Create first entry
                                chains.put(nt, node);
                            }
                        }
                    }
                }
            }

            return new WorkflowTemplate(head, chains, nutDaos);
        }

        /**
         * <p>
         * Collects in the given map the referenced DAOs.
         * </p>
         *
         * @param daoCollection all the instantiated DAOs
         * @return the collected DAOs
         */
        private NutDao[] collect(final Map<String, NutDao> daoCollection) {
            // Retrieve each DAO associated to all provided IDs
            final NutDao[] nutDaos = new NutDao[ndbIds.length];

            for (int i = 0; i < ndbIds.length; i++) {
                final String ndbId = ndbIds[i];
                final NutDao dao = daoCollection.get(ndbId);

                if (dao == null) {
                    throw new IllegalStateException(String.format("'%s' not associated to any %s", ndbId, NutDaoService.class.getName()));
                }

                if (!dao.saveSupported()) {
                    throw new IllegalStateException(String.format("DAO built by '%s' does not supports save", ndbId));
                }

                nutDaos[i] = dao;
            }

            return nutDaos;
        }
    }

    /**
     * <p>
     * A registration for a {@link Workflow} to be created when the context is built.
     * </p>
     *
     * @author Guillaume DROUET
     * @version 1.0
     * @since 0.5.1
     */
    private final class WorkflowRegistration {

        /**
         * Create a workflow for each heap.
         */
        private final Boolean forEachHeap;

        /**
         * The heap pattern to resolve related heap.
         */
        private final String heapIdPattern;

        /**
         * A template ID.
         */
        private final String workflowTemplateId;

        /**
         * <p>
         *  Builds a new registration
         * </p>
         *
         * @param forEachHeap for each heap or not
         * @param heapIdPattern heap resolver pattern
         * @param workflowTemplateId an optional workflow
         */
        private WorkflowRegistration(final Boolean forEachHeap,
                                     final String heapIdPattern,
                                     final String workflowTemplateId) {
            this.forEachHeap = forEachHeap;
            this.heapIdPattern = heapIdPattern;
            this.workflowTemplateId  = workflowTemplateId;
        }

        /**
         * <p>
         * Gets a new map of {@link Workflow} for this registration.
         * </p>
         *
         * @param identifier the workflow ID
         * @param daoCollection a collection of DAO for heap creation
         * @return the new workflow
         * @throws WorkflowTemplateNotFoundException if the workflow template does not exists
         * @throws IOException if heap creation fails
         */
        private Map<String, Workflow> getWorkflowMap(final String identifier, final Map<String, NutDao> daoCollection)
                throws WorkflowTemplateNotFoundException, IOException {
            final WorkflowTemplate template = getWorkflowTemplate(workflowTemplateId, daoCollection);
            final Map<String, Workflow> retval = new HashMap<String, Workflow>();

            if (template == null) {
                WuicException.throwWorkflowTemplateNotFoundException(workflowTemplateId);
                return null;
            }

            final Map<NutType, ? extends NodeEngine> chains = template.getChains();
            final NutDao[] nutDaos = template.getStores();

            // Retrieve HEAP
            final Map<String, HeapRegistration> heaps = getNutsHeap(heapIdPattern);

            if (heaps.isEmpty()) {
                throw new IllegalStateException(String.format("'%s' is a regex which doesn't match any %s", heapIdPattern, NutsHeap.class.getName()));
            }

            final String id = identifier.substring(0, identifier.length() - heapIdPattern.length());

            if (forEachHeap) {
                for (final Map.Entry<String, HeapRegistration> heap : heaps.entrySet()) {
                    final String loopId = id + heap.getKey();
                    if (NumberUtils.isNumber(loopId)) {
                        WuicException.throwBadArgumentException(new IllegalArgumentException(
                                String.format("Workflow ID %s cannot be a numeric value", loopId)));
                    }

                    retval.put(loopId, new Workflow(template.getHead(), chains, heap.getValue().getHeap(heap.getKey(), daoCollection), nutDaos));
                }
            } else {
                if (NumberUtils.isNumber(id)) {
                    WuicException.throwBadArgumentException(new IllegalArgumentException(
                            String.format("Workflow ID %s cannot be a numeric value", id)));
                }

                final NutsHeap[] array = new NutsHeap[heaps.size()];
                int cpt = 0;

                for (final Map.Entry<String, HeapRegistration> heap : heaps.entrySet()) {
                    array[cpt++] = heap.getValue().getHeap(heap.getKey(), daoCollection);
                }

                final NutsHeap heap = new NutsHeap(currentTag, null, null, heapIdPattern, array);
                retval.put(id, new Workflow(template.getHead(), chains, heap));
            }

            return retval;
        }
    }

    /**
     * <p>
     * A registration for a {@link NutDao} to be created when the context is built.
     * </p>
     *
     * @author Guillaume DROUET
     * @version 1.0
     * @since 0.5.1
     */
    private final class NutDaoRegistration {

        /**
         * The builder.
         */
        private final ObjectBuilder<NutDao> nutDaoBuilder;

        /**
         * Some DAO to proxy.
         */
        private final Map<String, NutDaoRegistration> proxyDao;

        /**
         * Some nut to proxy.
         */
        private final Map<String, Nut> proxyNut;

        /**
         * A root path for all proxy paths.
         */
        private String proxyRootPath;

        /**
         * The created DAO.
         */
        private NutDao dao;

        /**
         * <p>
         * Builds a new registration for the given builder.
         * </p>
         *
         * @param nutDaoBuilder the DAO builder
         */
        private NutDaoRegistration(final ObjectBuilder<NutDao> nutDaoBuilder) {
            this.proxyDao = new HashMap<String, NutDaoRegistration>();
            this.proxyNut = new HashMap<String, Nut>();
            this.nutDaoBuilder = nutDaoBuilder;
            this.proxyRootPath = "";
        }

        /**
         * <p>
         * Gets the proxy for nuts.
         * </p>
         *
         * @return the proxy nut
         */
        private Map<String, Nut> getProxyNut() {
            return proxyNut;
        }

        /**
         * <p>
         * Gets the proxy for DAO.
         * </p>
         *
         * @return the DAO
         */
        private Map<String, NutDaoRegistration> getProxyDao() {
            return proxyDao;
        }

        /**
         * <p>
         * Sets the proxy root path.
         * </p>
         *
         * @param proxyRootPath the new root path
         */
        private void setProxyRootPath(final String proxyRootPath) {
            this.proxyRootPath = proxyRootPath;
        }

        /**
         * <p>
         * Configures the wrapped builder with the specified properties.
         * </p>
         *
         * @param properties the properties
         * @return this registration
         */
        private NutDaoRegistration configure(final Map<String, Object> properties) {
            ContextBuilder.this.configure(nutDaoBuilder, properties);
            return this;
        }

        /**
         * <p>
         * Gets the {@link NutDao} if the object has been already created with a call to {@link #getNutDao()}.
         * </p>
         *
         * @return the {@link NutDao} if already created, {@code null} otherwise
         */
        private NutDao getIfCreated() {
            return dao;
        }

        /**
         * <p>
         * Builds a new {@link NutDao} for this registration. The DAO is created when the first call to this
         * method performed. Then, the same instance will be returned for future calls. If the proxy settings
         * have changed, the instance will be modified to provide an up to date state.
         * </p>
         *
         * @return the DAO
         */
        private NutDao getNutDao() {
            if (!proxyNut.isEmpty() || !proxyDao.isEmpty()) {
                final ProxyNutDao proxy;

                if (dao != null) {
                    if (dao instanceof ProxyNutDao) {
                        proxy = ProxyNutDao.class.cast(dao);
                    } else {
                        proxy = new ProxyNutDao(proxyRootPath, dao);
                    }
                } else {
                    proxy = new ProxyNutDao(proxyRootPath, nutDaoBuilder.build());
                }

                for (final Map.Entry<String, Nut> entry : proxyNut.entrySet()) {
                    proxy.addRule(entry.getKey(), entry.getValue());
                }

                for (final Map.Entry<String, NutDaoRegistration> entry : proxyDao.entrySet()) {
                    proxy.addRule(entry.getKey(), entry.getValue().getNutDao());
                }

                dao = proxy;
            } else if (dao == null) {
                dao = nutDaoBuilder.build();
            }

            return dao;
        }
    }

    /**
     * <p>
     * Internal class used to track settings associated to a particular tag.
     * </p>
     *
     * @author Guillaume DROUET
     * @version 1.1
     * @since 0.4.0
     */
    private static class ContextSetting {

        /**
         * All DAO registration with their {@link ObjectBuilder} associated to their builder ID.
         */
        private Map<String, NutDaoRegistration> nutDaoMap = new HashMap<String, NutDaoRegistration>();

        /**
         * All {@link NutFilter daos} associated to their builder ID.
         */
        private Map<String, NutFilter> nutFilterMap = new HashMap<String, NutFilter>();

        /**
         * All {@link com.github.wuic.config.ObjectBuilder} building {@link Engine} associated to their builder ID.
         */
        private Map<String, ObjectBuilder<Engine>> engineMap = new HashMap<String, ObjectBuilder<Engine>>();

        /**
         * All {@link HeapRegistration heaps} associated to their ID.
         */
        private Map<String, HeapRegistration> nutsHeaps = new HashMap<String, HeapRegistration>();

        /**
         * All {@link WorkflowTemplate templates} {@link WorkflowTemplateRegistration registration} associated to their ID.
         */
        private Map<String, WorkflowTemplateRegistration> templates = new HashMap<String, WorkflowTemplateRegistration>();

        /**
         * All {@link Workflow workflows} associated to their ID.
         */
        private Map<String, WorkflowRegistration> workflowMap = new HashMap<String, WorkflowRegistration>();

        /**
         * All {@link ContextInterceptor interceptors}.
         */
        private List<ContextInterceptor> interceptorsList = new ArrayList<ContextInterceptor>();

        /**
         * <p>
         * Gets the {@link NutDao} associated to an ID.
         * </p>
         *
         * @return the map
         */
        public Map<String, NutDaoRegistration> getNutDaoMap() {
            return nutDaoMap;
        }

        /**
         * <p>
         * Gets the {@link NutFilter} associated to an ID.
         * </p>
         *
         * @return the map
         */
        public Map<String, NutFilter> getNutFilterMap() {
            return nutFilterMap;
        }


        /**
         * <p>
         * Gets the {@link com.github.wuic.config.ObjectBuilder} associated to an ID.
         * </p>
         *
         * @return the map
         */
        public Map<String, ObjectBuilder<Engine>> getEngineMap() {
            return engineMap;
        }

        /**
         * <p>
         * Gets the {@link HeapRegistration} associated to an ID.
         * </p>
         *
         * @return the map
         */
        public Map<String, HeapRegistration> getNutsHeaps() {
            return nutsHeaps;
        }

        /**
         * <p>
         * Gets the {@link WorkflowRegistration} associated to an ID.
         * </p>
         *
         * @return the map
         */
        public Map<String, WorkflowRegistration> getWorkflowMap() {
            return workflowMap;
        }

        /**
         * <p>
         * Gets the {@link WorkflowTemplateRegistration} associated to an ID.
         * </p>
         *
         * @return the map
         */
        public Map<String, WorkflowTemplateRegistration> getTemplateMap() {
            return templates;
        }

        /**
         * <p>
         * Gets the {@link ContextInterceptor interceptors}.
         * </p>
         *
         * @return the list
         */
        private List<ContextInterceptor> getInterceptorsList() {
            return interceptorsList;
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
     * Gets the default builder name for the given component class.
     * </p>
     *
     * @param component the component class
     * @return the default ID
     */
    public static String getDefaultBuilderId(final Class<?> component) {
        return "wuicDefault" + component.getSimpleName() + "Builder";
    }

    /**
     * <p>
     * Configures for each type provided by the engine builder factory and nut dao builder factory a
     * default instance identified with an id starting by {@link #BUILDER_ID_PREFIX} and followed by the type
     * name itself.
     * </p>
     *
     * @return the current builder
     * @throws IOException if configuration fails
     */
    public ContextBuilder configureDefault() throws IOException {
        if (!configureDefault) {
            new DefaultEngineContextBuilderConfigurator().configure(this);
            new DefaultDaoContextBuilderConfigurator().configure(this);
            configureDefault = true;
        }

        return this;
    }

    /**
     * <p>
     * Builds a new {@link NutDao} builder.
     * </p>
     *
     * @param type the type of DAO
     * @return the builder
     */
    public synchronized ObjectBuilder<NutDao> newNutDaoBuilder(final String type) {
        return nutDaoBuilderFactory.create(type);
    }

    /**
     * <p>
     * Decorates the current builder with a new builder associated to a specified tag. Tagging the context allows to
     * isolate a set of configurations that could be erased by calling {@link ContextBuilder#clearTag(Object)}.
     * This way, this feature is convenient when you need to poll the configurations to reload it.
     * </p>
     *
     * <p>
     * All configurations will be associated to the tag until the {@link com.github.wuic.ContextBuilder#releaseTag()}
     * method is called. If tag is currently set, then it is released when this method is called with a new tag.
     * </p>
     *
     * @param tag an arbitrary object which represents the current tag
     * @return the current builder which will associates all configurations to the tag
     * @see ContextBuilder#clearTag(Object)
     * @see com.github.wuic.ContextBuilder#releaseTag()
     */
    public ContextBuilder tag(final Object tag) {
        lock.lock();
        log.debug("ContextBuilder locked by {}", Thread.currentThread().toString());

        if (currentTag != null) {
            releaseTag();
        }

        currentTag = tag;
        setChanged();
        notifyObservers(tag);
        return this;
    }

    /**
     * <p>
     * Clears all configurations associated to the given tag.
     * </p>
     *
     * @param tag the tag
     * @return this {@link ContextBuilder}
     */
    public ContextBuilder clearTag(final Object tag) {
        try {
            if (!lock.isHeldByCurrentThread()) {
                lock.lock();
            }

            final ContextSetting setting = taggedSettings.remove(tag);

            // Shutdown all DAO (scheduled jobs, etc)
            if (setting != null) {
                for (final NutDaoRegistration dao : setting.nutDaoMap.values()) {
                    final NutDao d = dao.getIfCreated();

                    if (d != null) {
                        d.shutdown();
                    }
                }

                // Notifies any listeners to clear any cache
                for (final HeapRegistration heap : setting.getNutsHeaps().values()) {
                    final NutsHeap h = heap.getIfCreated();

                    if (h != null) {
                        h.notifyListeners(h);
                    }
                }
            }

            setChanged();
            notifyObservers(tag);

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

    /**
     * <p>
     * Inner class to configure a generic component.
     * </p>
     *
     * @author Guillaume DROUET
     * @version 1.0
     * @since 0.4.4
     */
    public abstract class ContextGenericBuilder {

        /**
         * The ID.
         */
        private String id;

        /**
         * The properties.
         */
        private Map<String, Object> properties;

        /**
         * <p>
         * Builds a new component identified by the given ID.
         * </p>
         *
         * @param id the ID
         */
        public ContextGenericBuilder(final String id) {
            this.id = id;
            this.properties = new HashMap<String, Object>();
        }

        /**
         * <p>
         * Gets the ID.
         * </p>
         *
         * @return the ID
         */
        protected String getId() {
            return id;
        }

        /**
         * <p>
         * Gets the properties
         * </p>
         *
         * @return the properties
         */
        protected Map<String, Object> getProperties() {
            return properties;
        }

        /***
         * <p>
         * Configures the given property.
         * </p>
         *
         * @param key the property key
         * @param value the property value
         * @return this
         */
        public abstract ContextGenericBuilder property(String key, Object value);

        /**
         * <p>
         * Injects in the enclosing builder the component with its settings and return it.
         * </p>
         *
         * <p>
         * Throws an {@link IllegalArgumentException} if a previously configured property is not supported
         * </p>
         *
         * @return the enclosing builder
         */
        public abstract ContextBuilder toContext();
    }

    /**
     * <p>
     * Inner class to configure a engine builder.
     * </p>
     *
     * @author Guillaume DROUET
     * @version 1.0
     * @since 0.4.4
     */
    public class ContextEngineBuilder extends ContextGenericBuilder {

        /**
         * The builder.
         */
        private ObjectBuilder<Engine> engineBuilder;

        /**
         * <p>
         * Builds a new instance.
         * </p>
         *
         * @param id the builder ID
         * @param type the builder type
         */
        public ContextEngineBuilder(final String id, final String type) {
            super(id);
            this.engineBuilder = engineBuilderFactory.create(type);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public ContextEngineBuilder property(final String key, final Object value) {
            getProperties().put(key, value);
            return this;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public ContextBuilder toContext() {
            engineBuilder(getId(), engineBuilder, getProperties());
            return ContextBuilder.this;
        }
    }

    /**
     * <p>
     * Inner class to configure a DAO builder.
     * </p>
     *
     * @author Guillaume DROUET
     * @version 1.0
     * @since 0.4.4
     */
    public class ContextNutDaoBuilder extends ContextGenericBuilder {

        /**
         * The wrapped registration.
         */
        private final NutDaoRegistration registration;

        /**
         * <p>
         * Builds a new instance.
         * </p>
         *
         * @param id the builder ID
         * @param type the builder type
         */
        public ContextNutDaoBuilder(final String id, final String type) {
            super(id);
            this.registration = new NutDaoRegistration(nutDaoBuilderFactory.create(type));
        }

        /**
         * <p>
         * Builds a new instance with an existing registration.
         * </p>
         *
         * @param id the builder ID
         * @param registration the resgistration
         */
        public ContextNutDaoBuilder(final String id, final NutDaoRegistration registration) {
            super(id);
            this.registration = registration;
        }

        /**
         * <p>
         * Specifies a root path for paths to proxy.
         * </p>
         *
         * @param path the root path
         * @return this
         */
        public ContextNutDaoBuilder proxyRootPath(final String path) {
            registration.setProxyRootPath(path);
            return this;
        }

        /**
         * <p>
         * Specifies a path to proxy to a DAO.
         * </p>
         *
         * @param path the path
         * @param id the DAO id
         * @return this
         */
        public ContextNutDaoBuilder proxyPathForDao(final String path, final String id) {
            for (final ContextSetting s : taggedSettings.values()) {
                final NutDaoRegistration reg = s.getNutDaoMap().get(id);

                if (reg != null) {
                    registration.getProxyDao().put(path, reg);
                    return this;
                }
            }

            return this;
        }

        /**
         * <p>
         * Specifies a path to proxy to a nut.
         * </p>
         *
         * @param path the path
         * @param nut the nut
         * @return this
         */
        public ContextNutDaoBuilder proxyPathForNut(final String path, final Nut nut) {
            registration.getProxyNut().put(path, nut);
            return this;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public ContextNutDaoBuilder property(final String key, final Object value) {
            getProperties().put(key, value);
            return this;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public ContextBuilder toContext() {
            nutDaoBuilder(getId(), registration, getProperties());
            return ContextBuilder.this;
        }
    }

    /**
     * <p>
     * Inner class to configure a filter builder.
     * </p>
     *
     * @author Guillaume DROUET
     * @version 1.0
     * @since 0.4.5
     */
    public class ContextNutFilterBuilder extends ContextGenericBuilder {

        /**
         * The builder.
         */
        private ObjectBuilder<NutFilter> nutFilterBuilder;

        /**
         * <p>
         * Builds a new instance.
         * </p>
         *
         * @param id the builder ID
         * @param type the builder type
         */
        public ContextNutFilterBuilder(final String id, final String type) {
            super(id);
            this.nutFilterBuilder = nutFilterBuilderFactory.create(type);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public ContextNutFilterBuilder property(final String key, final Object value) {
            getProperties().put(key, value);
            return this;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public ContextBuilder toContext() {
            nutFilterBuilder(getId(), nutFilterBuilder, getProperties());
            return ContextBuilder.this;
        }
    }

    /**
     * <p>
     * Returns a new context DAO builder.
     * </p>
     *
     * @param id the final builder's ID
     * @param type the final builder's type
     * @return the specific context builder
     */
    public ContextNutDaoBuilder contextNutDaoBuilder(final String id, final String type) {
        for (final ContextSetting setting : taggedSettings.values()) {
            final NutDaoRegistration registration = setting.getNutDaoMap().get(id);

            if (registration != null) {
                return new ContextNutDaoBuilder(id, registration);
            }
        }

        return new ContextNutDaoBuilder(id, type);
    }

    /**
     * <p>
     * Returns a new default context DAO builder.
     * </p>
     *
     * @param type the component to build
     * @return the specific context builder
     */
    public ContextNutDaoBuilder contextNutDaoBuilder(final Class<?> type) {
        return contextNutDaoBuilder(getDefaultBuilderId(type), type);
    }

    /**
     * <p>
     * Returns a new default context DAO builder associated to a particular ID.
     * </p>
     *
     * @param id the specific ID
     * @param type the component to build
     * @return the specific context builder
     */
    public ContextNutDaoBuilder contextNutDaoBuilder(final String id, final Class<?> type) {
        return contextNutDaoBuilder(id, type.getSimpleName() + "Builder");
    }

    /**
     * <p>
     * Returns a new context DAO builder associated to a particular ID and based on an existing context DAO builder.
     * </p>
     *
     * @param id the specific ID
     * @param cloneId the ID of the existing builder to clone
     * @return the specific context builder
     */
    public ContextNutDaoBuilder cloneContextNutDaoBuilder(final String id, final String cloneId) {
        for (final ContextSetting setting : taggedSettings.values()) {
            final NutDaoRegistration registration = setting.getNutDaoMap().get(cloneId);

            if (registration != null) {
                return new ContextNutDaoBuilder(id, registration);
            }
        }

        WuicException.throwBadArgumentException(new IllegalArgumentException(
                String.format("%s must be an existing NutDao builder to be cloned", cloneId)));
        return null;
    }

    /**
     * <p>
     * Returns a new context filter builder.
     * </p>
     *
     * @param id the final builder's ID
     * @param type the final builder's type
     * @return the specific context builder
     */
    public ContextNutFilterBuilder contextNutFilterBuilder(final String id, final String type) {
        return new ContextNutFilterBuilder(id, type);
    }

    /**
     * <p>
     * Returns a new context engine builder.
     * </p>
     *
     * @param id the final builder's ID
     * @param type the final builder's type
     * @return the specific context builder
     */
    public ContextEngineBuilder contextEngineBuilder(final String id, final String type) {
        return new ContextEngineBuilder(id, type);
    }

    /**
     * <p>
     * Returns a new context default engine builder.
     * </p>
     *
     * @param type the component to build
     * @return the specific context builder
     */
    public ContextEngineBuilder contextEngineBuilder(final Class<?> type) {
        return new ContextEngineBuilder(getDefaultBuilderId(type), type.getSimpleName() + "Builder");
    }

    /**
     * <p>
     * Adds a {@link ContextInterceptor} to the builder.
     * </p>
     *
     * @param interceptor the interceptor
     * @return this {@link ContextBuilder}
     */
    public ContextBuilder interceptor(final ContextInterceptor interceptor) {
        final ContextSetting setting = getSetting();
        setting.getInterceptorsList().add(interceptor);
        taggedSettings.put(currentTag, setting);
        setChanged();
        notifyObservers();
        return this;
    }

    /**
     * <p>
     * Add a new {@link NutDaoRegistration} identified by the specified ID.
     * </p>
     *
     * @param id the ID which identifies the builder in the context
     * @param registration the registration associated to its ID
     * @return this {@link ContextBuilder}
     */
    private ContextBuilder nutDao(final String id, final NutDaoRegistration registration) {
        final ContextSetting setting = getSetting();

        // Will override existing element
        for (final ContextSetting s : taggedSettings.values()) {
            s.nutDaoMap.remove(id);
        }

        setting.nutDaoMap.put(id, registration);
        taggedSettings.put(currentTag, setting);
        setChanged();
        notifyObservers(id);

        return this;
    }

    /**
     * <p>
     * Add a new {@link com.github.wuic.nut.filter.NutFilter} identified by the specified ID.
     * </p>
     *
     * @param id the ID which identifies the builder in the context
     * @param filter the filter associated to its ID
     * @return this {@link ContextBuilder}
     */
    public ContextBuilder nutFilter(final String id, final NutFilter filter) {
        final ContextSetting setting = getSetting();

        // Will override existing element
        for (final ContextSetting s : taggedSettings.values()) {
            s.nutFilterMap.remove(id);
        }

        setting.nutFilterMap.put(id, filter);
        taggedSettings.put(currentTag, setting);
        setChanged();
        notifyObservers(id);

        return this;
    }

    /**
     * <p>
     * Add a new {@link com.github.wuic.engine.Engine} builder identified by the specified ID.
     * </p>
     *
     * @param id the ID which identifies the builder in the context
     * @param engine the engine builder associated to its ID
     * @return this {@link ContextBuilder}
     */
    public ContextBuilder engineBuilder(final String id, final ObjectBuilder<Engine> engine) {
        final ContextSetting setting = getSetting();

        // Will override existing element
        for (final ContextSetting s : taggedSettings.values()) {
            s.engineMap.remove(id);
        }

        setting.engineMap.put(id, engine);
        taggedSettings.put(currentTag, setting);
        setChanged();
        notifyObservers(id);

        return this;
    }

    /**
     * <p>
     * Adds a new {@link com.github.wuic.nut.dao.NutDao} builder identified by the specified ID.
     * </p>
     *
     * <p>
     * If some properties are not supported by the builder, then an exception will be thrown.
     * </p>
     *
     * <p>
     * Throws an {@link IllegalArgumentException} if a property is not supported by the builder.
     * </p>
     *
     * @param id the ID which identifies the builder in the context
     * @param daoRegistration the registration wrapping the builder associated to its ID
     * @param properties the properties to use to configure the builder
     * @return this {@link ContextBuilder}
     */
    private ContextBuilder nutDaoBuilder(final String id,
                                         final NutDaoRegistration daoRegistration,
                                         final Map<String, Object> properties) {
        final ContextSetting setting = getSetting();

        // Will override existing element
        for (final ContextSetting s : taggedSettings.values()) {
            s.nutDaoMap.remove(id);
        }

        setting.nutDaoMap.put(id, daoRegistration.configure(properties));
        taggedSettings.put(currentTag, setting);
        setChanged();
        notifyObservers(id);

        return this;
    }

    /**
     * <p>
     * Adds a new {@link com.github.wuic.nut.filter.NutFilter} builder identified by the specified ID.
     * </p>
     *
     * <p>
     * If some properties are not supported by the builder, then an exception will be thrown.
     * </p>
     *
     * @param id the ID which identifies the builder in the context
     * @param filterBuilder the builder associated to its ID
     * @param properties the properties to use to configure the builder
     * @return this {@link ContextBuilder}
     */
    private ContextBuilder nutFilterBuilder(final String id,
                                            final ObjectBuilder<NutFilter> filterBuilder,
                                            final Map<String, Object> properties) {
        final ContextSetting setting = getSetting();

        // Will override existing element
        for (final ContextSetting s : taggedSettings.values()) {
            s.nutFilterMap.remove(id);
        }

        setting.nutFilterMap.put(id, configure(filterBuilder, properties).build());
        taggedSettings.put(currentTag, setting);
        setChanged();
        notifyObservers(id);

        return this;
    }

    /**
     * <p>
     * Creates a new heap as specified by {@link ContextBuilder#heap(boolean, String, String, String[], String[], HeapListener...)}
     * without any composition and which is not disposable.
     * </p>
     *
     * @param id the heap ID
     * @param ndbId the {@link com.github.wuic.nut.dao.NutDao} builder the heap is based on
     * @param path the path
     * @param listeners some listeners for this heap
     * @return this {@link ContextBuilder}
     * @throws IOException if the HEAP could not be created
     */
    public ContextBuilder heap(final String id, final String ndbId, final String[] path, final HeapListener ... listeners)
            throws IOException {
        return heap(false, id, ndbId, null, path, listeners);
    }

    /**
     * <p>
     * Creates a new disposable heap as specified by {@link ContextBuilder#heap(boolean, String, String, String[], String[], HeapListener...)}
     * without any composition.
     * </p>
     *
     * @param id the heap ID
     * @param ndbId the {@link com.github.wuic.nut.dao.NutDao} builder the heap is based on
     * @param path the path
     * @param listeners some listeners for this heap
     * @return this {@link ContextBuilder}
     * @throws IOException if the HEAP could not be created
     */
    public ContextBuilder disposableHeap(final String id, final String ndbId, final String[] path, final HeapListener ... listeners)
            throws IOException {
        return heap(true, id, ndbId, null, path, listeners);
    }

    /**
     * <p>
     * Defines a new {@link com.github.wuic.nut.NutsHeap heap} in this context. A heap is always identified
     * by an ID and is associated to {@link com.github.wuic.nut.dao.NutDao} builder to use to convert paths into
     * {@link com.github.wuic.nut.Nut}. A list of paths needs also to be specified to know which underlying
     * nuts compose the heap.
     * </p>
     *
     * <p>
     * The heap could be composed in part or totally of other heaps.
     * </p>
     *
     * <p>
     * If the {@link com.github.wuic.config.ObjectBuilder} ID is not known, a {@link IllegalArgumentException}
     * will be thrown.
     * </p>
     *
     * @param disposable if the heap is disposable or not (see {@link com.github.wuic.nut.dao.NutDaoListener#isDisposable()}
     * @param id the heap ID
     * @param heapIds the heaps composition
     * @param ndbId the {@link com.github.wuic.nut.dao.NutDao} builder the heap is based on
     * @param path the path
     * @param listeners some listeners for this heap
     * @return this {@link ContextBuilder}
     * @throws IOException if the HEAP could not be created
     */
    public ContextBuilder heap(final boolean disposable, final String id, final String ndbId, final String[] heapIds, final String[] path, final HeapListener ... listeners)
            throws IOException {
        if (NumberUtils.isNumber(id)) {
            WuicException.throwBadArgumentException(new IllegalArgumentException(String.format("Heap ID %s cannot be a numeric value", id)));
        }

        // Will override existing element
        for (final ContextSetting s : taggedSettings.values()) {
            s.getNutsHeaps().remove(id);
        }

        final ContextSetting setting = getSetting();

        setting.getNutsHeaps().put(id, new HeapRegistration(disposable, ndbId, heapIds, path, listeners));

        taggedSettings.put(currentTag, setting);
        setChanged();
        notifyObservers(id);

        return this;
    }

    /**
     * <p>
     * This internal methods takes the declared paths specified in parameters, filters them and returns teh result.
     * </p>
     *
     * <p>
     * Also throws a {@link IllegalArgumentException} if their is at least one path while the specified DAO is {@code null}.
     * </p>
     *
     * @param dao the {@link NutDao} that creates {@link com.github.wuic.nut.Nut} with given path
     * @param ndbId the ID associated to the DAO
     * @param path the paths that represent the {@link com.github.wuic.nut.Nut nuts}
     * @return the filtered paths
     */
    private List<String> pathList(final NutDao dao, final String ndbId, final String ... path) {
        List<String> pathList;

        if (path.length != 0) {
            if (dao == null) {
                final String msg = String.format("'%s' does not correspond to any %s, add it with nutDaoBuilder() first",
                        ndbId, NutDaoService.class.getName());
                WuicException.throwBadArgumentException(new IllegalArgumentException(msg));
                return null;
            } else {
                // Going to filter the list with all declared filters
                pathList = CollectionUtils.newList(path);

                for (final ContextSetting s : taggedSettings.values()) {
                    for (final NutFilter filter : s.getNutFilterMap().values()) {
                        pathList = filter.filterPaths(pathList);
                    }
                }
            }
        } else {
            pathList = Arrays.asList();
        }

        return pathList;
    }

    /**
     * <p>
     * Declares a new {@link com.github.wuic.engine.Engine} builder with its specific properties.
     * The builder is identified by an unique ID and produces in fine {@link com.github.wuic.engine.Engine engines}
     * that could be chained.
     * </p>
     *
     * @param id the {@link com.github.wuic.config.ObjectBuilder} ID
     * @param engineBuilder the {@link com.github.wuic.engine.Engine} builder to configure
     * @param properties the builder's properties (must be supported by the builder)
     * @return this {@link ContextBuilder}
     */
    private ContextBuilder engineBuilder(final String id,
                                         final ObjectBuilder<Engine> engineBuilder,
                                         final Map<String, Object> properties) {
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
     * @param ebIds the set of {@link com.github.wuic.engine.Engine} builder to use
     * @param daos the DAO
     * @return this {@link ContextBuilder}
     * @throws IOException if an I/O error occurs
     * @see ContextBuilder#template(String, String[], String[], Boolean, String...)
     */
    public ContextBuilder template(final String id,
                                   final String[] ebIds,
                                   final String ... daos) throws IOException {
        return template(id, ebIds, null, Boolean.TRUE, daos);
    }

    /**
     * <p>
     * Creates a new workflow template.
     * </p>
     *
     * <p>
     * The template consists to chain a set of engines produced by the specified {@link com.github.wuic.engine.Engine} builders.
     * There is a chain for each possible {@link NutType}. A chain that processes a particular {@link NutType} of
     * {@link com.github.wuic.nut.Nut} is composed of {@link Engine engines} ordered by type. All engines specified in
     * parameter as array are simply organized following those two criteria to create the chains. Moreover, default engines
     * could be injected in the chain to perform common operations to be done on nuts. If an {@link com.github.wuic.engine.Engine} builder
     * is specified in a chain while it is injected by default, then the configuration of the given builder will overrides
     * the default one.
     * </p>
     *
     * <p>
     * A set of {@link com.github.wuic.nut.dao.NutDao} builder could be specified to store processed nuts. When the client
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
     *      <li>Unknown {@link com.github.wuic.config.ObjectBuilder} ID</li>
     *      <li>A {@link NutDao} does not supports {@link NutDao#save(com.github.wuic.nut.Nut)} method</li>
     *  </ul>
     * </p>
     *
     * @param id the template's id
     * @param ebIds the set of {@link com.github.wuic.engine.Engine} builder to use
     * @param ebIdsExclusion some default builder to be excluded in the chain
     * @param ndbIds the set of {@link com.github.wuic.nut.dao.NutDao} builder where to eventually upload processed nuts
     * @param includeDefaultEngines include or not default engines
     * @return this {@link ContextBuilder}
     * @throws IOException if an I/O error occurs
     */
    public ContextBuilder template(final String id,
                                   final String[] ebIds,
                                   final String[] ebIdsExclusion,
                                   final Boolean includeDefaultEngines,
                                   final String ... ndbIds) throws IOException {
        final ContextSetting setting = getSetting();
        setting.getTemplateMap().put(id, new WorkflowTemplateRegistration(ebIds, ebIdsExclusion, includeDefaultEngines, ndbIds));
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
     * @param workflowTemplateId a template ID to initialize the workflow
     * @return this {@link ContextBuilder}
     * @throws IOException if an I/O error occurs
     * @throws WorkflowTemplateNotFoundException if the specified template ID does not exists
     */
    public ContextBuilder workflow(final String identifier,
                                   final Boolean forEachHeap,
                                   final String heapIdPattern,
                                   final String workflowTemplateId)
            throws IOException, WorkflowTemplateNotFoundException {
        final ContextSetting setting = getSetting();
        final String id = identifier + heapIdPattern;

        // Will override existing element
        for (final ContextSetting s : taggedSettings.values()) {
            s.getWorkflowMap().remove(id);
        }

        setting.getWorkflowMap().put(id, new WorkflowRegistration(forEachHeap, heapIdPattern, workflowTemplateId));
        taggedSettings.put(currentTag, setting);
        setChanged();
        notifyObservers(identifier);

        return this;
    }

    /**
     * <p>
     * Gets the {@link NutFilter filters} currently configured in this builder.
     * </p>
     *
     * @return the filters
     */
    public List<NutFilter> getFilters() {
        final List<NutFilter> retval = new ArrayList<NutFilter>();

        for (final ContextSetting setting : taggedSettings.values()) {
            retval.addAll( setting.getNutFilterMap().values());
        }

        return retval;
    }

    /**
     * <p>
     * Merges all the {@link ContextSetting settings} of the given {@link ContextBuilder} to the current setting of
     * this object.
     * </p>
     *
     * @param other the object to merge
     * @return this
     */
    public ContextBuilder mergeSettings(final ContextBuilder other) {
        for (final ContextSetting s : other.taggedSettings.values()) {
            for (final Map.Entry<String, NutDaoRegistration> entry : s.getNutDaoMap().entrySet()) {
                nutDao(entry.getKey(), entry.getValue());
            }

            for (final Map.Entry<String, ObjectBuilder<Engine>> entry : s.getEngineMap().entrySet()) {
                engineBuilder(entry.getKey(), entry.getValue());
            }

            for (final Map.Entry<String, NutFilter> entry : s.getNutFilterMap().entrySet()) {
                nutFilter(entry.getKey(), entry.getValue());
            }

            for (final Map.Entry<String, HeapRegistration> entry : s.getNutsHeaps().entrySet()) {
                final ContextSetting setting = getSetting();
                s.getNutsHeaps().remove(entry.getKey());
                setting.getNutsHeaps().put(entry.getKey(), entry.getValue());
                taggedSettings.put(currentTag, setting);
            }

            for (final Map.Entry<String, WorkflowTemplateRegistration> entry : s.getTemplateMap().entrySet()) {
                final ContextSetting setting = getSetting();
                s.getTemplateMap().remove(entry.getKey());
                setting.getTemplateMap().put(entry.getKey(), entry.getValue());
                taggedSettings.put(currentTag, setting);
            }

            for (final Map.Entry<String, WorkflowRegistration> entry : s.getWorkflowMap().entrySet()) {
                final ContextSetting setting = getSetting();
                s.getWorkflowMap().remove(entry.getKey());
                setting.getWorkflowMap().put(entry.getKey(), entry.getValue());
                taggedSettings.put(currentTag, setting);
            }

            final ContextSetting setting = getSetting();
            setting.getInterceptorsList().addAll(s.getInterceptorsList());
            taggedSettings.put(currentTag, setting);
            setChanged();
            notifyObservers(setting);
        }

        return this;
    }

    /**
     * <p>
     * Builds the context. Should throws an {@link IllegalStateException} if the context is not correctly configured.
     * For instance: associate a heap to an undeclared {@link com.github.wuic.nut.dao.NutDao} builder ID.
     * </p>
     *
     * @return the new {@link Context}
     * @throws WuicException if context construction fails
     */
    public Context build() throws WuicException {

        // This thread needs to lock to build the context
        // However, if it is already done, the method should not unlock at the end of the method
        final boolean requiresLock = !lock.isHeldByCurrentThread();

        try {
            if (requiresLock) {
                lock.lock();
            }

            final Map<String, Workflow> workflowMap = new HashMap<String, Workflow>();
            final Map<String, NutsHeap> heapMap = new HashMap<String, NutsHeap>();
            final Map<String, NutDao> daoMap = new HashMap<String, NutDao>();

            for (final ContextSetting setting : taggedSettings.values()) {
                for (final Map.Entry<String, NutDaoRegistration> dao : setting.getNutDaoMap().entrySet()) {
                    daoMap.put(dao.getKey(), dao.getValue().getNutDao());
                }
            }

            // Add all specified workflow
            for (final ContextSetting setting : taggedSettings.values()) {
                for (final Map.Entry<String, WorkflowRegistration> entry : setting.getWorkflowMap().entrySet()) {
                    workflowMap.putAll(entry.getValue().getWorkflowMap(entry.getKey(), daoMap));
                }

                for (final Map.Entry<String, HeapRegistration> heap : setting.getNutsHeaps().entrySet()) {
                    heapMap.put(heap.getKey(), heap.getValue().getHeap(heap.getKey(), daoMap));
                }
            }

            // Create a default workflow for heaps not referenced by any workflow
            heapLoop :
            for (final NutsHeap heap : heapMap.values()) {
                for (final Workflow workflow : workflowMap.values()) {
                    if (workflow.getHeap().containsHeap(heap)) {
                        continue heapLoop;
                    }
                }

                // No workflow has been found : create a default with the heap ID as ID
                workflowMap.put(heap.getId(), new Workflow(createHead(Boolean.TRUE, null), createChains(Boolean.TRUE, null), heap));
            }

            final List<ContextInterceptor> interceptors = new ArrayList<ContextInterceptor>();

            for (final ContextSetting setting : taggedSettings.values()) {
                interceptors.addAll(setting.getInterceptorsList());
            }

            return new Context(this, workflowMap, interceptors);
        } catch (IOException ioe) {
            WuicException.throwWuicException(ioe);
            return null;
        } finally {
            if (requiresLock) {
                lock.unlock();
            }
        }
    }

    /**
     * <p>
     * Gets the {@link ObjectBuilderFactory} which builds {@link Engine}.
     * </p>
     *
     * @return the factory
     */
    ObjectBuilderFactory<Engine> getEngineBuilderFactory() {
        return engineBuilderFactory;
    }

    /**
     * <p>
     * Gets the {@link ObjectBuilderFactory} which builds {@link NutDao}.
     * </p>
     *
     * @return the factory
     */
    ObjectBuilderFactory<NutDao> getNutDaoBuilderFactory() {
        return nutDaoBuilderFactory;
    }

    /**
     * <p>
     * Gets the {@link ObjectBuilderFactory} which builds {@link NutFilter}.
     * </p>
     *
     * @return the factory
     */
    ObjectBuilderFactory<NutFilter> getNutFilterBuilderFactory() {
        return nutFilterBuilderFactory;
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
    @SuppressWarnings("unchecked")
    private Map<NutType, NodeEngine> createChains(final Boolean includeDefaultEngines, final String[] ebIdsExclusions) {
        final Map<NutType, NodeEngine> chains = new HashMap<NutType, NodeEngine>();

        // Include default engines
        if (includeDefaultEngines) {
            if (!configureDefault) {
                log.warn("This builder can't include default engines to chains if you've not call configureDefault before");
                return chains;
            }

            for (final ObjectBuilderFactory.KnownType knownType : engineBuilderFactory.knownTypes()) {
                if ((NodeEngine.class.isAssignableFrom(knownType.getClassType()))
                    && EngineService.class.cast(knownType.getClassType().getAnnotation(EngineService.class)).injectDefaultToWorkflow()
                    && ((ebIdsExclusions == null || CollectionUtils.indexOf(knownType.getTypeName(), ebIdsExclusions) != -1))) {
                    final String id = BUILDER_ID_PREFIX + knownType.getTypeName();
                    NodeEngine engine = NodeEngine.class.cast(newEngine(id));

                    // TODO: would be easier if nut types are provided by service annotation
                    for (final NutType nutType : engine.getNutTypes()) {
                        NodeEngine chain = chains.get(nutType);

                        if (chain == null) {
                            chains.put(nutType, engine);
                        } else {
                            chains.put(nutType, NodeEngine.chain(chain, engine));
                        }

                        engine = NodeEngine.class.cast(newEngine(id));
                    }
                }
            }
        }

        return chains;
    }

    /**
     * <p>
     * Creates the engine that will be the head of the chain of responsibility.
     * </p>
     *
     * <p>
     * If an {@link HeadEngine} is configured with {@link EngineService#isCoreEngine()} = false,
     * it will be returned in place of any {@link HeadEngine} configured with {@link EngineService#isCoreEngine()} = true.
     * because extensions override core in this case.
     * </p>
     *
     * @param includeDefaultEngines if include default engines or not
     * @param ebIdsExclusions the engines to exclude
     * @return the {@link HeadEngine}
     */
    @SuppressWarnings("unchecked")
    private HeadEngine createHead(final Boolean includeDefaultEngines, final String[] ebIdsExclusions) {
        if (includeDefaultEngines) {
            HeadEngine core = null;

            for (final ObjectBuilderFactory.KnownType knownType : engineBuilderFactory.knownTypes()) {
                final EngineService annotation = EngineService.class.cast(knownType.getClassType().getAnnotation(EngineService.class));
                if (HeadEngine.class.isAssignableFrom(knownType.getClassType())
                        && annotation.injectDefaultToWorkflow()
                        && ((ebIdsExclusions == null || CollectionUtils.indexOf(knownType.getTypeName(), ebIdsExclusions) != -1))) {
                    final String id = BUILDER_ID_PREFIX + knownType.getTypeName();
                    HeadEngine engine = HeadEngine.class.cast(newEngine(id));

                    if (annotation.isCoreEngine()) {
                        core = engine;
                    } else {
                        // Extension found, use it
                        return engine;
                    }
                }
            }

            // Use core if no extension set
            return core;
        } else {
            return null;
        }
    }

    /**
     * <p>
     * Gets the {@link WorkflowTemplate} associated to the given ID.
     * </p>
     *
     * @param id the ID
     * @param daoCollection the collection of declared {@link NutDao}
     * @return the matching {@link WorkflowTemplate template}
     */
    private WorkflowTemplate getWorkflowTemplate(final String id, final Map<String, NutDao> daoCollection) {
        final Iterator<ContextSetting> it = taggedSettings.values().iterator();
        WorkflowTemplateRegistration retval = null;

        while (it.hasNext() && retval == null) {
            retval = it.next().getTemplateMap().get(id);
        }

        return retval != null ? retval.getTemplate(daoCollection) : null;
    }

    /**
     * <p>
     * Gets the {@link HeapRegistration registration} associated to an ID matching the given regex.
     * </p>
     *
     * @param regex the regex ID
     * @return the matching {@link HeapRegistration registration}
     */
    private Map<String, HeapRegistration> getNutsHeap(final String regex) {
        final Map<String, HeapRegistration> retval = new HashMap<String, HeapRegistration>();
        final Pattern pattern = Pattern.compile(regex);

        for (final ContextSetting setting : taggedSettings.values()) {
            for (final Map.Entry<String, HeapRegistration> entry : setting.getNutsHeaps().entrySet()) {
                if (pattern.matcher(entry.getKey()).matches()) {
                    retval.put(entry.getKey(), entry.getValue());
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
        for (final ContextSetting setting : taggedSettings.values()) {
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
     * <p>
     * Throws an {@link IllegalArgumentException} if a specified property is not supported by the builder
     * </p>
     *
     * @param builder the builder
     * @param properties the properties to use to configure the builder
     * @param <O> the type produced by the builder
     * @param <T> the type of builder
     * @return the given builder
     */
    private <O, T extends ObjectBuilder<O>> T configure(final T builder,  final Map<String, Object> properties) {
        for (final Map.Entry entry : properties.entrySet()) {
            builder.property(String.valueOf(entry.getKey()), entry.getValue());
        }

        return builder;
    }
}