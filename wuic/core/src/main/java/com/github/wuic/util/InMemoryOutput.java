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

import java.io.ByteArrayOutputStream;
import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;

/**
 * <p>
 * A data output that can be written in bytes or characters to internal arrays.
 * </p>
 *
 * @author Guillaume DROUET
 * @since 0.5.3
 */
public final class InMemoryOutput extends AbstractOutput {

    /**
     * An output stream for bytes.
     */
    ByteArrayOutputStream outputStream;

    /**
     * An output stream for chars.
     */
    CharArrayWriter writer;

    /**
     * <p>
     * Builds a new instance.
     * </p>
     *
     * @param charset the charset
     */
    public InMemoryOutput(final String charset) {
        super(charset);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() throws IOException {
        IOUtils.close(outputStream, writer);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected OutputStream internalOutputStream() {
        outputStream = new ByteArrayOutputStream();
        return outputStream;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Writer internalWriter() {
        writer = new CharArrayWriter();
        return writer;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Input input(final String charset) {
        if (!isWrite()) {
            WuicException.throwBadStateException(new IllegalStateException("Can't create an input from a not written output."));
        }

        return writer != null ? new InMemoryInput(writer.toCharArray(), charset) : new InMemoryInput(outputStream.toByteArray(), charset);
    }

    /**
     * <p>
     * Builds an execution from the data written to the wrapped writer or output stream.
     * </p>
     *
     * @return the execution based on written content
     */
    public Pipe.Execution execution() {
        if (!isWrite()) {
            WuicException.throwBadStateException(new IllegalStateException("Can't create an execution from a not written output."));
        }

        return writer != null ? new Pipe.Execution(writer.toCharArray(), getCharset()) : new Pipe.Execution(outputStream.toByteArray(), getCharset());
    }
}
