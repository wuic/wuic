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
import com.github.wuic.context.ContextBuilder;
import com.github.wuic.util.BiFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Arrays;
import java.util.List;

/**
 * <p>
 * This factory creates builder for a particular type of object. All implementations of this type must be
 * annotated with a specified annotation to be detected over the classpath.
 * </p>
 *
 * @author Guillaume DROUET
 * @since 0.5
 * @param <T> the type of objects produced by the builder
 */
public class ObjectBuilderFactory<T> implements AnnotationProcessor {

    /**
     * Data object that provides a detected service and the type name that identifies its builder.
     *
     * @author Guillaume DROUET
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
         * Alias.
         */
        private String alias;

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
            this.alias = classType.isAnnotationPresent(Alias.class) ? classType.getAnnotation(Alias.class).value() : null;
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
         * <p>
         * Gets the alias.
         * </p>
         *
         * @return the alias, {@code null} if not alias is defined
         */
        public String getAlias() {
            return alias;
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
     * A list of inspector to execute for each instance of class specified as key.
     * Inspectors associated to {@code null} will be applied on any object.
     */
    private Map<Class, List<ObjectBuilderInspector>> inspectors;

    /**
     * <p>
     * Creates a new instance by copy.
     * </p>
     *
     * @param other the other {@code ObjectBuilderFactory} to copy
     */
    public ObjectBuilderFactory(final ObjectBuilderFactory<T> other) {
        this.annotationToScan = other.annotationToScan;
        this.knownTypes = other.knownTypes;
        this.inspectors = other.inspectors;
    }

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
        this.inspectors = new HashMap<Class, List<ObjectBuilderInspector>>();
        new ServiceLoaderAnnotationScanner().scan(packageToScan, this);
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
        this.inspectors = new HashMap<Class, List<ObjectBuilderInspector>>();

        for (final Class<? extends T> clazz : classes) {
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
     * Adds an inspector. If the inspector is annotated with {@link ObjectBuilderInspector.InspectedType}, then it will
     * be applied to object instantiated from specified classes only. Otherwise the inspector will be called for any object.
     * </p>
     *
     * @param inspectors the map to populate
     * @param obi the inspector
     */
    public static void inspector(final Map<Class, List<ObjectBuilderInspector>> inspectors, final ObjectBuilderInspector obi) {
        final BiFunction<Class, ObjectBuilderInspector, String> populator = new BiFunction<Class, ObjectBuilderInspector, String>() {

            /**
             * {@inheritDoc}
             */
            @Override
            public String apply(final Class clazz, final ObjectBuilderInspector i) {
                List<ObjectBuilderInspector> inspectorList = inspectors.get(clazz);

                // First inspector registered for this class
                if (inspectorList == null) {
                    inspectorList = new ArrayList<ObjectBuilderInspector>();
                    inspectors.put(clazz, inspectorList);
                }

                inspectorList.add(i);

                return null;
            }
        };

        boolean perClass = false;

        // Check if inspector should apply just a few objects
        final ObjectBuilderInspector o = getWrappedObjectBuilderInspector(obi);

        if (o.getClass().isAnnotationPresent(ObjectBuilderInspector.InspectedType.class)) {
            for (final Class<?> clazz : o.getClass().getAnnotation(ObjectBuilderInspector.InspectedType.class).value()) {
                populator.apply(clazz, obi);
            }

            perClass = true;
        }

        if (o instanceof PerClassObjectBuilderInspector) {
            for (final Class<?> clazz : PerClassObjectBuilderInspector.class.cast(o).inspectedTypes()) {
                populator.apply(clazz, obi);
            }

            perClass = true;
        }

        if (!perClass) {
            populator.apply(null, obi);
        }
    }

    /**
     * <p>
     * Gets the wrapped {@link ObjectBuilderInspector} by detecting the instance of {@link ContextBuilder.ProfileObjectBuilderInspector}
     * specified in parameters.
     * </p>
     *
     * @param objectBuilderInspector the potential wrapper
     * @return the wrapped object
     */
    private static ObjectBuilderInspector getWrappedObjectBuilderInspector(final ObjectBuilderInspector objectBuilderInspector) {
        ObjectBuilderInspector o = objectBuilderInspector;

        while (o instanceof ContextBuilder.ProfileObjectBuilderInspector) {
            o = ContextBuilder.ProfileObjectBuilderInspector.class.cast(o).getWrap();
        }

        return o;
    }

    /**
     * <p>
     * Adds an inspector as specified in {@link #inspector(java.util.Map, ObjectBuilderInspector)}.
     * </p>
     *
     * @param obi the inspector
     * @return this
     */
    public ObjectBuilderFactory<T> inspector(final ObjectBuilderInspector obi) {
        inspector(inspectors, obi);
        return this;
    }

    /**
     * <p>
     * Returns the alias of the given builder name if exists.
     * </p>
     *
     * @param builderName the builder name
     * @return the alias is exist, {@code null} otherwise
     */
    public String findAlias(final String builderName) {
        for (final ObjectBuilderFactory.KnownType knownType : knownTypes()) {
            if (knownType.getTypeName().equals(builderName) || builderName.equals(knownType.getAlias())) {
                return knownType.getAlias();
            }
        }

        return null;
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
     * Inspects the object specified in parameter with given map of inspectors.
     * This method is invoked when the internal {@link com.github.wuic.config.ObjectBuilderFactory.Builder#build()}
     * method is invoked and can be overridden in order to customize the build process.
     * </p>
     *
     * @param builder the builder
     * @param inspectors the inspectors to apply
     * @param object the object to inspect
     * @return the inspected object
     */
    protected T inspect(final ObjectBuilder<T> builder, final Map<Class, List<ObjectBuilderInspector>> inspectors, final T object) {
        T retval = object;

        for (final Map.Entry<Class, List<ObjectBuilderInspector>> entry : inspectors.entrySet()) {
            for (final ObjectBuilderInspector i : entry.getValue()) {
                // Case 1: apply inspectors declared for any class
                // Case 2: apply inspectors explicitly declared for this class
                if (entry.getKey() == null || entry.getKey().isAssignableFrom(retval == null ? builder.getType() : retval.getClass())) {
                    retval = i.inspect(retval);
                }
            }
        }

        return retval;
    }

    /**
     * <p>
     * This builder creates a new instance from a specified constructor and the arguments
     * retrieved from the property setters.
     * </p>
     *
     * @author Guillaume DROUET
     * @since 0.5
     */
    final class Builder extends AbstractObjectBuilder<T> {

        /**
         * The constructor.
         */
        private Constructor<T> constructor;

        /**
         * The inspectors to apply for this builder.
         */
        private final Map<Class, List<ObjectBuilderInspector>> internalInspectors;

        /**
         * <p>
         * Builds a new instance.
         * </p>
         *
         * @param constructor the constructor to use
         */
        private Builder(final Constructor<T> constructor) {
            this.constructor = constructor;
            this.internalInspectors = new LinkedHashMap<Class, List<ObjectBuilderInspector>>();
        }

        /**
         * <p>
         * Adds an inspector that will call the given method when an object is created.
         * </p>
         *
         * @param method the method
         */
        private void inspector(final Method method) {
            ObjectBuilderFactory.inspector(internalInspectors, new MethodInvokerObjectBuilderInspector(method));
        }

        /**
         * {@inheritDoc}
         */
        public Class<T> getType() {
            return constructor.getDeclaringClass();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public T build() {
            final Object[] params = constructor.getParameterTypes().length == 0 ?
                    new Object[0] : getAllProperties(constructor.toString());

            try {
                // Apply internal inspectors first, global inspectors then
                return inspect(this, inspectors, inspect(this, internalInspectors, constructor.newInstance(params)));
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

        /**
         * <p>
         * This {@link ObjectBuilderInspector} invokes a configured method with a set of parameters when the object if inspected.
         * </p>
         *
         * @author Guillaume DROUET
         * @since 0.5.3
         */
        private final class MethodInvokerObjectBuilderInspector implements PerClassObjectBuilderInspector {

            /**
             * The method to invoke.
             */
            private final Method method;

            /**
             * <p>
             * Builds a new instance.
             * </p>
             *
             * @param method the method
             */
            private MethodInvokerObjectBuilderInspector(final Method method) {
                this.method = method;
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public <T> T inspect(final T object) {
                if (object != null) {
                    try {
                        final Object[] params = method.getParameterTypes().length == 0 ?
                                new Object[0] : getAllProperties(method.toString());
                        object.getClass().getMethod(method.getName(), method.getParameterTypes()).invoke(object, params);
                    } catch (IllegalAccessException iae) {
                        logger.error("", iae);
                    } catch (InvocationTargetException ite) {
                        logger.error("", ite);

                        // If an init method throws an exception during it invocation we return null in order to reproduce
                        // the same behavior than an instantiation failure due to an uncaught exception in the constructor
                        return null;
                    } catch (NoSuchMethodException nsme) {
                        logger.error("", nsme);
                    }
                }

                return object;
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public Class[] inspectedTypes() {
                return new Class[] {
                        method.getDeclaringClass()
                };
            }
        }
    }

    /**
     * <p>
     * Creates a builder for the given type. The type is the simple name of the class produced by the builder.
     * Alternatively the type can be the value defined in {@link Alias} used to annotate the associated class
     * (equality is case insensitive). All supported type are detected during classpath scanning. If a type
     * could not be registered, the cause is logged and when retrieved with this method, {@code null} will be returned.
     * </p>
     *
     * <p>
     * Throws an {@code IllegalArgumentException} if the there is a bad usage of annotation or if the type is unknown.
     * </p>
     *
     * @param type the type created by the builder
     * @return the builder
     */
    @SuppressWarnings("unchecked")
    public ObjectBuilder<T> create(final String type) {
        Class<T> clazz = null;

        for (final KnownType knownType : knownTypes) {
            if (knownType.getTypeName().equals(type) || (knownType.getAlias() != null && knownType.getAlias().equalsIgnoreCase(type))) {
                clazz = knownType.getClassType();
                break;
            }
        }

        if (clazz == null) {
            throw new IllegalArgumentException(type + " is not supported! Available builders are: " + Arrays.toString(knownTypes.toArray()));
        }

        // Detect constructors
        final Constructor[] constructors = clazz.getDeclaredConstructors();

        // Try to find a valid constructor annotated with config annotation
        for (final Constructor constructor : constructors) {
            if (constructor.isAnnotationPresent(Config.class)) {
                final Builder retval = new Builder(constructor);

                if (inspectParamAnnotations(constructor.getParameterAnnotations(), constructor.toString(), retval)) {
                    detectConfigMethods(retval, clazz);
                    return retval;
                }
            } else {
                logger.debug("{} is not annotated with {}: ignoring...", constructor.toString(), Config.class.getName());
            }
        }

        logger.debug("No valid constructor annotated with {}, trying to gte default constructor.", Config.class.getName());

        try {
            // Raise an exception if no default constructor is available
            final Constructor constructor = clazz.getConstructor();
            final Builder retval = new Builder(constructor);

            if (inspectParamAnnotations(constructor.getParameterAnnotations(), constructor.toString(), retval)) {
                detectConfigMethods(retval, clazz);
                return retval;
            }
        } catch (NoSuchMethodException nsme) {
            logger.debug("Unable to find a default constructor. Returning null instance...", nsme);
        }

        logger.error(String.format("Unable to find any constructor to create a builder for %s", type), new IllegalStateException());

        return null;
    }

    /**
     * <p>
     * Detects the methods annotated with {@link Config} and make sure they are called to initialize an instance.
     * </p>
     *
     * @param builder the builder
     * @param clazz the class instantiated by the builder
     */
    private void detectConfigMethods(final Builder builder, final Class<?> clazz) {
        for (final Method method : clazz.getDeclaredMethods()) {

            // Annotation found
            if (method.isAnnotationPresent(Config.class)) {
                // Method has been validated, add the inspector that will invoke it
                if (inspectParamAnnotations(method.getParameterAnnotations(), method.toString(), builder)) {
                    builder.inspector(method);
                }
            }
        }

        if (clazz.getSuperclass() != null) {
            detectConfigMethods(builder, clazz.getSuperclass());
        }
    }

    /**
     * <p>
     * Adds the {@link PropertySetter} to the {@link Builder} specified in parameter extracted from the given annotated parameters.
     * </p>
     *
     * @param annotations the annotation
     * @param methodName the method/constructor name
     * @param builder the builder
     * @return {@code true} if all annotations have been validated, {@code false} otherwise
     */
    private boolean inspectParamAnnotations(final Annotation[][] annotations,
                                            final String methodName,
                                            final Builder builder) {
        for (final Annotation[] paramAnnotation : annotations) {
            try {
                if (1 != paramAnnotation.length) {
                    logger.warn(String.format(IAE_MSG, methodName), new IllegalArgumentException());
                    return false;
                }

                final PropertySetter propertySetter = PropertySetterFactory.INSTANCE.create(builder, paramAnnotation[0]);

                if (propertySetter == null) {
                    logger.warn(String.format(IAE_MSG, methodName), new IllegalArgumentException());
                    return false;
                } else {
                    builder.addPropertySetter(methodName, propertySetter);
                }
            } catch (Exception e) {
                logger.error(String.format("Unable to create a builder with constructor %s", methodName), e);
                return false;
            }
        }

        return true;
    }
}
