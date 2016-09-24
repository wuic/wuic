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

import java.io.OutputStream;
import java.io.Writer;

/**
 * <p>
 * A base implementation for {@link Output} that checks streams are not already written, avoiding to perform multiple
 * stream openings.
 * </p>
 *
 * @author Guillaume DROUET
 * @since 0.5.3
 */
public abstract class AbstractOutput implements Output {

    /**
     * Already written or not.
     */
    private boolean write;

    /**
     * The charset.
     */
    private String charset;

    /**
     * <p>
     * Builds a new instance.
     * </p>
     */
    public AbstractOutput(final String charset) {
        this.write = false;
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
    public final OutputStream outputStream() {
        if (write) {
            WuicException.throwBadStateException(new IllegalStateException("Can't write to the same stream twice."));
        }

        write = true;
        return internalOutputStream();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final Writer writer() {
        if (write) {
            WuicException.throwBadStateException(new IllegalStateException("Can't write to the same stream twice."));
        }

        write = true;
        return internalWriter();
    }

    /**
     * <p>
     * Indicates if this {@code Output} has been already read.
     * The {@code Output} is read once {@link #outputStream()} or {@link #writer()} has been called.
     * </p>
     *
     * @return {@code true} if the instance is written, {@code false} otherwise
     */
    public final boolean isWrite() {
        return write;
    }

    /**
     * <p>
     * Opens the output stream for chars.
     * </p>
     *
     * @return the output stream
     */
    protected abstract Writer internalWriter();

    /**
     * <p>
     * Opens the output stream for chars.
     * </p>
     *
     * @return the output stream
     */
    protected abstract OutputStream internalOutputStream();
}