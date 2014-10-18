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

import com.github.wuic.exception.NutNotFoundException;
import com.github.wuic.exception.wrapper.StreamException;
import com.github.wuic.util.IOUtils;
import com.github.wuic.util.Pipe;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;

/**
 * <p>
 * A transformed nut wraps a {@link ConvertibleNut} and considers its input stream as the result of the transformation.
 * Consequently, not transformers will be used and no changes of the state will be accepted.
 * </p>
 *
 * <p>
 * The instances could be serialized so the wrapped nut must be a {@link Serializable}.
 * </p>
 *
 * @author Guillaume DROUET
 * @version 1.0
 * @since 0.5.0
 */
public class TransformedNut extends NutWrapper implements Serializable {

    /**
     * <p>
     * Builds a new instance.
     * </p>
     *
     * @param w the wrapped nut
     */
    public TransformedNut(final ConvertibleNut w) {
        super(w);

        if (!Serializable.class.isAssignableFrom(w.getClass())) {
            throw new IllegalArgumentException("Transformed nut is serializable so its wrapped nut must be serializable.");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setNutName(final String nutName) {
        throw new IllegalStateException("Can't change the name of an already transformed nut.");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void transform(final OutputStream outputStream) throws IOException {
        InputStream is = null;

        try {
            is = openStream();
            IOUtils.copyStream(is, outputStream);
        } catch (NutNotFoundException nnfe) {
            throw new IOException(nnfe);
        } catch (StreamException se) {
            throw new IOException(se);
        } finally {
            IOUtils.close(is, outputStream);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addTransformer(final Pipe.Transformer transformer) {
        throw new IllegalStateException("Can't add a transformer to an already transformed nut.");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addReferencedNut(final ConvertibleNut referenced) {
        throw new IllegalStateException("Can't add a referenced nut to an already transformed nut.");
    }
}
