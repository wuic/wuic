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


package com.capgemini.web.wuic.servlet;

import java.lang.reflect.Field;
import java.util.List;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

/**
 * <p>
 * Abstract servlet which initializes the servlet context and the servlet mapping
 * used for WUIC.
 * </p>
 *
 * @author Guillaume DROUET
 * @version 1.0
 * @since 0.1.1
 */
public abstract class WuicServlet extends HttpServlet {
    
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
        try {
            servletContext = config.getServletContext();
            
            // Get the first servlet mapping for this servlet
            Class<?> clazz = config.getClass();
            Field configField;
            
            // Use reflection to read the field 'config' of the ServletConfig
            configField = clazz.getDeclaredField("config");
            configField.setAccessible(true);
    
            // Then do the same thing for the 'mappings' field
            clazz = configField.get(config).getClass();
            final Field mappingsField = clazz.getDeclaredField("mappings");
            mappingsField.setAccessible(true);
            
            // Get all the mappings
            final List<?> mappings = (List<?>) mappingsField.get(configField.get(config));
    
            // Must found one mapping
            if (mappings == null || mappings.isEmpty()) {
                throw new ServletException("One mapping is required for the servlet " + getClass().getName());
            }
            
            servletMapping = mappings.get(0).toString();
            
            super.init(config);
        } catch (SecurityException se) {
            throw new ServletException(se);
        } catch (NoSuchFieldException nsfe) {
            throw new ServletException(nsfe);
        } catch (IllegalAccessException iae) {
            throw new ServletException(iae);
        }
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
