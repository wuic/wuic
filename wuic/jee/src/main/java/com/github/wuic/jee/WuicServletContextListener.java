/*
 * "Copyright (c) 2015   Capgemini Technology Services (hereinafter "Capgemini")
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
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

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
     * Message logged when a custom implementation could not be applied.
     */
    private static final String CUSTOM_PARAM_PROVIDER_MESSAGE = "Cannot apply custom parameter provider";

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
     * <p>
     * Gets the {@link BiFunction} providing parameters..
     * </p>
     *
     * @param servletContext the context that must contains the function
     * @return the function
     */
    @SuppressWarnings("unchecked")
    public static BiFunction<String, String, String> getParamProvider(final ServletContext servletContext) {
        final Object fct = servletContext.getAttribute(ApplicationConfig.INIT_PARAM_FUNCTION);
        if (fct == null) {
            final String message = String.format("BiFunction is null, seems the %s was not initialized successfully.", WuicServletContextListener.class.getName());
            throw new BadArgumentException(new IllegalArgumentException(message));
        }

        return (BiFunction<String, String, String>) fct;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked")
    public void contextInitialized(final ServletContextEvent sce) {
        final ServletContext sc = sce.getServletContext();
        final String paramClass = sc.getInitParameter(ApplicationConfig.INIT_PARAM_FUNCTION);
        final BiFunction<String, String, String> paramProvider;

        // No specific provider, use default
        if (paramClass == null) {
            paramProvider = paramProvider(sc);
        } else {
            try {
                // The class BiFunction implementation must be parameterized only with java.lang.String types
                // If it does not expose a default constructor, it must provide a constructor expected a ServletContext
                Constructor<?> defaultConstructor = null;
                Constructor<?> scConstructor = null;
                final Class<?> clazz = Class.forName(paramClass);

                // Lookup constructor
                for (final Constructor<?> c : clazz.getDeclaredConstructors()) {
                    if (c.getParameterTypes().length == 0) {
                        defaultConstructor = c;
                    } else if (c.getParameterTypes().length == 1 && c.getParameterTypes()[0].equals(ServletContext.class)) {
                        scConstructor = c;
                    }
                }

                // Check if constructor exists
                if (scConstructor != null) {
                    paramProvider = (BiFunction<String, String, String>) scConstructor.newInstance(sc);
                } else if (defaultConstructor != null) {
                    paramProvider = (BiFunction<String, String, String>) defaultConstructor.newInstance();
                } else {
                    throw new IllegalStateException(
                            String.format("'%s' provide at least a default constructor or a constructor expecting one parameter of type '%s'",
                                    paramClass, ServletContext.class.getName()));
                }
            } catch (ClassNotFoundException cnfe) {
                throw new IllegalStateException(CUSTOM_PARAM_PROVIDER_MESSAGE, cnfe);
            } catch (IllegalAccessException iae) {
                throw new IllegalStateException(CUSTOM_PARAM_PROVIDER_MESSAGE, iae);
            } catch (InvocationTargetException ite) {
                throw new IllegalStateException(CUSTOM_PARAM_PROVIDER_MESSAGE, ite);
            } catch (InstantiationException ie) {
                throw new IllegalStateException(CUSTOM_PARAM_PROVIDER_MESSAGE, ie);
            }
        }

        sc.setAttribute(ApplicationConfig.INIT_PARAM_FUNCTION, paramProvider);
        final WuicFacadeBuilder builder = new WuicFacadeBuilder(paramProvider)
                .objectBuilderInspector(new WebappNutDaoBuilderInspector(sc));

        try {
            sc.setAttribute(ApplicationConfig.WEB_WUIC_FACADE, builder.build());
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
     * Returns the function the {@link WuicFacadeBuilder} will use to retrieve properties.
     * </p>
     *
     * @param sc the servlet context
     * @return the function to apply
     */
    protected BiFunction<String, String, String> paramProvider(final ServletContext sc) {
        return new InitParamProperties(sc);
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
