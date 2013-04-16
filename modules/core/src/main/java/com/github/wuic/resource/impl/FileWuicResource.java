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

package com.github.wuic.resource.impl;

import com.github.wuic.FileType;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * <p>
 * Represents a resource on the file system provided or to be managed by the WUIC framework.
 * </p>
 * 
 * @author Guillaume DROUET
 * @version 1.1
 * @since 0.1.1
 */
public class FileWuicResource extends AbstractWuicResource {

    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 1L;

    /**
     * The root directory that contains the files of a same group.
     */
    private String rootDirectory;

    /**
     * <p>
     * Builds a new {@code WuicResource} based on a given file.
     * </p>
     * 
     * @param rootDir the root directory
     * @param name the source file
     * @param ft the file type
     */
    public FileWuicResource(final String rootDir, final String name, final FileType ft) {
        super(name, ft);
        rootDirectory = rootDir;
    }
    
    /**
     * <p>
     * Opens and returns an {@code InputStream} pointing to the resource. 
     * </p>
     * 
     * @return the opened input stream
     * @throws IOException if an I/O error occurs
     */
    public InputStream openStream() throws IOException {
        if (getName() == null) {
            return null;
        }
        
        final StringBuilder pathBuilder = new StringBuilder();
        pathBuilder.append(rootDirectory);
        pathBuilder.append(System.getProperty("file.separator"));
        pathBuilder.append(getName());
        
        return new FileInputStream(pathBuilder.toString());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getBaseDirectory() {
        return rootDirectory;
    }
}
