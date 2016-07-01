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
import com.github.wuic.ProcessContext;
import com.github.wuic.exception.WuicException;
import com.github.wuic.nut.dao.NutDao;
import com.github.wuic.nut.sourcemap.FilePosition;
import com.github.wuic.nut.sourcemap.SourceMapConsumerV3;
import com.github.wuic.nut.sourcemap.SourceMapGeneratorV3;
import com.github.wuic.nut.sourcemap.SourceMapParseException;
import com.github.wuic.nut.sourcemap.proto.Mapping;
import com.github.wuic.util.IOUtils;
import com.github.wuic.util.Pipe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * This {@link SourceMapNut} implementation relies on source map implementation from google clojure and represents
 * the mapping between a set of original {@link Nut nuts} and the enclosing class, which is the result of the transformation
 * process.
 * </p>
 *
 * @author Guillaume DROUET
 * @since 0.5.3
 */
public class SourceMapNutImpl extends SourceMapNutAdapter implements SourceMapNut {

    /**
     * The extension used for source map.
     */
    private static final String EXTENSION = NutType.MAP.getExtensions()[0];

    /**
     * Logger.
     */
    private final Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * The nut owning the source map.
     */
    private final ConvertibleNut owner;

    /**
     * The original nuts.
     */
    private Map<String, ConvertibleNut> sources;

    /**
     * Generator.
     */
    private SourceMapGeneratorV3 generator;

    /**
     * Consumer.
     */
    private SourceMapConsumerV3 consumer;

    /**
     * Indicates if the consumer is synchronized with generator state.
     */
    private boolean upToDate;

    /**
     * Possibility to customize the owner and source map name.
     */
    private String customOwnerName;

    /**
     * <p>
     * Creates a new instance representing an empty source map..
     * </p>
     *
     * @param nut the nut owning the source map
     * @throws WuicException if source map can't be read
     */
    public SourceMapNutImpl(final ConvertibleNut nut) throws WuicException {
        super(nut.getName() + EXTENSION, NutType.MAP, nut.getVersionNumber());
        owner = nut;
        sources = new LinkedHashMap<String, ConvertibleNut>();
        generator = new SourceMapGeneratorV3();
        setSourceRoot();
        upToDate = false;
    }

    /**
     * <p>
     * Creates a new instance with given source mapping data.
     * Sources are discovered in the content and created from given {@link NutsHeap}.
     * </p>
     *
     * @param heap the heap that created the nuts
     * @param convertibleNut the convertible nut
     * @param nut the nut that represents this source map
     * @param processContext the process context
     * @throws WuicException if source map can't be read
     */
    public SourceMapNutImpl(final NutsHeap heap,
                            final ConvertibleNut convertibleNut,
                            final ConvertibleNut nut,
                            final ProcessContext processContext) throws WuicException {
        this(heap, convertibleNut, nut, processContext, true);
    }

    /**
     * <p>
     * Creates a new instance with given source mapping data and list of sources specified as parameter.
     * </p>
     *
     * @param heap the heap that created the nuts
     * @param convertibleNut the convertible nut
     * @param nut the nut that represents this source map
     * @param processContext the process context
     * @param resolveSources if sources should be resolved from read content or not
     * @throws WuicException if source map can't be read
     */
    public SourceMapNutImpl(final NutsHeap heap,
                            final ConvertibleNut convertibleNut,
                            final ConvertibleNut nut,
                            final ProcessContext processContext,
                            final boolean resolveSources) throws WuicException {
        super(nut);

        this.owner = convertibleNut;
        this.upToDate = false;
        this.sources = new LinkedHashMap<String, ConvertibleNut>();
        init(heap, processContext, nut, resolveSources);
    }

    /**
     * <p>
     * Sets the source root.
     * </p>
     */
    private void setSourceRoot() {
        // Build the source root
        final StringBuilder sourceRoot = new StringBuilder();
        final String name = customOwnerName == null ? owner.getName() : customOwnerName;
        final String[] paths = name.split("/");

        // Ignore first '/'
        for (int i = name.indexOf(0) == '/' ? 1 : 0; i < paths.length - 1; i++) {
            sourceRoot.append("../");
        }

        generator.setSourceRoot(sourceRoot.toString());
    }

    /**
     * <p>
     * Initializes the source mapping with the given existing source map.
     * Additional source can be added to the mapping by calling {@link #addSource(int, int, int, int, ConvertibleNut)}.
     * </p>
     *
     * @param nutsHeap the {@link NutsHeap} that produced the enclosing processed nut
     * @param processContext the current process context
     * @param sourceMapNut the new source map
     * @param resolveSources resolve sources from read content or not
     * @throws WuicException if source map can't be read
     */
    private void init(final NutsHeap nutsHeap,
                      final ProcessContext processContext,
                      final ConvertibleNut sourceMapNut,
                      final boolean resolveSources)
            throws WuicException {
        InputStream is = null;
        String sourceMap = null;

        // Read source map content
        try {
            is = sourceMapNut.openStream();
            sourceMap = IOUtils.readString(new InputStreamReader(is));
        } catch (IOException ioe) {
            WuicException.throwWuicException(ioe);
        } finally {
            IOUtils.close(is);
        }

        generator = new SourceMapGeneratorV3();
        consumer = new SourceMapConsumerV3();

        try {
            // Parse the existing source map
            consumer.parse(sourceMap);

            if (resolveSources) {
                resolveSources(nutsHeap, processContext);
            }

            generator.mergeMapSection(0, 0, sourceMap);
            setSourceRoot();

            // Consumer is now synchronized with consumer
            upToDate = true;
        } catch (SourceMapParseException smpe) {
            WuicException.throwBadStateException(new IllegalStateException("Invalid source map.", smpe));
        } catch (IOException ioe) {
            WuicException.throwBadStateException(new IllegalStateException("Unable to read original source.", ioe));
        }
    }

