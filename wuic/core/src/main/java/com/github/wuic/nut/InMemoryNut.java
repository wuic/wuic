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


package com.github.wuic.nut;

import com.github.wuic.NutType;
import com.github.wuic.util.FutureLong;
import com.github.wuic.util.IOUtils;
import com.github.wuic.util.InMemoryInput;
import com.github.wuic.util.InMemoryOutput;
import com.github.wuic.util.Input;
import com.github.wuic.util.NutUtils;
import com.github.wuic.util.Pipe;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * <p>
 * Represents an in-memory nut which can be created on the fly in any context.
 * </p>
 *
 * <p>
 * This class also aims to provide a fully {@link java.io.Serializable} {@link Nut}.
 * Since the {@link java.util.concurrent.Future} implementation which provides the version number could is not
 * always a {@link java.io.Serializable}, then the version number must be directly provided.
 * </p>
 * 
 * @author Guillaume DROUET
 * @since 0.2.0
 */
public class InMemoryNut extends PipedConvertibleNut implements Serializable, SizableNut {

    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 7883435600781576457L;

    /**
     * Indicates if the content is subject to change or not.
     */
    private boolean dynamicContent;

    /**
     * The bytes.
     */
    private byte[] byteArray;

    /**
     * The chars.
     */
    private char[] charArray;

    /**
     * <p>
     * Builds a new {@code Nut} transformed nut based on a specified byte array. Content will be static and call to
     * {@link #setByteArray(byte[])} will be avoided.
     * </p>
     *
     * @param bytes the byte array
     * @param name the nut name
     * @param nt the {@link NutType}
     * @param version the version number
     */
    public InMemoryNut(final byte[] bytes,
                       final String name,
                       final NutType nt,
                       final Source source,
                       final Long version) {
        super(name, nt, new FutureLong(version), Boolean.FALSE);
        dynamicContent = false;
        setSource(source);
        setByteArray(bytes, false);
    }

