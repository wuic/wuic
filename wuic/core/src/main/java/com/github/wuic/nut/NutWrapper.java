/*
 * "Copyright (c) 2015   Capgemini Technology Services (hereinafter "Capgemini")
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

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * <p>
 * This class wraps a {@link com.github.wuic.nut.Nut} and implements {@link Nut} interface by delegating method calls.
 * Useful when you need to change only some method behavior.
 * </p>
 *
 * @author Guillaume DROUET
 * @version 1.0
 * @since 0.4.4
 */
public class NutWrapper extends AbstractNut implements ConvertibleNut {

    /**
     * The wrapped nut.
     */
    private ConvertibleNut wrapped;

    /**
     * <p>
     * Default constructor for serialization purpose.
     * </p>
     */
    public NutWrapper() {
    }

    /**
     * <p>
     * Builds a new instance.
     * </p>
     *
     * @param w teh wrapped nut
     */
    public NutWrapper(final ConvertibleNut w) {
        super(w);
        wrapped = w;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public InputStream openStream() throws NutNotFoundException {
        return wrapped.openStream();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<ConvertibleNut> getOriginalNuts() {
        return wrapped.getOriginalNuts();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return wrapped.getName();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setNutName(final String nutName) {
        wrapped.setNutName(nutName);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public NutType getNutType() {
        return wrapped.getNutType();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setNutType(final NutType nutType) {
        wrapped.setNutType(nutType);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void transform(final Pipe.OnReady... onReady) throws IOException {
        wrapped.transform(onReady);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onReady(final Pipe.OnReady onReady) {
        wrapped.onReady(onReady);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Pipe.OnReady> getReadyCallbacks() {
        return wrapped.getReadyCallbacks();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addTransformer(final Pipe.Transformer<ConvertibleNut> transformer) {
        wrapped.addTransformer(transformer);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Pipe.Transformer<ConvertibleNut>> getTransformers() {
        return wrapped.getTransformers();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addReferencedNut(ConvertibleNut referenced) {
        wrapped.addReferencedNut(referenced);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<ConvertibleNut> getReferencedNuts() {
        return wrapped.getReferencedNuts();
    }
}
