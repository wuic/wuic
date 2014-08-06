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

import com.github.wuic.WuicFacade;
import com.github.wuic.config.ObjectBuilderInspector;
import com.github.wuic.exception.WuicException;
import com.github.wuic.exception.wrapper.BadArgumentException;
import com.github.wuic.nut.dao.jee.WebappNutDaoBuilderInspector;
import com.github.wuic.util.IOUtils;
import com.github.wuic.util.WuicScheduledThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * <p>
 * Servlet context listener that injects into the {@link WuicJeeContext} the context and the {@link WuicFacade}.
 * </p>
 *
 * @author Guillaume DROUET
 * @version 1.2
 * @since 0.4.1
 */
public class WuicServletContextListener implements ServletContextListener {

    /**
     * Init parameter which indicates if configurations injected by tag supports (JSP, Thymeleaf, etc) should be done
     * each time a page is processed or not.
     */
    public static final String WUIC_SERVLET_MULTIPLE_CONG_IN_TAG_SUPPORT = "c.g.w.wuicMultipleConfigInTagSupport";

    /**
     * Init parameter which indicates the WUIC context path.
     */
    public static final String WUIC_SERVLET_CONTEXT_PARAM = "c.g.w.wuicContextPath";

    /**
     * Init parameter which indicates the WUIC xml file.
     */
    public static final String WUIC_SERVLET_XML_PATH_PARAM = "c.g.w.wuicXmlPath";

    /**
     * Init parameter which indicates that the WUIC context path is a system property.
     */
    public static final String WUIC_SERVLET_XML_SYS_PROP_PARAM = "c.g.w.wuicXmlPathAsSystemProperty";

    /**
     * Init parameter which indicates to use or not context builder configurators which inject default DAOs and engines.
     */
    public static final String WUIC_USE_DEFAULT_CONTEXT_BUILDER_CONFIGURATORS = "c.g.w.useDefaultContextBuilderConfigurators";

    /**
     * The logger.
     */
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    /**
     * {@inheritDoc}
     */
    @Override
    public void contextInitialized(final ServletContextEvent sce) {
        final String servletMapping = sce.getServletContext().getInitParameter(WUIC_SERVLET_CONTEXT_PARAM);
        WuicJeeContext.setContext(sce.getServletContext());

        // Context where nuts will be exposed
        final String wuicCp = IOUtils.mergePath("/", sce.getServletContext().getContextPath(), servletMapping == null ? "" : servletMapping);

        log.info("WUIC's full context path is {}", wuicCp);

        try {
            final String useDefaultConfStr = sce.getServletContext().getInitParameter(WUIC_USE_DEFAULT_CONTEXT_BUILDER_CONFIGURATORS);
            final Boolean useDefaultConf = useDefaultConfStr == null ? Boolean.TRUE : Boolean.parseBoolean(useDefaultConfStr);

            final String wuicXmlPath = sce.getServletContext().getInitParameter(WUIC_SERVLET_XML_PATH_PARAM);
            final WuicFacade facade;
            final ObjectBuilderInspector inspector = new WebappNutDaoBuilderInspector(sce.getServletContext());

            // Choose specific location for XML file
            if (wuicXmlPath == null) {
                facade = WuicFacade.newInstance(wuicCp, useDefaultConf, inspector);
            } else {
                if (Boolean.parseBoolean(sce.getServletContext().getInitParameter(WUIC_SERVLET_XML_SYS_PROP_PARAM))) {
                    facade = WuicFacade.newInstance(wuicCp, new URL(System.getProperty(wuicXmlPath)), useDefaultConf, inspector);
                } else {
                    facade = WuicFacade.newInstance(wuicCp, new URL(wuicXmlPath), useDefaultConf, inspector);
                }
            }

            WuicJeeContext.setFacade(facade);
        } catch (WuicException we) {
            throw new BadArgumentException(new IllegalArgumentException("Unable to initialize WuicServlet", we));
        } catch (MalformedURLException mue) {
            throw new BadArgumentException(new IllegalArgumentException("Unable to initialize WuicServlet", mue));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void contextDestroyed(final ServletContextEvent sce) {
        WuicJeeContext.setFacade(null);
        WuicJeeContext.setContext(null);
        WuicScheduledThreadPool.getInstance().shutdown();
    }
}
