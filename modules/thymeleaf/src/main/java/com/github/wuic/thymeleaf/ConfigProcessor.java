
/*
 * "Copyright (c) 2013   Capgemini Technology Services (hereinafter "Capgemini")
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


package com.github.wuic.thymeleaf;

import com.github.wuic.WuicFacade;
import com.github.wuic.exception.WuicException;
import com.github.wuic.exception.wrapper.BadArgumentException;
import com.github.wuic.jee.WuicJeeContext;
import com.github.wuic.xml.ReaderXmlContextBuilderConfigurator;
import org.thymeleaf.Arguments;
import org.thymeleaf.dom.Element;
import org.thymeleaf.processor.element.AbstractRemovalElementProcessor;
import org.thymeleaf.util.DOMUtils;

import javax.xml.bind.JAXBException;
import java.io.Reader;
import java.io.StringReader;

/**
 * <p>
 * This processor evaluates the XML configuration described in the first element child and injects it to the global
 * configuration.
 * </p>
 *
 * @author Guillaume DROUET
 * @version 1.1
 * @since 0.4.2
 */
public class ConfigProcessor extends AbstractRemovalElementProcessor {

    /**
     * <p>
     * Builds a new instance.
     * </p>
     */
    public ConfigProcessor() {
        super("config");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean getRemoveElementAndChildren(final Arguments arguments, final Element element) {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean removeHostElementIfChildNotRemoved(final Arguments arguments, final Element element) {
        try {
            // Get the facade
            final WuicFacade facade = WuicJeeContext.getWuicFacade();

            // Let's load the wuic.xml file and configure the builder with it
            final Reader reader = new StringReader(DOMUtils.getHtml5For(element.getFirstElementChild()));
            facade.configure(new ReaderXmlContextBuilderConfigurator(reader, toString(),
                    WuicJeeContext.initParams().wuicServletMultipleConfInTagSupport()));

            return false;
        } catch (WuicException we) {
            throw new BadArgumentException(new IllegalArgumentException(we));
        } catch (JAXBException se) {
            throw new BadArgumentException(new IllegalArgumentException(
                    "First DOM element child is not a valid XML to describe WUIC configuration", se));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getPrecedence() {
        return 0;
    }
}
