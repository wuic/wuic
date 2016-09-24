/*
 * Copyright (c) 2016   The authors of WUIC
 *
 * License/Terms of Use
 * Permission is hereby granted, free of charge and for the term of intellectual
 * property rights on the Software, to any person obtaining a copy of this software
 * and associated documentation files (the "Software"), to use, copy, modify and
 * propagate free of charge, anywhere in the world, all or part of the Software
 * subject to the following mandatory conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, PEACEFUL ENJOYMENT,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS
 * OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */


package com.github.wuic.nut;

import com.github.wuic.mbean.TransformationStat;
import com.github.wuic.util.IOUtils;
import com.github.wuic.util.InMemoryInput;
import com.github.wuic.util.Input;
import com.github.wuic.util.NutUtils;
import com.github.wuic.util.Pipe;
import com.github.wuic.util.TimerTreeFactory;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

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
    private final char[] charArray;

    /**
     * The charset.
     */
    private final String charset;

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
     * @param charset the charset
     * @throws IOException
     */
    public StaticSourceMapNut(final SourceMapNut o, final String charset) throws IOException {
        super(o);
        this.name = o.getName();
        this.originalNuts = new ArrayList<ConvertibleNut>();
        this.charset = charset;

        Input is = null;

        try {
            is = o.openStream();
            final Pipe.Execution e = is.execution();

            if (e.isText())  {
                charArray = Arrays.copyOf(e.getCharResult(), e.getContentLength());
            } else {
                charArray = IOUtils.toChars(Charset.forName(charset), e.getByteResult());
            }
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
    public Map<String, List<TransformationStat>> transform(final TimerTreeFactory timerTreeFactory, final Pipe.OnReady... onReady)
            throws IOException {
        return transform(onReady);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, List<TransformationStat>> transform(final Pipe.OnReady... onReady) throws IOException {

        // Notify callbacks
        NutUtils.invokeCallbacks(new Pipe.Execution(charArray, charset), onReady);

        return Collections.emptyMap();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Input openStream() throws IOException {
        return new InMemoryInput(charArray, charset);
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
