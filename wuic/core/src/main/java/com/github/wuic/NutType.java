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

import com.github.wuic.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.Arrays;

/**
 * <p>
 * Represents a possible type to be compressed and / or aggregated in WUIC.
 * </p>
 * 
 * @author Guillaume DROUET
 * @since 0.1.0
 */
public class NutType implements Serializable {

    /**
     * Logger.
     */
    private final Logger log = LoggerFactory.getLogger(getClass());

    /**
     * The built-in type.
     */
    private final EnumNutType enumNutType;

    /**
     * The charset.
     */
    private final String charset;

    /**
     * <p>
     * Builds a new {@link NutType}.
     * </p>
     * 
     * @param enumNutType the built-in type
     * @param charset the charset to use for encoding/decoding text streams
     */
    public NutType(final EnumNutType enumNutType, final String charset) {
        this.enumNutType = enumNutType;
        this.charset = charset;
        Logging.PROPERTIES.log("Creating nut type with charset {}", charset);
    }
    
    /**
     * <p>
     * Returns the possible extensions.
     * </p>
     * 
     * @return the possible extensions
     */
    public String[] getExtensions() {
        return enumNutType.getExtensions();
    }
    
    /**
     * <p>
     * Returns the MIME type.
     * </p>
     * 
     * @return the MIME type
     */
    public String getMimeType() {
        return enumNutType.getMimeType();
    }

    /**
     * <p>
     * Gets the information for server hint.
     * </p>
     *
     * @return the information, {@code null} if none
     */
    public String getHintInfo() {
        return enumNutType.getHintInfo();
    }

    /**
     * <p>
     * Gets the name.
     * </p>
     *
     * @return the name
     */
    public String name() {
        return enumNutType.name();
    }

    /**
     * <p>
     * Indicates if the nut is text or binary.
     * </p>
     *
     * @return {@code true} if text, {@code false} if binary
     */
    public boolean isText() {
        return enumNutType.isText();
    }

    /**
     * <p>
     * Indicates if this type is based on the given built-in type.
     * </p>
     *
     * @param enumNutType the built-in type to compare
     * @return {@code true} if {@code enumNutType} equals to {@link #enumNutType}
     */
    public boolean isBasedOn(final EnumNutType enumNutType) {
        return enumNutType.equals(this.enumNutType);
    }

    /**
     * <p>
     * Gets the charset.
     * </p>
     *
     * @return the charset
     */
    public String getCharset() {
        return charset;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return String.format("%s with extension %s", enumNutType.name(), StringUtils.merge(getExtensions(), ", "));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }

        if (obj instanceof NutType) {
            final NutType other = NutType.class.cast(obj);

            if (enumNutType.equals(other.enumNutType)) {
                if (!charset.equals(other.charset)) {
                    log.warn("Different charset detected!", new IllegalArgumentException(
                            String.format("%s != %s", charset, other.charset)));
                } else {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return Arrays.deepHashCode(new Object[] { enumNutType, charset, });
    }
}
