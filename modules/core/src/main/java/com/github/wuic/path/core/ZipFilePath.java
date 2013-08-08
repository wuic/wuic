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

import com.github.wuic.exception.wrapper.BadArgumentException;
import com.github.wuic.path.DirectoryPath;
import com.github.wuic.path.FilePath;
import com.github.wuic.util.CloseableZipFileAdapter;
import com.github.wuic.util.IOUtils;
import com.github.wuic.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * <p>
 * This class represents a path which could be considered as a {@link com.github.wuic.path.FilePath} and as a {@link com.github.wuic.path.DirectoryPath} with a
 * ZIP archive behind the scene. It is a directory since we can consider its entries as children and a path since we can
 * directly read the archive.
 * </p>
 *
 * @author Guillaume DROUET
 * @version 1.1
 * @since 0.3.4
 */
public class ZipFilePath extends ZipDirectoryPath implements FilePath, DirectoryPath {

    /**
     * The logger.
     */
    private final Logger log = LoggerFactory.getLogger(ZipFilePath.class);

    /**
     * The archive.
     */
    private File zipFile;

    /**
     * If this file should be deleted or not.
     */
    private Boolean keepZipFile;

    /**
     * <p>
     * Builds a new instance with the file name as path name.
     * </p>
     *
     * @param file the path
     * @param parent the parent
     * @throws java.io.IOException if any I/O error occurs
     */
    public ZipFilePath(final File file, final DirectoryPath parent) throws IOException {
        this(file, file.getName(), parent, Boolean.TRUE);
    }

    /**
     * <p>
     * Builds a new instance. Throws an {@link IllegalArgumentException} if the given path is not a ZIP archive.
     * </p>
     *
     * @param file the path
     * @param name the name
     * @param parent the parent
     * @param keepFile if given file should be deleted when object is garbage collected
     * @throws java.io.IOException if any I/O error occurs
     */
    public ZipFilePath(final File file, final String name, final DirectoryPath parent, final Boolean keepFile)
            throws IOException {
        super(name, parent);

        if (!IOUtils.isArchive(file)) {
            throw new IllegalArgumentException(String.format("%s is not a ZIP archive", file.getAbsolutePath()));
        }

        zipFile = file;
        keepZipFile = keepFile;
    }

    /**
     * <p>
     * Gets the concrete path pointing to the archive on path system.
     * </p>
     *
     * @return the raw path
     */
    public File getRawFile() {
        return zipFile;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public InputStream openStream() throws IOException {
        return new FileInputStream(zipFile);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String[] list() throws IOException {
        ZipFile archive = null;

        try {
            archive = new ZipFile(zipFile);
            final Enumeration<? extends ZipEntry> entries = archive.entries();
            final List<String> retval = new ArrayList<String>();

            // Read entries
            while (entries.hasMoreElements()) {
                final ZipEntry entry = entries.nextElement();
                final String entryName = entry.getName();

                // We only add the entries at the root level
                if (entryName.split(IOUtils.STD_SEPARATOR).length == 1) {
                    retval.add(entryName);
                }
            }

            return retval.toArray(new String[retval.size()]);
        } finally {
            IOUtils.close(new CloseableZipFileAdapter(archive));
        }
    }

    /**
     * <p>
     * Lists all the entries in the archive relatively to the given root entry name.
     * </p>
     *
     * @param baseEntry the root entry
     * @return the entries which are children of the root entry
     * @throws IOException if any I/O error occurs
     */
    public String[] list(final String baseEntry) throws IOException {
        ZipFile archive = null;

        // Directories end with a /
        final String rootEntry = StringUtils.merge(new String[] { baseEntry, "/", }, "/");

        try {
            archive = new ZipFile(zipFile);
            final Enumeration<? extends ZipEntry> entries = archive.entries();
            final List<String> retval = new ArrayList<String>();

            // Make sure we are going to list the entries of directory inside the archive
            if (!archive.getEntry(rootEntry).isDirectory()) {
                final String message = String.format("%s is not a ZIP directory entry", rootEntry);
                throw new BadArgumentException(new IllegalArgumentException(message));
            }

            // Read entries
            while (entries.hasMoreElements()) {
                final ZipEntry entry = entries.nextElement();
                final String entryName = entry.getName();

                // We only add the entries at the root level
                final String relativeEntry = entryName.replace(rootEntry, "");

                if (entryName.startsWith(rootEntry) && !relativeEntry.isEmpty() && relativeEntry.split(IOUtils.STD_SEPARATOR).length == 1) {
                    retval.add(relativeEntry);
                }
            }

            return retval.toArray(new String[retval.size()]);
        } finally {
            if (archive != null) {
                archive.close();
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ZipFile getZipFile() throws IOException {
        return new ZipFile(zipFile);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getLastUpdate() {
        return zipFile.lastModified();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String absoluteEntryOf(final String child) throws IOException {
        return child;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void finalize() throws Throwable {
        if (!keepZipFile && !zipFile.delete()) {
            log.warn("A ZipFilePath has been garbage collected but unable to delete {}", zipFile.getAbsolutePath());
        }

        super.finalize();
    }
}
