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


package com.github.wuic.resource.impl;

import com.github.wuic.FileType;
import com.github.wuic.exception.WuicRfPropertyNotSupportedException;
import com.github.wuic.exception.wrapper.StreamException;
import com.github.wuic.resource.WuicResource;
import com.github.wuic.resource.WuicResourceFactory;
import com.github.wuic.resource.WuicResourceProtocol;
import com.github.wuic.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * <p>
 * Abstract implementation of what is a {@link WuicResourceFactory}.
 * </p>
 *
 * @author Guillaume DROUET
 * @version 1.1
 * @since 0.3.1
 */
public abstract class AbstractWuicResourceFactory implements WuicResourceFactory {

    /**
     * Protocol to use to access factories.
     */
    private WuicResourceProtocol wuicProtocol;

    /**
     * <p>
     * Builds a new instance thanks to a specific {@link WuicResourceProtocol protocol}.
     * </p>
     *
     * @param protocol the protocol allowing to access resources
     */
    public AbstractWuicResourceFactory(final WuicResourceProtocol protocol) {
        wuicProtocol = protocol;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<WuicResource> create(final String pathName) throws StreamException {
        final List<String> pathNames = computeRealPaths(pathName);
        final List<WuicResource> retval = new ArrayList<WuicResource>(pathNames.size());

        for (String p : pathNames) {
            final String ext = p.substring(p.lastIndexOf('.'));
            final FileType type = FileType.getFileTypeForExtension(ext);
            retval.add(wuicProtocol.accessFor(p, type));
        }

        return retval;
    }

    /**
     * {@inheritDoc}
     */
    public List<String> computeRealPaths(final String pathName) throws StreamException {
        final List<String> paths = wuicProtocol.listResourcesPaths(getPattern(pathName));
        final List<String> retval = new ArrayList<String>(paths.size());

        // Convention : we guarantee that all path begin with the slash character to ease research
        for (String p : paths) {
            retval.add(StringUtils.merge(new String[] { "/", p, }, "/"));
        }

        return retval;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setProperty(final String key, final String value) throws WuicRfPropertyNotSupportedException {
        throw new UnsupportedOperationException(String.format("This factory don't support any property. Given key is %s with value %s", key, value));
    }

    /**
     * <p>
     * Sets the protocol.
     * </p>
     *
     * @param wuicProtocol the new protocol
     */
    protected void setWuicProtocol(final WuicResourceProtocol wuicProtocol) {
        this.wuicProtocol = wuicProtocol;
    }

    /**
     * <p>
     * Gets a pattern for the given path.
     * </p>
     *
     * @param path the path
     * @return the {@code Pattern}
     */
    public abstract Pattern getPattern(final String path);

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return String.format("%s based on protocol %s", getClass().getName(), wuicProtocol);
    }

    /**
     * <p>
     * Factory that produces resource with a path which matches or contains one occurrence of the given path.
     * </p>
     *
     * @author Guillaume DROUET
     * @version 1.0
     * @since 0.3.1
     */
    public static class DefaultWuicResourceFactory extends AbstractWuicResourceFactory {

        /**
         * <p>
         * Builds a new instance for quoting thanks to a specific {@link WuicResourceProtocol protocol}.
         * </p>
         *
         * @param protocol the protocol allowing to access resources
         */
        public DefaultWuicResourceFactory(final WuicResourceProtocol protocol) {
            super(protocol);
        }

        /**
         * {@inheritDoc}
         */
        public Pattern getPattern(final String path) {
            return Pattern.compile(Pattern.quote(path));
        }
    }

    /**
     * <p>
     * Factory that produces resource with a path considered as a regular expression.
     * </p>
     *
     * @author Guillaume DROUET
     * @version 1.0
     * @since 0.3.1
     */
    public static class RegexWuicResourceFactory extends AbstractWuicResourceFactory {

        /**
         * <p>
         * Builds a new instance for regex thanks to a specific {@link WuicResourceProtocol protocol}.
         * </p>
         *
         * @param protocol the protocol allowing to access resources
         */
        public RegexWuicResourceFactory(final WuicResourceProtocol protocol) {
            super(protocol);
        }

        /**
         * {@inheritDoc}
         */
        public Pattern getPattern(final String path)  {
            return Pattern.compile(path);
        }
    }
}
