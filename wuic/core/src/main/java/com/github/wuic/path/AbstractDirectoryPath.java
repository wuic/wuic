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


package com.github.wuic.path;

import com.github.wuic.path.core.SimplePath;
import com.github.wuic.util.IOUtils;
import com.github.wuic.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;

/**
 * <p>
 * Abstract implementation of a {@link DirectoryPath}. Subclasses just need to implement the
 * {@link AbstractDirectoryPath#buildChild(String)} method.
 * </p>
 *
 * @author Guillaume DROUET
 * @since 0.3.4
 */
public abstract class AbstractDirectoryPath extends SimplePath implements DirectoryPath {

    /**
     * Logger.
     */
    private final Logger log = LoggerFactory.getLogger(getClass());

    /**
     * <p>
     * Builds a new instance.
     * </p>
     *
     * @param charset charset
     * @param name the name
     * @param parent the parent
     */
    public AbstractDirectoryPath(final String name, final DirectoryPath parent, final String charset) {
        super(name, parent, charset);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Path getChild(final String path) throws IOException {
        log.debug("Create path child for path '{}'", path);

        String normalized = StringUtils.removeTrailing(IOUtils.normalizePathSeparator(path), IOUtils.STD_SEPARATOR);
        final String[] children = normalized.split(IOUtils.STD_SEPARATOR);

        // Build direct child
        String child = children[0];
        log.debug("Current directory path is {}", getAbsolutePath());
        log.debug("Going to build child {}", child);

        Path retval;

        // Double dot case
        if ("..".equals(child)) {
            retval = getParent();
        } else {
            retval = buildChild(child);
        }

        if (retval != null && children.length > 1) {
            final String remaining = IOUtils.mergePath(Arrays.copyOfRange(children, 1, children.length));

            if (retval instanceof DirectoryPath) {
                return DirectoryPath.class.cast(retval).getChild(remaining);
            } else {
                final String message = String.format("Could not go through %s because %s is not a directory", remaining, child);
                throw new IllegalArgumentException(message);
            }
        } else {
            return retval;
        }
    }

    /**
     * <p>
     * Builds the path corresponding to the given direct child name of this directory.
     * </p>
     *
     * @param child the child name
     * @return the built path
     * @throws IOException if any I/O error occurs
     */
    protected abstract Path buildChild(final String child) throws IOException;
}
