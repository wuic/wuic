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


package com.github.wuic;

import com.github.wuic.engine.EngineType;
import com.github.wuic.exception.wrapper.BadArgumentException;
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
     * Sprite path support.
     */
    //SPRITE(new String[] {".png", ".PNG"}, null, Boolean.TRUE),

    /**
     * EOT path support.
     */
    //EOT(new String[] {".eot", ".EOT"}, "application/vnd.ms-fontobject", Boolean.TRUE),

    /**
     * PNG path support.
     */
    PNG(new String[] {".png", ".PNG"}, "image/png", Boolean.TRUE, EngineType.INSPECTOR),

    /**
     * GIF path support.
     */
    GIF(new String[] {".gif", ".GIF"}, "image/gif", Boolean.TRUE, EngineType.INSPECTOR),

    /**
     * Javascript files support.
     */
    JAVASCRIPT(new String[] { ".js" }, "text/javascript", Boolean.FALSE),
    
    /**
     * CSS files support.
     */
    CSS(new String[] { ".css" }, "text/css", Boolean.FALSE),

    /**
     * HTML files support.
     */
    HTML(new String[] { ".html" }, "text/html", Boolean.FALSE);


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
     * The engines that should be applies in best effort.
     */
    private EngineType[] requiredForBestEffort;

    /**
     * <p>
     * Builds a new {@link NutType} according to the given extensions and the
     * given MIME type.
     * </p>
     * 
     * @param exts the extensions
     * @param mime the MIME type
     * @param isBinary if the path type is binary or not
     * @param rfbe the engines that should be applies in best effort
     */
    private NutType(final String[] exts, final String mime, final Boolean isBinary, final EngineType ... rfbe) {
        extensions = Arrays.copyOf(exts, exts.length);
        mimeType = mime;
        isText = !isBinary;
        requiredForBestEffort = EngineType.without(rfbe);
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
     * <p>
     * Gets the engines that should be applied in best effort.
     * </p>
     *
     * @return the mandatory types
     */
    public EngineType[] getRequiredForBestEffort() {
        return requiredForBestEffort;
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
     * Throws an {@code BadArgumentException} if the extension does not belongs to any path type
     * </p>
     *
     * @param ext the extension
     * @return the path type
     */
    public static NutType getNutTypeForExtension(final String ext) {
        for (final NutType nutType : NutType.values()) {
            for (String e : nutType.getExtensions()) {
                if (e.equals(ext)) {
                    return nutType;
                }
            }
        }

        throw new BadArgumentException(new IllegalArgumentException(String.format("%s is not associated to any NutType", ext)));
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
