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
import com.github.wuic.configuration.SourceRootProvider;

import java.util.List;

/**
 * <p>
 * This class represents a set a files associated to their {@link Configuration}.
 * All the files should be in the same type.
 * </p>
 * 
 * @author Guillaume DROUET
 * @version 1.0
 * @since 0.1.0
 */
public class FilesGroup {

    /**
     * The configuration.
     */
    private Configuration configuration;
    
    /**
     * The files list.
     */
    private List<String> files;

    /**
     * The source root provider.
     */
    private SourceRootProvider sourceRootProvider;
    
    /**
     * <p>
     * Builds a new {@link FilesGroup}. All the files must be named with an
     * extension that matches the {@link FileType}. If it is not the case, then
     * an {@link IllegalArgumentException} will be thrown.
     * </p>
     * 
     * @param config the {@link Configuration}
     * @param filesList the files
     * @param theSourceRootProvider the {@link SourceRootProvider}
     */
    public FilesGroup(final Configuration config,
            final List<String> filesList,
            final SourceRootProvider theSourceRootProvider) {
        this.configuration = config;
        this.files = filesList;
        this.sourceRootProvider = theSourceRootProvider;
        checkFiles();
    }

    /**
     * <p>
     * Checks that the {@link FileType} and the files list of this group are not
     * null. If they are, this methods will throw an {@link IllegalArgumentException}.
     * This exception could also be thrown if one file of the list does have a name
     * which ends with one of the possible {@link FileType#extensions extensions}.
     * </p>
     */
    private void checkFiles() {
        
        // Non null assertion
        if (files == null || configuration == null) {
            throw new IllegalArgumentException("A group must have a non-null files list and a non-null file type");
        }
        
        // Check the extension of each file
        for (String file : files) {
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
                throw new IllegalArgumentException("Bad extensions for files associated to the FileType " + type);
            }
        }
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
     * Gets the files list.
     * </p>
     * 
     * @return the files
     */
    public List<String> getFiles() {
        return files;
    }

    /**
     * <p>
     * Gets the {@link SourceRootProvider}.
     * </p>
     * 
     * @return the source root provider
     */
    public SourceRootProvider getSourceRootProvider() {
        return sourceRootProvider;
    }
}
