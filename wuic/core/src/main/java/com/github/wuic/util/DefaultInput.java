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

import com.github.wuic.exception.WuicException;

import java.io.ByteArrayInputStream;
import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;

/**
 * <p>
 * A source of data represented with bytes or characters. An instance is supposed to be read only once because the
 * underlying {@link java.io.Reader} or {@link java.io.InputStream} possibly can't be reset.
 * </p>
 *
 * @author Guillaume DROUET
 * @since 0.5.3
 */
public class DefaultInput extends AbstractInput {

    /**
     * The input as an input stream.
     */
    private InputStream inputStream;

    /**
     * The source as a reader.
     */
    private Reader reader;

    /**
     * <p>
     * Builds a new instance with a byte stream.
     * </p>
     *
     * @param charset the charset for encoding/decoding text stream
     * @param inputStream the input stream
     */
    public DefaultInput(final InputStream inputStream, final String charset) {
        super(charset);
        this.inputStream = inputStream;
    }

    /**
     * <p>
     * Builds a new instance with a char stream.
     * </p>
     *
     * @param charset the charset for encoding/decoding text stream
     * @param reader the reader
     */
    public DefaultInput(final Reader reader, final String charset) {
        super(charset);
        this.reader = reader;
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
            // Need to read stream data
            final Output output = new InMemoryOutput(getCharset());

            if (reader != null) {
                IOUtils.copyStream(reader, output.writer());
            } else {
                IOUtils.copyStream(inputStream, output.outputStream());
            }

            return output.execution();
        } finally {
            IOUtils.close(this);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public InputStream internalInputStream(final String charset) throws IOException {
        if (inputStream == null) {
            try {
                final CharArrayWriter caw = new CharArrayWriter();
                IOUtils.copyStream(reader, caw);
                return new ByteArrayInputStream(IOUtils.toBytes(Charset.forName(charset), caw.toCharArray()));
            } finally {
                IOUtils.close(reader);
            }
        }

        return inputStream;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Reader internalReader(final String charset) throws IOException {
        return reader == null ? new InputStreamReader(inputStream, charset) : reader;
    }
}
