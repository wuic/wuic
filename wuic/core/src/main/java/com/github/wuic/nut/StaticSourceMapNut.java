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

import com.github.wuic.util.IOUtils;
import com.github.wuic.util.Pipe;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * A {@link SourceMapNut} representation that can be serialized but can't be modified.
 * </p>
 *
 * @author Guillaume DROUET
 * @since 0.5.3
 */
public class StaticSourceMapNut extends SourceMapNutAdapter implements SourceMapNut {

    /**
     * The source map content.
     */
    private final byte[] byteArray;

    /**
     * The original nuts.
     */
    private final List<ConvertibleNut> originalNuts;

    /**
     * The display name.
     */
    private String name;

    /**
     * <p>
     * Builds a new instance.
     * </p>
     *
     * @param o the source map nut
     * @throws IOException
     */
    public StaticSourceMapNut(final SourceMapNut o) throws IOException {
        super(o);
        this.name = o.getName();
        this.originalNuts = new ArrayList<ConvertibleNut>();
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();

        InputStream is = null;

        try {
            is = o.openStream();
            IOUtils.copyStream(is, bos);
            byteArray = bos.toByteArray();
        } finally {
            IOUtils.close(is);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addSource(final int startLine, final int startColumn, final int endLine, final int endColumn, final ConvertibleNut nut) {
        throw new UnsupportedOperationException("Static sourcemap is not supposed to be modified.");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ConvertibleNut getNutAt(final int line, final int column) throws IOException {
        throw new UnsupportedOperationException("Static sourcemap is not able to provide source location.");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void transform(final Pipe.OnReady... onReady) throws IOException {
        for (final Pipe.OnReady cb : onReady) {
            cb.ready(new Pipe.Execution(byteArray));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public InputStream openStream() throws IOException {
        return new ByteArrayInputStream(byteArray);
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
    public void addOriginalNut(final ConvertibleNut convertibleNut) {
        originalNuts.add(convertibleNut);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return name;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setNutName(final String nutName) {
        throw new UnsupportedOperationException("Can't change the nut name");
    }
}
