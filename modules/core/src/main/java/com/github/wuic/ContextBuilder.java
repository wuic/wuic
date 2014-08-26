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

import com.github.wuic.config.ObjectBuilder;
import com.github.wuic.config.ObjectBuilderFactory;
import com.github.wuic.config.ObjectBuilderInspector;
import com.github.wuic.engine.Engine;
import com.github.wuic.engine.EngineService;
import com.github.wuic.engine.HeadEngine;
import com.github.wuic.engine.NodeEngine;
import com.github.wuic.exception.BuilderPropertyNotSupportedException;
import com.github.wuic.exception.UnableToInstantiateException;
import com.github.wuic.exception.WorkflowTemplateNotFoundException;
import com.github.wuic.exception.wrapper.BadArgumentException;
import com.github.wuic.exception.wrapper.StreamException;
import com.github.wuic.nut.dao.NutDao;
import com.github.wuic.nut.dao.NutDaoService;
import com.github.wuic.nut.NutsHeap;
import com.github.wuic.nut.filter.NutFilter;
import com.github.wuic.nut.filter.NutFilterHolder;
import com.github.wuic.nut.filter.NutFilterService;
import com.github.wuic.util.CollectionUtils;
import com.github.wuic.util.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
 * all settings defined at a moment. Example :
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
 *
 * <p>
 * If any operation is performed without any tag, then an exception will be thrown. Moreover, when the
 * {@link ContextBuilder#tag(String)} method is called, the current threads holds a lock on the object.
 * It will be released when the {@link com.github.wuic.ContextBuilder#releaseTag()} will be called.
 * Consequenlty, it is really important to always call this last method in a finally block.
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
    public static final String ID_PREFIX = "wuicDefault";

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
        this.taggedSettings = new HashMap<String, ContextSetting>();
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
            try {
                for (final ObjectBuilderFactory.KnownType type : engineBuilderFactory.knownTypes()) {
                    ctxBuilder.contextEngineBuilder(ID_PREFIX + type.getTypeName(), type.getTypeName()).toContext();
                }
                // Should never occur
            } catch (BuilderPropertyNotSupportedException bpnse) {
                throw new IllegalStateException(bpnse);
            } catch (UnableToInstantiateException itie) {
                throw new IllegalStateException(itie);
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
        protected Long getLastUpdateTimestampFor(final String path) throws StreamException {
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
            try {
                for (final ObjectBuilderFactory.KnownType type : nutDaoBuilderFactory.knownTypes()) {
                    ctxBuilder.contextNutDaoBuilder(ID_PREFIX + type.getTypeName(), type.getTypeName()).toContext();
                }
                // Should never occur
            } catch (BuilderPropertyNotSupportedException bpnse) {
                throw new IllegalStateException(bpnse);
            } catch (UnableToInstantiateException itie) {
                throw new IllegalStateException(itie);
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
        protected Long getLastUpdateTimestampFor(final String path) throws StreamException {
            // Never poll
            return 1L;
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
         * All {@link NutDao daos} associated to their builder ID.
         */
        private Map<String, NutDao> nutDaoMap = new HashMap<String, NutDao>();

        /**
         * All {@link NutFilter daos} associated to their builder ID.
         */
        private Map<String, NutFilter> nutFilterMap = new HashMap<String, NutFilter>();

        /**
         * All {@link com.github.wuic.config.ObjectBuilder} building {@link Engine} associated to their ID.
         */
        private Map<String, ObjectBuilder<Engine>> engineMap = new HashMap<String, ObjectBuilder<Engine>>();

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
        public Map<String, NutDao> getNutDaoMap() {
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
     * Configures for each type provided by the engine builder factory and nut dao builder factory a
     * default instance identified with an id starting by {@link #ID_PREFIX} and followed by the type
     * name itself.
     * </p>
     *
     * @return the current builder
     * @throws StreamException if configuration fails
     */
    public ContextBuilder configureDefault() throws StreamException {
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
     * Builds a new {@link Engine} builder.
     * </p>
     *
     * @param type the type of engine
     * @return the builder
     */
    public synchronized ObjectBuilder<Engine> newEngineBuilder(final String type) {
        return engineBuilderFactory.create(type);
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
        protected String id;

        /**
         * The properties.
         */
        protected Map<String, Object> properties;

        /**
         * <p>
         * Builds a new component identified by the given ID.
         * </p>
         *
         * @param id the ID
         * @throws UnableToInstantiateException if underlying class could not be instantiated
         */
        public ContextGenericBuilder(final String id) throws UnableToInstantiateException {
            this.id = id;
            this.properties = new HashMap<String, Object>();
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
         * @return the enclosing builder
         * @throws BuilderPropertyNotSupportedException if a previously configured property is not supported
         */
        public abstract ContextBuilder toContext() throws BuilderPropertyNotSupportedException;
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
         * @throws UnableToInstantiateException if underlying class could not be instantiated
         */
        public ContextEngineBuilder(final String id, final String type) throws UnableToInstantiateException {
            super(id);
            this.engineBuilder = engineBuilderFactory.create(type);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public ContextEngineBuilder property(final String key, final Object value) {
            properties.put(key, value);
            return this;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public ContextBuilder toContext() throws BuilderPropertyNotSupportedException {
            engineBuilder(id, engineBuilder, properties);
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
         * The builder.
         */
        private ObjectBuilder<NutDao> nutDaoBuilder;

        /**
         * <p>
         * Builds a new instance.
         * </p>
         *
         * @param id the builder ID
         * @param type the builder type
         * @throws UnableToInstantiateException if underlying class could not be instantiated
         */
        public ContextNutDaoBuilder(final String id, final String type) throws UnableToInstantiateException {
            super(id);
            this.nutDaoBuilder = nutDaoBuilderFactory.create(type);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public ContextNutDaoBuilder property(final String key, final Object value) {
            properties.put(key, value);
            return this;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public ContextBuilder toContext() throws BuilderPropertyNotSupportedException {
            nutDaoBuilder(id, nutDaoBuilder, properties);
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
         * @throws UnableToInstantiateException if underlying class could not be instantiated
         */
        public ContextNutFilterBuilder(final String id, final String type) throws UnableToInstantiateException {
            super(id);
            this.nutFilterBuilder = nutFilterBuilderFactory.create(type);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public ContextNutFilterBuilder property(final String key, final Object value) {
            properties.put(key, value);
            return this;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public ContextBuilder toContext() throws BuilderPropertyNotSupportedException {
            nutFilterBuilder(id, nutFilterBuilder, properties);
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
     * @throws UnableToInstantiateException if underlying class could not be instantiated
     */
    public ContextNutDaoBuilder contextNutDaoBuilder(final String id, final String type) throws UnableToInstantiateException {
        return new ContextNutDaoBuilder(id, type);
    }

    /**
     * <p>
     * Returns a new context filter builder.
     * </p>
     *
     * @param id the final builder's ID
     * @param type the final builder's type
     * @return the specific context builder
     * @throws UnableToInstantiateException if underlying class could not be instantiated
     */
    public ContextNutFilterBuilder contextNutFilterBuilder(final String id, final String type) throws UnableToInstantiateException {
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
     * @throws UnableToInstantiateException if underlying class could not be instantiated
     */
    public ContextEngineBuilder contextEngineBuilder(final String id, final String type) throws UnableToInstantiateException {
        return new ContextEngineBuilder(id, type);
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
     * Add a new {@link com.github.wuic.nut.dao.NutDao} identified by the specified ID.
     * </p>
     *
     * @param id the ID which identifies the builder in the context
     * @param dao the dao associated to its ID
     * @return this {@link ContextBuilder}
     */
    public ContextBuilder nutDao(final String id, final NutDao dao) {
        final ContextSetting setting = getSetting();

        // Will override existing element
        for (ContextSetting s : taggedSettings.values()) {
            s.nutDaoMap.remove(id);
        }

        setting.nutDaoMap.put(id, dao);
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
     * @param id the ID which identifies the builder in the context
     * @param daoBuilder the builder associated to its ID
     * @param properties the properties to use to configure the builder
     * @return this {@link ContextBuilder}
     * @throws com.github.wuic.exception.BuilderPropertyNotSupportedException if a property is not supported by the builder
     */
    private ContextBuilder nutDaoBuilder(final String id,
                                         final ObjectBuilder<NutDao> daoBuilder,
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
     * @throws com.github.wuic.exception.BuilderPropertyNotSupportedException if a property is not supported by the builder
     */
    private ContextBuilder nutFilterBuilder(final String id,
                                            final ObjectBuilder<NutFilter> filterBuilder,
                                            final Map<String, Object> properties)
            throws BuilderPropertyNotSupportedException {
        final ContextSetting setting = getSetting();

        // Will override existing element
        for (ContextSetting s : taggedSettings.values()) {
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
     * Creates a new heap as specified by {@link ContextBuilder#heap(String, String, String[], String...)} without any
     * composition.
     * </p>
     *
     * @param id the heap ID
     * @param ndbId the {@link com.github.wuic.nut.dao.NutDao} builder the heap is based on
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
     * If the {@link com.github.wuic.config.ObjectBuilder} ID is not known, a {@link com.github.wuic.exception.wrapper.BadArgumentException}
     * will be thrown.
     * </p>
     *
     * @param id the heap ID
     * @param heapIds the heaps composition
     * @param ndbId the {@link com.github.wuic.nut.dao.NutDao} builder the heap is based on
     * @param path the path
     * @return this {@link ContextBuilder}
     * @throws StreamException if the HEAP could not be created
     */
    public ContextBuilder heap(final String id, final String ndbId, final String[] heapIds, final String ... path) throws StreamException {
        NutDao dao = null;

        if (NumberUtils.isNumber(id)) {
            throw new BadArgumentException(new IllegalArgumentException(String.format("Heap ID %s cannot be a numeric value", id)));
        }

        // Will override existing element
        for (final ContextSetting s : taggedSettings.values()) {
            s.getNutsHeaps().remove(id);

            // Find DAO
            if (ndbId != null && dao == null) {
                dao = s.getNutDaoMap().get(ndbId);
            }
        }

        // Check content and apply filters
        final List<String> pathList = pathList(dao, ndbId, path);

        final ContextSetting setting = getSetting();

        // Composition detected, collected nested and referenced heaps
        if (heapIds != null && heapIds.length != 0) {
            final List<NutsHeap> composition = new ArrayList<NutsHeap>();

            for (final String regex : heapIds) {
                composition.addAll(getNutsHeap(regex));
            }

            setting.getNutsHeaps().put(id, new NutsHeap(pathList, dao, id, composition.toArray(new NutsHeap[composition.size()])));
        } else {
            setting.getNutsHeaps().put(id, new NutsHeap(pathList, dao, id));
        }

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
     * Also throws a {@link BadArgumentException} if their is at least one path while the specified DAO is {@code null}.
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
                throw new BadArgumentException(new IllegalArgumentException(msg));
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
     * @throws BuilderPropertyNotSupportedException if a property is not supported
     */
    private ContextBuilder engineBuilder(final String id,
                                         final ObjectBuilder<Engine> engineBuilder,
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
     * @param ebIds the set of {@link com.github.wuic.engine.Engine} builder to use
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
                throw new IllegalStateException(String.format("'%s' not associated to any %s", ndbId, NutDaoService.class.getName()));
            }

            if (!dao.saveSupported()) {
                throw new IllegalStateException(String.format("DAO built by '%s' does not supports save", ndbId));
            }

            nutDaos[i] = dao;
        }

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

        setting.getTemplateMap().put(id, new WorkflowTemplate(head, chains, nutDaos));

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

        final Map<NutType, ? extends NodeEngine> chains = template.getChains();
        final NutDao[] nutDaos = template.getStores();

        // Retrieve HEAP
        final List<NutsHeap> heaps = getNutsHeap(heapIdPattern);

        if (heaps.isEmpty()) {
            throw new IllegalStateException(String.format("'%s' is a regex which doesn't match any %s", heapIdPattern, NutsHeap.class.getName()));
        }

        if (forEachHeap) {
            for (final NutsHeap heap : heaps) {
                final String id = identifier + heap.getId();

                if (NumberUtils.isNumber(id)) {
                    throw new BadArgumentException(new IllegalArgumentException(
                            String.format("Workflow ID %s cannot be a numeric value", id)));
                }

                // Will override existing element
                for (final ContextSetting s : taggedSettings.values()) {
                    s.getWorkflowMap().remove(id);
                }

                setting.getWorkflowMap().put(id, new Workflow(template.getHead(), chains, heap, nutDaos));
            }
        } else {
            if (NumberUtils.isNumber(identifier)) {
                throw new BadArgumentException(new IllegalArgumentException(
                        String.format("Workflow ID %s cannot be a numeric value", identifier)));
            }

            final NutsHeap[] array = heaps.toArray(new NutsHeap[heaps.size()]);
            setting.getWorkflowMap().put(identifier, new Workflow(template.getHead(), chains, new NutsHeap(null, null, heapIdPattern, array)));
        }

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
     * Builds the context. Should throws an {@link IllegalStateException} if the context is not correctly configured.
     * For instance : associate a heap to an undeclared {@link com.github.wuic.nut.dao.NutDao} builder ID.
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
                    final String id = ID_PREFIX + knownType.getTypeName();
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
                    final String id = ID_PREFIX + knownType.getTypeName();
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
    private <O, T extends ObjectBuilder<O>> T configure(final T builder,  final Map<String, Object> properties)
            throws BuilderPropertyNotSupportedException {
        for (final Map.Entry entry : properties.entrySet()) {
            builder.property(String.valueOf(entry.getKey()), entry.getValue());
        }

        return builder;
    }
}