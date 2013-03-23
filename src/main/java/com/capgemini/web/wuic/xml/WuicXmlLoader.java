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


package com.capgemini.web.wuic.xml;

import com.capgemini.web.wuic.FileType;
import com.capgemini.web.wuic.FilesGroup;
import com.capgemini.web.wuic.WuicResource;
import com.capgemini.web.wuic.configuration.BadConfigurationException;
import com.capgemini.web.wuic.configuration.Configuration;
import com.capgemini.web.wuic.configuration.DomConfigurationBuilder;
import com.capgemini.web.wuic.configuration.ImageConfiguration;
import com.capgemini.web.wuic.configuration.SourceRootProvider;
import com.capgemini.web.wuic.configuration.SpriteConfiguration;
import com.capgemini.web.wuic.configuration.WuicEhcacheProvider;
import com.capgemini.web.wuic.configuration.impl.DefaultEhCacheProvider;
import com.capgemini.web.wuic.configuration.impl.ImageConfigurationImpl;
import com.capgemini.web.wuic.configuration.impl.SpriteConfiguratonDomBuilder;
import com.capgemini.web.wuic.configuration.impl.YuiCssConfigurationDomBuilder;
import com.capgemini.web.wuic.configuration.impl.YuiJavascriptConfigurationDomBuilder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import net.sf.ehcache.Cache;

import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * <p>
 * This class is in charge to load the WUIC XML file which contains all the
 * grouped files and the configurations to apply for the different file types.
 * </p>
 * 
 * <p>
 * When you create a new {@code WuicXmlLoader}, then the file 'wuic.xml' located
 * at the root classpath is read using DOM API. Try create an instance of this
 * class only once in a production environment.
 * </p>
 * 
 * @author Guillaume DROUET
 * @version 1.2
 * @since 0.1.0
 */
public final class WuicXmlLoader {

    /**
     * Time stamp which is used as unique identified for generated groups.
     */
    private static final long INSTANCE_TIMESTAMP = System.nanoTime();

    /**
     * The logger.
     */
    private final Logger logger = Logger.getLogger(getClass());

    /**
     * All the {@link DomConfigurationBuilder builders} for each supported {@link FileType}.
     */
    private Map<FileType, DomConfigurationBuilder> confBuildersForFileType;
    
    /**
     * All the loaded {@link Configuration configurations} associated to their ID.
     */
    private Map<String, Configuration> builtConfigurations;
    
    /**
     * All the loaded files groups associated to their ID.
     */
    private Map<String, FilesGroup> filesGroups;
    
    /**
     * The source root providers associated to their ID.
     */
    private Map<String, SourceRootProvider> sourceRootProviders;
    
    /**
     * The cache indicated in the configuration.
     */
    private Cache cache;
    
    /**
     * <p>
     * Builds a new {@code WuicXmlLoader}. Try to load the 'wuic.xml' file using
     * the DOM API and read the configurations and the files groups.
     * </p>
     * 
     * @throws IOException if an I/O error occurs while reading the file
     * @throws BadConfigurationException if the 'wuic.xml' file is not properly defined
     */
    public WuicXmlLoader() throws IOException, BadConfigurationException {
        final DocumentBuilderFactory factory = getFactory();

        try {
            // The elements to be read from the file
            sourceRootProviders = new HashMap<String, SourceRootProvider>();
            builtConfigurations = new HashMap<String, Configuration>();
            filesGroups = new HashMap<String, FilesGroup>();
            
            // All possible builders for each supported FileType
            confBuildersForFileType = new HashMap<FileType, DomConfigurationBuilder>();
            confBuildersForFileType.put(FileType.CSS, new YuiCssConfigurationDomBuilder());
            confBuildersForFileType.put(FileType.JAVASCRIPT, new YuiJavascriptConfigurationDomBuilder());
            confBuildersForFileType.put(FileType.SPRITE, new SpriteConfiguratonDomBuilder());

            // Create and configure the builder
            final DocumentBuilder builder = factory.newDocumentBuilder();
            builder.setErrorHandler(new WuicErrorHandler());
            builder.setEntityResolver(new WuicEntityResolver());
            
            // Load the DOM
            final Document document = builder.parse(getClass().getResourceAsStream("/wuic.xml"));

            // Load cache
            readCache(document);
            
            // Read source root providers
            readSourceRootProviders(document);
            
            // Read configurations
            readConfigurations(document);
            
            // Read files groups
            readGroups(document);
        } catch (ParserConfigurationException pce) {
            throw new BadConfigurationException(pce);
        } catch (SAXException se) {
            throw new BadConfigurationException(se);
        }
    }

