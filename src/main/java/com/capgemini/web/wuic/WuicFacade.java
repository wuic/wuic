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
 * •   The above copyright notice and this permission notice shall be included in
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


package com.capgemini.web.wuic;

import com.capgemini.web.wuic.configuration.BadConfigurationException;
import com.capgemini.web.wuic.configuration.Configuration;
import com.capgemini.web.wuic.configuration.SourceRootProvider;
import com.capgemini.web.wuic.engine.Engine;
import com.capgemini.web.wuic.engine.EngineOutputManager;
import com.capgemini.web.wuic.factory.EngineFactoryBuilder;
import com.capgemini.web.wuic.factory.impl.EngineFactoryBuilderImpl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * This class is a facade which exposes the WUIC functionalities by simplifying
 * them within some exposed methods.
 * </p>
 * 
 * @author Guillaume DROUET
 * @version 1.2
 * @since 0.1.0
 */
public final class WuicFacade {

    /**
     * The unique instance.
     */
    private static WuicFacade instance = null;
    
    /**
     * Logger.
     */
    private final Logger log = LoggerFactory.getLogger(this.getClass());
    
    /**
     * The {@link EngineFactoryBuilder} used in this facade.
     */
    private EngineFactoryBuilder factoryBuilder;
    
    /**
     * This {@code Map} contains the date stored when the file was treated for
     * the last time. 
     */
    private Map<String, Long> lastOperations;
    
    /**
     * <p>
     * Builds a new {@link WuicFacade}.
     * </p>
     * 
     * @throws IOException if an I/O error occurs
     * @throws BadConfigurationException if the 'wuic.xml' file is not well configured
     */
    private WuicFacade() throws IOException, BadConfigurationException {
        factoryBuilder = new EngineFactoryBuilderImpl();
        lastOperations = new HashMap<String, Long>();
    }
    
    /**
     * <p>
     * Gets the unique instance. If an error occurs, it will be wrapped in a
     * {@link WuicException} which will be thrown.
     * </p>
     * 
     * @return the unique instance
     */
    public static synchronized WuicFacade getInstance() {
        if (instance == null) {
            try {
                instance = new WuicFacade();
            } catch (BadConfigurationException bce) {
                throw new WuicException(bce);
            } catch (IOException ioe) {
                throw new WuicException(ioe);
            }
        }
        
        return instance;
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
     * @return the files
     */
    public synchronized List<WuicResource> getGroup(final String id) {
        try {
            if (log.isInfoEnabled()) {
                log.info("Getting files for group : " + id);
            }
            
            // Get the group
            final FilesGroup group = factoryBuilder.getLoader().getFilesGroup(id);
            
            // Get it configuration
            final Configuration configuration = group.getConfiguration();
            
            // Get the files group
            final List<String> filesList = group.getFiles();

            // Gets an input stream for each file
            final List<WuicResource> resources = new ArrayList<WuicResource>();
            final SourceRootProvider srcRootProvider = group.getSourceRootProvider();

            for (String file : filesList) {
                final Long ts = getLastOperation(id, file);
                final WuicResource resource = srcRootProvider.getStreamResource(id, file);
                
                /*
                 * If the file has not changed since the last work, then it
                 * is not necessary to read it once again. The file generated
                 * the last time could be reused. Setting a null InputStream
                 * tells the engine to reuse its last work.
                 */
                if (ts == null || (ts != null && srcRootProvider.hasChanged(id, file, ts))) {
                    resources.add(resource);
                    putLastOperation(id, file);
                } else {
                    // TODO : possibility to imagine a mechanism that reuse files that have not changed...
                    resources.add(resource);
                }
            }

            // Initialize the working directory for the engines to be executed
            final EngineOutputManager outManager = EngineOutputManager.getInstance();
            outManager.initWorkingDirectory(configuration, null);
            
            // Build the engine that generates the files
            final FileType fileType = group.getConfiguration().getFileType();
            final Engine engine = factoryBuilder.build().create(fileType);
         
            // Parse the files
            final List<WuicResource> retval = engine.parse(resources);
            
            return retval;
        } catch (BadConfigurationException bce) {
            throw new WuicException(bce);
        } catch (IOException ioe) {
            throw new WuicException(ioe);
        }
    }
    
    /**
     * <p>
     * Gets the last operation time.
     * </p>
     * 
     * @param groupId the group
     * @param file the file
     * @return the time stamp
     */
    private Long getLastOperation(final String groupId, final String file) {
        return lastOperations.get(getOperationId(groupId, file));
    }
    
    /**
     * <p>
     * Put the operation time.
     * </p>
     * 
     * @param groupId the group
     * @param file the file
     */
    private void putLastOperation(final String groupId, final String file) {
        lastOperations.put(getOperationId(groupId, file), System.currentTimeMillis());
    }

    /**
     * <p>
     * Generates an ID. Pattern : [groupId]$[file].
     * </p>
     * 
     * @param groupId the group
     * @param file the file
     * @return the ID
     */
    private String getOperationId(final String groupId, final String file) {
        return new StringBuilder().append(groupId).append("$").append(file).toString();
    }
}
