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

import com.github.wuic.config.ObjectBuilder;
import com.github.wuic.config.ObjectBuilderInspector;
import com.github.wuic.context.Context;
import com.github.wuic.context.ContextBuilder;
import com.github.wuic.context.ContextBuilderConfigurator;
import com.github.wuic.context.HeapResolutionEvent;
import com.github.wuic.context.WorkflowExecutionEvent;
import com.github.wuic.engine.EngineType;
import com.github.wuic.exception.WuicException;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.URL;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

import com.github.wuic.mbean.FacadeStats;
import com.github.wuic.nut.ConvertibleNut;
import com.github.wuic.nut.dao.NutDao;
import com.github.wuic.util.JmxPropertyResolver;
import com.github.wuic.util.NumberUtils;
import com.github.wuic.util.Timer;
import com.github.wuic.util.UrlProviderFactory;
import com.github.wuic.util.UrlUtils;
import com.github.wuic.util.WuicScheduledThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import javax.xml.bind.JAXBException;

/**
 * <p>
 * This class is a facade which exposes the WUIC features by simplifying them within some exposed methods.
 * </p>
 *
 * @author Guillaume DROUET
 * @since 0.1.0
 */
@ObjectBuilderInspector.InspectedType(ClassPathResourceResolverHandler.class)
public final class WuicFacade implements ObjectBuilderInspector, PropertyChangeListener {

    /**
     * <p>
     * This enumeration allows to indicate how the {@link com.github.wuic.context.Context} should be initialized when
     * {@link com.github.wuic.context.ContextBuilder#build()} is called.
     * </p>
     *
     * @author Guillaume DROUET
     * @since 0.5.0
     */
    public enum WarmupStrategy {

        /**
         * Just build the context and return it.
         */
        NONE,

        /**
         * Runs synchronously all workflows and return the context when execution is finished.
         */
        SYNC,

        /**
         * Runs asynchronously all workflows and return the context immediately.
         */
        ASYNC
    }

    /**
     * <p>
     * Executes the specified workflow ID.
     * </p>
     *
     * @author Guillaume DROUET
     * @since 0.5.0
     */
    private final class ExecuteWorkflowJob implements Callable<List<ConvertibleNut>> {

        /**
         * The workflow ID to process.
         */
        private String workflowId;

        /**
         * <p>
         * Builds a new instance.
         * </p>
         *
         * @param workflowId the workflow ID
         */
        private ExecuteWorkflowJob(final String workflowId) {
            this.workflowId = workflowId;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public List<ConvertibleNut> call() throws WuicException {
            return runWorkflow(workflowId, ProcessContext.DEFAULT);
        }
    }

    /**
     * Logger.
     */
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    /**
     * The settings.
     */
    private final WuicFacadeBuilder config;

    /**
     * The context builder.
     */
    private final ContextBuilder builder;

    /**
     * Facade statistics.
     */
    private FacadeStats facadeStats;

    /**
     * The nut type factory.
     */
    private NutTypeFactory nutTypeFactory;

    /**
     * Context.
     */
    private Context context;

    /**
     * <p>
     * Builds a new {@link WuicFacade}.
     * A {@link com.github.wuic.mbean.FacadeStatsMXBean} bean is published to JMX in order to provide statistics.
     * </p>
     *
     * @param b the builder that contains settings in its state
     * @throws WuicException if the configuration file path is not well configured
     */
    WuicFacade(final WuicFacadeBuilder b) throws WuicException {
        config = b;

        try {
            if (b.wuicConfigurationPaths() != null) {
                for (final URL path : b.wuicConfigurationPaths()) {
                    b.addConfigurator(path);
                }
            }
        } catch (JAXBException je) {
            WuicException.throwWuicXmlReadException(je) ;
        } catch (IOException ioe) {
            WuicException.throwWuicException(ioe);
        }

        final List<ObjectBuilderInspector> inspectors = b.getObjectBuilderInspectors();

        // This inspector will configure any ClassPathResourceResolverHandler
        inspectors.add(this);

        builder = new ContextBuilder(b.contextBuilder(), b.getPropertyResolver(),
                inspectors.toArray(new ObjectBuilderInspector[inspectors.size()]));
        builder.addHeapResolutionListener(this);
        final ContextBuilderConfigurator[] array = new ContextBuilderConfigurator[config.getConfigurators().size()];
        configure(b.getUseDefaultContextBuilderConfigurator(), b.getConfigurators().toArray(array));

        // finally merge all settings
        builder.tag(getClass().getName()).mergeSettings(b.contextBuilder()).releaseTag();

        configureJmx(b);
        buildContext();
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
        return builder.newNutDaoBuilder(type);
    }

    /**
     * <p>
     * Clears the given tag in the {@link ContextBuilder}.
     * </p>
     *
     * @param tag the tag to clear
     */
    public synchronized void clearTag(final String tag) {
        builder.clearTag(tag);
    }

