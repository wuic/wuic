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

import com.github.wuic.engine.EngineBuilderFactory;
import com.github.wuic.exception.wrapper.StreamException;
import com.github.wuic.nut.NutDaoBuilderFactory;
import com.github.wuic.exception.WuicException;
import com.github.wuic.exception.xml.WuicXmlReadException;

import java.net.URL;
import java.util.*;
import java.util.regex.Pattern;

import com.github.wuic.nut.Nut;
import com.github.wuic.nut.NutsHeap;
import com.github.wuic.nut.core.CompositeNut;
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
 * @version 1.8
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
    private ContextBuilder builder;

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
     * @param contextBuilderConfigurators configurators to be used on the builder
     * @throws WuicException if the 'wuic.xml' path is not well configured
     */
    private WuicFacade(final String cp,
                       final ContextBuilderConfigurator ... contextBuilderConfigurators)
            throws WuicException {
        builder = new ContextBuilder();
        configure(contextBuilderConfigurators);
        context = builder.build();
        contextPath = cp;
    }

    /**
     * <p>
     * Gets a new instance. If an error occurs, it will be wrapped in a
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
        return newInstance(contextPath, WuicFacade.class.getResource("/wuic.xml"), useDefaultConfigurator);
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
     * @return the unique instance
     *
     */
    public static synchronized WuicFacade newInstance(final String contextPath,
                                                      final URL wuicXmlPath,
                                                      final Boolean useDefaultConfigurator) throws WuicException {
        try {
            if (wuicXmlPath != null) {
                if (useDefaultConfigurator) {
                    return new WuicFacade(contextPath,
                            new NutDaoBuilderFactory().newContextBuilderConfigurator(),
                            new EngineBuilderFactory().newContextBuilderConfigurator(),
                            new FileXmlContextBuilderConfigurator(wuicXmlPath));
                } else {
                    return new WuicFacade(contextPath,
                            new FileXmlContextBuilderConfigurator(wuicXmlPath));
                }
            } else  if (useDefaultConfigurator) {
                return new WuicFacade(contextPath,
                        new NutDaoBuilderFactory().newContextBuilderConfigurator(),
                        new EngineBuilderFactory().newContextBuilderConfigurator());
            } else {
                return new WuicFacade(contextPath);
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
     * @param configurators the configurators
     * @throws StreamException if an I/O error occurs
     */
    public synchronized void configure(final ContextBuilderConfigurator... configurators) throws StreamException {
        for (final ContextBuilderConfigurator contextBuilderConfigurator : configurators) {
            contextBuilderConfigurator.configure(builder);
        }
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
        final long start = System.currentTimeMillis();

        log.info("Getting nuts for workflow : {}", id);

        // Update context if necessary
        if (!context.isUpToDate()) {
            context = builder.build();
        }

        // TODO : move this code to context#process(String, String) method
        final String[] composition = id.split(Pattern.quote(NutsHeap.ID_SEPARATOR));
        final List<Nut> retval;

        if (composition.length > 1) {
            final List<Nut> nuts = new ArrayList<Nut>();

            for (final String wId : composition) {
                nuts.addAll(context.process(wId, contextPath));
            }

            retval = CompositeNut.mergeNuts(nuts);
        } else {
            // Parse the nuts
            retval = context.process(id, contextPath);
        }

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
     * Returns the context path.
     * </p>
     *
     * @return context path
     */
    public String getContextPath() {
        return contextPath;
    }
}
