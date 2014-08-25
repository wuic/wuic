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

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

/**
 * <p>
 * This class helps to extract parameters from URL. The expected URL structure is described in {@link #MATCHER_MESSAGE}.
 * </p>
 *
 * @author Guillaume DROUET
 * @version 1.0
 * @since 0.5.0
 */
public final class UrlMatcher {

    /**
     * The expected URL.
     */
    public static final String MATCHER_MESSAGE = "Expected URL pattern: [ContextPath]/[workflowId]/[timestamp]/[nutName]";

    /**
     * Elements exploded from URL.
     */
    private String[] elements;

    /**
     * If the URL matches or not.
     */
    private boolean matches;

    /**
     * Index of value corresponding to workflow.
     */
    private int workflowIndex;

    /**
     * Index of value corresponding to name.
     */
    private int nameIndex;

    /**
     * <p>
     * Builds a new instance.
     * </p>
     *
     * @param requestUri the request URI
     */
    public UrlMatcher(final String requestUri) {
        elements = StringUtils.removeTrailing(requestUri, "/").split("/");
        matches = elements.length >= NumberUtils.THREE;

        if (matches) {
            if (elements.length > NumberUtils.THREE
                    && NumberUtils.isNumber(elements[elements.length - NumberUtils.TWO])) {
                workflowIndex = elements.length - NumberUtils.THREE;
            } else if (elements.length == NumberUtils.THREE
                    && NumberUtils.isNumber(elements[elements.length - NumberUtils.TWO])) {
                matches = false;
            } else {
                workflowIndex = elements.length - NumberUtils.TWO;
            }

            nameIndex = elements.length - 1;
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
        return URLDecoder.decode(elements[workflowIndex], "UTF-8");
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
        return URLDecoder.decode(elements[nameIndex], "UTF-8");
    }
}
