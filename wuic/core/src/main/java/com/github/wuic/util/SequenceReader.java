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

import java.io.IOException;
import java.io.Reader;
import java.util.Iterator;
import java.util.List;

/**
 * <p>
 * The {@code SequenceReader} is inspired from the {@link java.io.SequenceInputStream} class to represent a logical
 * concatenation of {@link java.io.Reader readers}.
 * </p>
 *
 * @author Guillaume DROUET
 * @since 0.5.3
 */
public class SequenceReader extends Reader {

    /**
     * The concatenated readers.
     */
    private Iterator<? extends Reader> e;

    /**
     * The current reader.
     */
    private Reader in;

    /**
     * <p>
     * Creates a new instance with a list of {@link Reader readers}. The readers that are produced by the list will be
     * read, in order, to provide the characters to be read from this {@code SequenceReader}. After each reader from the
     * list is exhausted, it is closed by calling its {@code close} method.
     * </p>
     *
     * @param e an enumeration of input streams.
     */
    public SequenceReader(final List<? extends Reader> e) {
        this.e = e.iterator();
        try {
            nextStream();
        } catch (IOException ex) {
            // This should never happen
            throw new Error("panic");
        }
    }

    /**
     *  Continues reading in the next reader if an EOF is reached.
     */
    private final void nextStream() throws IOException {
        if (in != null) {
            in.close();
        }

        if (e.hasNext()) {
            in = e.next();
            if (in == null)
                throw new NullPointerException();
        }
        else in = null;

    }

    /**
     * <p>
     * Reads the next char of data from this reader. The char is returned as an <code>int</code> in the range {@code 0}
     * to {@code 255}. If no char is available because the end of the stream has been reached, the value {@code -1} is
     * returned. This method blocks until input data is available, the end of the stream is detected, or an exception is
     * thrown.
     * </p>
     *
     * <p>
     * This method tries to read one character from the current substream. If it reaches the end of the stream, it calls
     * the {@code close} method of the current substream and begins reading from the next substream.
     *
     * @return the next byte of data, or {@code -1} if the end of the stream is reached
     * @throws IOException  if an I/O error occurs
     */
    @Override
    public int read() throws IOException {
        while (in != null) {
            int c = in.read();
            if (c != -1) {
                return c;
            }
            nextStream();
        }
        return -1;
    }

    /**
     * <p>
     * Reads up to {@code len} characters of data from this reader into an array of chars. If {@code len} is not zero,
     * the method blocks until at least 1 character of input is available; otherwise, no characters are read and {@code 0}
     * is returned.
     * </p>
     * 
     * <p>
     * The {@code read} method of {@code SequenceReader} tries to read the data from the current substream. If it fails
     * to read any characters because the substream has reached the end of the stream, it calls the {@code close} method
     * of the current substream and begins reading from the next substream.
     *
     * @param c the buffer into which the data is read
     * @param off the start offset in array {@code c} at which the data is written
     * @param len the maximum number of chars read
     * @return int the number of chars read
     * @throws IOException if an I/O error occurs
     */
    @Override
    public int read(char c[], int off, int len) throws IOException {
        if (in == null) {
            return -1;
        } else if (c == null) {
            throw new NullPointerException();
        } else if (off < 0 || len < 0 || len > c.length - off) {
            throw new IndexOutOfBoundsException();
        } else if (len == 0) {
            return 0;
        }
        do {
            int n = in.read(c, off, len);
            if (n > 0) {
                return n;
            }
            nextStream();
        } while (in != null);
        return -1;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() throws IOException {
        do {
            nextStream();
        } while (in != null);
    }
}

