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


package com.github.wuic.nut;

import com.github.wuic.NutType;
import com.github.wuic.util.FutureLong;
import com.github.wuic.util.NutUtils;
import com.github.wuic.util.Pipe;

import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;

/**
 * <p>
 * An adapter for {@link SourceMapNut} implementation providing correct behavior for most of the {@link ConvertibleNut}
 * methods.
 * </p>
 *
 * @author Guillaume DROUET
 * @since 0.5.3
 */
public abstract class SourceMapNutAdapter extends AbstractNut implements SourceMapNut {

    /**
     * <p>
     * Builds a new instance.
     * </p>
     *
     * @param name the name
     * @param ft the type
     * @param v the version
     */
    protected SourceMapNutAdapter(final String name, final NutType ft, final Future<Long> v) {
        super(name, ft, v instanceof FutureLong ? v : new FutureLong(NutUtils.getVersionNumber(v)));
    }

    /**
     * <p>
     * Builds a new instance.
     * </p>
     *
     * @param o the nut to copy
     */
    protected SourceMapNutAdapter(final Nut o) {
        this(o.getInitialName(), o.getInitialNutType(),  o.getVersionNumber());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final Source getSource() {
        throw new UnsupportedOperationException("Source has not source.");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void setSource(final Source source) {
        throw new UnsupportedOperationException("Source has not source.");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final NutType getNutType() {
        return getInitialNutType();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void setNutType(final NutType nutType) {
        throw new UnsupportedOperationException("Source type is not supposed to change.");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void onReady(final Pipe.OnReady onReady) {
        throw new UnsupportedOperationException("Source is not supposed to be transformed.");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final List<Pipe.OnReady> getReadyCallbacks() {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void addTransformer(final Pipe.Transformer<ConvertibleNut> transformer) {
        throw new UnsupportedOperationException("Source is not supposed to be transformed.");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final Set<Pipe.Transformer<ConvertibleNut>> getTransformers() {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void addReferencedNut(final ConvertibleNut referenced) {
        throw new UnsupportedOperationException("Source is not supposed to reference nuts.");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void setIsSubResource(final boolean subResource) {
        throw new UnsupportedOperationException("Source is not supposed to reference nuts.");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final List<ConvertibleNut> getReferencedNuts() {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final boolean isTransformed() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void setIsCompressed(final Boolean c) {
        throw new UnsupportedOperationException("Source is not supposed to be compressed.");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final Boolean isCompressed() {
        return Boolean.FALSE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final boolean isSubResource() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final boolean ignoreCompositeStreamOnTransformation() {
        return false;
    }
}
