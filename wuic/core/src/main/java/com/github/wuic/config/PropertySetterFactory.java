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

import com.github.wuic.exception.UnableToInstantiateException;
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
 * @version 1.0
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
     * @throws UnableToInstantiateException if the {@link PropertySetter} could not be instantiated
     */
    @SuppressWarnings("unchecked")
    public <T> PropertySetter<T> create(final AbstractObjectBuilder<T> builder, final Annotation annotation)
            throws UnableToInstantiateException {
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
                    logger.error("Cannot instantiate PropertySetter. Make sure it provides a default constructor.");
                    throw new UnableToInstantiateException(ie);
                } catch (IllegalAccessException iae) {
                    throw new UnableToInstantiateException(iae);
                } catch (InvocationTargetException ite) {
                    throw new UnableToInstantiateException(ite);
                }
            }
        }

        return null;
    }
}
