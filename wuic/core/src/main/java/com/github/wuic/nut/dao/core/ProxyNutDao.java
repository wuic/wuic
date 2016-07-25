/*
 * "Copyright (c) 2016   Capgemini Technology Services (hereinafter "Capgemini")
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


package com.github.wuic.nut.dao.core;

import com.github.wuic.ProcessContext;
import com.github.wuic.nut.Nut;
import com.github.wuic.nut.dao.NutDao;
import com.github.wuic.nut.dao.NutDaoListener;
import com.github.wuic.nut.dao.NutDaoWrapper;
import com.github.wuic.util.IOUtils;
import com.github.wuic.util.Input;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * Internal {@link com.github.wuic.nut.dao.NutDao} class that maps particular path to a particular {@link Nut} or then
 * to a particular {@link NutDao}. If the path is not mapped, then a delegated DAO is called.
 * </p>
 *
 * @author Guillaume DROUET
 * @since 0.4.4
 */
public class ProxyNutDao extends NutDaoWrapper implements NutDao {

    /**
     * All mapped path to corresponding nut.
     */
    private Map<String, Nut> proxyNut;

    /**
     * All mapped path to corresponding {@link NutDao}.
     */
    private Map<String, NutDao> proxyNutDao;

    /**
     * The root path.
     */
    private String rootPath;

    /**
     * <p>
     * Builds a new instance thanks to a delegated {@link NutDao}.
     * </p>
     *
     * @param rootPath the root path that prefixes any rule
     * @param delegate the delegated DAO
     */
    public ProxyNutDao(final String rootPath, final NutDao delegate) {
        super(delegate);
        this.proxyNut = new HashMap<String, Nut>();
        this.proxyNutDao = new HashMap<String, NutDao>();
        this.rootPath = rootPath;
    }

    /**
     * <p>
     * Adds a mapping between a path and a nut.
     * </p>
     *
     * @param path the path
     * @param nut the nut returned when path is used
     */
    public void addRule(final String path, final Nut nut){
        proxyNut.put(rootPath.isEmpty() ? path : IOUtils.mergePath(rootPath, path), nut);
    }

    /**
     * <p>
     * Adds a mapping between a path and a nut DAO.
     * </p>
     *
     * @param path the path
     * @param dao the {@link NutDao} to call when path is used
     */
    public void addRule(final String path, final NutDao dao){
        proxyNutDao.put(rootPath.isEmpty() ? path : IOUtils.mergePath(rootPath, path), dao);
    }

    /**
     * <p>
     * Gets a DAO for a particular path.
     * </p>
     *
     * @param path the proxy path
     * @return the mapped DAO
     */
    public NutDao getNutDao(final String path) {
        final NutDao dao = proxyNutDao.get(path);
        return (dao == null) ? getNutDao() : dao;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void observe(final String realPath, final NutDaoListener... listeners) throws IOException {
        getNutDao(realPath).observe(realPath, listeners);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Nut> create(final String path, final ProcessContext processContext) throws IOException {
        final Nut nut = proxyNut.get(path);
        List<Nut> retval;

        // Nut not mapped, delegate call
        if (nut == null) {
            retval = getNutDao(path).create(path, processContext);
        } else {
            retval = Arrays.asList(nut);
        }

        return retval;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Nut> create(final String path, final PathFormat format, final ProcessContext processContext) throws IOException {
        final Nut nut = proxyNut.get(path);
        List<Nut> retval;

        // Nut not mapped, delegate call
        if (nut == null) {
            retval = getNutDao(path).create(path, format, processContext);
        } else {
            retval = Arrays.asList(nut);
        }

        return retval;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Input newInputStream(final String path, final ProcessContext processContext) throws IOException {
        final Nut nut = proxyNut.get(path);

        // Path not mapped, call delegate
        if (nut == null) {
            return super.newInputStream(path, processContext);
        } else {
            return nut.openStream();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return IOUtils.NEW_LINE
                + "Delegate: "
                + getNutDao()
                + IOUtils.NEW_LINE
                + "Proxy DAO: "
                + Arrays.deepToString(proxyNutDao.keySet().toArray())
                + IOUtils.NEW_LINE
                + "Proxy Nuts: "
                + Arrays.deepToString(proxyNut.keySet().toArray());
    }
}