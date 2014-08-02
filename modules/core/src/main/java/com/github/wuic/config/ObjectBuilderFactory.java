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


package com.github.wuic.config;

import com.github.wuic.AnnotationProcessor;
import com.github.wuic.exception.BuilderPropertyNotSupportedException;
import com.github.wuic.exception.UnableToInstantiateException;
import com.github.wuic.util.GenericBuilder;
import com.github.wuic.util.ReflectionsAnnotationScanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

/**
 * <p>
 * This factory creates builder for a particular type of object. All implementations of this type must be
 * annotated with a specified annotation to be detected over the classpath.
 * </p>
 *
 * @author Guillaume DROUET
 * @version 1.0
 * @since 0.5
 * @param <T> the type of objects produced by the builder
 */
public class ObjectBuilderFactory<T> implements AnnotationProcessor {

    /**
     * Bad annotation usage warning message.
     */
    private static final String IAE_MSG = "All parameters in the constructor annotated with %s must be annotated with a config annotation.";

    /**
     * The logger.
     */
    private final Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * The annotation to scan.
     */
    private Class<? extends Annotation> annotationToScan;

    /**
     * All the discovered types.
     */
    private Map<String, Class<T>> knownTypes;

    /**
     * <p>
     * Builds a new instance.
     * </p>
     *
     * @param annotationToScan the annotation to discover
     * @param packageToScan the package to scan
     */
    public ObjectBuilderFactory(final Class<? extends Annotation> annotationToScan,
                                final String packageToScan) {
        this.annotationToScan = annotationToScan;
        this.knownTypes = new HashMap<String, Class<T>>();
        new ReflectionsAnnotationScanner().scan(packageToScan, this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<? extends Annotation> requiredAnnotation() {
        return annotationToScan;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked")
    public void handle(final Class<?> annotatedType) {
        if (annotatedType.getDeclaringClass() != null) {
            logger.warn(String.format("Your service annotation can't be applied to inner class %s", annotatedType.getName()),
                    new IllegalArgumentException());
        } else {
            knownTypes.put(annotatedType.getSimpleName(), (Class<T>) annotatedType);
        }
    }

    /**
     * <p>
     * This builder creates a new instance from a specified constructor and the arguments
     * retrieved from the property setters.
     * </p>
     *
     * @author Guillaume DROUET
     * @version 1.0
     * @since 0.5
     */
    final class Builder extends AbstractGenericBuilder<T> {

        /**
         * The constructor.
         */
        private Constructor<T> constructor;

        /**
         * <p>
         * Builds a new instance.
         * </p>
         *
         * @param constructor the constructor to use
         */
        private Builder(final Constructor<T> constructor) {
            this.constructor = constructor;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected T internalBuild() throws BuilderPropertyNotSupportedException {
            final Object[] params = getAllProperties();

            try {
                return constructor.newInstance(params);
            } catch (IllegalAccessException iae) {
                 logger.error("", iae);
            } catch (InstantiationException ie) {
                logger.error("", ie);
            } catch (InvocationTargetException ite) {
                logger.error("", ite);
            }

            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void throwPropertyNotSupportedException(final String key) throws BuilderPropertyNotSupportedException {
            throw new BuilderPropertyNotSupportedException.Adapter(key);
        }
    }

    /**
     * <p>
     * Creates a builder for the given type. The type is the simple name of the class produced by the builder.
     * All supported type are detected during classpath scanning. If a type could not be registered, the
     * cause is logged and when retrieved with this method, {@code null} will be returned.
     * </p>
     *
     * @param type the type created by the builder
     * @return the builder, {@code null} if the there is a bad usage of annotation or if the type is unknown
     */
    @SuppressWarnings("unchecked")
    public GenericBuilder<T> create(final String type) {
        final Class<T> clazz = knownTypes.get(type);

        if (clazz == null) {
            return null;
        }

        // Detect constructors
        final Constructor[] constructors = clazz.getDeclaredConstructors();

        constructor:
        for (final Constructor constructor : constructors) {
            if (!constructor.isAnnotationPresent(ConfigConstructor.class)) {
                logger.debug("{} is not annotated with {}: ignoring...", constructor.toString(), ConfigConstructor.class.getName());
                continue;
            }

            logger.debug("Evaluating constructor '{}'", constructor.toString());
            final Builder retval = new Builder(constructor);
            final Annotation[][] annotations = constructor.getParameterAnnotations();

            for (final Annotation[] paramAnnotation : annotations) {
                try {
                    if (1 != paramAnnotation.length) {
                        logger.warn(String.format(IAE_MSG, constructor.toString()), new IllegalArgumentException());
                        continue constructor;
                    }

                    final PropertySetter propertySetter = PropertySetterFactory.INSTANCE.create(retval, paramAnnotation[0]);

                    if (propertySetter == null) {
                        logger.warn(String.format(IAE_MSG, constructor.toString()), new IllegalArgumentException());
                        continue constructor;
                    } else {
                        retval.addPropertySetter(propertySetter);
                    }
                } catch (UnableToInstantiateException utoie) {
                    logger.error(String.format("Unable to create a builder with constructor %s", constructor.toString()), utoie);
                    continue constructor;
                }
            }

            return retval;
        }

        logger.error(String.format("Unable to find any constructor to create a builder for %s", type), new IllegalStateException());

        return null;
    }
}
