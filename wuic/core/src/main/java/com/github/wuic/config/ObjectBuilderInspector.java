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


package com.github.wuic.config;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>
 * An inspector called each time a builder produced by {@link ObjectBuilderFactory} builds on a object to give
 * a chance to modify the object before it gets returned.
 * </p>
 *
 * @author Guillaume DROUET
 * @since 0.5
 */
public interface ObjectBuilderInspector {

    /**
     * <p>
     * This annotation can be used on the {@link ObjectBuilderInspector} implementation to specify a particular type
     * of object to be inspected. If the annotation is not used, then the inspector should be applied to any type.
     * </p>
     *
     * @author Guillaume DROUET
     * @since 0.5.3
     */
    @Target({ElementType.TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    public @interface InspectedType {

        /**
         * <p>
         * Array of classes that will be inspected by the annotated inspector.
         * </p>
         *
         * @return the classes
         */
        Class[] value();
    }

    /**
     * <p>
     * Inspects a built object.
     * </p>
     *
     * @param object the object
     * @param <T> the type of object
     * @return the inspected object
     */
    <T> T inspect(T object);
}
