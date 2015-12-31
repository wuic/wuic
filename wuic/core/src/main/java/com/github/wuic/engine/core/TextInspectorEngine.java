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


package com.github.wuic.engine.core;

import com.github.wuic.NutType;
import com.github.wuic.engine.EngineRequest;
import com.github.wuic.engine.EngineRequestBuilder;
import com.github.wuic.engine.LineInspector;
import com.github.wuic.engine.LineInspectorListener;
import com.github.wuic.engine.NodeEngine;
import com.github.wuic.exception.WuicException;
import com.github.wuic.nut.CompositeNut;
import com.github.wuic.nut.ConvertibleNut;
import com.github.wuic.nut.filter.NutFilter;
import com.github.wuic.nut.filter.NutFilterHolder;
import com.github.wuic.util.CollectionUtils;
import com.github.wuic.util.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * <p>
 * Basic inspector engine for text nuts processing text line per line. This kind of engine inspects
 * each nut of a request to eventually alter their content or to extract other referenced nuts
 * thanks to a set of {@link LineInspector inspectors}.
 * </p>
 *
 * @author Guillaume DROUET
 * @version 1.5
 * @since 0.3.3
 */
public abstract class TextInspectorEngine
        extends NodeEngine
        implements NutFilterHolder, EngineRequestTransformer.RequireEngineRequestTransformer {

    /**
     * The inspectors of each line
     */
    private List<LineInspector> lineInspectors;

    /**
     * Inspects or not.
     */
    private Boolean doInspection;

    /**
     * The charset of inspected file.
     */
    private String charset;

    /**
     * <p>
     * Builds a new instance.
     * </p>
     *
     * @param inspect activate inspection or not
     * @param cs files charset
     * @param inspectors the line inspectors to use
     */
    public TextInspectorEngine(final Boolean inspect, final String cs, final LineInspector... inspectors) {
        lineInspectors = CollectionUtils.newList(inspectors);
        doInspection = inspect;
        charset = cs;
    }

    /**
     * <p>
     * Adds a new inspector.
     * </p>
     *
     * @param inspector the new inspector
     */
    public final void addInspector(final LineInspector inspector) {
        lineInspectors.add(inspector);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<ConvertibleNut> internalParse(final EngineRequest request) throws WuicException {
        if (works()) {
            for (final ConvertibleNut nut : request.getNuts()) {
                inspect(nut, request);
            }
        }

        return request.getNuts();
    }

    /**
     * <p>
     * Extracts from the given nut all the nuts referenced by the @import statement in CSS.
     * </p>
     *
     * @param nut the nut
     * @param request the initial request
     * @throws WuicException if an I/O error occurs while reading
     */
    protected void inspect(final ConvertibleNut nut, final EngineRequest request)
            throws WuicException {
        nut.addTransformer(new EngineRequestTransformer(request, this));
    }

    /**
     * <p>
     * Inspects the given line and eventually adds some extracted nuts to the nut referencing it.
     * </p>
     *
     * <p>
     * This method is recursive.
     * </p>
     *
     * @param line the line to be inspected
     * @param request the initial request
     * @param inspector the inspector to use
     * @param replacementInfoSet the collection where any referenced nut identified by the method will be added
     * @param cis a composite stream which indicates what nut owns the transformed text, {@code null} if the nut is not a composition
     * @param original the inspected nut
     * @throws WuicException if an I/O error occurs while reading
     */
    protected void inspectLine(final String line,
                               final EngineRequest request,
                               final LineInspector inspector,
                               final Set<LineInspector.ReplacementInfo> replacementInfoSet,
                               final CompositeNut.CompositeInputStream cis,
                               final ConvertibleNut original)
            throws WuicException {
        // Clean previous inspections
        inspector.newInspection();

        // Looking for matching statements
        inspector.inspect(new Listener(replacementInfoSet, inspector, original, request), line.toCharArray(), request, cis, original);
    }

    /**
     * <p>
     * Includes content in place of all nuts references in the specified {@code String}.
     * </p>
     *
     * @param line the line with references
     * @param replacementInfoList where replacement with references has been made
     * @param referencer the referencer
     * @return the replaced line
     * @throws IOException if an I/O error occurs
     */
    private String include(final String line,
                           final Set<LineInspector.ReplacementInfo> replacementInfoList,
                           final ConvertibleNut referencer)
            throws IOException {
        final StringBuilder stringBuilder = new StringBuilder(line);

        // Including from the end to the beginning of the line
        for (final LineInspector.ReplacementInfo replacementInfo : replacementInfoList) {

            // Just performs replacement if not referenced nuts has been found
            if (replacementInfo.getConvertibleNuts() == null) {
                replacementInfo.replace(stringBuilder);
                continue;
            }

            // Try inlining
            if (referencer.getInitialName().equals(replacementInfo.getReferencer().getInitialName())) {
                final String append = replacementInfo.asString();

                if (append != null) {
                    // Content resolve, perform inline
                    replacementInfo.replace(stringBuilder, append);
                } else {
                    // Content not resolved, adds inspected URL
                    replacementInfo.replace(stringBuilder);
                }
            }

            // Included nuts are already transformed
            for (final ConvertibleNut ref : replacementInfo.getConvertibleNuts()) {
                addReferenceNutNotTransformed(referencer, ref);
            }
        }

        return stringBuilder.toString();
    }

    /**
     * <p>
     * Adds the nuts in the replacement list to their referencer.
     * </p>
     *
     * @param convertibleNut the referencer
     * @param replacementInfoList the replacements that contains nuts to associate
     */
    private void populateReferencedNuts(final ConvertibleNut convertibleNut, final Set<LineInspector.ReplacementInfo> replacementInfoList) {
        for (final LineInspector.ReplacementInfo replacementInfo : replacementInfoList) {
            if (replacementInfo.getReferencer().getInitialName().equals(convertibleNut.getInitialName())) {
                for (final ConvertibleNut ref : replacementInfo.getConvertibleNuts()) {
                    convertibleNut.addReferencedNut(ref);
                    populateReferencedNuts(ref, replacementInfoList);
                }
            }
        }
    }

    /**
     * <p>
     * Adds the given 'ref' nut as a referenced nut of the specified nut if its {@link ConvertibleNut#isTransformed()}
     * method returns {@code false}. The method is called recursively on referenced nuts.
     * </p>
     *
     * @param convertibleNut the nut
     * @param ref the referenced nut
     */
    private void addReferenceNutNotTransformed(final ConvertibleNut convertibleNut, final ConvertibleNut ref) {
        if (!ref.isTransformed()) {
            convertibleNut.addReferencedNut(ref);
        }

        if (ref.getReferencedNuts() != null) {
            for (final ConvertibleNut r : ref.getReferencedNuts()) {
                addReferenceNutNotTransformed(convertibleNut, r);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Boolean works() {
        return doInspection;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setNutFilter(final List<NutFilter> nutFilters) {
        for (final LineInspector i : lineInspectors) {
            if (NutFilterHolder.class.isAssignableFrom(i.getClass())) {
                NutFilterHolder.class.cast(i).setNutFilter(nutFilters);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void transform(final InputStream is, final OutputStream os, final ConvertibleNut convertibleNut, final EngineRequest request)
            throws IOException {
        // Make sure that replacement are iterated in the right order
        final Set<LineInspector.ReplacementInfo> replacements = new TreeSet<LineInspector.ReplacementInfo>();

        // Read the content and compute the position in case of aggregation
        String line = IOUtils.readString(new InputStreamReader(is, charset));
        final CompositeNut.CompositeInputStream cis = (is instanceof CompositeNut.CompositeInputStream) ?
                CompositeNut.CompositeInputStream.class.cast(is) : null;

        for (final LineInspector inspector : lineInspectors) {
            try {
                inspectLine(line, request, inspector, replacements, cis, convertibleNut);
            } catch (WuicException we) {
                throw new IOException(we);
            }
        }

        if (!replacements.isEmpty()) {

            // Keep all rewritten URL in best effort, try to include otherwise
            if (!request.isBestEffort()) {
                line = include(line, replacements, convertibleNut);
            } else {
                // Performs replacements first
                final StringBuilder lineBuilder = new StringBuilder(line);

                for (final LineInspector.ReplacementInfo replacementInfo : replacements) {
                    replacementInfo.replace(lineBuilder);
                }

                line = lineBuilder.toString();

                // Populate
                populateReferencedNuts(convertibleNut, replacements);
            }
        }

        os.write((line + '\n').getBytes());
    }

    /**
     * <p>
     * Internal implementation of {@link LineInspector}.
     * </p>
     *
     * @author Guillaume DROUET
     * @since 0.5.3
     */
    private class Listener implements LineInspectorListener {

        /**
         * The replacements populated when the listener is notified.
         */
        private final Set<LineInspector.ReplacementInfo> replacementInfoSet;

        /**
         * The inspector that notifies this listener.
         */
        private LineInspector inspector;

        /**
         * The nut original providing the inspected stream.
         */
        private ConvertibleNut originalNut;

        /**
         * The request that initiated the inspection.
         */
        private final EngineRequest request;

        /**
         * <p>
         * Builds a new instance.
         * </p>
         *
         * @param replacementInfoSet see {@link #replacementInfoSet}
         * @param inspector see {@link #inspector}
         * @param originalNut see {@link #originalNut}
         * @param request see {@link #request}
         */
        public Listener(final Set<LineInspector.ReplacementInfo> replacementInfoSet,
                        final LineInspector inspector,
                        final ConvertibleNut originalNut,
                        final EngineRequest request) {
            this.replacementInfoSet = replacementInfoSet;
            this.inspector = inspector;
            this.originalNut = originalNut;
            this.request = request;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void onMatch(final String find,
                            final int start,
                            final int end,
                            final String replacement,
                            final List<? extends ConvertibleNut> extracted)
                throws WuicException {
            // Create replacement to perform later
            if (!replacement.equals(find)) {
                replacementInfoSet.add(inspector.replacementInfo(start, end, originalNut, extracted, replacement));
            }

            // Add the nut and inspect it recursively if it's a CSS path
            if (extracted != null) {
                for (final ConvertibleNut c : extracted) {
                    if (c.getInitialNutType().equals(NutType.CSS)) {
                        inspect(c, new EngineRequestBuilder(request).nuts(extracted).build());
                    }
                }
            }
        }
    }
}
