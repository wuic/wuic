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


package com.github.wuic.tag;

import com.github.wuic.WuicFacade;
import com.github.wuic.exception.WuicException;
import com.github.wuic.jee.WuicJeeContext;
import com.github.wuic.xml.ReaderXmlContextBuilderConfigurator;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.BodyContent;
import javax.servlet.jsp.tagext.BodyTagSupport;
import javax.xml.bind.JAXBException;

/**
 * <p>
 * This tag evaluates the XML configuration described in the body-content and injects it to the global configuration.
 * </p>
 *
 * @author Guillaume DROUET
 * @version 1.0
 * @since 0.4.2
 */
public class WuicXmlConfigurationTag extends BodyTagSupport {

    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 4305181623848741300L;

    /**
     * <p>
     * Includes according to the page name.
     * </p>
     *
     * <p>
     * Only CSS and Javascript files could be imported.
     * </p>
     *
     * <p>
     * Can creates heaps on the fly based on given body content.
     * </p>
     *
     * @throws JspException if an I/O error occurs
     */
    @Override
    public int doAfterBody() throws JspException {
        try {
            // Get the facade
            final WuicFacade facade = WuicJeeContext.getWuicFacade();

            // Let's load the wuic.xml file and configure the builder with it
            final BodyContent content = getBodyContent();
            facade.configure(new ReaderXmlContextBuilderConfigurator(content.getReader(), toString(),
                    WuicJeeContext.initParams().wuicServletMultipleConfInTagSupport()));
            content.clearBody();
        } catch (WuicException we) {
            throw new JspException(we);
        } catch (JAXBException se) {
            throw new JspException("Body content is not a valid XML to describe WUIC configuration", se);
        }

        return SKIP_BODY;
    }
}
