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

import com.github.wuic.nut.Nut;

import java.io.IOException;

/**
 * <p>
 * Utility class for HTML output.
 * </p>
 *
 * @author Guillaume DROUET
 * @version 1.1
 * @since 0.4.1
 */
public final class HtmlUtil {

    /**
     * Utility class. Hide default constructor.
     */
    private HtmlUtil() {

    }

    /**
     * <p>
     * Writes the import statement in HTML into the output stream for the given nut, using a default {@link UrlProviderFactory}.
     * </p>
     *
     * @param workflowContextPath the workflow context path
     * @param nut the nut to import
     * @throws java.io.IOException if an I/O error occurs
     */
    public static String writeScriptImport(final Nut nut, final String workflowContextPath) throws IOException {
        return writeScriptImport(nut, UrlUtils.urlProviderFactory().create(workflowContextPath));
    }

    /**
     * <p>
     * Writes the import statement in HTML into the output stream for the given nut.
     * </p>
     *
     * @param urlProvider the {@link UrlProvider}
     * @param nut the nut to import
     * @throws java.io.IOException if an I/O error occurs
     */
    public static String writeScriptImport(final Nut nut, final UrlProvider urlProvider) throws IOException {
        switch (nut.getNutType()) {
            case CSS :
                return cssImport(nut, urlProvider);

            case JAVASCRIPT :
                return javascriptImport(nut, urlProvider);

            default :
                return "";
        }
    }

    /**
     * <p>
     * Generates import for CSS script.
     * </p>
     *
     * @param urlProvider the {@link UrlProvider}
     * @param nut the CSS nut
     * @return the import
     */
    public static String cssImport(final Nut nut, final UrlProvider urlProvider) {
        final StringBuilder retval = new StringBuilder();

        retval.append("<link rel=\"stylesheet\" type=\"text/css\" href=\"");
        retval.append(urlProvider.getUrl(nut));
        retval.append("\" />");

        return retval.toString();
    }

    /**
     * <p>
     * Generates import for Javascript script.
     * </p>
     *
     * @param nut the Javascript nut
     * @param urlProvider the {@link UrlProvider}
     *
     * @return the import
     */
    public static String javascriptImport(final Nut nut, final UrlProvider urlProvider) {
        final StringBuilder retval = new StringBuilder();

        retval.append("<script type=\"text/javascript");
        retval.append("\" src=\"");
        retval.append(urlProvider.getUrl(nut));
        retval.append("\"></script>");

        return retval.toString();
    }
}
