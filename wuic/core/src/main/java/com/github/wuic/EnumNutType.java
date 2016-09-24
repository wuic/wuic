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


package com.github.wuic;

import java.util.Arrays;

/**
 * <p>
 * Enumeration of the built-in types to be compressed and / or aggregated in WUIC.
 * </p>
 *
 * @author Guillaume DROUET
 * @since 0.5.3
 */
public enum EnumNutType {

    /**
     * HTML files support. Keep this as first enum to improve {@link NutTypeFactory#getNutTypeForMimeType(String)} often use for HTML.
     */
    HTML(new String[] { ".html" }, "text/html", EnumNutType.HTML_PAGE, true),

    /**
     * EOT path support.
     */
    EOT(new String[] {".eot", ".EOT"}, "application/vnd.ms-fontobject", EnumNutType.FONT, false),

    /**
     * EOT path support.
     */
    OTF(new String[] {".otf", ".OTF"}, "font/opentype", EnumNutType.FONT, false),

    /**
     * TTF path support.
     */
    TTF(new String[] {".ttf", ".TTF"}, "application/octet-stream", EnumNutType.FONT, false),

    /**
     * WOFF path support.
     */
    WOFF(new String[] {".woff", ".WOFF"}, "application/x-font-woff", EnumNutType.FONT, false),

    /**
     * WOFF2 path support.
     */
    WOFF2(new String[] {".woff2", ".WOFF2"}, "application/font-woff2", EnumNutType.FONT, false),

    /**
     * SVG path support.
     */
    SVG(new String[] {".svg", ".SVG"}, "image/svg+xml", EnumNutType.IMAGE, true),

    /**
     * ICO file.
     */
    ICO(new String[] {".ico", ".ICO"}, "image/x-icon", EnumNutType.IMAGE, false),

    /**
     * PNG path support.
     */
    PNG(new String[] {".png", ".PNG"}, "image/png", EnumNutType.IMAGE, false),

    /**
     * JPG path support.
     */
    JPG(new String[] {".jpg", ".JPG", ".jpeg", ".JPEG"}, "image/jpeg", EnumNutType.IMAGE, false),

    /**
     * GIF path support.
     */
    GIF(new String[] {".gif", ".GIF"}, "image/gif", EnumNutType.IMAGE, false),

    /**
     * Javascript files support.
     */
    JAVASCRIPT(new String[] { ".js" }, "text/javascript", EnumNutType.SCRIPT, true),

    /**
     * CSS files support.
     */
    CSS(new String[] { ".css" }, "text/css", EnumNutType.STYLESHEET, true),

    /**
     * Typescript files support.
     */
    TYPESCRIPT(new String[] { ".ts" }, "text/x.typescript", null, true),

    /**
     * JSX files support.
     */
    JSX(new String[] { ".jsx" }, "text/jsx", null, true),

    /**
     * LESS files support.
     */
    LESS(new String[] { ".less" }, "text/css", null, true),

    /**
     * MAP files support.
     */
    MAP(new String[] { ".map" }, "application/json", null, true),

    /**
     * Application cache files support.
     */
    APP_CACHE(new String[] { ".appcache" }, "text/cache-manifest", null, true);

    /**
     * Constant to hint images.
     */
    private static final String IMAGE = "image";

    /**
     * Constant to hint fonts.
     */
    private static final String FONT = "font";

    /**
     * Constant to hint scripts.
     */
    private static final String SCRIPT = "script";

    /**
     * Constant to hint CSS.
     */
    private static final String STYLESHEET = "stylesheet";

    /**
     * Constant to hint HTML pages.
     */
    private static final String HTML_PAGE = "html";

    /**
     * Possible extensions.
     */
    private final String[] extensions;

    /**
     * MIME type.
     */
    private final String mimeType;

    /**
     * Server hint information.
     */
    private final String hintInfo;

    /**
     * Type is text or not.
     */
    private final boolean text;

    /**
     * <p>
     * Builds a new {@link NutType} according to the given extensions and the
     * given MIME type.
     * </p>
     *
     * @param exts the extensions
     * @param mime the MIME type
     * @param hint the hint information
     * @param isText nut is binary or text
     */
    private EnumNutType(final String[] exts, final String mime, final String hint, final boolean isText) {
        extensions = Arrays.copyOf(exts, exts.length);
        mimeType = mime;
        hintInfo = hint;
        text = isText;
    }

    /**
     * <p>
     * Returns the possible extensions.
     * </p>
     *
     * @return the possible extensions
     */
    public String[] getExtensions() {
        return extensions;
    }

    /**
     * <p>
     * Returns the MIME type.
     * </p>
     *
     * @return the MIME type
     */
    public String getMimeType() {
        return mimeType;
    }

    /**
     * <p>
     * Gets the information for server hint.
     * </p>
     *
     * @return the information, {@code null} if none
     */
    public String getHintInfo() {
        return hintInfo;
    }

    /**
     * <p>
     * Indicates if the nut is text or binary.
     * </p>
     *
     * @return {@code true} if text, {@code false} if binary
     */
    public boolean isText() {
        return text;
    }
}