    /**
     * <p>
     * Configures the internal builder with the given configurators.
     * </p>
     *
     * @param useDefault injects default configuration for each known engine and DAO type or not
     * @param configurators the configurators
     * @throws WuicException if an I/O error occurs or context can't be built
     */
    public synchronized void configure(final Boolean useDefault, final ContextBuilderConfigurator... configurators) throws WuicException {
        try {
            if (useDefault) {
                builder.configureDefault();
            }

            builder.configure(configurators);
        } catch (IOException ioe) {
            WuicException.throwWuicException(ioe);
        }

        refreshContext();
    }

    /**
     * <p>
     * Configures the internal builder with the given configurators. By default, injects default configuration for each
     * known engine and DAO type or not
     * </p>
     *
     * @param configurators the configurators
     * @throws WuicException if an I/O error occurs or context can't be built
     */
    public synchronized void configure(final ContextBuilderConfigurator... configurators) throws WuicException {
        configure(Boolean.TRUE, configurators);
    }
    
    /**
     * <p>
     * Gets the nut with the given path processed by the given workflow identified by the specified ID.
     * </p>
     *
     * <p>
     * The path should be used with the name of one nut returned when invoking
     * {@link #runWorkflow(String, UrlProviderFactory, ProcessContext, com.github.wuic.engine.EngineType...)}.
     * </p>
     * 
     * @param id the workflow ID
     * @param path the requested path, {@code null} to retrieve all paths
     * @param urlProviderFactory the URL provider
     * @param processContext the process context
     * @param skip the engine types
     * @return the processed nuts
     * @throws WuicException if the context can't be processed
     */
    public synchronized ConvertibleNut runWorkflow(final String id,
                                                   final String path,
                                                   final UrlProviderFactory urlProviderFactory,
                                                   final ProcessContext processContext,
                                                   final EngineType ... skip)
            throws WuicException {
        try {
            final Timer timer = beforeRunWorkflow(id);
            final ConvertibleNut retval = context.process(config.getContextPath(), id, path, urlProviderFactory, processContext, skip);
            Logging.TIMER.log("Workflow retrieved in {} seconds", (float) (timer.end()) / (float) NumberUtils.ONE_THOUSAND);
            return retval;
        } catch (IOException ioe) {
            WuicException.throwWuicException(ioe);
            return null;
        }
    }

    /**
     * <p>
     * Gets the nut with the given path processed by the given workflow identified by the specified ID.
     * </p>
     *
     * <p>
     * The path should be used with the name of nuts returned when invoking
     * {@link WuicFacade#runWorkflow(String, String, UrlProviderFactory, ProcessContext, EngineType...)}.
     * </p>
     *
     * @param id the workflow ID
     * @param path the requested path, {@code null} to retrieve all paths
     * @param processContext the process context
     * @param skip the engine types
     * @return the processed nuts
     * @throws WuicException if the context can't be processed
     */
    public synchronized ConvertibleNut runWorkflow(final String id,
                                                   final String path,
                                                   final ProcessContext processContext,
                                                   final EngineType ... skip)
            throws WuicException {
        return runWorkflow(id, path, UrlUtils.urlProviderFactory(), processContext, skip);
    }

    /**
     * <p>
     * Gets the nuts processed by the given workflow identified by the specified ID.
     * </p>
     *
     * @param id the workflow ID
     * @param urlProviderFactory the URL provider
     * @param processContext the process context
     * @param skip the engine types
     * @return the processed nuts
     * @throws WuicException if the context can't be processed
     */
    public synchronized List<ConvertibleNut> runWorkflow(final String id,
                                                         final UrlProviderFactory urlProviderFactory,
                                                         final ProcessContext processContext,
                                                         final EngineType ... skip)
            throws WuicException {
        final Timer timer = beforeRunWorkflow(id);
        final List<ConvertibleNut> retval = context.process(config.getContextPath(), id, urlProviderFactory, processContext, skip);
        Logging.TIMER.log("Workflow retrieved in {} seconds", (float) (timer.end()) / (float) NumberUtils.ONE_THOUSAND);

        return retval;
    }

    /**
     * <p>
     * Gets the nuts processed by the given workflow identified by the specified ID inside a specified context.
     * </p>
     *
     * @param id the workflow ID
     * @param skip the engine types
     * @param processContext the process context
     * @return the processed nuts
     * @throws WuicException if the context can't be processed
     */
    public synchronized List<ConvertibleNut> runWorkflow(final String id, final ProcessContext processContext, final EngineType ... skip)
            throws WuicException {
        return runWorkflow(id, UrlUtils.urlProviderFactory(), processContext, skip);
    }

    /**
     * <p>
     * Returns the workflow IDs.
     * </p>
     *
     * @return the IDs
     */
    public Set<String> workflowIds() {
        return context.workflowIds();
    }

    /**
     * <p>
     * Method called before running a workflow.
     * </p>
     *
     * @param id the workflow to be run
     * @return the timer started at the beginning of the call
     * @throws WuicException if context can't be built
     */
    private Timer beforeRunWorkflow(final String id) throws WuicException {
        final Timer retval = new Timer();
        retval.start();

        log.info("Getting nuts for workflow : {}", id);
        refreshContext();

        return retval;
    }

