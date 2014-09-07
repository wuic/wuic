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


package com.github.wuic.exception;

import com.github.wuic.config.ObjectBuilder;

/**
 * <p>
 * This exception is thrown when a {@link com.github.wuic.config.ObjectBuilder} does not support
 * a specified property.
 * </p>
 *
 * @author Guillaume DROUET
 * @version 1.1
 * @since 0.4.0
 */
public class BuilderPropertyNotSupportedException extends WuicException implements ErrorCode {

    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = -8457851407298538536L;

    /**
     * <p>
     * Builds a new exception.
     * </p>
     *
     * @param key the property key
     * @param builderClass the built class
     */
    public BuilderPropertyNotSupportedException(final String key, final Class<?> builderClass) {
        super(String.format("%s is not a property which is supported by the %s builder", key, builderClass.getName()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getErrorCode() {
        return BUILDER_PROPERTY_NOT_SUPPORTED;
    }

    /**
     * <p>
     * Adapter class for generic purpose.
     * </p>
     *
     * @author Guillaume DROUET
     * @version 1.0
     * @since 0.5.0
     */
    public static class Adapter extends BuilderPropertyNotSupportedException {

        /**
         * <p>
         * Builds a new instance.
         * </p>
         *
         * @param key the unsupported property
         */
        public Adapter(final String key) {
            super(key, ObjectBuilder.class);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public long getErrorCode() {
            return BUILDER_PROPERTY_NOT_SUPPORTED;
        }
    }
}
