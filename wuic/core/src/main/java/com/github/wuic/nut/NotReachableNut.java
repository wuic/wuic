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
import com.github.wuic.exception.WuicException;
import com.github.wuic.util.FutureLong;
import com.github.wuic.util.Pipe;

import java.io.IOException;
import java.io.InputStream;

/**
 * <p>
 * This class represents a nut that is not reachable. Usually instantiated by the
 * {@link com.github.wuic.engine.core.StaticEngine}.
 * </p>
 *
 * @author Guillaume DROUET
 * @version 1.1
 * @since 0.4.1
 */
public class NotReachableNut extends AbstractConvertibleNut {

    /**
     * The nut's heap.
     */
    private String heap;

    /**
     * <p>
     * Creates a new instance.
     * </p>
     *
     * @param name the nut name
     * @param nutType the nut type
     * @param heapId the nut's heap
     * @param version the version number
     */
    public NotReachableNut(final String name, final NutType nutType, final String heapId, final Long version) {
        super(name, nutType, new FutureLong(version), Boolean.FALSE);
        heap = heapId;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public InputStream openStream() throws IOException {
        WuicException.throwNutNotFoundException(getInitialName(), heap);
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void transform(final Pipe.OnReady... onReady) throws IOException {
        WuicException.throwNutNotFoundException(getInitialName(), heap);
    }

    /**
     *{@inheritDoc}
     */
    @Override
    public boolean isTransformed() {
        return false;
    }
}
