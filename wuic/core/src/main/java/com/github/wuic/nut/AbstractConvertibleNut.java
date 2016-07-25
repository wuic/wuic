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
import com.github.wuic.exception.WuicException;
import com.github.wuic.util.Input;
import com.github.wuic.util.Pipe;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;

/**
 * <p>
 * Base class for {@link ConvertibleNut} implementation with state management (attributes like transformers or referenced
 * nuts). Transformation is delegated to subclass.
 * </p>
 *
 * @author Guillaume DROUET
 * @since 0.5.0
 */
public abstract class AbstractConvertibleNut extends AbstractNut implements ConvertibleNut {

    /**
     * The path name.
     */
    private String nutName;

    /**
     * The nut type.
     */
    private NutType nutType;

    /**
     * Returns all the referenced nuts.
     */
    private List<ConvertibleNut> referencedNuts;

    /**
     * The original nuts.
     */
    private Source source;

    /**
     * Some transformers.
     */
    private Set<Pipe.Transformer<ConvertibleNut>> transformers;

    /**
     * Callbacks.
     */
    private List<Pipe.OnReady> onReady;

    /**
     * Compressed or not.
     */
    private Boolean compressed;

    /**
     * Sub resource or not.
     */
    private boolean subResource;

    /**
     * Ignores the composite stream during transformation.
     */
    private boolean ignoreCompositeStreamOnTransformation;

    /**
     * Converted nut.
     */
    private Nut wrap;

    /**
     * <p>
     * Builds a new instance.
     * </p>
     *
     * @param o the converted nut
     */
    protected AbstractConvertibleNut(final Nut o) {
        super(o);
        wrap = o;

        if (ConvertibleNut.class.isAssignableFrom(o.getClass())) {
            final ConvertibleNut c = ConvertibleNut.class.cast(o);
            transformers = c.getTransformers() != null ? new LinkedHashSet<Pipe.Transformer<ConvertibleNut>>(c.getTransformers()) : null;
            onReady = c.getReadyCallbacks();
            source = c.getSource();
            ignoreCompositeStreamOnTransformation = c.ignoreCompositeStreamOnTransformation();
            setNutName(c.getName());
            setNutType(c.getNutType());
            referencedNuts = c.getReferencedNuts();
            setIsCompressed(c.isCompressed());
            setIsSubResource(c.isSubResource());
        } else {
            setNutName(o.getInitialName());
            setNutType(o.getInitialNutType());
            setIsCompressed(Boolean.FALSE);
            setIsSubResource(true);
            source = new SourceImpl();
            ignoreCompositeStreamOnTransformation = false;
        }
    }

    /**
     * <p>
     * Builds a new instance.
     * </p>
     *
     * @param name the nut's name
     * @param ft the nut's type
     * @param v version number
     * @param c is compressed or not
     */
    protected AbstractConvertibleNut(final String name,
                                     final NutType ft,
                                     final Future<Long> v,
                                     final Boolean c) {
        super(name, ft, v);
        setNutName(name);
        setNutType(ft);
        setIsCompressed(c);
        setIsSubResource(true);
        source = new SourceImpl();
        ignoreCompositeStreamOnTransformation = false;
    }

    /**
     * <p>
     * Sets the original nuts for the given {@link Source object}.
     * </p>
     *
     * @param o the original nuts
     */
    protected final void setOriginalNuts(final Source src, final List<ConvertibleNut> o) {
        for (final ConvertibleNut c : o) {
            src.addOriginalNut(c);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<Pipe.Transformer<ConvertibleNut>> getTransformers() {
        return transformers;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Pipe.OnReady> getReadyCallbacks() {
        return onReady;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final Boolean isCompressed() {
        return compressed;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void setIsCompressed(final Boolean compressed) {
        this.compressed = compressed;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void setNutName(final String nutName) {
        this.nutName = nutName;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void setNutType(final NutType nutType) {
        this.nutType = nutType;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void setIsSubResource(final boolean sr) {
        subResource = sr;
    }

    @Override
    public final boolean isSubResource() {
        return subResource;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addTransformer(final Pipe.Transformer<ConvertibleNut> transformer) {
        if (transformers == null) {
            transformers = new LinkedHashSet<Pipe.Transformer<ConvertibleNut>>();
        }

        transformers.add(transformer);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onReady(final Pipe.OnReady callback) {
        if (onReady == null) {
            onReady = new ArrayList<Pipe.OnReady>();
        }

        onReady.add(callback);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return nutName;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NutType getNutType() {
        return nutType;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addReferencedNut(final ConvertibleNut referenced) {
        if (referenced.isDynamic()) {
            WuicException.throwBadArgumentException(new IllegalArgumentException(
                    String.format("Nut '%s' is dynamic and could not be referenced by '%s'.", referenced.getName(), getName())));
        }

        if (referencedNuts == null) {
            referencedNuts = new ArrayList<ConvertibleNut>();
        }

        // Do not allow duplicate nuts (many nuts with same name)
        if (referencedNuts.contains(referenced)) {
            referencedNuts.remove(referenced);
        }

        referencedNuts.add(referenced);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<ConvertibleNut> getReferencedNuts() {
        return referencedNuts;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Source getSource() {
        return source;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setSource(final Source src) {
        setOriginalNuts(src, source.getOriginalNuts());
        this.source = src;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Input openStream() throws IOException {
        return wrap.openStream();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isDynamic() {
        return wrap.isDynamic();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getParentFile() {
        return wrap.getParentFile();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean ignoreCompositeStreamOnTransformation() {
        return ignoreCompositeStreamOnTransformation;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(final Object other) {
        if (other instanceof ConvertibleNut) {
            return ((ConvertibleNut) other).getName().equals(getName());
        } else {
            return Boolean.FALSE;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return getName().hashCode();
    }
}
