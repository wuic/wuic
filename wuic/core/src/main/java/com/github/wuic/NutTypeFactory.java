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

import com.github.wuic.exception.WuicException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * This factory produces {@link NutType types}.
 * </p>
 *
 * @author Guillaume DROUET
 * @since 0.5.3
 */
public class NutTypeFactory {

    /**
     * The logger.
     */
    private final Logger log = LoggerFactory.getLogger(getClass());

    /**
     * The types charset.
     */
    private final String charset;

    /**
     * <p>
     * Builds a new instance.
     * </p>
     *
     * @param charset the charset
     */
    public NutTypeFactory(final String charset) {
        Logging.PROPERTIES.log("NutType factory produces instances with charset {}", charset);
        this.charset = charset;
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
    public NutType getNutTypeForExtension(final String ext) {
        for (final EnumNutType nutType : EnumNutType.values()) {
            for (final String e : nutType.getExtensions()) {
                if (e.equals(ext)) {
                    return getNutType(nutType);
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
    public NutType getNutTypeForMimeType(final String mimeType) {
        final String[] split = mimeType.split(";");

        for (final EnumNutType nutType : EnumNutType.values()) {
            for (final String s : split) {
                if (nutType.getMimeType().equals(s)) {
                    return getNutType(nutType);
                }
            }
        }

        return null;
    }

    /**
     * <p>
     * Gets a {@link NutType} based on the given built-in type.
     * </p>
     *
     * @param enumNutType the built-in type
     * @return the type
     */
    public NutType getNutType(final EnumNutType enumNutType) {
        return new NutType(enumNutType, charset);
    }

    /**
     * <p>
     * Builds a list of {@link NutType} from the given array of built-in types.
     * </p>
     *
     * @param enumNutTypes the built-in types
     * @return the nut types
     */
    public List<NutType> getNutType(final EnumNutType[] enumNutTypes) {
        final List<NutType> retval = new ArrayList<NutType>(enumNutTypes.length);

        for (final EnumNutType e : enumNutTypes) {
            retval.add(getNutType(e));
        }

        return retval;
    }

    /**
     * <p>
     * Computes the {@link NutType} for the given path.
     * </p>
     *
     * @param path the path
     * @return the {@link NutType}, {@code null} if no extension exists
     */
    public NutType getNutType(final String path) {
        final int index = path.lastIndexOf('.');

        if (index < 0) {
            log.warn(String.format("'%s' does not contains any extension, ignoring nut", path));
            return null;
        }

        final String ext = path.substring(index);
        return getNutTypeForExtension(ext);
    }

    /**
     * <p>
     * Gets the charset used by this factory.
     * </p>
     *
     * @return the charset
     */
    public String getCharset() {
        return charset;
    }
}
