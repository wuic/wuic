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

import com.github.wuic.configuration.Configuration;
import com.github.wuic.engine.Engine;
import com.github.wuic.engine.EngineRequest;
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
import java.util.regex.Pattern;

/**
 * <p>
 * Abstraction of an inspector engine for text resources. This kind of engine inspects each resource of a request
 * to eventually alter their content or to extract other referenced resources.
 * </p>
 *
 * @author Guillaume DROUET
 * @version 1.0
 * @since 0.3.3
 */
public abstract class CGAbstractTextInspectorEngine extends Engine {

    /**
     * Configuration based on CSS.
     */
    private Configuration configuration;

    /**
     * <p>
     * Builds a new instance.
     * </p>
     *
     * @param config the configuration
     */
    public CGAbstractTextInspectorEngine(final Configuration config) {
        configuration = config;
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
                inspect(resource, request, retval, 0);
            }
        }

        if (getNext() != null) {
            return getNext().parse(new EngineRequest(retval, request.getContextPath(), request.getGroup()));
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
     * @param depth of the path starting at the context path to the path the resource will be loaded
     * @return the resource corresponding the inspected resource specified in parameter
     * @throws IOException if an I/O error occurs while reading
     */
    protected WuicResource inspect(final WuicResource resource, final EngineRequest request, final List<WuicResource> inspectedList, final int depth)
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
                os.write((inspectLine(line, resourceLocation, request, inspectedList, depth) + "\n").getBytes());
            }

            // Create and add the inspected resource with its transformations
            final WuicResource inspected = new ByteArrayWuicResource(os.toByteArray(), resource.getName(), resource.getFileType());
            inspected.setCacheable(inspected.isCacheable());
            inspected.setAggregatable(inspected.isAggregatable());
            inspected.setTextCompressible(inspected.isTextCompressible());
            inspected.setBinaryCompressible(inspected.isBinaryCompressible());

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
     * @param depth of the path starting at the context path to the path the resource will be loaded
     * @throws IOException if an I/O error occurs while reading
     * return the given line eventually transformed
     */
    protected String inspectLine(String line,
                                 String resourceLocation,
                                 EngineRequest request,
                                 List<WuicResource> extracted,
                                 int depth)
            throws IOException {
        // Use a builder to transform the line
        final StringBuffer retval = new StringBuffer();

        // Looking for import statements
        final Matcher matcher = getPattern().matcher(line);

        while (matcher.find()) {
            // Compute replacement, extract resource name and referenced resources
            final StringBuilder replacement = new StringBuilder();
            final String resourceName = appendTransformation(matcher, replacement, depth, resourceLocation);
            matcher.appendReplacement(retval, replacement.toString());

            List<WuicResource> res = request.getGroup().getResourceFactory().create(resourceName);

            // Process resource
            if (getNext() != null) {
                res = getNext().parse(new EngineRequest(res, request.getContextPath(), request.getGroup()));
            }

            // Add the resource and inspect it recursively
            for (WuicResource r : res) {
                // Evict aggregation for extracted values
                final WuicResource inspected = inspect(r, request, extracted, depth + resourceName.split("/").length - 1);
                inspected.setAggregatable(Boolean.FALSE);
                extracted.remove(inspected);
                extracted.add(inspected);
            }
        }

        matcher.appendTail(retval);

        return retval.toString();
    }

    /**
     * <p>
     * Gets the pattern to find text to be replaced inside the lines.
     * </p>
     *
     * @return the pattern to use
     */
    protected abstract Pattern getPattern();

    /**
     * <p>
     * Computes the replacement to be made inside the text for the given {@code Matcher} which its {@code find()}
     * method as just been called.
     * </p>
     *
     * @param matcher the matcher which provides found text thanks to its {@code group()} method.
     * @param replacement the text which will replace the matching text
     * @param depth of the path starting at the context path to the path the resource will be loaded
     * @param resourceLocation the location of the current resource
     * @return the resource name that was referenced in the macthing text
     */
    protected abstract String appendTransformation(Matcher matcher,
                                                   StringBuilder replacement,
                                                   int depth,
                                                   String resourceLocation);

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
