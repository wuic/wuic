/*
 * "Copyright (c) 2014   Capgemini Technology Services (hereinafter "Capgemini")
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
import com.github.wuic.path.DirectoryPath;
import com.github.wuic.path.DirectoryPathFactory;
import com.github.wuic.path.Path;
import com.github.wuic.path.core.FsDirectoryPathFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.Closeable;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.zip.CRC32;

/**
 * <p>
 * Utility class built on top of the {@code java.io} package helping WUIC to deal with
 * I/O.
 * </p>
 *
 * @author Guillaume DROUET
 * @version 1.6
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
     * Returns a new {@link MessageDigest} based on CRC algorithm.
     * </p>
     *
     * @return the message digest
     */
    public static MessageDigest newMessageDigest() {
        return new CrcMessageDigest();
    }

    /**
     * <p>
     * Digests each {@code String} in the given array and return the corresponding MD5 signature.
     * </p>
     *
     * @param strings the string array
     * @return the digested bytes
     */
    public static byte[] digest(final String ... strings) {
        final MessageDigest md = newMessageDigest();

        for (final String string : strings) {
            md.update(string.getBytes());
        }

        return md.digest();
    }

    /**
     * <p>
     * Digests each {@code byte} array in the given array and return the corresponding MD5 signature.
     * </p>
     *
     * @param bytes the byte arrays
     * @return the digested bytes
     */
    public static byte[] digest(final byte[] ... bytes) {
        final MessageDigest md = newMessageDigest();

        for (final byte[] byteArray : bytes) {
            md.update(byteArray);
        }

        return md.digest();
    }


    /**
     * <p>
     * Merges the given {@code String} array with the standard {@link IOUtils#STD_SEPARATOR separator}.
     * </p>
     *
     * @param paths the paths to be merged
     * @return the merged paths
     */
    public static String mergePath(final String ... paths) {
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
    public static void close(final Closeable... closeableArray) {
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
     * @return the content length
     * @throws com.github.wuic.exception.wrapper.StreamException in an I/O error occurs
     */
    public static int copyStream(final InputStream is, final OutputStream os)
            throws StreamException {
        try {
            return copyStreamIoe(is, os);
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
     * @return the content length
     * @throws IOException in an I/O error occurs
     */
    public static int copyStreamIoe(final InputStream is, final OutputStream os)
            throws IOException {
        int retval = 0;
        int offset;
        final byte[] buffer = new byte[WUIC_BUFFER_LEN];

        while ((offset = is.read(buffer)) != -1) {
            os.write(buffer, 0, offset);
            retval += offset - 1;
        }

        return retval;
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
        } catch (EOFException oef) {
            LOGGER.trace("File is not an archive, probably empty file", oef);
            return Boolean.FALSE;
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
        return listFile(parent, "", pattern, CollectionUtils.EMPTY_STRING_LIST);
    }

    /**
     * <p>
     * Searches as specified in {@link #listFile(com.github.wuic.path.DirectoryPath, java.util.regex.Pattern)} with
     * a list that contains all begin paths to ignore.
     * </p>
     *
     * @param parent the directory
     * @param pattern the pattern to filter files
     * @param skipStartsWithList a list that contains all begin paths to ignore
     * @return the matching files
     * @throws StreamException if any I/O error occurs
     */
    public static List<String> listFile(final DirectoryPath parent, final Pattern pattern, final List<String> skipStartsWithList)
            throws StreamException {
        return listFile(parent, "", pattern, skipStartsWithList);
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
     * @param skipStartsWithList a list that contains all begin paths to ignore
     * @return the matching files
     * @throws StreamException if any I/O error occurs
     */
    public static List<String> listFile(final DirectoryPath parent, final String relativePath, final Pattern pattern, final List<String> skipStartsWithList)
            throws StreamException {
        try {
            final String[] children = parent.list();
            final List<String> retval = new ArrayList<String>();

            // Check each child path
            childrenLoop:
            for (final String child : children) {
                final Path path = parent.getChild(child);
                final String childRelativePath = relativePath.isEmpty() ? child : mergePath(relativePath, child);

                // Child is a directory, search recursively
                if (path instanceof DirectoryPath) {

                    // Search recursively if and only if the beginning of the path if not in the excluding list
                    for (final String skipStartWith : skipStartsWithList) {
                        if (childRelativePath.startsWith(skipStartWith)) {
                            continue childrenLoop;
                        }
                    }

                    retval.addAll(listFile(DirectoryPath.class.cast(path), childRelativePath, pattern, skipStartsWithList));
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
     * Returns a hierarchy of {@link Path paths} represented by the given {@code String}. The given {@link DirectoryPathFactory}
     * is used to create directories.
     * </p>
     *
     * @param path the path hierarchy
     * @param factory the factory.
     * @return the last {@link Path} of the hierarchy with its parent
     * @throws IOException if any I/O error occurs
     */
    public static Path buildPath(final String path, final DirectoryPathFactory factory) throws IOException {
        LOG.debug("Build path for '{}'", path);

        // Always use '/' separator, even on windows
        final String absolutePath = IOUtils.normalizePathSeparator(path);
        final String[] tree = absolutePath.split(IOUtils.STD_SEPARATOR);

        // Build the root => force the path to / if its empty
        final String root = tree.length == 0 ? "/" : tree[0];
        final DirectoryPath retval =
                factory.create(root.isEmpty() && path.startsWith(IOUtils.STD_SEPARATOR) ? IOUtils.STD_SEPARATOR : root);

        // Build child path
        if (tree.length > 1) {
            return retval.getChild(IOUtils.mergePath(Arrays.copyOfRange(tree, 1, tree.length)));
        // No parent
        } else {
            return retval;
        }
    }

    /**
     * <p>
     * Returns a hierarchy of {@link Path paths} represented by the given {@code String}.
     * Uses a {@link FsDirectoryPathFactory} to create instances.
     * </p>
     *
     * @param path the path hierarchy
     * @return the last {@link Path} of the hierarchy with its parent
     * @throws IOException if any I/O error occurs
     */
    public static Path buildPath(final String path) throws IOException {
        return buildPath(path, new FsDirectoryPathFactory());
    }

    /**
     * <p>
     * A {@link CRC32} is wrapped inside this class which is a {@link MessageDigest}.
     * </p>
     *
     * @author Guillaume DROUET
     * @version 1.0
     * @since 0.5.0
     */
    public static final class CrcMessageDigest extends MessageDigest {

        /**
         * The CRC32 instance.
         */
        private final CRC32 crc;

        /**
         * Builds a new instance.
         */
        public CrcMessageDigest() {
            super("");
            crc = new CRC32();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void engineUpdate(final byte input) {
            crc.update(input);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void engineUpdate(final byte[] input, final int offset, final int len) {
            crc.update(input, offset, len);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected byte[] engineDigest() {
            ByteBuffer buffer = ByteBuffer.allocate(NumberUtils.HEIGHT);
            buffer.putLong(crc.getValue());
            return buffer.array();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void engineReset() {
            crc.reset();
        }
    }
}
