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


package com.github.wuic.nut;

import com.github.wuic.NutType;
import com.github.wuic.exception.NutNotFoundException;
import com.github.wuic.util.Pipe;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

/**
 * <p>
 * Base class for {@link ConvertibleNut} implementation with state management (attributes like transformers or referenced
 * nuts). Transformation is delegated to subclass.
 * </p>
 *
 * @author Guillaume DROUET
 * @version 1.0
 * @since 0.5.0
 */
public abstract class AbstractConvertibleNut extends AbstractNut implements ConvertibleNut {

    /**
     * The path name.
     */
    private String nutName;

    /**
     * Returns all the referenced nuts.
     */
    private List<ConvertibleNut> referencedNuts;

    /**
     * The original nuts.
     */
    private List<ConvertibleNut> originalNuts;

    /**
     * Some transformers.
     */
    private List<Pipe.Transformer<ConvertibleNut>> transformers;

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
        setNutName(o.getInitialName());

        if (ConvertibleNut.class.isAssignableFrom(o.getClass())) {
            transformers = ConvertibleNut.class.cast(o).getTransformers();
        }
    }


    /**
     * <p>
     * Builds a new instance.
     * </p>
     *
     * @param name the nut's name
     * @param ft the nut's type
     * @param comp compressed or not
     * @param c cacheable or not
     * @param a aggregatable or not
     * @param v version number
     */
    protected AbstractConvertibleNut(final String name,
                                     final NutType ft,
                                     final Boolean comp,
                                     final Boolean c,
                                     final Boolean a,
                                     final Future<Long> v) {
        super(name, ft, comp, c, a, v);
        setNutName(name);
    }

    /**
     * <p>
     * Sets the original nuts.
     * </p>
     *
     * @param o the original nuts
     */
    protected void setOriginalNuts(final List<ConvertibleNut> o) {
        this.originalNuts = o;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Pipe.Transformer<ConvertibleNut>> getTransformers() {
        return transformers;
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
    public void addTransformer(final Pipe.Transformer<ConvertibleNut> transformer) {
        if (transformers == null) {
            transformers = new ArrayList<Pipe.Transformer<ConvertibleNut>>();
        }

        transformers.add(transformer);
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
    public void addReferencedNut(final ConvertibleNut referenced) {
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
    public List<ConvertibleNut> getOriginalNuts() {
        return originalNuts;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public InputStream openStream() throws NutNotFoundException {
        return wrap.openStream();
    }
}
