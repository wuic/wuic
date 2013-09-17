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


package com.github.wuic.sample.polling.servlet;

import com.github.wuic.exception.wrapper.StreamException;
import com.github.wuic.util.IOUtils;
import com.github.wuic.xml.XmlPropertyBean;
import com.github.wuic.xml.XmlWuicBean;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

import java.io.*;

/**
 * <p>
 * Saves the sumbitted script into the file loaded by WUIC.
 * </p>
 *
 * @author Guillaume DROUET
 * @version 1.0
 * @since 0.4.0
 */
public class SaveServlet extends HttpServlet {

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doPost(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
        InputStream is = null;
        OutputStream os = null;
        File file = null;

        try {
            // Save script
            final String code = req.getParameter("script");
            file = new File(InitJavascriptFileListener.DIRECTORY_PATH, InitJavascriptFileListener.FILE_NAME);
            is = new ByteArrayInputStream(code.getBytes());
            os = new FileOutputStream(file);
            IOUtils.copyStream(is, os);
        } catch (StreamException se) {
            throw new ServletException(se);
        } finally {
            IOUtils.close(is, os);
        }

        try {
            // Save configuration
            file = new File(InitJavascriptFileListener.DIRECTORY_PATH, "wuic.xml");
            final Boolean cache = "on".equals(req.getParameter("cache"));
            final JAXBContext context = JAXBContext.newInstance(XmlWuicBean.class);
            final XmlWuicBean bean = (XmlWuicBean) context.createUnmarshaller().unmarshal(file);
            final XmlPropertyBean prop = bean.getEngineBuilders().get(0).getProperties().remove(0);
            bean.getEngineBuilders().get(0).getProperties().add(new XmlPropertyBean(prop.getKey(), cache.toString()));
            context.createMarshaller().marshal(bean, file);
        } catch (JAXBException je) {
            throw new ServletException(je);
        }

        resp.sendRedirect("/");
    }
}
