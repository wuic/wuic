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


package com.github.wuic.path.core;

import com.github.wuic.path.AbstractDirectoryPath;
import com.github.wuic.path.DirectoryPath;
import com.github.wuic.path.Path;
import com.github.wuic.util.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * <p>
 * This {@link com.github.wuic.path.DirectoryPath} represented a directory on the path system.
 * </p>
 *
 * @author Guillaume DROUET
 * @version 1.2
 * @since 0.3.4
 */
public class FsDirectoryPath extends AbstractDirectoryPath implements DirectoryPath {

    /**
     * Logger.
     */
    private final Logger log = LoggerFactory.getLogger(getClass());

    /**
     * The directory path represented by this path.
     */
    private File directory;

    /**
     * <p>
     * Builds a new instance. Throws a {@link IllegalArgumentException} if the specified path does not represents a
     * directory on the path system.
     * </p>
     *
     * @param file the directory
     * @param parent the parent, {@code null} if this path is a root
     */
    public FsDirectoryPath(final File file, final DirectoryPath parent) {
        super(file.getName().isEmpty() ? file.getPath() : file.getName(), parent);

        if (!file.isDirectory()) {
            throw new IllegalArgumentException(String.format("%s is not a directory on the file system", file.getAbsolutePath()));
        }

        directory = file;
    }

    /**
     * <p>
     * Builds a new instance. Throws a {@link IllegalArgumentException} if the specified path does not represents a
     * directory on the path system.
     * </p>
     *
     * @param path the directory
     * @param parent the parent, {@code null} if this path is a root
     */
    public FsDirectoryPath(final String path, final DirectoryPath parent) {
        this(new File(path), parent);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Path buildChild(final String child) throws IOException {
        final String absolutePath = IOUtils.mergePath(getAbsolutePath(), child);
        log.debug("Build child with absolute path {}", absolutePath);
        File file = new File(absolutePath);

        // In classpath notation, JAR ends with a "!"
        if (!file.exists() && file.getName().endsWith("!")) {
            file = new File(file.getParent(), file.getName().substring(0, file.getName().length() - 1));
        }

        if (!file.exists()) {
            throw new FileNotFoundException(String.format("%s is not an existing file", file.getAbsolutePath()));
        } else if (file.isDirectory()) {
            return new FsDirectoryPath(file, this);
        } else {
            return IOUtils.isArchive(file) ? new ZipFilePath(file, this) : new FsFilePath(file, this);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String[] list() throws IOException {
        return directory.list();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getLastUpdate() {
        return directory.lastModified();
    }
}
