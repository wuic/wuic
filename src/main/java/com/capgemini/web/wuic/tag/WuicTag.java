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
 * •   The above copyright notice and this permission notice shall be included in
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


package com.capgemini.web.wuic.tag;

import com.capgemini.web.wuic.WuicFacade;
import com.capgemini.web.wuic.WuicResource;
import com.capgemini.web.wuic.servlet.WuicTagServlet;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.TagSupport;

/**
 * <p>
 * This tag writes the scripts of the page specified to it attributes.
 * </p>
 *
 * @author Guillaume DROUET
 * @version 1.0
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
            final List<WuicResource> files = WuicFacade.getInstance().getGroup(pageName);
            
            for (WuicResource resource : files) {
                switch (resource.getFileType()) {
                    case CSS :
                        pageContext.getOut().println(cssImport(resource));
                        break;
                        
                    case JAVASCRIPT :
                        pageContext.getOut().println(javascriptImport(resource));
                        break;
                        
                    default :
                    final StringBuilder msg = new StringBuilder();
                    msg.append(resource.getName());
                    msg.append(" could not be imported. WuicTag currently uspports .js and .css extensions only");
                    
                    throw new JspException(msg.toString());
                }
            }
        } catch (IOException ioe) {
            throw new JspException("I/O Error", ioe);
        }
        
        return SKIP_BODY;
    }
    
    /**
     * <p>
     * Generates import for CSS script.
     * </p>
     * 
     * @param resource the CSS resource
     * @return the import
     * @throws UnsupportedEncodingException if the URL could not be encoded
     */
    private String cssImport(final WuicResource resource) throws UnsupportedEncodingException {
        final StringBuilder retval = new StringBuilder();
        
        retval.append("<link rel=\"stylesheet\" type=\"text/css\" href=\"");
        retval.append(getUrl(resource));
        retval.append("\" />");
        
        return retval.toString();
    }
    
    /**
     * <p>
     * Generates import for Javascript script.
     * </p>
     * 
     * @param resource the Javascript resource
     * @return the import
     * @throws UnsupportedEncodingException if the URL could not be encoded
     */
    private String javascriptImport(final WuicResource resource) throws UnsupportedEncodingException {
        final StringBuilder retval = new StringBuilder();
        
        retval.append("<script type=\"text/javascript");
        retval.append("\" src=\"");
        retval.append(getUrl(resource));
        retval.append("\"></script>");
        
        return retval.toString();
    }
    
    /**
     * <p>
     * Generates the URL to use to access to the given resource.
     * </p>
     * 
     * @param resource the resource
     * @return the url
     * @throws UnsupportedEncodingException if the URL could not be encoded
     */
    private String getUrl(final WuicResource resource) throws UnsupportedEncodingException {
        final StringBuilder urlBuilder = new StringBuilder();
        
        urlBuilder.append(WuicTagServlet.servletContext().getContextPath());
        urlBuilder.append(WuicTagServlet.servletMapping().replace("*", ""));
        urlBuilder.append(pageName);
        urlBuilder.append("/?file=");
        urlBuilder.append(URLEncoder.encode(resource.getName(), "UTF-8"));
        
        return urlBuilder.toString();
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
