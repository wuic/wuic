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


package com.github.wuic.resource.impl.classpath;

import com.github.wuic.FileType;
import com.github.wuic.exception.wrapper.BadArgumentException;
import com.github.wuic.exception.wrapper.StreamException;
import com.github.wuic.resource.WuicResource;
import com.github.wuic.resource.WuicResourceProtocol;
import com.github.wuic.resource.impl.InputStreamWuicResource;
import com.github.wuic.util.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * <p>
 * A {@link com.github.wuic.resource.WuicResourceProtocol} implementation for classpath accesses.
 * </p>
 *
 * @author Guillaume DROUET
 * @version 1.3
 * @since 0.3.1
 */
public class ClasspathWuicResourceProtocol implements WuicResourceProtocol {

    /**
     * Logger.
     */
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    /**
     * Classpath entry for the base path.
     */
    private URL classPathEntry;

    /**
     * Base path to classpath root.
     */
    private String basePath;

    /**
     * <p>
     * Builds a new instance
     * </p>
     *
     * @param bp the base path in classpath
     */
    public ClasspathWuicResourceProtocol(final String bp) {
        this.basePath = bp;
        classPathEntry = getClass().getResource(basePath);

        if (classPathEntry == null) {
            throw new BadArgumentException(new IllegalArgumentException("Unable to find '" + basePath + "' in the classpath"));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<String> listResourcesPaths(final Pattern pattern) throws StreamException {

        final List<String> absolutePaths = IOUtils.lookupFileResources(classPathEntry.getFile(), "", pattern);
        final List<String> retval = new ArrayList<String>(absolutePaths.size());

        log.debug("Listing resources paths matching the pattern : {}", pattern.pattern());

        for (String absolutePath : absolutePaths) {
            if (!absolutePath.startsWith("/") && !"/".equals(basePath)) {
                absolutePath = "/".concat(absolutePath);
            }

            // Need to remove the absolute part of the path to retrieve the resource with getClass().getResourceAsStream(path)
            retval.add(absolutePath.replace(classPathEntry.getFile().replace('\\', '/'), ""));
        }

        return retval;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public WuicResource accessFor(final String realPath, final FileType type) throws StreamException {
        final StringBuilder cp = new StringBuilder();
        cp.append(basePath);

        if (!"/".endsWith(basePath) && !realPath.startsWith("/")) {
            cp.append("/");
        } else if ("/".endsWith(basePath) && realPath.startsWith("/")) {
            cp.deleteCharAt(basePath.length() - 1);
        }

        cp.append(realPath);

        return new InputStreamWuicResource(new ClasspathInputStreamOpener(cp.toString()), realPath, type);
    }

    /**
     * {@inheritDoc}
     */
    public String toString() {
        return String.format("%s with base path %s", getClass().getName(), basePath);
    }
}