    /**
     * <p>
     * Generates a group ID based on the files.
     * </p>
     * 
     * @param files the files
     * @return the generated group ID
     */
    public static String createGeneratedGroupId(final Collection<String> files) {
        final StringBuilder imageIdBuilder = new StringBuilder();
        imageIdBuilder.append(INSTANCE_TIMESTAMP);
        imageIdBuilder.append("$");
        
        // Identifier is based on an hash code obtained with the all the files names
        final HashCodeBuilder builder = new HashCodeBuilder();
        
        for (String res : files) {
            builder.append(res);
        }
        
        imageIdBuilder.append(builder.toHashCode()); 
        
        return imageIdBuilder.toString();
    }

    /**
     * <p>
     * Generates a group ID based on the given group. The ID is not generated to
     * identify the given group but another group which refers to it.
     * </p>
     * 
     * @param group the group the generated ID should be based on
     * @return the generated group ID
     */
    private String createGeneratedGroupId(final FilesGroup group) {
        return createGeneratedGroupId(group.getFiles());
    }
    
    /**
     * <p>
     * Gets the {@code DocumentBuilderFactory} to use. Configured to produce
     * builders that create document with :
     * <ul>
     * <li>Ignoring comments</li>
     * <li>Validating the documents</li>
     * <li>Ignoring white spaces elements</li>
     * </ul>
     * </p>
     * 
     * @return the document builder factory to use
     */
    private DocumentBuilderFactory getFactory() {
        final DocumentBuilderFactory retval = DocumentBuilderFactory.newInstance();
        
        retval.setIgnoringComments(Boolean.TRUE);
        retval.setValidating(Boolean.TRUE);
        retval.setIgnoringElementContentWhitespace(Boolean.TRUE);
        
        return retval;
    }

    /**
     * <p>
     * Reads the {@link SourceRootProvider} defined in the wuic.xml file. A set
     * of <sourceRootProvider> tags should be defined and will be read by this
     * method.
     * </p>
     * 
     * @param document the document which contains the {@link SourceRootProvider} to read
     * @throws BadConfigurationException if the document does not contains the expected structure
     */
    private void readSourceRootProviders(final Document document) throws BadConfigurationException {
        // Get the nodes
        final NodeList sourceRootProviderList = document.getElementsByTagName("sourceRootProvider");

        // Read each node
        for (int i = 0; i < sourceRootProviderList.getLength(); i++) {
            final Node sourceRootProviderNode = sourceRootProviderList.item(i);
            
            // Get the ID that identifies the class instance
            final Node idAttr = sourceRootProviderNode.getAttributes().getNamedItem("id");
            
            if (idAttr == null) {
                throw new BadConfigurationException("sourceRootProvider tag must contains an ID attribute");
            }
            
            // Get the class to be instantiated
            final Node classAttr = sourceRootProviderNode.getAttributes().getNamedItem("class");
            
            if (classAttr == null) {
                throw new BadConfigurationException("sourceRootProvider tag must contains a class attribute");
            }

            // Node try to create an instance of a SourceRootProvider
            final String className = classAttr.getNodeValue();
            
            try {
                final Class<?> currentClass = Class.forName(className);
                final Class<SourceRootProvider> targetClass = SourceRootProvider.class;
                sourceRootProviders.put(idAttr.getNodeValue(), targetClass.cast(currentClass.newInstance()));
            } catch (ClassNotFoundException cnfe) {
                throw new BadConfigurationException(cnfe);
            } catch (IllegalAccessException iae) {
                throw new BadConfigurationException(iae);
            } catch (InstantiationException ie) {
                throw new BadConfigurationException(ie);
            }
        }
    }

