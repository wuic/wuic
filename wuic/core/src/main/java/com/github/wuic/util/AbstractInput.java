/*
 * Copyright (c) 2016   The authors of WUIC
 *
 * License/Terms of Use
 * Permission is hereby granted, free of charge and for the term of intellectual
 * property rights on the Software, to any person obtaining a copy of this software
 * and associated documentation files (the "Software"), to use, copy, modify and
 * propagate free of charge, anywhere in the world, all or part of the Software
 * subject to the following mandatory conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, PEACEFUL ENJOYMENT,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS
 * OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */


package com.github.wuic.util;

import com.github.wuic.exception.WuicException;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

/**
 * <p>
 * A base implementation for {@link Input} that checks streams are not already opened, avoiding to perform multiple
 * stream openings.
 * </p>
 *
 * @author Guillaume DROUET
 * @since 0.5.3
 */
public abstract class AbstractInput implements Input {

    /**
     * {@code true} if this source is read as text, {@code false} as bytes, {@code null} if not read.
     */
    private Boolean text;

    /**
     * The charset.
     */
    private String charset;

    /**
     * Stream currently opened.
     */
    private Closeable current;

    /**
     * <p>
     * Builds a new instance.
     * </p>
     *
     * @param charset the charset to encode/decode text
     */
    protected AbstractInput(final String charset) {
        this.charset = charset;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getCharset() {
        return charset;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final InputStream inputStream() throws IOException {
        if (isClosed()) {
            WuicException.throwBadStateException(new IllegalStateException("Input is closed."));
        }

        if (isRead()) {
            WuicException.throwBadStateException(new IllegalStateException("Can't read the same stream twice."));
        }

        text = Boolean.FALSE;
        current = internalInputStream(charset);
        return InputStream.class.cast(current);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final Reader reader() throws IOException {
        if (isClosed()) {
            WuicException.throwBadStateException(new IllegalStateException("Input is closed."));
        }

        if (isRead()) {
            WuicException.throwBadStateException(new IllegalStateException("Can't read the same stream twice."));
        }

        text = Boolean.TRUE;
        current = internalReader(charset);
        return Reader.class.cast(current);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void close() throws IOException {
        IOUtils.close(current);
        current = null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isReadAsByte() {
        return isRead() && !text;
    }

    /**
     * <p>
     * Indicates if this input is closed.
     * </p>
     *
     * @return {@code true} if closed, {@code false} otherwise
     */
    public final boolean isClosed() {
        return isRead() && current == null;
    }

    /**
     * <p>
     * Indicates if this {@code Input} has been already read.
     * The {@code Input} is read once {@link #inputStream()} or {@link #reader()} has been called.
     * </p>
     *
     * @return {@code true} if the instance is read, {@code false} otherwise
     */
    public final boolean isRead() {
        return text != null;
    }

    /**
     * <p>
     * Indicates if the input has been read as text or bytes.
     * </p>
     *
     * @return {@code true} if this source is read as text, {@code false} as bytes, {@code null} if not read.
     */
    public final Boolean isText() {
        return text;
    }

    /**
     * <p>
     * Opens an {@code InputStream}.
     * </p>
     *
     * @param charset the charset that should be used if needed
     * @return the {@code InputStream}
     * @throws IOException if any I/O error occurs
     */
    protected abstract InputStream internalInputStream(String charset) throws IOException;

    /**
     * <p>
     * Opens an {@code Reader}.
     * </p>
     *
     * @param charset the charset that should be used if needed
     * @return the {@code Reader}
     * @throws IOException if any I/O error occurs
     */
    protected abstract Reader internalReader(String charset) throws IOException;
}