    /**
     * <p>
     * Builds a new {@code Nut} transformed nut based on a specified char array. Content will be static and call to
     * {@link #setCharArray(char[], boolean)} will be avoided.
     * </p>
     *
     * @param chars the char array
     * @param name the nut name
     * @param nt the {@link NutType}
     * @param version the version number
     */
    public InMemoryNut(final char[] chars,
                       final String name,
                       final NutType nt,
                       final Source source,
                       final Long version) {
        super(name, nt, new FutureLong(version), Boolean.FALSE);
        dynamicContent = false;
        setSource(source);
        setCharArray(chars, false);
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
     * @param dynamicContent {@code true} if you will call {@link #setByteArray(byte[])} in the future
     */
    public InMemoryNut(final byte[] bytes, final String name, final NutType nt, final Long version, final boolean dynamicContent) {
        super(name, nt, new FutureLong(version), Boolean.FALSE);
        this.dynamicContent = dynamicContent;
        setByteArray(bytes, false);
    }

    /**
     * <p>
     * Builds a new {@code Nut} original nut based on a given char array.
     * </p>
     *                    char
     * @param chars the char array
     * @param name the nut name
     * @param nt the {@link NutType}
     * @param version the version number
     * @param dynamicContent {@code true} if you will call {@link #setCharArray(char[], boolean)} in the future
     */
    public InMemoryNut(final char[] chars, final String name, final NutType nt, final Long version, final boolean dynamicContent) {
        super(name, nt, new FutureLong(version), Boolean.FALSE);
        this.dynamicContent = dynamicContent;
        setCharArray(chars, false);
    }

    /**
     * <p>
     * Builds a new {@link InMemoryNut}.
     * </p>
     *
     * @param e the execution providing char or byte stream
     * @param name the nut name
     * @param nutType the nut type
     * @param source the source, {@code isDynamic} is ignored if not {@code null}
     * @param versionNumber the version number
     * @param isDynamic if nut is dynamic or not, ignored if {@code source} is not {@code null}
     * @return the new nut
     */
    private static InMemoryNut newNut(final Pipe.Execution e,
                                       final String name,
                                       final NutType nutType,
                                       final Source source,
                                       final Long versionNumber,
                                       final boolean isDynamic) {
        if (source == null) {
            return e.isText() ? new InMemoryNut(e.getCharResult(), name, nutType, versionNumber, isDynamic)
                    : new InMemoryNut(e.getByteResult(), name, nutType, versionNumber, isDynamic);
        } else {
            return e.isText() ? new InMemoryNut(e.getCharResult(), name, nutType, source, versionNumber)
                    : new InMemoryNut(e.getByteResult(), name, nutType, source, versionNumber);
        }
    }

    /**
     * <p>
     * Converts the given nuts list and its referenced nuts into nuts wrapping an in memory byte array.
     * </p>
     *
     * @param nuts the nuts to convert
     * @return the byte array nut
     * @throws IOException if any I/O error occurs
     */
    public static List<ConvertibleNut> toByteArrayNut(final List<ConvertibleNut> nuts) throws IOException {
        final List<ConvertibleNut> retval = new ArrayList<ConvertibleNut>(nuts.size());

        for (final ConvertibleNut nut : nuts) {
            retval.add(toByteArrayNut(nut));
        }

        return retval;
    }

    /**
     * <p>
     * Converts the given nut and its referenced nuts into nuts wrapping an in memory byte array.
     * </p>
     *
     * @param nut the nut to convert
     * @return the byte array nut
     * @throws IOException if any I/O error occurs
     */
    public static ConvertibleNut toByteArrayNut(final ConvertibleNut nut) throws IOException {
        InputStream is = null;

        try {
            final InMemoryOutput os = new InMemoryOutput(nut.getNutType().getCharset());
            nut.transform(new Pipe.DefaultOnReady(os));

            final Pipe.Execution e = os.execution();
            final InMemoryNut bytes;
            final String name = nut.getName();

            // This is an original nut
            if (nut.getSource().getOriginalNuts().isEmpty()) {
                bytes = newNut(e, name, nut.getNutType(), null, NutUtils.getVersionNumber(nut), nut.isDynamic());
            } else {
                final Source s = nut.getSource();

                // Make sure to produce a serializable ource instance
                final Source src = s instanceof SourceMapNut ?
                        new StaticSourceMapNut(SourceMapNut.class.cast(s), nut.getNutType().getCharset()) : new SourceImpl();

                // Converts the sources to their byte array version
                for (final ConvertibleNut sourceNut : s.getOriginalNuts()) {
                    // Do nothing if source is already a byte array
                    if (!(sourceNut instanceof InMemoryNut)) {
                        // Convert the source to byte/char array
                        final Long v = NutUtils.getVersionNumber(sourceNut);
                        src.addOriginalNut(newNut(sourceNut.openStream().execution(), sourceNut.getName(), sourceNut.getNutType(), null, v, false));
                    } else {
                        src.addOriginalNut(sourceNut);
                    }
                }

                final Long version = NutUtils.getVersionNumber(src.getOriginalNuts());
                bytes = newNut(e, name, nut.getNutType(), src, version, false);
            }

            bytes.setIsCompressed(nut.isCompressed());
            bytes.setProxyUri(nut.getProxyUri());

            if (nut.getReferencedNuts() != null && nut.getReferencedNuts() != bytes.getReferencedNuts()) {
                for (final ConvertibleNut ref : nut.getReferencedNuts()) {
                    final List<ConvertibleNut> o = bytes.getSource().getOriginalNuts();
                    final int index = o == null ? -1 : o.indexOf(ref);

                    // If original is also a referenced nut (cases like image aggregation), its already transformed
                    if (index != -1)  {
                        bytes.addReferencedNut(o.get(index));
                    } else {
                        bytes.addReferencedNut(toByteArrayNut(ref));
                    }
                }
            }

            return new TransformedNut(bytes);
        } finally {
            IOUtils.close(is);
        }
    }

    /**
     * <p>
     * Sets the content.
     * </p>
     *
     * @param bytes the new content
     */
    public void setByteArray(final byte[] bytes) {
        setByteArray(bytes, true);
    }

    /**
     * <p>
     * Sets the content.
     * </p>
     *
     * @param chars the new content
     */
    public void setCharArray(final char[] chars) {
        setCharArray(chars, true);
    }

    /**
     * <p>
     * Gets an execution based on the content.
     * </p>
     *
     * @param charset the charset to use
     * @return the execution
     */
    public Pipe.Execution execution(final String charset) {
        return byteArray == null ? new Pipe.Execution(charArray, charset) : new Pipe.Execution(byteArray, charset);
    }

    /**
     * <p>
     * Sets the content.
     * </p>
     *
     * @param checkDynamic {@code true} if we make sure this nut is dynamic to allow this method call
     * @param chars the new content
     */
    private void setCharArray(final char[] chars, final boolean checkDynamic) {
        if (checkDynamic && !isDynamic()) {
            throw new IllegalStateException("isDynamic() returns false, which means that setByteArray(byte[]) can't be called.");
        }

        byteArray = null;
        charArray = Arrays.copyOf(chars, chars.length);
    }

    /**
     * <p>
     * Sets the content.
     * </p>
     *
     * @param checkDynamic {@code true} if we make sure this nut is dynamic to allow this method call
     * @param bytes the new content
     */
    private void setByteArray(final byte[] bytes, final boolean checkDynamic) {
        if (checkDynamic && !isDynamic()) {
            throw new IllegalStateException("isDynamic() returns false, which means that setByteArray(byte[]) can't be called.");
        }

        charArray = null;
        byteArray = Arrays.copyOf(bytes, bytes.length);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Input openStream() {
        return byteArray == null ?
                new InMemoryInput(charArray, getNutType().getCharset()) : new InMemoryInput(byteArray, getNutType().getCharset());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isDynamic() {
        return dynamicContent;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int size() {
        return byteArray == null ? charArray.length : byteArray.length;
    }
}
