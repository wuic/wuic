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


package com.github.wuic.servlet.path;

import com.github.wuic.path.AbstractDirectoryPath;
import com.github.wuic.path.DirectoryPath;
import com.github.wuic.path.Path;
import com.github.wuic.util.IOUtils;
import com.github.wuic.util.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletContext;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Set;

/**
 * <p>
 * Represents a directory in the war file.
 * </p>
 *
 * @author Guillaume DROUET
 * @since 0.4.2
 */
public class WebappDirectoryPath extends AbstractDirectoryPath {

    /**
     * Logger.
     */
    private final Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * The servlet context.
     */
    private ServletContext context;

    /**
     * <p>
     * Creates a new instance.
     * </p>
     *
     * @param name the name
     * @param parent the parent
     * @param context the servlet context used to create children
     * @param charset charset to use
     */
    public WebappDirectoryPath(final String name, final DirectoryPath parent, final ServletContext context, final String charset) {
        super(name, parent, charset);
        this.context = context;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Path buildChild(final String child) throws IOException {
        final String absoluteParent = getAbsolutePath();
        final String absoluteChild = IOUtils.mergePath(absoluteParent, child);
        final Set paths = context.getResourcePaths(absoluteParent);

        if (paths == null) {
            logger.warn("Path {} in {} was not found in webapp directory path.", absoluteParent, absoluteChild);
            throw new FileNotFoundException();
        // If child is a directory, it will ends with a '/' and won't be match the absolute child path in returned set
        } else if (paths.contains(absoluteChild)) {
            return new WebappFilePath(child, this, context, getCharset());
        } else {
            return new WebappDirectoryPath(child, this, context, getCharset());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked")
    public String[] list() throws IOException {
        final Set<String> res = context.getResourcePaths(getAbsolutePath());
        final String[] retval = new String[res.size()];
        int i = 0;

        for (final String path : res) {
            // Removes the parent path part
            final int index = path.lastIndexOf('/', path.length() - NumberUtils.TWO);
            retval[i++] = index == -1 ? path : path.substring(index + 1);
        }

        return retval;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getLastUpdate() throws IOException {
        // In JEE, war is can't be reached so we are not able to get last timestamp
        return -1L;
    }
}
