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


package com.github.wuic.engine.core;

import com.github.wuic.NutType;
import com.github.wuic.engine.EngineRequest;
import com.github.wuic.engine.EngineService;
import com.github.wuic.engine.EngineType;
import com.github.wuic.engine.NodeEngine;
import com.github.wuic.engine.ScriptLineInspector;
import com.github.wuic.exception.WuicException;
import com.github.wuic.nut.CompositeNut;
import com.github.wuic.nut.ConvertibleNut;
import com.github.wuic.nut.SourceImpl;
import com.github.wuic.util.IOUtils;

import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * <p>
 * This compressor provides modest script compression capabilities.
 * It removes the comments and blank lines from the transformed script.
 * </p>
 *
 * @author Guillaume DROUET
 * @since 0.5.3
 */
@EngineService(injectDefaultToWorkflow = true, isCoreEngine = true)
public class ScriptCompressorEngine extends AbstractCompressorEngine {

    /**
     * <p>
     * Copies the the given char array to the output stream specified in parameter ignoring the blank lines.
     * </p>
     *
     * @param target the output stream
     * @param buffer the buffer to read
     * @param offset the buffer offset
     * @param length the content length
     * @param request the engine request that initiates compression
     * @param previousDelimiter the delimiter that lead to the given chunk of data creation
     */
    private void compress(final OutputStream target,
                          final char[] buffer,
                          final int offset,
                          final int length,
                          final EngineRequest request,
                          final ScriptLineInspector.Range.Delimiter previousDelimiter) {
        try {
            final Charset cs = Charset.forName(request.getCharset());

            // String literal detecting, we don't remove blank lines
            if (buffer[offset] == '`') {
                target.write(new String(buffer, offset, length).getBytes(cs));
                return;
            }

            boolean blankLine;

            // Safe operation: makes sure a new line follows removed comment
            if (ScriptLineInspector.Range.Delimiter.START_SINGLE_LINE_OF_COMMENT.equals(previousDelimiter)) {
                target.write(IOUtils.NEW_LINE.getBytes(cs));
                blankLine = true;
            } else {
                blankLine = false;
            }

            int mark = offset;

            for (int i = offset; i < offset + length; i++) {
                final char c = buffer[i];

                // To remove blank lines, do not copy tabs and white spaces
                if (c == '\t' || c == ' ') {
                    if (!blankLine) {
                        target.write(String.valueOf(c).getBytes(cs));
                    }
                } else if (c == '\n') {
                    // Won't copy the new line if it's blank
                    mark = i;
                    blankLine = true;
                } else if (c == '\r' && i < offset + length - 1 && buffer[i + 1] == '\n') {
                    // Handle \r\n
                    mark = ++i;
                    blankLine = true;
                } else if (blankLine) {
                    // Not a blank line, copy all the skipped tabs and white spaces
                    blankLine = false;
                    target.write(new String(buffer, mark, i - mark + 1).getBytes(cs));
                } else {
                    // We already know that we are not on a blank line
                    target.write(String.valueOf(c).getBytes(cs));
                }
            }
        } catch (IOException ioe) {
            WuicException.throwBadStateException(ioe);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean transform(final InputStream source, final OutputStream target, final ConvertibleNut convertibleNut, final EngineRequest request)
            throws IOException {
        // Source map is broken
        convertibleNut.setSource(new SourceImpl(convertibleNut.getSource()));

        // First read the stream
        final CharArrayWriter charArrayWriter = new CharArrayWriter();
        IOUtils.copyStreamToWriterIoe(source, charArrayWriter);
        final char[] chars = charArrayWriter.toCharArray();
        final AtomicReference<ScriptLineInspector.Range.Delimiter> previousDelimiter = new AtomicReference<ScriptLineInspector.Range.Delimiter>();

        // This inspector just matches everything that is not a comment
        final ScriptLineInspector lineInspector = new ScriptLineInspector.Adapter(ScriptLineInspector.ScriptMatchCondition.NO_COMMENT_SPLIT_LITERALS) {
            @Override
            public Range doFind(final char[] buffer,
                                final int offset,
                                final int length,
                                final EngineRequest request,
                                final CompositeNut.CompositeInputStream cis,
                                final ConvertibleNut originalNut,
                                final Range.Delimiter delimiter) {
                compress(target, buffer, offset, length, request, previousDelimiter.get());
                previousDelimiter.set(delimiter);
                return null;
            }
        };

        try {
            // Perform comment and blank line suppression
            lineInspector.inspect(chars, request, null, convertibleNut);
        } catch (WuicException we) {
            throw new IOException(we);
        }

        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<NutType> getNutTypes() {
        return Arrays.asList(NutType.JAVASCRIPT, NutType.CSS);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public EngineType getEngineType() {
        return EngineType.MINIFICATION;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Boolean works() {

        // Checks if any other engine already performs minification
        if (super.works()) {
            NodeEngine e = this;

            while ((e = e.getPrevious()) != null) {
                if (EngineType.MINIFICATION.equals(e.getEngineType()) && e.works()) {
                    return Boolean.FALSE;
                }
            }

            e = this;

            while ((e = e.getNext()) != null) {
                if (EngineType.MINIFICATION.equals(e.getEngineType()) && e.works()) {
                    return Boolean.FALSE;
                }
            }

            return Boolean.TRUE;
        }

        return Boolean.FALSE;
    }
}