    /**
     * <p>
     * Resolves the original sources in the current consumer with the given {@link NutsHeap}.
     * </p>
     *
     * @param nutsHeap the heap
     * @param processContext the process context
     * @throws IOException if any I/O error occurs
     */
    private void resolveSources(final NutsHeap nutsHeap, final ProcessContext processContext) throws IOException {
        // Extract names from the source map
        final Collection<String> originalSourceNames = consumer.getOriginalSources();

        // Try to create a nut for each original source name
        for (final String path : originalSourceNames) {
            final String name = consumer.getSourceRoot() == null ? path : IOUtils.mergePath(consumer.getSourceRoot(), path);
            final List<Nut> res = nutsHeap.create(owner, name, NutDao.PathFormat.RELATIVE_FILE, processContext);

            if (res.isEmpty()) {
                logger.warn("{} is referenced as a relative file but it was not found with in the DAO.", name);
            } else {
                // Should contain one item
                final ConvertibleNut nut = new PipedConvertibleNut(res.get(0));
                nut.setNutName(name);
                sources.put(name, nut);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addSource(final int startLine, final int startColumn, final int endLine, final int endColumn, final ConvertibleNut nut) {
        InputStream is = null;

        try {
            // Update the generator
            if (nut.getSource() instanceof SourceMapNut) {
                is = SourceMapNut.class.cast(nut.getSource()).openStream();
                generator.mergeMapSection(startLine, startColumn, IOUtils.readString(new InputStreamReader(is)));
            } else {
                // First position inside the nut content...
                final FilePosition start = new FilePosition(startLine, startColumn);

                // ... to the last position inside the nut content
                final FilePosition end = new FilePosition(endLine, endColumn);

                // Range always refer to the position 0,0 inside source
                generator.addMapping(nut.getInitialName(), null, new FilePosition(0, 0), start, end);
            }
        } catch (SourceMapParseException smpe) {
            WuicException.throwBadStateException(new IllegalStateException("Bad source map format.", smpe));
        } catch (IOException ioe) {
            WuicException.throwBadStateException(new IllegalStateException("Unable to read source map.", ioe));
        } finally {
            IOUtils.close(is);
        }

        // The consumer is no more synchronized
        upToDate = false;
        sources.put(nut.getInitialName(), nut);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addOriginalNut(final ConvertibleNut convertibleNut) {
        sources.put(convertibleNut.getInitialName(), convertibleNut);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<ConvertibleNut> getOriginalNuts() {
        return new ArrayList<ConvertibleNut>(sources.values());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ConvertibleNut getNutAt(final int line, final int column) throws IOException {

        // No mapping is registered for this source map
        if (generator == null) {
            return null;
        }

        // Sources have been added and the consumer is not yet updated
        if (!upToDate) {
            consumer = new SourceMapConsumerV3();

            try {
                consumer.parse(toString());
            } catch (SourceMapParseException smpe) {
                WuicException.throwStreamException(new IOException("Unable to parse current mapping", smpe));
            }

            // Consumer will be reused for future access
            upToDate = true;
        }

        // Get the mapping
        final Mapping.OriginalMapping mapping = consumer.getMappingForLine(line + 1, column + 1);

        return (mapping != null) ? sources.get(mapping.getOriginalFile()) : null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public InputStream openStream() throws IOException {
        return new ByteArrayInputStream(toString().getBytes());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return customOwnerName == null ? getInitialName() : customOwnerName + EXTENSION;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setNutName(final String nutName) {
        final int index = nutName.lastIndexOf(EXTENSION);

        if (index == -1) {
            WuicException.throwBadArgumentException(new IllegalArgumentException(String.format("%s must ends with %d", nutName, index)));
        }

        customOwnerName = nutName.substring(0, index);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void transform(final Pipe.OnReady... onReady) throws IOException {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();

        InputStream is = null;

        try {
            is = openStream();
            IOUtils.copyStream(is, bos);
        } finally {
            IOUtils.close(is);
        }

        for (final Pipe.OnReady callback : onReady) {
            callback.ready(new Pipe.Execution(bos.toByteArray()));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        try {
            final StringBuilder stringBuilder = new StringBuilder();

            // If no mapping exist an empty mapping is written
            if (generator != null) {

                // Source map name
                final String name = customOwnerName == null ? owner.getName() : customOwnerName + EXTENSION;
                generator.appendTo(stringBuilder, name);
            } else {
                new SourceMapGeneratorV3().appendTo(stringBuilder, owner.getName());
            }

            return stringBuilder.toString();
        } catch (IOException ioe) {
            WuicException.throwBadStateException(ioe);
            return null;
        }
    }
}