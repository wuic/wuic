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
import com.github.wuic.engine.EngineType;
import com.github.wuic.exception.WuicException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

import com.github.wuic.nut.ConvertibleNut;
import com.github.wuic.nut.dao.NutDao;
import com.github.wuic.util.NumberUtils;
import com.github.wuic.util.UrlProviderFactory;
import com.github.wuic.util.UrlUtils;
import com.github.wuic.util.WuicScheduledThreadPool;
import com.github.wuic.xml.FileXmlContextBuilderConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBException;

/**
 * <p>
 * This class is a facade which exposes the WUIC features by simplifying
 * them within some exposed methods.
 * </p>
 *
 * @author Guillaume DROUET
 * @version 1.9
 * @since 0.1.0
 */
public final class WuicFacade {

    /**
     * <p>
     * This enumeration allows to indicate how the {@link Context} should be initialized when
     * {@link ContextBuilder#build()} is called.
     * </p>
     *
     * @author Guillaume DROUET
     * @version 1.0
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
     * @version 1.0
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
            return runWorkflow(workflowId);
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
     * Context.
     */
    private Context context;

    /**
     * <p>
     * Builds a new {@link WuicFacade}
     * </p>
     *
     * @param b the builder that contains settings in its state
     * @throws WuicException if the 'wuic.xml' path is not well configured
     */
    WuicFacade(final WuicFacadeBuilder b) throws WuicException {
        config = b;

        try {
            if (b.getWuicXmlPath() != null) {
                b.getConfigurators().add(new FileXmlContextBuilderConfigurator(config.getWuicXmlPath()));
            }
        } catch (JAXBException je) {
            WuicException.throwWuicXmlReadException(je) ;
        }

        // No inspector, directly use the wrapped context builder
        if (b.getObjectBuilderInspector() == null) {
            builder = new ContextBuilder(
                    b.contextBuilder().getEngineBuilderFactory(),
                    b.contextBuilder().getNutDaoBuilderFactory(),
                    b.contextBuilder().getNutFilterBuilderFactory());
        } else {
            // build a new context builder with specific inspector and reuse the factories already declared
            builder = new ContextBuilder(
                    b.contextBuilder().getEngineBuilderFactory(),
                    b.contextBuilder().getNutDaoBuilderFactory(),
                    b.contextBuilder().getNutFilterBuilderFactory(),
                    b.getObjectBuilderInspector());
        }

        final ContextBuilderConfigurator[] array = new ContextBuilderConfigurator[config.getConfigurators().size()];
        configure(b.getUseDefaultContextBuilderConfigurator(), b.getConfigurators().toArray(array));

        // finally merge all settings
        builder.tag(getClass().getName()).mergeSettings(b.contextBuilder()).releaseTag();

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

            for (final ContextBuilderConfigurator contextBuilderConfigurator : configurators) {
                contextBuilderConfigurator.configure(builder);
            }
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
     * {@link #runWorkflow(String, com.github.wuic.util.UrlProviderFactory, com.github.wuic.engine.EngineType...)}.
     * </p>
     * 
     * @param id the workflow ID
     * @param path the requested path, {@code null} to retrieve all paths
     * @param urlProviderFactory the URL provider
     * @param skip the engine types
     * @return the processed nuts
     * @throws WuicException if the context can't be processed
     */
    public synchronized ConvertibleNut runWorkflow(final String id, final String path, final UrlProviderFactory urlProviderFactory, final EngineType ... skip)
            throws WuicException {
        try {
            final long start = beforeRunWorkflow(id);
            final ConvertibleNut retval = context.process(config.getContextPath(), id, path, urlProviderFactory, skip);
            log.info("Workflow retrieved in {} seconds", (float) (System.currentTimeMillis() - start) / (float) NumberUtils.ONE_THOUSAND);
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
     * {@link WuicFacade#runWorkflow(String, String, com.github.wuic.util.UrlProviderFactory, com.github.wuic.engine.EngineType...)}.
     * </p>
     *
     * @param id the workflow ID
     * @param path the requested path, {@code null} to retrieve all paths
     * @param skip the engine types
     * @return the processed nuts
     * @throws WuicException if the context can't be processed
     */
    public synchronized ConvertibleNut runWorkflow(final String id, final String path, final EngineType ... skip)
            throws WuicException {
        return runWorkflow(id, path, UrlUtils.urlProviderFactory(), skip);
    }


    /**
     * <p>
     * Gets the nuts processed by the given workflow identified by the specified ID.
     * </p>
     *
     * @param id the workflow ID
     * @param urlProviderFactory the URL provider
     * @param skip the engine types
     * @return the processed nuts
     * @throws WuicException if the context can't be processed
     */
    public synchronized List<ConvertibleNut> runWorkflow(final String id,
                                                         final UrlProviderFactory urlProviderFactory,
                                                         final EngineType ... skip)
            throws WuicException {
        final long start = beforeRunWorkflow(id);
        final List<ConvertibleNut> retval = new ArrayList<ConvertibleNut>(context.process(config.getContextPath(), id, urlProviderFactory, skip));
        log.info("Workflow retrieved in {} seconds", (float) (System.currentTimeMillis() - start) / (float) NumberUtils.ONE_THOUSAND);

        return retval;
    }

    /**
     * <p>
     * Gets the nuts processed by the given workflow identified by the specified ID.
     * </p>
     *
     * @param id the workflow ID
     * @param skip the engine types
     * @return the processed nuts
     * @throws WuicException if the context can't be processed
     */
    public synchronized List<ConvertibleNut> runWorkflow(final String id, final EngineType ... skip) throws WuicException {
        return runWorkflow(id, UrlUtils.urlProviderFactory(), skip);
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
     * @return the timestamp as start time
     * @throws WuicException if context can't be built
     */
    private long beforeRunWorkflow(final String id) throws WuicException {
        final long start = System.currentTimeMillis();

        log.info("Getting nuts for workflow : {}", id);
        refreshContext();

        return start;
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
     * Indicates to view templates if multiple configurations are allowed.
     * </p>
     *
     * @return {@code true} if allowed, {@code false} otherwise
     */
    public Boolean allowsMultipleConfigInTagSupport() {
        return config.getMultipleConfigInTagSupport();
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

        switch (config.getWarmUpStrategy()) {
            case NONE:
                log.info("Building the context without any warmup");
                break;
            case SYNC:
                log.info("Building the context with synchronous call for each workflow");

                for (final String wId : workflowIds()) {
                    runWorkflow(wId);
                }

                break;
            case ASYNC:
                log.info("Building the context with asynchronous call for each workflow");

                for (final String wId : workflowIds()) {
                    WuicScheduledThreadPool.getInstance().executeAsap(new ExecuteWorkflowJob(wId));
                }

                break;
        }
    }
}
