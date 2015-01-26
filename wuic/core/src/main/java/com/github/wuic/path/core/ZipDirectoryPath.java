/*
 * "Copyright (c) 2015   Capgemini Technology Services (hereinafter "Capgemini")
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
import com.github.wuic.util.CloseableZipFileAdapter;
import com.github.wuic.util.IOUtils;
import com.github.wuic.util.StringUtils;

import java.io.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * <p>
 * This class represents {@link com.github.wuic.path.DirectoryPath} for ZIP archives. Subclasses simply need to implement a
 * {@link com.github.wuic.path.core.ZipDirectoryPath#getZipFile()} method to retrieve a ZIP archive and
 * {@link com.github.wuic.path.core.ZipDirectoryPath#absoluteEntryOf(String)} method which returns an absolute
 * path of a specified entry in the archive.
 * </p>
 *
 * @author Guillaume DROUET
 * @version 1.1
 * @since 0.3.4
 */
public abstract class ZipDirectoryPath extends AbstractDirectoryPath implements DirectoryPath {

    /**
     * <p>
     * Builds a new instance.
     * </p>
     *
     * @param name the name
     * @param parent the parent
     */
    public ZipDirectoryPath(final String name, final DirectoryPath parent) {
        super(name, parent);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Path buildChild(final String child) throws IOException {
        ZipFile archive = null;
        InputStream is = null;
        OutputStream os = null;

        try {
            archive = getZipFile();
            final String absoluteEntry = absoluteEntryOf(child);
            ZipEntry entry = archive.getEntry(absoluteEntry);
            is = archive.getInputStream(entry);

            // Directories must end with a / otherwise input stream is null
            if (is == null) {
                entry = archive.getEntry(StringUtils.merge(new String[] { absoluteEntryOf(child), "/", }, "/"));
            }

            // Entry is a directory
            if (entry.isDirectory()) {
                return new ZipEntryDirectoryPath(child, this);
                // If the entry is a ZIP archive itself, copy it on the disk to be able to read it
            } else if (IOUtils.isArchive(is == null ? archive.getInputStream(entry) : is)) {
                final File entryArchiveDisk = File.createTempFile("entryArchive", ".zip");
                is = archive.getInputStream(entry);
                os = new FileOutputStream(entryArchiveDisk);
                IOUtils.copyStream(is, os);
                return new ZipFilePath(entryArchiveDisk, child, this);
            } else {

                // Entry is a path
                return new ZipEntryFilePath(child, this);
            }
        } finally {
            IOUtils.close(os, is, new CloseableZipFileAdapter(archive));
        }
    }

    /**
     * <p>
     * Gets the ZIP file related to this directory.
     * </p>
     *
     * @return the ZIP file
     * @throws IOException if any I/O error occurs
     */
    protected abstract ZipFile getZipFile() throws IOException;

    /**
     * <p>
     * Gets the absolute entry name of the given {@code String} built relatively to this directory's archive.
     * </p>
     *
     * @return the ZIP file
     * @throws IOException if any I/O error occurs
     */
    protected abstract String absoluteEntryOf(String child) throws IOException;
}
