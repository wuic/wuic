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


package com.github.wuic.nut.dao;

import com.github.wuic.ProcessContext;
import com.github.wuic.nut.Nut;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * <p>
 * This interface abstracts the way you can read and eventually save nuts through a particular protocol.
 * </p>
 *
 * @author Guillaume DROUET
 * @version 1.6
 * @since 0.3.1
 */
public interface NutDao {

    /**
     * <p>
     * This enumeration specifies formats of path specified to create {@link com.github.wuic.nut.Nut nuts}.
     * </p>
     *
     * @author Guillaume DROUET
     * @version 1.0
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
     * Saves the give nut. An {@link UnsupportedOperationException} will be thrown if the implementation supports
     * only nut access.
     * </p>
     *
     * @param nut the nut to save
     */
    void save(Nut nut);

    /**
     * <p>
     * Indicates if this DAO is able to save a nut, which depends on the underlying protocol.
     * </p>
     *
     * @return {@code true} if {@link NutDao#save(Nut)} is supported, {@code false} otherwise
     */
    Boolean saveSupported();

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
    InputStream newInputStream(String path, ProcessContext processContext) throws IOException;

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
