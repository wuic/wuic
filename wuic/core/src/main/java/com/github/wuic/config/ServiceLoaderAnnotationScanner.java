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
