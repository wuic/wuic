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


package com.github.wuic.nut;

import com.github.wuic.FileType;
import com.github.wuic.configuration.Configuration;
import com.github.wuic.exception.wrapper.BadArgumentException;
import com.github.wuic.exception.wrapper.StreamException;
import com.github.wuic.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * This class represents a set of {@link Nut nuts}. Each {@link Nut} is built thanks to an associated {@link NutDao}
 * which only needs a particular path to create it.
 * </p>
 *
 * <p>
 * A path is an abstract location of one to many resources because it could be a regular expression. It is relative to
 * the base path of the {@link NutDao}.
 * </p>
 *
 * <p>
 * All the paths must refer to the same type fo path (CSS, JS, etc).
 * </p>
 * 
 * @author Guillaume DROUET
 * @version 1.4
 * @since 0.1.0
 */
public class NutsHeap {

    /**
     * Message's template displayed when no nut has been found.
     */
    private static final String EMPTY_PATH_MESSAGE = "Path(s) %s retrieved with %s don't correspond to any physic resources";

    /**
     * Message's template displayed when the extensions of resources path is not correct.
     */
    private static final String BAD_EXTENSIONS_MESSAGE = "Bad extension for nut %s associated to the FileType %s";

    /**
     * The configuration.
     */
    private Configuration configuration;
    
    /**
     * The paths list.
     */
    private List<String> paths;

    /**
     * The nut DAO.
     */
    private NutDao nutDao;

    /**
     * The resources corresponding to the paths.
     */
    private List<Nut> resources;

    /**
     * The ID identifying this group.
     */
    private String id;

    /**
     * <p>
     * Builds a new {@link NutsHeap}. All the paths must be named with an
     * extension that matches the {@link com.github.wuic.FileType}. If it is not the case, then
     * an {@link BadArgumentException} will be thrown.
     * </p>
     * 
     * @param config the {@link Configuration}
     * @param pathsList the paths
     * @param theNutDao the {@link NutDao}
     * @param heapId the heap ID
     */
    public NutsHeap(final Configuration config,
                    final List<String> pathsList,
                    final NutDao theNutDao,
                    final String heapId) {
        this.id = heapId;
        this.configuration = config;
        this.paths = pathsList;
        this.nutDao = theNutDao;
    }

    /**
     * <p>
     * Checks that the {@link com.github.wuic.FileType} and the paths list of this group are not
     * null. If they are, this methods will throw an {@link BadArgumentException}.
     * This exception could also be thrown if one path of the list does have a name
     * which ends with one of the possible {@link com.github.wuic.FileType#extensions extensions}.
     * </p>
     */
    private void checkFiles() {
        
        // Non null assertion
        if (paths == null) {
            throw new BadArgumentException(new IllegalArgumentException("A group must have a non-null paths list"));
        // Do not allow empty groups
        } else if (configuration == null) {
            throw new BadArgumentException(new IllegalArgumentException("A group must have a non-null configuration"));
        } else if (resources.isEmpty()) {
            final String merge = StringUtils.merge(paths.toArray(new String[paths.size()]), ", ");
            throw new BadArgumentException(new IllegalArgumentException(String.format(EMPTY_PATH_MESSAGE, merge, nutDao.toString())));
        }

        // Check the extension of each path
        for (Nut res : resources) {

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
            
            // The path has not one of the possible extension : throw an IAE
            if (!valid) {
                final String message = String.format(BAD_EXTENSIONS_MESSAGE, file, type);
                throw new BadArgumentException(new IllegalArgumentException(message));
            }
        }
    }

    /**
     * <p>
     * Gets the heap's ID.
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
     * Gets the {@link NutDao}.
     * </p>
     * 
     * @return the nut DAO
     */
    public NutDao getNutDao() {
        return nutDao;
    }

    /**
     * <p>
     * Gets all the nuts of this heap.
     * </p>
     *
     * @return the nuts
     * @throws StreamException if an I/O error occurs while building nuts
     */
    public List<Nut> getNuts() throws StreamException {
        if (resources == null) {
            this.resources = new ArrayList<Nut>();

            for (String path : paths) {
                resources.addAll(nutDao.create(path));
            }

            checkFiles();
        }

        return resources;
    }
}
