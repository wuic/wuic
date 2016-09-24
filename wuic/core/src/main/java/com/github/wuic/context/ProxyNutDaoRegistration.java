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


package com.github.wuic.context;

import com.github.wuic.nut.dao.NutDao;
import com.github.wuic.nut.dao.core.ProxyNutDao;

/**
 * <p>
 * An class representing a registration that leads to a proxy creation. This creation
 * can't be completed until referenced DAO are created. This class keeps data to update
 * the proxy when possible.
 * </p>
 *
 * @author Guillaume DROUET
 * @since 0.5.1
 */
public final class ProxyNutDaoRegistration {

    /**
     * The proxy path.
     */
    private final String path;

    /**
     * The referenced DAO.
     */
    private final String daoId;

    /**
     * The proxy instance to update.
     */
    private final ProxyNutDao proxy;

    /**
     * <p>
     * Builds a new instance.
     * </p>
     *
     * @param path the path
     * @param daoId the DAO id
     * @param proxyNutDao the proxy
     */
    ProxyNutDaoRegistration(final String path, final String daoId, final ProxyNutDao proxyNutDao) {
        this.path = path;
        this.daoId = daoId;
        this.proxy = proxyNutDao;
    }

    /**
     * <p>
     * Provides the referenced DAO.
     * </p>
     *
     * @return the DAO id
     */
    String getDaoId() {
        return daoId;
    }

    /**
     * <p>
     * Adds a rule with the given DAO.
     * </p>
     *
     * @param dao the referenced DAO
     */
    void addRule(final NutDao dao) {
        proxy.addRule(path, dao);
    }
}