    /**
     * <p>
     * Refresh the context if necessary.
     * </p>
     *
     * @return {@code true} if the context is re-built,{@code false} otherwise
     * @throws WuicException context can't be built
     */
    public boolean refreshContext() throws WuicException {
        // Update context if necessary
        if (context != null && !context.isUpToDate()) {
            buildContext();
            facadeStats.addRefreshCount();
            return true;
        } else {
            return false;
        }
    }

    /**
     * <p>
     * Returns the context path.
     * </p>
     *
     * @return context path
     */
    public String getContextPath() {
        return config.getContextPath();
    }

    /**
     * <p>
     * Gets the nut type factory.
     * </p>
     *
     * @return the nut type factory
     * @throws WuicException if any refresh operation fails
     */
    public NutTypeFactory getNutTypeFactory() throws WuicException {
        refreshContext();
        return nutTypeFactory;
    }

    /**
     * <p>
     * Indicates to view templates if multiple configurations are allowed.
     * </p>
     *
     * @return {@code true} if allowed, {@code false} otherwise
     */
    public Boolean allowsMultipleConfigInTagSupport() {
        return config.getMultipleConfigInTagSupport();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void propertyChange(final PropertyChangeEvent evt) {
        if (evt.getNewValue() instanceof WorkflowExecutionEvent) {
            final WorkflowExecutionEvent e = WorkflowExecutionEvent.class.cast(evt.getNewValue());
            facadeStats.addWorkflowExecution(e.getExecution(), e.getId());
        } else if (evt.getNewValue() instanceof HeapResolutionEvent) {
            final HeapResolutionEvent e = HeapResolutionEvent.class.cast(evt.getNewValue());
            facadeStats.addHeapResolution(e.getResolution(), e.getId());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> T inspect(final T object) {
        ClassPathResourceResolverHandler.class.cast(object).setClasspathResourceResolver(config.getClasspathResourceResolver());

        return object;
    }

    /**
     * <p>
     * Configures JMX.
     * </p>
     *
     * @param b the facade builder
     * @throws WuicException if configuration fails
     */
    private void configureJmx(final WuicFacadeBuilder b) throws WuicException {
        try {
            // Build and expose statistic bean via JMX
            final String maxExecutions = b.getPropertyResolver().resolveProperty(ApplicationConfig.MAX_WORKFLOW_EXECUTION_STATS);
            final String maxResolutions = b.getPropertyResolver().resolveProperty(ApplicationConfig.MAX_HEAP_RESOLUTION_STATS);
            facadeStats = new FacadeStats(maxExecutions == null ? NumberUtils.ONE_THOUSAND : Integer.parseInt(maxExecutions),
                    maxResolutions == null ? NumberUtils.ONE_THOUSAND : Integer.parseInt(maxResolutions));
            final MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
            final ObjectName statName = new ObjectName("com.github.wuic.jmx:type=FacadeStats");

            if (mbs.isRegistered(statName)) {
                mbs.unregisterMBean(statName);
            }

            mbs.registerMBean(facadeStats, statName);

            // Exposes JMX bean to change global properties
            final ObjectName propertyName = new ObjectName("com.github.wuic.jmx:type=PropertyResolver");
            final WuicFacade capture = this;
            final JmxPropertyResolver jmxPropertyResolver = new JmxPropertyResolver() {
                @Override
                public void addProperty(final String key, final String value) {
                    super.addProperty(key, value);
                    log.info("Setting property {} with value {} via JMX. All cached configurations are going to be evicted.", key, value);

                    // Notify the context to refresh it before the next call
                    capture.builder.forceRefresh();
                }
            };

            if (mbs.isRegistered(propertyName)) {
                mbs.unregisterMBean(propertyName);
            }

            mbs.registerMBean(jmxPropertyResolver, propertyName);
            b.addPropertyResolver(jmxPropertyResolver);
        } catch (InstanceNotFoundException infe) {
            WuicException.throwWuicException(infe);
        } catch (InstanceAlreadyExistsException iaee) {
            WuicException.throwWuicException(iaee);
        } catch (MBeanRegistrationException mbre) {
            WuicException.throwWuicException(mbre);
        } catch (NotCompliantMBeanException ncmbe) {
            WuicException.throwWuicException(ncmbe);
        } catch (MalformedObjectNameException mone) {
            WuicException.throwWuicException(mone);
        }
    }

    /**
     * <p>
     * Builds the {@link Context} according the {@link WarmupStrategy} set.
     * </p>
     *
     * @throws WuicException if context can't be built
     */
    private void buildContext() throws WuicException {
        context = builder.build();
        context.addPropertyChangeListener(this);
        nutTypeFactory = builder.getNutTypeFactory();

        switch (config.getWarmUpStrategy()) {
            case NONE:
                log.info("Building the context without any warmup");
                break;
            case SYNC:
                log.info("Building the context with synchronous call for each workflow");

                for (final String wId : workflowIds()) {
                    runWorkflow(wId, ProcessContext.DEFAULT);
                }

                break;
            case ASYNC:
                log.info("Building the context with asynchronous call for each workflow");

                for (final String wId : workflowIds()) {
                    WuicScheduledThreadPool.INSTANCE.executeAsap(new ExecuteWorkflowJob(wId));
                }

                break;
        }
    }
}
