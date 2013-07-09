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


package com.github.wuic.util;

import com.github.wuic.exception.wrapper.StreamException;
import com.github.wuic.util.path.DirectoryPath;
import com.github.wuic.util.path.FsDirectoryPath;
import com.github.wuic.util.path.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.io.Closeable;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * <p>
 * Utility class built on top of the {@code java.io} package helping WUIC to deal with
 * I/O.
 * </p>
 *
 * @author Guillaume DROUET
 * @version 1.2
 * @since 0.3.1
 */
public final class IOUtils {

    /**
     * Logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(IOUtils.class);

    /**
     * The slash character is the standard separator used internally, even on windows platform.
     */
    public static final String STD_SEPARATOR = "/";

    /**
     * Length of a memory buffer used in WUIC.
     */
    public static final int WUIC_BUFFER_LEN = 2048;

    /**
     * All ZIP files begins with this magic number.
     */
    public static final int ZIP_MAGIC_NUMBER = 0x504b0304;

    /**
     * Logger.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(IOUtils.class);

    /**
     * <p>
     * Prevent instantiation of this class which provides only static methods.
     * </p>
     */
    private IOUtils() {

    }

    /**
     * <p>
     * Merges the given {@code String} array with the standard {@link IOUtils#STD_SEPARATOR separator}.
     * </p>
     *
     * @param paths the paths to be merged
     * @return the merged paths
     */
    public static String mergePath(String ... paths) {
        return StringUtils.merge(paths, STD_SEPARATOR);
    }

    /**
     * <p>
     * Makes sure a given path uses the slash character has path separator by replacing all backslashes.
     * </p>
     *
     * @param path the path to normalize
     * @return the normalized path
     */
    public static String normalizePathSeparator(final String path) {
        return path.replace("\\", STD_SEPARATOR);
    }

    /**
     * <p>
     * Tries to close the given objects and log the {@link IOException} at INFO level
     * to make the code more readable when we assume that the {@link IOException} won't be managed.
     * </p>
     *
     * <p>
     * Also ignore {@code null} parameters.
     * </p>
     *
     * @param closeableArray the objects to close
     */
    public static void close(final Closeable ... closeableArray) {
        for (Closeable closeable : closeableArray) {
            try {
                if (closeable != null) {
                    closeable.close();
                }
            } catch (IOException ioe) {
                LOGGER.info("Can't close the object", ioe);
            }
        }
    }

    /**
     * <p>
     * Reads the given reader and returns its content in a {@code String}.
     * </p>
     *
     * @param reader the reader
     * @return the content
     * @throws IOException if an I/O error occurs
     */
    public static String readString(final InputStreamReader reader) throws IOException {
        final StringBuilder builder = new StringBuilder();
        final char[] buff = new char[IOUtils.WUIC_BUFFER_LEN];
        int offset;

        // read content
        while ((offset = reader.read(buff)) != -1) {
            builder.append(buff, 0, offset);
        }

        return builder.toString();
    }

    /**
     * <p>
     * Copies the data from the given input stream into the given writer.
     * </p>
     *
     * @param is the {@code InputStream}
     * @param writer the {@code Writer}
     * @param cs the charset to use to convert byte array to char array
     * @throws com.github.wuic.exception.wrapper.StreamException in an I/O error occurs
     */
    public static void copyStreamToWriter(final InputStream is, final Writer writer, final String cs)
            throws StreamException {
        int offset;
        final byte[] buffer = new byte[WUIC_BUFFER_LEN];

        try {
            while ((offset = is.read(buffer)) != -1) {
                writer.write(new String(Arrays.copyOf(buffer, offset), cs));
            }
        } catch (IOException ioe) {
            throw new StreamException(ioe);
        }
    }

    /**
     * <p>
     * Copies the data from the given input stream into the given output stream.
     * </p>
     *
     * @param is the {@code InputStream}
     * @param os the {@code OutputStream}
     * @throws com.github.wuic.exception.wrapper.StreamException in an I/O error occurs
     */
    public static void copyStream(final InputStream is, final OutputStream os)
            throws StreamException {
        try {
            copyStreamIoe(is, os);
        } catch (IOException ioe) {
            throw new StreamException(ioe);
        }
    }

