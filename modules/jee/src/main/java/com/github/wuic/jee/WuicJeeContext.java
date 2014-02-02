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


package com.github.wuic.jee;

import com.github.wuic.WuicFacade;
import com.github.wuic.exception.wrapper.BadArgumentException;

import javax.servlet.ServletContext;

/**
 * <p>
 * Provides an access to the {@link WuicFacade} in a JEE context.
 * </p>
 *
 * @author Guillaume DROUET
 * @version 1.0
 * @since 0.4.1
 */
public final class WuicJeeContext {

    /**
     * Facade.
     */
    private static WuicFacade facade;

    /**
     * Servlet context.
     */
    private static ServletContext servletContext;

    /**
     * All init-paremeters.
     */
    private static InitParams initParams;

    /**
     * Default constructor.
     */
    private WuicJeeContext() {

    }

    /**
     * <p>
     * Class created for convenience providing init-params used to configure WUIC.
     * </p>
     *
     * @version 1.0
     * @since 0.4.3
     */
    public static final class InitParams {

        /**
         * Multiple configuration or not.
         */
        private static Boolean multiple;

        /**
         * <p>
         * Builds a new instance.
         * </p>
         */
        private InitParams() {
            // Get init-parameter
            final String m = getInitParameter(WuicServletContextListener.WUIC_SERVLET_MULTIPLE_CONG_IN_TAG_SUPPORT, "true");
            multiple = Boolean.parseBoolean(m);
        }

        /**
         * <p>
         * Returns the init-parameter value associated to key
         * {@link WuicServletContextListener#WUIC_SERVLET_MULTIPLE_CONG_IN_TAG_SUPPORT}.
         * </p>
         *
         * @return the parsed {@code Boolean} associated to the value ({@code true} by default).
         */
        public Boolean wuicServletMultipleConfInTagSupport() {
            return multiple;
        }
    }

    /**
     * <p>
     * Returns the init-parameter in the {@link ServletContext}. If the value is {@code null}, then the specified default
     * value is returned.
     * </p>
     *
     * @param key the init-parameter key
     * @param ifNull the default value
     * @return the value associated to the key, default value if {@code null}
     */
    private static String getInitParameter(final String key, final String ifNull) {
        final String retval = getServletContext().getInitParameter(key);
        return retval == null ? ifNull : retval;
    }

    /**
     * <p>
     * Gets the {@link InitParams parameters}.
     * </p>
     *
     * @return the init-params
     */
    public static InitParams initParams() {
        return initParams;
    }

    /**
     * <p>
     * Gets the {@link WuicFacade}. If the facade is {@link null}, then an exception will be thrown.
     * </p>
     *
     * @return the facade
     */
    public static WuicFacade getWuicFacade() {
        if (facade == null) {
            final String message = String.format("WuicFacade is null, seems the %s was not initialized successfully.", WuicServletContextListener.class.getName());
            throw new BadArgumentException(new IllegalArgumentException(message));
        }

        return facade;
    }

    /**
     * <p>
     * Gets the {@code ServletContext}. If the facade is {@link null}, then an exception will be thrown.
     * </p>
     *
     * @return the servlet context
     */
    public static ServletContext getServletContext() {
        if (servletContext == null) {
            final String message = String.format("ServletContext is null, seems the %s was not initialized successfully.", WuicServletContextListener.class.getName());
            throw new BadArgumentException(new IllegalArgumentException(message));
        }

        return servletContext;

    }

    /**
     * <p>
     * Sets both {@link WuicFacade} and {@code ServletContext}.
     * </p>
     *
     * @param sc the servlet context
     */
    static void setContext(final ServletContext sc) {
        servletContext = sc;
        initParams = new InitParams();
    }

    /**
     * <p>
     * Sets both {@link WuicFacade}.
     * </p>
     *
     * @param f the wuic facade
     */
    static void setFacade(final WuicFacade f) {
        facade = f;
    }
}
