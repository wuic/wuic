/*
 * Copyright (c) 2016   The authors of WUIC
 *
 * License/Terms of Use
 * Permission is hereby granted, free of charge and for the term of intellectual
 * property rights on the Software, to any person obtaining a copy of this software
 * and associated documentation files (the "Software"), to use, copy, modify and
 * propagate free of charge, anywhere in the world, all or part of the Software
 * subject to the following mandatory conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, PEACEFUL ENJOYMENT,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS
 * OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */


package com.github.wuic.nut.dao.servlet;

import com.github.wuic.config.ObjectBuilderInspector;
import com.github.wuic.nut.dao.core.DiskNutDao;

import javax.servlet.ServletContext;

/**
 * <p>
 * This inspector replaces the built {@link WebappNutDao} by a {@link DiskNutDao} if the war is exploded. Otherwise
 * it just set the servlet context to the {@link WebappNutDao}.
 * </p>
 *
 * @author Guillaume DROUET
 * @since 0.5
 */
@ObjectBuilderInspector.InspectedType({ ServletContextHandler.class, WebappNutDao.class})
public class WebappNutDaoBuilderInspector implements ObjectBuilderInspector {

    /**
     * Default base path when running an exploded war.
     */
    private static final String EXPLODED_BASE_PATH = ".";

    /**
     * Default base path when running an not exploded war.
     */
    private static final String WAR_BASE_PATH = "/";

    /**
     * Flags that indicates if war is exploded.
     */
    private final Boolean isExploded;

    /**
     * The servlet context.
     */
    private final ServletContext servletContext;

    /**
     * <p>
     * Builds a new instance.
     * </p>
     *
     * @param servletContext the servlet context
     */
    public WebappNutDaoBuilderInspector(final ServletContext servletContext) {
        this.servletContext = servletContext;
        this.isExploded = servletContext.getRealPath(EXPLODED_BASE_PATH) != null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T> T inspect(final T object) {
        if (object instanceof WebappNutDao) {
            final WebappNutDao webappNutDao = WebappNutDao.class.cast(object);

            // Pre compute base path
            String basePath = webappNutDao.getBasePath();

            // Manage for DAO the case of base path in exploded context (file path)
            if (isExploded) {
                // and then pre compute real path
                basePath = servletContext.getRealPath(basePath);
                final Long fixedVersionNumber = webappNutDao.getVersionNumberStrategy().getFixedVersionNumber();

                // We pre compute specifically base path so we pass false to indicate that base path is not a system property
                final DiskNutDao dao =  new WebappDiskNutDao(webappNutDao);

                dao.init(WAR_BASE_PATH.equals(basePath) ? EXPLODED_BASE_PATH : basePath,
                        webappNutDao.getProxyUris(),
                        webappNutDao.getPollingInterval(),
                        webappNutDao.getRegularExpression(),
                        webappNutDao.getWildcardExpression());
                dao.init(webappNutDao.getVersionNumberStrategy().getContentBasedVersionNumber(),
                        webappNutDao.getVersionNumberStrategy().getComputeVersionAsynchronously(),
                        fixedVersionNumber == null ? null : fixedVersionNumber.toString());
                dao.setNutTypeFactory(webappNutDao.getNutTypeFactory());
                dao.setTemporaryFileManager(webappNutDao.getTemporaryFileManager());
                return (T) dao;
            }
        }

        if (object instanceof ServletContextHandler) {
            ServletContextHandler.class.cast(object).setServletContext(servletContext);
        }

        return object;
    }
}
