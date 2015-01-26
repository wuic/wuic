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


package com.github.wuic;

import com.github.wuic.exception.WuicException;
import com.github.wuic.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

/**
 * <p>
 * Enumeration of the possible types to be compressed and / or aggregated in WUIC.
 * </p>
 * 
 * @author Guillaume DROUET
 * @version 1.5
 * @since 0.1.0
 */
public enum NutType {

    /**
     * HTML files support. Keep this as first enum to improve {@link #getNutTypeForMimeType(String)} often use for HTML.
     */
    HTML(new String[] { ".html" }, "text/html", Boolean.FALSE),

    /**
     * EOT path support.
     */
    EOT(new String[] {".eot", ".EOT"}, "application/vnd.ms-fontobject", Boolean.TRUE),

    /**
     * EOT path support.
     */
    OTF(new String[] {".otf", ".OTF"}, "font/opentype", Boolean.TRUE),

    /**
     * TTF path support.
     */
    TTF(new String[] {".ttf", ".TTF"}, "application/octet-stream", Boolean.TRUE),

    /**
     * WOFF path support.
     */
    WOFF(new String[] {".woff", ".WOFF"}, "application/x-font-woff", Boolean.TRUE),

    /**
     * SVG path support.
     */
    SVG(new String[] {".svg", ".SVG"}, "image/svg+xml", Boolean.TRUE),

    /**
     * ICO file.
     */
    ICO(new String[] {".ico", ".ICO"}, "image/x-icon", Boolean.TRUE),

    /**
     * PNG path support.
     */
    PNG(new String[] {".png", ".PNG"}, "image/png", Boolean.TRUE),

    /**
     * GIF path support.
     */
    GIF(new String[] {".gif", ".GIF"}, "image/gif", Boolean.TRUE),

    /**
     * Javascript files support.
     */
    JAVASCRIPT(new String[] { ".js" }, "text/javascript", Boolean.FALSE),
    
    /**
     * CSS files support.
     */
    CSS(new String[] { ".css" }, "text/css", Boolean.FALSE),

    /**
     * Typescript files support.
     */
    TYPESCRIPT(new String[] { ".ts" }, "text/x.typescript", Boolean.FALSE),

    /**
     * MAP files support.
     */
    MAP(new String[] { ".map" }, "application/json", Boolean.FALSE);

    /**
     * The logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(NutType.class);
    /**
     * Possible extensions.
     */
    private String[] extensions;
    
    /**
     * MIME type.
     */
    private String mimeType;

    /**
     * Text or binary.
     */
    private Boolean isText;

    /**
     * <p>
     * Builds a new {@link NutType} according to the given extensions and the
     * given MIME type.
     * </p>
     * 
     * @param exts the extensions
     * @param mime the MIME type
     * @param isBinary if the path type is binary or not
     */
    private NutType(final String[] exts, final String mime, final Boolean isBinary) {
        extensions = Arrays.copyOf(exts, exts.length);
        mimeType = mime;
        isText = !isBinary;
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
     * Indicates if this nut is in a text format.
     * </p>
     *
     * @return {@code true} if the nut is a text, {@code false} otherwise
     */
    public Boolean isText() {
        return isText;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return String.format("%s with extension %s", name(), StringUtils.merge(extensions, ", "));
    }

    /**
     * <p>
     * Returns the {@link NutType} which the given extension belongs to.
     * </p>
     *
     * <p>
     * Throws an {@code BadArgumentException} if the extension does not belongs to any path type.
     * </p>
     *
     * @param ext the extension
     * @return the nut type
     */
    public static NutType getNutTypeForExtension(final String ext) {
        for (final NutType nutType : NutType.values()) {
            for (String e : nutType.getExtensions()) {
                if (e.equals(ext)) {
                    return nutType;
                }
            }
        }

        WuicException.throwBadArgumentException(new IllegalArgumentException(String.format("%s is not associated to any NutType", ext)));
        return null;
    }

    /**
     * <p>
     * Returns the {@link NutType} which the given mime type belongs to.
     * </p>
     *
     * @param mimeType the mime type
     * @return the nut type, {@code null} if nothing match
     */
    public static NutType getNutTypeForMimeType(final String mimeType) {
        final String[] split = mimeType.split(";");

        for (final NutType nutType : NutType.values()) {
            for (final String s : split) {
                if (nutType.getMimeType().equals(s)) {
                    return nutType;
                }
            }
        }

        return null;
    }

    /**
     * <p>
     * Computes the {@link NutType} for the given path.
     * </p>
     *
     * @param path the path
     * @return the {@link NutType}, {@code null} if no extension exists
     */
    public static NutType getNutType(final String path) {
        final int index = path.lastIndexOf('.');

        if (index < 0) {
            LOG.warn(String.format("'%s' does not contains any extension, ignoring nut", path));
            return null;
        }

        final String ext = path.substring(index);
        return getNutTypeForExtension(ext);
    }
}
