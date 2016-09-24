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


package com.github.wuic.util;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

/**
 * <p>
 * This class helps to extract parameters from URL. The expected URL structure is described in {@link #MATCHER_MESSAGE}.
 * </p>
 *
 * <p>
 * Details:
 * <ul>
 *     <li>Workflow ID can't be a numeric value</li>
 *     <li>Nut name's first level path cannot be a numeric value (ex: 4000/foo.js)</li>
 *     <li>Version number must be a numeric value</li>
 *     <li>Version number is optional</li>
 * </ul>
 * </p>
 *
 * @author Guillaume DROUET
 * @since 0.5.0
 */
public final class UrlMatcher {

    /**
     * The expected URL.
     */
    public static final String MATCHER_MESSAGE = "Expected URL pattern: [workflowId]/[timestamp]/[nutName] (see javadoc for more details)";

    /**
     * If the URL matches or not.
     */
    private boolean matches;

    /**
     * Version number if any.
     */
    private String versionNumber;

    /**
     * Value corresponding to workflow.
     */
    private String workflowId;

    /**
     * Value corresponding to name.
     */
    private String name;

    /**
     * <p>
     * Builds a new instance.
     * </p>
     *
     * @param requestUri the request URI
     */
    UrlMatcher(final String requestUri) {
        final String uri = StringUtils.removeTrailing(requestUri, "/");
        final int workflowEndIndex = uri.indexOf('/');

        // No workflow ID
        if (workflowEndIndex == -1) {
            matches = false;
        } else {
            workflowId = uri.substring(0, workflowEndIndex);

            // Workflow ID is actually a version number
            if (NumberUtils.isNumber(workflowId)) {
                matches = false;
            } else {
                final int versionNumberIndex = uri.indexOf('/', workflowEndIndex + 1);

                // No version number
                if (versionNumberIndex == -1) {
                    if (uri.length() > uri.indexOf('/', workflowEndIndex + NumberUtils.TWO)) {
                        name = uri.substring(workflowEndIndex + 1);
                        matches = !NumberUtils.isNumber(name);
                    } else {
                        matches = false;
                    }
                } else {
                    final String version = uri.substring(workflowEndIndex + 1, versionNumberIndex);

                    // Check that version is a numeric value
                    if (NumberUtils.isNumber(version)) {
                        versionNumber = version;
                        name = uri.substring(versionNumberIndex + 1);
                        matches = true;
                    // Version number was actually the name's first level path
                    } else if (uri.length() > uri.indexOf('/', versionNumberIndex + NumberUtils.TWO)) {
                        matches = true;
                        versionNumber = null;
                        name = uri.substring(workflowEndIndex + 1);
                    } else {
                        // No nut name
                        matches = false;
                    }
                }
            }

            // Additional rule: check that first level path is not a numeric value
            if (name != null) {
                final int slashIndex = name.indexOf('/');

                if (slashIndex != -1 && NumberUtils.isNumber(name.substring(0, slashIndex))) {
                    matches = false;
                }
            }
        }
    }

    /**
     * <p>
     * Indicates if the URL matches.
     * </p>
     *
     * @return {@code true} if URL matches, {@code false} otherwise
     */
    public boolean matches() {
        return matches;
    }

    /**
     * <p>
     * Gets the workflow ID.
     * </p>
     *
     * @return the workflow ID
     * @throws UnsupportedEncodingException if UTF-8 is not supported
     */
    public String getWorkflowId() throws UnsupportedEncodingException {
        return URLDecoder.decode(workflowId, "UTF-8");
    }

    /**
     * <p>
     * Gets the nut name.
     * </p>
     *
     * @return the nut name
     * @throws UnsupportedEncodingException if UTF-8 is not supported
     */
    public String getNutName() throws UnsupportedEncodingException {
        return URLDecoder.decode(name, "UTF-8");
    }

    /**
     * <p>
     * Gets the version number.
     * </p>
     *
     * @return the version number, {@code null} if not set
     * @throws UnsupportedEncodingException if UTF-8 is not supported
     */
    public String getVersionNumber() throws UnsupportedEncodingException {
        return versionNumber == null ? null : URLDecoder.decode(versionNumber, "UTF-8");
    }
}
