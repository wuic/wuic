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


package com.github.wuic.path.core;

import com.github.wuic.path.DirectoryPath;
import com.github.wuic.path.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * <p>
 * This virtual {@link DirectoryPath} is based on a several other {@link DirectoryPath} and proxy method call to them
 * when {@link #getChild(String)} {@link #list()}, {@link #getLastUpdate()} are called.
 * </p>
 *
 * <p>
 * Note that since there are multiple parent, a  {@link UnsupportedOperationException} will be thrown is
 * {@link #getAbsolutePath()} and {@link #getParent()} are called.
 * </p>
 *
 * @author Guillaume DROUET
 * @version 1.0
 * @since 0.5.0
 */
public class VirtualDirectoryPath implements DirectoryPath {

    /**
     * The logger.
     */
    private final Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * The compositions.
     */
    private List<DirectoryPath> composition;

    /**
     * The name;
     */
    private String name;

    /**
     * <p>
     * Builds a new instance.
     * </p>
     *
     * @param n the name
     * @param directories the directories
     */
    public VirtualDirectoryPath(final String n, final List<DirectoryPath> directories) {
        name = n;
        composition = directories;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Path getChild(final String path) throws IOException {
        for (final DirectoryPath dp : composition) {
            try {
                final Path retval =  dp.getChild(path);

                if (retval != null) {
                    return retval;
                }
            } catch (Exception e) {
                // TODO: fix issue #99
                logger.debug("Path does not exist in this concrete directory, skipping", e);
            }
        }

        throw new FileNotFoundException(path);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String[] list() throws IOException {
        final List<String> retval = new ArrayList<String>();

        for (final DirectoryPath dp : composition) {
            Collections.addAll(retval, dp.list());
        }

        return retval.toArray(new String[retval.size()]);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return name;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DirectoryPath getParent() {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getAbsolutePath() {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getLastUpdate() throws IOException {
        long retval = -1;

        for (final DirectoryPath dp : composition) {
            if (dp.getLastUpdate() > retval) {
                retval = dp.getLastUpdate();
            }
        }

        return retval;
    }
}
