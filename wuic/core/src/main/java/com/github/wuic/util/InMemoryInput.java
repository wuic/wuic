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

import java.io.ByteArrayInputStream;
import java.io.CharArrayReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.Arrays;

/**
 * <p>
 * This {@code Input} provides streams from wrapped byte of char array.
 * </p>
 *
 * @author Guillaume DROUET
 * @since 0.5.3
 */
public class InMemoryInput extends AbstractInput {

    /**
     * The source as a char array.
     */
    private char[] chars;

    /**
     * The source as a byte array.
     */
    private byte[] bytes;

    /**
     * <p>
     * Builds a new instance from a particular output.
     * </p>
     *
     * @param charset the charset
     * @param output the output
     */
    public InMemoryInput(final InMemoryOutput output, final String charset) {
        super(charset);

        if (output.outputStream != null) {
            bytes = output.outputStream.toByteArray();
        }

        if (output.writer != null) {
            chars = output.writer.toCharArray();
        }
    }

    /**
     * <p>
     * Builds a new instance based on a byte array.
     * </p>
     *
     * @param charset the charset to encode/decode text stream
     * @param bytes the bytes
     */
    public InMemoryInput(final byte[] bytes, final String charset) {
        super(charset);
        this.bytes = Arrays.copyOf(bytes, bytes.length);
    }

    /**
     * <p>
     * Builds a new instance based on a char array.
     * </p>
     *
     * @param charset the charset to encode/decode text stream
     * @param chars the characters
     */
    public InMemoryInput(final char[] chars, final String charset) {
        super(charset);
        this.chars = Arrays.copyOf(chars, chars.length);
    }

    /**
     * <p>
     * Builds a new instance based on a {@code String}.
     * </p>
     *
     * @param str the string
     * @param charset the charset to encode/decode text stream
     */
    public InMemoryInput(final String str, final String charset) {
        this(str.toCharArray(), charset);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Pipe.Execution execution() throws IOException {
        if (isClosed()) {
            WuicException.throwBadStateException(new IllegalStateException("Input is closed."));
        }

        // Can't read reader or input stream
        if (isRead()) {
            WuicException.throwBadStateException(new IllegalStateException("Can't build an execution from a read input."));
        }

        try {
            // Arrays already available
            return bytes != null ? new Pipe.Execution(bytes, getCharset()) : new Pipe.Execution(chars, getCharset());
        } finally {
            close();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public InputStream internalInputStream(final String charset) throws IOException {
        if (bytes == null) {
            return new ByteArrayInputStream(IOUtils.toBytes(Charset.forName(charset), chars));
        } else {
            return new ByteArrayInputStream(bytes);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Reader internalReader(final String charset) throws IOException {
        return chars == null ? new InputStreamReader(new ByteArrayInputStream(bytes), charset) : new CharArrayReader(chars);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isSourceAsByte() {
        return bytes != null;
    }
}
