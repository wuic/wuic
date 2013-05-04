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


package com.github.wuic.servlet;

import com.github.wuic.WuicFacade;
import com.github.wuic.resource.WuicResource;
import org.apache.commons.io.IOUtils;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * <p>
 * Abstract servlet which initializes the servlet context and the servlet mapping
 * used for WUIC.
 * </p>
 *
 * @author Guillaume DROUET
 * @version 1.3
 * @since 0.1.1
 */
public class WuicServlet extends HttpServlet {

    /**
     * WUIC's facade attribute name.
     */
    public static final String WUIC_FACADE_ATTRIBUTE = "WUIC_FACADE";

    /**
     * Init parameter which indicates the WUIC context path.
     */
    public static final String WUIC_SERVLET_CONTEXT_PARAM = "c.g.w.wuicContextPath";

    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = -7678202861072625737L;

    /**
     * The servlet context detected when this servlet was initialized.
     */
    private static ServletContext servletContext;
    
    /**
     * The servlet mapping.
     */
    private static String servletMapping;

    /**
     * {@inheritDoc}
     */
    @Override
    public void init(final ServletConfig config) throws ServletException {
        servletContext = config.getServletContext();
        servletMapping = config.getInitParameter(WUIC_SERVLET_CONTEXT_PARAM);

        // Context where resources will be exposed
        final StringBuilder wuicCp = new StringBuilder(servletContext().getContextPath());
        wuicCp.append(wuicCp.length() == 0 ? "" : "/");
        wuicCp.append(servletMapping());

        final WuicFacade facade = WuicFacade.newInstance(wuicCp.toString());
        config.getServletContext().setAttribute(WUIC_FACADE_ATTRIBUTE, facade);

        super.init(config);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void doGet(final HttpServletRequest request, final HttpServletResponse response)
            throws ServletException, IOException {
        final int lastOffset = request.getRequestURI().lastIndexOf('/');
        final String urlWithoutLast = request.getRequestURI().substring(0, lastOffset);
        final String pageName = urlWithoutLast.substring(urlWithoutLast.lastIndexOf('/') + 1);

        if (pageName != null) {
            getPage(pageName, request, response);
        }
    }

    /**
     * <p>
     * Gets a page.
     * </p>
     *
     * @param pageName the page name
     * @param request the request
     * @param response the response
     * @throws IOException if an I/O error occurs
     */
    private void getPage(final String pageName, final HttpServletRequest request,
                         final HttpServletResponse response) throws IOException {
        final String fileName = request.getParameter("file");

        // Get the files group
        final List<WuicResource> files = getWuicFacade().getGroup(pageName);
        InputStream is = null;

        for (WuicResource resource : files) {
            response.setContentType(resource.getFileType().getMimeType());

            if (fileName == null || resource.getName().equals(fileName)) {
                try {
                    is = resource.openStream();
                    IOUtils.copy(is, response.getOutputStream());
                    is = null;
                } finally {
                    if (is != null) {
                        is.close();
                    }
                }
            }
        }
    }

    /**
     * <p>
     * Gets the WUIC's facade from the servlet context.
     * </p>
     *
     * @return the WUIC's facade
     */
    public WuicFacade getWuicFacade() {
        return (WuicFacade) getServletContext().getAttribute(WUIC_FACADE_ATTRIBUTE);
    }

    /**
     * <p>
     * Returns the {@code ServletContext}.
     * </p>
     * 
     * @return the servlet context
     */
    public static ServletContext servletContext() {
        return servletContext;
    }
    
    /**
     * <p>
     * Returns the first servlet mapping for this servlet.
     * </p>
     * 
     * @return the servlet mapping
     */
    public static String servletMapping() {
        return servletMapping;
    }
}
