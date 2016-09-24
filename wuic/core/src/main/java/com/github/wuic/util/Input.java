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

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

/**
 * <p>
 * A source of data represented with bytes or characters. An instance is supposed to be read only once because the
 * underlying {@link java.io.Reader} or {@link java.io.InputStream} possibly can't be reset.
 * </p>
 *
 * @author Guillaume DROUET
 * @since 0.5.3
 */
public interface Input extends Closeable {

    /**
     * <p>
     * Build an execution based on the read content. Methods {@link #inputStream()} and {@link #reader()}
     * must not been already called when this method is invoked. This method should close the underlying streams.
     * </p>
     *
     * @return the execution
     * @throws java.io.IOException if content can't be read
     */
    Pipe.Execution execution() throws IOException;

    /**
     * <p>
     * Read this source as byte stream.
     * </p>
     *
     * @return the input stream
     * @throws java.io.IOException if any I/O error occurs
     */
    InputStream inputStream() throws IOException;

    /**
     * <p>
     * Read this source as character stream.
     * </p>
     *
     * @return the reader
     * @throws java.io.IOException if any I/O error occurs
     */
    Reader reader() throws IOException;

    /**
     * <p>
     * Gets the charset used for encoding/decoding charset.
     * </p>
     *
     * @return the charset
     */
    String getCharset();

    /**
     * <p>
     * Indicates if the input has been read as a byte stream by calling {@link #inputStream()} or as a char stream by
     * calling {@link #reader()}. This method will return {@code false} if the input has not been read.
     * </p>
     *
     * @return {@code true} if {@link #inputStream()} has been called, {@code false} otherwise
     */
    boolean isReadAsByte();

    /**
     * <p>
     * Indicates if the input source is a byte stream a char stream.
     * </p>
     *
     * @return {@code true} if input source is a byte stream, {@code false} if it's a char stream
     */
    boolean isSourceAsByte();
}
