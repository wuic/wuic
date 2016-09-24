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


package com.github.wuic.nut.dao;

import com.github.wuic.ProcessContext;
import com.github.wuic.nut.Nut;
import com.github.wuic.util.Input;

import java.io.IOException;
import java.util.List;

/**
 * <p>
 * This interface abstracts the way you can read nuts through a particular protocol.
 * </p>
 *
 * @author Guillaume DROUET
 * @since 0.3.1
 */
public interface NutDao {

    /**
     * <p>
     * This enumeration specifies formats of path specified to create {@link com.github.wuic.nut.Nut nuts}.
     * </p>
     *
     * @author Guillaume DROUET
     * @since 0.4.2
     */
    enum PathFormat {

        /**
         * Any format. We don't know anything of the format.
         */
        ANY(Boolean.TRUE),

        /**
         * Relative file format.
         */
        RELATIVE_FILE(Boolean.FALSE);

        /**
         * Could represent a regex or not.
         */
        private Boolean regex;

        /**
         * <p>
         * Builds a new enum thanks to given flag.
         * </p>
         *
         * @param r {@code true} is the value could corresponds to a regex, {@code false} otherwise
         */
        private PathFormat(final Boolean r) {
            regex = r;
        }

        /**
         * <p>
         * Indicates if this format could corresponds to a regex.
         * </p>
         *
         * @return {@code true} is the value could corresponds to a regex, {@code false} otherwise
         */
        public Boolean canBeRegex() {
            return regex;
        }
    }

    /**
     * <p>
     * Adds a set of {@link NutDaoListener listeners} to be notified when an update has been detected on the nut.
     * The targeted nut is represented by the specified path.
     * </p>
     *
     * <p>
     * The listeners will be stored in a weak reference so if no strong reference to them exist, they will be
     * garbage collected.
     * </p>
     *
     * @param realPath the real path name of the nut.
     * @param listeners some listeners to be notified when an update has been detected on a nut
     * @throws IOException if an I/O occurs while retrieving last update of the nut
     */
    void observe(String realPath, NutDaoListener ... listeners) throws IOException;

    /**
     * <p>
     * Creates a list of {@link com.github.wuic.nut.Nut nuts} thanks to the given path considered as represented in any format.
     * </p>
     *
     * @param path the path representing the location of the nut(s)
     * @param processContext the process context calling this method
     * @return the created nut(s)
     * @throws java.io.IOException if an I/O error occurs when creating the nut
     * @see NutDao#create(String, com.github.wuic.nut.dao.NutDao.PathFormat, com.github.wuic.ProcessContext)
     */
    List<Nut> create(String path, ProcessContext processContext) throws IOException;

    /**
     * <p>
     * Creates a list of {@link Nut nuts} thanks to the given path.
     * </p>
     *
     * <p>
     * A {@link PathFormat} also gives information on the format used in the string representation of the given path.
     * </p>
     *
     * @param path the path representing the location of the nut(s)
     * @param format the path format
     * @param processContext the process context calling this method
     * @return the created nut(s)
     * @throws IOException if an I/O error occurs when creating the nut
     */
    List<Nut> create(String path, PathFormat format, ProcessContext processContext) throws IOException;

    /**
     * <p>
     * Returns an URI in a {@code String} representation of a proxy serving the given nut.
     * </p>
     *
     * <p>
     * If many proxies are defined, proxy URI is selected in a round-robin mode. Each time a proxy is used, it won't
     * be reused until all other proxies have been used too.
     * </p>
     *
     * @param nut the nut
     * @return the proxy URI, {@code null} if not proxy is set
     */
    String proxyUriFor(Nut nut);

    /**
     * <p>
     * Shutdowns this DAO by releasing its resources.
     * </p>
     */
    void shutdown();

    /**
     * <p>
     * Returns an instance that prefixes any nut name to be created with the given root path.
     * </p>
     *
     * @param rootPath the root path
     * @return the enclosed instance
     */
    NutDao withRootPath(String rootPath);

    /**
     * <p>
     * Opens the stream for the given path.
     * </p>
     *
     * @param path the path to access
     * @param processContext the process context calling this method
     * @return the stream
     * @throws IOException if stream could not be opened
     */
    Input newInputStream(String path, ProcessContext processContext) throws IOException;

    /**
     * <p>
     * Indicates if the given path exists or not.
     * </p>
     *
     * @param path the path the DAO should be able to resolve
     * @param processContext the process context calling this method
     * @return {@code true} if path is resolved, {@code false} otherwise
     * @throws IOException if any I/O error occurs
     */
    Boolean exists(String path, ProcessContext processContext) throws IOException;
}
