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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;

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
 * @version 1.1
 * @since 0.3.1
 */
public final class IOUtils {

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
        int offset = -1;

        // read content
        while ((offset = reader.read(buff)) != -1) {
            builder.append(buff, 0, offset);
        }

        return builder.toString();
    }

    /**
     * <p>
     * Copies the data from the given reader to the given output stream.
     * </p>
     *
     * @param reader the {@code Reader}
     * @param output the {@code OutputStream}
     * @throws IOException in an I/O error occurs
     */
    public static void copyReaderToStream(final Reader reader, final OutputStream output)
            throws IOException {
        int offset = -1;
        final char[] buffer = new char[WUIC_BUFFER_LEN];

        while ((offset = reader.read(buffer)) != -1) {
            output.write(String.copyValueOf(buffer, 0, offset).getBytes());
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
     * @throws IOException in an I/O error occurs
     */
    public static void copyStreamToWriter(final InputStream is, final Writer writer, final String cs)
            throws IOException {
        int offset = -1;
        final byte[] buffer = new byte[WUIC_BUFFER_LEN];

        while ((offset = is.read(buffer)) != -1) {
            writer.write(new String(Arrays.copyOf(buffer, offset), cs));
        }
    }

    /**
     * <p>
     * Copies the data from the given input stream into the given output stream.
     * </p>
     *
     * @param is the {@code InputStream}
     * @param os the {@code OutputStream}
     * @throws IOException in an I/O error occurs
     */
    public static void copyStream(final InputStream is, final OutputStream os)
            throws IOException {
        int offset = -1;
        final byte[] buffer = new byte[WUIC_BUFFER_LEN];

        while ((offset = is.read(buffer)) != -1) {
            os.write(buffer, 0, offset);
        }
    }
    
    /**
     * <p>
     * Checks if a given file could be or contains a resource matching the specified pattern.
     * </p>
     *
     * <p>
     * Supports research through directory and ZIP archives.
     * </p>
     *
     * @param file the file to check
     * @param pattern the pattern to match
     * @return the matching paths
     * @throws IOException if any I/O error occurs while reading file
     */
    public static List<String> lookupFileResources(final File file, final Pattern pattern) throws IOException {
        final String pathName = file.getAbsolutePath().replace('\\', '/');

        if (file.isFile()) {
            final Matcher matcher = pattern.matcher(pathName);

            if (matcher.find()) {
                return Arrays.asList(matcher.group());
            } else if (pathName.endsWith(".jar") || pathName.endsWith(".zip")) {
                return lookupArchiveResources(new ZipFile(file), pattern);
            } else {
                return Arrays.asList();
            }
        } else {
            return lookupDirectoryResources(file, pattern);
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
     * @throws IOException if any I/O error occurs while reading the archive
     */
    public static List<String> lookupArchiveResources(final ZipFile zipFile, final Pattern pattern) throws IOException {
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
     * Looks up a file with a name matching the given pattern if the specified directory.
     * </p>
     *
     * @param directory the directory
     * @param pattern the pattern
     * @return the matching paths
     * @throws IOException if the given file is not a directory
     */
    public static List<String> lookupDirectoryResources(final File directory, final Pattern pattern) throws IOException {
        if (!directory.isDirectory()) {
            throw new IOException(new IllegalArgumentException(directory.getAbsolutePath() + " must be a directory."));
        } else {
            final List<String> retval = new ArrayList<String>();

            for (String pathName : directory.list()) {
                retval.addAll(lookupFileResources(new File(directory, pathName), pattern));
            }

            return retval;
        }
    }
}
