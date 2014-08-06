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
import com.github.wuic.config.ObjectBuilderInspector;
import com.github.wuic.exception.wrapper.StreamException;
import com.github.wuic.exception.WuicException;
import com.github.wuic.exception.xml.WuicXmlReadException;

import java.net.URL;
import java.util.*;

import com.github.wuic.nut.Nut;
import com.github.wuic.nut.dao.NutDao;
import com.github.wuic.util.NumberUtils;
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
     * Logger.
     */
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    /**
     * The context builder.
     */
    private final ContextBuilder builder;

    /**
     * Context.
     */
    private Context context;

    /**
     * The context path where the files will be exposed.
     */
    private String contextPath;

    /**
     * <p>
     * Builds a new {@link WuicFacade} for a particular wuic.xml path .
     * </p>
     *
     * @param cp the context path where the files will be exposed
     * @param useDefault injects default configuration for each known engine and DAO type
     * @param contextBuilderConfigurators configurators to be used on the builder
     * @param inspector an additional inspector
     * @throws WuicException if the 'wuic.xml' path is not well configured
     */
    private WuicFacade(final String cp,
                       final Boolean useDefault,
                       final ObjectBuilderInspector inspector,
                       final ContextBuilderConfigurator ... contextBuilderConfigurators)
            throws WuicException {
        builder = inspector == null ? new ContextBuilder() : new ContextBuilder(inspector);
        configure(useDefault, contextBuilderConfigurators);
        context = builder.build();
        contextPath = cp;
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
     * Gets a new instance. If an error occurs, it will be wrapped in a
     * {@link com.github.wuic.exception.WuicRuntimeException} which will be thrown.
     * </p>
     *
     * @param contextPath the context where the nuts will be exposed
     * @param useDefaultConfigurator use or not default configurators that injects default DAO and engines
     * @param inspector additional inspector
     * @return the unique instance
     * @throws WuicException if the 'wuic.xml' path is not well configured
     */
    public static synchronized WuicFacade newInstance(final String contextPath, final Boolean useDefaultConfigurator, final ObjectBuilderInspector inspector)
            throws WuicException {
        return newInstance(contextPath, WuicFacade.class.getResource("/wuic.xml"), useDefaultConfigurator, inspector);
    }

    /**
     * <p>
     * Gets a new instance witout any additional inspector. If an error occurs, it will be wrapped in a
     * {@link com.github.wuic.exception.WuicRuntimeException} which will be thrown.
     * </p>
     *
     * @param contextPath the context where the nuts will be exposed
     * @param useDefaultConfigurator use or not default configurators that injects default DAO and engines
     * @return the unique instance
     * @throws WuicException if the 'wuic.xml' path is not well configured
     */
    public static synchronized WuicFacade newInstance(final String contextPath, final Boolean useDefaultConfigurator)
            throws WuicException {
        return newInstance(contextPath, WuicFacade.class.getResource("/wuic.xml"), useDefaultConfigurator, null);
    }

    /**
     * <p>
     * Gets a new instance without any additional inspector. If an error occurs, it will be wrapped in a
     * {@link com.github.wuic.exception.WuicRuntimeException} which will be thrown.
     * </p>
     *
     * @param contextPath the context where the nuts will be exposed
     * @param useDefaultConfigurator use or not default configurators that injects default DAO and engines
     * @param wuicXmlPath the specific wuic.xml path URL (could be {@code null}
     * @return the unique instance
     * @throws WuicException if the 'wuic.xml' path is not well configured
     */
    public static synchronized WuicFacade newInstance(final String contextPath, final URL wuicXmlPath, final Boolean useDefaultConfigurator)
            throws WuicException {
        return newInstance(contextPath, wuicXmlPath, useDefaultConfigurator, null);
    }

    /**
     * <p>
     * Gets a new instance. If an error occurs, it will be wrapped in a
     * {@link com.github.wuic.exception.WuicRuntimeException} which will be thrown.
     * </p>
     *
     * @param wuicXmlPath the specific wuic.xml path URL (could be {@code null}
     * @param useDefaultConfigurator use or not default configurators that injects default DAO and engines
     * @param contextPath the context where the nuts will be exposed
     * @param inspector additional inspector
     * @return the unique instance
     */
    public static synchronized WuicFacade newInstance(final String contextPath,
                                                      final URL wuicXmlPath,
                                                      final Boolean useDefaultConfigurator,
                                                      final ObjectBuilderInspector inspector)
            throws WuicException {
        try {
            if (wuicXmlPath != null) {
                return new WuicFacade(contextPath, useDefaultConfigurator, inspector, new FileXmlContextBuilderConfigurator(wuicXmlPath));
            } else {
                return new WuicFacade(contextPath, useDefaultConfigurator, inspector);
            }
        } catch (JAXBException je) {
            throw new WuicXmlReadException("unable to load wuic.xml", je) ;
        }
    }

    /**
     * <p>
     * Configures the internal builder with the given configurators.
     * </p>
     *
     * @param useDefault injects default configuration for each known engine and DAO type or not
     * @param configurators the configurators
     * @throws StreamException if an I/O error occurs
     */
    public synchronized void configure(final Boolean useDefault, final ContextBuilderConfigurator... configurators) throws StreamException {
        if (useDefault) {
            builder.configureDefault();
        }

        for (final ContextBuilderConfigurator contextBuilderConfigurator : configurators) {
            contextBuilderConfigurator.configure(builder);
        }
    }

    /**
     * <p>
     * Configures the internal builder with the given configurators. By default, injects default configuration for each
     * known engine and DAO type or not
     * </p>
     *
     * @param configurators the configurators
     * @throws StreamException if an I/O error occurs
     */
    public synchronized void configure(final ContextBuilderConfigurator... configurators) throws StreamException {
        configure(Boolean.TRUE, configurators);
    }
    
    /**
     * <p>
     * Gets the nut with the given path processed by the given workflow identified by the specified ID.
     * </p>
     *
     * <p>
     * The path should be used with the name of nuts returned when invoking {@link WuicFacade#runWorkflow(String)}.
     * </p>
     * 
     * @param id the workflow ID
     * @param path the requested path, {@code null} to retrieve all paths
     * @return the processed nuts
     * @throws WuicException if the context can't be processed
     */
    public synchronized Nut runWorkflow(final String id, final String path) throws WuicException {
        final long start = beforeRunWorkflow(id);
        final Nut retval = context.process(contextPath, id, path);
        log.info("Workflow retrieved in {} seconds", (float) (System.currentTimeMillis() - start) / (float) NumberUtils.ONE_THOUSAND);

        return retval;
    }

    /**
     * <p>
     * Gets the nuts processed by the given workflow identified by the specified ID.
     * </p>
     *
     * @param id the workflow ID
     * @return the processed nuts
     * @throws WuicException if the context can't be processed
     */
    public synchronized List<Nut> runWorkflow(final String id) throws WuicException {
        final long start = beforeRunWorkflow(id);
        final List<Nut> retval = new ArrayList<Nut>(context.process(contextPath, id));
        log.info("Workflow retrieved in {} seconds", (float) (System.currentTimeMillis() - start) / (float) NumberUtils.ONE_THOUSAND);

        return retval;
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
     */
    private long beforeRunWorkflow(final String id) {
        final long start = System.currentTimeMillis();

        log.info("Getting nuts for workflow : {}", id);

        // Update context if necessary
        if (!context.isUpToDate()) {
            context = builder.build();
        }

        return start;
    }

    /**
     * <p>
     * Returns the context path.
     * </p>
     *
     * @return context path
     */
    public String getContextPath() {
        return contextPath;
    }
}
