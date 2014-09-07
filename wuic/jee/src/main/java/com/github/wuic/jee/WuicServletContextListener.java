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


package com.github.wuic.jee;

import com.github.wuic.ApplicationConfig;
import com.github.wuic.WuicFacade;
import com.github.wuic.WuicFacadeBuilder;
import com.github.wuic.exception.WuicException;
import com.github.wuic.exception.wrapper.BadArgumentException;
import com.github.wuic.nut.dao.jee.WebappNutDaoBuilderInspector;
import com.github.wuic.util.BiFunction;
import com.github.wuic.util.IOUtils;
import com.github.wuic.util.WuicScheduledThreadPool;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

/**
 * <p>
 * Servlet context listener that injects into the {@link javax.servlet.ServletContext} the {@link WuicFacade} as an
 * attribute mapped to {@link ApplicationConfig#WEB_WUIC_FACADE} name.
 * </p>
 *
 * @author Guillaume DROUET
 * @version 1.2
 * @since 0.4.1
 */
public class WuicServletContextListener implements ServletContextListener {

    /**
     * <p>
     * Gets the {@link WuicFacade} injected into the given context.
     * </p>
     *
     * @param servletContext the context that must contains the facade
     * @return the facade
     */
    public static WuicFacade getWuicFacade(final ServletContext servletContext) {
        final Object facade = servletContext.getAttribute(ApplicationConfig.WEB_WUIC_FACADE);
        if (facade == null) {
            final String message = String.format("WuicFacade is null, seems the %s was not initialized successfully.", WuicServletContextListener.class.getName());
            throw new BadArgumentException(new IllegalArgumentException(message));
        }

        return WuicFacade.class.cast(facade);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void contextInitialized(final ServletContextEvent sce) {
        final WuicFacadeBuilder builder = new WuicFacadeBuilder(new InitParamProperties(sce.getServletContext()))
                .objectBuilderInspector(new WebappNutDaoBuilderInspector(sce.getServletContext()));

        try {
            sce.getServletContext().setAttribute(ApplicationConfig.WEB_WUIC_FACADE, builder.build());
        } catch (WuicException we) {
            throw new BadArgumentException(new IllegalArgumentException("Unable to initialize WuicServlet", we));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void contextDestroyed(final ServletContextEvent sce) {
        WuicScheduledThreadPool.getInstance().shutdown();
    }

    /**
     * <p>
     * A class that retrieves properties from init-param configured inside a servlet context.
     * </p>
     *
     * @author Guillaume DROUET
     * @version 1.0
     * @since 0.5.0
     */
    public static final class InitParamProperties implements BiFunction<String, String, String> {

        /**
         * The servlet context.
         */
        private ServletContext servletContext;

        /**
         * <p>
         * Builds a new instance.
         * </p>
         *
         * @param sc the servlet context
         */
        public InitParamProperties(final ServletContext sc) {
            servletContext = sc;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String apply(final String key, final String defaultValue) {
            String retval = servletContext.getInitParameter(key);
            if (retval == null) {
                retval = defaultValue;
            }

            if (ApplicationConfig.WUIC_SERVLET_CONTEXT_PARAM.equals(key)) {
                return IOUtils.mergePath(servletContext.getContextPath(), retval);
            } else {
                return retval;
            }
        }
    }
}
