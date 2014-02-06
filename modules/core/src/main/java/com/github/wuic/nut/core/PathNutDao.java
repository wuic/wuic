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


package com.github.wuic.nut.core;

import com.github.wuic.NutType;
import com.github.wuic.exception.SaveOperationNotSupportedException;
import com.github.wuic.exception.wrapper.BadArgumentException;
import com.github.wuic.exception.wrapper.StreamException;
import com.github.wuic.nut.AbstractNutDao;
import com.github.wuic.nut.Nut;
import com.github.wuic.path.DirectoryPath;
import com.github.wuic.path.FilePath;
import com.github.wuic.path.Path;
import com.github.wuic.util.IOUtils;

import java.io.IOException;
import java.math.BigInteger;
import java.util.List;
import java.util.regex.Pattern;

/**
 * <p>
 * A {@link com.github.wuic.nut.NutDao} implementation for accesses based on the path API provided by WUIC.
 * </p>
 *
 * <p>
 * The class is abstract and asks subclass to define the way the base directory should be defined.
 * </p>
 *
 * @author Guillaume DROUET
 * @version 1.0
 * @since 0.4.2
 */
public abstract class PathNutDao extends AbstractNutDao {

    /**
     * Base directory where the protocol has to look up.
     */
    protected DirectoryPath baseDirectory;

    /**
     * {@code true} if the path is a regex, {@code false} otherwise
     */
    private Boolean regularExpression;

    /**
     * <p>
     * Builds a new instance with a base directory.
     * </p>
     *
     * @param base the directory where we have to look up
     * @param basePathAsSysProp {@code true} if the base path is a system property
     * @param pollingSeconds the interleave for polling operations in seconds (-1 to deactivate)
     * @param proxies the proxies URIs in front of the nut
     * @param regex if the path should be considered as a regex or not
     */
    public PathNutDao(final String base,
                      final Boolean basePathAsSysProp,
                      final String[] proxies,
                      final int pollingSeconds,
                      final Boolean regex) {
        super(base, basePathAsSysProp, proxies, pollingSeconds);
        regularExpression = regex;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<String> listNutsPaths(final String pattern) throws StreamException {
        init();
        final Pattern compiled = Pattern.compile(regularExpression ? pattern : Pattern.quote(pattern));
        return IOUtils.listFile(DirectoryPath.class.cast(baseDirectory), compiled);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Nut accessFor(final String realPath, final NutType type) throws StreamException {
        init();

        try {
            final Path p = baseDirectory.getChild(realPath);

            if (p instanceof FilePath) {
                final FilePath fp = FilePath.class.cast(p);
                return new FilePathNut(fp, realPath, type, new BigInteger(getLastUpdateTimestampFor(fp).toString()));
            } else {
                throw new BadArgumentException(new IllegalArgumentException(String.format("%s is not a file", p)));
            }
        } catch (IOException ioe) {
            throw new StreamException(ioe);
        }
    }

    /**
     * <p>
     * Gets the last update timestamp of the given {@link Path}.
     * </p>
     *
     * @param path the path
     * @return the last timestamp
     * @throws StreamException if any I/O error occurs
     */
    private Long getLastUpdateTimestampFor(final Path path) throws StreamException {
        try {
            return path.getLastUpdate();
        } catch (IOException ioe) {
            throw new StreamException(ioe);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Long getLastUpdateTimestampFor(final String path) throws StreamException {
        try {
            return getLastUpdateTimestampFor(baseDirectory.getChild(path));
        } catch (IOException ioe) {
            throw new StreamException(ioe);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void save(final Nut nut) {
        // TODO : update path API
        throw new SaveOperationNotSupportedException(this.getClass());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Boolean saveSupported() {
        // TODO : return true once path API supports write operations
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public String toString() {
        return String.format("%s with base directory %s", getClass().getName(), baseDirectory);
    }

    /**
     * <p>
     * Initializes the {@link com.github.wuic.path.DirectoryPath} if {@code null}. Throws an {@code BadArgumentException} if
     * the given {@code String} does not represents a directory.
     * </p>
     *
     * @throws com.github.wuic.exception.wrapper.StreamException if any I/O error occurs
     */
    private void init() throws StreamException {
        if (baseDirectory == null) {
            try {
                baseDirectory = createBaseDirectory();
            } catch (IOException ioe) {
                throw new StreamException(ioe);
            }
        }
    }

    /**
     * <p>
     * Creates the {@link DirectoryPath} associated to the {@link AbstractNutDao#basePath}.
     * </p>
     *
     * @return the {@link DirectoryPath}
     * @throws IOException if any I/O error occurs
     */
    protected abstract DirectoryPath createBaseDirectory() throws IOException;
}
