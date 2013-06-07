/*
 * "Copyright (c) 2013   Capgemini Technology Services (hereinafter "Capgemini")
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


package com.github.wuic.engine.impl.embedded;

import com.github.wuic.FileType;
import com.github.wuic.configuration.Configuration;
import com.github.wuic.engine.Engine;
import com.github.wuic.engine.EngineRequest;
import com.github.wuic.engine.LineInspector;
import com.github.wuic.resource.WuicResource;
import com.github.wuic.resource.impl.ByteArrayWuicResource;
import com.github.wuic.util.IOUtils;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

/**
 * <p>
 * Basic inspector engine for text resources processing text line per line. This kind of engine inspects
 * each resource of a request to eventually alter their content or to extract other referenced resources
 * thanks to a set of {@link LineInspector inspectors}.
 * </p>
 *
 * @author Guillaume DROUET
 * @version 1.0
 * @since 0.3.3
 */
public class CGTextInspectorEngine extends Engine {

    /**
     * Configuration based on CSS.
     */
    private Configuration configuration;

    /**
     * The inspectors of each line
     */
    private LineInspector[] lineInspectors;

    /**
     * <p>
     * Builds a new instance.
     * </p>
     *
     * @param config the configuration
     * @param inspectors the line inspectors to use
     */
    public CGTextInspectorEngine(final Configuration config, final LineInspector... inspectors) {
        configuration = config;
        lineInspectors = inspectors;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<WuicResource> parse(final EngineRequest request) throws IOException {
        // Will contains both group's resources eventually modified or extracted resources.
        final List<WuicResource> retval = new ArrayList<WuicResource>();

        if (works()) {
            for (WuicResource resource : request.getResources()) {
                if (resource.isAggregatable() && request.getGroup().getConfiguration().aggregate()) {
                    inspect(resource, request, retval);
                } else {
                    inspect(resource, request, retval);
                }
            }
        }

        if (getNext() != null) {
            return getNext().parse(new EngineRequest(retval, request));
        } else {
            return retval;
        }
    }

    /**
     * <p>
     * Extracts from the given resource all the resources referenced by the @import statement in CSS.
     * </p>
     *
     * <p>
     * This method is recursive.
     * </p>
     *
     * @param inspectedList the list where the inspected resources should be stored
     * @param resource the resource
     * @param request the initial request
     * @return the resource corresponding the inspected resource specified in parameter
     * @throws IOException if an I/O error occurs while reading
     */
    protected WuicResource inspect(final WuicResource resource, final EngineRequest request, final List<WuicResource> inspectedList)
            throws IOException {
        // Extracts the location where resource is listed in order to compute the location of the extracted imported resources
        final int lastIndexOfSlash = resource.getName().lastIndexOf("/") + 1;
        final String name = resource.getName();
        final String resourceLocation = lastIndexOfSlash == 0 ? "" : name.substring(0, lastIndexOfSlash);

        BufferedReader br = null;
        String line;

        try {
            // Read the file line per line
            br = new BufferedReader(new InputStreamReader(resource.openStream(), configuration.charset()));

            // Reads each line and keep the transformations in memory
            final ByteArrayOutputStream os = new ByteArrayOutputStream();

            while ((line = br.readLine()) != null) {
                for (LineInspector inspector : lineInspectors) {
                    line = inspectLine(line, resourceLocation, request, inspectedList, inspector);
                }

                os.write((line + "\n").getBytes());
            }

            // Create and add the inspected resource with its transformations
            final WuicResource inspected = new ByteArrayWuicResource(os.toByteArray(), resource.getName(), resource.getFileType());
            inspected.setCacheable(resource.isCacheable());
            inspected.setAggregatable(resource.isAggregatable());
            inspected.setTextCompressible(resource.isTextCompressible());
            inspected.setBinaryCompressible(resource.isBinaryCompressible());

            inspectedList.add(inspected);

            return inspected;
        } finally {
            IOUtils.close(br);
        }
    }

    /**
     * <p>
     * Inspects the given line and eventually returns some extracted resources.
     * </p>
     *
     * <p>
     * This method is recursive.
     * </p>
     *
     * @param line the line to be inspected
     * @param resourceLocation the location of the resource
     * @param request the initial request
     * @param extracted the list where extracted resources should be added
     * @param inspector the inspector to use
     * @throws IOException if an I/O error occurs while reading
     * return the given line eventually transformed
     */
    protected String inspectLine(final String line,
                                 final String resourceLocation,
                                 final EngineRequest request,
                                 final List<WuicResource> extracted,
                                 final LineInspector inspector)
            throws IOException {
        // Use a builder to transform the line
        final StringBuffer retval = new StringBuffer();

        // Looking for import statements
        final Matcher matcher = inspector.getPattern().matcher(line);

        while (matcher.find()) {
            // Compute replacement, extract resource name and referenced resources
            final StringBuilder replacement = new StringBuilder();
            final String resourceName = inspector.appendTransformation(matcher, replacement, request.getRequestPath() + "/" + request.getGroup().getId(),
                    resourceLocation, request.getGroup().getResourceFactory());
            matcher.appendReplacement(retval, replacement.toString());

            List<WuicResource> res = request.getGroup().getResourceFactory().create(resourceName);

            // Process resource
            if (getNext() != null) {
                res = getNext().parse(new EngineRequest(res, request));
            }

            // Add the resource and inspect it recursively if it's a CSS file
            for (WuicResource r : res) {
                WuicResource inspected = r;

                if (r.getFileType().equals(FileType.CSS)) {
                    // Depth should take care or current value and relative position of the original resource
                    inspected = inspect(r, request, extracted);
                }

                configureExtracted(inspected);
                extracted.remove(inspected);
                extracted.add(inspected);
            }
        }

        matcher.appendTail(retval);

        return retval.toString();
    }

    /**
     * <p>
     * Configures the given extracted resources to know if it should be aggregated, compressed, cached, etc.
     * </p>
     *
     * @param resource the resource to configure
     */
    private void configureExtracted(final WuicResource resource) {
        resource.setAggregatable(Boolean.FALSE);
        resource.setTextCompressible(resource.getFileType().isText());
        resource.setBinaryCompressible(!resource.getFileType().isText());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Configuration getConfiguration() {
        return configuration;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Boolean works() {

        // TODO : create an engine's property after refactoring targeted for v0.4
        return Boolean.TRUE;
    }
}
