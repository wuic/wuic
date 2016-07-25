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
import com.github.wuic.path.FilePath;
import com.github.wuic.util.DefaultInput;
import com.github.wuic.util.Input;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipFile;

/**
 * <p>
 * This {@link com.github.wuic.path.FilePath} represents an entry inside a ZIP archive identified as a path.
 * </p>
 *
 * @author Guillaume DROUET
 * @since 0.3.4
 */
public class ZipEntryFilePath extends ZipEntryPath implements FilePath {

    /**
     * <p>
     * Builds a new instance.
     * </p>
     *
     * @param charset the charset
     * @param entry the ZIP entry name
     * @param parent the parent
     */
    public ZipEntryFilePath(final String entry, final DirectoryPath parent, final String charset) {
        super(entry, parent, charset);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Input openStream() throws IOException {
        final ArchiveWithParentEntry wrapper = findZipArchive("");
        final File file = wrapper.getArchive().getRawFile();

        final ZipFile zipFile;

        if (file.exists()) {
            zipFile = new ZipFile(file);
        } else {
            wrapper.getArchive().getChild(getName());
            zipFile = new ZipFile(file);
        }

        final InputStream is = zipFile.getInputStream(zipFile.getEntry(wrapper.getEntryPath()));

        return new DefaultInput(new ZipFileInputStream(file, zipFile, is), getCharset());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getLastUpdate() throws IOException {
        final ArchiveWithParentEntry wrapper = findZipArchive("");

        // Let's GC call finalize and close the ZIP path
        final ZipFile zipFile = new ZipFile(wrapper.getArchive().getRawFile());

        return zipFile.getEntry(wrapper.getEntryPath()).getTime();
    }

    /**
     * <p>
     * This class wraps a {@link ZipFile} and delegate the {@link #read()} method to an {@link InputStream} created by
     * itself. When {@link #close()} is invoked, both file and stream are closed.
     * </p>
     *
     * @author Guillaume DROUET
     * @since 0.3.4
     */
    public final class ZipFileInputStream extends InputStream {

        /**
         * The file on file system.
         */
        private File file;

        /**
         * The ZIP file.
         */
        private ZipFile zipFile;

        /**
         * The delegate input stream.
         */
        private InputStream inputStream;

        /**
         * <p>
         * Creates a new instance.
         * </p>
         *
         * @param f the file on file system
         * @param zf the ZIP file
         * @param is the delegate stream
         */
        private ZipFileInputStream(final File f, final ZipFile zf, final InputStream is) {
            file = f;
            zipFile = zf;
            inputStream = is;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int read() throws IOException {
            return inputStream.read();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void close() throws IOException {
            inputStream.close();
            zipFile.close();
        }

        /**
         * <p>
         * Gets the file on file system.
         * </p>
         *
         * @return the file
         */
        public File getFile() {
            return file;
        }
    }
}
