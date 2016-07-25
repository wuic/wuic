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
}
