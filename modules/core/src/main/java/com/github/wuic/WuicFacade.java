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
import com.github.wuic.nut.NutDaoBuilderFactory;
import com.github.wuic.exception.WuicException;
import com.github.wuic.exception.xml.WuicXmlReadException;

import java.util.List;

import com.github.wuic.nut.Nut;
import com.github.wuic.util.NumberUtils;
import com.github.wuic.xml.WuicXmlContextBuilderConfigurator;
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
 * @version 1.6
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
     * Builds a new {@link WuicFacade}.
     * </p>
     *
     * @param cp the context path where the files will be exposed
     * @throws WuicException if the 'wuic.xml' path is not well configured
     */
    private WuicFacade(final String cp) throws WuicException {
        this("/wuic.xml", cp);
    }

    /**
     * <p>
     * Builds a new {@link WuicFacade} for a particular wuic.xml path .
     * </p>
     *
     * @param wuicXmlPath the wuic.xml location in the classpath
     * @param cp the context path where the files will be exposed
     * @throws WuicException if the 'wuic.xml' path is not well configured
     */
    private WuicFacade(final String wuicXmlPath, final String cp) throws WuicException {
        builder = new ContextBuilder();

        try {
            // TODO : create flag to not use default configuration
            new NutDaoBuilderFactory().newContextBuilderConfigurator().configure(builder);
            new EngineBuilderFactory().newContextBuilderConfigurator().configure(builder);
            new WuicXmlContextBuilderConfigurator(getClass().getResource(wuicXmlPath)).configure(builder);
            context = builder.build();
        } catch (JAXBException je) {
            throw new WuicXmlReadException("unable to load wuic.xml", je) ;
        }
        contextPath = cp;
    }

    /**
     * <p>
     * Gets a new instance. If an error occurs, it will be wrapped in a
     * {@link com.github.wuic.exception.WuicRuntimeException} which will be thrown.
     * </p>
     *
     * @param contextPath the context where the nuts will be exposed
     * @return the unique instance
     * @throws WuicException if the 'wuic.xml' path is not well configured
     */
    public static synchronized WuicFacade newInstance(final String contextPath) throws WuicException {
        return newInstance(contextPath, null);
    }

    /**
     * <p>
     * Gets a new instance. If an error occurs, it will be wrapped in a
     * {@link com.github.wuic.exception.WuicRuntimeException} which will be thrown.
     * </p>
     *
     * @param wuicXmlPath the specific wuic.xml path in classpath (could be {@code null}
     * @param contextPath the context where the nuts will be exposed
     * @return the unique instance
     *
     */
    public static synchronized WuicFacade newInstance(final String contextPath, final String wuicXmlPath) throws WuicException {
        if (wuicXmlPath != null) {
            return new WuicFacade(wuicXmlPath, contextPath);
        } else {
            return new WuicFacade(contextPath);
        }
    }
    
    /**
     * <p>
     * Gets the nuts processed by the workflow identified by the given ID.
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

        // Parse the nuts
        final List<Nut> retval = context.process(id, contextPath);

        log.info("Workflow retrieved in {} seconds", (float) (System.currentTimeMillis() - start) / (float) NumberUtils.ONE_THOUSAND);

        return retval;
    }
}
