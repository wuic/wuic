/*
 * "Copyright (c) 2016   Capgemini Technology Services (hereinafter "Capgemini")
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

import com.github.wuic.path.DirectoryPath;
import com.github.wuic.path.Path;
import com.github.wuic.util.CollectionUtils;
import com.github.wuic.util.IOUtils;

import java.io.IOException;
import java.util.List;

/**
 * <p>
 * This {@link com.github.wuic.path.Path} represents an entry inside a ZIP archive.
 * </p>
 *
 * @author Guillaume DROUET
 * @version 1.1
 * @since 0.3.4
 */
public class ZipEntryPath extends SimplePath implements Path {

    /**
     * <p>
     * Builds a new instance. Throws an {@link IllegalArgumentException} if the parent is {@code null} because an entry
     * must have a parent.
     * </p>
     *
     * @param entry the ZIP entry name
     * @param parent the parent
     */
    public ZipEntryPath(final String entry, final DirectoryPath parent) {
        super(entry, parent);

        if (parent == null) {
            throw new IllegalArgumentException(String.format("%s is a ZIP entry and must have a non-null parent", entry));
        }
    }

    /**
     * <p>
     * This method goes through parents to find the first {@link ZipFilePath} which represents the archive containing
     * this path.
     * </p>
     *
     * @param path a path the path to append to the parent path starting at the zip archive root entry
     * @return a wrapper of the archive and the entry to the given path
     */
    public ArchiveWithParentEntry findZipArchive(final String path) {
        final List<String> entries = CollectionUtils.newList(getName());

        if  (path != null && !path.isEmpty()) {
            entries.add(path);
        }

        // Go through parents until we find the ZIP archive which is able to access to its entries
        DirectoryPath zipArchive = getParent();

        while (zipArchive != null && !(zipArchive instanceof ZipFilePath)) {
            final String name = zipArchive.getName();
            entries.add(0, name);
            zipArchive = zipArchive.getParent();
        }

        // Should never occurs, a ZIP archive must exist as a parent
        if (zipArchive == null) {
            throw new IllegalStateException();
        }

        final String[] array = entries.toArray(new String[entries.size()]);
        return new ArchiveWithParentEntry(ZipFilePath.class.cast(zipArchive), IOUtils.mergePath(array));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getLastUpdate() throws IOException {
        // We let the GC close the ZIP archive by calling its finalize method
        return findZipArchive("").getArchive().getZipFile().getEntry(getName()).getTime();
    }

    /**
     * <p>
     * Simple wrapper of a ZIP archive and a path which could be retrieved as an entry.
     * </p>
     *
     * @author Guillaume DROUET
     * @version 1.0
     * @since 0.3.4
     */
    protected final class ArchiveWithParentEntry {

        /**
         * The ZIP archive.
         */
        private ZipFilePath archive;

        /**
         * The entry path.
         */
        private String entryPath;

        /**
         * <p>
         * Builds a new instance.
         * </p>
         *
         * @param zfp the archive
         * @param entry the entry path inside the archive
         */
        ArchiveWithParentEntry(final ZipFilePath zfp, String entry) {
            archive = zfp;
            entryPath = entry;
        }

        /**
         * <p>
         * Gets an archive.
         * </p>
         *
         * @return the archive
         */
        public ZipFilePath getArchive() {
            return archive;
        }

        /**
         * <p>
         * Gets an entry path.
         * </p>
         *
         * @return the entry path
         */
        public String getEntryPath() {
            return entryPath;
        }
    }
}
