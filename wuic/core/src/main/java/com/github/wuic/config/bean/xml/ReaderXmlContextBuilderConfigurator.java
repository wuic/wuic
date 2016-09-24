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


package com.github.wuic.config.bean.xml;

import com.github.wuic.ProcessContext;
import com.github.wuic.config.bean.WuicBean;
import com.github.wuic.exception.WuicException;

import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.IOException;
import java.io.Reader;

/**
 * <p>
 * Represents a configurator based on XML read from a {@link Reader}. Polling is not supported by this kind of
 * configurator.
 * </p>
 *
 * @author Guillaume DROUET
 * @since 0.4.2
 */
public abstract class ReaderXmlContextBuilderConfigurator extends XmlContextBuilderConfigurator {

    /**
     * <p>
     * Creates a new instance.
     * </p>
     *
     * @param tag the tag
     * @param multiple {@code true} if multiple configurations with the same tag could be executed, {@code false} otherwise
     * @param processContext the process context
     * @throws JAXBException if an context can't be initialized
     */
    public ReaderXmlContextBuilderConfigurator(final String tag,
                                               final Boolean multiple,
                                               final ProcessContext processContext)
            throws JAXBException {
        super(multiple, tag, processContext);
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
    protected final WuicBean unmarshal(final Unmarshaller unmarshaller) throws JAXBException {
        try {
            return (WuicBean) unmarshaller.unmarshal(getReader());
        } catch (IOException ioe) {
            throw new JAXBException(ioe);
        }
    }

    /**
     * <p>
     * A simple {@link ReaderXmlContextBuilderConfigurator} wrapping a {@code Reader}.
     * </p>
     *
     * @author Guillaume DROUET
     * @since 0.5.3
     */
    public static class Simple extends ReaderXmlContextBuilderConfigurator {

        /**
         * The {@link  Reader} pointing to the XML stream.
         */
        private Reader reader;

        /**
         * <p>
         * Builds a new instance.
         * </p>
         *
         * @param r the reader
         * @param tag the tag
         * @param multiple {@code true} if multiple configurations with the same tag could be executed, {@code false} otherwise
         * @param processContext the process context
         * @throws JAXBException if an context can't be initialized
         */
        public Simple(final Reader r, final String tag, final Boolean multiple, final ProcessContext processContext) throws JAXBException {
            super(tag, multiple, processContext);

            if (r == null) {
                WuicException.throwWuicXmlReadException(new IllegalArgumentException("XML configuration reader for WUIC is null"));
            }

            reader = r;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected Reader getReader() {
            return reader;
        }
    }

}