    /**
     * <p>
     * Reads from the given document the cache name from EHCache framework to use
     * in WUIC framework.
     * </p>
     * 
     * <p>
     * If the cache does not exists, then a default cache will be used.
     * </p>
     * 
     * @param document the document which contains the {@link SourceRootProvider} to read
     * @throws BadConfigurationException if a specified ehcache-provider could not be used
     */
    private void readCache(final Document document) throws BadConfigurationException {
        final String cacheProviderClass = document.getDocumentElement().getAttribute("ehcache-provider");
        final WuicEhcacheProvider cacheProvider;
        
        if (cacheProviderClass == null || cacheProviderClass.isEmpty()) {
            cacheProvider = new DefaultEhCacheProvider();
        } else {
            try {
                cacheProvider = WuicEhcacheProvider.class.cast(Class.forName(cacheProviderClass).newInstance());
            } catch (InstantiationException ie) {
                final String message = "Cannot create a WuicEhcacheProvider for class : " + cacheProviderClass;
                throw new BadConfigurationException(message, ie);
            } catch (IllegalAccessException iae) {
                final String message = "Cannot create a WuicEhcacheProvider for class : " + cacheProviderClass;
                throw new BadConfigurationException(message, iae);
            } catch (ClassNotFoundException cnfe) {
                final String message = "Cannot create a WuicEhcacheProvider for class : " + cacheProviderClass;
                throw new BadConfigurationException(message, cnfe);
            }
        }
        
        cache = cacheProvider.getCache();
    }
    
    /**
     * <p>
     * Reads the configurations. All the configurations are located in a
     * <configurations> tag which must exists. Each possible configuration is
     * represented by a <configuration> tag as a child of the <configurations>
     * tag.
     * </p>
     * 
     * @param document the document which contains the configurations to read
     * @throws BadConfigurationException if the document does not contains the expected structure
     */
    private void readConfigurations(final Document document) throws BadConfigurationException {
        // Get the root element
        final Node configurations = document.getElementsByTagName("configurations").item(0);
        
        // Read each configuration
        for (int i = 0; i < configurations.getChildNodes().getLength(); i++) {
            final Node configuration = configurations.getChildNodes().item(i);
            
            // From the type, we can determine which builder has to be used
            final Node typeAttribute = configuration.getAttributes().getNamedItem("type");
            final FileType type = FileType.parseFileType(typeAttribute.getNodeValue());
            
            // A builder exists
            if (confBuildersForFileType.containsKey(type)) {
                
                // The 'id' attribute identifies the configuration
                final Node idAttribute = configuration.getAttributes().getNamedItem("id");
                
                // Build the Configuration instance
                final Configuration conf = confBuildersForFileType.get(type).build(configuration, cache);
                
                // Store the built configuration
                builtConfigurations.put(idAttribute.getNodeValue(), conf);
            } else {
                // A builder has not be found, notify it in a warning message
                logger.warn("There is no builder for the type " + type, new UnsupportedOperationException());
            }
        }
    }

    /**
     * <p>
     * Reads the files groups.  All the groups are located in a <groups>
     * tag which must exists. Each group is represented by a <group> tag
     * as a child of the <groups> tag. The group itself has a list of <file>
     * tags which represent all the files composing it.
     * </p>
     * 
     * @param document the document which contains the groups to read
     * @throws BadConfigurationException if the {@link SourceRootProvider} could not be read
     */
    private void readGroups(final Document document) throws BadConfigurationException {
        // Get the root element
        final Node groups = document.getElementsByTagName("groups").item(0);
                
        // Read each group
        for (int i = 0; i < groups.getChildNodes().getLength(); i++) {
            final Node group = groups.getChildNodes().item(i);
            
            final List<String> files = new ArrayList<String>(group.getChildNodes().getLength());
            
            // Read each child representing a file of the group
            for (int j = 0; j < group.getChildNodes().getLength(); j++) {
                final Node file = group.getChildNodes().item(j);
                files.add(file.getFirstChild().getTextContent());
            }
            
            // The group is identified by the 'id' attribute
            final Node idAttribute = group.getAttributes().getNamedItem("id");
            
            // The group points to a configuration id
            final Node configAttribute = group.getAttributes().getNamedItem("configuration");
            final Configuration config = builtConfigurations.get(configAttribute.getNodeValue());
            
            // The source root provider to use is specified with its ID
            final Node srpAttribute = group.getAttributes().getNamedItem("sourceRootProvider");
            
            if (srpAttribute == null || !sourceRootProviders.containsKey(srpAttribute.getNodeValue())) {
                final StringBuilder msg = new StringBuilder("The group does not ");
                msg.append("contains a sourceRootProvider attribute that ");
                msg.append("corresponds to an existing sourceRootProvider ID");
                throw new BadConfigurationException(msg.toString());
            }

            // Get the source root provider and create the files group
            final SourceRootProvider srp = sourceRootProviders.get(srpAttribute.getNodeValue());
            final FilesGroup filesGroup = new FilesGroup(config, files, srp);
            
            // Particular case : sprite needs an image group
            if (config instanceof SpriteConfiguration) {
                putImageGroupFor(idAttribute.getNodeValue(), filesGroup);
            }
            
            // Add all the files read from the document and associate them to the ID
            filesGroups.put(idAttribute.getNodeValue(), filesGroup);
        }
    }
    
