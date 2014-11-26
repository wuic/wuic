/*
 * "Copyright (c) 2014   Capgemini Technology Services (hereinafter "Capgemini")
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


package com.github.wuic.xml;

import com.github.wuic.exception.wrapper.StreamException;
import com.github.wuic.exception.xml.WuicXmlReadException;
import com.github.wuic.util.IOUtils;

import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

/**
 * <p>
 * Represents a configurator based on XML read from an {@link URL}. Polling is supported thanks to the modification date
 * provided by the {@link URL} object.
 * </p>
 *
 * @author Guillaume DROUET
 * @version 1.0
 * @since 0.4.2
 */
public class FileXmlContextBuilderConfigurator extends XmlContextBuilderConfigurator {

    /**
     * The {@link java.net.URL} pointing to the wuic.xml file.
     */
    private URL xmlFile;

    /**
     * <p>
     * Creates a new instance.
     * </p>
     *
     * @param wuicXml the wuic.xml file URL
     * @throws javax.xml.bind.JAXBException if an context can't be initialized
     */
    public FileXmlContextBuilderConfigurator(final URL wuicXml) throws JAXBException, WuicXmlReadException {
        if (wuicXml == null) {
            throw new WuicXmlReadException("XML configuration URL for WUIC is null", new IllegalArgumentException());
        }

        xmlFile = wuicXml;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getTag() {
        return xmlFile.toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Long getLastUpdateTimestampFor(final String path) throws StreamException {
        InputStream is = null;
        try {
            final URLConnection c = xmlFile.openConnection();
            is = c.getInputStream();
            return c.getLastModified();
        } catch (IOException ioe) {
            throw new StreamException(ioe);
        } finally {
            IOUtils.close(is);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected XmlWuicBean unmarshal(final Unmarshaller unmarshaller) throws JAXBException {
        return (XmlWuicBean) unmarshaller.unmarshal(xmlFile);
    }
}
