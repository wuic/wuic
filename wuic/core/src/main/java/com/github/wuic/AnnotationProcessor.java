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

import java.lang.annotation.Annotation;

/**
 * <p>
 * An annotation processor is notified of all types annotated with a specified annotation.
 * </p>
 *
 * @author Guillaume DROUET
 * @since 0.5.0
 */
public interface AnnotationProcessor {

    /**
     * <p>
     * The annotation that must be detected on type to be handled.
     * </p>
     *
     * @return the annotation
     */
    Class<? extends Annotation> requiredAnnotation();

    /**
     * <p>
     * Handles a type annotated with the required annotation.
     * </p>
     *
     * @param annotatedType the annotated type
     */
    void handle(Class<?> annotatedType);
}
