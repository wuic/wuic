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

import com.github.wuic.ClassPathResourceResolver;
import com.github.wuic.ClassPathResourceResolverHandler;
import com.github.wuic.NutType;
import com.github.wuic.engine.EngineRequest;
import com.github.wuic.engine.EngineService;
import com.github.wuic.engine.EngineType;
import com.github.wuic.engine.NodeEngine;
import com.github.wuic.exception.WuicException;
import com.github.wuic.nut.ConvertibleNut;
import com.github.wuic.nut.NotReachableNut;
import com.github.wuic.util.CollectionUtils;
import com.github.wuic.util.IOUtils;
import com.github.wuic.util.NumberUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <p>
 * This engine should be used for non-dynamic WUIC features. Actually, static means that WUIC is not able
 * to access nuts. This engine considers that nuts have been already processed and that their links are stored
 * in a file inside the classpath. This file should be generated at build time with the maven plugin.
 * </p>
 *
 * <p>
 * When this engine is called, it just looks for nuts paths and then return it. If file that contains nuts path
 * has not been found, then an exception is thrown. Consequently, the engine won't call any next engine in the
 * chain of responsibility.
 * </p>
 *
 * @author Guillaume DROUET
 * @since 0.4.1
 */
@EngineService(injectDefaultToWorkflow = false, isCoreEngine = true)
public class StaticEngine extends NodeEngine implements ClassPathResourceResolverHandler {

    /**
     * File pattern.
     */
    public static final String STATIC_WORKFLOW_FILE = "/wuic-static/%s";

    /**
     * Pattern that matches a key/value pair pattern. One key/value pair per line is detected.
     */
    public static final Pattern PATTERN_KEY_VALUE = Pattern.compile("(.+)[ ]\"*((?<=\")[^\"]+(?=\")|([^" + System.getProperty("line.separator") + "]+))\"*");

    /**
     * Cached workflow already retrieved.
     */
    private Map<String, List<ConvertibleNut>> retrievedWorkflow;

    /**
     * The classpath resource resolver.
     */
    private ClassPathResourceResolver classpathResourceResolver;
    
    /**
     * <p>
     * Builds a new instance.
     * </p>
     */
    public StaticEngine() {
        retrievedWorkflow = new HashMap<String, List<ConvertibleNut>>();
    }

    /**
     * <p>
     * Gets the the result information associated to the given workflow processed at build time.
     * </p>
     *
     * @param classpathResourceResolver the {@link com.github.wuic.ClassPathResourceResolver} to use
     * @param workflowId the workflow ID
     * @return the data file
     * @throws WuicException if the workflow has not been found or could not be processed
     */
    public static List<ConvertibleNut> getNuts(final ClassPathResourceResolver classpathResourceResolver, final String workflowId)
            throws WuicException {
        final String fileName = String.format(STATIC_WORKFLOW_FILE, workflowId).substring(1);
        final InputStream is = classpathResourceResolver.getResourceAsStream(fileName);
        InputStreamReader isr = null;

        // Not well packaged
        if (is == null) {
            WuicException.throwStaticWorkflowNotFoundException(workflowId);
        }

        try {
            isr = new InputStreamReader(is);
            final String paths = IOUtils.readString(isr);
            final Matcher matcher = PATTERN_KEY_VALUE.matcher(paths);
            final Map<Integer, ConvertibleNut> nutPerDepth = new LinkedHashMap<Integer, ConvertibleNut>();
            final List<ConvertibleNut> retval = new ArrayList<ConvertibleNut>();

            // Read each file associated to its type
            while (matcher.find()) {
                final NutType nutType = NutType.getNutTypeForExtension(matcher.group(NumberUtils.TWO));
                final String pathLine = matcher.group(1);
                int depth = 0;

                while (pathLine.charAt(depth) == '\t') {
                    depth++;
                }

                final String path = pathLine.substring(depth);
                final ConvertibleNut nut;

                if (!path.contains("http://")) {
                    final int versionDelimiterIndex = path.indexOf('/');
                    final String name = path.substring(versionDelimiterIndex + 1);
                    final Long version = Long.parseLong(path.substring(0, versionDelimiterIndex));
                    nut = new NotReachableNut(name, nutType, workflowId, version);
                } else {
                    nut = new NotReachableNut(path, nutType, workflowId, 0L);
                }

                if (depth == 0) {
                    retval.add(nut);
                } else {
                    nutPerDepth.get(depth - 1).addReferencedNut(nut);
                }

                nutPerDepth.put(depth, nut);
            }

            return retval;
        } catch (IOException ioe) {
            WuicException.throwWuicException(ioe);
            return null;
        } finally {
            IOUtils.close(is, isr);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setClasspathResourceResolver(final ClassPathResourceResolver crr) {
        classpathResourceResolver = crr;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected List<ConvertibleNut> internalParse(final EngineRequest request) throws WuicException {
        List<ConvertibleNut> retval = retrievedWorkflow.get(request.getWorkflowId());

        // Workflow already retrieved
        if (retval != null) {
            return retval;
        } else {
            retval = getNuts(classpathResourceResolver, request.getWorkflowId());
            retrievedWorkflow.put(request.getWorkflowId(), retval);
        }

        return retval;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<NutType> getNutTypes() {
        return CollectionUtils.newList(NutType.values());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public EngineType getEngineType() {
        return EngineType.CACHE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Boolean works() {
        return Boolean.TRUE;
    }
}
