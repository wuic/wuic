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

import com.github.wuic.exception.WuicException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * <p>
 * Factory that creates a {@link PropertySetter} based on a parameter type. The parameter must be annotated with
 * a supported annotation:
 * <ul>
 *     <li>{@link BooleanConfigParam}</li>
 *     <li>{@link StringConfigParam}</li>
 *     <li>{@link ObjectConfigParam}</li>
 *     <li>{@link IntegerConfigParam}</li>
 * </ul>
 * </p>
 *
 * @author Guillaume DROUET
 * @since 0.5
 */
public enum PropertySetterFactory {

    /**
     * The single instance.
     */
    INSTANCE;

    /**
     * The logger.
     */
    private final Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * Supported annotations.
     */
    private Map<Class<? extends Annotation>, Constructor<? extends ConfigParam>> annotations;

    /**
     * <p>
     * Builds a new instance.
     * </p>
     */
    private PropertySetterFactory() {
        this.annotations = new HashMap<Class<? extends Annotation>, Constructor<? extends ConfigParam>>();
        put(BooleanConfigParam.class, BooleanConfigParam.ConfigParamImpl.class);
        put(StringConfigParam.class, StringConfigParam.ConfigParamImpl.class);
        put(ObjectConfigParam.class, ObjectConfigParam.ConfigParamImpl.class);
        put(IntegerConfigParam.class, IntegerConfigParam.ConfigParamImpl.class);
    }

    /**
     * <p>
     * Puts a new annotation support.
     * </p>
     *
     * @param annotation the annotation
     * @param configParam the {@link ConfigParam} implementation that deals with the annotation
     */
    private void put(final Class<? extends Annotation> annotation, final Class<? extends ConfigParam> configParam) {
        try {
            this.annotations.put(annotation, configParam.getConstructor(annotation));
        } catch (NoSuchMethodException nse) {
            logger.error("Any ConfigParam implementation must expose a public constructor with its managed annotation in parameter", nse);
        }
    }

    /**
     * <p>
     * Creates a new {@link PropertySetter} for the given builder and the specified parameter annotation.
     * </p>
     *
     * @param builder the builder
     * @param annotation the parameter annotation
     * @param <T> the property setter type
     * @return the new {@link PropertySetter}
     */
    @SuppressWarnings("unchecked")
    public <T> PropertySetter<T> create(final AbstractObjectBuilder<T> builder, final Annotation annotation) {
        // Check if the parameter is annotated with a supported annotation
        for (final Entry<Class<? extends Annotation>, Constructor<? extends ConfigParam>> entry : annotations.entrySet()) {
            if (annotation.toString().contains(entry.getKey().getName())) {
                try {
                    // Constructor must expect the annotation in parameter
                    final ConfigParam configParam = entry.getValue().newInstance(annotation);
                    final PropertySetter<T> retval = configParam.setter().newInstance();
                    retval.init(builder, configParam.propertyKey(), configParam.defaultValue());
                    return retval;
                } catch (InstantiationException ie) {
                    WuicException.throwUnableToInstantiateException(new IllegalArgumentException(
                            "Cannot instantiate PropertySetter. Make sure it provides a default constructor.", ie));
                } catch (IllegalAccessException iae) {
                    WuicException.throwUnableToInstantiateException(iae);
                } catch (InvocationTargetException ite) {
                    WuicException.throwUnableToInstantiateException(ite);
                }
            }
        }

        return null;
    }
}
