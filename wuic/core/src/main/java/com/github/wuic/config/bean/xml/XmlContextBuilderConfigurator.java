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
import com.github.wuic.config.bean.json.BeanContextBuilderConfigurator;
import com.github.wuic.exception.WuicException;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.validation.SchemaFactory;
import java.net.URL;

/**
 * <p>
 * This configurator implements XML supports for WUIC. It abstracts the way the XML is read and unmarshal with JAXB.
 * </p>
 *
 * @author Guillaume DROUET
 * @since 0.4.0
 */
public abstract class XmlContextBuilderConfigurator extends BeanContextBuilderConfigurator {

    /**
     * To read wuic.xml content.
     */
    private Unmarshaller unmarshaller;

    /**
     * <p>
     * Builds a new instance.
     * </p>
     *
     * @param tag the tag
     * @param processContext the process context
     * @throws JAXBException if an context can't be initialized
     */
    public XmlContextBuilderConfigurator(final String tag, final ProcessContext processContext) throws JAXBException {
        this(Boolean.TRUE, tag, processContext);
    }

    /**
     * <p>
     * Creates a new instance.
     * </p>
     *
     * @param tag the tag
     * @param pc the process context
     * @param multiple {@code true} if multiple configurations with the same tag could be executed, {@code false} otherwise
     * @throws JAXBException if an context can't be initialized
     */
    public XmlContextBuilderConfigurator(final Boolean multiple, final String tag, final ProcessContext pc) throws JAXBException {
        super(multiple, tag, pc);
        final JAXBContext jc = JAXBContext.newInstance(WuicBean.class);
        unmarshaller = jc.createUnmarshaller();

        try {
            final URL xsd = getClass().getResource("/wuic.xsd");
            unmarshaller.setSchema(SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI).newSchema(xsd));
        } catch (SAXException se) {
            WuicException.throwBadArgumentException(se);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public WuicBean getWuicBean() throws WuicException {
        try {
            return unmarshal(unmarshaller);
        } catch (JAXBException je) {
            WuicException.throwBadStateException(je);
            return null;
        }
    }

    /**
     * <p>
     * Unmashal the {@link com.github.wuic.config.bean.WuicBean} with the given unmarhalled.
     * </p>
     *
     * @param unmarshaller the unmarshaller
     * @return the unmarshalled bean
     * @throws JAXBException if the XML can't be read
     */
    protected abstract WuicBean unmarshal(final Unmarshaller unmarshaller) throws JAXBException;
}