    /**
     * <p>
     * Creates and puts an image group based of the given sprite group.
     * </p>
     * 
     * @param groupId the group ID
     * @param filesGroup the sprite group
     */
    private void putImageGroupFor(final String groupId, final FilesGroup filesGroup) {
        // Builds the ID based on the given sprite group
        final String imageId = createGeneratedGroupId(filesGroup);
        
        // Register configuration
        final ImageConfiguration config = new ImageConfigurationImpl(filesGroup.getConfiguration());
        builtConfigurations.put("image-png", config);
        
        // Creates the image group base on the given group
        final FilesGroup imageGroup = new FilesGroup(config,
                filesGroup.getFiles(),
                new ProxySourceRootProvider(filesGroup.getSourceRootProvider(), imageId, groupId));
       
        // Add the new group
        filesGroups.put(imageId, imageGroup);
    }
    
    /**
     * <p>
     * Gets a configuration identified by the given ID.
     * </p>
     * 
     * @param id the id
     * @return the {@link Configuration}, {@code null} if not item exists
     */
    public Configuration getConfiguration(final String id) {
        return builtConfigurations.get(id);
    }
    
    /**
     * <p>
     * Gets a group of files identified by the given ID.
     * </p>
     * 
     * @param id the id
     * @return the {@code List<String>}, {@code null} if not item exists
     */
    public FilesGroup getFilesGroup(final String id) {
        return filesGroups.get(id);
    }
    
    /**
     * <p>
     * Gets all the registered group identifiers. 
     * </p>
     * 
     * @return the collection of IDs
     */
    public Collection<String> filesGroupIdList() {
        return filesGroups.keySet();
    }
    
    /**
     * <p>
     * Gets the cache.
     * </p>
     * 
     * @return the cache
     */
    public Cache getCache() {
        return cache;
    }

    /**
     * <p>
     * Proxy source root provider that maps an given group to another group. This
     * another group has to be used when calling the wrapped {@link SourceRootProvider}.
     * </p>
     * 
     * @author Guillaume DROUET
     * @version 1.0
     * @since 0.2.0
     */
    private static class ProxySourceRootProvider implements SourceRootProvider {
        
        /**
         * The wrappes {@link SourceRootProvider}.
         */
        private SourceRootProvider sourceRootProvider;
        
        /**
         * The proxy group ID.
         */
        private String proxyGroupId;
        
        /**
         * The mapper group to actually use.
         */
        private String targetGroupId;
        
        /**
         * <p>
         * Builds a new instance.
         * </p>
         * 
         * @param srp the source root provider
         * @param pgid the proxy group id
         * @param tgid the target group id
         */
        public ProxySourceRootProvider(final SourceRootProvider srp, final String pgid, final String tgid) {
            sourceRootProvider = srp;
            proxyGroupId = pgid;
            targetGroupId = tgid;
        }
        
        /**
         * {@inheritDoc}
         */
        @Override
        public Boolean hasChanged(final String gid, final String file, final Long since) {
            if (proxyGroupId.equals(gid)) {
                return sourceRootProvider.hasChanged(targetGroupId, file, since);
            } else {
                return sourceRootProvider.hasChanged(gid, file, since);
            }
        }
        
        /**
         * {@inheritDoc}
         */
        @Override
        public WuicResource getStreamResource(final String gid, final String file)
                throws IOException {
            if (proxyGroupId.equals(gid)) {
                return sourceRootProvider.getStreamResource(targetGroupId, file);
            } else {
                return sourceRootProvider.getStreamResource(gid, file);
            }
        }
    }
}