    /**
     * <p>
     * Copies the data from the given input stream into the given output stream and doesn't wrap any {@code IOException}.
     * </p>
     *
     * @param is the {@code InputStream}
     * @param os the {@code OutputStream}
     * @throws IOException in an I/O error occurs
     */
    public static void copyStreamIoe(final InputStream is, final OutputStream os)
            throws IOException {
        int offset;
        final byte[] buffer = new byte[WUIC_BUFFER_LEN];

        while ((offset = is.read(buffer)) != -1) {
            os.write(buffer, 0, offset);
        }
    }

    /**
     * <p>
     * Checks if the path path points to a a valid ZIP archive.
     * </p>
     *
     * @param file the path to check
     * @return {@code true} if path is an archive, {@code false} otherwise
     */
    public static Boolean isArchive(final File file) throws IOException {
        // File must exist, reachable and with a sufficient size to contain magic number
        if (file == null || !file.isFile() || !file.canRead() || file.length() < NumberUtils.TWO * NumberUtils.TWO) {
            return Boolean.FALSE;
        } else {
            return isArchive(new BufferedInputStream(new FileInputStream(file)));
        }
    }

    /**
     * <p>
     * Checks if the given stream points represents a valid ZIP archive.
     * </p>
     *
     * @param inputStream the stream to check
     * @return {@code true} if the stream should be an archive, {@code false} otherwise
     */
    public static Boolean isArchive(final InputStream inputStream) throws IOException {
        DataInputStream in = null;

        try {
            // Check that the path begins with magic number
            in = new DataInputStream(inputStream);
            return in.readInt() == ZIP_MAGIC_NUMBER;
        } finally {
            close(in);
        }
    }

    /**
     * <p>
     * Lists all the files from the given directory matching the given pattern.
     * </p>
     *
     * <p>
     * For instance, if a directory /foo contains a path in foo/oof/path.js, calling this method with an {@link Pattern}
     * .* will result in an array containing the {@code String} {@code oof/path.js}.
     * </p>
     *
     * @param parent the directory
     * @param pattern the pattern to filter files
     * @return the matching files
     * @throws StreamException if any I/O error occurs
     */
    public static List<String> listFile(final DirectoryPath parent, final Pattern pattern) throws StreamException {
        return listFile(parent, "", pattern);
    }

    /**
     * <p>
     * Lists the files matching the given pattern in the directory path and its subdirectory represented by
     * a specified {@code relativePath}.
     * </p>
     *
     * @param parent the parent
     * @param relativePath the directory path relative to the parent
     * @param pattern the pattern which filters files
     * @return the matching files
     * @throws StreamException if any I/O error occurs
     */
    public static List<String> listFile(final DirectoryPath parent, final String relativePath, final Pattern pattern) throws StreamException {
        try {
            final String[] children = parent.list();
            final List<String> retval = new ArrayList<String>();

            // Check each child path
            for (String child : children) {
                final Path path = parent.getChild(child);
                final String childRelativePath = relativePath.isEmpty() ? child : mergePath(relativePath, child);

                // Child is a directory, search recursively
                if (path instanceof DirectoryPath) {
                    retval.addAll(listFile(DirectoryPath.class.cast(path), childRelativePath, pattern));
                // Files matches, return
                } else if (pattern.matcher(childRelativePath).matches()) {
                    retval.add(childRelativePath);
                }
            }

            return retval;
        } catch (IOException ioe) {
            throw new StreamException(ioe);
        }
    }

    /**
     * <p>
     * Returns a hierarchy of {@link Path paths} represented by the given {@code String}.
     * </p>
     *
     * @param path the path hierarchy
     * @return the last {@link Path} of the hierarchy with its parent
     * @throws IOException if any I/O error occurs
     */
    public static Path buildPath(final String path) throws IOException {
        LOG.debug("Build path for '{}'", path);

        // Always use '/' separator, even on windows
        final String absolutePath = IOUtils.normalizePathSeparator(path);
        final String[] tree = absolutePath.split(IOUtils.STD_SEPARATOR);

        // Build the root => force the path to / if its empty
        final String root = tree[0];
        DirectoryPath retval = new FsDirectoryPath(root.isEmpty() && path.startsWith(IOUtils.STD_SEPARATOR) ?
                IOUtils.STD_SEPARATOR : root, null);

        // Build child path
        if (tree.length > 1) {
            return retval.getChild(IOUtils.mergePath(Arrays.copyOfRange(tree, 1, tree.length)));
        // No parent
        } else {
            return retval;
        }
    }
}
