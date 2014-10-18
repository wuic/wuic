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

import com.github.wuic.nut.ConvertibleNut;
import com.github.wuic.nut.Nut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.zip.GZIPInputStream;

/**
 * <p>
 * This class provides utility methods around {@link Nut} management.
 * </p>
 *
 * @author Guillaume DROUET
 * @version 1.0
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
     * Finds a nut with a name matching the given path.
     * </p>
     *
     * @param nut the nut to test
     * @param nutName the nutName
     * @return the nut, {@code null} if not found
     */
    public static ConvertibleNut findByName(final ConvertibleNut nut, final String nutName) {
        String parsedName = StringUtils.simplifyPathWithDoubleDot(nut.getName());

        // Nut found : write the stream and return
        if (parsedName.equals(nutName) || ("/" + parsedName).equals(nutName) || parsedName.equals("/" + nutName)) {
            return nut;
        } else if (nut.getReferencedNuts() != null) {
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
        try {
            return nut.getVersionNumber().get();
        } catch (ExecutionException ee) {
            LOGGER.error("Can't get the version number. Returning 0...", ee);
        } catch (InterruptedException ie) {
            LOGGER.error("Can't get the version number. Returning 0...", ie);
        }

        return 0L;
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

        final MessageDigest md = IOUtils.newMessageDigest();

        for (final Nut o : nuts) {
            md.update(ByteBuffer.allocate(NumberUtils.HEIGHT).putLong(getVersionNumber(o)).array());
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
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        n.transform(bos);
        final byte[] b = bos.toByteArray();

        return n.isCompressed() ?
                IOUtils.readString(new InputStreamReader(new GZIPInputStream(new ByteArrayInputStream(b)))) : new String(b);
    }
}
