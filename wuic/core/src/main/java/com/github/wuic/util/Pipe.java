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


package com.github.wuic.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Deque;
import java.util.LinkedList;

/**
 * <p>
 * This class helps to chains transformers that read {@link InputStream} and writes some transformed bytes to a
 * {@link ByteArrayOutputStream}.
 * </p>
 *
 * @author Guillaume DROUET
 * @version 1.0
 * @since 0.5.0
 * @param <T> the type of convertible object
 */
public final class Pipe<T> {

    /**
     * <p>
     * This interface describes an object which is able to write into an {@link OutputStream} some transformed bytes
     * read from {@link InputStream}.
     * </p>
     *
     * <p>
     * When registered with the {@link Pipe}, the close method is invoked transparently.
     * </p>
     *
     * <p>
     * That transformer is also able to change the state of on 'convertible' object identified as the origin of the
     * source input stream.
     * </p>
     *
     * @author Guillaume DROUET
     * @version 1.0
     * @since 0.5.0
     * @param <T> the type of convertible object
     */
    public interface Transformer<T> {

        /**
         * <p>
         * Reads the stream and writes the transformed bytes.
         * </p>
         *
         * @param is the input
         * @param os the output
         * @param convertible the object that provides the original input stream
         * @throws IOException if an I/O error occurs
         */
        void transform(InputStream is, OutputStream os, T convertible) throws IOException;

        /**
         * <p>
         * Indicates if the content generated when {@link #transform(InputStream, OutputStream, Object)} is called can
         * be aggregated to an other one. This is usually the case but won't be possible for instance when a magic
         * number should appears only at the beginning of the composite stream and not in each stream of the aggregation.
         * </p>
         *
         * @return {@code true} if aggregation is possible, {@code false} otherwise
         */
        boolean canAggregateTransformedStream();
    }

    /**
     * <p>
     * The default transformer just copies the {@link InputStream} to the {@link OutputStream} and produces a content
     * which can be aggregated to an other one.
     * </p>
     *
     * @author Guillaume DROUET
     * @version 1.0
     * @since 0.5.0
     * @param <T> the type of convertible object
     */
    public static class DefaultTransformer<T> implements Transformer<T> {

        /**
         * {@inheritDoc}
         */
        @Override
        public void transform(final InputStream is, final OutputStream os, final T convertible) throws IOException {
            IOUtils.copyStreamIoe(is, os);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean canAggregateTransformedStream() {
            return true;
        }
    }

    /**
     * Input stream of the pipe.
     */
    private InputStream inputStream;

    /**
     * Nodes that wrap registered {@link Transformer transformers}.
     */
    private Deque<Transformer<T>> transformers;

    /**
     * The convertible object.
     */
    private T convertible;

    /**
     * <p>
     * Creates a new instance with an input.
     * </p>
     *
     * @param c the convertible bound to the input stream
     * @param is the {@link InputStream}
     */
    public Pipe(final T c, final InputStream is) {
        convertible = c;
        inputStream = is;
        transformers = new LinkedList<Transformer<T>>();
    }

    /**
     * <p>
     * Registers the given {@link Transformer} to the pipe. The {@link InputStream} sent to this transformer with be
     * connected to the {@link OutputStream} of the {@link Transformer} previously registered.
     * </p>
     *
     * @param transformer the transformer
     * @throws IOException if streams could not be piped
     */
    public void register(final Transformer<T> transformer) throws IOException {
        if (!transformers.isEmpty()
                && !transformers.getLast().canAggregateTransformedStream()
                && transformer.canAggregateTransformedStream()) {
            throw new IllegalArgumentException(
                    "You can't add a transformer which produces a stream which could be aggregated after a stream which doesn't.");
        }

        transformers.addLast(transformer);
    }

    /**
     * <p>
     * Executes this pipe by writing to the given {@link OutputStream} the result generated by all
     * registered {@link Transformer transformers}.
     * </p>
     *
     * @throws IOException if an I/O error occurs
     */
    public void execute(final OutputStream os) throws IOException {
        if (transformers.isEmpty()) {
            IOUtils.copyStreamIoe(inputStream, os);
            return;
        }

        InputStream is = inputStream;

        for (final Transformer<T> t : transformers) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();

            if (!t.equals(transformers.getLast())) {
                try {
                    t.transform(is, out, convertible);
                } finally {
                    IOUtils.close(is);
                }

                is = new ByteArrayInputStream(out.toByteArray());
            } else {
                try {
                    t.transform(is, os, convertible);
                } finally {
                    IOUtils.close(is);
                }
            }
        }
    }
}
