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

import com.github.wuic.AnnotationProcessor;
import com.github.wuic.AnnotationScanner;

import java.lang.annotation.Annotation;
import java.util.ServiceLoader;

/**
 * <p>
 * An {@link AnnotationScanner} that relies on {@code ServiceLoader} to load annotated services. The class looks at
 * the {@link com.github.wuic.AnnotationProcessor#requiredAnnotation() required} annotation and if it's annotated with
 * {@link ServiceLoaderClasses}. If it's the case, the classes returned by {@link ServiceLoaderClasses#value()}  will
 * be loaded with the {@code ServiceLoader}. {@link AnnotationProcessor} will be called only if the class name starts
 * with the given base package.
 * </p>
 *
 * @author Guillaume DROUET
 * @since 0.5.3
 */
public class ServiceLoaderAnnotationScanner implements AnnotationScanner {

    /**
     * {@inheritDoc}
     */
    @Override
    public void scan(final String basePackage, final AnnotationProcessor ... processors) {

        // Check the required annotation of each processor
        for (final AnnotationProcessor processor : processors) {
            final Class<? extends Annotation> ra = processor.requiredAnnotation();

            // If the required annotation is annotated with the right annotation, process it
            if (ra.isAnnotationPresent(ServiceLoaderClasses.class)) {
                for (final Class<?> clazz : ra.getAnnotation(ServiceLoaderClasses.class).value()) {
                    reportHandle(clazz, processor, basePackage);
                }
            }
        }
    }

    /**
     * <p>
     * If the given class starts with the package specified in parameter, the class is loaded by the {@code ServiceLoader}.
     * The discovered implementation are notified to the given processor.
     * </p>
     *
     * @param clazz class to load
     * @param processor the processor to notify
     * @param filer required base package for the class
     */
    private void reportHandle(final Class<?> clazz, final AnnotationProcessor processor, final String filer) {

        // Class is in the right package
        if (clazz.getName().startsWith(filer)) {
            final ServiceLoader<?> serviceLoader = ServiceLoader.load(clazz);

            for (final Object o : serviceLoader) {
                processor.handle(o.getClass());
            }
        }
    }
}
