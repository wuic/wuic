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


package com.github.wuic.tag;

import com.github.wuic.WuicFacade;
import com.github.wuic.exception.WuicException;
import com.github.wuic.exception.wrapper.BadArgumentException;
import com.github.wuic.exception.wrapper.StreamException;
import com.github.wuic.nut.Nut;
import com.github.wuic.servlet.WuicServlet;
import com.github.wuic.util.IOUtils;

import java.io.IOException;
import java.util.List;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.TagSupport;

/**
 * <p>
 * This tag writes the scripts of the page specified to it attributes.
 * </p>
 *
 * @author Guillaume DROUET
 * @version 1.3
 * @since 0.1.0
 */
public class WuicTag extends TagSupport {

    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 4305181623848741300L;

    /**
     * The page name.
     */
    private String pageName;
    
    /**
     * <p>
     * Includes according to the page name.
     * </p>
     * 
     * <p>
     * Only CSS and Javascript files could be imported.
     * </p>
     * 
     * @throws JspException if an I/O error occurs
     * @return the {@code SKIP_BODY} code
     */
    @Override
    public int doStartTag() throws JspException {
        try {
            // Get the files group
            final WuicFacade facade = ((WuicFacade) pageContext.getServletContext().getAttribute("WUIC_FACADE"));

            if (facade == null) {
                throw new BadArgumentException(new IllegalArgumentException("WuicFacade is null, seems the WuicServlet did not initialized successfully."));
            }

            final List<Nut> nuts = facade.runWorkflow(pageName);

            for (final Nut nut : nuts) {
                writeScriptImport(nut);
            }
        } catch (IOException ioe) {
            throw new JspException("Can't write import statements into JSP output stream", new StreamException(ioe));
        } catch (WuicException we) {
            throw new JspException(we);
        }
        
        return SKIP_BODY;
    }

    private void writeScriptImport(final Nut nut) throws IOException {
        switch (nut.getNutType()) {
            case CSS :
                pageContext.getOut().println(cssImport(nut));
                break;

            case JAVASCRIPT :
                pageContext.getOut().println(javascriptImport(nut));
                break;

            default :
                // TODO : think about an effective way to define nuts which should be imported
                if (nut.getReferencedNuts() != null) {
                    for (final Nut ref : nut.getReferencedNuts()) {
                        writeScriptImport(ref);
                    }
                }
        }
    }

    /**
     * <p>
     * Generates import for CSS script.
     * </p>
     * 
     * @param nut the CSS nut
     * @return the import
     */
    private String cssImport(final Nut nut) {
        final StringBuilder retval = new StringBuilder();
        
        retval.append("<link rel=\"stylesheet\" type=\"text/css\" href=\"");
        retval.append(getUrl(nut));
        retval.append("\" />");
        
        return retval.toString();
    }
    
    /**
     * <p>
     * Generates import for Javascript script.
     * </p>
     * 
     * @param nut the Javascript nut
     * @return the import
     */
    private String javascriptImport(final Nut nut) {
        final StringBuilder retval = new StringBuilder();
        
        retval.append("<script type=\"text/javascript");
        retval.append("\" src=\"");
        retval.append(getUrl(nut));
        retval.append("\"></script>");
        
        return retval.toString();
    }
    
    /**
     * <p>
     * Generates the URL to use to access to the given nut.
     * </p>
     * 
     * @param nut the nut
     * @return the url
     */
    private String getUrl(final Nut nut) {
        return IOUtils.mergePath("/",
                WuicServlet.servletContext().getContextPath(),
                WuicServlet.servletMapping(),
                pageName,
                nut.getName());
    }

    /**
     * <p>
     * Returns the page name.
     * </p>
     * 
     * @return the page name
     */
    public String getPageName() {
        return pageName;
    }

    /**
     * <p>
     * Sets the page name.
     * </p>
     * 
     * @param page the page name
     */
    public void setPageName(final String page) {
        this.pageName = page;
    }
}
