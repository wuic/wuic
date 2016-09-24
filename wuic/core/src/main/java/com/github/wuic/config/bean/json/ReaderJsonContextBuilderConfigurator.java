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


package com.github.wuic.config.bean.json;

import com.github.wuic.config.bean.WuicBean;
import com.github.wuic.exception.WuicException;
import com.google.gson.Gson;

import java.io.IOException;
import java.io.Reader;

/**
 * <p>
 * Represents a configurator based on JSON read from a {@link java.io.Reader}. Polling is not supported by this kind of
 * configurator.
 * </p>
 *
 * @author Guillaume DROUET
 * @since 0.5.3
 */
public abstract class ReaderJsonContextBuilderConfigurator extends JsonContextBuilderConfigurator {

    /**
     * <p>
     * Creates a new instance.
     * </p>
     *
     * @param tag the tag
     */
    public ReaderJsonContextBuilderConfigurator(final String tag) {
        super(tag, null);
    }

    /**
     * <p>
     * Gets the reader.
     * </p>
     *
     * @return the reader
     * @throws IOException if any I/O error occurs
     */
    protected abstract Reader getReader() throws IOException;

    /**
     * {@inheritDoc}
     */
    @Override
    protected Long getLastUpdateTimestampFor(final String path) throws IOException {
        return -1L;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected WuicBean unmarshal(final Gson unmarshaller) throws IOException  {
        return unmarshaller.fromJson(getReader(), WuicBean.class);
    }

    /**
     * <p>
     * A simple {@link ReaderJsonContextBuilderConfigurator} wrapping a {@code Reader}.
     * </p>
     *
     * @author Guillaume DROUET
     * @since 0.5.3
     */
    public static class Simple extends ReaderJsonContextBuilderConfigurator {

        /**
         * The {@link  Reader} pointing to the JSON stream.
         */
        private Reader reader;

        /**
         * <p>
         * Creates a new instance.
         * </p>
         *
         * @param r the reader to JSON
         * @param tag the tag
         */
        public Simple(final Reader r, final String tag) {
            super(tag);

            if (r == null) {
                WuicException.throwWuicXmlReadException(new IllegalArgumentException("XML configuration reader for WUIC is null"));
            }

            reader = r;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected Reader getReader() throws IOException {
            return reader;
        }
    }
}
