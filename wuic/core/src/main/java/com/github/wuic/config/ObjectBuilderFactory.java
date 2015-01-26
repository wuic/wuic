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


package com.github.wuic.config;

import com.github.wuic.AnnotationProcessor;
import com.github.wuic.util.AnnotationDetectorScanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
     * Data object that provides a detected service and the type name that identifies its builder.
     *
     * @author Guillaume DROUET
     * @version 1.0
     * @since 0.5
     */
    public final class KnownType {

        /**
         * The class type.
         */
        private Class<T> classType;

        /**
         * The type name.
         */
        private String typeName;

        /**
         * <p>
         * Builds a new instance.
         * </p>
         *
         * @param classType the class type
         */
        public KnownType(final Class<T> classType) {
            this.classType = classType;
            this.typeName = classType.getSimpleName() + "Builder";
        }

        /**
         * Gets the class type.
         *
         * @return the class
         */
        public Class<T> getClassType() {
            return classType;
        }

        /**
         * Gets the name that identified the class builder.
         *
         * @return the type name
         */
        public String getTypeName() {
            return typeName;
        }

        /**
         * {@inheritDoc}
         */
        public String toString() {
            return getTypeName();
        }
    }

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
    private List<KnownType> knownTypes;

    /**
     * A list of inspector to execute on any built object.
     */
    private List<ObjectBuilderInspector> inspectors;

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
        this.knownTypes = new ArrayList<KnownType>();
        this.inspectors = new ArrayList<ObjectBuilderInspector>();
        new AnnotationDetectorScanner().scan(packageToScan, this);
    }

    /**
     * <p>
     * Builds a new instance.
     * </p>
     *
     * @param annotationToScan the annotation to discover
     * @param classes a list of classes to directly scan instead of discovering types by scanning classpath
     */
    public ObjectBuilderFactory(final Class<? extends Annotation> annotationToScan,
                                final Class<? extends T> ... classes) {
        this.annotationToScan = annotationToScan;
        this.knownTypes = new ArrayList<KnownType>();
        this.inspectors = new ArrayList<ObjectBuilderInspector>();

        for (Class<? extends T> clazz : classes) {
            if (clazz.isAnnotationPresent(annotationToScan)) {
                handle(clazz);
            } else {
                logger.warn(String.format("%s must be annotated with %s, ignoring...", clazz.getName(), annotationToScan.getName()),
                        new IllegalArgumentException());
            }
        }
    }

    /**
     * <p>
     * Adds an inspector.
     * </p>
     *
     * @param i the inspector
     * @return this
     */
    public ObjectBuilderFactory<T> inspector(final ObjectBuilderInspector i) {
        inspectors.add(i);
        return this;
    }

    /**
     * <p>
     * Returns all the supported types.
     * </p>
     *
     * @return the known types
     */
    public List<KnownType> knownTypes() {
        return knownTypes;
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
    public final void handle(final Class<?> annotatedType) {
        if (annotatedType.getDeclaringClass() != null) {
            logger.warn(String.format("Your service annotation can't be applied to inner class %s", annotatedType.getName()),
                    new IllegalArgumentException());
        } else {
            knownTypes.add(new KnownType((Class<T>) annotatedType));
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
    final class Builder extends AbstractObjectBuilder<T> {

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
        protected T internalBuild() {
            final Object[] params = getAllProperties();

            try {
                T retval = constructor.newInstance(params);

                for (final ObjectBuilderInspector i : inspectors) {
                    retval = i.inspect(retval);
                }

                return retval;
            } catch (IllegalAccessException iae) {
                 logger.error("", iae);
            } catch (InstantiationException ie) {
                logger.error("", ie);
            } catch (InvocationTargetException ite) {
                logger.error("", ite);
            } catch (IllegalArgumentException iae) {
                logger.error("", iae);
            }

            return null;
        }
    }

    /**
     * <p>
     * Creates a builder for the given type. The type is the simple name of the class produced by the builder.
     * All supported type are detected during classpath scanning. If a type could not be registered, the
     * cause is logged and when retrieved with this method, {@code null} will be returned.
     * </p>
     *
     * <p>
     * Throws an {@code IllegalArgumentException} if the there is a bad usage of annotation or if the type is unknown
     * </p>
     *
     * @param type the type created by the builder
     * @return the builder
     */
    @SuppressWarnings("unchecked")
    public ObjectBuilder<T> create(final String type) {
        Class<T> clazz = null;

        for (final KnownType knownType : knownTypes) {
            if (knownType.getTypeName().equals(type)) {
                clazz = knownType.getClassType();
                break;
            }
        }

        if (clazz == null) {
            throw new IllegalArgumentException(type + " is not supported! Available builders are: " + Arrays.toString(knownTypes.toArray()));
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
                } catch (Exception e) {
                    logger.error(String.format("Unable to create a builder with constructor %s", constructor.toString()), e);
                    continue constructor;
                }
            }

            return retval;
        }

        logger.error(String.format("Unable to find any constructor to create a builder for %s", type), new IllegalStateException());

        return null;
    }
}
