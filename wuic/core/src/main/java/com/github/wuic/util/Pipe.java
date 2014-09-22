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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.LinkedList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * <p>
 * This class helps to chains transformers that read {@link InputStream} and writes some transformed bytes to a
 * {@link ByteArrayOutputStream}. {@link Transformer Transformers} can be chained internally with a
 * {@link PipedInputStream} and a {@link PipedOutputStream}.
 * </p>
 *
 * @author Guillaume DROUET
 * @version 1.0
 * @since 0.5.0
 */
public final class Pipe {

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
     * @author Guillaume DROUET
     * @version 1.0
     * @since 0.5.0
     */
    public interface Transformer {

        /**
         * <p>
         * Reads the stream and writes the transformed bytes.
         * </p>
         *
         * @param is the input
         * @param os the output
         * @throws IOException if an I/O error occurs
         */
        void transform(InputStream is, OutputStream os) throws IOException;
    }

    /**
     * <p>
     * Internal class that wraps {@link Transformer} and the stream to use.
     * </p>
     *
     * @author Guillaume DROUET
     * @version 1.0
     * @since 0.5.0
     */
    private static final class Node implements Runnable {

        /**
         * The input stream.
         */
        private InputStream inputStream;

        /**
         * The output stream.
         */
        private OutputStream outputStream;

        /**
         * The transformer that reads and writes.
         */
        private Transformer transformer;

        /**
         * <p>
         * Sets the input stream.
         * </p>
         *
         * @param is the input stream
         */
        private void setInputStream(final InputStream is) {
            this.inputStream = is;
        }

        /**
         * <p>
         * Sets the output stream.
         * </p>
         *
         * @param os the output stream
         */
        private void setOutputStream(final OutputStream os) {
            this.outputStream = os;
        }

        /**
         * <p>
         * Sets the transformer.
         * </p>
         *
         * @param t the transformer
         */
        private void setTransformer(final Transformer t) {
            this.transformer = t;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void run() {
            try {
                transformer.transform(inputStream, outputStream);
            } catch (IOException ioe) {
                throw new RuntimeException(ioe);
            } finally {
                IOUtils.close(inputStream, outputStream);
            }
        }
    }

    /**
     * Input stream of the pipe.
     */
    private InputStream inputStream;

    /**
     * Output stream of the pipe.
     */
    private ByteArrayOutputStream outputStream;

    /**
     * Nodes that wrap registered {@link Transformer transformers}.
     */
    private LinkedList<Node> nodes;

    /**
     * <p>
     * Creates a new instance with an input.
     * </p>
     *
     * @param is the {@link InputStream}
     */
    public Pipe(final InputStream is) {
        inputStream = is;
        outputStream = new ByteArrayOutputStream();
        nodes = new LinkedList<Node>();
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
    public void register(final Transformer transformer) throws IOException {
        final Node node = new Node();
        node.setOutputStream(outputStream);
        node.setTransformer(transformer);

        if (nodes.isEmpty()) {
            node.setInputStream(inputStream);
        } else {
            final PipedOutputStream pos = new PipedOutputStream();
            final Node prev = nodes.getLast();
            node.setInputStream(new PipedInputStream(pos));
            prev.setOutputStream(pos);
        }

        nodes.addLast(node);
    }

    /**
     * <p>
     * Executes this pipe by writing to the returned {@link ByteArrayOutputStream} the result generated by all
     * registered {@link Transformer transformers}.
     * </p>
     *
     * @return the pipe's output
     * @throws IOException if an I/O error occurs
     */
    public ByteArrayOutputStream execute() throws IOException {
        // Keep futures to wait result
        final Future[] futures = new Future[nodes.size()];
        int cpt = 0;

        // Execute transformers in separated threads
        for (final Node n : nodes) {
            futures[cpt++] = WuicScheduledThreadPool.getInstance().executeAsap(n);
        }

        // Wait for the end of execution of all transformers
        for (final Future<?> f : futures) {
            try {
                f.get();
            } catch (ExecutionException ee) {
                throw new IOException(ee);
            } catch (InterruptedException ie) {
                throw new IOException(ie);
            }
        }

        return outputStream;
    }
}
