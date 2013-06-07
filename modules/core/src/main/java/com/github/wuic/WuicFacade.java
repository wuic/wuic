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

import com.github.wuic.configuration.BadConfigurationException;
import com.github.wuic.engine.Engine;
import com.github.wuic.engine.EngineRequest;
import com.github.wuic.factory.EngineFactoryBuilder;
import com.github.wuic.factory.impl.EngineFactoryBuilderImpl;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

import com.github.wuic.resource.WuicResource;
import com.github.wuic.util.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * This class is a facade which exposes the WUIC features by simplifying
 * them within some exposed methods.
 * </p>
 * 
 * @author Guillaume DROUET
 * @version 1.5
 * @since 0.1.0
 */
public final class WuicFacade {

    /**
     * Logger.
     */
    private final Logger log = LoggerFactory.getLogger(this.getClass());
    
    /**
     * The {@link EngineFactoryBuilder} used in this facade.
     */
    private EngineFactoryBuilder factoryBuilder;

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
     * @throws IOException if an I/O error occurs
     * @throws BadConfigurationException if the 'wuic.xml' file is not well configured
     */
    private WuicFacade(final String cp) throws IOException, BadConfigurationException {
        factoryBuilder = new EngineFactoryBuilderImpl();
        contextPath = cp;
    }

    /**
     * <p>
     * Builds a new {@link WuicFacade} for a particular wuic.xml file .
     * </p>
     *
     * @param wuicXmlPath the wuic.xml location in the classpath
     * @param cp the context path where the files will be exposed
     * @throws IOException if an I/O error occurs
     * @throws BadConfigurationException if the 'wuic.xml' file is not well configured
     */
    private WuicFacade(final String wuicXmlPath, final String cp) throws IOException, BadConfigurationException {
        factoryBuilder = new EngineFactoryBuilderImpl(wuicXmlPath);
        contextPath = cp;
    }

    /**
     * <p>
     * Gets a new instance. If an error occurs, it will be wrapped in a
     * {@link WuicException} which will be thrown.
     * </p>
     *
     * @param contextPath the context where the resources will be exposed
     * @return the unique instance
     */
    public static synchronized WuicFacade newInstance(final String contextPath) {
        return newInstance(contextPath, null);
    }

    /**
     * <p>
     * Gets a new instance. If an error occurs, it will be wrapped in a
     * {@link WuicException} which will be thrown.
     * </p>
     *
     * @param wuicXmlPath the specific wuic.xml path in classpath (could be {@code null}
     * @param contextPath the context where the resources will be exposed
     * @return the unique instance
     */
    public static synchronized WuicFacade newInstance(final String contextPath, final String wuicXmlPath) {
        try {
            if (wuicXmlPath != null) {
                return new WuicFacade(wuicXmlPath, contextPath);
            } else {
                return new WuicFacade(contextPath);
            }
        } catch (BadConfigurationException bce) {
            throw new WuicException(bce);
        } catch (IOException ioe) {
            throw new WuicException(ioe);
        }
    }

    /**
     * <p>
     * Gets all the registered group identifiers. 
     * </p>
     * 
     * @return the collection of IDs
     */
    public synchronized Collection<String> allGroups() {
        return factoryBuilder.getLoader().filesGroupIdList();
    }
    
    /**
     * <p>
     * Gets the files representing group identified by the given ID. If any error
     * occurs, then they will be wrapped in a {@link WuicException} which will be
     * thrown.
     * </p>
     * 
     * @param id the group ID
     * @param requestPath the path from the requester location
     * @return the files
     */
    public synchronized List<WuicResource> getGroup(final String id, final String requestPath) {
        try {
            final long start = System.currentTimeMillis();

            log.info("Getting files for group : {}", id);

            // Get the group
            final FilesGroup group = factoryBuilder.getLoader().getFilesGroup(id);

            // Build the engine that generates the files
            final FileType fileType = group.getConfiguration().getFileType();
            final Engine engine = factoryBuilder.build().create(fileType);
         
            // Parse the files
            final List<WuicResource> retval = engine.parse(new EngineRequest(contextPath, requestPath, group));

            log.info("Group retrieved in {} seconds", (float) (System.currentTimeMillis() - start) / (float) NumberUtils.ONE_THOUSAND);

            return retval;
        } catch (BadConfigurationException bce) {
            throw new WuicException(bce);
        } catch (IOException ioe) {
            throw new WuicException(ioe);
        }
    }
}
