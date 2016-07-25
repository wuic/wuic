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


package com.github.wuic.util;

import com.github.wuic.nut.ConvertibleNut;
import com.github.wuic.nut.Nut;
import com.github.wuic.nut.Source;
import com.github.wuic.nut.SourceMapNut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.GZIPInputStream;

/**
 * <p>
 * This class provides utility methods around {@link Nut} management.
 * </p>
 *
 * @author Guillaume DROUET
 * @since 0.4.4
 */
public final class NutUtils {

    /**
     * Logger.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(NutUtils.class);

    /**
     * <p>
     * Prevent instantiation of this utility class.
     * </p>
     */
    private NutUtils() {
        // Nothing to do
    }

    /**
     * <p>
     * Finds a nut with a name matching the given path within a given source map.
     * </p>
     *
     * @param nut the nut that owns the source map
     * @param sourceMapNut the nut to test
     * @param nutName the nutName
     * @return the nut, {@code null} if not found
     */
    public static ConvertibleNut findInSourceMapByName(final ConvertibleNut nut, final SourceMapNut sourceMapNut, final String nutName) {
        final ConvertibleNut sourceMap = findByName(sourceMapNut, nutName);

        if (sourceMap != null) {
            return sourceMap;
        } else {

            // Search in original nuts
            for (final ConvertibleNut o : sourceMapNut.getOriginalNuts()) {

                // Avoid cycles
                if (!o.equals(nut)) {
                    final ConvertibleNut res = findByName(o, nutName);

                    if (res != null) {
                        return res;
                    }
                }
            }
        }

        return null;
    }

    /**
     * <p>
     * Finds a nut with a name matching the given path.
     * </p>
     *
     * @param nut the nut to test
     * @param nutName the nutName
     * @return the nut, {@code null} if not found
     */
    public static ConvertibleNut findByName(final ConvertibleNut nut, final String nutName) {
        String parsedName = StringUtils.simplifyPathWithDoubleDot(nut.getName());

        // Nut found: write the stream and return
        if (parsedName.equals(nutName)
                || ('/' + parsedName).equals(nutName)
                || parsedName.equals('/' + nutName)
                || IOUtils.mergePath("best-effort", nutName).equals(parsedName)) {
            return nut;
        } else if (!(nut instanceof Source) && (nut.getSource() instanceof SourceMapNut)) {
            // Source map
            final ConvertibleNut retval = findInSourceMapByName(nut, SourceMapNut.class.cast(nut.getSource()), nutName);

            if (retval != null) {
                return retval;
            }
        }

        if (nut.getReferencedNuts() != null) {
            // Find in referenced nuts
            final ConvertibleNut ref = findByName(nut.getReferencedNuts(), nutName);

            if (ref != null) {
                return ref;
            }
        }

        return null;
    }

    /**
     * <p>
     * Finds a nut with a name matching the given path.
     * </p>
     *
     * @param nuts the nuts to iterate to find
     * @param name the nut name
     * @return the nut, {@code null} if not found
     */
    public static ConvertibleNut findByName(final List<ConvertibleNut> nuts, final String name) {
        // Iterates the nuts to find the requested element
        for (final ConvertibleNut nut : nuts) {
            final ConvertibleNut retval = findByName(nut, name);

            if (retval != null) {
                return retval;
            }
        }

        return null;
    }

    /**
     * <p>
     * Gets the version number from the given nut.
     * </p>
     *
     * @param nut the nut
     * @return the version number string representation, 0 if can't retrieve it
     */
    public static Long getVersionNumber(final Nut nut) {
        return getVersionNumber(nut.getVersionNumber());
    }

    /**
     * <p>
     * Gets the number from the given future.
     * </p>
     *
     * @param future the future
     * @return the version number string representation, 0 if can't retrieve it
     */
    public static Long getVersionNumber(final Future<Long> future) {
        try {
            return future.get();
        } catch (ExecutionException ee) {
            LOGGER.error("Can't get the version number. Returning 0...", ee);
        } catch (InterruptedException ie) {
            LOGGER.error("Can't get the version number. Returning 0...", ie);
        }

        return 0L;
    }

    /**
     * <p>
     * Gets the version number from the given source.
     * </p>
     *
     * @param source the source nut
     * @return the version number string representation, 0 if can't retrieve it
     */
    public static Long getVersionNumber(final Source source) {
        return getVersionNumber(source.getOriginalNuts());
    }

    /**
     * <p>
     * Computes the version number based on version number retrieved from given nuts.
     * </p>
     *
     * @param nuts the nuts
     * @return the computed version number
     */
    public static Long getVersionNumber(final List<? extends Nut> nuts) {
        if (nuts.size() == 1) {
            return getVersionNumber(nuts.get(0));
        }

        final List<Long> versionNumbers = new ArrayList<Long>();

        // Collect all version number
        for (final Nut o : nuts) {
            versionNumbers.add(getVersionNumber(o));
        }

        // When a fixed version number is set, all version number should be the same
        // In that case, we want to apply the fixed version to the composition instead of digesting a new one
        if (new HashSet<Long>(versionNumbers).size() == 1) {
            return versionNumbers.get(0);
        }

        final MessageDigest md = IOUtils.newMessageDigest();

        for (final Long v : versionNumbers) {
            md.update(ByteBuffer.allocate(NumberUtils.HEIGHT).putLong(v).array());
        }

        return ByteBuffer.wrap(md.digest()).getLong();
    }

    /**
     * <p>
     * Reads the result of the transformation process configured on the {@link Nut}. If the {@link Nut} is compressed,
     * then the result if uncompressed to return a readable string.
     * </p>
     *
     * @param n the nut
     * @return the result of transformation
     * @throws IOException if transformation fails
     */
    public static String readTransform(final ConvertibleNut n) throws IOException {
        final AtomicReference<String> retval = new AtomicReference<String>();

        n.transform(new Pipe.OnReady() {
            @Override
            public void ready(final Pipe.Execution e) throws IOException {
                if (e.isText()) {
                    final CharArrayWriter writer = new CharArrayWriter();

                    try {
                        e.writeResultTo(writer);
                        retval.set(writer.toString());
                    } finally {
                        IOUtils.close(writer);
                    }
                } else {
                    final ByteArrayOutputStream bos = new ByteArrayOutputStream();

                    try {
                        e.writeResultTo(bos);
                        final byte[] b = bos.toByteArray();
                        retval.set(n.isCompressed() ?
                                IOUtils.readString(new InputStreamReader(new GZIPInputStream(new ByteArrayInputStream(b)))) : new String(b));
                    } finally {
                        IOUtils.close(bos);
                    }
                }
            }
        });

        return retval.get();
    }

    /**
     * <p>
     * Gets the common parent file of the given list of nuts.
     * </p>
     *
     * @param nuts the nuts
     * @return the common parent, {@code null} if a call to {@link com.github.wuic.nut.Nut#getParentFile()} returns {@code null}
     */
    public static String getParentFile(final List<? extends Nut> nuts) {
        final List<String> paths = new ArrayList<String>(nuts.size());

        for (final Nut nut : nuts) {

            // the parent file is null, unable to find common parent with other files
            if (nut.getParentFile() == null) {
                return null;
            } else {
                paths.add(nut.getParentFile());
            }
        }

        return StringUtils.computeCommonPathBeginning(paths);
    }
}
