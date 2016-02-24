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


package com.github.wuic.util;

import com.github.wuic.nut.ConvertibleNut;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * <p>
 * Utility class for HTML output.
 * </p>
 *
 * @author Guillaume DROUET
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
    public static String writeScriptImport(final ConvertibleNut nut, final String workflowContextPath)
            throws IOException {
        return writeScriptImport(nut, UrlUtils.urlProviderFactory().create(workflowContextPath), new HashMap<String, String>());
    }

    /**
     * <p>
     * Writes the import statement in HTML into the output stream for the given nut, using a default {@link UrlProviderFactory}.
     * An {@link Map} of additional attributes is specified and <b>must not be read only since this method puts entries to it</b>.
     * </p>
     *
     * @param workflowContextPath the workflow context path
     * @param nut the nut to import
     * @param attributes some attributes to insert in the written script
     * @throws java.io.IOException if an I/O error occurs
     */
    public static String writeScriptImport(final ConvertibleNut nut, final String workflowContextPath, final Map<String, String> attributes)
            throws IOException {
        return writeScriptImport(nut, UrlUtils.urlProviderFactory().create(workflowContextPath), attributes);
    }

    /**
     * <p>
     * Writes the import statement in HTML into the output stream for the given nut and without additional attributes.
     * </p>
     *
     * @param urlProvider the {@link UrlProvider}
     * @param nut the nut to import
     * @throws java.io.IOException if an I/O error occurs
     */
    public static String writeScriptImport(final ConvertibleNut nut, final UrlProvider urlProvider) throws IOException {
        return writeScriptImport(nut, urlProvider, new HashMap<String, String>());
    }

    /**
     * <p>
     * Writes the import statement in HTML into the output stream for the given nut and with the given additional attributes.
     * <b>The map containing attributes must not be read only since this method adds entries to it</b>.
     * </p>
     *
     * @param urlProvider the {@link UrlProvider}
     * @param nut the nut to import
     * @param attributes some attributes to insert in the written script
     * @throws java.io.IOException if an I/O error occurs
     */
    public static String writeScriptImport(final ConvertibleNut nut, final UrlProvider urlProvider, final Map<String, String> attributes)
            throws IOException {
        final StringBuilder sb = new StringBuilder();
        final int insertIndex;

        switch (nut.getNutType()) {
            case CSS :
                insertIndex = cssImport(nut, urlProvider, sb, attributes);
                break;
            case JAVASCRIPT :
                insertIndex = javascriptImport(nut, urlProvider, sb, attributes);
                break;
            case JPG:
            case PNG :
                insertIndex = imgImport(nut, urlProvider, sb);
                break;
            case ICO :
                insertIndex = iconImport(nut, urlProvider, sb, attributes);
                break;
            default :
                return "";
        }

        for (final Map.Entry<String, String> attr : attributes.entrySet()) {
            sb.insert(insertIndex, '"')
                    .insert(insertIndex, attr.getValue())
                    .insert(insertIndex, '"')
                    .insert(insertIndex, '=')
                    .insert(insertIndex, attr.getKey())
                    .insert(insertIndex, " ");
        }

        return sb.toString();
    }

    /**
     * <p>
     * Generates import for icons.
     * </p>
     *
     * @param urlProvider the {@link UrlProvider}
     * @param nut the .ICO nut
     * @param sb the builder where statement is appended
     * @param attributes the attributes to be populated
     * @return the index where attributes could be inserted
     */
    public static int iconImport(final ConvertibleNut nut,
                                 final UrlProvider urlProvider,
                                 final StringBuilder sb,
                                 final Map<String, String> attributes) {
        attributes.put("rel", "shortcut");
        link(nut, urlProvider, sb);

        return "<link".length();
    }

    /**
     * <p>
     * Generates import for CSS script.
     * </p>
     *
     * @param urlProvider the {@link UrlProvider}
     * @param nut the CSS nut
     * @param sb the builder where statement is appended
     * @param attributes the attributes to be populated
     * @return the index where attributes could be inserted
     */
    public static int cssImport(final ConvertibleNut nut,
                                final UrlProvider urlProvider,
                                final StringBuilder sb,
                                final Map<String, String> attributes) {
        attributes.put("rel", "stylesheet");
        attributes.put("type", "text/css");
        link(nut, urlProvider, sb);

        return "<link".length();
    }

    /**
     * <p>
     * Creates a link with a href attribute.
     * </p>
     *
     * @param urlProvider the {@link UrlProvider}
     * @param nut the CSS nut
     * @param sb the builder where statement is appended
     */
    private static void link(final ConvertibleNut nut, final UrlProvider urlProvider, final StringBuilder sb) {
        sb.append("<link href=\"");
        sb.append(urlProvider.getUrl(nut));
        sb.append("\" />");
    }

    /**
     * <p>
     * Generates import for Javascript script.
     * </p>
     *
     * @param nut the Javascript nut
     * @param urlProvider the {@link UrlProvider}
     * @param sb the builder where statement is appended
     * @param attributes the attributes to be populated
     * @return the index where attributes could be inserted
     */
    public static int javascriptImport(final ConvertibleNut nut,
                                       final UrlProvider urlProvider,
                                       final StringBuilder sb,
                                       final Map<String, String> attributes) {
        attributes.put("type", "text/javascript");
        sb.append("<script");
        sb.append(" src=\"");
        sb.append(urlProvider.getUrl(nut));
        sb.append("\"></script>");

        return "<script".length();
    }

    /**
     * <p>
     * Generates import for images.
     * </p>
     *
     * @param nut the image nut
     * @param urlProvider the {@link UrlProvider}
     * @param sb the builder where statement is appended
     * @return the index where attributes could be inserted
     */
    public static int imgImport(final ConvertibleNut nut, final UrlProvider urlProvider, final StringBuilder sb) {
        sb.append("<img src=\"");
        sb.append(urlProvider.getUrl(nut));
        sb.append("\" />");

        return NumberUtils.FOUR;
    }
}
