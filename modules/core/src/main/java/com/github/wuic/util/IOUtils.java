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

import com.github.wuic.exception.WuicResourceNotFoundException;
import com.github.wuic.exception.wrapper.BadArgumentException;
import com.github.wuic.exception.wrapper.StreamException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

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
     * The slash character is the standard separator used internally, even on windows platform.
     */
    public static final String STD_SEPARATOR = "/";

    /**
     * Length of a memory buffer used in WUIC.
     */
    public static final int WUIC_BUFFER_LEN = 2048;

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
     * Tries to close the given object and log the {@link IOException} at INFO level
     * to make the code more readable when we assume that the {@link IOException} won't be managed.
     * </p>
     *
     * <p>
     * Also ignore {@code null} parameters.
     * </p>
     *
     * @param closeable the object to close
     */
    public static void close(final Closeable closeable) {
        try {
            if (closeable != null) {
                closeable.close();
            }
        } catch (IOException ioe) {
            LOGGER.info("Can't close the object", ioe);
        }
    }

    /**
     * <p>
     * Deletes a directory. Begins by delete the files inside the directory recursively.
     * </p>
     *
     * @param directory directory to delete
     * @throws IOException if a file could not be deleted
     */
    public static void deleteDirectory(final File directory) throws IOException {
        if (directory.exists()) {
            // Delete all files inside directory
            for (File file : directory.listFiles()) {

                // Delete directory recursively
                if (file.isDirectory()) {
                    deleteDirectory(file);
                } else if (file.isFile() && !file.delete()) {
                    throw new IOException("Can't delete the file " + file.getAbsolutePath());
                }
            }

            if (!directory.delete()) {
                throw new IOException("Can't delete the file " + directory.getAbsolutePath());
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
     * Builds a new stream pointing to a file represented by the given path.
     * </p>
     *
     * @param path the path
     * @return the stream
     * @throws WuicResourceNotFoundException if the file is not found
     */
    public static InputStream newFileInputStream(final String path) throws WuicResourceNotFoundException {
        try {
            return new FileInputStream(path);
        } catch (FileNotFoundException fne) {
            throw new WuicResourceNotFoundException(fne);
        }
    }

    /**
     * <p>
     * Copies the data from the given reader to the given output stream.
     * </p>
     *
     * @param reader the {@code Reader}
     * @param output the {@code OutputStream}
     * @throws StreamException in an I/O error occurs
     */
    public static void copyReaderToStream(final Reader reader, final OutputStream output)
            throws StreamException {
        int offset;
        final char[] buffer = new char[WUIC_BUFFER_LEN];

        try {
            while ((offset = reader.read(buffer)) != -1) {
                output.write(String.copyValueOf(buffer, 0, offset).getBytes());
            }
        } catch (IOException ioe) {
            throw new StreamException(ioe);
        }
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
        int offset;
        final byte[] buffer = new byte[WUIC_BUFFER_LEN];

        try {
            while ((offset = is.read(buffer)) != -1) {
                os.write(buffer, 0, offset);
            }
        } catch (IOException ioe) {
            throw new StreamException(ioe);
        }
    }

    /**
     * <p>
     * Indicates if the given {@code File} is a file on the file system. In addition to the {@code File#isFile()} method,
     * this method is able to scan ZIP archives like .jar and .zip files to determine if any entry inside it is a file or
     * not. The expected annotation is a '!' at the end of the archive file as it is represented when retrieving a resource
     * path from the classpath.
     * </p>
     *
     * <p>
     * For instance, if you have a file 'file.js' as an entry of a jar 'archive.jar', then
     * <pre>IOUtils.isFile(new File("/foo/archive.jar!/file.js"));</pre> will return {@code true}.
     * </p>
     *
     * @param file the file to analyze
     * @return {@code true} if the file is a file, even if it is an entry of an archive
     * @throws StreamException if an I/O error occurs while reading an arcgive
     */
    public static Boolean isFile(final File file) throws StreamException {
        // Always use '/' separator, even on windows
        final String absolutePath = normalizePathSeparator(file.getPath()).replace("file:/", "");
        final String[] tree = absolutePath.split(STD_SEPARATOR);

        // Iterate through the tree to check that parent paths are directories or archive file
        final StringBuilder sb = new StringBuilder();

        // Be careful, we don't test the last path in this loop
        for (int i = 0; i < tree.length - 1; i++) {
            sb.append(tree[i]);

            // Archive : check if the rest of the path of a file entry or not
            if (sb.toString().endsWith(".jar!") || sb.toString().endsWith(".zip!")) {
                try {
                    final String entryName = absolutePath.substring(sb.length() + 1);
                    return !new ZipFile(sb.substring(0, sb.length() - 1)).getEntry(entryName).isDirectory();
                } catch (IOException ioe) {
                    throw new StreamException(ioe);
                }
            // Parent is not an archive and not a directory : last path of not a file
            } else if (new File(sb.toString()).isFile()) {
                return Boolean.FALSE;
            } else {
                sb.append(STD_SEPARATOR);
            }
        }

        return new File(absolutePath).isFile();
    }

    /**
     * <p>
     * Returns all the files path which belong as children to the given path file. If the file is a directory, then
     * the result is obvious but the method also supports archive files. If the file name ends with .zip, .zip!, .jar or
     * .jar!, the file is read as ZIP archive and all its root path are returned.
     * </p>
     *
     * @param path the file
     * @return the children paths
     * @throws StreamException if any I/O error occurs while reading ZIP archive
     */
    public static String[] listPath(final File path) throws StreamException {
        if (path.isDirectory()) {
            return path.list();
        } else {
            // Always work with slash as path separator, even on windows
            final String absolutePath = normalizePathSeparator(path.getPath());
            int index;

            if ((index = absolutePath.indexOf(".jar")) != -1
                    || (index = absolutePath.indexOf(".jar!")) != -1
                    || (index = absolutePath.indexOf(".zip")) != -1
                    || (index = absolutePath.indexOf(".zip!")) != -1) {
                try {
                    // We need to read archive entries
                    final int end = absolutePath.indexOf(STD_SEPARATOR, index);
                    String zipPath = absolutePath.substring(0, end);

                    if (zipPath.endsWith("!")) {
                        zipPath = zipPath.substring(0, zipPath.length() - 1);
                    }

                    ZipFile archive = null;

                    try {
                        archive = new ZipFile(new File(new URI(zipPath)));
                        final Enumeration<? extends ZipEntry> entries = archive.entries();
                        final List<String> retval = new ArrayList<String>();
                        final String rootEntry = absolutePath.substring(end + 1).concat(STD_SEPARATOR);

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

                            if (entryName.startsWith(rootEntry) && !relativeEntry.isEmpty() && relativeEntry.split(STD_SEPARATOR).length == 1) {
                                retval.add(relativeEntry);
                            }
                        }

                        return retval.toArray(new String[retval.size()]);
                    } finally {
                        if (archive != null) {
                            archive.close();
                        }
                    }
                } catch (IOException ioe) {
                    throw new StreamException(ioe);
                } catch (URISyntaxException use) {
                    // Should never occur since we get the URI name with File#getPath() method
                    throw new BadArgumentException(new IllegalArgumentException(use));
                }
            } else {
                throw new BadArgumentException(new IllegalArgumentException(String.format("%s is not a directory or a ZIP archive", path.getAbsolutePath())));
            }
        }
    }

    /**
     * <p>
     * Checks if a given file which is relative to the specified start path is a resource matching the specified pattern.
     * </p>
     *
     * <p>
     * Supports research through directories and ZIP archives.
     * </p>
     *
     * @param startPath the path where research starts
     * @param relativeFile the file to test
     * @param pattern the pattern to match
     * @return the matching paths
     * @throws com.github.wuic.exception.wrapper.StreamException if any I/O error occurs while reading file
     */
    public static List<String> lookupFileResources(final String startPath, final String relativeFile, final Pattern pattern) throws StreamException {
        final String pathName = relativeFile.replace('\\', '/');
        final File absoluteFile = new File(startPath, relativeFile);

        if (isFile(absoluteFile)) {
            final Matcher matcher = pattern.matcher(pathName);

            if (matcher.find()) {
                return Arrays.asList(pathName);
            } else if (pathName.endsWith(".jar") || pathName.endsWith(".zip")) {
                try {
                    return lookupArchiveResources(new ZipFile(absoluteFile), pattern);
                } catch (IOException ioe) {
                    throw new StreamException(ioe);
                }
            } else {
                return Arrays.asList();
            }
        } else {
            return lookupDirectoryResources(startPath, relativeFile, pattern);
        }
    }

    /**
     * <p>
     * Looks up for an entry matching the given pattern in the specified zip file.
     * </p>
     *
     * @param zipFile the zip file to read
     * @param pattern the pattern to match
     * @return the matching paths
     */
    public static List<String> lookupArchiveResources(final ZipFile zipFile, final Pattern pattern) {
        final Enumeration<? extends ZipEntry> entries = zipFile.entries();
        final List<String> retval = new ArrayList<String>();

        while (entries.hasMoreElements()) {
            final ZipEntry entry = entries.nextElement();

            if (pattern.matcher(entry.getName()).matches()) {
                retval.add(entry.getName());
            }
        }

        return retval;
    }

    /**
     * <p>
     * Looks for a file with a name matching the given pattern if the specified directory.
     * </p>
     *
     * @param startPath the path where research starts
     * @param relativeDirectory the directory to test
     * @param pattern the pattern
     * @return the matching paths
     */
    public static List<String> lookupDirectoryResources(final String startPath, final String relativeDirectory, final Pattern pattern)
            throws StreamException {
        final File absoluteDirectory = new File(startPath, relativeDirectory);

        if (isFile(absoluteDirectory)) {
            throw new BadArgumentException(new IllegalArgumentException(String.format("%s must be a directory.", absoluteDirectory.getAbsolutePath())));
        } else {
            // The directory could also be an archive file
            final List<String> retval = new ArrayList<String>();
            final String[] paths = listPath(absoluteDirectory);

            for (String pathName : paths) {
                retval.addAll(lookupFileResources(startPath, StringUtils.merge(new String[] { relativeDirectory, pathName, }, "/"), pattern));
            }

            return retval;
        }
    }
}
