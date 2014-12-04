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


package com.github.wuic.engine.core;

import com.github.wuic.NutType;
import com.github.wuic.engine.EngineRequest;
import com.github.wuic.engine.LineInspector;
import com.github.wuic.engine.NodeEngine;
import com.github.wuic.exception.WuicException;
import com.github.wuic.nut.CompositeNut;
import com.github.wuic.nut.ConvertibleNut;
import com.github.wuic.nut.Nut;
import com.github.wuic.nut.filter.NutFilter;
import com.github.wuic.nut.filter.NutFilterHolder;
import com.github.wuic.util.CollectionUtils;
import com.github.wuic.util.IOUtils;
import com.github.wuic.util.Pipe;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

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
public abstract class TextInspectorEngine extends NodeEngine implements NutFilterHolder {

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

        if (getNext() != null) {
            return getNext().parse(request);
        } else {
            return request.getNuts();
        }
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
        nut.addTransformer(new TextInspectorTransformer(request));
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
     * @param referencedNuts the collection where any referenced nut identified by the method will be added
     * @param cis a composite stream which indicates what nut owns the transformed text, {@code null} if the nut is not a composition
     * @param original the inspected nut
     * @throws WuicException if an I/O error occurs while reading
     * @return the given line eventually transformed
     */
    protected String inspectLine(final String line,
                                 final EngineRequest request,
                                 final LineInspector inspector,
                                 final List<ConvertibleNut> referencedNuts,
                                 final CompositeNut.CompositeInputStream cis,
                                 final ConvertibleNut original)
            throws WuicException {

        // Use a builder to transform the line
        final StringBuffer retval = new StringBuffer();

        // Looking for import statements
        final Matcher matcher = inspector.getPattern().matcher(line);

        while (matcher.find()) {
            // Compute replacement, extract nut name and referenced nuts
            final StringBuilder replacement = new StringBuilder();
            final List<? extends ConvertibleNut> res = inspector.appendTransformation(matcher, replacement, request, cis, original);

            // Evict special $ character
            matcher.appendReplacement(retval, replacement.toString().replaceAll("\\$", ""));

            // Add the nut and inspect it recursively if it's a CSS path
            if (res != null) {
                for (final ConvertibleNut r : res) {
                    if (r.getNutType().equals(NutType.CSS)) {
                        inspect(r, new EngineRequest(res, request));
                    }

                    configureExtracted(r);
                    referencedNuts.add(r);
                }
            }
        }

        matcher.appendTail(retval);

        return retval.toString();
    }

    /**
     * <p>
     * Configures the given extracted nuts to know if it should be aggregated, compressed, cached, etc.
     * </p>
     *
     * @param nut the nut to configure
     */
    private void configureExtracted(final Nut nut) {
        // TODO : is it really required ? Why ???
        nut.setAggregatable(Boolean.FALSE);
        nut.setTextReducible(nut.getNutType().isText());
        nut.setBinaryReducible(!nut.getNutType().isText());
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
     * <p>
     * This transformer applies inspection process.
     * </p>
     *
     * @author Guillaume DROUET
     * @version 1.0
     * @since 0.5.0
     */
    private final class TextInspectorTransformer extends Pipe.DefaultTransformer<ConvertibleNut> {

        /**
         * The request.
         */
        private EngineRequest request;

        /**
         * <p>
         * Builds a new instance.
         * </p>
         *
         * @param req the request
         */
        private TextInspectorTransformer(final EngineRequest req) {
            this.request = req;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void transform(final InputStream is, final OutputStream os, final ConvertibleNut convertibleNut)
                throws IOException {
            final List<ConvertibleNut> referencedNuts = new ArrayList<ConvertibleNut>();
            String line = IOUtils.readString(new InputStreamReader(is, charset));
            final CompositeNut.CompositeInputStream cis = (is instanceof CompositeNut.CompositeInputStream) ?
                    CompositeNut.CompositeInputStream.class.cast(is) : null;

            for (final LineInspector inspector : lineInspectors) {
                try {
                    line = inspectLine(line, request, inspector, referencedNuts, cis, convertibleNut);
                } catch (WuicException we) {
                    throw new IOException(we);
                }
            }

            os.write((line + "\n").getBytes());

            // Also add all the referenced nuts
            for (final ConvertibleNut ref : referencedNuts) {
                convertibleNut.addReferencedNut(ref);
            }
        }
    }
}
