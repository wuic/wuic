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


package com.github.wuic.nut.core;

import com.github.wuic.NutType;
import com.github.wuic.exception.NutNotFoundException;
import com.github.wuic.exception.wrapper.StreamException;
import com.github.wuic.nut.AbstractNut;
import com.github.wuic.nut.Nut;
import com.github.wuic.util.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * <p>
 * Represents an in-memory nut which can be created on the fly in any context.
 * </p>
 * 
 * @author Guillaume DROUET
 * @version 1.6
 * @since 0.2.0
 */
public class ByteArrayNut extends AbstractNut {

    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 7883435600781576457L;

    /**
     * The bytes.
     */
    private byte[] byteArray;
    
    /**
     * <p>
     * Builds a new {@code Nut} transformed nut based on a given byte array.
     * </p>
     * 
     * @param bytes the byte array
     * @param name the nut name
     * @param nt the {@link NutType}
     * @param originalNuts the original nuts
     */
    public ByteArrayNut(final byte[] bytes, final String name, final NutType nt, final List<Nut> originalNuts) {
        super(name, nt, Boolean.TRUE, Boolean.TRUE, Boolean.TRUE, Boolean.TRUE, originalNuts);
        byteArray = Arrays.copyOf(bytes, bytes.length);
    }

    /**
     * <p>
     * Builds a new {@code Nut} transformed nut based on a {@code null} byte array.
     * </p>
     *
     * @param name the nut name
     * @param nt the {@link NutType}
     * @param originalNuts the original nuts
     */
    public ByteArrayNut(final String name, final NutType nt, final List<Nut> originalNuts) {
        super(name, nt, Boolean.TRUE, Boolean.TRUE, Boolean.TRUE, Boolean.TRUE, originalNuts);
        byteArray = null;
    }

    /**
     * <p>
     * Builds a new {@code Nut} original nut based on a given byte array.
     * </p>
     *
     * @param bytes the byte array
     * @param name the nut name
     * @param nt the {@link NutType}
     * @param version the version number
     */
    public ByteArrayNut(final byte[] bytes, final String name, final NutType nt, final BigInteger version) {
        super(name, nt, Boolean.TRUE, Boolean.TRUE, Boolean.TRUE, Boolean.TRUE, version);
        byteArray = Arrays.copyOf(bytes, bytes.length);
    }

    /**
     * <p>
     * Builds a copy of given original {@code Nut} and puts its content into a memory byte array.
     * </p>
     *
     * @param nut the original nut
     * @throws NutNotFoundException if original {@link Nut} is wrongly built
     * @throws StreamException if content fails to be copied
     */
    public ByteArrayNut(final Nut nut) throws NutNotFoundException, StreamException {
        super(nut);

        InputStream is = null;

        try {
            is = nut.openStream();
            final ByteArrayOutputStream os = new ByteArrayOutputStream();
            IOUtils.copyStream(is, os);
            setBytes(os.toByteArray());
        } finally {
            IOUtils.close(is);
        }
    }

    /**
     * <p>
     * Converts the given nuts list and its referenced nuts into nuts wrapping an in memory byte array.
     * </p>
     *
     * @return the byte array nut
     * @throws StreamException if an I/O error occurs
     * @throws NutNotFoundException if given nut not normally created
     */
    public static List<Nut> toByteArrayNut(final List<Nut> nuts) throws StreamException, NutNotFoundException {
        final List<Nut> retval = new ArrayList<Nut>(nuts.size());

        for (final Nut nut : nuts) {
            retval.add(toByteArrayNut(nut));
        }

        return retval;
    }

    /**
     * <p>
     * Converts the given nut and its referenced nuts into nuts wrapping an in memory byte array.
     * </p>
     *
     * @return the byte array nut
     * @throws StreamException if an I/O error occurs
     * @throws NutNotFoundException if given nut not normally created
     */
    public static Nut toByteArrayNut(final Nut nut) throws StreamException, NutNotFoundException {
        InputStream is = null;

        try {
            is = nut.openStream();
            final ByteArrayOutputStream os = new ByteArrayOutputStream();
            IOUtils.copyStream(is, os);
            final Nut bytes;

            // This is an original nut
            if (nut.getOriginalNuts() == null) {
                bytes = new ByteArrayNut(nut);
            } else {
                bytes = new ByteArrayNut(os.toByteArray(), nut.getName(), nut.getNutType(), toByteArrayNut(nut.getOriginalNuts()));
            }

            bytes.setProxyUri(nut.getProxyUri());

            if (nut.getReferencedNuts() != null) {
                for (final Nut ref : nut.getReferencedNuts()) {
                    bytes.addReferencedNut(toByteArrayNut(ref));
                }
            }

            return bytes;
        } finally {
            IOUtils.close(is);
        }
    }

    /**
     * <p>
     * Sets the given array as byte array content.
     * </p>
     *
     * @param bytes the bytes
     */
    public void setBytes(final byte[] bytes) {
        byteArray = Arrays.copyOf(bytes, bytes.length);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public InputStream openStream() throws NutNotFoundException {
        return new ByteArrayInputStream(byteArray);
    }
}
