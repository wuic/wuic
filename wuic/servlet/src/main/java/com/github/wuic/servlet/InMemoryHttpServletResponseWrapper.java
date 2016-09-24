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


package com.github.wuic.servlet;

import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * <p>
 * Wraps an {@link HttpServletResponse} to capture the written stream.
 * </p>
 *
 * @author Guillaume DROUET
 * @since 0.4.4
 */
public class InMemoryHttpServletResponseWrapper extends OkHttpServletResponseWrapper {

    /**
     * <p>
     * Extends {@link ServletOutputStream} and stores the stream in a wrapped byte array.
     * </p>
     *
     * @author Guillaume DROUET
     * @since 0.4.4
     */
    private final class ByteArrayServletStream extends ServletOutputStream {

        /**
         * The byte array.
         */
        private ByteArrayOutputStream baos;

        /**
         * <p>
         * Builds a new instance.
         * </p>
         *
         * @param baos the wrapped byte array
         */
        private ByteArrayServletStream(final ByteArrayOutputStream baos) {
            this.baos = baos;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void write(final int param) throws IOException {
            baos.write(param);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isReady() {
            return false;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void setWriteListener(final WriteListener writeListener) {
        }
    }

    /**
     * Wrapped byte array.
     */
    private ByteArrayOutputStream baos = new ByteArrayOutputStream();

    /**
     * Wrapped char array.
     */
    private final CharArrayWriter charArrayWriter = new CharArrayWriter();

    /**
     * Print writer built on top of char array.
     */
    private PrintWriter pw;

    /**
     * Servlet output stream built on top of byte array.
     */
    private ServletOutputStream sos;

    /**
     * <p>
     * Builds a new instance.
     * </p>
     */
    public InMemoryHttpServletResponseWrapper() {
    }

    /**
     * <p>
     * Builds a new instance.
     * </p>
     *
     * @param httpServletResponse a response to wrap
     */
    public InMemoryHttpServletResponseWrapper(final HttpServletResponse httpServletResponse) {
        super(httpServletResponse);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ServletOutputStream getOutputStream() throws IOException {
        if (pw != null) {
            throw new IllegalStateException("getWriter() already called!");
        }

        if (sos == null) {
            sos = new ByteArrayServletStream(baos);
        }

        return sos;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PrintWriter getWriter() throws IOException {
        if (sos != null) {
            throw new IllegalStateException("getOutputStream() already called!");
        }

        if (pw == null) {
            pw = new PrintWriter(charArrayWriter);
        }

        return pw;
    }

    /**
     * <p>
     * Gets the byte array.
     * </p>
     *
     * @return the byte array, {@code null} if nothing has been written to {@link #getOutputStream()}
     */
    public byte[] toByteArray() {
        return baos != null ? baos.toByteArray() : null;
    }

    /**
     * <p>
     * Gets the char array.
     * </p>
     *
     * @return the char array, {@code null} if nothing has been written to {@link #getWriter()}
     */
    public char[] toCharArray() {
        return pw != null ? charArrayWriter.toCharArray() : null;
    }
}
