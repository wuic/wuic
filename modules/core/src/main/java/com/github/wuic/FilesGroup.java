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

import com.github.wuic.configuration.Configuration;
import com.github.wuic.exception.wrapper.BadArgumentException;
import com.github.wuic.exception.wrapper.StreamException;
import com.github.wuic.resource.WuicResource;
import com.github.wuic.resource.WuicResourceFactory;
import com.github.wuic.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * This class represents a set of paths associated to their {@link Configuration}.
 * </p>
 *
 * <p>
 * A path is a relative and abstract location of one to many resources because it could be a regular expression.
 * </p>
 *
 * <p>
 * All the paths must refer to the same type fo file (CSS, JS, etc).
 * </p>
 * 
 * @author Guillaume DROUET
 * @version 1.4
 * @since 0.1.0
 */
public class FilesGroup {

    /**
     * Message's template displayed when no resource has been found.
     */
    private static final String EMPTY_PATH_MESSAGE = "Path(s) %s retrieved with %s don't correspond to any physic resources";

    /**
     * Message's template displayed when the extensions of resources path is not correct.
     */
    private static final String BAD_EXTENSIONS_MESSAGE = "Bad extension for resource %s associated to the FileType %s";

    /**
     * The configuration.
     */
    private Configuration configuration;
    
    /**
     * The paths list.
     */
    private List<String> paths;

    /**
     * The resource factory.
     */
    private WuicResourceFactory resourceFactory;

    /**
     * The resources corresponding to the paths.
     */
    private List<WuicResource> resources;

    /**
     * The ID identifying this group.
     */
    private String id;

    /**
     * <p>
     * Builds a new {@link FilesGroup}. All the paths must be named with an
     * extension that matches the {@link FileType}. If it is not the case, then
     * an {@link BadArgumentException} will be thrown.
     * </p>
     * 
     * @param config the {@link Configuration}
     * @param pathsList the paths
     * @param theResourceFactory the {@link WuicResourceFactory}
     * @param groupId the group ID
     * @throws com.github.wuic.exception.wrapper.StreamException if the resources can be retrieved
     */
    public FilesGroup(final Configuration config,
            final List<String> pathsList,
            final WuicResourceFactory theResourceFactory,
            final String groupId)
            throws StreamException {
        this.id = groupId;
        this.configuration = config;
        this.paths = pathsList;
        this.resourceFactory = theResourceFactory;
        this.resources = new ArrayList<WuicResource>();

        for (String path : paths) {
            resources.addAll(resourceFactory.create(path));
        }

        checkFiles();
    }

    /**
     * <p>
     * Builds a new {@link FilesGroup} by copying an existing one but with a new configuration.
     * </p>
     *
     * @param config the {@link Configuration}
     * @param other the group to copy
     * @throws com.github.wuic.exception.wrapper.StreamException if the resources can't be retrieved
     */
    public FilesGroup(final Configuration config,
                      final FilesGroup other) throws StreamException {
        this(config, other.paths, other.resourceFactory, other.id);
    }

    /**
     * <p>
     * Checks that the {@link FileType} and the paths list of this group are not
     * null. If they are, this methods will throw an {@link BadArgumentException}.
     * This exception could also be thrown if one file of the list does have a name
     * which ends with one of the possible {@link FileType#extensions extensions}.
     * </p>
     */
    private void checkFiles() {
        
        // Non null assertion
        if (paths == null || configuration == null) {
            throw new BadArgumentException(new IllegalArgumentException("A group must have a non-null paths list and a non-null file type"));
        // Do not allow empty groups
        }  else if (resources.isEmpty()) {
            final String merge = StringUtils.merge(paths.toArray(new String[paths.size()]), ", ");
            throw new BadArgumentException(new IllegalArgumentException(String.format(EMPTY_PATH_MESSAGE, merge, resourceFactory.toString())));
        }

        // Check the extension of each file
        for (WuicResource res : resources) {

            // Extract name to be test
            final String file = res.getName();

            Boolean valid = Boolean.FALSE;
            final FileType type = configuration.getFileType();
            
            final String[] extensions = type.getExtensions();
            
            // Apply test for each possible extension
            for (String extension : extensions) {
                if (file.endsWith(extension)) {
                    valid = Boolean.TRUE;
                    break;
                }
            }
            
            // The file has not one of the possible extension : throw an IAE
            if (!valid) {
                final String message = String.format(BAD_EXTENSIONS_MESSAGE, file, type);
                throw new BadArgumentException(new IllegalArgumentException(message));
            }
        }
    }

    /**
     * <p>
     * Gets the group's ID.
     * </p>
     *
     * @return the ID
     */
    public String getId() {
        return id;
    }

    /**
     * <p>
     * Gets the {@link Configuration}.
     * </p>
     * 
     * @return the configuration
     */
    public Configuration getConfiguration() {
        return configuration;
    }

    /**
     * <p>
     * Gets the paths list.
     * </p>
     * 
     * @return the paths
     * @throws com.github.wuic.exception.wrapper.StreamException if an error occurs while getting paths
     */
    public List<String> getPaths() throws StreamException {
        if (paths.isEmpty()) {
            return paths;
        } else {
            final List<String> retval = new ArrayList<String>();

            for (String path : paths) {
                retval.addAll(resourceFactory.computeRealPaths(path));
            }

            return retval;
        }
    }

    /**
     * <p>
     * Gets the {@link com.github.wuic.resource.WuicResourceFactory}.
     * </p>
     * 
     * @return the resource factory
     */
    public WuicResourceFactory getResourceFactory() {
        return resourceFactory;
    }

    /**
     * <p>
     * Gets all the resources of this group.
     * </p>
     *
     * @return the resources
     */
    public List<WuicResource> getResources() {
        return resources;
    }
}
