/*
 * "Copyright (c) 2013   Capgemini Technology Services (hereinafter "Capgemini")
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


package com.github.wuic.resource.impl.disk;

import com.github.wuic.FileType;
import com.github.wuic.exception.wrapper.BadArgumentException;
import com.github.wuic.exception.wrapper.StreamException;
import com.github.wuic.resource.WuicResource;
import com.github.wuic.resource.WuicResourceProtocol;
import com.github.wuic.util.IOUtils;
import com.github.wuic.util.path.DirectoryPath;
import com.github.wuic.util.path.FilePath;
import com.github.wuic.util.path.Path;

import java.io.IOException;
import java.util.List;
import java.util.regex.Pattern;

/**
 * <p>
 * A {@link com.github.wuic.resource.WuicResourceProtocol} implementation for classpath accesses.
 * </p>
 *
 * @author Guillaume DROUET
 * @version 1.1
 * @since 0.3.1
 */
public class DiskWuicResourceProtocol implements WuicResourceProtocol {

    /**
     * Base directory where the protocol has to look up.
     */
    private DirectoryPath baseDirectory;

    /**
     * The path which represents the directory location.
     */
    private String path;

    /**
     * <p>
     * Builds a new instance with a base directory.
     * </p>
     *
     * @param base the directory where we have to look up
     */
    public DiskWuicResourceProtocol(final String base) {
        path = base;
    }

    /**
     * <p>
     * Initializes the {@link DirectoryPath} if {@code null}. Throws an {@code BadArgumentException} if
     * the given {@code String} does not represents a directory.
     * </p>
     *
     * @throws StreamException if any I/O error occurs
     */
    private void init() throws StreamException {
        if (baseDirectory == null) {
            try {
                final Path file = IOUtils.buildPath(path);

                if (file instanceof DirectoryPath) {
                    baseDirectory = DirectoryPath.class.cast(file);
                } else {
                    throw new BadArgumentException(new IllegalArgumentException(String.format("%s is not a directory", path)));
                }
            } catch (IOException ioe) {
                throw new StreamException(ioe);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<String> listResourcesPaths(final Pattern pattern) throws StreamException {
        init();
        return IOUtils.listFile(DirectoryPath.class.cast(baseDirectory), pattern);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public WuicResource accessFor(final String realPath, final FileType type) throws StreamException {
        init();

        try {
            final Path p = baseDirectory.getChild(realPath);

            if (p instanceof FilePath) {
                return new FilePathWuicResource(FilePath.class.cast(p), realPath, type);
            } else {
                throw new BadArgumentException(new IllegalArgumentException(String.format("%s is not a file", p)));
            }
        } catch (IOException ioe) {
            throw new StreamException(ioe);
        }
    }

    /**
     * {@inheritDoc}
     */
    public String toString() {
        return String.format("%s with base directory %s", getClass().getName(), baseDirectory);
    }
}
